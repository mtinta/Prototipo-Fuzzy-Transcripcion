package com.unsa.dhh.captions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
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
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
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
}
