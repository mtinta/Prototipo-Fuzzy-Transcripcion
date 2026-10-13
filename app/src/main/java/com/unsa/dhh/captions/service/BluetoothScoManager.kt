package com.unsa.dhh.captions.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Módulo 1: Administrador de conexión de audio inalámbrico vía Bluetooth SCO.
 * Configura la captura de voz a través de Bluetooth SCO o mediante el micrófono del teléfono.
 * Garantiza fallback automático y soporte directo para el micrófono del dispositivo.
 */
class BluetoothScoManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isScoConnected = MutableStateFlow(false)
    val isScoConnected: StateFlow<Boolean> = _isScoConnected.asStateFlow()

    private val _scoAudioState = MutableStateFlow("MIC_TELEFONO")
    val scoAudioState: StateFlow<String> = _scoAudioState.asStateFlow()

    private var isReceiverRegistered = false

    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1) ?: -1
            when (state) {
                AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                    Log.d(TAG, "Bluetooth SCO conectado con éxito (16 kHz Mono).")
                    _isScoConnected.value = true
                    _scoAudioState.value = "CONECTADO_SCO"
                }
                AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                    Log.w(TAG, "Bluetooth SCO desconectado. Usando micrófono del teléfono.")
                    _isScoConnected.value = false
                    _scoAudioState.value = "MIC_TELEFONO"
                    fallbackToPhoneMic()
                }
                AudioManager.SCO_AUDIO_STATE_CONNECTING -> {
                    Log.d(TAG, "Conectando socket Bluetooth SCO...")
                    _scoAudioState.value = "CONECTANDO"
                }
                AudioManager.SCO_AUDIO_STATE_ERROR -> {
                    Log.e(TAG, "Error en socket Bluetooth SCO. Usando micrófono del teléfono.")
                    _isScoConnected.value = false
                    _scoAudioState.value = "MIC_TELEFONO"
                    fallbackToPhoneMic()
                }
            }
        }
    }

    /**
     * Activa directamente el micrófono del teléfono (modo de audio normal).
     */
    fun usePhoneMic() {
        try {
            audioManager.isBluetoothScoOn = false
            audioManager.stopBluetoothSco()
            audioManager.mode = AudioManager.MODE_NORMAL
            _isScoConnected.value = false
            _scoAudioState.value = "MIC_TELEFONO"
            Log.i(TAG, "Micrófono del teléfono activado.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al activar el micrófono del teléfono: ${e.message}", e)
        }
    }

    /**
     * Inicia el ruteo de audio hacia el micrófono Bluetooth SCO de solapa del docente si está disponible.
     */
    fun startBluetoothSco() {
        try {
            if (!isReceiverRegistered) {
                val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
                context.registerReceiver(scoReceiver, filter)
                isReceiverRegistered = true
            }

            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
            Log.i(TAG, "Solicitud de inicio Bluetooth SCO enviada.")
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al iniciar Bluetooth SCO: ${e.message}", e)
            fallbackToPhoneMic()
        }
    }

    private fun fallbackToPhoneMic() {
        try {
            audioManager.isBluetoothScoOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.e(TAG, "Error en fallback a micrófono del teléfono: ${e.message}")
        }
    }

    /**
     * Detiene el canal Bluetooth SCO y restaura la configuración predeterminada del micrófono del teléfono.
     */
    fun stopBluetoothSco() {
        try {
            audioManager.isBluetoothScoOn = false
            audioManager.stopBluetoothSco()
            audioManager.mode = AudioManager.MODE_NORMAL

            if (isReceiverRegistered) {
                context.unregisterReceiver(scoReceiver)
                isReceiverRegistered = false
            }
            _isScoConnected.value = false
            _scoAudioState.value = "MIC_TELEFONO"
            Log.i(TAG, "Micrófono del teléfono restaurado correctamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al liberar Bluetooth SCO: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "BluetoothScoManager"
    }
}
