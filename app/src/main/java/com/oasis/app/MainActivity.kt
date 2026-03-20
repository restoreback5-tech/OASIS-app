package com.oasis.app
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Reloj dinámico
        val tvCurrentTime = findViewById<TextView>(R.id.tv_current_time)
        fun updateTime() {
            val calendar = Calendar.getInstance()
            tvCurrentTime.text = String.format("%02d:%02d", 
                calendar.get(Calendar.HOUR_OF_DAY), 
                calendar.get(Calendar.MINUTE))
        }
        updateTime()
        Handler(Looper.getMainLooper()).post(object : Runnable {
            override fun run() { updateTime(); Handler(Looper.getMainLooper()).postDelayed(this, 1000) }
        })

        speechBubble = findViewById(R.id.speech_bubble)
        tts = TextToSpeech(this, this)

        // Botón LLAMAR
        findViewById<Button>(R.id.btn_call).setOnClickListener {
            playSound(R.raw.volumeincremental)
            speak("¿A quién quieres llamar?")
            startActivity(Intent(Intent.ACTION_DIAL))
        }

        // Botón MENSAJES
        findViewById<Button>(R.id.btn_messages).setOnClickListener {
            playSound(R.raw.volumeincremental)
            speak("Abriendo mensajes")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))
        }

        // Botón CONTACTOS
        findViewById<Button>(R.id.btn_contacts).setOnClickListener {
            playSound(R.raw.volumeincremental)
            speak("Abriendo contactos")
            startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
        }

        // Botón MÁS
        findViewById<Button>(R.id.btn_more).setOnClickListener {
            playSound(R.raw.volumeincremental)
            speak("Más funciones disponibles")
            speechBubble.text = "Próximamente..."
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "MX")
            speak("Bienvenido a OASIS")
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        speechBubble.text = text
    }

    private fun playSound(soundId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, soundId)
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        mediaPlayer?.release()
        super.onDestroy()
    }
}
