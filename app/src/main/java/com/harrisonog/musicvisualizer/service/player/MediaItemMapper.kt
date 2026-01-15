package com.harrisonog.musicvisualizer.service.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.harrisonog.musicvisualizer.domain.model.Song

object MediaItemMapper {

    fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun List<Song>.toMediaItems(): List<MediaItem> = map { it.toMediaItem() }

    fun MediaItem.toSong(): Song? {
        val mediaId = mediaId.toLongOrNull() ?: return null
        val metadata = mediaMetadata

        return Song(
            id = mediaId,
            title = metadata.title?.toString() ?: "Unknown",
            artist = metadata.artist?.toString() ?: "Unknown Artist",
            album = metadata.albumTitle?.toString() ?: "Unknown Album",
            duration = 0L, // Duration not available from MediaItem metadata
            contentUri = localConfiguration?.uri ?: Uri.EMPTY,
            albumArtUri = metadata.artworkUri
        )
    }

    fun MediaItem.toSongWithDuration(duration: Long): Song? {
        val mediaId = mediaId.toLongOrNull() ?: return null
        val metadata = mediaMetadata

        return Song(
            id = mediaId,
            title = metadata.title?.toString() ?: "Unknown",
            artist = metadata.artist?.toString() ?: "Unknown Artist",
            album = metadata.albumTitle?.toString() ?: "Unknown Album",
            duration = duration,
            contentUri = localConfiguration?.uri ?: Uri.EMPTY,
            albumArtUri = metadata.artworkUri
        )
    }
}
