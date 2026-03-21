package com.oasis.app
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
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
        findViewById<Button>(R.id.btn_more).setOnClickListener { playSound(R.raw.volumeincremental); showAppsMenuDialog() }
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
            cmd.contains("llamar") || cmd.contains("llama") -> { val contactName = extractContactName(cmd); if (contactName.isNotEmpty()) { searchAndCall(contactName) } else { playSound(R.raw.volumeincremental); speak("Abriendo teléfono"); startActivity(Intent(Intent.ACTION_DIAL)) } }
            cmd.contains("mensaje") || cmd.contains("whatsapp") || cmd.contains("wasap") -> { val contactName = extractContactName(cmd); if (contactName.isNotEmpty()) { searchAndMessage(contactName) } else { playSound(R.raw.volumeincremental); speak("Abriendo mensajes"); startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))) } }
            cmd.contains("contacto") || cmd.contains("agenda") || cmd.contains("buscar") -> { val contactName = extractContactName(cmd); if (contactName.isNotEmpty()) { searchContacts(contactName) } else { playSound(R.raw.volumeincremental); speak("Abriendo contactos"); startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)) } }
            cmd.contains("hola") || cmd.contains("buenas") -> { speak("Hola, soy OASIS. ¿En qué te ayudo?") }
            else -> { speak("Di: llamar, mensajes o contactos") }
        }
    }
    private fun extractContactName(cmd: String): String { val keywords = listOf("llamar","llama","mensaje","mensajes","whatsapp","wasap","contacto","contactos","agenda","buscar","a","al","la","los","las"); val words = cmd.split(" "); val nameWords = words.filter { word -> !keywords.contains(word) && word.length > 2 }; return nameWords.joinToString(" ").capitalize() }
    private fun searchAndCall(contactName: String) { val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI; val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME); val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"; val selectionArgs = arrayOf("%$contactName%"); val cursor: Cursor? = contentResolver.query(uri, projection, selection, selectionArgs, null); if (cursor != null && cursor.moveToFirst()) { val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)); val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)); cursor.close(); playSound(R.raw.volumeincremental); speak("Llamando a $name"); startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))) } else { cursor?.close(); playSound(R.raw.volumeincremental); speak("No encontré a $contactName en tus contactos") } }
    private fun searchAndMessage(contactName: String) { val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI; val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME); val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"; val selectionArgs = arrayOf("%$contactName%"); val cursor: Cursor? = contentResolver.query(uri, projection, selection, selectionArgs, null); if (cursor != null && cursor.moveToFirst()) { val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)); val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)); cursor.close(); playSound(R.raw.volumeincremental); speak("Enviando mensaje a $name"); startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:$number"))) } else { cursor?.close(); playSound(R.raw.volumeincremental); speak("No encontré a $contactName en tus contactos") } }
    private fun searchContacts(contactName: String) { val uri = ContactsContract.Contacts.CONTENT_URI; val selection = "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?"; val selectionArgs = arrayOf("%$contactName%"); val cursor: Cursor? = contentResolver.query(uri, null, selection, selectionArgs, null); if (cursor != null && cursor.count > 0) { cursor.close(); playSound(R.raw.volumeincremental); speak("Encontré contactos con $contactName"); startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.contacts/contacts/?filter=$contactName"))) } else { cursor?.close(); playSound(R.raw.volumeincremental); speak("No encontré contactos con $contactName") } }
    private fun playSound(id: Int) { mediaPlayer?.release(); mediaPlayer = MediaPlayer.create(this, id); mediaPlayer?.start() }
    private fun showAppsMenuDialog() { val dialogView = layoutInflater.inflate(R.layout.apps_menu_dialog, null); val dialog = android.app.AlertDialog.Builder(this).setView(dialogView).create(); dialogView.findViewById<Button>(R.id.app_calculator).setOnClickListener { try { startActivity(Intent("android.intent.action.CALCULATOR")) } catch (e: Exception) { speak("Calculadora no disponible") }; dialog.dismiss() }; dialogView.findViewById<Button>(R.id.app_camera).setOnClickListener { try { startActivity(Intent("android.media.action.IMAGE_CAPTURE")) } catch (e: Exception) { speak("Cámara no disponible") }; dialog.dismiss() }; dialogView.findViewById<Button>(R.id.app_files).setOnClickListener { try { startActivity(Intent("android.intent.action.OPEN_DOCUMENT_TREE")) } catch (e: Exception) { speak("Archivos no disponible") }; dialog.dismiss() }; dialogView.findViewById<Button>(R.id.app_settings).setOnClickListener { try { startActivity(Intent("android.settings.SETTINGS")) } catch (e: Exception) { speak("Ajustes no disponible") }; dialog.dismiss() }; dialogView.findViewById<Button>(R.id.app_flashlight).setOnClickListener { speak("Activa la linterna desde el panel de notificaciones"); dialog.dismiss() }; dialogView.findViewById<Button>(R.id.app_calendar).setOnClickListener { try { startActivity(Intent("android.intent.action.MAIN").apply { addCategory("android.intent.category.APP_CALENDAR") }) } catch (e: Exception) { speak("Calendario no disponible") }; dialog.dismiss() }; dialogView.findViewById<Button>(R.id.btn_close_menu).setOnClickListener { dialog.dismiss() }; dialog.show() }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) { speak("Permiso de micrófono concedido"); handler.postDelayed({ startListening() }, 1000) } else { speechBubble.text = "Activa el micrófono en ajustes" } }
    override fun onDestroy() { if (::tts.isInitialized) { tts.stop(); tts.shutdown() }; speechRecognizer?.destroy(); mediaPlayer?.release(); updateRunnable?.let { handler.removeCallbacks(it) }; listenRunnable?.let { handler.removeCallbacks(it) }; super.onDestroy() }
}
