@file:OptIn(ExperimentalMaterial3Api::class)

package com.studiomovel.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

// ---------- Comentários (de um vídeo ou do canal todo) ----------

/** videoId == null → comentários recentes de todos os vídeos do canal. */
@Composable
fun TelaComentarios(conta: ContaCanal, aut: Autorizador, videoId: String?, tituloVideo: String?, voltar: () -> Unit) {
    BackHandler(onBack = voltar)
    val escopo = rememberCoroutineScope()
    val aviso = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    var lista by remember { mutableStateOf<List<Comentario>>(emptyList()) }
    var titulos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var proxima by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var carregandoMais by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var respondendo by remember { mutableStateOf<Comentario?>(null) }
    var paraOcultar by remember { mutableStateOf<Comentario?>(null) }
    var enviando by remember { mutableStateOf(false) }

    suspend fun buscarTitulos(token: String, novos: List<Comentario>) {
        if (videoId != null) return
        val faltam = novos.map { it.videoId }.distinct().filter { it.isNotBlank() && it !in titulos }
        try {
            titulos = titulos + YouTubeApi.titulos(token, faltam)
        } catch (e: Exception) {
            // sem título não tem problema
        }
    }

    fun carregar() {
        escopo.launch {
            carregando = true
            erro = null
            try {
                val token = aut.token(conta.email)
                if (token == null) {
                    erro = "Permissão não concedida."
                } else {
                    val p = YouTubeApi.comentarios(token, conta.canalId, videoId)
                    lista = p.comentarios
                    proxima = p.proxima
                    buscarTitulos(token, p.comentarios)
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
                val p = YouTubeApi.comentarios(token, conta.canalId, videoId, pagina)
                lista = lista + p.comentarios.filter { novo -> lista.none { it.id == novo.id } }
                proxima = p.proxima
                buscarTitulos(token, p.comentarios)
            } catch (e: Exception) {
                aviso.showSnackbar(mensagemErro(e))
            } finally {
                carregandoMais = false
            }
        }
    }

    fun enviarResposta(c: Comentario, texto: String) {
        escopo.launch {
            enviando = true
            try {
                val token = aut.token(conta.email) ?: return@launch
                val r = YouTubeApi.responder(token, c.id, texto, c.videoId)
                    .copy(autor = conta.titulo, foto = conta.foto, autorCanalId = conta.canalId)
                lista = lista.map {
                    if (it.id == c.id) it.copy(respostas = it.respostas + r, totalRespostas = it.totalRespostas + 1) else it
                }
                respondendo = null
                aviso.showSnackbar("Resposta publicada!")
            } catch (e: Exception) {
                aviso.showSnackbar(mensagemErro(e))
            } finally {
                enviando = false
            }
        }
    }

    fun ocultar(c: Comentario) {
        escopo.launch {
            try {
                val token = aut.token(conta.email) ?: return@launch
                YouTubeApi.ocultar(token, c.id)
                lista = lista.filter { it.id != c.id }
                aviso.showSnackbar("Comentário ocultado. Ele fica em \"Em análise\" no YouTube Studio.")
            } catch (e: Exception) {
                aviso.showSnackbar(mensagemErro(e))
            }
        }
    }

    LaunchedEffect(conta.email, videoId) { carregar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (videoId == null) "Comentários recentes" else "Comentários", maxLines = 1)
                        Text(
                            tituloVideo ?: conta.titulo,
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
        snackbarHost = { SnackbarHost(aviso) },
    ) { margem ->
        Box(Modifier.padding(margem).fillMaxSize()) {
            when {
                carregando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                erro != null -> Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(erro ?: "", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { carregar() }) { Text("Tentar de novo") }
                }
                lista.isEmpty() -> Text(
                    "Nenhum comentário ainda.",
                    Modifier.align(Alignment.Center).padding(32.dp),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(lista, key = { it.id }) { c ->
                        CartaoComentario(
                            c = c,
                            meuCanalId = conta.canalId,
                            tituloVideo = if (videoId == null) titulos[c.videoId] else null,
                            responder = { respondendo = c },
                            ocultar = { paraOcultar = c },
                            abrirVideo = {
                                ctx.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${c.videoId}&lc=${c.id}"))
                                )
                            },
                        )
                    }
                    if (proxima != null) item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (carregandoMais) CircularProgressIndicator(Modifier.size(28.dp))
                            else TextButton(onClick = { carregarMais() }) { Text("Carregar mais") }
                        }
                    }
                }
            }
        }
    }

    respondendo?.let { c ->
        var texto by remember(c.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!enviando) respondendo = null },
            title = { Text("Responder ${c.autor}") },
            text = {
                Column {
                    Text(
                        c.texto,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Sua resposta") },
                        minLines = 3,
                        enabled = !enviando,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { enviarResposta(c, texto.trim()) },
                    enabled = texto.isNotBlank() && !enviando,
                ) { Text(if (enviando) "Enviando..." else "Publicar") }
            },
            dismissButton = {
                TextButton(onClick = { respondendo = null }, enabled = !enviando) { Text("Cancelar") }
            },
        )
    }

    paraOcultar?.let { c ->
        AlertDialog(
            onDismissRequest = { paraOcultar = null },
            title = { Text("Ocultar este comentário?") },
            text = {
                Text("O comentário de ${c.autor} some do vídeo e vai para \"Em análise\" no YouTube Studio. Se mudar de ideia, dá para aprovar de volta lá.")
            },
            confirmButton = {
                TextButton(onClick = { ocultar(c); paraOcultar = null }) { Text("Ocultar") }
            },
            dismissButton = { TextButton(onClick = { paraOcultar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun CartaoComentario(
    c: Comentario,
    meuCanalId: String,
    tituloVideo: String?,
    responder: () -> Unit,
    ocultar: () -> Unit,
    abrirVideo: () -> Unit,
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CorCartao)) {
        Column(Modifier.padding(12.dp)) {
            if (tituloVideo != null) {
                Text(
                    "▶ $tituloVideo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
            }
            LinhaComentario(c, meuCanalId, grande = true)
            c.respostas.forEach { r ->
                Spacer(Modifier.height(8.dp))
                Row {
                    Spacer(Modifier.width(36.dp))
                    LinhaComentario(r, meuCanalId, grande = false)
                }
            }
            if (c.totalRespostas > c.respostas.size) {
                Text(
                    "+ ${c.totalRespostas - c.respostas.size} respostas (veja no YouTube)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 36.dp, top = 6.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = abrirVideo) { Text("Ver no YouTube") }
                if (c.autorCanalId != meuCanalId) {
                    TextButton(onClick = ocultar) { Text("Ocultar", color = MaterialTheme.colorScheme.tertiary) }
                }
                if (c.podeResponder) TextButton(onClick = responder) { Text("Responder") }
            }
        }
    }
}

@Composable
private fun LinhaComentario(c: Comentario, meuCanalId: String, grande: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        AsyncImage(
            model = c.foto,
            contentDescription = null,
            modifier = Modifier.size(if (grande) 32.dp else 24.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.autor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (c.autorCanalId == meuCanalId) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    "  ·  " + dataHora(c.dataIso) + if (c.curtidas > 0) "  ·  👍 ${numero(c.curtidas)}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(c.texto, style = if (grande) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall)
        }
    }
}
