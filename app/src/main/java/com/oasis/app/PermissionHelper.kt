package com.oasis.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHelper(
    private val activity: Activity,
    private val ttsModule: TTSModule,
    private val soundModule: SoundModule
) {

    fun requestMicrophonePermission(onGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            ttsModule.speak("Necesito permiso para usar el micrófono y escucharte")
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_MICROPHONE
            )
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_MICROPHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ttsModule.speak("Permiso de micrófono concedido")
                    soundModule.play(R.raw.confirmar)
                    // No llamamos a onGranted aquí porque la actividad debe decidir cuándo empezar
                } else {
                    ttsModule.speak("No puedo escucharte sin permiso de micrófono")
                    soundModule.play(R.raw.error)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_MICROPHONE = 100
    }
}
