package com.unsa.dhh.captions.domain.asr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Módulo 2: motor ASR local con Vosk (Kaldi), completamente sin conexión.
 *
 * Por qué Vosk: el reconocedor local de Android no entrega CONFIDENCE_SCORES (siempre 0.0, C fija en 0.65), con lo
 * que el modelo Mamdani quedaba reducido a las reglas 10–18. Vosk entrega la confianza C **por palabra** (posterior
 * del lattice). En la fase 0 (evaluation/vosk_fase0) separó palabras erróneas de correctas con AUROC 0.87.
 *
 * Diseño:
 *  - Captura propia con AudioRecord (16 kHz, mono, PCM 16 bits) y `setPreferredDevice` para usar el receptor USB
 *    (SX26) cuando está conectado. Un único hilo lee el micrófono y alimenta al reconocedor (Recognizer no es
 *    thread-safe); los resultados se publican en el hilo principal, en orden.
 *  - Flujo continuo: no hay reinicios entre enunciados. El fin de enunciado lo decide el endpointer del modelo
 *    (conf/model.conf: 0.5–2.0 s de silencio), igual que en la fase 0.
 *  - Parciales con C = [PARTIAL_CONFIDENCE] (igual que el motor de Android); los finales llevan la C real.
 *  - [audioSink] recibe cada búfer leído (el mismo audio que reconoce Vosk) para grabarlo en WAV.
 */
