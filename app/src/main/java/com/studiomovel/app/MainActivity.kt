@file:OptIn(ExperimentalMaterial3Api::class)

package com.studiomovel.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    // Criado aqui (antes da tela) porque registra as janelas de login do Google.
    private val autorizador = Autorizador(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val contas = Contas(this)
        setContent { Tema { App(contas, autorizador) } }
    }
}

// ---------- Tema ----------

@Composable
fun Tema(conteudo: @Composable () -> Unit) {
    val escuro = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val cores = when {
        Build.VERSION.SDK_INT >= 31 -> if (escuro) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        escuro -> darkColorScheme(primary = Color(0xFFB69CFF), secondary = Color(0xFF6FD3FF))
        else -> lightColorScheme(primary = Color(0xFF5B2BB5), secondary = Color(0xFF0B6FA8))
    }
    MaterialTheme(colorScheme = cores) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { conteudo() }
    }
}

// ---------- Navegação ----------

@Composable
fun App(contas: Contas, aut: Autorizador) {
    var lista by remember { mutableStateOf(contas.listar()) }
    var abertoEmail by rememberSaveable { mutableStateOf<String?>(null) }
    val aberto = lista.firstOrNull { it.email == abertoEmail }

    if (aberto == null) {
        TelaCanais(
            lista = lista,
            contas = contas,
            aut = aut,
            aoMudar = { lista = contas.listar() },
            abrir = { abertoEmail = it.email },
        )
    } else {
        TelaVideos(conta = aberto, aut = aut, voltar = { abertoEmail = null })
    }
}

// ---------- Tela 1: meus canais ----------

@Composable
fun TelaCanais(
    lista: List<ContaCanal>,
    contas: Contas,
    aut: Autorizador,
    aoMudar: () -> Unit,
    abrir: (ContaCanal) -> Unit,
) {
    val escopo = rememberCoroutineScope()
    val aviso = remember { SnackbarHostState() }
    var ocupado by remember { mutableStateOf(false) }
    var paraRemover by remember { mutableStateOf<ContaCanal?>(null) }

    fun adicionar() {
        escopo.launch {
            val email = aut.escolherConta() ?: return@launch
            ocupado = true
            try {
                val token = aut.token(email)
                if (token == null) {
                    aviso.showSnackbar("Permissão não concedida para $email.")
                } else {
                    val canal = YouTubeApi.meuCanal(token, email)
                    contas.salvar(canal)
                    aoMudar()
                    aviso.showSnackbar("${canal.titulo} conectado!")
                }
            } catch (e: Exception) {
                aviso.showSnackbar(mensagemErro(e))
            } finally {
                ocupado = false
            }
        }
    }

    fun atualizarTodos() {
        escopo.launch {
            ocupado = true
            val falhas = mutableListOf<String>()
            for (c in contas.listar()) {
                try {
                    val token = aut.token(c.email) ?: continue
                    contas.atualizar(YouTubeApi.meuCanal(token, c.email))
                } catch (e: Exception) {
                    falhas += "${c.titulo}: ${mensagemErro(e)}"
                }
            }
            aoMudar()
            ocupado = false
            if (falhas.isNotEmpty()) aviso.showSnackbar(falhas.joinToString("\n"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus canais") },
                actions = {
                    if (lista.isNotEmpty()) {
                        IconButton(onClick = { atualizarTodos() }, enabled = !ocupado) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!ocupado) adicionar() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Adicionar canal") },
            )
        },
        snackbarHost = { SnackbarHost(aviso) },
    ) { margem ->
        Column(Modifier.padding(margem).fillMaxSize()) {
            if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (lista.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Nenhum canal ainda.\n\nToque em \"Adicionar canal\" e escolha uma das suas contas Google.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(lista, key = { it.email }) { c ->
                        CartaoCanal(c, abrir = { abrir(c) }, remover = { paraRemover = c })
                    }
                }
            }
        }
    }

    paraRemover?.let { c ->
        AlertDialog(
            onDismissRequest = { paraRemover = null },
            title = { Text("Remover ${c.titulo}?") },
            text = { Text("O canal sai só deste app. Nada é apagado no YouTube. Você pode adicionar de novo quando quiser.") },
            confirmButton = {
                TextButton(onClick = { contas.remover(c.email); aoMudar(); paraRemover = null }) { Text("Remover") }
            },
            dismissButton = { TextButton(onClick = { paraRemover = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun CartaoCanal(c: ContaCanal, abrir: () -> Unit, remover: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = abrir)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = c.foto,
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    c.inscritos?.let { "${numero(it)} inscritos" } ?: "Inscritos ocultos",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    c.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = remover) {
                Icon(Icons.Default.Delete, contentDescription = "Remover canal")
            }
        }
    }
}

// ---------- Tela 2: vídeos de um canal ----------

private enum class Filtro(val rotulo: String) {
    TODOS("Todos"), PUBLICOS("Públicos"), NAO_LISTADOS("Não listados"), PRIVADOS("Privados")
}

@Composable
fun TelaVideos(conta: ContaCanal, aut: Autorizador, voltar: () -> Unit) {
    BackHandler(onBack = voltar)
    val escopo = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var filtro by rememberSaveable { mutableStateOf(Filtro.TODOS) }

    fun carregar() {
        escopo.launch {
            carregando = true
            erro = null
            try {
                val token = aut.token(conta.email)
                if (token == null) {
                    erro = "Permissão não concedida."
                } else {
                    videos = YouTubeApi.videos(token, conta.playlistEnvios)
                }
            } catch (e: Exception) {
                erro = mensagemErro(e)
            } finally {
                carregando = false
            }
        }
    }
    LaunchedEffect(conta.email) { carregar() }

    val visiveis = videos.filter {
        when (filtro) {
            Filtro.TODOS -> true
            Filtro.PUBLICOS -> it.privacidade == "public"
            Filtro.NAO_LISTADOS -> it.privacidade == "unlisted"
            Filtro.PRIVADOS -> it.privacidade == "private"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(conta.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
    ) { margem ->
        Column(Modifier.padding(margem).fillMaxSize()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Filtro.entries.forEach { f ->
                    val qtd = when (f) {
                        Filtro.TODOS -> videos.size
                        Filtro.PUBLICOS -> videos.count { it.privacidade == "public" }
                        Filtro.NAO_LISTADOS -> videos.count { it.privacidade == "unlisted" }
                        Filtro.PRIVADOS -> videos.count { it.privacidade == "private" }
                    }
                    FilterChip(
                        selected = filtro == f,
                        onClick = { filtro = f },
                        label = { Text("${f.rotulo} ($qtd)") },
                    )
                }
            }
            when {
                carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                erro != null -> Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(erro ?: "", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { carregar() }) { Text("Tentar de novo") }
                }
                visiveis.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhum vídeo aqui.")
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(visiveis, key = { it.id }) { v -> ItemVideo(v) }
                }
            }
        }
    }
}

@Composable
fun ItemVideo(v: Video) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    fun abrirLink(url: String) = ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    val studio = "https://studio.youtube.com/video/${v.id}/edit"

    Row(
        Modifier.fillMaxWidth().clickable { abrirLink(studio) },
        verticalAlignment = Alignment.Top,
    ) {
        AsyncImage(
            model = v.miniatura,
            contentDescription = null,
            modifier = Modifier.width(132.dp).height(74.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                v.titulo,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            SeloPrivacidade(v)
            Spacer(Modifier.height(2.dp))
            Text(
                "${numero(v.views)} visualizações · ${numero(v.comentarios)} comentários · ${data(v.publicadoEm)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opções")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Editar no YouTube Studio") },
                    onClick = { menu = false; abrirLink(studio) },
                )
                DropdownMenuItem(
                    text = { Text("Assistir no YouTube") },
                    onClick = { menu = false; abrirLink("https://www.youtube.com/watch?v=${v.id}") },
                )
                DropdownMenuItem(
                    text = { Text("Compartilhar link") },
                    onClick = {
                        menu = false
                        val envio = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://youtu.be/${v.id}")
                        }
                        ctx.startActivity(Intent.createChooser(envio, "Compartilhar"))
                    },
                )
            }
        }
    }
}

