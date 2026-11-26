package com.studiomovel.app

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
}
