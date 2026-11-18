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

/** Selección del término corregido con los tres factores (C, D, S) en lugar de solo D. */
class CandidateRerankTest {
    private val assets = listOf("src/main/assets", "app/src/main/assets").map(::File).first { it.exists() }
    private inline fun <reified T> load(rel: String): T =
        File(assets, rel).reader(Charsets.UTF_8).use { Gson().fromJson(it, object : TypeToken<T>() {}.type) }

    private val dict: List<AcademicTerm> = load("dictionaries/biologia.json")
    private val clusters: List<TopicCluster> = load("clusters/biologia_clusters.json")

    private fun DecisionEvaluator.say(text: String, conf: Float = 0.5f, final: Boolean = false) =
        text.split(" ").let { w -> processSentence(w, w.map { conf }, dict, clusters, updateLessonMemory = final) }

    @Test fun contextPicksCelulaOverMedula() {
        val s = "la sedula tiene nucleo y citoplasma"
        val sinRerank = DecisionEvaluator(rerankCandidates = false).say(s)[1]
        assertFalse("Sin rerank el contexto celular frena 'médula' y se pierde la corrección", sinRerank.finalWord == "célula")
        val conRerank = DecisionEvaluator(rerankCandidates = true).say(s)[1]
        assertEquals("célula", conRerank.finalWord)
        assertTrue(conRerank.wasSubstituted)
    }

    @Test fun nervousContextKeepsMedula() {
        val r = DecisionEvaluator().say("el nervio sedula")[2]
        assertEquals("médula", r.finalWord)
    }

    @Test fun exactMatchesAreUnchangedByRerank() {
        val s = "el ribosoma sintetiza la enzima en el citoplasma"
        val a = DecisionEvaluator(rerankCandidates = false).say(s, conf = 0.9f).map { it.finalWord }
        val b = DecisionEvaluator(rerankCandidates = true).say(s, conf = 0.9f).map { it.finalWord }
        assertEquals(a, b)
    }

    @Test fun rerankNeverStealsEverydayWords_evenWithLowConfidenceAndActiveTopic() {
        val ev = DecisionEvaluator(lessonContext = LessonContext())
        repeat(3) { ev.say("la mitosis y la meiosis dividen la célula en el ciclo celular", 0.9f, final = true) }
        val everyday = "realiza dentro contiene protege durante mientras originan"
        for (conf in listOf(0.3f, 0.5f, 0.7f)) {
            assertEquals("conf=$conf", everyday, ev.say(everyday, conf).joinToString(" ") { it.finalWord })
        }
    }

    private fun activeMitosisClass() = DecisionEvaluator(lessonContext = LessonContext()).also { ev ->
        repeat(3) { ev.say("la mitosis y la meiosis dividen la célula en el ciclo celular", 0.9f, final = true) }
    }

    @Test fun activeTopic_cannotHijackWordsWithPoorSimilarity() {
        // "produce"~"profase" (D=0.63) y "duplica"~"placa" (D=0.57): S alto por el tema vigente no debe bastar
        val ev = activeMitosisClass()
        val text = "la meiosis produce gametos y se duplica el material"
        for (conf in listOf(0.3f, 0.65f, 0.9f)) {
            val out = ev.say(text, conf).map { it.finalWord }
            assertEquals("conf=$conf", listOf("la", "meiosis", "produce", "gametos", "y", "se", "duplica", "el", "material"), out)
        }
    }

    @Test fun pluralOfATerm_isNotCollapsedToSingular() {
        val ev = DecisionEvaluator()
        assertEquals("gametos", ev.say("gametos", 0.3f).single().finalWord)
        assertEquals("nucleotidos", ev.say("nucleotidos", 0.3f).single().finalWord)
        assertTrue(ev.isInflectionOf("gametos", "gameto"))
        assertTrue(ev.isInflectionOf("Células", "celula"))
        assertFalse(ev.isInflectionOf("nefron", "nefrona"))
        assertFalse(ev.isInflectionOf("mitosis", "mitosis"))
    }

    @Test fun closerPhoneticTermWinsOverTopicTerm() {
        // "meyosis" (D=0.86 meiosis) no debe convertirse en "mitosis" (D=0.71) aunque la clase sea de mitosis
        val r = activeMitosisClass().say("meyosis", 0.45f).single()
        assertEquals("meiosis", r.finalWord)
    }

    @Test fun macroArea_letsCelulaAndMitocondriaSupportEachOther() {
        // "célula" (citología) y "mitocondria" (organelos) están en clusters distintos pero en la misma área
        val r = DecisionEvaluator().say("la cedula y la mitocondria", 0.45f)
        assertEquals("la célula y la mitocondria", r.joinToString(" ") { it.finalWord })
        assertTrue("S > 0 por crédito de área", r[1].semanticCoherence >= 0.5)
    }

    @Test fun sameWithoutMacroArea_noCredit() {
        val noMacro = clusters.map { it.copy(macroId = null) }
        val w = "la cedula y la mitocondria".split(" ")
        val r = DecisionEvaluator().processSentence(w, w.map { 0.45f }, dict, noMacro)
        assertEquals("sin áreas definidas no hay crédito parcial", "la cedula y la mitocondria", r.joinToString(" ") { it.finalWord })
    }

    @Test fun rejectedCandidates_doNotFeedLessonMemory() {
        val ev = DecisionEvaluator(lessonContext = LessonContext())
        // Palabra cotidiana con candidato débil (no se sustituye): no debe dejar rastro en la memoria
        ev.say("la profesora produce silencio", 0.5f, final = true)
        assertTrue(ev.lessonContext!!.snapshot().isEmpty())
        // Un término bien escrito sí cuenta como evidencia de tema
        ev.say("la mitosis divide la célula", 0.9f, final = true)
        assertFalse(ev.lessonContext.snapshot().isEmpty())
    }

    @Test fun lessonMemory_creditsSameArea() {
        val ev = DecisionEvaluator(lessonContext = LessonContext())
        repeat(3) { ev.say("el ribosoma y la mitocondria y el citoplasma", 0.9f, final = true) } // organelos / citología
        val r = ev.say("mitocis", 0.45f).single() // división celular: misma área "biología celular"
        assertEquals("mitosis", r.finalWord)
        assertTrue("S debe ser >= neutro: ${r.semanticCoherence}", r.semanticCoherence >= 0.5)
    }
}
