package com.unsa.dhh.captions.domain.fuzzy

import kotlin.math.max
import kotlin.math.min

/**
 * Funciones de pertenencia difusas (triangulares y trapezoidales).
 */
sealed class MembershipFunction {
    abstract fun calculateMembership(x: Double): Double

    /**
     * Función trapezoidal definida por los puntos [a, b, c, d].
     * Si a == b, el flanco izquierdo es vertical.
     * Si c == d, el flanco derecho es vertical.
     */
    data class Trapezoidal(val a: Double, val b: Double, val c: Double, val d: Double) : MembershipFunction() {
        override fun calculateMembership(x: Double): Double {
            if (a == b && x <= b) return 1.0
            if (c == d && x >= c) return 1.0
            return when {
                x <= a || x >= d -> 0.0
                x in b..c -> 1.0
                x > a && x < b -> (x - a) / (b - a)
                x > c && x < d -> (d - x) / (d - c)
                else -> 0.0
            }.coerceIn(0.0, 1.0)
        }
    }

    /**
     * Función triangular definida por los vértices [a, b, c].
     */
    data class Triangular(val a: Double, val b: Double, val c: Double) : MembershipFunction() {
        override fun calculateMembership(x: Double): Double {
            return when {
                x <= a || x >= c -> 0.0
                x == b -> 1.0
                x > a && x < b -> (x - a) / (b - a)
                x > b && x < c -> (c - x) / (c - b)
                else -> 0.0
            }.coerceIn(0.0, 1.0)
        }
    }
}
