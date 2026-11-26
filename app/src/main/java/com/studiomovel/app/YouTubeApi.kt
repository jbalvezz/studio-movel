package com.studiomovel.app

import android.text.Html
import org.json.JSONObject
import java.net.URLEncoder

data class Video(
    val id: String,
    val titulo: String,
    val miniatura: String,
    val privacidade: String,   // public | unlisted | private
    val agendadoPara: String?, // data de publicação agendada, se houver
    val publicadoEm: String,
    val views: Long,
    val comentarios: Long,
)

data class Comentario(
    val id: String,
    val autor: String,
    val autorCanalId: String,
    val foto: String,
    val texto: String,
    val dataIso: String,
    val curtidas: Long,
    val videoId: String,
    val respostas: List<Comentario> = emptyList(),
    val totalRespostas: Int = 0,
    val podeResponder: Boolean = true,
)

class PaginaComentarios(val comentarios: List<Comentario>, val proxima: String?)

/** Conversa com a YouTube Data API v3. */
object YouTubeApi {
    private const val BASE = "https://www.googleapis.com/youtube/v3"

    /** Dados do canal da conta autorizada. */
    suspend fun meuCanal(token: String, email: String): ContaCanal {
        val json = Rede.get("$BASE/channels?part=snippet,statistics,contentDetails&mine=true", token)
        val itens = json.optJSONArray("items")
        if (itens == null || itens.length() == 0) {
            throw ErroApi(404, "Esta conta não tem canal no YouTube.")
        }
        val c = itens.getJSONObject(0)
        val snippet = c.getJSONObject("snippet")
        val stats = c.optJSONObject("statistics")
        return ContaCanal(
            email = email,
            canalId = c.getString("id"),
            titulo = snippet.optString("title"),
            foto = snippet.optJSONObject("thumbnails")?.optJSONObject("default")?.optString("url") ?: "",
            inscritos = if (stats != null && !stats.optBoolean("hiddenSubscriberCount"))
                stats.optString("subscriberCount").toLongOrNull() else null,
            playlistEnvios = c.getJSONObject("contentDetails")
                .getJSONObject("relatedPlaylists").getString("uploads"),
        )
    }

    /** Últimos vídeos enviados (inclui não listados e privados, porque é o dono que está pedindo). */
    suspend fun videos(token: String, playlistEnvios: String, maximo: Int = 50): List<Video> {
        val lista = Rede.get(
            "$BASE/playlistItems?part=contentDetails&maxResults=$maximo&playlistId=" +
                URLEncoder.encode(playlistEnvios, "UTF-8"),
            token
        ).optJSONArray("items") ?: return emptyList()
        val ids = (0 until lista.length()).map {
            lista.getJSONObject(it).getJSONObject("contentDetails").getString("videoId")
        }
        if (ids.isEmpty()) return emptyList()

        val itens = Rede.get(
            "$BASE/videos?part=snippet,status,statistics&id=" + ids.joinToString(","),
            token
        ).optJSONArray("items") ?: return emptyList()

        val porId = (0 until itens.length()).associate { i ->
            val v = itens.getJSONObject(i)
            val snippet = v.getJSONObject("snippet")
            val status = v.getJSONObject("status")
            val stats = v.optJSONObject("statistics")
            val thumbs = snippet.optJSONObject("thumbnails")
            v.getString("id") to Video(
                id = v.getString("id"),
                titulo = snippet.optString("title"),
                miniatura = (thumbs?.optJSONObject("medium") ?: thumbs?.optJSONObject("default"))
                    ?.optString("url") ?: "",
                privacidade = status.optString("privacyStatus"),
                agendadoPara = status.optString("publishAt").ifBlank { null },
                publicadoEm = snippet.optString("publishedAt"),
                views = stats?.optString("viewCount")?.toLongOrNull() ?: 0,
                comentarios = stats?.optString("commentCount")?.toLongOrNull() ?: 0,
            )
        }
        return ids.mapNotNull { porId[it] } // mantém a ordem: mais recente primeiro
    }

    /**
     * Muda a visibilidade: "public", "unlisted", "private" ou agendado (private + publishAt).
     * Lê o status atual antes e reenvia tudo, para não apagar outras opções do vídeo
     * (conteúdo para crianças, incorporação, licença...).
     */
    suspend fun mudarVisibilidade(token: String, videoId: String, privacidade: String, agendarPara: String?) {
        val atual = Rede.get("$BASE/videos?part=status&id=$videoId", token)
            .optJSONArray("items")?.optJSONObject(0)?.optJSONObject("status")
            ?: throw ErroApi(404, "Vídeo não encontrado.")
        val novo = JSONObject()
        for (campo in listOf("embeddable", "license", "publicStatsViewable", "selfDeclaredMadeForKids", "containsSyntheticMedia")) {
            if (atual.has(campo)) novo.put(campo, atual.get(campo))
        }
        novo.put("privacyStatus", privacidade)
        if (agendarPara != null) novo.put("publishAt", agendarPara)
        Rede.put(
            "$BASE/videos?part=status",
            token,
            JSONObject().put("id", videoId).put("status", novo),
        )
    }

