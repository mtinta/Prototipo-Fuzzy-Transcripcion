package com.unsa.dhh.captions

import com.google.gson.Gson
import com.unsa.dhh.captions.domain.fuzzy.JFuzzyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Paridad Kotlin <-> Python: el benchmark de la tesis (Python) y la app (Kotlin) deben calcular lo mismo.
 * Referencia generada con `python3 evaluation/generar_paridad_mamdani.py`.
 */
class MamdaniParityTest {
    private val engine = JFuzzyEngine()

    @Test fun kotlinEngineMatchesPythonReferenceOnGrid() {
        val f = listOf("../evaluation/paridad_mamdani.json", "evaluation/paridad_mamdani.json").map(::File).first { it.exists() }
        val rows = Gson().fromJson(f.readText(), Array<DoubleArray>::class.java)
        assertTrue(rows.size > 1000)
        for (r in rows) {
            assertEquals("C=${r[0]} D=${r[1]} S=${r[2]}", r[3], engine.evaluate(r[0], r[1], r[2]), 1e-9)
        }
    }

    @Test fun perfectMatchAndPerfectContext_isNotZero() {
        // Regresión: en los bordes C/D/S = 1.0 la pertenencia de los hombros derechos debe ser 1, no 0
        assertTrue(engine.evaluate(0.3, 1.0, 1.0) >= 0.65)
        assertTrue(engine.evaluate(0.0, 1.0, 1.0) >= 0.65)
    }
}
