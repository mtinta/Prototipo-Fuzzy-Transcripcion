package com.unsa.dhh.captions.domain.asr

/**
 * Política de recuperación ante errores de `SpeechRecognizer` (lógica pura, sin dependencias de Android,
 * por eso es verificable con pruebas unitarias en JVM).
 *
 * El reconocedor se configura con una escalera de perfiles ordenados de más a menos preferible
 * (p. ej. local del equipo -> sin conexión preferido -> en línea -> español de Latinoamérica).
 * Esta política decide, ante cada error, si basta con reintentar, hay que recrear el reconocedor,
 * conviene pasar al siguiente perfil o es inútil seguir intentando.
 */
class RecognitionFallbackPolicy(
    private val profileCount: Int,
    private val maxClientErrorsPerProfile: Int = 3
) {
    sealed class Action {
        /** Reintentar con el mismo reconocedor (silencios, red intermitente). */
        object Restart : Action()
        /** Destruir y recrear el reconocedor y reintentar (errores del cliente/audio/ocupado). */
        object Recreate : Action()
        /** Pasar al siguiente perfil de la escalera. */
        data class SwitchProfile(val index: Int) : Action()
        /** No hay nada más que probar: detener y avisar. */
        object GiveUp : Action()
    }

    var profileIndex: Int = 0
        private set
    private var clientErrors = 0

    /** Un resultado válido demuestra que el perfil actual funciona. */
    fun onSuccess() {
        clientErrors = 0
    }

    fun reset() {
        profileIndex = 0
        clientErrors = 0
    }

    fun onError(code: Int): Action = when (code) {
        // Idioma no soportado / paquete no disponible: reintentar no sirve, hay que cambiar de perfil
        ERROR_LANGUAGE_NOT_SUPPORTED, ERROR_LANGUAGE_UNAVAILABLE -> advance()
        // Sin permiso de micrófono: ningún perfil lo arregla
        ERROR_INSUFFICIENT_PERMISSIONS -> Action.GiveUp
        // Errores del cliente: típicos de reinicios solapados o cambio de fuente de audio (USB)
        ERROR_AUDIO, ERROR_CLIENT, ERROR_RECOGNIZER_BUSY, ERROR_SERVER_DISCONNECTED -> {
            clientErrors++
            if (clientErrors >= maxClientErrorsPerProfile) advance() else Action.Recreate
        }
        // Silencio, sin coincidencia, red intermitente, servidor: reintentar
        else -> Action.Restart
    }

    private fun advance(): Action {
        clientErrors = 0
        return if (profileIndex + 1 < profileCount) {
            profileIndex++
            Action.SwitchProfile(profileIndex)
        } else {
            Action.GiveUp
        }
    }

    companion object {
        // Códigos de android.speech.SpeechRecognizer (los últimos existen desde API 31; minSdk es 26)
        const val ERROR_AUDIO = 3
        const val ERROR_CLIENT = 5
        const val ERROR_INSUFFICIENT_PERMISSIONS = 9
        const val ERROR_RECOGNIZER_BUSY = 8
        const val ERROR_SERVER_DISCONNECTED = 11
        const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        const val ERROR_LANGUAGE_UNAVAILABLE = 13
    }
}
