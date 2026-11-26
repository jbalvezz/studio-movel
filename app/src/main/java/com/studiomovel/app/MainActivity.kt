@file:OptIn(ExperimentalMaterial3Api::class)

package com.studiomovel.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.lazy.itemsIndexed
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

// Cores tiradas do ícone: azul-ciano, roxo e vermelho do play.
val CorFundo1 = Color(0xFF0B0E22)
private val CorFundo2 = Color(0xFF16123A)
val CorCartao = Color(0xFF1D1A44).copy(alpha = 0.78f)

@Composable
fun Tema(conteudo: @Composable () -> Unit) {
    val cores = darkColorScheme(
        primary = Color(0xFF5FE3FF),
        onPrimary = Color(0xFF00222B),
        secondary = Color(0xFFB08CFF),
        tertiary = Color(0xFFFF4D5E),
        background = CorFundo1,
        surface = Color(0xFF1D1A44),
        surfaceVariant = Color(0xFF2A2660),
        onSurfaceVariant = Color(0xFFC4C2E8),
        primaryContainer = Color(0xFF0E5D73),
        onPrimaryContainer = Color(0xFFD5F6FF),
        secondaryContainer = Color(0xFF3B2C7A),
        onSecondaryContainer = Color(0xFFEDE5FF),
    )
    MaterialTheme(colorScheme = cores) {
        Box(Modifier.fillMaxSize().drawBehind { fundo() }) {
            // Marca d'água discreta com o ícone, no canto de baixo.
            Image(
                painter = painterResource(R.drawable.marca_dagua),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 40.dp).size(260.dp)
                    .offset(x = 70.dp).alpha(0.07f),
            )
            // Surface transparente: só para o texto ficar claro sobre o fundo escuro.
            Surface(Modifier.fillMaxSize(), color = Color.Transparent, contentColor = cores.onBackground) { conteudo() }
        }
    }
}

/** Degradê escuro com brilhos suaves nas cores do ícone. */
private fun DrawScope.fundo() {
    drawRect(Brush.verticalGradient(listOf(CorFundo1, CorFundo2, CorFundo1)))
    fun brilho(cor: Color, cx: Float, cy: Float, raio: Float) = drawCircle(
        brush = Brush.radialGradient(
            listOf(cor, Color.Transparent),
            center = Offset(size.width * cx, size.height * cy),
            radius = size.minDimension * raio,
        ),
        radius = size.minDimension * raio,
        center = Offset(size.width * cx, size.height * cy),
    )
    brilho(Color(0x447B4DFF), 0.05f, 0.08f, 0.9f)  // roxo
    brilho(Color(0x3322D3EE), 1.0f, 0.45f, 0.8f)   // ciano
    brilho(Color(0x22FF3B4E), 0.15f, 1.0f, 0.7f)   // vermelho
}

// ---------- Navegação ----------