@Composable
fun SeloPrivacidade(v: Video) {
    val (texto, cor) = when {
        v.privacidade == "private" && v.agendadoPara != null -> "Agendado · ${dataHora(v.agendadoPara)}" to Color(0xFF1E88E5)
        v.privacidade == "public" -> "Público" to Color(0xFF2E7D32)
        v.privacidade == "unlisted" -> "Não listado" to Color(0xFFEF6C00)
        v.privacidade == "private" -> "Privado" to Color(0xFF757575)
        else -> v.privacidade to Color(0xFF757575)
    }
    Surface(color = cor, shape = RoundedCornerShape(6.dp)) {
        Text(
            texto,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ---------- Utilidades ----------

fun numero(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1f mi", n / 1_000_000.0).replace('.', ',')
    n >= 1_000 -> String.format("%.1f mil", n / 1_000.0).replace('.', ',')
    else -> n.toString()
}

private val fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val fmtDataHora = DateTimeFormatter.ofPattern("dd/MM HH:mm")

fun data(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(fmtData)
} catch (e: Exception) {
    ""
}

fun dataHora(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(fmtDataHora)
} catch (e: Exception) {
    iso
}

/** Traduz erros técnicos para algo que dá para entender. */
fun mensagemErro(e: Exception): String = when (e) {
    is ApiException ->
        "O Google recusou o login (código ${e.statusCode}). Confira o passo 4 do guia (credencial Android com o SHA-1 certo)."
    is ErroApi -> when {
        e.codigo == 401 -> "O acesso expirou. Toque em atualizar para entrar de novo."
        e.codigo == 403 && (e.message ?: "").contains("quota", ignoreCase = true) ->
            "Limite diário do YouTube atingido. Volta a funcionar amanhã."
        e.codigo == 403 -> "O YouTube negou o acesso: ${e.message}"
        else -> e.message ?: "Erro ${e.codigo}"
    }
    is java.io.IOException -> "Sem conexão com a internet."
    else -> e.message ?: "Algo deu errado."
}
