package com.unsa.dhh.captions.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fuentes de entrada de audio para la captura pedagógica.
 */
enum class AudioInputSource(val displayName: String, val shortName: String) {
    INTERNAL_MIC("Micrófono del teléfono", "Mic. del teléfono"),
    WIRELESS_USB_MIC("Micrófono inalámbrico de solapa (SX26/Maxtron 2.4 GHz)", "SX26 inalámbrico"),
    WIRED_HEADSET_MIC("Micrófono alámbrico auxiliar (3.5mm)", "Mic. alámbrico")
}

/**
 * Módulo 1: Administrador de Dispositivos de Audio Externos.
 * Soporta micrófonos de solapa inalámbricos con receptor USB-C digital (SX26 Wireless, Maxtron 2.4 GHz)
 * y micrófonos auxiliares, garantizando fallback automático e instantáneo al micrófono interno
 * ante cualquier desconexión física del receptor.
 */
class ExternalAudioDeviceManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentSource = MutableStateFlow(AudioInputSource.INTERNAL_MIC)
    val currentSource: StateFlow<AudioInputSource> = _currentSource.asStateFlow()

    private val _isExternalMicConnected = MutableStateFlow(false)
    val isExternalMicConnected: StateFlow<Boolean> = _isExternalMicConnected.asStateFlow()

    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var isReceiverRegistered = false

    private val usbPlugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Log.d(TAG, "Broadcast recibido: $action")
            when (action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED,
                Intent.ACTION_HEADSET_PLUG -> {
                    checkConnectedAudioDevices()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.w(TAG, "Receptor USB desconectado físicamente. Activando fallback a micrófono interno.")
                    fallbackToInternalMic()
                }
            }
        }
    }

    /**
     * Inicia la monitorización continua de dispositivos de audio.
     */
    fun startMonitoring() {
        Log.i(TAG, "Iniciando monitorización de dispositivos de audio (SX26 / Maxtron / Interno)...")
        audioManager.mode = AudioManager.MODE_NORMAL

        // 1. Registro mediante AudioDeviceCallback nativo (API 23+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    Log.i(TAG, "Dispositivo de audio conectado al teléfono.")
                    checkConnectedAudioDevices()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    Log.w(TAG, "Dispositivo de audio desconectado del teléfono.")
                    checkConnectedAudioDevices()
                }
            }
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        }

        // 2. Registro de BroadcastReceiver para intents de USB y plug
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(Intent.ACTION_HEADSET_PLUG)
            }
            try {
                context.registerReceiver(usbPlugReceiver, filter)
                isReceiverRegistered = true
            } catch (e: Exception) {
                Log.e(TAG, "Error registrando usbPlugReceiver: ${e.message}", e)
            }
        }

        // Evaluar estado actual al iniciar
        checkConnectedAudioDevices()
    }

    /**
     * Detiene la monitorización y desregistra los callbacks.
     */
    fun stopMonitoring() {
        Log.i(TAG, "Deteniendo monitorización de dispositivos de audio...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            audioDeviceCallback = null
        }
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(usbPlugReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error desregistrando usbPlugReceiver: ${e.message}", e)
            }
            isReceiverRegistered = false
        }
    }

    /**
     * Inspecciona todos los dispositivos de entrada conectados actualmente al dispositivo.
     */
    fun checkConnectedAudioDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val source = classifyInputs(devices.map { it.type })
            devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
                ?.let { Log.i(TAG, "Receptor USB-C detectado (SX26 / Maxtron 2.4 GHz): ${it.productName}") }
            val detectedExternal = source != AudioInputSource.INTERNAL_MIC
            val externalType = source

            if (detectedExternal) {
                _currentSource.value = externalType
                _isExternalMicConnected.value = true
                Log.i(TAG, "Fuente de audio activa: ${externalType.displayName}")
            } else {
                fallbackToInternalMic()
            }
        } else {
            // En versiones anteriores, fallback seguro a micrófono interno
            fallbackToInternalMic()
        }
    }

    /**
     * Dispositivo de entrada que debe usar una captura propia (motor Vosk con AudioRecord.setPreferredDevice),
     * con la misma prioridad que [classifyInputs]: receptor USB > alámbrico 3.5 mm. null = micrófono interno.
     */
    fun preferredInputDevice(): AudioDeviceInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        return devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
    }

    /**
     * Conmuta de inmediato al micrófono interno del teléfono.
     */
    fun fallbackToInternalMic() {
        audioManager.mode = AudioManager.MODE_NORMAL
        _currentSource.value = AudioInputSource.INTERNAL_MIC
        _isExternalMicConnected.value = false
        Log.i(TAG, "Micrófono interno del teléfono activado (Modo Normal).")
    }

    companion object {
        private const val TAG = "ExternalAudioDevMgr"

        /**
         * Decide la fuente activa a partir de los tipos de entrada presentes (función pura, testeable).
         * Prioridad: receptor USB inalámbrico > micrófono alámbrico 3.5 mm > micrófono interno.
         * Antes el resultado dependía del orden en que Android listaba los dispositivos.
         */
        fun classifyInputs(inputTypes: List<Int>): AudioInputSource = when {
            inputTypes.any { it == AudioDeviceInfo.TYPE_USB_DEVICE || it == AudioDeviceInfo.TYPE_USB_HEADSET } ->
                AudioInputSource.WIRELESS_USB_MIC
            inputTypes.any { it == AudioDeviceInfo.TYPE_WIRED_HEADSET } -> AudioInputSource.WIRED_HEADSET_MIC
            else -> AudioInputSource.INTERNAL_MIC
        }
    }
}
