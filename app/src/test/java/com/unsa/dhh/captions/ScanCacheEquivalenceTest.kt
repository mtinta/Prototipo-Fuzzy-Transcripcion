package com.unsa.dhh.captions

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.unsa.dhh.captions.data.decision.DecisionEvaluator
import com.unsa.dhh.captions.data.local.entities.AcademicTerm
import com.unsa.dhh.captions.data.local.entities.TopicCluster
import com.unsa.dhh.captions.domain.fuzzy.LevenshteinMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Las optimizaciones de latencia (memoria de recorridos del diccionario y descarte por largo en las uniones) no
 * pueden cambiar NINGÚN resultado: un evaluador que ya vio las palabras (parciales de Vosk) debe dar lo mismo que
 * uno nuevo. Verificado además bit a bit sobre los 11 corpus el 18/11/2026.
 */
class ScanCacheEquivalenceTest {
    private val root = listOf(File("."), File("..")).first { File(it, "evaluation").exists() }
    private val assets = File(root, "app/src/main/assets")
    private inline fun <reified T> load(rel: String): T =
        File(assets, rel).reader(Charsets.UTF_8).use { Gson().fromJson(it, object : TypeToken<T>() {}.type) }

    private val dict: List<AcademicTerm> = load("dictionaries/biologia.json")
    private val clusters: List<TopicCluster> = load("clusters/biologia_clusters.json")

    @Test
    fun evaluadorConMemoriaDaLoMismoQueUnoNuevo() {
        val corpus = JsonParser.parseString(File(root, "evaluation/corpus_vosk_tts_cyt_prueba.json").readText()).asJsonArray
        val streaming = DecisionEvaluator(lessonContext = null)
        var checked = 0
        for (item in corpus.take(80)) {
            val o = item.asJsonObject
            val words = o.get("raw_asr").asString.split(" ").filter { it.isNotBlank() }
            val confs = o.getAsJsonArray("word_confidences").map { it.asFloat }
            for (k in 1 until words.size) streaming.processSentence(words.take(k), confs.take(k), dict, clusters, false)
            val cached = streaming.processSentence(words, confs, dict, clusters, true)
            val fresh = DecisionEvaluator(lessonContext = null).processSentence(words, confs, dict, clusters, true)
            assertEquals(o.get("id").toString(), fresh, cached)
            checked++
        }
        assertTrue(checked > 50)
    }

    @Test
    fun cotaDeLargoNuncaEsMenorQueLaSimilitudReal() {
        val m = LevenshteinMatcher()
        val rnd = Random(7)
        val letters = "abcdeilmnoprstuvzhqyñáéíóú"
        repeat(20_000) {
            val a = String(CharArray(rnd.nextInt(0, 14)) { letters[rnd.nextInt(letters.length)] })
            val b = String(CharArray(rnd.nextInt(0, 14)) { letters[rnd.nextInt(letters.length)] })
            val pa = m.prepare(a)
            val pb = m.prepareTerm(b)
            assertTrue("$a / $b", m.combinedSimilarityUpperBound(pa, pb) >= m.combinedSimilarity(pa, pb))
        }
    }
}
