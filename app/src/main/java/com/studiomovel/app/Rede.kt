package com.studiomovel.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ErroApi(val codigo: Int, mensagem: String) : Exception(mensagem)

/** Chamadas HTTP às APIs do Google com o "passe de acesso" da conta. */
object Rede {
    suspend fun get(url: String, token: String): JSONObject = chamar("GET", url, token, null)

    suspend fun post(url: String, token: String, corpo: JSONObject): JSONObject =
        chamar("POST", url, token, corpo)

    private suspend fun chamar(metodo: String, url: String, token: String, corpo: JSONObject?): JSONObject =
        withContext(Dispatchers.IO) {
            val con = URL(url).openConnection() as HttpURLConnection
            try {
                con.requestMethod = metodo
                con.setRequestProperty("Authorization", "Bearer $token")
                con.setRequestProperty("Accept", "application/json")
                con.connectTimeout = 15_000
                con.readTimeout = 20_000
                if (corpo != null) {
                    con.doOutput = true
                    con.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    con.outputStream.use { it.write(corpo.toString().toByteArray()) }
                }
                val codigo = con.responseCode
                if (codigo !in 200..299) {
                    val texto = con.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    val msg = try {
                        JSONObject(texto).getJSONObject("error").optString("message")
                    } catch (e: Exception) {
                        texto.take(200)
                    }
                    throw ErroApi(codigo, msg.ifBlank { "Erro $codigo" })
                }
                val texto = con.inputStream.bufferedReader().use { it.readText() }
                if (texto.isBlank()) JSONObject() else JSONObject(texto)
            } finally {
                con.disconnect()
            }
        }
}
