package com.oasis.app
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Locale
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var speechBubble: TextView
    private var mediaPlayer: MediaPlayer? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var listenRunnable: Runnable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tts = TextToSpeech(this, this)
        findViewById<View>(R.id.animated_bg)?.let { bg -> (bg.background as? AnimationDrawable)?.start() }
        val tvCurrentTime = findViewById<TextView>(R.id.tv_current_time)
        speechBubble = findViewById(R.id.speech_bubble)
        fun updateTime() { val c = Calendar.getInstance(); tvCurrentTime.text = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) }
        updateTime()
        updateRunnable = object : Runnable { override fun run() { updateTime(); handler.postDelayed(this, 1000) } }
        handler.post(updateRunnable!!)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100) } else { handler.postDelayed({ startListening() }, 2000) }
        findViewById<Button>(R.id.btn_call).setOnClickListener { playSound(R.raw.volumeincremental); speak("¿A quién quieres llamar?"); startActivity(Intent(Intent.ACTION_DIAL)) }
        findViewById<Button>(R.id.btn_messages).setOnClickListener { playSound(R.raw.volumeincremental); speak("Abriendo mensajes"); startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))) }
        findViewById<Button>(R.id.btn_contacts).setOnClickListener { playSound(R.raw.volumeincremental); speak("Abriendo contactos"); startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)) }
        findViewById<Button>(R.id.btn_more).setOnClickListener { playSound(R.raw.volumeincremental); speak("Más funciones próximamente") }
    }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) { tts.language = Locale("es", "MX"); handler.postDelayed({ speak("Bienvenido a OASIS") }, 500) } }
    private fun speak(text: String) { if (::tts.isInitialized) { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null); speechBubble.text = text } }
    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { speechBubble.text = "🎤 Escuchando..." }
            override fun onBeginningOfSpeech() { speechBubble.text = "👂 Te escucho..." }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { speechBubble.text = "Bienvenido a OASIS"; listenRunnable = Runnable { startListening() }; handler.postDelayed(listenRunnable!!, 2000) }
            override fun onResults(results: Bundle?) { val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if (!matches.isNullOrEmpty()) { val cmd = matches[0].lowercase(Locale.getDefault()); processCommand(cmd) }; listenRunnable = Runnable { startListening() }; handler.postDelayed(listenRunnable!!, 1000) }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "MX").language) }
        speechRecognizer?.startListening(intent)
    }
    private fun processCommand(cmd: String) {
        when {
            cmd.contains("llamar") || cmd.contains("llama") -> { playSound(R.raw.volumeincremental); speak("Abriendo teléfono"); startActivity(Intent(Intent.ACTION_DIAL)) }
            cmd.contains("mensaje") || cmd.contains("whatsapp") -> { playSound(R.raw.volumeincremental); speak("Abriendo mensajes"); startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))) }
            cmd.contains("contacto") || cmd.contains("agenda") -> { playSound(R.raw.volumeincremental); speak("Abriendo contactos"); startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)) }
            cmd.contains("hola") || cmd.contains("buenas") -> { speak("Hola, soy OASIS. ¿En qué te ayudo?") }
            else -> { speak("Di: llamar, mensajes o contactos") }
        }
    }
    private fun playSound(id: Int) { mediaPlayer?.release(); mediaPlayer = MediaPlayer.create(this, id); mediaPlayer?.start() }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            speak("Permiso de micrófono concedido")
            handler.postDelayed({ startListening() }, 1000)
        } else {
            speechBubble.text = "Activa el micrófono en ajustes"
        }
    }
    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        speechRecognizer?.destroy()
        mediaPlayer?.release()
        updateRunnable?.let { handler.removeCallbacks(it) }
        listenRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}
