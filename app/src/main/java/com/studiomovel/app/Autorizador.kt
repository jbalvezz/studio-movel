package com.studiomovel.app

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Cuida do login de cada conta Google.
 * O próprio Android (Google Play Services) guarda a autorização de cada conta e renova o acesso
 * sozinho — por isso não é preciso trocar de conta, e o app nunca vê senha nenhuma.
 *
 * Precisa ser criado ANTES da tela aparecer (como campo da Activity).
 */
class Autorizador(private val activity: ComponentActivity) {

    companion object {
        val ESCOPOS = listOf(
            Scope("https://www.googleapis.com/auth/youtube.force-ssl"),
            Scope("https://www.googleapis.com/auth/gmail.modify"),
        )
    }

    private val cliente by lazy { Identity.getAuthorizationClient(activity) }
    private val fila = Mutex() // uma janela de permissão por vez

    private var esperandoConta: CompletableDeferred<String?>? = null
    private val seletorConta = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        val email = if (r.resultCode == Activity.RESULT_OK)
            r.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) else null
        esperandoConta?.complete(email)
        esperandoConta = null
    }

    private var esperandoPermissao: CompletableDeferred<String?>? = null
    private val telaPermissao = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { r ->
        val dados = r.data
        val token = if (r.resultCode == Activity.RESULT_OK && dados != null) {
            try {
                cliente.getAuthorizationResultFromIntent(dados).accessToken
            } catch (e: Exception) {
                null
            }
        } else null
        esperandoPermissao?.complete(token)
        esperandoPermissao = null
    }

    /** Abre a lista de contas Google do celular e devolve o e-mail escolhido (ou null se cancelar). */
    suspend fun escolherConta(): String? {
        val intent = AccountManager.newChooseAccountIntent(
            null, null, arrayOf("com.google"), null, null, null, null
        )
        val espera = CompletableDeferred<String?>()
        esperandoConta = espera
        seletorConta.launch(intent)
        return espera.await()
    }

    /**
     * Devolve um "passe de acesso" válido para a conta. Se ainda não houver permissão,
     * mostra a tela do Google pedindo autorização (só na primeira vez).
     * Retorna null se a pessoa cancelar.
     */
    suspend fun token(email: String): String? = fila.withLock {
        val pedido = AuthorizationRequest.builder()
            .setRequestedScopes(ESCOPOS)
            .setAccount(Account(email, "com.google"))
            .build()
        val resultado = cliente.authorize(pedido).await()
        if (resultado.hasResolution()) {
            val pendente = resultado.pendingIntent ?: return@withLock null
            val espera = CompletableDeferred<String?>()
            esperandoPermissao = espera
            telaPermissao.launch(IntentSenderRequest.Builder(pendente.intentSender).build())
            espera.await()
        } else {
            resultado.accessToken
        }
    }
}
