package com.harrisonog.musicvisualizer.domain.repository

import com.harrisonog.musicvisualizer.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing music data.
 *
 * @property getAllSongs A flow emitting a list of all songs in the music library.
 * @property getSongById A flow emitting a specific song by its ID.
 * @property refreshMusicLibrary Refreshes the music library by scanning the device's storage.
 * @property searchSongs Searches for songs matching the given query.
 */
interface MusicRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun getSongById(id: Long): Flow<Song?>
    suspend fun refreshMusicLibrary()
    fun searchSongs(query: String): Flow<List<Song>>
}
