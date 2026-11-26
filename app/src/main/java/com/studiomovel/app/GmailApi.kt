package com.studiomovel.app

import android.text.Html
import android.util.Base64
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class Email(
    val id: String,
    val remetente: String,   // só o nome (ou o e-mail, se não tiver nome)
    val assunto: String,
    val previa: String,
    val dataMs: Long,
    val naoLido: Boolean,
)

data class EmailCompleto(
    val id: String,
    val remetente: String,   // "Nome <email>"
    val para: String,
    val assunto: String,
    val dataMs: Long,
    val html: String,        // corpo pronto para mostrar (texto simples vira HTML)
    val threadId: String = "",
    val responderPara: String = "",   // Reply-To, ou o remetente
    val idMensagem: String = "",      // cabeçalho Message-ID (para a resposta cair na mesma conversa)
    val referencias: String = "",
)

class PaginaEmails(val emails: List<Email>, val proxima: String?)

/** Conversa com a Gmail API (só a caixa de entrada, por enquanto). */
object GmailApi {
    private const val BASE = "https://gmail.googleapis.com/gmail/v1/users/me"

    suspend fun caixaDeEntrada(token: String, pagina: String? = null): PaginaEmails = coroutineScope {
        var url = "$BASE/messages?labelIds=INBOX&maxResults=25"
        if (pagina != null) url += "&pageToken=" + URLEncoder.encode(pagina, "UTF-8")
        val lista = Rede.get(url, token)
        val ids = lista.optJSONArray("messages")?.let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it).getString("id") }
        } ?: emptyList()

        // Busca os cabeçalhos de todas ao mesmo tempo (bem mais rápido).
        val emails = ids.map { id ->
            async {
                val m = Rede.get(
                    "$BASE/messages/$id?format=metadata&metadataHeaders=From&metadataHeaders=Subject",
                    token
                )
                val cab = cabecalhos(m.optJSONObject("payload"))
                Email(
                    id = id,
                    remetente = soNome(cab["from"] ?: ""),
                    assunto = (cab["subject"] ?: "").ifBlank { "(sem assunto)" },
                    previa = desfazerEntidades(m.optString("snippet")),
                    dataMs = m.optString("internalDate").toLongOrNull() ?: 0,
                    naoLido = rotulos(m).contains("UNREAD"),
                )
            }
        }.awaitAll()
        PaginaEmails(emails, lista.optString("nextPageToken").ifBlank { null })
    }

    suspend fun abrir(token: String, id: String): EmailCompleto {
        val m = Rede.get("$BASE/messages/$id?format=full", token)
        val payload = m.optJSONObject("payload")
        val cab = cabecalhos(payload)
        val html = acharParte(payload, "text/html")
        val texto = acharParte(payload, "text/plain")
        val corpo = when {
            html != null -> html
            texto != null -> "<div style=\"white-space:pre-wrap\">" + Html.escapeHtml(texto) + "</div>"
            else -> "<p>" + Html.escapeHtml(desfazerEntidades(m.optString("snippet"))) + "</p>"
        }
        return EmailCompleto(
            id = id,
            remetente = cab["from"] ?: "",
            para = cab["to"] ?: "",
            assunto = (cab["subject"] ?: "").ifBlank { "(sem assunto)" },
            dataMs = m.optString("internalDate").toLongOrNull() ?: 0,
            html = corpo,
            threadId = m.optString("threadId"),
            responderPara = (cab["reply-to"] ?: "").ifBlank { cab["from"] ?: "" },
            idMensagem = cab["message-id"] ?: "",
            referencias = cab["references"] ?: "",
        )
    }

    /** Tira o "não lido" da mensagem (igual a abrir no Gmail). */
    suspend fun marcarComoLido(token: String, id: String) {
        Rede.post(
            "$BASE/messages/$id/modify",
            token,
            JSONObject().put("removeLabelIds", JSONArray().put("UNREAD"))
        )
    }

    /** Responde ao e-mail (sai da própria conta, na mesma conversa, e fica em "Enviados"). */
    /**
     * Para quem a resposta vai. Se a mensagem foi enviada pela própria conta (ex.: você abriu
     * algo que você mesmo mandou), responde para o destinatário original, igual ao Gmail.
     */
    fun destinoResposta(m: EmailCompleto, meuEmail: String): String =
        if (soEmail(m.remetente).equals(meuEmail, ignoreCase = true) && m.para.isNotBlank()) m.para
        else m.responderPara

    suspend fun responder(token: String, m: EmailCompleto, texto: String, meuEmail: String) {
        val assunto = if (m.assunto.startsWith("Re:", ignoreCase = true)) m.assunto else "Re: ${m.assunto}"
        val cab = StringBuilder()
        val fim = "" + 13.toChar() + 10.toChar() // quebra de linha de e-mail (CR LF)
        val para = destinoResposta(m, meuEmail).filter { it != 13.toChar() && it != 10.toChar() }
        cab.append("To: ").append(para).append(fim)
        cab.append("Subject: =?UTF-8?B?").append(b64(assunto.toByteArray(Charsets.UTF_8), false)).append("?=").append(fim)
        if (m.idMensagem.isNotBlank()) {
            cab.append("In-Reply-To: ").append(m.idMensagem).append(fim)
            cab.append("References: ").append((m.referencias + " " + m.idMensagem).trim()).append(fim)
        }
        cab.append("MIME-Version: 1.0").append(fim)
        cab.append("Content-Type: text/plain; charset=UTF-8").append(fim)
        cab.append("Content-Transfer-Encoding: base64").append(fim).append(fim)
        cab.append(Base64.encodeToString(texto.toByteArray(Charsets.UTF_8), Base64.DEFAULT))
        val corpo = JSONObject().put("raw", b64(cab.toString().toByteArray(Charsets.UTF_8), true))
        if (m.threadId.isNotBlank()) corpo.put("threadId", m.threadId)
        Rede.post("$BASE/messages/send", token, corpo)
    }

    /** Tira da caixa de entrada (continua em "Todos os e-mails"). */
    suspend fun arquivar(token: String, id: String) {
        Rede.post("$BASE/messages/$id/modify", token, JSONObject().put("removeLabelIds", JSONArray().put("INBOX")))
    }

    /** Manda para a lixeira (o Gmail apaga de vez depois de 30 dias). */
    suspend fun lixeira(token: String, id: String) {
        Rede.post("$BASE/messages/$id/trash", token)
    }

    /** Arquiva vários de uma vez (uma chamada só). */
    suspend fun arquivarVarios(token: String, ids: Collection<String>) {
        if (ids.isEmpty()) return
        val lista = JSONArray().apply { ids.forEach { put(it) } }
        Rede.post(
            "$BASE/messages/batchModify",
            token,
            JSONObject().put("ids", lista).put("removeLabelIds", JSONArray().put("INBOX")),
        )
    }

    /** Manda vários para a lixeira (todos ao mesmo tempo). */
    suspend fun lixeiraVarios(token: String, ids: Collection<String>) = coroutineScope {
        ids.map { id -> async { lixeira(token, id) } }.awaitAll()
        Unit
    }

    /** Quantas conversas não lidas há na caixa de entrada. */
    suspend fun naoLidos(token: String): Int =
        Rede.get("$BASE/labels/INBOX", token).optInt("threadsUnread")

    // ---------- ajudantes ----------

    private fun b64(bytes: ByteArray, urlSafe: Boolean): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP or (if (urlSafe) Base64.URL_SAFE else 0))


    private fun rotulos(m: JSONObject): Set<String> {
        val arr = m.optJSONArray("labelIds") ?: return emptySet()
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    private fun cabecalhos(payload: JSONObject?): Map<String, String> {
        val arr = payload?.optJSONArray("headers") ?: return emptyMap()
        return (0 until arr.length()).associate {
            val h = arr.getJSONObject(it)
            h.optString("name").lowercase() to h.optString("value")
        }
    }

    /** Procura dentro da mensagem (que pode ter várias partes) o corpo do tipo pedido. */
    private fun acharParte(parte: JSONObject?, tipo: String): String? {
        if (parte == null) return null
        if (parte.optString("mimeType").startsWith(tipo)) {
            val dados = parte.optJSONObject("body")?.optString("data")
            if (!dados.isNullOrBlank()) return decodificar(dados)
        }
        val filhas = parte.optJSONArray("parts") ?: return null
        for (i in 0 until filhas.length()) {
            acharParte(filhas.getJSONObject(i), tipo)?.let { return it }
        }
        return null
    }

    private fun decodificar(dados: String): String =
        String(Base64.decode(dados, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8)

    private fun desfazerEntidades(s: String): String =
        Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString()

    /** "Fulano de Tal <fulano@x.com>" -> "fulano@x.com" */
    fun soEmail(from: String): String =
        if (from.contains("<")) from.substringAfter("<").substringBefore(">").trim() else from.trim()

    /** "Fulano de Tal <fulano@x.com>" -> "Fulano de Tal" */
    fun soNome(from: String): String {
        val nome = from.substringBefore("<").trim().trim('"')
        return nome.ifBlank { from.substringAfter("<").substringBefore(">").trim() }
    }
}
