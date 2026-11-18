package com.unsa.dhh.captions.domain.fuzzy

import android.content.Context
import android.util.Log
import kotlin.math.max
import kotlin.math.min

/**
 * Módulo 3: Motor de Inferencia Difusa Mamdani con Defuzzificación por Centroide (COG).
 * Implementa el razonamiento difuso para determinar el Grado de Justificación de Corrección (y* in [0.0, 1.0])
 * a partir de tres variables lingüísticas:
 * 1. Confianza Acústica (C)
 * 2. Similitud Léxico-Fonética (D)
 * 3. Coherencia Semántica de Dominio / Cluster (S)
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

    // Funciones de membresía para la entrada Coherencia Semántica S in [0.0, 1.0]
    private val semBaja = MembershipFunction.Trapezoidal(0.0, 0.0, 0.25, 0.45)
    private val semMedia = MembershipFunction.Triangular(0.30, 0.50, 0.70)
    private val semAlta = MembershipFunction.Trapezoidal(0.55, 0.75, 1.0, 1.0)

    // Funciones de pertenencia para la salida Justificación y* in [0.0, 1.0]
    private val justBaja = MembershipFunction.Trapezoidal(0.0, 0.0, 0.30, 0.50)
    private val justMedia = MembershipFunction.Triangular(0.35, 0.55, 0.75)
    private val justAlta = MembershipFunction.Trapezoidal(0.60, 0.80, 1.0, 1.0)

    /**
     * Evalúa las 27 reglas difusas y calcula el valor nítido y* usando el método del Centroide (COG).
     *
     * @param confianza Acústica C in [0.0, 1.0]
     * @param similitud Léxico-fonética D in [0.0, 1.0]
     * @param semantica Coherencia contextual por clusters S in [0.0, 1.0] (por defecto 0.50)
     * @return Grado de Justificación y* in [0.0, 1.0]
     */
    fun evaluate(confianza: Double, similitud: Double, semantica: Double = 0.50): Double {
        val c = confianza.coerceIn(0.0, 1.0)
        val d = similitud.coerceIn(0.0, 1.0)
        val s = semantica.coerceIn(0.0, 1.0)

        // 1. Fuzzificación de entradas
        val muCBaja = confBaja.calculateMembership(c)
        val muCMedia = confMedia.calculateMembership(c)
        val muCAlta = confAlta.calculateMembership(c)

        val muDBaja = simBaja.calculateMembership(d)
        val muDMedia = simMedia.calculateMembership(d)
        val muDAlta = simAlta.calculateMembership(d)

        val muSBaja = semBaja.calculateMembership(s)
        val muSMedia = semMedia.calculateMembership(s)
        val muSAlta = semAlta.calculateMembership(s)

        // 2. Activación de 27 Reglas Mamdani (Operador t-norma MIN: min(muC, muD, muS))
        // --- Grupo 1: C IS Baja ---
        val r1 = min(muCBaja, min(muDAlta, muSAlta))   // J Alta
        val r2 = min(muCBaja, min(muDAlta, muSMedia))  // J Alta
        val r3 = min(muCBaja, min(muDAlta, muSBaja))   // J Media
        val r4 = min(muCBaja, min(muDMedia, muSAlta))  // J Alta
        val r5 = min(muCBaja, min(muDMedia, muSMedia)) // J Media
        val r6 = min(muCBaja, min(muDMedia, muSBaja))  // J Baja
        val r7 = min(muCBaja, min(muDBaja, muSAlta))   // J Media
        val r8 = min(muCBaja, min(muDBaja, muSMedia))  // J Baja
        val r9 = min(muCBaja, min(muDBaja, muSBaja))   // J Baja

        // --- Grupo 2: C IS Media ---
        val r10 = min(muCMedia, min(muDAlta, muSAlta))  // J Alta
        val r11 = min(muCMedia, min(muDAlta, muSMedia)) // J Alta
        val r12 = min(muCMedia, min(muDAlta, muSBaja))  // J Media
        val r13 = min(muCMedia, min(muDMedia, muSAlta)) // J Alta
        val r14 = min(muCMedia, min(muDMedia, muSMedia))// J Media
        val r15 = min(muCMedia, min(muDMedia, muSBaja)) // J Baja
        val r16 = min(muCMedia, min(muDBaja, muSAlta))  // J Media
        val r17 = min(muCMedia, min(muDBaja, muSMedia)) // J Baja
        val r18 = min(muCMedia, min(muDBaja, muSBaja))  // J Baja

        // --- Grupo 3: C IS Alta ---
        val r19 = min(muCAlta, min(muDAlta, muSAlta))  // J Media
        val r20 = min(muCAlta, min(muDAlta, muSMedia)) // J Media
        val r21 = min(muCAlta, min(muDAlta, muSBaja))  // J Baja
        val r22 = min(muCAlta, min(muDMedia, muSAlta)) // J Media
        val r23 = min(muCAlta, min(muDMedia, muSMedia))// J Baja
        val r24 = min(muCAlta, min(muDMedia, muSBaja)) // J Baja
        val r25 = min(muCAlta, min(muDBaja, muSAlta))  // J Baja
        val r26 = min(muCAlta, min(muDBaja, muSMedia)) // J Baja
        val r27 = min(muCAlta, min(muDBaja, muSBaja))  // J Baja

        // Agregación de consecuentes por conjunto difuso (Operador s-norma MAX)
        val activaAlta = maxOf(r1, r2, r4, r10, r11, r13)
        val activaMedia = maxOf(r3, r5, r7, r12, r14, r16, r19, r20, r22)
        val activaBaja = maxOf(r6, r8, r9, r15, r17, r18, r21, r23, r24, r25, r26, r27)

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
