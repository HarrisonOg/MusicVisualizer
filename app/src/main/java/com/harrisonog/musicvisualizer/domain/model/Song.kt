package com.harrisonog.musicvisualizer.domain.model

import android.net.Uri

/**
 * Represents a song in the music library.
 *
 * @property id The unique identifier of the song.
 * @property title The title of the song.
 * @property artist The name of the artist.
 * @property album The name of the album.
 * @property duration The duration of the song in milliseconds.
 * @property contentUri The URI to access the song's content.
 * @property albumArtUri The URI to access the song's album art.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?
) {
    val durationFormatted: String
        get() {
            val minutes = duration / 1000 / 60
            val seconds = (duration / 1000) % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}
