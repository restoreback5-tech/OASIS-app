package com.oasis.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.Button

class AppLauncherModule(
    private val context: Context,
    private val ttsModule: TTSModule,
    private val soundModule: SoundModule
) {

    fun showAppsMenu() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.apps_menu_dialog, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.app_calculator)?.setOnClickListener {
            launchApp("android.intent.action.CALCULATOR", "Calculadora")
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.app_camera)?.setOnClickListener {
            launchApp("android.media.action.IMAGE_CAPTURE", "Cámara")
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.app_files)?.setOnClickListener {
            launchApp("android.intent.action.OPEN_DOCUMENT_TREE", "Archivos")
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.app_settings)?.setOnClickListener {
            launchApp("android.settings.SETTINGS", "Ajustes")
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.app_flashlight)?.setOnClickListener {
            ttsModule.speak("Activa la linterna desde el panel de notificaciones")
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.app_calendar)?.setOnClickListener {
            launchApp("android.intent.action.MAIN", "Calendario", "android.intent.category.APP_CALENDAR")
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btn_close_menu)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun launchApp(appName: String) {
        // Búsqueda simple por nombre (se puede ampliar)
        when (appName.lowercase()) {
            "calculadora" -> launchApp("android.intent.action.CALCULATOR", "Calculadora")
            "cámara", "camara" -> launchApp("android.media.action.IMAGE_CAPTURE", "Cámara")
            "whatsapp", "wasap" -> launchPackage("com.whatsapp", "WhatsApp")
            else -> ttsModule.speak("No sé cómo abrir $appName")
        }
    }

    private fun launchApp(action: String, name: String, category: String? = null) {
        try {
            val intent = Intent(action)
            category?.let { intent.addCategory(it) }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ttsModule.speak("Abriendo $name")
                soundModule.play(R.raw.confirmar)
            } else {
                ttsModule.speak("$name no disponible")
                soundModule.play(R.raw.error)
            }
        } catch (e: Exception) {
            ttsModule.speak("No se pudo abrir $name")
        }
    }

    private fun launchPackage(packageName: String, name: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent)
                ttsModule.speak("Abriendo $name")
                soundModule.play(R.raw.confirmar)
            } else {
                ttsModule.speak("$name no instalada")
            }
        } catch (e: Exception) {
            ttsModule.speak("Error al abrir $name")
        }
    }
}
