@file:OptIn(ExperimentalMaterial3Api::class)

package com.studiomovel.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

// ---------- Caixa de entrada de um canal ----------

@Composable
fun TelaCaixa(conta: ContaCanal, aut: Autorizador, voltar: () -> Unit, abrir: (String) -> Unit) {
    BackHandler(onBack = voltar)
    val escopo = rememberCoroutineScope()
    var emails by remember { mutableStateOf<List<Email>>(emptyList()) }
    var proxima by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var carregandoMais by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    fun carregar() {
        escopo.launch {
            carregando = true
            erro = null
            try {
                val token = aut.token(conta.email)
                if (token == null) {
                    erro = "Permissão não concedida."
                } else {
                    val p = GmailApi.caixaDeEntrada(token)
                    emails = p.emails
                    proxima = p.proxima
                }
            } catch (e: Exception) {
                erro = mensagemErro(e)
            } finally {
                carregando = false
            }
        }
    }

    fun carregarMais() {
        val pagina = proxima ?: return
        escopo.launch {
            carregandoMais = true
            try {
                val token = aut.token(conta.email) ?: return@launch
                val p = GmailApi.caixaDeEntrada(token, pagina)
                emails = emails + p.emails.filter { novo -> emails.none { it.id == novo.id } }
                proxima = p.proxima
            } catch (e: Exception) {
                erro = mensagemErro(e)
            } finally {
                carregandoMais = false
            }
        }
    }
    LaunchedEffect(conta.email) { carregar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("E-mail", maxLines = 1)
                        Text(
                            conta.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                colors = barraTransparente(),
                navigationIcon = {
                    IconButton(onClick = voltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { carregar() }, enabled = !carregando) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                },
            )
        },
        containerColor = Color.Transparent,
    ) { margem ->
        Box(Modifier.padding(margem).fillMaxSize()) {
            when {
                carregando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                erro != null && emails.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(erro ?: "", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { carregar() }) { Text("Tentar de novo") }
                }
                emails.isEmpty() -> Text("Caixa de entrada vazia.", Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(emails, key = { it.id }) { e ->
                        ItemEmail(e) {
                            emails = emails.map { if (it.id == e.id) it.copy(naoLido = false) else it }
                            abrir(e.id)
                        }
                    }
                    if (proxima != null) item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (carregandoMais) CircularProgressIndicator(Modifier.padding(12.dp))
                            else TextButton(onClick = { carregarMais() }) { Text("Carregar mais") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemEmail(e: Email, aoTocar: () -> Unit) {
    val peso = if (e.naoLido) FontWeight.Bold else FontWeight.Normal
    Card(
        Modifier.fillMaxWidth().clickable(onClick = aoTocar),
        colors = CardDefaults.cardColors(containerColor = CorCartao),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            // Bolinha com a inicial do remetente
            Surface(
                color = if (e.naoLido) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        e.remetente.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        e.remetente,
                        fontWeight = peso,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        quando(e.dataMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (e.naoLido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(e.assunto, fontWeight = peso, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    e.previa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------- Leitura de uma mensagem ----------

@Composable
fun TelaMensagem(conta: ContaCanal, aut: Autorizador, id: String, voltar: () -> Unit) {
    BackHandler(onBack = voltar)
    var msg by remember { mutableStateOf<EmailCompleto?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        try {
            val token = aut.token(conta.email)
            if (token == null) {
                erro = "Permissão não concedida."
            } else {
                msg = GmailApi.abrir(token, id)
                try {
                    GmailApi.marcarComoLido(token, id)
                } catch (e: Exception) {
                    // não atrapalha a leitura se falhar
                }
            }
        } catch (e: Exception) {
            erro = mensagemErro(e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(msg?.assunto ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = barraTransparente(),
                navigationIcon = {
                    IconButton(onClick = voltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        containerColor = Color.Transparent,
    ) { margem ->
        Box(Modifier.padding(margem).fillMaxSize()) {
            val m = msg
            when {
                erro != null -> Text(erro ?: "", Modifier.align(Alignment.Center).padding(32.dp))
                m == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Column(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(m.assunto, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text("De: ${m.remetente}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Para: ${m.para}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            quando(m.dataMs, completo = true),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // O corpo do e-mail aparece num "papel" branco, como no Gmail.
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        CorpoEmail(m.html)
                    }
                }
            }
        }
    }
}

/** Mostra o HTML do e-mail. JavaScript fica desligado; links abrem fora do app. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CorpoEmail(html: String) {
    val pagina = """
        <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
        <style>body{margin:12px;font-family:sans-serif;color:#202124;word-wrap:break-word}
        img{max-width:100%;height:auto} table{max-width:100%}</style></head>
        <body>$html</body></html>
    """.trimIndent()
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                setBackgroundColor(android.graphics.Color.WHITE)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        } catch (e: Exception) {
                            // sem app para abrir esse link
                        }
                        return true
                    }
                }
            }
        },
        update = { it.loadDataWithBaseURL(null, pagina, "text/html", "utf-8", null) },
    )
}

/** "14:32" se for hoje, "25/11" se for este ano, senão "25/11/2024". */
fun quando(ms: Long, completo: Boolean = false): String {
    if (ms <= 0) return ""
    val zona = java.time.ZoneId.systemDefault()
    val d = java.time.Instant.ofEpochMilli(ms).atZone(zona)
    val hoje = java.time.LocalDate.now(zona)
    val padrao = when {
        completo -> "dd/MM/yyyy 'às' HH:mm"
        d.toLocalDate() == hoje -> "HH:mm"
        d.year == hoje.year -> "dd/MM"
        else -> "dd/MM/yyyy"
    }
    return d.format(java.time.format.DateTimeFormatter.ofPattern(padrao))
}
