package com.studiomovel.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Uma conta Google conectada e o canal do YouTube dela (guardado para mostrar mesmo sem internet). */
data class ContaCanal(
    val email: String,
    val canalId: String,
    val titulo: String,
    val foto: String,
    val inscritos: Long?,
    val playlistEnvios: String,
)

/** Guarda a lista de contas no próprio celular. Não guarda senhas nem tokens. */
class Contas(context: Context) {
    private val prefs = context.getSharedPreferences("contas", Context.MODE_PRIVATE)

    fun listar(): List<ContaCanal> {
        val arr = JSONArray(prefs.getString("lista", "[]"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ContaCanal(
                email = o.getString("email"),
                canalId = o.optString("canalId"),
                titulo = o.optString("titulo"),
                foto = o.optString("foto"),
                inscritos = if (o.has("inscritos")) o.getLong("inscritos") else null,
                playlistEnvios = o.optString("playlistEnvios"),
            )
        }
    }

    /** Adiciona ou atualiza (mesmo e-mail = mesma conta). */
    fun salvar(c: ContaCanal) {
        val nova = listar().filter { it.email != c.email } + c
        gravar(nova)
    }

    /** Atualiza os dados sem mudar a ordem da lista. */
    fun atualizar(c: ContaCanal) {
        gravar(listar().map { if (it.email == c.email) c else it })
    }

    fun remover(email: String) = gravar(listar().filter { it.email != email })

    /** Sobe (passo = -1) ou desce (passo = +1) um canal na lista. */
    fun mover(email: String, passo: Int) {
        val lista = listar().toMutableList()
        val i = lista.indexOfFirst { it.email == email }
        val j = i + passo
        if (i < 0 || j !in lista.indices) return
        val c = lista.removeAt(i)
        lista.add(j, c)
        gravar(lista)
    }

    private fun gravar(lista: List<ContaCanal>) {
        val arr = JSONArray()
        lista.forEach { c ->
            arr.put(JSONObject().apply {
                put("email", c.email)
                put("canalId", c.canalId)
                put("titulo", c.titulo)
                put("foto", c.foto)
                c.inscritos?.let { put("inscritos", it) }
                put("playlistEnvios", c.playlistEnvios)
            })
        }
        prefs.edit().putString("lista", arr.toString()).apply()
    }
}
