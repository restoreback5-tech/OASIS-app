
package com.oasis.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.drawable.AnimationDrawable
import android.hardware.camera2.CameraManager
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
import com.oasis.core.sound.SoundManager
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var speechBubble: TextView
    private lateinit var sound: SoundManager
    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var listenRunnable: Runnable? = null

override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inicializar SoundManager
        sound = SoundManager(this)
        
        // Inicializar TTS
        tts = TextToSpeech(this, this)
        
        // Animación de fondo
        findViewById<View>(R.id.animated_bg)?.let { bg ->
            (bg.background as? AnimationDrawable)?.start()
        }
        
        val tvCurrentTime = findViewById<TextView>(R.id.tv_current_time)
        speechBubble = findViewById(R.id.speech_bubble)
        
        // Actualizar hora
        fun updateTime() {
            val c = Calendar.getInstance()
            tvCurrentTime.text = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        }
        
        updateTime()
        updateRunnable = object : Runnable {
            override fun run() {
                updateTime()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateRunnable!!)
        
        // Permiso de micrófono
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else {
            handler.postDelayed({ startListening() }, 2000)
        }
        
        // Botón Llamar
        findViewById<Button>(R.id.btn_call).setOnClickListener {
            try {
                sound.play(R.raw.volumeincremental)
                speak("¿A quién quieres llamar?")
                startActivity(Intent(Intent.ACTION_DIAL))
            } catch (e: Exception) {
                speak("Error al abrir teléfono")
            }
        }        
        // Botón Mensajes
        findViewById<Button>(R.id.btn_messages).setOnClickListener {
            try {
                sound.play(R.raw.volumeincremental)
                speak("Abriendo mensajes")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))
            } catch (e: Exception) {
                speak("Error al abrir mensajes")
            }
        }
        
        // Botón Contactos
        findViewById<Button>(R.id.btn_contacts).setOnClickListener {
            try {
                sound.play(R.raw.volumeincremental)
                speak("Abriendo contactos")
                startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
            } catch (e: Exception) {
                speak("Error al abrir contactos")
            }
        }
        
        // Botón Más
        findViewById<Button>(R.id.btn_more).setOnClickListener {
            try {
                sound.play(R.raw.volumeincremental)
                showAppsMenuDialog()
            } catch (e: Exception) {
                speak("Error al abrir menú")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("es", "MX")
            handler.postDelayed({ speak("Bienvenido a OASIS") }, 2000)
        }
    }

    private fun speak(text: String) {
        try {
            if (::tts.isInitialized) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                speechBubble.text = text
            }
        } catch (e: Exception) {
            // Ignorar error
        }    }

    private fun startListening() {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) return
            
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    speechBubble.text = "Escuchando..."
                }
                
                override fun onBeginningOfSpeech() { }
                override fun onRmsChanged(rmsdB: Float) { }
                override fun onBufferReceived(buffer: ByteArray?) { }
                override fun onEndOfSpeech() { }
                
                override fun onError(error: Int) {
                    listenRunnable = object : Runnable {
                        override fun run() {
                            startListening()
                        }
                    }
                    handler.postDelayed(listenRunnable!!, 1000)
                }
                
                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                        processCommand(it)
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) { }
                override fun onEvent(eventType: Int, params: Bundle?) { }
            })
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            speak("Error al escuchar")
        }
    }

    private fun processCommand(cmd: String) {
        try {            when {
                cmd.contains("llamar") || cmd.contains("llama") -> {
                    val contactName = extractContactName(cmd)
                    if (contactName.isNotEmpty()) {
                        searchAndCall(contactName)
                    } else {
                        sound.play(R.raw.volumeincremental)
                        speak("Abriendo teléfono")
                        startActivity(Intent(Intent.ACTION_DIAL))
                    }
                }
                
                cmd.contains("mensaje") || cmd.contains("whatsapp") || cmd.contains("wasap") -> {
                    val contactName = extractContactName(cmd)
                    if (contactName.isNotEmpty()) {
                        searchAndMessage(contactName)
                    } else {
                        sound.play(R.raw.volumeincremental)
                        speak("Abriendo mensajes")
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))
                    }
                }
                
                cmd.contains("contacto") || cmd.contains("agenda") || cmd.contains("buscar") -> {
                    val contactName = extractContactName(cmd)
                    if (contactName.isNotEmpty()) {
                        searchContacts(contactName)
                    } else {
                        sound.play(R.raw.volumeincremental)
                        speak("Abriendo contactos")
                        startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
                    }
                }
                
                cmd.contains("hola") || cmd.contains("buenas") -> {
                    speak("Hola, soy OASIS. ¿En qué te ayudo?")
                }
                
                else -> {
                    speak("Di: llamar, mensajes o contactos")
                }
            }
        } catch (e: Exception) {
            speak("No entendí el comando")
        }
    }

    private fun extractContactName(cmd: String): String {
        val keywords = listOf("llamar", "llama", "mensaje", "mensajes", "whatsapp", "wasap", "contacto", "contactos", "buscar", "agenda")
        val words = cmd.lowercase().split(" ")        for (i in words.indices) {
            if (words[i] in keywords && i + 1 < words.size) {
                return words.drop(i + 1).joinToString(" ").trim()
            }
        }
        return ""
    }

    private fun searchAndCall(contactName: String) {
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$contactName%")
            val cursor: Cursor? = contentResolver.query(uri, projection, selection, selectionArgs, null)
            
            if (cursor != null && cursor.moveToFirst()) {
                val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                cursor.close()
                sound.play(R.raw.volumeincremental)
                speak("Llamando a $name")
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
            } else {
                cursor?.close()
                sound.play(R.raw.volumeincremental)
                speak("No encontré a $contactName")
            }
        } catch (e: Exception) {
            speak("Error al buscar contacto")
        }
    }

    private fun searchAndMessage(contactName: String) {
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$contactName%")
            val cursor: Cursor? = contentResolver.query(uri, projection, selection, selectionArgs, null)
            
            if (cursor != null && cursor.moveToFirst()) {
                val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                cursor.close()
                sound.play(R.raw.volumeincremental)
                speak("Enviando mensaje a $name")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:$number")))
            } else {
                cursor?.close()                sound.play(R.raw.volumeincremental)
                speak("No encontré a $contactName")
            }
        } catch (e: Exception) {
            speak("Error al buscar contacto")
        }
    }

    private fun searchContacts(contactName: String) {
        try {
            val uri = ContactsContract.Contacts.CONTENT_URI
            val selection = "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$contactName%")
            val cursor: Cursor? = contentResolver.query(uri, null, selection, selectionArgs, null)
            
            if (cursor != null && cursor.count > 0) {
                cursor.close()
                sound.play(R.raw.volumeincremental)
                speak("Encontré contactos con $contactName")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.contacts/contacts/?filter=$contactName")))
            } else {
                cursor?.close()
                sound.play(R.raw.volumeincremental)
                speak("No encontré contactos con $contactName")
            }
        } catch (e: Exception) {
            speak("Error al buscar")
        }
    }

    private fun toggleFlashlight() {
        try {
            val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, true)
            handler.postDelayed({
                cameraManager.setTorchMode(cameraId, false)
            }, 500)
        } catch (e: Exception) {
            speak("Error con la linterna")
        }
    }

    private fun showAppsMenuDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.apps_menu_dialog, null)
            val dialog = android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create()
            dialog.show()        } catch (e: Exception) {
            speak("Error al abrir menú")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handler.postDelayed({ startListening() }, 2000)
            } else {
                speak("Necesito permiso de micrófono para escucharte")
            }
        }
    }

    override fun onDestroy() {
        try {
            if (::tts.isInitialized) {
                tts.stop()
                tts.shutdown()
            }
            speechRecognizer?.destroy()
            sound.release()
            updateRunnable?.let { handler.removeCallbacks(it) }
            listenRunnable?.let { handler.removeCallbacks(it) }
        } catch (e: Exception) {
            // Ignorar error
        }
        super.onDestroy()
    }
}
