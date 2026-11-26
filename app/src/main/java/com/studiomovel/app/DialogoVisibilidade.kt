@file:OptIn(ExperimentalMaterial3Api::class)

package com.studiomovel.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val opcoes = listOf(
    "public" to "Público",
    "unlisted" to "Não listado",
    "private" to "Privado",
    "agendar" to "Agendar publicação",
)

/**
 * Janela para mudar a visibilidade de um vídeo.
 * confirmar(privacidade, agendarPara) — agendarPara vem no formato ISO (UTC) quando for agendamento.
 */
@Composable
fun DialogoVisibilidade(
    v: Video,
    salvando: Boolean,
    confirmar: (String, String?) -> Unit,
    cancelar: () -> Unit,
) {
    val inicial = if (v.privacidade == "private" && v.agendadoPara != null) "agendar" else v.privacidade
    var escolha by remember { mutableStateOf(inicial) }
    // Data/hora do agendamento (padrão: amanhã às 12:00, ou o agendamento que já existe).
    var quando by remember {
        mutableStateOf(
            v.agendadoPara?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }.getOrNull() }
                ?: LocalDate.now().plusDays(1).atTime(12, 0)
        )
    }
    var escolhendoData by remember { mutableStateOf(false) }
    var escolhendoHora by remember { mutableStateOf(false) }
    val noFuturo = quando.isAfter(LocalDateTime.now().plusMinutes(5))

    AlertDialog(
        onDismissRequest = { if (!salvando) cancelar() },
        title = { Text("Visibilidade") },
        text = {
            Column {
                Text(v.titulo, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                opcoes.forEach { (valor, rotulo) ->
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = !salvando) { escolha = valor },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = escolha == valor, onClick = { escolha = valor }, enabled = !salvando)
                        Text(rotulo)
                    }
                }
                if (escolha == "agendar") {
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.padding(start = 12.dp)) {
                        OutlinedButton(onClick = { escolhendoData = true }) {
                            Text(quando.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        }
                        Spacer(Modifier.padding(4.dp))
                        OutlinedButton(onClick = { escolhendoHora = true }) {
                            Text(quando.format(DateTimeFormatter.ofPattern("HH:mm")))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (noFuturo) "O vídeo fica privado e vira público sozinho nessa hora. Só funciona em vídeos que ainda não foram publicados."
                        else "Escolha uma data e hora no futuro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (noFuturo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !salvando && (escolha != "agendar" || noFuturo),
                onClick = {
                    if (escolha == "agendar") {
                        val iso = quando.atZone(ZoneId.systemDefault()).toInstant()
                            .truncatedTo(ChronoUnit.SECONDS).toString()
                        confirmar("private", iso)
                    } else {
                        confirmar(escolha, null)
                    }
                },
            ) { Text(if (salvando) "Salvando..." else "Salvar") }
        },
        dismissButton = { TextButton(onClick = cancelar, enabled = !salvando) { Text("Cancelar") } },
    )

    if (escolhendoData) {
        val hojeUtc = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = quando.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= hojeUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { escolhendoData = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        val dia = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        quando = LocalDateTime.of(dia, quando.toLocalTime())
                    }
                    escolhendoData = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { escolhendoData = false }) { Text("Cancelar") } },
        ) { DatePicker(estado) }
    }

    if (escolhendoHora) {
        val estado = rememberTimePickerState(initialHour = quando.hour, initialMinute = quando.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { escolhendoHora = false },
            title = { Text("Horário") },
            text = { TimePicker(estado) },
            confirmButton = {
                TextButton(onClick = {
                    quando = LocalDateTime.of(quando.toLocalDate(), LocalTime.of(estado.hour, estado.minute))
                    escolhendoHora = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { escolhendoHora = false }) { Text("Cancelar") } },
        )
    }
}