    /** Comentários de um vídeo, ou de todos os vídeos do canal (se videoId for null). */
    suspend fun comentarios(token: String, canalId: String, videoId: String?, pagina: String? = null): PaginaComentarios {
        var url = "$BASE/commentThreads?part=snippet,replies&maxResults=30&order=time&textFormat=plainText"
        url += if (videoId != null) "&videoId=$videoId" else "&allThreadsRelatedToChannelId=$canalId"
        if (pagina != null) url += "&pageToken=" + URLEncoder.encode(pagina, "UTF-8")
        val json = Rede.get(url, token)
        val itens = json.optJSONArray("items")
        val lista = (0 until (itens?.length() ?: 0)).map { i ->
            val fio = itens!!.getJSONObject(i)
            val s = fio.getJSONObject("snippet")
            val principal = lerComentario(s.getJSONObject("topLevelComment"), s.optString("videoId"))
            val resp = fio.optJSONObject("replies")?.optJSONArray("comments")
            val respostas = (0 until (resp?.length() ?: 0))
                .map { lerComentario(resp!!.getJSONObject(it), s.optString("videoId")) }
                .sortedBy { it.dataIso } // mais antiga primeiro, como numa conversa
            principal.copy(
                respostas = respostas,
                totalRespostas = s.optInt("totalReplyCount"),
                podeResponder = s.optBoolean("canReply", true),
            )
        }
        return PaginaComentarios(lista, json.optString("nextPageToken").ifBlank { null })
    }

    /** Responde a um comentário. Devolve a resposta já publicada. */
    suspend fun responder(token: String, comentarioId: String, texto: String, videoId: String): Comentario {
        val r = Rede.post(
            "$BASE/comments?part=snippet",
            token,
            JSONObject().put("snippet", JSONObject().put("parentId", comentarioId).put("textOriginal", texto)),
        )
        return lerComentario(r, videoId).let { if (it.texto.isBlank()) it.copy(texto = texto) else it }
    }

    /** Oculta o comentário: ele vai para "Em análise" no YouTube Studio (dá para aprovar de volta lá). */
    suspend fun ocultar(token: String, comentarioId: String) {
        Rede.post("$BASE/comments/setModerationStatus?id=$comentarioId&moderationStatus=heldForReview", token)
    }

    /** Título e descrição atuais do vídeo. */
    suspend fun textos(token: String, videoId: String): Pair<String, String> {
        val s = Rede.get("$BASE/videos?part=snippet&id=$videoId", token)
            .optJSONArray("items")?.optJSONObject(0)?.optJSONObject("snippet")
            ?: throw ErroApi(404, "Vídeo não encontrado.")
        return s.optString("title") to s.optString("description")
    }

    /**
     * Troca título e descrição. Lê o snippet atual e reenvia categoria, tags e idiomas,
     * porque o YouTube apaga o que não for mandado de volta.
     */
    suspend fun editarTextos(token: String, videoId: String, titulo: String, descricao: String) {
        val atual = Rede.get("$BASE/videos?part=snippet&id=$videoId", token)
            .optJSONArray("items")?.optJSONObject(0)?.optJSONObject("snippet")
            ?: throw ErroApi(404, "Vídeo não encontrado.")
        val novo = JSONObject()
        for (campo in listOf("categoryId", "tags", "defaultLanguage", "defaultAudioLanguage")) {
            if (atual.has(campo)) novo.put(campo, atual.get(campo))
        }
        novo.put("title", titulo)
        novo.put("description", descricao)
        Rede.put(
            "$BASE/videos?part=snippet",
            token,
            JSONObject().put("id", videoId).put("snippet", novo),
        )
    }

    /** Títulos de vários vídeos de uma vez (para mostrar em qual vídeo foi o comentário). */
    suspend fun titulos(token: String, ids: List<String>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        val itens = Rede.get("$BASE/videos?part=snippet&id=" + ids.take(50).joinToString(","), token)
            .optJSONArray("items") ?: return emptyMap()
        return (0 until itens.length()).associate {
            val v = itens.getJSONObject(it)
            v.getString("id") to v.getJSONObject("snippet").optString("title")
        }
    }

    private fun lerComentario(c: JSONObject, videoId: String): Comentario {
        val s = c.optJSONObject("snippet") ?: JSONObject()
        val texto = s.optString("textOriginal").ifBlank {
            Html.fromHtml(s.optString("textDisplay"), Html.FROM_HTML_MODE_LEGACY).toString()
        }
        return Comentario(
            id = c.optString("id"),
            autor = s.optString("authorDisplayName"),
            autorCanalId = s.optJSONObject("authorChannelId")?.optString("value") ?: "",
            foto = s.optString("authorProfileImageUrl"),
            texto = texto.trim(),
            dataIso = s.optString("publishedAt"),
            curtidas = s.optLong("likeCount"),
            videoId = videoId,
        )
    }
}
