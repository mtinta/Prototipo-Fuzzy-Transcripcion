package com.unsa.dhh.captions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.unsa.dhh.captions.data.decision.DecisionEvaluator
import com.unsa.dhh.captions.data.decision.LessonContext
import com.unsa.dhh.captions.data.local.entities.AcademicTerm
import com.unsa.dhh.captions.data.local.entities.TopicCluster
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Memoria de la clase con diccionario y clusters reales de Biología (S sigue entrando al motor de 27 reglas). */
class LessonMemoryPipelineTest {
    private val assets = listOf("src/main/assets", "app/src/main/assets").map(::File).first { it.exists() }
    private inline fun <reified T> load(rel: String): T =
        File(assets, rel).reader(Charsets.UTF_8).use { Gson().fromJson(it, object : TypeToken<T>() {}.type) }

    private val dict: List<AcademicTerm> = load("dictionaries/biologia.json")
    private val clusters: List<TopicCluster> = load("clusters/biologia_clusters.json")
    private var now = 0L
    private fun withMemory() = DecisionEvaluator(lessonContext = LessonContext(clock = { now }))
    private fun withoutMemory() = DecisionEvaluator(lessonContext = null)

    private fun DecisionEvaluator.say(text: String, conf: Float, final: Boolean) =
        processSentence(text.split(" "), text.split(" ").map { conf }, dict, clusters, updateLessonMemory = final)

    private val teachCell = "la mitosis y la meiosis dividen la célula en el ciclo celular"

    @Test fun singleTermSentence_inheritsClassTopic() {
        val ev = withMemory()
        ev.say(teachCell, 0.9f, final = true)
        val r = ev.say("mitocis", 0.45f, final = false).single()
        assertEquals("mitosis", r.finalWord)
        assertTrue("S debe superar el neutro por contexto de clase: ${r.semanticCoherence}", r.semanticCoherence > 0.60)

        val baseline = withoutMemory().say("mitocis", 0.45f, final = false).single()
        assertEquals("Sin memoria, una frase de un solo término recibe S neutro", 0.50, baseline.semanticCoherence, 1e-9)
    }

    @Test fun classContext_preventsOffTopicSubstitution() {
        // "sedula" está más cerca de "médula" (D=0.83) que de "célula" (0.73). Sin contexto se corrige mal.
        val noMem = withoutMemory().say("sedula", 0.5f, final = false).single()
        assertEquals("médula", noMem.finalWord)

        val ev = withMemory()
        ev.say(teachCell, 0.9f, final = true)
        val withMem = ev.say("sedula", 0.5f, final = false).single()
        assertFalse("En clase de célula no debe forzarse 'médula'", withMem.wasSubstituted)
        assertEquals("sedula", withMem.finalWord)
        assertTrue(withMem.semanticCoherence < noMem.semanticCoherence)
    }

    @Test fun partialResults_doNotUpdateMemory() {
        val ev = withMemory()
        ev.say(teachCell, 0.9f, final = false)
        assertTrue(ev.lessonContext!!.snapshot().isEmpty())
        ev.say(teachCell, 0.9f, final = true)
        assertFalse(ev.lessonContext.snapshot().isEmpty())
    }

    @Test fun afterLongSilence_orReset_sNeutralAgain() {
        val ev = withMemory()
        ev.say(teachCell, 0.9f, final = true)
        now += 20 * LessonContext.DEFAULT_HALF_LIFE_MS
        assertEquals(0.50, ev.say("mitocis", 0.45f, final = false).single().semanticCoherence, 1e-9)

        ev.say(teachCell, 0.9f, final = true)
        ev.resetLesson()
        assertEquals(0.50, ev.say("mitocis", 0.45f, final = false).single().semanticCoherence, 1e-9)
    }

    @Test fun everydayWords_stillNeverReplaced_withMemoryActive() {
        val ev = withMemory()
        repeat(3) { ev.say(teachCell, 0.9f, final = true) }
        val text = "realiza dentro contiene protege durante mientras originan"
        val out = ev.say(text, 0.5f, final = false).joinToString(" ") { it.finalWord }
        assertEquals(text, out)
    }

    @Test fun realTime_fullSentenceWithMemory_underBudget() {
        val ev = withMemory()
        ev.say(teachCell, 0.9f, final = true) // calienta caché de índice
        val sentence = "durante la mitocis la sedula se divide mientras en la meyosis se originan los gametos"
        val n = 50
        val t0 = System.nanoTime()
        repeat(n) { ev.say(sentence, 0.5f, final = true) }
        val avgMs = (System.nanoTime() - t0) / 1e6 / n
        // Meta de la tesis: < 1000 ms por oración; TC-PERF-01 exige <= 100 ms. JVM de escritorio, holgado.
        assertTrue("promedio $avgMs ms por oración (14 palabras, diccionario real completo)", avgMs < 100.0)
    }
}