class VoskSpeechEngine(
    private val context: Context,
    /** Micrófono preferido (receptor USB / alámbrico). null = entrada por defecto del sistema. */
    private val preferredInputDevice: () -> AudioDeviceInfo? = { null },
    /** Recibe una copia del audio capturado (grabación del docente). null = no se graba. */
    private val audioSink: CaptureAudioSink? = null
) : SpeechEngine {

    private val _engineState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    override val engineState: StateFlow<SpeechState> = _engineState.asStateFlow()

    private val _transcriptionStream = MutableSharedFlow<RawTranscriptionResult>(replay = 0)
    override val transcriptionStream: SharedFlow<RawTranscriptionResult> = _transcriptionStream.asSharedFlow()

    private val _modeNotice = MutableStateFlow<String?>(null)
    override val modeNotice: StateFlow<String?> = _modeNotice.asStateFlow()

    private val resultSequence = AtomicLong(0)
    override fun latestResultSequence(): Long = resultSequence.get()

    /** Todo el estado de control vive en el hilo principal; el hilo de captura solo lee [capturing]. */
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile private var model: Model? = null
    /** El modelo nativo se libera una sola vez (lo puede hacer destroy() o el hilo de captura al salir). */
    private val modelClosed = AtomicBoolean(false)
    private var modelLoading = false
    private var modelFailed = false

    /** El usuario quiere estar escuchando (aunque la captura esté reiniciándose o el modelo cargando). */
    private var wantListening = false
    /** Hay que volver a abrir la captura cuando termine la actual (cambio de micrófono, reintento). */
    private var restartPending = false
    private var captureThread: Thread? = null
    private var restartJob: Job? = null
    private var consecutiveFailures = 0
    @Volatile private var destroyed = false

    /** Bandera del bucle de captura: false = terminar tras la lectura en curso (≤ 100 ms). */
    @Volatile private var capturing = false

    init {
        loadModel()
    }

    // ------------------------------------------------------------------ modelo

    private fun loadModel() {
        if (model != null || modelLoading) return
        modelLoading = true
        modelFailed = false
        _modeNotice.value = "Preparando el modelo de voz sin conexión…"
        // Copia assets/vosk-model-es a almacenamiento externo de la app (solo la primera vez: compara el archivo
        // uuid) y carga el modelo en un hilo propio; las devoluciones llegan al hilo principal.
        StorageService.unpack(
            context, MODEL_ASSET_DIR, MODEL_TARGET_DIR,
            { loaded: Model ->
                modelLoading = false
                if (destroyed) {
                    loaded.close()
                    return@unpack
                }
                model = loaded
                _modeNotice.value = null
                Log.i(TAG, "Modelo Vosk listo: $MODEL_NAME")
                if (wantListening) startCapture()
            },
            { e: IOException ->
                modelLoading = false
                modelFailed = true
                Log.e(TAG, "No se pudo cargar el modelo Vosk: ${e.message}", e)
                wantListening = false
                _modeNotice.value = "No se pudo cargar el modelo de voz Vosk (${e.message}). " +
                    "Reinstala la app o elige el motor de Android en el panel del investigador."
                _engineState.value = SpeechState.Error("Modelo de voz no disponible", ERROR_MODEL)
                _engineState.value = SpeechState.Idle
            }
        )
    }

    // ------------------------------------------------------------------ control (hilo principal)

    override fun startListening() {
        if (destroyed) return
        wantListening = true
        consecutiveFailures = 0
        restartJob?.cancel()
        when {
            model == null -> if (modelFailed || !modelLoading) loadModel() // arranca al terminar de cargar
            captureThread != null -> if (!capturing) restartPending = true  // aún cerrando la captura anterior
            else -> startCapture()
        }
    }

    override fun stopListening() {
        wantListening = false
        restartPending = false
        restartJob?.cancel()
        capturing = false
        if (captureThread == null) _engineState.value = SpeechState.Idle
        // Si hay captura, onCaptureEnded() publica Idle cuando el hilo entregue el último enunciado.
    }

    override fun restartForAudioSourceChange() {
        if (!wantListening || model == null) return
        Log.i(TAG, "Cambió la fuente de audio: reabriendo la captura")
        if (captureThread != null) {
            restartPending = true
            capturing = false
        } else {
            scheduleStart(AUDIO_SOURCE_CHANGE_DELAY_MS)
        }
    }

    override fun destroy() {
        destroyed = true
        wantListening = false
        restartPending = false
        restartJob?.cancel()
        capturing = false
        // Nunca cerrar el modelo con el reconocedor aún en uso: si el hilo sigue vivo, lo cierra él al salir
        val thread = captureThread
        if (thread == null || !thread.isAlive) closeModel()
        mainScope.cancel()
    }

    private fun closeModel() {
        if (modelClosed.compareAndSet(false, true)) model?.close()
        model = null
    }

    private fun scheduleStart(delayMs: Long) {
        restartJob?.cancel()
        restartJob = mainScope.launch {
            delay(delayMs)
            if (wantListening) startCapture()
        }
    }

    private fun startCapture() {
        val m = model ?: return
        if (captureThread != null || destroyed) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            wantListening = false
            _engineState.value = SpeechState.Error("Faltan permisos de micrófono", ERROR_INSUFFICIENT_PERMISSIONS)
            _engineState.value = SpeechState.Idle
            return
        }
        val device = preferredInputDevice()
        capturing = true
        captureThread = Thread({ captureLoop(m, device) }, "VoskCapture").apply { start() }
        _engineState.value = SpeechState.Listening
        Log.i(TAG, "Captura iniciada (entrada: ${device?.productName ?: "predeterminada del sistema"})")
    }

    /** Llamado en el hilo principal cuando termina el hilo de captura (por detención o por fallo). */
    private fun onCaptureEnded(failure: String?) {
        captureThread = null
        if (destroyed) return

        if (failure != null && wantListening) {
            consecutiveFailures++
            if (consecutiveFailures <= MAX_CONSECUTIVE_FAILURES) {
                // Transitorio (micrófono ocupado, receptor USB recién conectado...): reintento silencioso
                Log.w(TAG, "Fallo de captura ($failure); reintento $consecutiveFailures/$MAX_CONSECUTIVE_FAILURES")
                restartPending = false
                scheduleStart(RETRY_DELAY_MS * consecutiveFailures)
            } else {
                wantListening = false
                _modeNotice.value = "No se pudo usar el micrófono ($failure). Revisa que ninguna otra app lo esté usando."
                _engineState.value = SpeechState.Error(failure, ERROR_AUDIO)
                _engineState.value = SpeechState.Idle
            }
            return
        }

        if (restartPending && wantListening) {
            restartPending = false
            scheduleStart(AUDIO_SOURCE_CHANGE_DELAY_MS)
            return
        }
        restartPending = false
        if (!wantListening) _engineState.value = SpeechState.Idle
    }

    // ------------------------------------------------------------------ captura (hilo VoskCapture)

    /** Datos del enunciado en curso, solo los toca el hilo de captura. */
    private class UtteranceTracker {
        var lastPartial = ""
        var firstPartialAtMs: Long? = null
        fun reset() {
            lastPartial = ""
            firstPartialAtMs = null
        }
    }

    @SuppressLint("MissingPermission") // se comprueba en startCapture()
    private fun captureLoop(m: Model, device: AudioDeviceInfo?) {
        var record: AudioRecord? = null
        var recognizer: Recognizer? = null
        var failure: String? = null
        try {
            val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuffer <= 0) throw IllegalStateException("el micrófono no admite 16 kHz mono")
            record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer * 2, SAMPLE_RATE) // ≥ 0.5 s de margen
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) throw IllegalStateException("no se pudo abrir el micrófono")
            if (device != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) record.setPreferredDevice(device)

            recognizer = Recognizer(m, SAMPLE_RATE.toFloat()).apply { setWords(true) }
            val clock = AudioClock(SAMPLE_RATE)
            val utterance = UtteranceTracker()
            val buffer = ShortArray(SAMPLE_RATE / 10) // 100 ms

            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw IllegalStateException("el micrófono está ocupado por otra app")
            }

            var peak = 0
            var samplesSinceLevelLog = 0
            while (capturing) {
                val n = record.read(buffer, 0, buffer.size)
                if (n < 0) throw IllegalStateException("error de lectura del micrófono ($n)")
                if (n == 0) continue
                val readAtMs = SystemClock.elapsedRealtime()
                clock.onRead(n, readAtMs)
                audioSink?.onAudio(buffer, n, SAMPLE_RATE, readAtMs)
                // Diagnóstico: pico de entrada cada 5 s (0 = silencio digital: el micrófono no entrega audio)
                for (i in 0 until n) peak = maxOf(peak, kotlin.math.abs(buffer[i].toInt()))
                samplesSinceLevelLog += n
                if (samplesSinceLevelLog >= SAMPLE_RATE * 5) {
                    Log.i(TAG, "NIVEL pico=${"%.1f".format(peak * 100.0 / Short.MAX_VALUE)}% de escala")
                    peak = 0
                    samplesSinceLevelLog = 0
                }
                if (recognizer.acceptWaveForm(buffer, n)) {
                    publishFinal(recognizer.result, clock, utterance)
                } else {
                    publishPartial(recognizer.partialResult, utterance)
                }
            }
            // Detención: entregar el enunciado que estaba en curso
            publishFinal(recognizer.finalResult, clock, utterance)
        } catch (e: Exception) {
            failure = e.message ?: e.javaClass.simpleName
            Log.e(TAG, "Captura Vosk interrumpida: $failure", e)
        } finally {
            try { record?.stop() } catch (_: Exception) {}
            record?.release()
            recognizer?.close()
            audioSink?.onCaptureEnded()
            if (destroyed) closeModel()
        }
        mainScope.launch { onCaptureEnded(failure) }
    }

    private fun publishPartial(json: String, utterance: UtteranceTracker) {
        val text = VoskResultParser.parsePartial(json)
        if (text.isEmpty() || text == utterance.lastPartial) return // Vosk repite el parcial en cada búfer
        utterance.lastPartial = text
        if (utterance.firstPartialAtMs == null) utterance.firstPartialAtMs = SystemClock.elapsedRealtime()
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val result = RawTranscriptionResult(
            rawText = text,
            words = words,
            wordConfidences = words.map { PARTIAL_CONFIDENCE },
            isFinal = false,
            asrProfile = PROFILE_LABEL,
            sequence = resultSequence.incrementAndGet()
        )
        mainScope.launch {
            _engineState.value = SpeechState.PartialText(text)
            _transcriptionStream.emit(result)
        }
    }

    private fun publishFinal(json: String, clock: AudioClock, utterance: UtteranceTracker) {
        val resultsAtMs = SystemClock.elapsedRealtime()
        val firstPartialAtMs = utterance.firstPartialAtMs
        utterance.reset()
        val words = VoskResultParser.parseFinal(json)
        if (words.isEmpty()) return // silencio o ruido sin palabras

        val text = words.joinToString(" ") { it.word }
        val confidences = words.map { it.confidence }
        // Evidencia ASR-02: C por palabra tal como la entrega Vosk. Filtrar: adb logcat -s VoskSpeechEngine:I | grep ASR02
        Log.i(TAG, "ASR02 perfil=$PROFILE_LABEL n=${words.size} confianzas=$confidences texto=\"$text\"")
        val result = RawTranscriptionResult(
            rawText = text,
            words = words.map { it.word },
            wordConfidences = confidences,
            isFinal = true,
            rawConfidences = confidences,
            asrProfile = PROFILE_LABEL,
            beginOfSpeechAtMs = clock.toElapsedRealtime(words.first().startSec),
            firstPartialAtMs = firstPartialAtMs,
            endOfSpeechAtMs = clock.toElapsedRealtime(words.last().endSec),
            resultsAtMs = resultsAtMs,
            sequence = resultSequence.incrementAndGet()
        )
        mainScope.launch {
            consecutiveFailures = 0
            _engineState.value = SpeechState.FinalText(text, confidences)
            _transcriptionStream.emit(result)
            if (wantListening && captureThread != null) _engineState.value = SpeechState.Listening
        }
    }

    companion object {
        private const val TAG = "VoskSpeechEngine"
        const val SAMPLE_RATE = 16000

        /** Carpeta del modelo dentro de assets/ (debe contener el archivo `uuid`) y destino al descomprimirlo. */
        const val MODEL_ASSET_DIR = "vosk-model-es"
        const val MODEL_TARGET_DIR = "vosk-model"
        const val MODEL_NAME = "vosk-model-small-es-0.42"
        const val PROFILE_LABEL = "Vosk local ($MODEL_NAME)"

        /** C de los parciales (sin lattice todavía): la misma que usaba el motor de Android. */
        const val PARTIAL_CONFIDENCE = 0.60f

        private const val MAX_CONSECUTIVE_FAILURES = 3
        private const val RETRY_DELAY_MS = 500L
        private const val AUDIO_SOURCE_CHANGE_DELAY_MS = 400L

        // Mismos códigos que android.speech.SpeechRecognizer, para que el ViewModel los trate igual
        const val ERROR_AUDIO = 3
        const val ERROR_INSUFFICIENT_PERMISSIONS = 9
        const val ERROR_MODEL = -2
    }
}
