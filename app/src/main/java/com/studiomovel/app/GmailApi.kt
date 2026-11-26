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

    // ---------- ajudantes ----------

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

    /** "Fulano de Tal <fulano@x.com>" -> "Fulano de Tal" */
    fun soNome(from: String): String {
        val nome = from.substringBefore("<").trim().trim('"')
        return nome.ifBlank { from.substringAfter("<").substringBefore(">").trim() }
    }
}
