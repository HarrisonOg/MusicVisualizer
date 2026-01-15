package com.harrisonog.musicvisualizer.domain.repository

import com.harrisonog.musicvisualizer.domain.model.Playlist
import com.harrisonog.musicvisualizer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getPlaylistById(id: Long): Flow<Playlist?>
    suspend fun createPlaylist(name: String): Long
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addSongToPlaylist(playlistId: Long, song: Song)
    suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    suspend fun reorderSongs(playlistId: Long, songId: Long, newPosition: Int)
}
