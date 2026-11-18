package com.unsa.dhh.captions

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.unsa.dhh.captions.presentation.TeleprompterScreen
import com.unsa.dhh.captions.presentation.theme.WCAGHighContrastTheme
import com.unsa.dhh.captions.presentation.viewmodel.CaptionsViewModel
import com.unsa.dhh.captions.service.AudioForegroundService

class MainActivity : ComponentActivity() {

    private val viewModel: CaptionsViewModel by viewModels()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            startAudioService()
        } else {
            Toast.makeText(
                this,
                "Se requiere permiso de micrófono para la transcripción en aula.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            WCAGHighContrastTheme {
                TeleprompterScreen(viewModel = viewModel)
            }
        }

        // Pantalla siempre encendida mientras se transcribe (clases de 1 h, con silencios) y modo alumno
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.studentLock, viewModel.isListening) { locked, listening -> locked to listening }
                    .collect { (locked, listening) ->
                        setKeepScreenOn(locked || listening)
                        applyStudentLock(locked)
                    }
            }
        }
    }

    private fun setKeepScreenOn(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private var pinCheckJob: Job? = null

    /**
     * Modo alumno: horizontal fija, sin barras del sistema y con la app FIJADA (screen pinning: Inicio, Recientes
     * y las notificaciones dejan de funcionar). Sin ser propietaria del dispositivo, Android pide confirmar la
     * fijación cada vez ("¿Fijar app?"). Para que el alumno no pueda desfijarla, en Ajustes del teléfono hay que
     * activar "Pedir PIN/patrón para dejar de fijar" (ver checklist del piloto).
     */
    private fun applyStudentLock(locked: Boolean) {
        val bars = WindowCompat.getInsetsController(window, window.decorView)
        if (locked) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            bars.hide(WindowInsetsCompat.Type.systemBars())
            if (!isPinned()) {
                try {
                    startLockTask()
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo fijar la pantalla: ${e.message}")
                }
                pinCheckJob?.cancel()
                pinCheckJob = lifecycleScope.launch {
                    delay(PIN_CHECK_DELAY_MS)
                    if (viewModel.studentLock.value && !isPinned()) {
                        Toast.makeText(
                            this@MainActivity,
                            "La pantalla NO quedó fijada: el alumno podría salir de la app. " +
                                "Desbloquee y vuelva a bloquear, y acepte \"Fijar\" en el aviso del sistema.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            pinCheckJob?.cancel()
            if (isPinned()) {
                try {
                    stopLockTask()
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo desfijar la pantalla: ${e.message}")
                }
            }
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            bars.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * Respaldo del BackHandler de Compose: en modo alumno la tecla/gesto Atrás se descarta aquí, antes de llegar a
     * cualquier pantalla, por si alguna capa del sistema (HiOS) la entrega sin pasar por Compose.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (viewModel.studentLock.value && event.keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.dispatchKeyEvent(event)
    }

    /** Tras el aviso "Fijar app" u otro diálogo del sistema, las barras vuelven a aparecer: se ocultan de nuevo. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && viewModel.studentLock.value) {
            WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun isPinned(): Boolean =
        getSystemService(ActivityManager::class.java)?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            startAudioService()
        }
    }

    private fun startAudioService() {
        val serviceIntent = Intent(this, AudioForegroundService::class.java).apply {
            action = AudioForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onDestroy() {
        val stopIntent = Intent(this, AudioForegroundService::class.java).apply {
            action = AudioForegroundService.ACTION_STOP
        }
        startService(stopIntent)
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MainActivity"
        const val PIN_CHECK_DELAY_MS = 8000L
    }
}
