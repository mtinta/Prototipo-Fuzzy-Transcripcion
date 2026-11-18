package com.unsa.dhh.captions.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unsa.dhh.captions.presentation.theme.HighContrastYellow
import com.unsa.dhh.captions.presentation.theme.PureBlack
import kotlin.math.roundToInt

/**
 * NASA-TLX, versión Raw (Anexo E de la tesis; Hart y Staveland, 1988; Hart, 2006).
 *
 * Se responde después de cada mitad de la clase. Cada subescala es una barra de 0 a 100 en pasos de 5.
 * La barra empieza sin marca (el cursor no se ve) para no sugerir un valor: el estudiante debe tocar las seis
 * antes de guardar. No se muestra la condición (T0/T2); la añade el ViewModel. La app NO calcula el puntaje:
 * solo guarda los 6 valores en CSV; la media (TLX Raw) se calcula en el análisis.
 */
@Composable
fun NasaTlxScreen(
    onDismiss: () -> Unit,
    /** Recibe los 6 valores (0-100) en el orden del Anexo E. */
    onSubmit: (values: List<Int>) -> Unit = {}
) {
    val subscales = remember {
        listOf(
            TlxSubscale("Demanda mental", "¿Cuánto tuviste que pensar y concentrarte para seguir la clase?", "Muy poco", "Muchísimo"),
            TlxSubscale("Demanda física", "¿Cuánto esfuerzo físico te exigió (mover los ojos, la postura)?", "Muy poco", "Muchísimo"),
            TlxSubscale("Demanda temporal", "¿Sentiste que el texto iba demasiado rápido?", "Nada rápido", "Demasiado rápido"),
            TlxSubscale("Desempeño", "¿Qué tan bien crees que entendiste la clase?", "Perfecto", "Nada"),
            TlxSubscale("Esfuerzo", "¿Cuánto te esforzaste para entender?", "Muy poco", "Muchísimo"),
            TlxSubscale("Frustración", "¿Qué tan molesto o frustrado te sentiste?", "Nada", "Muchísimo")
        )
    }

    // null = sin responder
    val values = remember { mutableStateListOf<Int?>(*arrayOfNulls(subscales.size)) }
    var saved by remember { mutableStateOf(false) }
    val answered = values.count { it != null }
    val allAnswered = answered == subscales.size

    val isSmartphone = DeviceClass.isCompact(LocalConfiguration.current)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(if (isSmartphone) 12.dp else 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = HighContrastYellow
                )
            }
            Text(
                text = "¿Cómo te sentiste en esta parte de la clase?",
                color = HighContrastYellow,
                fontSize = if (isSmartphone) 17.sp else 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Piensa solo en la parte de la clase que acaba de terminar. " +
                "Toca la barra en el lugar que mejor diga lo que sentiste. No hay respuestas correctas ni incorrectas.",
            color = Color.LightGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        subscales.forEachIndexed { index, item ->
            val value = values[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "${index + 1}. ${item.name}",
                        color = HighContrastYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = item.question, color = Color.White, fontSize = 14.sp)

                    Slider(
                        value = (value ?: 50).toFloat(),
                        onValueChange = { if (!saved) values[index] = (it / 5f).roundToInt() * 5 },
                        valueRange = 0f..100f,
                        steps = 19, // 21 posiciones: 0, 5, ..., 100
                        enabled = !saved,
                        colors = SliderDefaults.colors(
                            // Sin responder: cursor invisible y barra sin relleno, para no sugerir el 50
                            thumbColor = if (value == null) Color.Transparent else HighContrastYellow,
                            activeTrackColor = if (value == null) Color(0xFF555555) else HighContrastYellow,
                            inactiveTrackColor = Color(0xFF555555),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                            disabledThumbColor = HighContrastYellow,
                            disabledActiveTrackColor = HighContrastYellow,
                            disabledInactiveTrackColor = Color(0xFF555555)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.lowLabel, color = Color.Gray, fontSize = 11.sp)
                        Text(
                            text = value?.let { "Tu marca: $it" } ?: "Sin marcar",
                            color = if (value == null) Color.Gray else HighContrastYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(item.highLabel, color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                saved = true
                onSubmit(values.map { it!! })
            },
            enabled = allAnswered && !saved,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HighContrastYellow),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                when {
                    saved -> "Respuestas guardadas"
                    allAnswered -> "Guardar respuestas"
                    else -> "Marca las ${subscales.size} barras ($answered/${subscales.size})"
                },
                color = PureBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (saved) {
            Text(
                text = "¡Gracias! Tus respuestas fueron guardadas.",
                color = HighContrastYellow,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private data class TlxSubscale(val name: String, val question: String, val lowLabel: String, val highLabel: String)
