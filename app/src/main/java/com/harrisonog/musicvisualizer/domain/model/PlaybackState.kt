package com.harrisonog.musicvisualizer.domain.model

/**
 * Represents the current playback state of the music player.
 *
 * @property isPlaying Whether the player is currently playing music.
 * @property currentSong The currently playing song, if any.
 * @property currentPosition The current playback position in milliseconds.
 * @property duration The total duration of the current song in milliseconds.
 * @property repeatMode The current repeat mode of the player.
 * @property shuffleEnabled Whether shuffling is enabled.
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val isBuffering: Boolean = false
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f

    val currentPositionFormatted: String
        get() {
            val minutes = currentPosition / 1000 / 60
            val seconds = (currentPosition / 1000) % 60
            return String.format("%d:%02d", minutes, seconds)
        }

    val durationFormatted: String
        get() {
            val minutes = duration / 1000 / 60
            val seconds = (duration / 1000) % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}
