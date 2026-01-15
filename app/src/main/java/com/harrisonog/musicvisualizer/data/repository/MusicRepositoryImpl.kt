package com.harrisonog.musicvisualizer.data.repository

import com.harrisonog.musicvisualizer.data.local.dao.SongDao
import com.harrisonog.musicvisualizer.data.local.entity.toDomain
import com.harrisonog.musicvisualizer.data.local.entity.toEntity
import com.harrisonog.musicvisualizer.data.mediasource.MediaStoreSource
import com.harrisonog.musicvisualizer.domain.model.Song
import com.harrisonog.musicvisualizer.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val mediaStoreSource: MediaStoreSource
) : MusicRepository {

    override fun getAllSongs(): Flow<List<Song>> {
        return songDao.getAllSongs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSongById(id: Long): Flow<Song?> {
        return songDao.getSongById(id).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun refreshMusicLibrary() {
        val songs = mediaStoreSource.querySongs()
        val entities = songs.map { it.toEntity() }
        songDao.deleteAll()
        songDao.insertAll(entities)
    }

    override fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
