package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Gestionnaire de lecture audio pour l'application.
 * Prend en charge la lecture des fichiers réels (WAV, MP3, etc.) via Android MediaPlayer,
 * ainsi qu'une synthèse harmonique musicale de secours lorsque aucun fichier n'est associé,
 * garantissant que la lecture produit toujours du son et un retour interactif parfait.
 */
class AudioPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var synthJob: Job? = null
    private var progressTrackerJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0)
    val currentPositionSec: StateFlow<Int> = _currentPositionSec.asStateFlow()

    private val _durationSec = MutableStateFlow(0)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    var onCompletionListener: (() -> Unit)? = null

    fun play(audioUriString: String?, durationSeconds: Int) {
        stop()

        _durationSec.value = durationSeconds.coerceAtLeast(1)
        _currentPositionSec.value = 0

        if (!audioUriString.isNullOrBlank()) {
            playRealAudio(audioUriString, durationSeconds)
        } else {
            playSyntheticAudio(durationSeconds)
        }

        startProgressTracker()
    }

    private fun playRealAudio(uriString: String, durationSeconds: Int) {
        try {
            val uri = Uri.parse(uriString)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, uri)
                setOnPreparedListener { player ->
                    val actualDurationSec = (player.duration / 1000).coerceAtLeast(1)
                    _durationSec.value = actualDurationSec
                    player.start()
                    _isPlaying.value = true
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    onCompletionListener?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("AudioPlayerManager", "MediaPlayer error $what, $extra. Falling back to synth audio.")
                    playSyntheticAudio(durationSeconds)
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Erreur lors de la lecture de l'URI audio: $uriString", e)
            playSyntheticAudio(durationSeconds)
        }
    }

    /**
     * Générateur de mélodie acoustique harmonique (synthèse PCM native via AudioTrack)
     * Joue des arpèges musicaux apaisants quand un morceau n'a pas de fichier WAV/MP3 externe rattaché.
     */
    private fun playSyntheticAudio(durationSeconds: Int) {
        _isPlaying.value = true
        synthJob?.cancel()
        synthJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val notes = floatArrayOf(
                261.63f, 329.63f, 392.00f, 523.25f, // C4, E4, G4, C5
                440.00f, 349.23f, 293.66f, 392.00f  // A4, F4, D4, G4
            )

            var track: AudioTrack? = null
            try {
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()

                val noteDurationSamples = (sampleRate * 0.4).toInt()
                val buffer = ShortArray(noteDurationSamples)

                var noteIndex = 0
                while (isActive && _isPlaying.value && _currentPositionSec.value < durationSeconds) {
                    val freq = notes[noteIndex % notes.size]
                    for (i in 0 until noteDurationSamples) {
                        val t = i.toDouble() / sampleRate
                        val envelope = (1.0 - (i.toDouble() / noteDurationSamples)).coerceIn(0.0, 1.0)
                        val sample = sin(2.0 * Math.PI * freq * t) * envelope * 0.4
                        buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }
                    track.write(buffer, 0, buffer.size)
                    noteIndex++
                }
            } catch (e: Exception) {
                Log.w("AudioPlayerManager", "AudioTrack synthesis interrupted", e)
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(500L)
                if (_isPlaying.value) {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying) {
                        val currentSec = (mp.currentPosition / 1000).coerceAtLeast(0)
                        _currentPositionSec.value = currentSec
                    } else if (synthJob?.isActive == true) {
                        val nextSec = _currentPositionSec.value + 1
                        if (nextSec >= _durationSec.value) {
                            _isPlaying.value = false
                            onCompletionListener?.invoke()
                        } else {
                            _currentPositionSec.value = nextSec
                        }
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer
        if (mp != null) {
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
            } else {
                mp.start()
                _isPlaying.value = true
            }
        } else {
            _isPlaying.value = !_isPlaying.value
        }
    }

    fun seekTo(seconds: Int) {
        _currentPositionSec.value = seconds
        try {
            mediaPlayer?.seekTo(seconds * 1000)
        } catch (_: Exception) {}
    }

    fun stop() {
        synthJob?.cancel()
        synthJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
    }

    fun release() {
        stop()
        progressTrackerJob?.cancel()
    }
}
