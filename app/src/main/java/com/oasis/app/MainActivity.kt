package com.oasis.app
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.Calendar
import android.os.Handler
import android.os.Looper
import android.media.MediaPlayer
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var speechBubble: TextView
    private var mediaPlayer: MediaPlayer? = null
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val tvCurrentTime = findViewById<TextView>(R.id.tv_current_time)
        speechBubble = findViewById(R.id.speech_bubble)
        
        // Reloj
        fun updateTime() { 
            val c = Calendar.getInstance()
            tvCurrentTime.text = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) 
        }
        updateTime()
        Handler(Looper.getMainLooper()).post(object : Runnable { 
            override fun run() { updateTime(); Handler(Looper.getMainLooper()).postDelayed(this, 1000) } 
        })
        
        // Pedir permiso de micrófono
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }        
        tts = TextToSpeech(this, this)
        
        // Iniciar voz después de 2 segundos
        Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 2000)
        
        // Botones
        findViewById<Button>(R.id.btn_call).setOnClickListener { 
            playSound(R.raw.volumeincremental)
            speak("¿A quién quieres llamar?")
            startActivity(Intent(Intent.ACTION_DIAL)) 
        }
        
        findViewById<Button>(R.id.btn_messages).setOnClickListener { 
            playSound(R.raw.volumeincremental)
            speak("Abriendo mensajes")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))) 
        }
        
        findViewById<Button>(R.id.btn_contacts).setOnClickListener { 
            playSound(R.raw.volumeincremental)
            speak("Abriendo contactos")
            startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)) 
        }
        
        findViewById<Button>(R.id.btn_more).setOnClickListener { 
            playSound(R.raw.volumeincremental)
            speak("Más funciones próximamente") 
        }
    }
    
    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { speechBubble.text = "🎤 Escuchando..." }
            override fun onBeginningOfSpeech() { speechBubble.text = "👂 Te escucho..." }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            
            override fun onError(error: Int) {
                speechBubble.text = "Bienvenido a OASIS"
                Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 2000)
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {                    val command = matches[0].lowercase(Locale.getDefault())
                    processCommand(command)
                }
                Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 1000)
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "MX").language)
        }
        
        speechRecognizer?.startListening(intent)
    }
    
    private fun processCommand(cmd: String) {
        when {
            cmd.contains("llamar") || cmd.contains("llama") -> {
                playSound(R.raw.volumeincremental)
                speak("Abriendo teléfono")
                startActivity(Intent(Intent.ACTION_DIAL))
            }
            cmd.contains("mensaje") || cmd.contains("whatsapp") -> {
                playSound(R.raw.volumeincremental)
                speak("Abriendo mensajes")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))
            }
            cmd.contains("contacto") || cmd.contains("agenda") -> {
                playSound(R.raw.volumeincremental)
                speak("Abriendo contactos")
                startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
            }
            cmd.contains("hola") || cmd.contains("buenas") -> {
                speak("Hola, soy OASIS. ¿En qué te ayudo?")
            }
            else -> {
                speak("Di: llamar, mensajes o contactos")
            }
        }
    }
    
    override fun onInit(status: Int) { 
        if (status == TextToSpeech.SUCCESS) { 
            tts.language = Locale("es", "MX")
            Handler(Looper.getMainLooper()).postDelayed({ speak("Bienvenido a OASIS") }, 500)
        } 
    }    
    private fun speak(text: String) { 
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        speechBubble.text = text
    }
    
    private fun playSound(id: Int) { 
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, id)
        mediaPlayer?.start() 
    }
    
    override fun onDestroy() { 
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        speechRecognizer?.destroy()
        mediaPlayer?.release()
        super.onDestroy() 
    }
}
