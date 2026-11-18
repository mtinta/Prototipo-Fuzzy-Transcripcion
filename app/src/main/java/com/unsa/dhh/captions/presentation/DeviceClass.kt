package com.unsa.dhh.captions.presentation

import android.content.res.Configuration

/**
 * Clase de dispositivo para adaptar la interfaz a teléfonos y tabletas.
 *
 * Usa el ancho mínimo de pantalla (smallestScreenWidthDp), que NO cambia al girar el equipo. Antes se usaba
 * screenWidthDp: un teléfono en horizontal (~800 dp de ancho) se trataba como tableta y recibía letra de tableta.
 * Umbral de 600 dp: convención de Android para distinguir teléfonos de tabletas de 7" o más.
 */
object DeviceClass {
    const val COMPACT_MAX_DP = 600

    /** true = teléfono (pantalla compacta); false = tableta. */
    fun isCompact(configuration: Configuration): Boolean =
        configuration.smallestScreenWidthDp in 1 until COMPACT_MAX_DP

    /** Etiqueta que se guarda en los registros de evaluación. */
    fun label(configuration: Configuration): String = if (isCompact(configuration)) "telefono" else "tableta"
}
