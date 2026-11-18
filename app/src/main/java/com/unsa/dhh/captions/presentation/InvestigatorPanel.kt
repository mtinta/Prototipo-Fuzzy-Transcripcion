package com.unsa.dhh.captions.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unsa.dhh.captions.domain.asr.AsrEngineType
import com.unsa.dhh.captions.presentation.viewmodel.CaptionsViewModel

/**
 * Panel oculto del investigador (pulsación larga sobre el título de la pantalla).
 *
 * - Código del participante y etiqueta de la sesión (se guardan en el archivo de evaluación).
 * - Interruptor de condición experimental: T2 (corrección difusa visible) o T0 (texto crudo del ASR).
 *   En ambas condiciones el archivo registra el texto crudo Y el corregido.
 * - Motor de reconocimiento: Vosk (predeterminado, C por palabra) o Android (C fija 0.65), para comparar.
 * - Grabación del audio del docente (WAV junto al CSV; solo con Vosk), para la transcripción de referencia.
 * - BLOQUEAR PARA EL ALUMNO: horizontal fija, app fijada, pantalla encendida; el alumno solo desplaza el texto.
 * - Ruta del archivo en curso y comando para descargarlo.
 *
 * Cualquier cambio abre una sesión nueva (pantalla limpia + archivo nuevo).
 */
@Composable
fun InvestigatorPanel(viewModel: CaptionsViewModel, onDismiss: () -> Unit) {
    val correctionEnabled by viewModel.correctionEnabled.collectAsState()
    val currentParticipant by viewModel.participantCode.collectAsState()
    val currentLabel by viewModel.sessionLabel.collectAsState()
    val filePath by viewModel.evaluationFilePath.collectAsState()
    val currentEngine by viewModel.asrEngineType.collectAsState()
    val recordingStatus by viewModel.audioRecordingStatus.collectAsState()

    var participant by remember { mutableStateOf(currentParticipant) }
    var label by remember { mutableStateOf(currentLabel) }
    var correction by remember { mutableStateOf(correctionEnabled) }
    var engine by remember { mutableStateOf(currentEngine) }
    var recordAudio by remember { mutableStateOf(recordingStatus.enabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Panel del investigador", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Primero: en horizontal el panel deja muy poco alto y el botón quedaba escondido al fondo
                val pendingChanges = participant.trim() != currentParticipant || label.trim() != currentLabel ||
                    correction != correctionEnabled || engine != currentEngine || recordAudio != recordingStatus.enabled
                Button(
                    onClick = {
                        viewModel.lockForStudent()
                        onDismiss()
                    },
                    enabled = !pendingChanges,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("BLOQUEAR PARA EL ALUMNO") }
                Text(
                    if (pendingChanges) "Primero APLICAR los cambios (o deshacerlos)."
                    else "Horizontal fija, sin salir de la app y pantalla siempre encendida; el alumno solo desplaza " +
                        "el texto. Inicia el micrófono si está detenido. Acepte \"Fijar\" en el aviso del sistema. " +
                        "Para desbloquear: pulsación larga en la esquina superior derecha y PIN.",
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = participant,
                    onValueChange = { participant = it.take(12) },
                    label = { Text("Código del participante (E01, D01…)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(24) },
                    label = { Text("Etiqueta (piloto, clase1, dev…)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mostrar corrección difusa", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (correction) "Condición T2: se muestra el texto corregido"
                            else "Condición T0: se muestra el texto crudo del ASR",
                            fontSize = 12.sp
                        )
                    }
                    Switch(checked = correction, onCheckedChange = { correction = it })
                }
                Spacer(Modifier.height(12.dp))
                Text("Motor de reconocimiento", fontWeight = FontWeight.SemiBold)
                Column(modifier = Modifier.selectableGroup()) {
                    AsrEngineType.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = engine == option, onClick = { engine = option }, role = Role.RadioButton)
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = engine == option, onClick = null)
                            Text(option.displayName, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Grabar audio del docente", fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                !recordAudio -> "No se graba audio"
                                engine != AsrEngineType.VOSK -> "CON EL MOTOR ANDROID NO SE GRABA AUDIO"
                                else -> "WAV 16 kHz mientras el micrófono está activo (~115 MB por hora)"
                            },
                            fontSize = 12.sp,
                            color = if (recordAudio && engine != AsrEngineType.VOSK) androidx.compose.ui.graphics.Color(0xFFFF8A80)
                            else androidx.compose.ui.graphics.Color.Unspecified,
                            fontWeight = if (recordAudio && engine != AsrEngineType.VOSK) FontWeight.Bold else null
                        )
                    }
                    Switch(checked = recordAudio, onCheckedChange = { recordAudio = it })
                }
                recordingStatus.activeFile?.let { Text("Grabando: $it (en la carpeta de la sesión)", fontSize = 11.sp) }
                recordingStatus.error?.let {
                    Text(it, fontSize = 11.sp, color = androidx.compose.ui.graphics.Color(0xFFFF8A80))
                }
                Spacer(Modifier.height(12.dp))
                Text("Archivo en curso:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(filePath ?: "—", fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Descargar: adb pull /sdcard/Android/data/com.unsa.dhh.captions/files/evaluacion/ .",
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Aplicar cualquier cambio limpia la pantalla e inicia un archivo nuevo.",
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val identityChanged = participant.trim() != currentParticipant || label.trim() != currentLabel
                val engineChanged = engine != currentEngine
                // Antes de abrir la sesión nueva, para que su primer tramo de audio ya siga la opción elegida
                if (recordAudio != recordingStatus.enabled) viewModel.setAudioRecordingEnabled(recordAudio)
                if (engineChanged) viewModel.setAsrEngine(engine)
                if (identityChanged) viewModel.setSessionIdentity(participant, label)
                if (correction != correctionEnabled) viewModel.setCorrectionEnabled(correction)
                if (!engineChanged && !identityChanged && correction == correctionEnabled) viewModel.clearTranscription()
                onDismiss()
            }) { Text("APLICAR E INICIAR SESIÓN") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CERRAR") }
        }
    )
}
