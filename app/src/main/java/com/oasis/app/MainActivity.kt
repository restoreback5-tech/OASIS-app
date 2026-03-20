package com.oasis.app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var speechBubble: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Referencias a los elementos
        speechBubble = findViewById(R.id.speech_bubble)
        val btnAccept = findViewById<Button>(R.id.btn_accept)
        val btnReject = findViewById<Button>(R.id.btn_reject)

        // Inicializar voz (Text-to-Speech)
        tts = TextToSpeech(this, this)

        // Botón Aceptar (Verde)
        btnAccept.setOnClickListener {
            speak("Llamada aceptada")
            speechBubble.text = "Conectando..."
        }

        // Botón Rechazar (Rojo)
        btnReject.setOnClickListener {
            speak("Llamada rechazada")
            speechBubble.text = "Llamada finalizada"
        }
    }

    // Configurar la voz cuando esté lista
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "MX") // Español
            // Decir el mensaje inicial automáticamente
            speak("Te están llamando")
        }
    }

    // Función para hablar
    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        speechBubble.text = text
    }

    // Limpiar voz al cerrar
    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
