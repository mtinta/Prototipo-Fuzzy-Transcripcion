package com.unsa.dhh.captions.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unsa.dhh.captions.domain.asr.RawTranscriptionResult
import com.unsa.dhh.captions.presentation.theme.DarkSurface
import com.unsa.dhh.captions.presentation.theme.HighContrastYellow
import com.unsa.dhh.captions.presentation.theme.PureBlack
import com.unsa.dhh.captions.presentation.theme.getResponsiveTeleprompterStyle
import com.unsa.dhh.captions.presentation.viewmodel.CaptionsViewModel
import com.unsa.dhh.captions.service.AudioInputSource

/**
 * Módulo 5: Interfaz Accesible en Jetpack Compose (WCAG AAA).
 * Proyecta la transcripción continua en pantalla de alto contraste (19.56:1, #FFFF00 sobre #000000).
 * En modo horizontal (landscape), oculta todos los menús para dar prioridad 100% al texto.
 *
 * Modo alumno (panel del investigador → BLOQUEAR PARA EL ALUMNO): siempre la vista horizontal de solo texto;
 * el alumno solo puede desplazarlo. Atrás no hace nada. Para salir: pulsación larga en la esquina superior
 * derecha + PIN del investigador. La orientación, la fijación y la pantalla encendida las aplica MainActivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleprompterScreen(viewModel: CaptionsViewModel) {
    val configuration = LocalConfiguration.current
    // Teléfono o tableta (no cambia al girar el equipo): ver DeviceClass
    val isSmartphone = DeviceClass.isCompact(configuration)
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val textScale by viewModel.textScale.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val teleprompterLines by viewModel.teleprompterLines.collectAsState()
    val sessionStats by viewModel.sessionStats.collectAsState()
    val exportMsg by viewModel.lastExportMessage.collectAsState()
    val asrStatusMsg by viewModel.asrStatusMessage.collectAsState()
    val asrModeNotice by viewModel.asrModeNotice.collectAsState()
    val audioInputSource by viewModel.audioInputSource.collectAsState()
    val isExternalMicConnected by viewModel.isExternalMicConnected.collectAsState()
    val audioRecording by viewModel.audioRecordingStatus.collectAsState()
    val asrEngine by viewModel.asrEngineType.collectAsState()
    val studentLocked by viewModel.studentLock.collectAsState()

    // En modo alumno, Atrás no cierra la app ni nada de la pantalla
    BackHandler(enabled = studentLocked) {}

    // Grabación del docente: avisos que no pueden pasar desapercibidos (no hay equipo de respaldo)
    val recordingWarning = when {
        audioRecording.error != null -> "⚠ ${audioRecording.error}"
        isListening && audioRecording.enabled && asrEngine != com.unsa.dhh.captions.domain.asr.AsrEngineType.VOSK ->
            "⚠ Con el motor Android no se graba el audio de la clase."
        else -> null
    }

    // TICKET-MIC-03: aviso breve al conectar/desconectar el receptor (se omite la emisión inicial)
    val screenContext = androidx.compose.ui.platform.LocalContext.current
    var previousSource by remember { mutableStateOf<AudioInputSource?>(null) }
    LaunchedEffect(audioInputSource) {
        val before = previousSource
        previousSource = audioInputSource
        if (before != null && before != audioInputSource) {
            val msg = when {
                audioInputSource == AudioInputSource.WIRELESS_USB_MIC ->
                    "Micrófono de solapa inalámbrico SX26/Maxtron conectado"
                before == AudioInputSource.WIRELESS_USB_MIC ->
                    "Dongle USB desconectado. Usando micrófono integrado"
                else -> "Fuente de audio: ${audioInputSource.displayName}"
            }
            android.widget.Toast.makeText(screenContext, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    var showSusDialog by remember { mutableStateOf(false) }
    var showTlxDialog by remember { mutableStateOf(false) }
    var showCorrectionsDialog by remember { mutableStateOf(false) }
    var showInvestigatorPanel by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }

    if (showUnlockDialog) {
        UnlockDialog(
            onUnlock = { pin ->
                val ok = viewModel.unlockStudent(pin)
                if (ok) {
                    showUnlockDialog = false
                    showInvestigatorPanel = true
                }
                ok
            },
            onDismiss = { showUnlockDialog = false }
        )
    }

    if (showInvestigatorPanel) {
        InvestigatorPanel(viewModel = viewModel, onDismiss = { showInvestigatorPanel = false })
    }

    val listState = rememberLazyListState()
    val baseTextStyle = getResponsiveTeleprompterStyle(isSmartphone)
    // Tamaño de letra ajustable por el estudiante (A- / A+), aplicado sobre el tamaño base del dispositivo
    val responsiveTextStyle = baseTextStyle.copy(
        fontSize = baseTextStyle.fontSize * textScale,
        lineHeight = baseTextStyle.lineHeight * textScale
    )

    // Scroll suave automático al final cuando llega una nueva línea, SALVO que el alumno haya subido a releer:
    // antes cada frase nueva lo devolvía al final y no podía leer lo anterior mientras el docente hablaba.
    LaunchedEffect(teleprompterLines.size) {
        if (teleprompterLines.isNotEmpty()) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val wasNearEnd = lastVisible < 0 || lastVisible >= teleprompterLines.size - 3
            if (wasNearEnd) listState.animateScrollToItem(teleprompterLines.size - 1)
        }
    }

    if (showSusDialog) {
        Box(modifier = Modifier.fillMaxSize()) {
            SUSQuestionnaireScreen(
                onDismiss = { showSusDialog = false },
                onSubmit = { responses, complementary ->
                    viewModel.saveSusResponses(responses, complementary)
                }
            )
        }
        return
    }

    if (showTlxDialog) {
        Box(modifier = Modifier.fillMaxSize()) {
            NasaTlxScreen(
                onDismiss = { showTlxDialog = false },
                onSubmit = { values -> viewModel.saveTlxResponses(values) }
            )
        }
        return
    }

    if (showCorrectionsDialog) {
        Box(modifier = Modifier.fillMaxSize()) {
            CorrectionsHistoryScreen(
                viewModel = viewModel,
                onDismiss = { showCorrectionsDialog = false }
            )
        }
        return
    }

    if (isLandscape || studentLocked) {
        // ================================================================
        // MODO HORIZONTAL (LANDSCAPE): 100% PANTALLA COMPLETA DE TEXTO
        // ================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
                .padding(horizontal = if (isSmartphone) 16.dp else 28.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopStart
        ) {
            if (teleprompterLines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Coloque el dispositivo sobre la carpeta escolar.\nEl dictado del docente se presentará aquí en tiempo real.",
                        style = responsiveTextStyle.copy(
                            fontSize = if (isSmartphone) 20.sp else 26.sp,
                            lineHeight = if (isSmartphone) 30.sp else 38.sp,
                            color = Color(0xFF555555),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top
                ) {
                    items(teleprompterLines) { line ->
                        Text(
                            text = line,
                            // Mismo tamaño base que en vertical (22 sp teléfono / 30 sp tableta) × escala del estudiante
                            style = responsiveTextStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )
                    }
                }
            }

            // Avisos para el investigador (sin botones: el alumno no puede tocar nada)
            (recordingWarning ?: asrStatusMsg)?.let { warning ->
                Text(
                    text = warning,
                    color = Color(0xFFFFD180),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color(0xE04E2A00))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // La vista horizontal SIN bloquear es idéntica a la bloqueada: sin este aviso parecía bloqueada
            // y el alumno podía salir con Atrás o girar a vertical.
            if (!studentLocked) {
                Text(
                    text = "SIN BLOQUEAR",
                    color = Color(0xFFFF8A80),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(Color(0xCC3A0000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Acceso oculto del investigador: pulsación larga en la esquina superior derecha.
            // Bloqueado → pide el PIN; sin bloquear → abre el panel (en horizontal no hay título).
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(56.dp)
                    .pointerInput(studentLocked) {
                        detectTapGestures(onLongPress = {
                            if (studentLocked) showUnlockDialog = true else showInvestigatorPanel = true
                        })
                    }
            )
        }
    } else {
        // ================================================================
        // MODO VERTICAL (PORTRAIT): INTERFAZ CON MENÚS Y CONTROLES
        // ================================================================
        Scaffold(
            containerColor = PureBlack,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Prueba",
                            color = HighContrastYellow,
                            fontWeight = FontWeight.Black,
                            fontSize = if (isSmartphone) 18.sp else 22.sp,
                            // Acceso oculto al panel del investigador: pulsación larga sobre el título
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(onLongPress = { showInvestigatorPanel = true })
                            }
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.exportNotesAsTxt() }) {
                            Icon(Icons.Default.Description, contentDescription = "Exportar TXT", tint = HighContrastYellow)
                        }
                        IconButton(onClick = { viewModel.exportNotesAsPdf() }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF", tint = HighContrastYellow)
                        }
                        IconButton(onClick = { showCorrectionsDialog = true }) {
                            Icon(Icons.Default.Spellcheck, contentDescription = "Historial de Correcciones", tint = HighContrastYellow)
                        }
                        IconButton(onClick = { showTlxDialog = true }) {
                            Icon(Icons.Default.Psychology, contentDescription = "Cuestionario de esfuerzo (NASA-TLX)", tint = HighContrastYellow)
                        }
                        IconButton(onClick = { showSusDialog = true }) {
                            Icon(Icons.Default.RateReview, contentDescription = "Cuestionario SUS", tint = HighContrastYellow)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PureBlack)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = if (isSmartphone) 12.dp else 20.dp, vertical = 6.dp)
            ) {
                // Área curricular fija (la investigación se limita a Ciencia y Tecnología) y tamaño de letra
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(HighContrastYellow)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = CaptionsViewModel.SUBJECT_DISPLAY_NAME,
                            color = PureBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isSmartphone) 11.sp else 13.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextSizeButton(label = "A−", description = "Reducir letra", onClick = { viewModel.decreaseTextSize() })
                        Text(
                            text = "${(textScale * 100).toInt()}%",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        TextSizeButton(label = "A+", description = "Aumentar letra", onClick = { viewModel.increaseTextSize() })
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Indicador de Fuente de Audio (SX26/Maxtron 2.4G vs Micrófono Interno)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isExternalMicConnected) Color(0xFF00332C) else Color(0xFF1E1E1E))
                        .border(
                            width = 1.dp,
                            color = if (isExternalMicConnected) Color(0xFF00E676) else Color.DarkGray,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isExternalMicConnected) Color(0xFF00E676) else HighContrastYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = audioInputSource.shortName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isExternalMicConnected) Color(0xFFE0F2F1) else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = if (isExternalMicConnected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Text(
                        text = when (audioInputSource) {
                            AudioInputSource.WIRELESS_USB_MIC -> "2.4 GHz RF"
                            AudioInputSource.WIRED_HEADSET_MIC -> "ALÁMBRICO"
                            else -> "INTEGRADO"
                        },
                        maxLines = 1,
                        color = if (isExternalMicConnected) Color(0xFF00E676) else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Panel de Control y Telemetría
                if (isSmartphone) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.toggleListening() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListening) Color(0xFFD32F2F) else HighContrastYellow
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isListening) Color.White else PureBlack
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isListening) "DETENER MICRÓFONO" else "INICIAR MICRÓFONO",
                                    color = if (isListening) Color.White else PureBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Lat: ${sessionStats.averageLatencyMs}ms",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Corr: ${sessionStats.totalSubstitutions}",
                                    color = HighContrastYellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "S: ${(sessionStats.averageSemanticScore * 100).toInt()}%",
                                    color = Color(0xFF64B5F6),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { viewModel.clearTranscription() }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Limpiar", tint = Color.Gray)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.toggleListening() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) Color(0xFFD32F2F) else HighContrastYellow
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isListening) Color.White else PureBlack
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isListening) "DETENER MICRÓFONO" else "INICIAR MICRÓFONO",
                                color = if (isListening) Color.White else PureBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Latencia: ${sessionStats.averageLatencyMs} ms",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Correcciones: ${sessionStats.totalSubstitutions}",
                                color = HighContrastYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Coherencia Sem. media (S): ${(sessionStats.averageSemanticScore * 100).toInt()}%",
                                color = Color(0xFF64B5F6),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { viewModel.clearTranscription() }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Limpiar", tint = Color.Gray)
                            }
                        }
                    }
                }

                recordingWarning?.let { warning ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4E2A00))
                    ) {
                        Text(warning, color = Color(0xFFFFD180), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp))
                    }
                }

                // Aviso de fallo total del reconocimiento (los cambios automáticos de modo no se muestran)
                asrModeNotice?.let { notice ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2B33))
                    ) {
                        Text(notice, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                    }
                }

                // Notificación de estado ASR
                asrStatusMsg?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2723))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(msg, color = Color.White, fontSize = 12.sp)
                            Text(
                                "REINTENTAR",
                                color = HighContrastYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable {
                                    viewModel.retryListening()
                                }
                            )
                        }
                    }
                }

                // Notificación de exportación
                exportMsg?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B381A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(msg, color = Color.White, fontSize = 12.sp)
                            Text(
                                "OK",
                                color = HighContrastYellow,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { viewModel.clearExportMessage() }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ================================================================
                // ÁREA PRINCIPAL: PANTALLA DE TRANSCRIPCIÓN CONTINUA Y SCROLLABLE
                // ================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PureBlack)
                        .border(1.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
                        .padding(if (isSmartphone) 12.dp else 20.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (teleprompterLines.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Coloque el dispositivo sobre la carpeta escolar.\nEl dictado del docente se presentará aquí en tiempo real.",
                                style = responsiveTextStyle.copy(
                                    fontSize = if (isSmartphone) 18.sp else 24.sp,
                                    lineHeight = if (isSmartphone) 26.sp else 36.sp,
                                    color = Color(0xFF555555),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Top
                        ) {
                            items(teleprompterLines) { line ->
                                Text(
                                    text = line,
                                    style = responsiveTextStyle,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = if (isSmartphone) 4.dp else 8.dp)
                                )
                            }
                        }
                    }
                }

                // Pie de pantalla adaptable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isListening) "● Micrófono activo (Escuchando)" else "○ Micrófono pausado",
                        color = if (isListening) Color(0xFF00E676) else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    // Grabación del docente: visible siempre que se esté escribiendo un WAV
                    if (audioRecording.activeFile != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "● Grabando audio",
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/** PIN del investigador para salir del modo alumno. */
@Composable
private fun UnlockDialog(onUnlock: (String) -> Boolean, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val submit = { if (!onUnlock(pin)) { wrong = true; pin = "" } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Desbloquear (investigador)", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8); wrong = false },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    // En horizontal el teclado tapa el botón: la tecla ✓ del teclado también desbloquea
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() })
                )
                if (wrong) Text("PIN incorrecto", color = Color(0xFFFF8A80), fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }) { Text("DESBLOQUEAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

@Composable
private fun TextSizeButton(label: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurface)
            .border(1.dp, HighContrastYellow, RoundedCornerShape(6.dp))
            .clickable(onClickLabel = description) { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = HighContrastYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
