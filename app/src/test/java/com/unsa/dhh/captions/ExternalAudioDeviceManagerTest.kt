package com.unsa.dhh.captions

import android.media.AudioDeviceInfo
import com.unsa.dhh.captions.service.AudioInputSource
import com.unsa.dhh.captions.service.ExternalAudioDeviceManager
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para la máquina de estados de fuentes de audio
 * y fallback automático de micrófono inalámbrico USB (SX26 / Maxtron) a micrófono interno.
 */
class ExternalAudioDeviceManagerTest {

    @Test
    fun testAudioInputSource_enumDisplayNames() {
        assertEquals("Micrófono del teléfono", AudioInputSource.INTERNAL_MIC.displayName)
        assertEquals(
            "Micrófono inalámbrico de solapa (SX26/Maxtron 2.4 GHz)",
            AudioInputSource.WIRELESS_USB_MIC.displayName
        )
        assertEquals(
            "Micrófono alámbrico auxiliar (3.5mm)",
            AudioInputSource.WIRED_HEADSET_MIC.displayName
        )
    }

    @Test
    fun testAudioFallback_simulatedTransition() {
        var currentSource = AudioInputSource.INTERNAL_MIC
        var isExternalConnected = false

        // 1. Simulación: Dongle receptor USB-C conectado
        fun onUsbAttached() {
            currentSource = AudioInputSource.WIRELESS_USB_MIC
            isExternalConnected = true
        }

        // 2. Simulación: Dongle desconectado intempestivamente
        fun onUsbDetached() {
            currentSource = AudioInputSource.INTERNAL_MIC
            isExternalConnected = false
        }

        assertEquals(AudioInputSource.INTERNAL_MIC, currentSource)
        assertFalse(isExternalConnected)

        onUsbAttached()
        assertEquals(AudioInputSource.WIRELESS_USB_MIC, currentSource)
        assertTrue(isExternalConnected)

        onUsbDetached()
        assertEquals(AudioInputSource.INTERNAL_MIC, currentSource)
        assertFalse(isExternalConnected)
    }

    @Test
    fun classifyInputs_usbDongleDetected() {
        val types = listOf(AudioDeviceInfo.TYPE_BUILTIN_MIC, AudioDeviceInfo.TYPE_USB_DEVICE)
        assertEquals(AudioInputSource.WIRELESS_USB_MIC, ExternalAudioDeviceManager.classifyInputs(types))
        assertEquals(
            AudioInputSource.WIRELESS_USB_MIC,
            ExternalAudioDeviceManager.classifyInputs(listOf(AudioDeviceInfo.TYPE_USB_HEADSET))
        )
    }

    @Test
    fun classifyInputs_usbWinsOverWiredRegardlessOfOrder() {
        val a = listOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE)
        val b = a.reversed()
        assertEquals(AudioInputSource.WIRELESS_USB_MIC, ExternalAudioDeviceManager.classifyInputs(a))
        assertEquals(AudioInputSource.WIRELESS_USB_MIC, ExternalAudioDeviceManager.classifyInputs(b))
    }

    @Test
    fun classifyInputs_fallbackToInternalWhenDongleRemoved() {
        val withDongle = listOf(AudioDeviceInfo.TYPE_BUILTIN_MIC, AudioDeviceInfo.TYPE_USB_DEVICE)
        val afterRemoval = listOf(AudioDeviceInfo.TYPE_BUILTIN_MIC)
        assertEquals(AudioInputSource.WIRELESS_USB_MIC, ExternalAudioDeviceManager.classifyInputs(withDongle))
        assertEquals(AudioInputSource.INTERNAL_MIC, ExternalAudioDeviceManager.classifyInputs(afterRemoval))
        assertEquals(AudioInputSource.INTERNAL_MIC, ExternalAudioDeviceManager.classifyInputs(emptyList()))
    }

    @Test
    fun classifyInputs_wiredHeadsetOnly() {
        assertEquals(
            AudioInputSource.WIRED_HEADSET_MIC,
            ExternalAudioDeviceManager.classifyInputs(listOf(AudioDeviceInfo.TYPE_WIRED_HEADSET))
        )
    }
}
