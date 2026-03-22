package com.oasis.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var speechBubble: Any // Ajustar según el tipo real

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lógica de inicialización
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Configuración TTS
        }
    }

    private fun speak(text: String) {
        try {
            if (::tts.isInitialized) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startListening() {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                // Manejar error
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processCommand(cmd: String) {
        try {
            when {
                cmd.contains("llamar") -> { /* Lógica */ }
                // Otros casos
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractContactName(cmd: String): String {
        val keywords = listOf("a", "con", "para")
        // Lógica de extracción
        return ""
    }

    private fun searchAndCall(contactName: String) {
        try {
            val uri = android.provider.ContactsContract.Contacts.CONTENT_URI
            // Lógica de llamada
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun searchAndMessage(contactName: String) {
        try {
            val uri = android.provider.ContactsContract.Contacts.CONTENT_URI
            // Lógica de mensaje
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun searchContacts(contactName: String) {
        try {
            val uri = android.provider.ContactsContract.Contacts.CONTENT_URI
            // Lógica de búsqueda
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playSound(id: Int) {
        try {
            // mediaPlayer?.release()
            // mediaPlayer = ...
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleFlashlight() {
        try {
            val cameraManager = getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            // Lógica de linterna
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showAppsMenuDialog() {
        try {
            val dialogView = layoutInflater.inflate(0, null) // Reemplazar 0 por el layout real
            // Lógica de diálogo
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Lógica de permisos
    }

    override fun onDestroy() {
        try {
            if (::tts.isInitialized) {
                tts.stop()
                tts.shutdown()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}

