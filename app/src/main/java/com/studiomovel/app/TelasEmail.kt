@file:OptIn(ExperimentalMaterial3Api::class)

package com.studiomovel.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
    var selecionados by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmarLixeira by remember { mutableStateOf(false) }
    var ocupado by remember { mutableStateOf(false) }
    val aviso = remember { SnackbarHostState() }
    val selecionando = selecionados.isNotEmpty()
    BackHandler { if (selecionando) selecionados = emptySet() else voltar() }
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

    /** Arquiva ou manda para a lixeira todos os selecionados. */
    fun emLote(lixeira: Boolean) {
        val ids = selecionados.toList()
        escopo.launch {
            ocupado = true
            try {
                val token = aut.token(conta.email) ?: return@launch
                if (lixeira) GmailApi.lixeiraVarios(token, ids) else GmailApi.arquivarVarios(token, ids)
                emails = emails.filter { it.id !in ids }
                selecionados = emptySet()
                val n = ids.size
                aviso.showSnackbar(
                    if (lixeira) "$n ${if (n == 1) "e-mail foi" else "e-mails foram"} para a lixeira."
                    else "$n ${if (n == 1) "e-mail arquivado" else "e-mails arquivados"}."
                )
            } catch (e: Exception) {
                aviso.showSnackbar(mensagemErro(e))
            } finally {
                ocupado = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(aviso) },
        topBar = {
            if (selecionando) TopAppBar(
                title = { Text("${selecionados.size} selecionado" + if (selecionados.size > 1) "s" else "") },
                colors = barraTransparente(),
                navigationIcon = {
                    IconButton(onClick = { selecionados = emptySet() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar seleção")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { selecionados = emails.map { it.id }.toSet() },
                        enabled = !ocupado,
                    ) { Text("Todos") }
                    TextButton(onClick = { emLote(lixeira = false) }, enabled = !ocupado) { Text("Arquivar") }
                    TextButton(onClick = { confirmarLixeira = true }, enabled = !ocupado) {
                        Text("Lixeira", color = MaterialTheme.colorScheme.tertiary)
                    }
                },
            ) else TopAppBar(
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
            if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
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
                    if (!selecionando) item {
                        Text(
                            "Dica: segure o dedo num e-mail para selecionar vários.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                        )
                    }
                    items(emails, key = { it.id }) { e ->
                        fun alternar() {
                            selecionados = if (e.id in selecionados) selecionados - e.id else selecionados + e.id
                        }
                        ItemEmail(
                            e = e,
                            selecionado = e.id in selecionados,
                            aoTocar = {
                                if (selecionando) {
                                    alternar()
                                } else {
                                    emails = emails.map { if (it.id == e.id) it.copy(naoLido = false) else it }
                                    abrir(e.id)
                                }
                            },
                            aoSegurar = { alternar() },
                        )
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
    if (confirmarLixeira) {
        val n = selecionados.size
        AlertDialog(
            onDismissRequest = { confirmarLixeira = false },
            title = { Text("Mandar $n ${if (n == 1) "e-mail" else "e-mails"} para a lixeira?") },
            text = { Text("Eles vão para a lixeira do Gmail. Dá para recuperar por lá durante 30 dias.") },
            confirmButton = {
                TextButton(onClick = { confirmarLixeira = false; emLote(lixeira = true) }) { Text("Mandar para a lixeira") }
            },
            dismissButton = { TextButton(onClick = { confirmarLixeira = false }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemEmail(e: Email, selecionado: Boolean, aoTocar: () -> Unit, aoSegurar: () -> Unit) {
    val peso = if (e.naoLido) FontWeight.Bold else FontWeight.Normal
    Card(
        Modifier.fillMaxWidth().combinedClickable(onClick = aoTocar, onLongClick = aoSegurar),
        colors = CardDefaults.cardColors(
            containerColor = if (selecionado) MaterialTheme.colorScheme.primaryContainer else CorCartao
        ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            // Bolinha com a inicial do remetente
            Surface(
                color = when {
                    selecionado -> MaterialTheme.colorScheme.primary
                    e.naoLido -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (selecionado) "✓" else e.remetente.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold,
                        color = if (selecionado) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
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
    val escopo = rememberCoroutineScope()
    val aviso = remember { SnackbarHostState() }
    var msg by remember { mutableStateOf<EmailCompleto?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var respondendo by remember { mutableStateOf(false) }
    var confirmarLixeira by remember { mutableStateOf(false) }
    var ocupado by remember { mutableStateOf(false) }

    /** Roda uma ação no e-mail; se der certo e sairDepois, volta para a caixa. */
    fun acao(sairDepois: Boolean, sucesso: String, bloco: suspend (String) -> Unit) {
        escopo.launch {
            ocupado = true
            try {
                val token = aut.token(conta.email) ?: return@launch
                bloco(token)
                if (sairDepois) voltar() else aviso.showSnackbar(sucesso)
            } catch (e: Exception) {
                aviso.showSnackbar(mensagemErro(e))
            } finally {
                ocupado = false
            }
        }
    }

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
        snackbarHost = { SnackbarHost(aviso) },
        bottomBar = {
            if (msg != null) Row(
                Modifier.fillMaxWidth().background(CorFundo1).navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { respondendo = true }, enabled = !ocupado, modifier = Modifier.weight(1f)) {
                    Text("Responder")
                }
                OutlinedButton(
                    onClick = { acao(true, "") { GmailApi.arquivar(it, id) } },
                    enabled = !ocupado,
                    modifier = Modifier.weight(1f),
                ) { Text("Arquivar") }
                OutlinedButton(
                    onClick = { confirmarLixeira = true },
                    enabled = !ocupado,
                    modifier = Modifier.weight(1f),
                ) { Text("Lixeira", color = MaterialTheme.colorScheme.tertiary) }
            }
        },
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

    val m = msg
    if (respondendo && m != null) {
        var texto by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!ocupado) respondendo = false },
            title = { Text("Responder") },
            text = {
                Column {
                    Text(
                        "Para: ${GmailApi.destinoResposta(m, conta.email)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "De: ${conta.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Escreva sua resposta") },
                        minLines = 5,
                        enabled = !ocupado,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = texto.isNotBlank() && !ocupado,
                    onClick = {
                        val t = texto.trim()
                        acao(false, "Resposta enviada!") {
                            GmailApi.responder(it, m, t, conta.email)
                            respondendo = false
                        }
                    },
                ) { Text(if (ocupado) "Enviando..." else "Enviar") }
            },
            dismissButton = { TextButton(onClick = { respondendo = false }, enabled = !ocupado) { Text("Cancelar") } },
        )
    }

    if (confirmarLixeira) {
        AlertDialog(
            onDismissRequest = { confirmarLixeira = false },
            title = { Text("Mandar para a lixeira?") },
            text = { Text("O e-mail vai para a lixeira do Gmail. Dá para recuperar por lá durante 30 dias.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarLixeira = false
                    acao(true, "") { GmailApi.lixeira(it, id) }
                }) { Text("Mandar para a lixeira") }
            },
            dismissButton = { TextButton(onClick = { confirmarLixeira = false }) { Text("Cancelar") } },
        )
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
