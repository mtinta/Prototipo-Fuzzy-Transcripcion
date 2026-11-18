package com.unsa.dhh.captions

import com.unsa.dhh.captions.data.decision.LessonContext
import org.junit.Assert.*
import org.junit.Test

class LessonContextTest {
    private var now = 1_000_000L
    private val half = 120_000L
    private fun ctx() = LessonContext(halfLifeMs = half, clock = { now })

    @Test fun noEvidence_returnsNull() {
        assertNull(ctx().affinity(listOf("a")))
    }

    @Test fun accumulates_andRanksDominantTopic() {
        val c = ctx()
        c.recordTerms(listOf(listOf("celula"), listOf("celula"), listOf("celula"), listOf("genetica")))
        val inTopic = c.affinity(listOf("celula"))!!
        val other = c.affinity(listOf("genetica"))!!
        assertEquals(1.0, inTopic.value, 1e-9)
        assertEquals(1.0, inTopic.strength, 1e-9)
        assertEquals(1.0 / 3.0, other.value, 1e-9)
    }

    @Test fun unseenTopic_isFlooredNotZero() {
        val c = ctx()
        c.recordTerms(List(3) { listOf("celula") })
        assertEquals(LessonContext.LESSON_FLOOR, c.affinity(listOf("digestion"))!!.value, 1e-9)
    }

    @Test fun sharedTerm_splitsWeightAcrossClusters() {
        val c = ctx()
        c.recordTerms(List(3) { listOf("a", "b") })
        assertEquals(1.0, c.affinity(listOf("a"))!!.value, 1e-9) // a y b empatan => ambos dominantes
        assertEquals(1.5, c.snapshot()["a"]!!, 1e-9)
    }

    @Test fun decay_halvesPerHalfLife_andEventuallyForgets() {
        val c = ctx()
        c.recordTerms(List(4) { listOf("celula") })
        now += half
        assertEquals(2.0 / 3.0, c.affinity(listOf("celula"))!!.strength, 1e-9) // evidencia 4 -> 2 de 3
        now += 10 * half
        assertNull("Tras ~10 vidas medias la clase 'olvida' el tema", c.affinity(listOf("celula")))
    }

    @Test fun newTopicOvertakesOldOneAfterSwitch() {
        val c = ctx()
        c.recordTerms(List(3) { listOf("celula") })
        now += 3 * half
        c.recordTerms(List(3) { listOf("genetica") })
        assertTrue(c.affinity(listOf("genetica"))!!.value > c.affinity(listOf("celula"))!!.value)
        assertEquals(1.0, c.affinity(listOf("genetica"))!!.value, 1e-9)
    }

    @Test fun reset_clearsMemory() {
        val c = ctx()
        c.recordTerms(List(3) { listOf("celula") })
        c.reset()
        assertNull(c.affinity(listOf("celula")))
    }

    @Test fun realTimeBudget_hotPathIsMicroseconds() {
        val real = LessonContext()
        val ids = (1..12).map { "cluster$it" }
        val t0 = System.nanoTime()
        repeat(20_000) { i ->
            real.recordTerms(listOf(listOf(ids[i % 12]), listOf(ids[(i + 1) % 12])))
            real.affinity(listOf(ids[(i + 3) % 12], ids[(i + 5) % 12]))
        }
        val perCallUs = (System.nanoTime() - t0) / 1_000.0 / 20_000
        // Presupuesto de tiempo real: la memoria debe costar << 1 ms por palabra (holgado para no ser flaky)
        assertTrue("record+affinity tardó $perCallUs µs", perCallUs < 200.0)
    }
}
