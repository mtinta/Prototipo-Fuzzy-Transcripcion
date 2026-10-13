package com.unsa.dhh.captions.domain.fuzzy

import android.content.Context
import android.util.Log
import kotlin.math.max
import kotlin.math.min

/**
 * Módulo 3: Motor de Inferencia Difusa Mamdani con Defuzzificación por Centroide (COG).
 * Implementa el razonamiento difuso para determinar el Grado de Justificación de Corrección (y* in [0.0, 1.0])
 * a partir de la Confianza Acústica (C) y la Similitud de Texto (D).
 */
class JFuzzyEngine(private val context: Context? = null) {

    // Funciones de membresía para la entrada Confianza C in [0.0, 1.0]
    private val confBaja = MembershipFunction.Trapezoidal(0.0, 0.0, 0.35, 0.60)
    private val confMedia = MembershipFunction.Triangular(0.35, 0.60, 0.80)
    private val confAlta = MembershipFunction.Trapezoidal(0.65, 0.85, 1.0, 1.0)

    // Funciones de membresía para la entrada Similitud D in [0.0, 1.0]
    private val simBaja = MembershipFunction.Trapezoidal(0.0, 0.0, 0.40, 0.65)
    private val simMedia = MembershipFunction.Triangular(0.45, 0.70, 0.85)
    private val simAlta = MembershipFunction.Trapezoidal(0.75, 0.90, 1.0, 1.0)

    // Funciones de pertenencia para la salida Justificación y* in [0.0, 1.0]
    private val justBaja = MembershipFunction.Trapezoidal(0.0, 0.0, 0.30, 0.50)
    private val justMedia = MembershipFunction.Triangular(0.35, 0.55, 0.75)
    private val justAlta = MembershipFunction.Trapezoidal(0.60, 0.80, 1.0, 1.0)

    /**
     * Evalúa las reglas difusas y calcula el valor nítido y* usando el método del Centroide (COG).
     *
     * @param confianza Acústica C in [0.0, 1.0]
     * @param similitud Léxico-fonética D in [0.0, 1.0]
     * @return Grado de Justificación y* in [0.0, 1.0]
     */
    fun evaluate(confianza: Double, similitud: Double): Double {
        val c = confianza.coerceIn(0.0, 1.0)
        val d = similitud.coerceIn(0.0, 1.0)

        // 1. Fuzzificación de entradas
        val muCBaja = confBaja.calculateMembership(c)
        val muCMedia = confMedia.calculateMembership(c)
        val muCAlta = confAlta.calculateMembership(c)

        val muSBaja = simBaja.calculateMembership(d)
        val muSMedia = simMedia.calculateMembership(d)
        val muSAlta = simAlta.calculateMembership(d)

        // 2. Activación de Reglas Mamdani (Operador t-norma MIN)
        // Regla 1: IF C IS Baja AND S IS Alta THEN J IS Alta
        val r1 = min(muCBaja, muSAlta)
        // Regla 2: IF C IS Baja AND S IS Media THEN J IS Alta
        val r2 = min(muCBaja, muSMedia)
        // Regla 3: IF C IS Baja AND S IS Baja THEN J IS Baja
        val r3 = min(muCBaja, muSBaja)
        // Regla 4: IF C IS Media AND S IS Alta THEN J IS Alta
        val r4 = min(muCMedia, muSAlta)
        // Regla 5: IF C IS Media AND S IS Media THEN J IS Media
        val r5 = min(muCMedia, muSMedia)
        // Regla 6: IF C IS Media AND S IS Baja THEN J IS Baja
        val r6 = min(muCMedia, muSBaja)
        // Regla 7: IF C IS Alta AND S IS Alta THEN J IS Media
        val r7 = min(muCAlta, muSAlta)
        // Regla 8: IF C IS Alta AND S IS Media THEN J IS Baja
        val r8 = min(muCAlta, muSMedia)
        // Regla 9: IF C IS Alta AND S IS Baja THEN J IS Baja
        val r9 = min(muCAlta, muSBaja)

        // Agregación de consecuentes por conjunto difuso (Operador s-norma MAX)
        val activaAlta = max(r1, max(r2, r4))
        val activaMedia = max(r5, r7)
        val activaBaja = max(r3, max(r6, max(r8, r9)))

        // 3. Defuzzificación por Centroide de Gravedad (COG)
        val steps = 100
        val stepSize = 1.0 / steps
        var numerator = 0.0
        var denominator = 0.0

        for (i in 0..steps) {
            val y = i * stepSize
            // Membresía recortada por cada regla para el punto y
            val muYAlta = min(activaAlta, justAlta.calculateMembership(y))
            val muYMedia = min(activaMedia, justMedia.calculateMembership(y))
            val muYBaja = min(activaBaja, justBaja.calculateMembership(y))

            // Agregación difusa total en y
            val muCombined = max(muYAlta, max(muYMedia, muYBaja))

            numerator += y * muCombined
            denominator += muCombined
        }

        val cog = if (denominator > 0.00001) {
            numerator / denominator
        } else {
            0.0
        }

        return cog.coerceIn(0.0, 1.0)
    }

    companion object {
        private const val TAG = "JFuzzyEngine"
    }
}
