package com.harrisonog.musicvisualizer.data.local.entity

import android.net.Uri
import com.harrisonog.musicvisualizer.domain.model.Playlist
import com.harrisonog.musicvisualizer.domain.model.Song

fun SongEntity.toDomain(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    contentUri = Uri.parse(contentUri),
    albumArtUri = albumArtUri?.let { Uri.parse(it) }
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    contentUri = contentUri.toString(),
    albumArtUri = albumArtUri?.toString()
)

fun PlaylistEntity.toDomain(songs: List<Song> = emptyList()): Playlist = Playlist(
    id = id,
    name = name,
    songs = songs,
    createdAt = createdAt
)

fun PlaylistWithSongs.toDomain(): Playlist = Playlist(
    id = playlist.id,
    name = playlist.name,
    songs = songs.map { it.toDomain() },
    createdAt = playlist.createdAt
)

fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    createdAt = createdAt
)
