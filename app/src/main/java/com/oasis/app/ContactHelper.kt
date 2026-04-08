package com.oasis.app

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.appcompat.app.AppCompatActivity

class ContactHelper(
    private val activity: AppCompatActivity,
    private val ttsModule: TTSModule,
    private val soundModule: SoundModule
) {

    fun searchAndCall(contactName: String) {
        val cursor = findContact(contactName)
        if (cursor != null && cursor.moveToFirst()) {
            val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            cursor.close()
            soundModule.play(R.raw.confirmar)
            ttsModule.speak("Llamando a $name")
            activity.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
        } else {
            cursor?.close()
            soundModule.play(R.raw.error)
            ttsModule.speak("No encontré a $contactName en tus contactos")
        }
    }

    fun searchAndMessage(contactName: String) {
        val cursor = findContact(contactName)
        if (cursor != null && cursor.moveToFirst()) {
            val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            cursor.close()
            soundModule.play(R.raw.confirmar)
            ttsModule.speak("Enviando mensaje a $name")
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:$number")))
        } else {
            cursor?.close()
            soundModule.play(R.raw.error)
            ttsModule.speak("No encontré a $contactName en tus contactos")
        }
    }

    fun searchContacts(contactName: String) {
        val uri = ContactsContract.Contacts.CONTENT_URI
        val selection = "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactName%")
        val cursor = activity.contentResolver.query(uri, null, selection, selectionArgs, null)
        if (cursor != null && cursor.count > 0) {
            cursor.close()
            soundModule.play(R.raw.confirmar)
            ttsModule.speak("Encontré contactos con $contactName")
            activity.startActivity(Intent(
                Intent.ACTION_VIEW,
                Uri.parse("content://com.android.contacts/contacts/?filter=$contactName")
            ))
        } else {
            cursor?.close()
            soundModule.play(R.raw.error)
            ttsModule.speak("No encontré contactos con $contactName")
        }
    }

    fun extractContactName(cmd: String): String {
        val keywords = listOf(
            "llamar", "llama", "mensaje", "mensajes", "whatsapp",
            "wasap", "contacto", "contactos", "agenda", "buscar",
            "a", "al", "la", "los", "las"
        )
        val words = cmd.split(" ")
        val nameWords = words.filter { word ->
            !keywords.contains(word) && word.length > 2
        }
        return nameWords.joinToString(" ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun findContact(contactName: String): Cursor? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactName%")
        return activity.contentResolver.query(uri, projection, selection, selectionArgs, null)
    }
}
