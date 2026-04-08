package com.oasis.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    // Módulos
    private lateinit var ttsModule: TTSModule
    private lateinit var soundModule: SoundModule
    private lateinit var voiceModule: VoiceCommandModule
    private lateinit var contactHelper: ContactHelper
    private lateinit var appLauncher: AppLauncherModule
    private lateinit var permissionHelper: PermissionHelper

    // UI
    private lateinit var speechBubble: TextView
    private lateinit var tvCurrentTime: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var updateTimeRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar UI
        speechBubble = findViewById(R.id.speech_bubble)
        tvCurrentTime = findViewById(R.id.tv_current_time)

        // Inicializar módulos
        ttsModule = TTSModule(this) { text -> speechBubble.text = text }
        soundModule = SoundModule(this)
        permissionHelper = PermissionHelper(this, ttsModule, soundModule)
        voiceModule = VoiceCommandModule(this, ttsModule, soundModule, contactHelper, appLauncher)
        contactHelper = ContactHelper(this, ttsModule, soundModule)
        appLauncher = AppLauncherModule(this, ttsModule, soundModule)

        // Configurar listeners de UI
        findViewById<Button>(R.id.btn_call).setOnClickListener {
            soundModule.play(R.raw.touch)
            ttsModule.speak("¿A quién quieres llamar?")
            startActivity(Intent(Intent.ACTION_DIAL))
        }

        findViewById<Button>(R.id.btn_messages).setOnClickListener {
            soundModule.play(R.raw.touch)
            ttsModule.speak("Abriendo mensajes")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))
        }

        findViewById<Button>(R.id.btn_contacts).setOnClickListener {
            soundModule.play(R.raw.touch)
            ttsModule.speak("Abriendo contactos")
            startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
        }

        findViewById<Button>(R.id.btn_more).setOnClickListener {
            soundModule.play(R.raw.touch)
            appLauncher.showAppsMenu()
        }

        // Reloj
        updateTime()
        updateTimeRunnable = object : Runnable {
            override fun run() {
                updateTime()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimeRunnable!!)

        // Solicitar permiso de micrófono y empezar escucha
        permissionHelper.requestMicrophonePermission {
            voiceModule.startListening()
        }
    }

    private fun updateTime() {
        val c = Calendar.getInstance()
        tvCurrentTime.text = String.format(
            "%02d:%02d",
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE)
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsModule.shutdown()
        soundModule.release()
        voiceModule.destroy()
        updateTimeRunnable?.let { handler.removeCallbacks(it) }
    }
}
