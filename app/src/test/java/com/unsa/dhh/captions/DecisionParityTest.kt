package com.unsa.dhh.captions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.unsa.dhh.captions.data.decision.DecisionEvaluator
import com.unsa.dhh.captions.data.decision.LessonContext
import com.unsa.dhh.captions.data.local.entities.AcademicTerm
import com.unsa.dhh.captions.data.local.entities.TopicCluster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Paridad de FRASES COMPLETAS Kotlin <-> Python: la app debe corregir cada frase exactamente igual que el
 * benchmark de la tesis (evaluation/benchmark_runner.py), incluidas las uniones de palabras partidas por Vosk,
 * la memoria de la clase y la C neutra de los términos fuera de su vocabulario.
 * Datos: evaluation/paridad_decision.json (python3 evaluation/generar_paridad_decision.py).
 */
class DecisionParityTest {

    private class Item(val gap_s: Double, val words: List<String>, val confs: List<Float>, val expected: String)
    private class Corpus(val corpus: String, val oov: Boolean, val items: List<Item>)

    private fun file(vararg candidates: String) = candidates.map(::File).first { it.exists() }
    private inline fun <reified T> load(f: File): T =
        f.reader(Charsets.UTF_8).use { Gson().fromJson(it, object : TypeToken<T>() {}.type) }

    @Test fun appCorrectsEverySentenceExactlyLikeTheBenchmark() {
        val assets = file("src/main/assets", "app/src/main/assets")
        val dict: List<AcademicTerm> = load(File(assets, "dictionaries/biologia.json"))
        val clusters: List<TopicCluster> = load(File(assets, "clusters/biologia_clusters.json"))
        val oov: Set<String> = load<List<String>>(File(assets, "dictionaries/biologia_vosk_oov.json")).toSet()
        val fixture: List<Corpus> = load(file("../evaluation/paridad_decision.json", "evaluation/paridad_decision.json"))

        val mismatches = mutableListOf<String>()
        var sentences = 0
        var merged = 0
        for (c in fixture) {
            var now = 0L
            val ev = DecisionEvaluator(lessonContext = LessonContext(clock = { now }))
            for (item in c.items) {
                now += (item.gap_s * 1000).toLong()
                val out = ev.processSentence(
                    item.words, item.confs, dict, clusters, updateLessonMemory = true,
                    asrOutOfVocabulary = if (c.oov) oov else emptySet()
                )
                merged += out.count { it.wasSubstituted && it.originalWord.contains(' ') }
                val got = out.joinToString(" ") { it.finalWord }
                if (got != item.expected) mismatches += "${c.corpus}: «${item.words.joinToString(" ")}»\n  Python: ${item.expected}\n  Kotlin: $got"
                sentences++
            }
        }
        assertTrue("Se esperaban cientos de frases", sentences > 400)
        assertTrue("Deben existir uniones de palabras partidas en los corpus de Vosk", merged > 0)
        assertEquals("Frases distintas entre Kotlin y Python:\n" + mismatches.take(10).joinToString("\n"), 0, mismatches.size)
    }
}
