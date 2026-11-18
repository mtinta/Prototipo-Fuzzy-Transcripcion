package com.unsa.dhh.captions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.unsa.dhh.captions.data.decision.DecisionEvaluator
import com.unsa.dhh.captions.data.local.entities.AcademicTerm
import com.unsa.dhh.captions.data.local.entities.TopicCluster
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Prueba de integración con los diccionarios y clusters REALES de producción (648 términos de Ciencia y Tecnología: biología, química y ciclos biogeoquímicos).
 * Los tests con diccionarios mínimos no detectaban que los falsos positivos léxicos diluían el factor S.
 */
class RealAssetsPipelineTest {
    private val assets = listOf("src/main/assets", "app/src/main/assets").map(::File).first { it.exists() }

    private inline fun <reified T> load(rel: String): T =
        File(assets, rel).reader(Charsets.UTF_8).use { Gson().fromJson(it, object : TypeToken<T>() {}.type) }

    private val dict: List<AcademicTerm> = load("dictionaries/biologia.json")
    private val clusters: List<TopicCluster> = load("clusters/biologia_clusters.json")
    private val evaluator = DecisionEvaluator()

    private fun run(text: String, conf: List<Float>) =
        evaluator.processSentence(text.split(" "), conf, dict, clusters).joinToString(" ") { it.finalWord }

    @Test fun cellDivisionSentence_isCorrectedWithRealDictionary() {
        val out = run(
            "el sitoplasma contiene el riposoma y protege el nuleolo",
            listOf(0.9f, 0.45f, 0.82f, 0.85f, 0.39f, 0.9f, 0.83f, 0.85f, 0.4f).take(9)
        )
        assertTrue(out, out.contains("citoplasma") && out.contains("ribosoma") && out.contains("nucleolo"))
    }

    @Test fun everydayWords_areNeverReplaced() {
        val words = "realiza dentro contiene protege durante mientras originan"
        val out = run(words, List(7) { 0.5f })
        assertEquals(words, out)
    }

    @Test fun contextSet_ignoresNoiseMatches() {
        // "realiza"/"dentro" tienen D>=0.50 contra términos ajenos; no deben diluir S de "coroplasto"
        val results = evaluator.processSentence(
            listOf("la", "fotosintesis", "realiza", "dentro", "del", "coroplasto"),
            listOf(0.9f, 0.9f, 0.8f, 0.8f, 0.9f, 0.40f), dict, clusters
        )
        assertEquals("cloroplasto", results[5].finalWord)
        assertTrue("S debe ser >= 0.5 (contexto limpio)", results[5].semanticCoherence >= 0.5)
    }
}
