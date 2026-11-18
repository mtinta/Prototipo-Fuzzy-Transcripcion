package com.unsa.dhh.captions.data.decision

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Afinidad de un término candidato con el tema que la clase viene tratando.
 *
 * @property value     Afinidad relativa en [0.0, 1.0]: peso del cluster del candidato / peso del cluster dominante.
 * @property strength  Confiabilidad de la memoria en [0.0, 1.0]: crece con la evidencia acumulada y decae con el silencio.
 * @property bestClusterId Cluster del candidato con mayor peso en la memoria.
 */
data class LessonAffinity(val value: Double, val strength: Double, val bestClusterId: String)

/**
 * Memoria temática de la clase (extensión del Factor Semántico S).
 *
 * Mantiene un peso por cluster que sube cuando se reconocen con solidez términos de ese tema y decae
 * exponencialmente con el tiempo (vida media [halfLifeMs]). Permite que una oración con un único término
 * técnico herede el contexto de lo que se dijo antes, en vez de recibir un S neutro (0.50).
 *
 * Costo en tiempo real: [affinity] y [recordTerms] son O(clusters del candidato) y no asignan estructuras
 * proporcionales al diccionario. Es seguro entre hilos (el ViewModel procesa parciales y finales en paralelo).
 *
 * @param clock Fuente de tiempo en ms; inyectable para pruebas deterministas.
 */
class LessonContext(
    private val halfLifeMs: Long = DEFAULT_HALF_LIFE_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val weights = HashMap<String, Double>()
    private var lastUpdateMs: Long = Long.MIN_VALUE

    /** Área temática de cada cluster (idCluster -> idÁrea). Vacío = sin crédito por área. */
    private var macroByCluster: Map<String, String> = emptyMap()

    /** Define las áreas temáticas de los clusters vigentes. Se llama al cargar los clusters de una asignatura. */
    @Synchronized
    fun setMacroMap(map: Map<String, String>) {
        macroByCluster = map
    }

    /** Factor de decaimiento entre [lastUpdateMs] y [nowMs]. */
    private fun decayFactor(nowMs: Long): Double {
        if (lastUpdateMs == Long.MIN_VALUE) return 1.0
        val dt = max(0L, nowMs - lastUpdateMs)
        return 0.5.pow(dt.toDouble() / halfLifeMs.toDouble())
    }

    /**
     * Registra los términos de contexto de una oración FINAL.
     * Cada elemento es la lista de ids de cluster del término; si el término está en k clusters,
     * aporta 1/k a cada uno (un término compartido es evidencia ambigua).
     */
    @Synchronized
    fun recordTerms(termClusterIds: List<List<String>>) {
        if (termClusterIds.isEmpty()) return
        val now = clock()
        val factor = decayFactor(now)
        if (factor < 1.0) {
            val it = weights.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                val decayed = e.value * factor
                if (decayed < PRUNE_BELOW) it.remove() else e.setValue(decayed)
            }
        }
        lastUpdateMs = now
        for (ids in termClusterIds) {
            if (ids.isEmpty()) continue
            val share = 1.0 / ids.size
            for (id in ids) weights[id] = (weights[id] ?: 0.0) + share
        }
    }

    /**
     * Afinidad del candidato (dado por los ids de sus clusters) con el tema vigente.
     * Devuelve null si no hay evidencia útil todavía (inicio de clase o silencio prolongado).
     */
    @Synchronized
    fun affinity(candidateClusterIds: List<String>): LessonAffinity? {
        if (weights.isEmpty() || candidateClusterIds.isEmpty()) return null
        val factor = decayFactor(clock())
        var dominant = 0.0
        for (w in weights.values) if (w > dominant) dominant = w
        val evidence = dominant * factor
        if (evidence < MIN_EVIDENCE) return null

        var best = 0.0
        var bestId = candidateClusterIds.first()
        for (id in candidateClusterIds) {
            val w = weights[id] ?: 0.0
            if (w > best) { best = w; bestId = id }
        }
        var affinity = best / dominant

        // Crédito por área temática: hablar de "organelos" también prepara a "división celular" (misma área)
        if (macroByCluster.isNotEmpty()) {
            val macroWeights = HashMap<String, Double>()
            for ((cid, w) in weights) macroByCluster[cid]?.let { m -> macroWeights[m] = (macroWeights[m] ?: 0.0) + w }
            var dominantMacro = 0.0
            for (w in macroWeights.values) if (w > dominantMacro) dominantMacro = w
            if (dominantMacro > 0.0) {
                for (id in candidateClusterIds) {
                    val m = macroByCluster[id] ?: continue
                    val viaMacro = MACRO_CREDIT * (macroWeights[m] ?: 0.0) / dominantMacro
                    if (viaMacro > affinity) { affinity = viaMacro; if (best == 0.0) bestId = id }
                }
            }
        }
        val value = affinity.coerceIn(LESSON_FLOOR, 1.0)
        val strength = min(1.0, evidence / FULL_STRENGTH_EVIDENCE)
        return LessonAffinity(value, strength, bestId)
    }

    @Synchronized
    fun reset() {
        weights.clear()
        lastUpdateMs = Long.MIN_VALUE
    }

    /** Copia de los pesos actuales (solo diagnóstico/pruebas). */
    @Synchronized
    fun snapshot(): Map<String, Double> {
        val f = decayFactor(clock())
        return weights.mapValues { it.value * f }
    }

    companion object {
        /**
         * Vida media provisional (~5 oraciones). Compromiso: más corta = se adapta antes al cambio de tema;
         * más larga = protege mejor de correcciones fuera de tema. 45 s fue el mejor valor en ambos corpus
         * sintéticos (barrido 20-180 s en `benchmark_runner.py`); DEBE recalibrarse con grabaciones reales.
         */
        const val DEFAULT_HALF_LIFE_MS = 45_000L
        /** Evidencia mínima (≈ un término sólido reciente) para considerar que hay "tema vigente". */
        const val MIN_EVIDENCE = 0.5
        /** Igual que DecisionEvaluator.MACRO_CREDIT: crédito por pertenecer a la misma área temática. */
        const val MACRO_CREDIT = 0.5
        /** Evidencia para confianza plena: ≈ 3 términos sólidos del mismo tema. */
        const val FULL_STRENGTH_EVIDENCE = 3.0
        /** Piso de afinidad; coherente con S = 0.20 de "término sin cluster". Evita vetos absolutos al cambiar de tema. */
        const val LESSON_FLOOR = 0.20
        private const val PRUNE_BELOW = 0.01
    }
}
