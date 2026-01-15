package com.harrisonog.musicvisualizer.data.repository

import com.harrisonog.musicvisualizer.data.local.dao.PlaylistDao
import com.harrisonog.musicvisualizer.data.local.entity.PlaylistEntity
import com.harrisonog.musicvisualizer.data.local.entity.PlaylistSongCrossRef
import com.harrisonog.musicvisualizer.data.local.entity.toDomain
import com.harrisonog.musicvisualizer.data.local.entity.toEntity
import com.harrisonog.musicvisualizer.domain.model.Playlist
import com.harrisonog.musicvisualizer.domain.model.Song
import com.harrisonog.musicvisualizer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylistsWithSongs().map { playlistsWithSongs ->
            playlistsWithSongs.map { it.toDomain() }
        }
    }

    override fun getPlaylistById(id: Long): Flow<Playlist?> {
        return playlistDao.getPlaylistWithSongs(id).map { playlistWithSongs ->
            playlistWithSongs?.toDomain()
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        val playlist = PlaylistEntity(
            name = name,
            createdAt = System.currentTimeMillis()
        )
        return playlistDao.insertPlaylist(playlist)
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist.toEntity())
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        val position = playlistDao.getNextPosition(playlistId)
        val crossRef = PlaylistSongCrossRef(
            playlistId = playlistId,
            songId = song.id,
            position = position
        )
        playlistDao.addSongToPlaylist(crossRef)
    }

    override suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        var position = playlistDao.getNextPosition(playlistId)
        val crossRefs = songs.map { song ->
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = song.id,
                position = position++
            )
        }
        playlistDao.addSongsToPlaylist(crossRefs)
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    override suspend fun reorderSongs(playlistId: Long, songId: Long, newPosition: Int) {
        playlistDao.updateSongPosition(playlistId, songId, newPosition)
    }
}
