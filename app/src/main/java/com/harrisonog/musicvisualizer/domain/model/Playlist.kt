package com.harrisonog.musicvisualizer.domain.model

/**
 * Represents a playlist of songs.
 *
 * @property id The unique identifier of the playlist.
 * @property name The name of the playlist.
 * @property songs The list of songs in the playlist.
 * @property createdAt The timestamp when the playlist was created.
 * @property songCount The total number of songs in the playlist.
 * @property totalDuration The total duration of all songs in the playlist.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val songCount: Int get() = songs.size

    val totalDuration: Long get() = songs.sumOf { it.duration }

    val totalDurationFormatted: String
        get() {
            val totalSeconds = totalDuration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) {
                String.format("%d hr %d min", hours, minutes)
            } else {
                String.format("%d min", minutes)
            }
        }
}
