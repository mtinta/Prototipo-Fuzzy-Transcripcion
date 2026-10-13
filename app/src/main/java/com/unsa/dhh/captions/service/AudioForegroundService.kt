package com.unsa.dhh.captions.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.unsa.dhh.captions.MainActivity
import com.unsa.dhh.captions.R

/**
 * Módulo 1: Servicio en primer plano persistente en Android.
 * Mantiene la sesión de transcripción activa durante clases prolongadas (45 a 90 min)
 * sin ser interrumpido por las políticas de suspensión o administración de energía.
 */
class AudioForegroundService : Service() {

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    lateinit var bluetoothScoManager: BluetoothScoManager
        private set

    inner class LocalBinder : Binder() {
        fun getService(): AudioForegroundService = this@AudioForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creando AudioForegroundService...")
        bluetoothScoManager = BluetoothScoManager(applicationContext)

        // Adquirir WakeLock para evitar que el CPU duerma durante la clase
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DHHCaptions::AudioRecordingWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServiceExecution()
            ACTION_STOP -> stopServiceExecution()
        }
        return START_STICKY
    }

    private fun startServiceExecution() {
        Log.i(TAG, "Iniciando Foreground Service para captura de micrófono...")
        createNotificationChannel()

        val notification = buildPersistentNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        wakeLock?.acquire(90 * 60 * 1000L) // Hasta 90 minutos de clase
        bluetoothScoManager.usePhoneMic()
    }

    private fun stopServiceExecution() {
        Log.i(TAG, "Deteniendo Foreground Service y liberando recursos...")
        bluetoothScoManager.stopBluetoothSco()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildPersistentNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DHH Captions - Asistencia en Aula")
            .setContentText("Captura de audio y transcripción activa para estudiante.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Transcripción DHH",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal de notificación para el servicio de captura y subtitulado en vivo."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopServiceExecution()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AudioForegroundService"
        private const val CHANNEL_ID = "dhh_captions_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.unsa.dhh.captions.START_AUDIO_SERVICE"
        const val ACTION_STOP = "com.unsa.dhh.captions.STOP_AUDIO_SERVICE"
    }
}
