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
import android.view.animation.AnimationUtils
import android.view.View

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var speechBubble: TextView
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ===== Reloj dinámico =====
        val tvCurrentTime = findViewById<TextView>(R.id.tv_current_time)
        fun updateTime() {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            tvCurrentTime.text = String.format("%02d:%02d", hour, minute)
        }
        updateTime()
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                updateTime()
                handler.postDelayed(this, 1000)
            }
        })

        // Referencias
        speechBubble = findViewById(R.id.speech_bubble)
        val btnAccept = findViewById<Button>(R.id.btn_accept)
        val btnReject = findViewById<Button>(R.id.btn_reject)

        // Animación de ondas turquesa
        val waveBars = findViewById<ImageView>(R.id.wave_bars)
        val callingIndicator = findViewById<View>(R.id.calling_indicator)
        val anim = AnimationUtils.loadAnimation(this, R.anim.sound_waves)
        waveBars?.startAnimation(anim)
        callingIndicator?.startAnimation(anim?.apply { duration = 1000 })

        // TTS
        tts = TextToSpeech(this, this)

        // Botones
        btnAccept.setOnClickListener {
            playSound(R.raw.volumeincremental)
            speak("Llamada aceptada")
            speechBubble.text = "Conectando..."
        }
        btnReject.setOnClickListener {
            playSound(R.raw.dock)
            speak("Llamada rechazada")
            speechBubble.text = "Llamada finalizada"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "MX")
            speak("Te están llamando")
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
