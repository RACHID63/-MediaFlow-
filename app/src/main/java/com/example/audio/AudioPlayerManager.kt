package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

/**
 * Gestionnaire du lecteur audio Android natif (MediaPlayer).
 * Lit les fichiers MP3, WAV, OGG, M4A importés localement avec support de la lecture continue.
 */
class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    var onCompletionListener: (() -> Unit)? = null
    var onErrorListener: ((String) -> Unit)? = null

    fun playAudio(uriString: String?, onStarted: (durationSec: Int) -> Unit) {
        stop()
        if (uriString.isNullOrBlank()) return

        try {
            val uri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setOnPreparedListener { mp ->
                    val durationSec = (mp.duration / 1000).coerceAtLeast(1)
                    onStarted(durationSec)
                    mp.start()
                }
                setOnCompletionListener {
                    onCompletionListener?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                    onErrorListener?.invoke("Erreur de lecture audio ($what)")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Impossible d'initialiser MediaPlayer pour $uriString", e)
            onErrorListener?.invoke(e.message ?: "Erreur de chargement du fichier audio")
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    fun resume() {
        try {
            mediaPlayer?.start()
        } catch (_: Exception) {}
    }

    fun seekToSeconds(seconds: Int) {
        try {
            mediaPlayer?.seekTo(seconds * 1000)
        } catch (_: Exception) {}
    }

    fun getCurrentPositionSeconds(): Int {
        return try {
            (mediaPlayer?.currentPosition ?: 0) / 1000
        } catch (_: Exception) {
            0
        }
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}
