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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unsa.dhh.captions.presentation.theme.HighContrastYellow
import com.unsa.dhh.captions.presentation.theme.PureBlack

/**
 * Pantalla interactiva del Cuestionario SUS (System Usability Scale).
 * Corresponde al Anexo A y Sección 3.6.2 de la Tesis.
 * Evalúa los 10 ítems en escala Likert (1 a 5) y computa el puntaje final estandarizado (0 a 100).
 */
@Composable
fun SUSQuestionnaireScreen(
    onDismiss: () -> Unit
) {
    val susQuestions = remember {
        listOf(
            "1. Creo que me gustaría utilizar este sistema con frecuencia.",
            "2. Encontré que el sistema era innecesariamente complejo.",
            "3. Pensé que el sistema era fácil de usar.",
            "4. Creo que necesitaría el apoyo de una persona con conocimientos técnicos para poder utilizar este sistema.",
            "5. Encontré que las distintas funciones del sistema estaban bien integradas.",
            "6. Pensé que había demasiada inconsistencia en este sistema.",
            "7. Imagino que la mayoría de las personas aprenderían a utilizar este sistema muy rápidamente.",
            "8. Encontré que el sistema era muy complicado de usar.",
            "9. Me sentí muy seguro al utilizar este sistema.",
            "10. Necesité aprender muchas cosas antes de poder comenzar a utilizar este sistema."
        )
    }

    // Inicializar respuestas en 3 (Neutral) por defecto
    val responses = remember { mutableStateListOf(3, 3, 3, 3, 3, 3, 3, 3, 3, 3) }
    var calculatedScore by remember { mutableStateOf<Double?>(null) }

    val isSmartphone = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(if (isSmartphone) 12.dp else 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                    text = "Evaluación de Usabilidad",
                    color = HighContrastYellow,
                    fontSize = if (isSmartphone) 17.sp else 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("VOLVER", color = HighContrastYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Text(
            text = "Anexo A: Escala psicométrica estandarizada (Brooke, 1996) adaptada para estudiantes.",
            color = Color.LightGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        susQuestions.forEachIndexed { index, question ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = question,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Totalmente en desacuerdo (1)", color = Color.Gray, fontSize = 10.sp)
                        Text("Totalmente de acuerdo (5)", color = Color.Gray, fontSize = 10.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { value ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = responses[index] == value,
                                    onClick = { responses[index] = value },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = HighContrastYellow,
                                        unselectedColor = Color.Gray
                                    )
                                )
                                Text(
                                    text = value.toString(),
                                    color = if (responses[index] == value) HighContrastYellow else Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                var convertedSum = 0
                for (i in 0 until 10) {
                    val r = responses[i]
                    convertedSum += if (i % 2 == 0) {
                        r - 1
                    } else {
                        5 - r
                    }
                }
                calculatedScore = convertedSum * 2.5
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HighContrastYellow),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "Calcular Puntuación SUS",
                color = PureBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        calculatedScore?.let { score ->
            val rating = when {
                score >= 80.3 -> "Excelente (Grado A)"
                score >= 68.0 -> "Aceptable / Bueno (Meta de Tesis Superada)"
                score >= 51.0 -> "Marginalmente Aceptable"
                else -> "No Aceptable"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF222B18)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Puntaje SUS Resultante:",
                        color = Color.LightGray,
                        fontSize = 15.sp
                    )
                    Text(
                        text = String.format("%.1f / 100 pts", score),
                        color = HighContrastYellow,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Nivel de Usabilidad: $rating",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botón inferior visible para regresar a la transcripción en cualquier momento
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
