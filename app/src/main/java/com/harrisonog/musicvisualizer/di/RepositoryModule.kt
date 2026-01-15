package com.harrisonog.musicvisualizer.di

import com.harrisonog.musicvisualizer.data.repository.MusicRepositoryImpl
import com.harrisonog.musicvisualizer.data.repository.PlaylistRepositoryImpl
import com.harrisonog.musicvisualizer.domain.repository.MusicRepository
import com.harrisonog.musicvisualizer.domain.repository.PlaylistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        musicRepositoryImpl: MusicRepositoryImpl
    ): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        playlistRepositoryImpl: PlaylistRepositoryImpl
    ): PlaylistRepository
}
