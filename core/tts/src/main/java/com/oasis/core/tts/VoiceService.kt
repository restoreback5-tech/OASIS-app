package com.oasis.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceService(
    private val context: Context,
    private val onInit: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false
    
    init {
        tts = TextToSpeech(context, this)
    }
    
    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        if (isReady) {
            val result = tts?.setLanguage(Locale("es", "MX"))
            if (result == TextToSpeech.LANG_MISSING_DATA || 
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("es", "ES"))
            }
        }
        onInit(isReady)
    }
    
    fun speak(text: String, onBubbleUpdate: (String) -> Unit = {}) {
        if (!isReady || tts == null) return
        onBubbleUpdate(text)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    fun stop() {
        tts?.stop()
    }
    
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
    
    fun isReady(): Boolean = isReady
}
