package com.unsa.dhh.captions.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unsa.dhh.captions.domain.asr.RawTranscriptionResult
import com.unsa.dhh.captions.presentation.theme.DarkSurface
import com.unsa.dhh.captions.presentation.theme.HighContrastYellow
import com.unsa.dhh.captions.presentation.theme.PureBlack
import com.unsa.dhh.captions.presentation.theme.getResponsiveTeleprompterStyle
import com.unsa.dhh.captions.presentation.viewmodel.CaptionsViewModel

/**
 * Módulo 5: Interfaz Accesible en Jetpack Compose (WCAG AAA).
 * Proyecta la transcripción continua en pantalla de alto contraste (21:1, #FFFF00 sobre #000000).
 * En modo horizontal (landscape), oculta todos los menús para dar prioridad 100% al texto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleprompterScreen(viewModel: CaptionsViewModel) {
    val configuration = LocalConfiguration.current
    val isSmartphone = configuration.screenWidthDp < 600
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val teleprompterLines by viewModel.teleprompterLines.collectAsState()
    val sessionStats by viewModel.sessionStats.collectAsState()
    val exportMsg by viewModel.lastExportMessage.collectAsState()
    val asrStatusMsg by viewModel.asrStatusMessage.collectAsState()

    var showSusDialog by remember { mutableStateOf(false) }
    var showCorrectionsDialog by remember { mutableStateOf(false) }
    var showSimulationPanel by remember { mutableStateOf(false) }
    var testSimulatedInput by remember { mutableStateOf("la cedula y la mitocondria realizan fotosintesis") }

    val subjects = listOf("BIOLOGIA", "QUIMICA", "HISTORIA", "GEOGRAFIA")
    val listState = rememberLazyListState()
    val responsiveTextStyle = getResponsiveTeleprompterStyle(isSmartphone)

    // Scroll suave automático al final cuando llega una nueva línea
    LaunchedEffect(teleprompterLines.size) {
        if (teleprompterLines.isNotEmpty()) {
            listState.animateScrollToItem(teleprompterLines.size - 1)
        }
    }

    if (showSusDialog) {
        Box(modifier = Modifier.fillMaxSize()) {
            SUSQuestionnaireScreen(onDismiss = { showSusDialog = false })
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

    if (isLandscape) {
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
                            style = responsiveTextStyle.copy(
                                fontSize = if (isSmartphone) 22.sp else 28.sp,
                                lineHeight = if (isSmartphone) 32.sp else 40.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )
                    }
                }
            }
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
                            fontSize = if (isSmartphone) 18.sp else 22.sp
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
                        IconButton(onClick = { showSusDialog = true }) {
                            Icon(Icons.Default.RateReview, contentDescription = "Cuestionario SUS", tint = HighContrastYellow)
                        }
                        IconButton(onClick = { showSimulationPanel = !showSimulationPanel }) {
                            Icon(Icons.Default.Science, contentDescription = "Simulador", tint = HighContrastYellow)
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
                // Barra de Selección de Asignatura Compacta
                if (isSmartphone) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(subjects) { subject ->
                            SubjectBadge(
                                subject = subject,
                                isSelected = subject == selectedSubject,
                                onClick = { viewModel.selectSubject(subject) }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        subjects.forEach { subject ->
                            Box(modifier = Modifier.weight(1f)) {
                                SubjectBadge(
                                    subject = subject,
                                    isSelected = subject == selectedSubject,
                                    onClick = { viewModel.selectSubject(subject) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                            IconButton(onClick = { viewModel.clearTranscription() }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Limpiar", tint = Color.Gray)
                            }
                        }
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
                                    viewModel.clearAsrStatusMessage()
                                    viewModel.toggleListening()
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

                // Panel de simulación interactiva
                AnimatedVisibility(visible = showSimulationPanel) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Simulador de Aula Escolar (Ruido C = 0.45)",
                                color = HighContrastYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = testSimulatedInput,
                                onValueChange = { testSimulatedInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                                placeholder = { Text("Ej: la cedula y la mitocondria", color = Color.Gray) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val words = testSimulatedInput.split(" ").filter { it.isNotBlank() }
                                    val confidences = words.map { 0.45f }
                                    viewModel.processIncomingRawText(
                                        RawTranscriptionResult(
                                            rawText = testSimulatedInput,
                                            words = words,
                                            wordConfidences = confidences,
                                            isFinal = true
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                            ) {
                                Text("Inyectar Frase con Ruido", color = HighContrastYellow, fontSize = 11.sp)
                            }
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
                }
            }
        }
    }
}

@Composable
private fun SubjectBadge(
    subject: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) HighContrastYellow else DarkSurface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) HighContrastYellow else Color.DarkGray,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = subject,
            color = if (isSelected) PureBlack else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}
