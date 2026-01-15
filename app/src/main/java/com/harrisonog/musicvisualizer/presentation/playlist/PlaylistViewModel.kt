package com.harrisonog.musicvisualizer.presentation.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.musicvisualizer.domain.model.Playlist
import com.harrisonog.musicvisualizer.domain.model.Song
import com.harrisonog.musicvisualizer.domain.repository.PlaylistRepository
import com.harrisonog.musicvisualizer.service.MusicServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<PlaylistUiState> = combine(
        playlistRepository.getAllPlaylists(),
        _isLoading
    ) { playlists, isLoading ->
        PlaylistUiState(
            playlists = playlists,
            isLoading = isLoading && playlists.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistUiState()
    )

    init {
        viewModelScope.launch {
            _isLoading.value = false
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            val playlist = uiState.value.playlists.find { it.id == playlistId }
            if (playlist != null) {
                playlistRepository.updatePlaylist(playlist.copy(name = newName))
            }
        }
    }

    fun playPlaylist(playlist: Playlist, shuffle: Boolean = false) {
        if (playlist.songs.isNotEmpty()) {
            if (shuffle) {
                musicServiceConnection.setShuffleMode(true)
            }
            musicServiceConnection.playAll(playlist.songs, 0)
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, song)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun reorderSong(playlistId: Long, songId: Long, newPosition: Int) {
        viewModelScope.launch {
            playlistRepository.reorderSongs(playlistId, songId, newPosition)
        }
    }
}
