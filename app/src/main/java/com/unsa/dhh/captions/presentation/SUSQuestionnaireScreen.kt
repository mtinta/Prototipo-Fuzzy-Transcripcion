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
 * Presenta los 10 ítems en escala Likert (1 a 5) y después los 4 ítems complementarios del Anexo A
 * (legibilidad, velocidad, comodidad y distracción). La app NO calcula el puntaje: solo guarda las respuestas
 * en CSV; el puntaje SUS se calcula en el análisis (evaluation/evaluar_sesiones.py).
 */
@Composable
fun SUSQuestionnaireScreen(
    onDismiss: () -> Unit,
    /**
     * Recibe las 10 respuestas del SUS (1-5) y las 4 complementarias (1-5), tal como se marcaron.
     */
    onSubmit: (responses: List<Int>, complementary: List<Int>) -> Unit = { _, _ -> }
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
    // Anexo A, ítems complementarios. El 14 es inverso: se invierte en el análisis, no aquí.
    val complementaryQuestions = remember {
        listOf(
            "11. El texto se leía con facilidad.",
            "12. El texto avanzaba a una velocidad que podía seguir.",
            "13. Me sentí cómodo leyendo durante la clase.",
            "14. La pantalla me distraía de la clase."
        )
    }
    val allQuestions = susQuestions + complementaryQuestions

    // 0 = sin responder. Antes se iniciaba en 3 y un ítem no respondido contaba como "neutral".
    val responses = remember { mutableStateListOf(*Array(allQuestions.size) { 0 }) }
    var saved by remember { mutableStateOf(false) }
    val allAnswered = responses.all { it in 1..5 }

    val isSmartphone = DeviceClass.isCompact(androidx.compose.ui.platform.LocalConfiguration.current)

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
            text = "Marca un número del 1 al 5 en cada frase. No hay respuestas correctas ni incorrectas.",
            color = Color.LightGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        allQuestions.forEachIndexed { index, question ->
            if (index == susQuestions.size) {
                Text(
                    text = "Ahora piensa en cómo leíste el texto durante la clase.",
                    color = HighContrastYellow,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                )
            }
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
                saved = true
                onSubmit(responses.take(susQuestions.size), responses.drop(susQuestions.size))
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
                    else -> "Responde las ${allQuestions.size} preguntas (${responses.count { it in 1..5 }}/${allQuestions.size})"
                },
                color = PureBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (saved) {
            // No se muestra el puntaje al participante para no influir en sus respuestas ni en las siguientes sesiones.
            Text(
                text = "¡Gracias! Tus respuestas fueron guardadas.",
                color = HighContrastYellow,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
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
