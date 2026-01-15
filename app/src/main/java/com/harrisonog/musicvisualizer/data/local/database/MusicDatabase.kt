package com.harrisonog.musicvisualizer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.harrisonog.musicvisualizer.data.local.dao.PlaylistDao
import com.harrisonog.musicvisualizer.data.local.dao.SongDao
import com.harrisonog.musicvisualizer.data.local.entity.PlaylistEntity
import com.harrisonog.musicvisualizer.data.local.entity.PlaylistSongCrossRef
import com.harrisonog.musicvisualizer.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val DATABASE_NAME = "music_database"
    }
}