@Composable
fun App(contas: Contas, aut: Autorizador) {
    var lista by remember { mutableStateOf(contas.listar()) }
    var abertoEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var tela by rememberSaveable { mutableStateOf("videos") }     // "videos" ou "caixa"
    var mensagemId by rememberSaveable { mutableStateOf<String?>(null) }
    var comentVideoId by rememberSaveable { mutableStateOf<String?>(null) }   // null = canal todo
    var comentTitulo by rememberSaveable { mutableStateOf<String?>(null) }
    val aberto = lista.firstOrNull { it.email == abertoEmail }

    when {
        aberto == null -> TelaCanais(
            lista = lista,
            contas = contas,
            aut = aut,
            aoMudar = { lista = contas.listar() },
            abrir = { abertoEmail = it.email; tela = "videos" },
            abrirEmail = { abertoEmail = it.email; tela = "caixa"; mensagemId = null },
        )
        tela == "caixa" && mensagemId != null -> TelaMensagem(
            conta = aberto, aut = aut, id = mensagemId!!, voltar = { mensagemId = null },
        )
        tela == "caixa" -> TelaCaixa(
            conta = aberto, aut = aut,
            voltar = { abertoEmail = null },
            abrir = { mensagemId = it },
        )
        tela == "comentarios" -> TelaComentarios(
            conta = aberto, aut = aut, videoId = comentVideoId, tituloVideo = comentTitulo,
            voltar = { tela = "videos" },
        )
        else -> TelaVideos(
            conta = aberto, aut = aut,
            voltar = { abertoEmail = null },
            abrirEmail = { tela = "caixa"; mensagemId = null },
            abrirComentarios = { v -> comentVideoId = v?.id; comentTitulo = v?.titulo; tela = "comentarios" },
        )
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
    abrirEmail: (ContaCanal) -> Unit,
) {
    val escopo = rememberCoroutineScope()
    val aviso = remember { SnackbarHostState() }
    var ocupado by remember { mutableStateOf(false) }
    var paraRemover by remember { mutableStateOf<ContaCanal?>(null) }
    var organizando by rememberSaveable { mutableStateOf(false) }
    var naoLidos by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // Conta os e-mails não lidos de cada canal (sem abrir janelas de permissão).
    LaunchedEffect(lista.map { it.email }) {
        for (c in lista) {
            try {
                val token = aut.token(c.email, silencioso = true) ?: continue
                naoLidos = naoLidos + (c.email to GmailApi.naoLidos(token))
            } catch (e: Exception) {
                // sem contador para essa conta
            }
        }
    }

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
                title = { Text(if (organizando) "Organizar canais" else "Meus canais") },
                colors = barraTransparente(),
                actions = {
                    if (lista.size > 1 || organizando) {
                        IconButton(onClick = { organizando = !organizando }) {
                            if (organizando) Icon(Icons.Default.Check, contentDescription = "Concluir")
                            else Icon(Icons.Default.Edit, contentDescription = "Organizar canais")
                        }
                    }
                    if (lista.isNotEmpty() && !organizando) {
                        IconButton(onClick = { atualizarTodos() }, enabled = !ocupado) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                        }
                    }
                },
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (!organizando) ExtendedFloatingActionButton(
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
                    if (organizando) item {
                        Text(
                            "Use as setas para mudar a ordem. Toque no ✓ lá em cima quando terminar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    itemsIndexed(lista, key = { _, c -> c.email }) { i, c ->
                        CartaoCanal(
                            c = c,
                            organizando = organizando,
                            abrir = { abrir(c) },
                            gmail = { abrirEmail(c) },
                            naoLidos = naoLidos[c.email] ?: 0,
                            subir = if (i > 0) ({ contas.mover(c.email, -1); aoMudar() }) else null,
                            descer = if (i < lista.lastIndex) ({ contas.mover(c.email, 1); aoMudar() }) else null,
                            remover = { paraRemover = c },
                        )
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
fun CartaoCanal(
    c: ContaCanal,
    organizando: Boolean,
    abrir: () -> Unit,
    gmail: () -> Unit,
    naoLidos: Int,
    subir: (() -> Unit)?,
    descer: (() -> Unit)?,
    remover: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().clickable(enabled = !organizando, onClick = abrir),
        colors = CardDefaults.cardColors(containerColor = CorCartao),
    ) {
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
            if (organizando) {
                IconButton(onClick = { subir?.invoke() }, enabled = subir != null) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir")
                }
                IconButton(onClick = { descer?.invoke() }, enabled = descer != null) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer")
                }
                IconButton(onClick = remover) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover canal")
                }
            } else {
                IconButton(onClick = gmail) {
                    BadgedBox(badge = {
                        if (naoLidos > 0) Badge { Text(if (naoLidos > 99) "99+" else naoLidos.toString()) }
                    }) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Abrir Gmail desta conta ($naoLidos não lidos)",
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun barraTransparente() = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
)

// ---------- Tela 2: vídeos de um canal ----------

private enum class Filtro(val rotulo: String) {
    TODOS("Todos"), PUBLICOS("Públicos"), NAO_LISTADOS("Não listados"), PRIVADOS("Privados")
}

@Composable
fun TelaVideos(
    conta: ContaCanal,
    aut: Autorizador,
    voltar: () -> Unit,
    abrirEmail: () -> Unit,
    abrirComentarios: (Video?) -> Unit,
) {
    BackHandler(onBack = voltar)
    val escopo = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var filtro by rememberSaveable { mutableStateOf(Filtro.TODOS) }
    var mudando by remember { mutableStateOf<Video?>(null) }
    var editando by remember { mutableStateOf<Video?>(null) }
    var salvando by remember { mutableStateOf(false) }
    val aviso = remember { SnackbarHostState() }
    var miniaturaDe by remember { mutableStateOf<Video?>(null) }
    var enviandoMiniatura by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val escolherImagem = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val v = miniaturaDe
        miniaturaDe = null
        if (uri == null || v == null) return@rememberLauncherForActivityResult
        escopo.launch {
            enviandoMiniatura = true
            try {
                val jpeg = prepararMiniatura(ctx, uri)
                val token = aut.token(conta.email) ?: return@launch
                YouTubeApi.trocarMiniatura(token, v.id, jpeg)
                // muda o endereço para a imagem nova aparecer (senão fica a antiga guardada)
                videos = videos.map {
                    if (it.id == v.id) it.copy(miniatura = it.miniatura.substringBefore("?") + "?t=" + System.currentTimeMillis()) else it
                }
                aviso.showSnackbar("Miniatura trocada! Pode levar alguns minutos para aparecer em todo lugar.")
            } catch (e: Exception) {
                aviso.showSnackbar(mensagemErro(e))
            } finally {
                enviandoMiniatura = false
            }
        }
    }

    fun mudarVisibilidade(v: Video, privacidade: String, agendarPara: String?) {
        escopo.launch {
            salvando = true
            try {
                val token = aut.token(conta.email) ?: return@launch
                YouTubeApi.mudarVisibilidade(token, v.id, privacidade, agendarPara)
                videos = videos.map {
                    if (it.id == v.id) it.copy(privacidade = privacidade, agendadoPara = agendarPara) else it
                }
                mudando = null
                aviso.showSnackbar(
                    when {
                        agendarPara != null -> "Agendado para ${dataHora(agendarPara)}."
                        privacidade == "public" -> "Agora o vídeo está público!"
                        privacidade == "unlisted" -> "Agora o vídeo está não listado."
                        else -> "Agora o vídeo está privado."
                    }
                )
            } catch (e: Exception) {
                aviso.showSnackbar(mensagemErro(e))
            } finally {
                salvando = false
            }
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
                colors = barraTransparente(),
                navigationIcon = {
                    IconButton(onClick = voltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { abrirComentarios(null) }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Comentários recentes do canal")
                    }
                    IconButton(onClick = abrirEmail) {
                        Icon(Icons.Default.Email, contentDescription = "Gmail", tint = MaterialTheme.colorScheme.tertiary)
                    }
                    IconButton(onClick = { carregar() }, enabled = !carregando) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                },
            )
        },
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(aviso) },
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
            if (enviandoMiniatura) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
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
                    items(visiveis, key = { it.id }) { v ->
                        ItemVideo(
                            v,
                            mudarVisibilidade = { mudando = v },
                            comentarios = { abrirComentarios(v) },
                            editar = { editando = v },
                            trocarMiniatura = {
                                miniaturaDe = v
                                escolherImagem.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        )
                    }
                }
            }
        }
    }

    editando?.let { v ->
        DialogoEditar(
            v = v,
            conta = conta,
            aut = aut,
            fechar = { editando = null },
            salvo = { novoTitulo ->
                videos = videos.map { if (it.id == v.id) it.copy(titulo = novoTitulo) else it }
                editando = null
                escopo.launch { aviso.showSnackbar("Título e descrição salvos!") }
            },
        )
    }

    mudando?.let { v ->
        DialogoVisibilidade(
            v = v,
            salvando = salvando,
            confirmar = { p, quando -> mudarVisibilidade(v, p, quando) },
            cancelar = { mudando = null },
        )
    }
}

