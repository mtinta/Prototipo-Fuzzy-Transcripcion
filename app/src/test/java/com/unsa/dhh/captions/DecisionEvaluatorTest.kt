package com.unsa.dhh.captions

import com.unsa.dhh.captions.data.decision.DecisionEvaluator
import com.unsa.dhh.captions.data.local.entities.AcademicTerm
import com.unsa.dhh.captions.data.local.entities.TopicCluster
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para la arquitectura de dos pasadas y factor semántico por Topic Clusters en DecisionEvaluator.
 */
class DecisionEvaluatorTest {

    private lateinit var evaluator: DecisionEvaluator
    private lateinit var dictionary: List<AcademicTerm>
    private lateinit var clusters: List<TopicCluster>

    @Before
    fun setUp() {
        evaluator = DecisionEvaluator()

        // Vocabulario de prueba
        dictionary = listOf(
            AcademicTerm(id = 1, term = "célula", lemma = "célula", subject = "BIOLOGIA", category = "NOUN", phonetic = "selula"),
            AcademicTerm(id = 2, term = "procariota", lemma = "procariota", subject = "BIOLOGIA", category = "NOUN", phonetic = "prokariota"),
            AcademicTerm(id = 3, term = "núcleo", lemma = "núcleo", subject = "BIOLOGIA", category = "NOUN", phonetic = "nukleo"),
            AcademicTerm(id = 4, term = "membrana", lemma = "membrana", subject = "BIOLOGIA", category = "NOUN", phonetic = "membrana"),
            AcademicTerm(id = 5, term = "citoplasma", lemma = "citoplasma", subject = "BIOLOGIA", category = "NOUN", phonetic = "sitoplasma"),
            AcademicTerm(id = 6, term = "corazón", lemma = "corazón", subject = "BIOLOGIA", category = "NOUN", phonetic = "korason")
        )

        clusters = listOf(
            TopicCluster(
                topicId = "citologia_general",
                topicName = "Citología General",
                terms = listOf("célula", "procariota", "núcleo", "membrana", "citoplasma")
            ),
            TopicCluster(
                topicId = "sistema_circulatorio",
                topicName = "Sistema Circulatorio",
                terms = listOf("corazón", "arteria", "vena", "sangre")
            )
        )
    }

    @Test
    fun testTwoPassCoOccurrence_correctsSedulaToCelulaWithCluster() {
        // Frase con error fonético: "sedula procariota carece de nukleo"
        val words = listOf("sedula", "procariota", "carece", "de", "nukleo")
        val confidences = listOf(0.55f, 0.90f, 0.85f, 0.95f, 0.55f)

        val results = evaluator.processSentence(
            rawWords = words,
            confidences = confidences,
            dictionary = dictionary,
            clusters = clusters
        )

        // Verificación de la corrección "sedula" -> "célula"
        val celulaRes = results[0]
        assertEquals("sedula", celulaRes.originalWord)
        assertEquals("célula", celulaRes.finalWord)
        assertTrue("Debe ser sustituida", celulaRes.wasSubstituted)
        assertTrue("La justificación difusa debe superar el umbral", celulaRes.fuzzyJustification >= 0.65)
        assertEquals("Citología General", celulaRes.topicCluster)
        assertTrue("La coherencia semántica debe ser alta por co-ocurrencia (> 0.50)", celulaRes.semanticCoherence > 0.50)

        // Verificación de "nukleo" -> "núcleo"
        val nucleoRes = results[4]
        assertEquals("nukleo", nucleoRes.originalWord)
        assertEquals("núcleo", nucleoRes.finalWord)
        assertTrue("Debe ser sustituida", nucleoRes.wasSubstituted)
        assertEquals("Citología General", nucleoRes.topicCluster)
    }

    @Test
    fun testUnrelatedWord_doesNotStealClusterContext() {
        // Frase con un término aislado de anatomía circulatoria en medio de ruido
        val words = listOf("ayer", "fuimos", "al", "parque")
        val confidences = listOf(0.90f, 0.90f, 0.95f, 0.90f)

        val results = evaluator.processSentence(
            rawWords = words,
            confidences = confidences,
            dictionary = dictionary,
            clusters = clusters
        )

        assertTrue("Ninguna palabra común debe ser sustituida", results.none { it.wasSubstituted })
    }

    @Test
    fun testAllDictionaries_have100PercentClusterCoverage_noOrphanTerms() {
        val subjects = listOf("biologia", "quimica", "historia", "geografia")
        val gson = com.google.gson.Gson()

        for (subject in subjects) {
            val dictFile = getAssetFile("dictionaries/$subject.json")
            val clusterFile = getAssetFile("clusters/${subject}_clusters.json")

            assertTrue("El diccionario de $subject debe existir", dictFile.exists())
            assertTrue("El archivo de clusters de $subject debe existir", clusterFile.exists())

            val dictType = object : com.google.gson.reflect.TypeToken<List<AcademicTerm>>() {}.type
            val clusterType = object : com.google.gson.reflect.TypeToken<List<TopicCluster>>() {}.type

            val dictList: List<AcademicTerm> = gson.fromJson(dictFile.readText(), dictType)
            val clusterList: List<TopicCluster> = gson.fromJson(clusterFile.readText(), clusterType)

            assertFalse("El diccionario de $subject no debe estar vacío", dictList.isEmpty())
            assertFalse("Los clusters de $subject no deben estar vacíos", clusterList.isEmpty())

            val allClusterTerms = clusterList.flatMap { it.termsSet }.toSet()

            val orphans = mutableListOf<String>()
            for (term in dictList) {
                val termLower = term.term.lowercase()
                if (termLower !in allClusterTerms) {
                    orphans.add(term.term)
                }
            }

            assertTrue(
                "En $subject existen términos huérfanos sin cluster asignado: $orphans (cobertura incompleta)",
                orphans.isEmpty()
            )

            for (cluster in clusterList) {
                assertTrue(
                    "El cluster '${cluster.topicId}' en $subject debe tener al menos 5 términos (actual: ${cluster.terms.size})",
                    cluster.terms.size >= 5
                )
            }
        }
    }

