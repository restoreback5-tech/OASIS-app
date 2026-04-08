package com.oasis.app

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class VoiceCommandModule(
    private val activity: AppCompatActivity,
    private val ttsModule: TTSModule,
    private val soundModule: SoundModule,
    private val contactHelper: ContactHelper,
    private val appLauncher: AppLauncherModule
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            ttsModule.speak("Reconocimiento de voz no disponible")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                ttsModule.speak("Escuchando...")
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                isListening = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No entendí, intenta de nuevo"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Estoy ocupado, espera un momento"
                    else -> "Error de voz"
                }
                ttsModule.speak(errorMsg)
                restartListening()
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0].lowercase(Locale.getDefault())
                    processCommand(command)
                }
                restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "MX").language)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di un comando")
        }
        speechRecognizer?.startListening(intent)
    }

    private fun processCommand(cmd: String) {
        when {
            cmd.contains("llamar") || cmd.contains("llama") -> {
                val contactName = contactHelper.extractContactName(cmd)
                if (contactName.isNotEmpty()) {
                    contactHelper.searchAndCall(contactName)
                } else {
                    soundModule.play(R.raw.touch)
                    ttsModule.speak("¿A quién quieres llamar?")
                    activity.startActivity(Intent(Intent.ACTION_DIAL))
                }
            }
            cmd.contains("mensaje") || cmd.contains("whatsapp") || cmd.contains("wasap") -> {
                val contactName = contactHelper.extractContactName(cmd)
                if (contactName.isNotEmpty()) {
                    contactHelper.searchAndMessage(contactName)
                } else {
                    soundModule.play(R.raw.touch)
                    ttsModule.speak("¿A quién quieres enviar mensaje?")
                    activity.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("sms:")))
                }
            }
            cmd.contains("contacto") || cmd.contains("agenda") -> {
                val contactName = contactHelper.extractContactName(cmd)
                if (contactName.isNotEmpty()) {
                    contactHelper.searchContacts(contactName)
                } else {
                    soundModule.play(R.raw.touch)
                    ttsModule.speak("Abriendo contactos")
                    activity.startActivity(Intent(Intent.ACTION_VIEW, android.provider.ContactsContract.Contacts.CONTENT_URI))
                }
            }
            cmd.contains("abrir") || cmd.contains("abre") -> {
                val appName = extractAppName(cmd)
                if (appName.isNotEmpty()) {
                    appLauncher.launchApp(appName)
                } else {
                    ttsModule.speak("¿Qué aplicación quieres abrir?")
                }
            }
            cmd.contains("hola") || cmd.contains("buenas") -> {
                ttsModule.speak("Hola, soy OASIS. ¿En qué te ayudo?")
            }
            else -> {
                ttsModule.speak("Di: llamar, mensajes, contactos o abrir aplicación")
            }
        }
    }

    private fun extractAppName(cmd: String): String {
        val keywords = listOf("abrir", "abre", "lanza", "inicia")
        var result = cmd
        keywords.forEach { result = result.replace(it, "").trim() }
        return result.split(" ").firstOrNull() ?: ""
    }

    private fun restartListening() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            startListening()
        }, 1500)
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
