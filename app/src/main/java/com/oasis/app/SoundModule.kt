package com.oasis.app

import android.content.Context
import android.media.MediaPlayer

class SoundModule(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun play(resId: Int) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer?.start()
        } catch (e: Exception) {
            // Silenciar errores de sonido
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
