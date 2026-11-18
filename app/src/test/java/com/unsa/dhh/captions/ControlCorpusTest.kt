package com.unsa.dhh.captions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.unsa.dhh.captions.data.decision.DecisionEvaluator
import com.unsa.dhh.captions.data.local.entities.AcademicTerm
import com.unsa.dhh.captions.data.local.entities.TopicCluster
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Control negativo: lenguaje cotidiano de aula transcrito CORRECTAMENTE por el ASR (con confianzas bajas
 * incluidas). Cualquier sustitución es un falso positivo: cambiaría en silencio algo que ya estaba bien.
 */
class ControlCorpusTest {
    private data class Item(val raw_asr: String, val word_confidences: List<Float>)

    private fun file(vararg candidates: String) = candidates.map(::File).first { it.exists() }
    private inline fun <reified T> load(f: File): T =
        f.reader(Charsets.UTF_8).use { Gson().fromJson(it, object : TypeToken<T>() {}.type) }

    @Test fun everydayClassroomLanguage_isNeverModified() {
        val dict: List<AcademicTerm> = load(file("src/main/assets/dictionaries/biologia.json", "app/src/main/assets/dictionaries/biologia.json"))
        val clusters: List<TopicCluster> = load(file("src/main/assets/clusters/biologia_clusters.json", "app/src/main/assets/clusters/biologia_clusters.json"))
        val corpus: List<Item> = load(file("../evaluation/corpus_control_cotidiano.json", "evaluation/corpus_control_cotidiano.json"))
        val ev = DecisionEvaluator()
        var words = 0
        for (item in corpus) {
            val w = item.raw_asr.split(" ")
            val out = ev.processSentence(w, item.word_confidences, dict, clusters, updateLessonMemory = true)
            words += w.size
            assertEquals(item.raw_asr, out.joinToString(" ") { it.finalWord })
        }
        assertEquals(true, words > 100)
    }

    /**
     * Controles con el motor Vosk: (a) frases de clase CORRECTAS con C alta ("respiración celular" no debe volverse
     * "célula"); (b) 200 frases cotidianas de OpenSLR 73 transcritas por Vosk, con la C real y la lista de términos
     * fuera de su vocabulario activa (uniones de palabras + C neutra). Ninguna palabra puede cambiar.
     */
    @Test fun voskControls_areNeverModified_withSplitTermsAndOutOfVocabularyConfidence() {
        val dict: List<AcademicTerm> = load(file("src/main/assets/dictionaries/biologia.json", "app/src/main/assets/dictionaries/biologia.json"))
        val clusters: List<TopicCluster> = load(file("src/main/assets/clusters/biologia_clusters.json", "app/src/main/assets/clusters/biologia_clusters.json"))
        val oov: Set<String> = load<List<String>>(file("src/main/assets/dictionaries/biologia_vosk_oov.json", "app/src/main/assets/dictionaries/biologia_vosk_oov.json")).toSet()
        var words = 0
        for (name in listOf("corpus_control_clase_c_alta.json", "corpus_vosk_slr73_control.json")) {
            val corpus: List<Item> = load(file("../evaluation/$name", "evaluation/$name"))
            val ev = DecisionEvaluator()
            for (item in corpus) {
                val w = item.raw_asr.split(" ")
                val out = ev.processSentence(w, item.word_confidences, dict, clusters, updateLessonMemory = true, asrOutOfVocabulary = oov)
                words += w.size
                assertEquals("$name modificó una frase correcta", item.raw_asr, out.joinToString(" ") { it.finalWord })
            }
        }
        assertEquals(true, words > 1500)
    }
}
