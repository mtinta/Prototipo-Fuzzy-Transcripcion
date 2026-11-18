package com.unsa.dhh.captions.data.telemetry

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Escritor WAV (PCM 16 bits, mono) en vivo, sin dependencias de Android (se prueba en JVM).
 *
 * La cabecera se reescribe cada [headerUpdateBytes] bytes de audio: si la app se cierra de golpe a mitad de la
 * clase, el archivo sigue siendo un WAV válido con todo lo grabado hasta la última actualización (≤ 5 s perdidos
 * en la cabecera; las muestras ya escritas siguen en el archivo).
 */
class WavFileWriter(
    val file: File,
    private val sampleRate: Int,
    private val headerUpdateBytes: Long = sampleRate * BYTES_PER_SAMPLE * 5L
) : Closeable {

    private val raf = RandomAccessFile(file, "rw")
    private var scratch = ByteBuffer.allocate(0)
    private var bytesSinceHeader = 0L
    private var closed = false

    /** Bytes de audio escritos (sin la cabecera). */
    var dataBytes = 0L
        private set

    val sampleCount: Long get() = dataBytes / BYTES_PER_SAMPLE
    val durationSec: Double get() = sampleCount.toDouble() / sampleRate

    init {
        raf.setLength(0)
        raf.write(header(0))
    }

    fun write(samples: ShortArray, count: Int = samples.size) {
        check(!closed) { "WAV cerrado" }
        val n = count.coerceIn(0, samples.size)
        if (n == 0) return
        val bytes = n * BYTES_PER_SAMPLE
        if (scratch.capacity() < bytes) scratch = ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN)
        scratch.clear()
        for (i in 0 until n) scratch.putShort(samples[i])
        raf.write(scratch.array(), 0, bytes)
        dataBytes += bytes
        bytesSinceHeader += bytes
        if (bytesSinceHeader >= headerUpdateBytes) updateHeader()
    }

    /** Escribe en la cabecera el tamaño actual (RIFF y data) sin mover el final del archivo. */
    fun updateHeader() {
        val end = raf.filePointer
        raf.seek(0)
        raf.write(header(dataBytes))
        raf.seek(end)
        bytesSinceHeader = 0
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            updateHeader()
        } finally {
            raf.close()
        }
    }

    private fun header(dataLen: Long): ByteArray {
        val b = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray(Charsets.US_ASCII))
        b.putInt((36 + dataLen).coerceAtMost(UINT_MAX).toInt())
        b.put("WAVE".toByteArray(Charsets.US_ASCII))
        b.put("fmt ".toByteArray(Charsets.US_ASCII))
        b.putInt(16)                                   // tamaño del bloque fmt (PCM)
        b.putShort(1)                                  // formato PCM
        b.putShort(CHANNELS.toShort())
        b.putInt(sampleRate)
        b.putInt(sampleRate * CHANNELS * BYTES_PER_SAMPLE) // bytes por segundo
        b.putShort((CHANNELS * BYTES_PER_SAMPLE).toShort()) // alineación de bloque
        b.putShort(16)                                 // bits por muestra
        b.put("data".toByteArray(Charsets.US_ASCII))
        b.putInt(dataLen.coerceAtMost(UINT_MAX - 36).toInt())
        return b.array()
    }

    companion object {
        const val HEADER_BYTES = 44
        const val BYTES_PER_SAMPLE = 2
        private const val CHANNELS = 1
        private const val UINT_MAX = 0xFFFFFFFFL
    }
}