@Composable
fun ItemVideo(
    v: Video,
    mudarVisibilidade: () -> Unit,
    comentarios: () -> Unit,
    editar: () -> Unit,
    trocarMiniatura: () -> Unit,
) {
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
            Box(Modifier.clickable(onClick = mudarVisibilidade)) { SeloPrivacidade(v) }
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
                    text = { Text("Editar título, descrição e tags") },
                    onClick = { menu = false; editar() },
                )
                DropdownMenuItem(
                    text = { Text("Trocar miniatura") },
                    onClick = { menu = false; trocarMiniatura() },
                )
                DropdownMenuItem(
                    text = { Text("Mudar visibilidade") },
                    onClick = { menu = false; mudarVisibilidade() },
                )
                DropdownMenuItem(
                    text = { Text("Comentários (${numero(v.comentarios)})") },
                    onClick = { menu = false; comentarios() },
                )
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
        e.codigo == 403 && (e.message ?: "").contains("insufficient", ignoreCase = true) ->
            "Esta conta não deu a permissão do Gmail. Toque no lápis na tela inicial, remova o canal e adicione de novo, marcando todas as permissões."
        e.codigo == 403 && (e.message ?: "").contains("has not been used", ignoreCase = true) ->
            "A API não está ativada no Google Cloud (passo 2 do guia)."
        (e.message ?: "").contains("disabled comments", ignoreCase = true) ||
            (e.message ?: "").contains("commentsDisabled", ignoreCase = true) ->
            "Os comentários estão desativados neste vídeo."
        (e.message ?: "").contains("publishAt", ignoreCase = true) ||
            (e.message ?: "").contains("scheduled", ignoreCase = true) ->
            "Não deu para agendar: só funciona em vídeos privados que ainda não foram publicados."
        (e.message ?: "").contains("thumbnail", ignoreCase = true) ->
            "O YouTube não deixou trocar a miniatura. O canal precisa estar verificado por telefone (youtube.com/verify)."
        e.codigo == 403 -> "O Google negou o acesso: ${e.message}"
        else -> e.message ?: "Erro ${e.codigo}"
    }
    is java.io.IOException -> "Sem conexão com a internet."
    else -> e.message ?: "Algo deu errado."
}

/** Lê a imagem escolhida, reduz para no máximo 1280 px de largura e salva em JPEG com menos de 2 MB. */
suspend fun prepararMiniatura(ctx: android.content.Context, uri: Uri): ByteArray =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val limites = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, limites) }
        var amostra = 1
        while (limites.outWidth / (amostra * 2) >= 1280) amostra *= 2
        val opcoes = android.graphics.BitmapFactory.Options().apply { inSampleSize = amostra }
        val original = ctx.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, opcoes)
        } ?: throw ErroApi(0, "Não consegui abrir essa imagem.")
        val imagem = if (original.width > 1280) {
            val altura = (original.height * 1280.0 / original.width).toInt()
            android.graphics.Bitmap.createScaledBitmap(original, 1280, altura, true)
        } else original
        var qualidade = 92
        var bytes: ByteArray
        do {
            val saida = java.io.ByteArrayOutputStream()
            imagem.compress(android.graphics.Bitmap.CompressFormat.JPEG, qualidade, saida)
            bytes = saida.toByteArray()
            qualidade -= 10
        } while (bytes.size > 2_000_000 && qualidade > 20)
        bytes
    }
