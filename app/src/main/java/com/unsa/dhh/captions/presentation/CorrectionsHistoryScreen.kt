package com.unsa.dhh.captions.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unsa.dhh.captions.data.local.entities.SessionLogEntity
import com.unsa.dhh.captions.presentation.theme.DarkSurface
import com.unsa.dhh.captions.presentation.theme.HighContrastYellow
import com.unsa.dhh.captions.presentation.theme.PureBlack
import com.unsa.dhh.captions.presentation.viewmodel.CaptionsViewModel

/**
 * Pantalla de Historial de Correcciones Difusas.
 * Muestra el detalle de cada palabra sustituida por el motor difuso de Mamdani,
 * comparando la palabra original recibida del ASR frente a la forma canónica del diccionario.
 */
@Composable
fun CorrectionsHistoryScreen(
    viewModel: CaptionsViewModel,
    onDismiss: () -> Unit
) {
    val corrections = remember { viewModel.getCorrectionsHistory() }
    val isSmartphone = DeviceClass.isCompact(androidx.compose.ui.platform.LocalConfiguration.current)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(if (isSmartphone) 12.dp else 24.dp)
    ) {
        // Cabecera Superior
        Text(
            text = "Historial de Correcciones",
            color = HighContrastYellow,
            fontSize = if (isSmartphone) 17.sp else 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tarjeta resumen de métricas
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Tarjeta Sustituciones
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2211)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF2E3D1C))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Sustituciones",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${corrections.size}",
                        color = HighContrastYellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Umbral y* ≥ 0.65",
                        color = Color(0xFFAAAAAA),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lista de Correcciones
        if (corrections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se han realizado correcciones en la sesión actual.\nHable por el micrófono.",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(corrections) { item ->
                    CorrectionItemCard(item = item)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botón Inferior para Regresar
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            border = BorderStroke(1.5.dp, HighContrastYellow),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = HighContrastYellow
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "VOLVER A LA TRANSCRIPCIÓN",
                color = HighContrastYellow,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun CorrectionItemCard(item: SessionLogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Palabra original ASR (resaltada con tachado suave)
                    Text(
                        text = item.originalWord,
                        color = Color(0xFFFF8A80),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = HighContrastYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Palabra corregida del diccionario
                    Text(
                        text = item.correctedWord,
                        color = HighContrastYellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Badge de Materia
                Box(
                    modifier = Modifier
                        .background(Color(0xFF222222), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.subject,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (item.topicCluster != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tema: ${item.topicCluster}",
                    color = Color(0xFF81C784),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Desglose de Métricas Difusas Tri-Variables (C, D, S y Justificación y*)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "C: ${(item.acousticConfidence * 100).toInt()}%",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Text(
                    text = "D: ${(item.lexicalSimilarity * 100).toInt()}%",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Text(
                    text = "S: ${(item.semanticCoherence * 100).toInt()}%",
                    color = Color(0xFF81C784),
                    fontSize = 11.sp
                )
                Text(
                    text = "Justif. y*: ${String.format(java.util.Locale.US, "%.2f", item.fuzzyJustification)}",
                    color = HighContrastYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
