package com.oasis.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechBubble: TextView? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
            window.statusBarColor = android.graphics.Color.parseColor("#000000")
        } catch (e: Exception) {
            // Si el layout falla, crear UI mínima
            val tv = TextView(this)
            tv.text = "OASIS"
            tv.setTextColor(android.graphics.Color.WHITE)
            tv.gravity = android.view.Gravity.CENTER
            setContentView(tv)
            return
        }

        // Inicializar vistas con null safety
        try { speechBubble = findViewById(R.id.speech_bubble) } catch (e: Exception) { }
        
        // Mostrar hora        try {
            val tvTime: TextView? = findViewById(R.id.tv_current_time)
            tvTime?.let {
                val c = Calendar.getInstance()
                it.text = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
            }
        } catch (e: Exception) { }

        // Inicializar TTS con try-catch
        try {
            tts = TextToSpeech(this, this)
        } catch (e: Exception) {
            showToast("Voz no disponible")
        }

        // Configurar botones con null safety
        setupButton(R.id.btn_call) { startActivity(Intent(Intent.ACTION_DIAL)) }
        setupButton(R.id.btn_messages) { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("sms:"))) }
        setupButton(R.id.btn_contacts) { startActivity(Intent(android.provider.ContactsContract.Contacts.CONTENT_URI)) }
        setupButton(R.id.btn_more) { showToast("Menú pronto") }

        // Pedir permisos
        requestPermissionsIfNeeded()

        // Iniciar escucha después de un delay seguro
        handler.postDelayed({ startListeningSafe() }, 3000)
    }

    private fun setupButton(id: Int, onClick: () -> Unit) {
        try {
            findViewById<Button>(id)?.setOnClickListener {
                try {
                    playSoundSafe()
                    onClick()
                } catch (e: Exception) { showToast("Error") }
            }
        } catch (e: Exception) { }
    }

    private fun requestPermissionsIfNeeded() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            }
        } catch (e: Exception) { }
    }

    private fun startListeningSafe() {
        if (isListening) return        try {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) return
            isListening = true
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { speechBubble?.text = "🎤" }
                override fun onBeginningOfSpeech() { speechBubble?.text = "👂" }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { 
                    speechBubble?.text = "OASIS"
                    isListening = false
                    handler.postDelayed({ startListeningSafe() }, 2000)
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        processCommand(matches[0].lowercase(Locale.getDefault()))
                    }
                    isListening = false
                    handler.postDelayed({ startListeningSafe() }, 1000)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "MX").language)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            speechBubble?.text = "Voz: error"
        }
    }

    private fun processCommand(cmd: String) {
        try {
            when {
                cmd.contains("llamar") || cmd.contains("llama") -> {
                    speakSafe("Abriendo teléfono")
                    startActivity(Intent(Intent.ACTION_DIAL))
                }
                cmd.contains("mensaje") || cmd.contains("whatsapp") -> {
                    speakSafe("Mensajes")
                    startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("sms:")))
                }
                cmd.contains("contacto") || cmd.contains("agenda") -> {
                    speakSafe("Contactos")                    startActivity(Intent(android.provider.ContactsContract.Contacts.CONTENT_URI))
                }
                cmd.contains("hola") -> speakSafe("Hola, soy OASIS")
                else -> {}
            }
        } catch (e: Exception) { showToast("No entendí") }
    }

    private fun speakSafe(text: String) {
        try {
            tts?.let {
                if (it.isSpeaking) it.stop()
                it.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                speechBubble?.text = text
            }
        } catch (e: Exception) { }
    }

    private fun playSoundSafe() {
        try {
            val mp = android.media.MediaPlayer.create(this, R.raw.volumeincremental)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        } catch (e: Exception) { }
    }

    private fun showToast(msg: String) {
        try { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() } catch (e: Exception) { }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try { tts?.language = Locale("es", "MX") } catch (e: Exception) { }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showToast("Micrófono activo")
        }
    }

    override fun onDestroy() {
        try { tts?.stop(); tts?.shutdown() } catch (e: Exception) { }
        try { speechRecognizer?.destroy() } catch (e: Exception) { }
        try { handler.removeCallbacksAndMessages(null) } catch (e: Exception) { }
        super.onDestroy()
    }
}
