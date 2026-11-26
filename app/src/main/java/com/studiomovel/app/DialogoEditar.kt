package com.studiomovel.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val MAX_TITULO = 100
private const val MAX_DESCRICAO = 5000
private const val MAX_TAGS = 500

/** Janela para editar o título e a descrição de um vídeo. */
@Composable
fun DialogoEditar(
    v: Video,
    conta: ContaCanal,
    aut: Autorizador,
    fechar: () -> Unit,
    salvo: (String) -> Unit,
) {
    val escopo = rememberCoroutineScope()
    var titulo by remember { mutableStateOf(v.titulo) }
    var descricao by remember { mutableStateOf<String?>(null) } // null = carregando
    var tagsTexto by remember { mutableStateOf("") }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(v.id) {
        try {
            val token = aut.token(conta.email) ?: run { erro = "Permissão não concedida."; return@LaunchedEffect }
            val (t, d, tags) = YouTubeApi.textos(token, v.id)
            titulo = t
            descricao = d
            tagsTexto = tags.joinToString(", ")
        } catch (e: Exception) {
            erro = mensagemErro(e)
        }
    }

    // O YouTube não aceita os sinais < e > no título nem na descrição.
    val temSinais = titulo.contains('<') || titulo.contains('>') ||
        (descricao ?: "").contains('<') || (descricao ?: "").contains('>')
    val bytesDescricao = (descricao ?: "").toByteArray(Charsets.UTF_8).size
    val tags = tagsTexto.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
    // O YouTube conta tags com espaço como se tivessem aspas (+2).
    val tamanhoTags = tags.sumOf { it.length + (if (it.contains(' ')) 2 else 0) } + (tags.size - 1).coerceAtLeast(0)
    val valido = titulo.isNotBlank() && titulo.length <= MAX_TITULO &&
        bytesDescricao <= MAX_DESCRICAO && !temSinais && descricao != null && tamanhoTags <= MAX_TAGS

    AlertDialog(
        onDismissRequest = { if (!salvando) fechar() },
        title = { Text("Editar título, descrição e tags") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when {
                    erro != null && descricao == null -> Text(erro ?: "")
                    descricao == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> {
                        OutlinedTextField(
                            value = titulo,
                            onValueChange = { titulo = it.replace("\n", " ") },
                            label = { Text("Título") },
                            supportingText = { Text("${titulo.length}/$MAX_TITULO") },
                            isError = titulo.isBlank() || titulo.length > MAX_TITULO,
                            enabled = !salvando,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = descricao ?: "",
                            onValueChange = { descricao = it },
                            label = { Text("Descrição") },
                            supportingText = { Text("$bytesDescricao/$MAX_DESCRICAO") },
                            isError = bytesDescricao > MAX_DESCRICAO,
                            enabled = !salvando,
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tagsTexto,
                            onValueChange = { tagsTexto = it.replace("\n", " ") },
                            label = { Text("Tags (separe com vírgula)") },
                            supportingText = { Text("${tags.size} tags · $tamanhoTags/$MAX_TAGS") },
                            isError = tamanhoTags > MAX_TAGS,
                            enabled = !salvando,
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (temSinais) Text(
                            "O YouTube não aceita os sinais < e >. Tire-os para salvar.",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (erro != null) Text(
                            erro ?: "",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido && !salvando,
                onClick = {
                    escopo.launch {
                        salvando = true
                        erro = null
                        try {
                            val token = aut.token(conta.email) ?: return@launch
                            val t = titulo.trim()
                            YouTubeApi.editarTextos(token, v.id, t, descricao ?: "", tags)
                            salvo(t)
                        } catch (e: Exception) {
                            erro = mensagemErro(e)
                        } finally {
                            salvando = false
                        }
                    }
                },
            ) { Text(if (salvando) "Salvando..." else "Salvar") }
        },
        dismissButton = { TextButton(onClick = fechar, enabled = !salvando) { Text("Cancelar") } },
    )
}