    @Test
    fun testChemistryCoOccurrence_boostsSemanticCoherence() {
        val gson = com.google.gson.Gson()
        val dictList: List<AcademicTerm> = gson.fromJson(
            getAssetFile("dictionaries/quimica.json").readText(),
            object : com.google.gson.reflect.TypeToken<List<AcademicTerm>>() {}.type
        )
        val clusterList: List<TopicCluster> = gson.fromJson(
            getAssetFile("clusters/quimica_clusters.json").readText(),
            object : com.google.gson.reflect.TypeToken<List<TopicCluster>>() {}.type
        )

        // "oksidasion" con baja confianza (0.55) junto a "reducción" con alta confianza (0.90)
        val words = listOf("la", "oksidasion", "y", "reducción", "ocurren", "juntas")
        val confidences = listOf(0.95f, 0.55f, 0.95f, 0.90f, 0.95f, 0.95f)

        val results = evaluator.processSentence(
            rawWords = words,
            confidences = confidences,
            dictionary = dictList,
            clusters = clusterList
        )

        val oxRes = results[1]
        assertEquals("oksidasion", oxRes.originalWord)
        assertEquals("oxidación", oxRes.finalWord)
        assertTrue("Debe sustituir silenciosamente", oxRes.wasSubstituted)
        assertTrue("Coherencia semántica S debe ser > 0.50 por co-ocurrencia con reducción", oxRes.semanticCoherence > 0.50)
        assertEquals("Reacciones Químicas, Termoquímica y Redox", oxRes.topicCluster)
    }

    @Test
    fun testHistoryCoOccurrence_boostsSemanticCoherence() {
        val gson = com.google.gson.Gson()
        val dictList: List<AcademicTerm> = gson.fromJson(
            getAssetFile("dictionaries/historia.json").readText(),
            object : com.google.gson.reflect.TypeToken<List<AcademicTerm>>() {}.type
        )
        val clusterList: List<TopicCluster> = gson.fromJson(
            getAssetFile("clusters/historia_clusters.json").readText(),
            object : com.google.gson.reflect.TypeToken<List<TopicCluster>>() {}.type
        )

        // "tawantinsuyo" deformado con baja confianza (0.55) co-ocurriendo con "ayllu" (0.90)
        val words = listOf("el", "tawantinsuyo", "organizaba", "el", "ayllu")
        val confidences = listOf(0.95f, 0.55f, 0.90f, 0.95f, 0.90f)

        val results = evaluator.processSentence(
            rawWords = words,
            confidences = confidences,
            dictionary = dictList,
            clusters = clusterList
        )

        val tahuanRes = results[1]
        assertEquals("tawantinsuyo", tahuanRes.originalWord)
        assertEquals("Tahuantinsuyo", tahuanRes.finalWord)
        assertTrue("Debe sustituir silenciosamente", tahuanRes.wasSubstituted)
        assertTrue("Coherencia semántica S debe ser > 0.50", tahuanRes.semanticCoherence > 0.50)
        assertEquals("Sociedad y Organización Andina Prehispánica", tahuanRes.topicCluster)
    }

    @Test
    fun testGeographyCoOccurrence_boostsSemanticCoherence() {
        val gson = com.google.gson.Gson()
        val dictList: List<AcademicTerm> = gson.fromJson(
            getAssetFile("dictionaries/geografia.json").readText(),
            object : com.google.gson.reflect.TypeToken<List<AcademicTerm>>() {}.type
        )
        val clusterList: List<TopicCluster> = gson.fromJson(
            getAssetFile("clusters/geografia_clusters.json").readText(),
            object : com.google.gson.reflect.TypeToken<List<TopicCluster>>() {}.type
        )

        // "kordillera" deformado con baja confianza (0.55) co-ocurriendo con "meseta" (0.90)
        val words = listOf("la", "kordillera", "rodea", "la", "meseta")
        val confidences = listOf(0.95f, 0.55f, 0.90f, 0.95f, 0.90f)

        val results = evaluator.processSentence(
            rawWords = words,
            confidences = confidences,
            dictionary = dictList,
            clusters = clusterList
        )

        val cordilleraRes = results[1]
        assertEquals("kordillera", cordilleraRes.originalWord)
        assertEquals("cordillera", cordilleraRes.finalWord)
        assertTrue("Debe sustituir silenciosamente", cordilleraRes.wasSubstituted)
        assertTrue("Coherencia semántica S debe ser > 0.50", cordilleraRes.semanticCoherence > 0.50)
        assertEquals("Geomorfología y Relieve", cordilleraRes.topicCluster)
    }

    private fun getAssetFile(relativePath: String): java.io.File {
        val candidates = listOf(
            java.io.File("src/main/assets", relativePath),
            java.io.File("app/src/main/assets", relativePath),
            java.io.File("../app/src/main/assets", relativePath)
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Asset no encontrado: $relativePath en ninguna ruta candidata: $candidates")
    }
}
