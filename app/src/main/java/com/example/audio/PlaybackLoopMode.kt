package com.example.audio

/**
 * Modes de lecture et de répétition pour les albums et les playlists :
 * - ONCE : Tourne une seule fois, s'arrête automatiquement à la fin de la dernière chanson.
 * - CONTINUOUS : Tourne en boucle continue indéfiniment (repart à la 1re piste).
 * - TIMED_CONTINUOUS : Tourne en boucle continue pendant une durée (timeline) donnée, puis s'arrête.
 */
enum class PlaybackLoopMode(val label: String, val description: String) {
    ONCE("Une seule fois", "S'arrête à la fin de la liste"),
    CONTINUOUS("Boucle continue", "Tourne en boucle indéfiniment"),
    TIMED_CONTINUOUS("Boucle avec minuteur", "S'arrête après un temps programmé")
}

/**
 * État de la timeline / minuteur de mise en veille pour la lecture.
 */
data class PlaybackTimerState(
    val isActive: Boolean = false,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0
) {
    val progressFraction: Float
        get() = if (totalSeconds > 0) {
            ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    fun formattedRemaining(): String {
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        return "%d:%02d".format(mins, secs)
    }

    fun formattedTotal(): String {
        val mins = totalSeconds / 60
        return if (mins >= 60) {
            val hours = mins / 60
            val remMins = mins % 60
            if (remMins > 0) "${hours}h${remMins}m" else "${hours}h"
        } else {
            "${mins} min"
        }
    }
}
