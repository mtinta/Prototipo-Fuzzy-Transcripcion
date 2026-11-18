package com.unsa.dhh.captions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.unsa.dhh.captions.data.decision.DecisionEvaluator
import com.unsa.dhh.captions.data.decision.LessonContext
import com.unsa.dhh.captions.data.local.entities.AcademicTerm
import com.unsa.dhh.captions.data.local.entities.TopicCluster
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Términos que Vosk no conoce y escribe en pedazos (58 % del diccionario de biología). Casos tomados de la salida
 * real de Vosk (emulador y corpus TTS) y protecciones contra falsas correcciones.
 */
class SplitTermTest {

    private val assets = listOf("src/main/assets", "app/src/main/assets").map(::File).first { it.exists() }
    private inline fun <reified T> load(rel: String): T =
        File(assets, rel).reader(Charsets.UTF_8).use { Gson().fromJson(it, object : TypeToken<T>() {}.type) }

    private val dict: List<AcademicTerm> = load("dictionaries/biologia.json")
    private val clusters: List<TopicCluster> = load("clusters/biologia_clusters.json")
    private val voskOov: Set<String> = load<List<String>>("dictionaries/biologia_vosk_oov.json").toSet()

    private fun correct(
        sentence: String,
        confidences: List<Float>,
        mode: DecisionEvaluator.SplitTermMode = DecisionEvaluator.SplitTermMode.MORPHEME,
        oov: Set<String> = voskOov
    ): String {
        val ev = DecisionEvaluator(lessonContext = LessonContext(clock = { 0L }), splitTermMode = mode)
        return ev.processSentence(sentence.split(" "), confidences, dict, clusters, asrOutOfVocabulary = oov)
            .joinToString(" ") { it.finalWord }
    }

    @Test fun procariotaSplitByVosk_isRecovered() {
        // Salida real de Vosk en el emulador al decir "procariota"
        assertEquals(
            "una célula procariota no tiene núcleo",
            correct("una célula pero carlota no tiene núcleo", listOf(1f, 1f, 0.81f, 0.84f, 1f, 1f, 1f))
        )
    }

    @Test fun threeWordSplitWithAccentlessTerm_isRecovered() {
        assertEquals(
            "los cromosomas se separan en la anafase",
            correct("los cromosomas se separan en la ana fase", listOf(1f, 1f, 1f, 1f, 1f, 1f, 0.7f, 0.8f))
        )
    }

    @Test fun pieceThatIsARealTerm_mergesOnlyWhenTheAsrDoubted() {
        // "plasma" es término del diccionario: se une a "citoplasma" solo si el ASR dudó de algún pedazo
        // (con contexto de la célula; sin contexto S = 0 y se conserva el texto del ASR)
        val sentence = "el núcleo y el sito plasma forman parte de la célula"
        val doubted = listOf(1f, 1f, 1f, 1f, 0.55f, 0.95f, 1f, 1f, 1f, 1f, 1f)
        assertEquals("el núcleo y el citoplasma forman parte de la célula", correct(sentence, doubted))
        val confident = doubted.toMutableList().also { it[4] = 0.95f }
        assertEquals(sentence, correct(sentence, confident))
    }

    @Test fun correctSentenceWithATermFollowedByAWord_isNotMerged() {
        // "núcleo" + "lo" se parece a "nucleolo", pero la frase es correcta y el ASR estaba seguro
        assertEquals(
            "el núcleo lo protege la membrana",
            correct("el núcleo lo protege la membrana", List(6) { 0.97f })
        )
    }

    @Test fun shortFunctionWordPairs_areNeverMerged() {
        // "y una" -> "yuna" se parecería a "yunga": demasiado corto para unirse
        assertEquals("tienes que saber y una respuesta", correct("tienes que saber y una respuesta", List(6) { 0.97f }))
    }

    @Test fun wholeWordWithHighConfidence_doesNotGetNeutralConfidence() {
        // "saber" escrita por Vosk con seguridad NO debe volverse "suber" (término que Vosk no conoce)
        assertEquals("quisiera saber qué está pasando", correct("quisiera saber qué está pasando", List(5) { 1f }))
    }

    @Test fun neighbourWordIsNotAbsorbedIntoAnExactTerm() {
        // Antes del ajuste "la fotosintesis" se unía y se perdía el artículo
        assertEquals("la fotosíntesis ocurre en la hoja", correct("la fotosintesis ocurre en la hoja", listOf(1f, 0.6f, 1f, 1f, 1f, 1f)))
    }

    @Test fun offMode_keepsTheWordByWordBehaviour() {
        assertEquals(
            "una célula pero carlota no tiene núcleo",
            correct("una célula pero carlota no tiene núcleo", listOf(1f, 1f, 0.81f, 0.84f, 1f, 1f, 1f), DecisionEvaluator.SplitTermMode.OFF)
        )
    }

    @Test fun mergedResult_keepsTheOriginalPiecesForTheCorrectionsLog() {
        val ev = DecisionEvaluator(lessonContext = LessonContext(clock = { 0L }))
        val out = ev.processSentence(
            "una célula pero carlota no tiene núcleo".split(" "), listOf(1f, 1f, 0.81f, 0.84f, 1f, 1f, 1f),
            dict, clusters, asrOutOfVocabulary = voskOov
        )
        val merged = out.single { it.wasSubstituted }
        assertEquals("pero carlota", merged.originalWord)
        assertEquals("procariota", merged.finalWord)
        assertEquals(0.65, merged.acousticConfidence, 1e-9) // C neutra: Vosk no conoce "procariota"
    }

    @Test fun functionWordGluedToAnExactTerm_neverFormsAnotherTerm() {
        // Química: los prefijos convierten "término exacto + palabra vacía" en otro término, a veces el contrario
        // ("de nitrificación" -> desnitrificación, "la polar" -> apolar). Peor caso: el ASR dudó de todo (C = 0.55).
        for (sentence in listOf(
            "el proceso de nitrificación produce nitrato",
            "el metano el etano y el propano son gases",
            "se forma una capa de óxido en el hierro",
            "el agua es una molécula polar y la polar se disuelve"
        )) {
            assertEquals(sentence, correct(sentence, List(sentence.split(" ").size) { 0.55f }))
        }
    }

    @Test fun pluralOfAcuteTerm_dropsTheAccent() {
        // Antes: "las sin facciones" -> "las infecciónes" (corpus TTS + Vosk, biología prueba #1)
        val ev = DecisionEvaluator(lessonContext = LessonContext(clock = { 0L }))
        assertEquals("infecciones", ev.pluralOf("infección"))
        assertEquals("cationes", ev.pluralOf("catión"))
        assertEquals("gametos", ev.pluralOf("gameto"))
        assertEquals("polímeros", ev.pluralOf("polímero"))
        assertEquals("enlaces", ev.pluralOf("enlace"))
        assertEquals("vectores", ev.pluralOf("vector"))
    }
}
