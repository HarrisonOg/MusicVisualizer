package com.harrisonog.musicvisualizer.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.musicvisualizer.domain.model.Song
import com.harrisonog.musicvisualizer.domain.repository.MusicRepository
import com.harrisonog.musicvisualizer.service.MusicServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption {
    TITLE, ARTIST, ALBUM, DURATION
}

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.TITLE,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<LibraryUiState> = combine(
        musicRepository.getAllSongs(),
        _searchQuery,
        _sortOption,
        _isLoading
    ) { songs, query, sortOption, isLoading ->
        val filteredSongs = songs
            .filter { song -> matchesSearch(song, query) }
            .sortedWith(getSortComparator(sortOption))

        LibraryUiState(
            songs = filteredSongs,
            isLoading = isLoading && songs.isEmpty(),
            searchQuery = query,
            sortOption = sortOption
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    init {
        refreshLibrary()
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                musicRepository.refreshMusicLibrary()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun playSong(song: Song) {
        val currentSongs = uiState.value.songs
        val index = currentSongs.indexOf(song)
        if (index >= 0) {
            musicServiceConnection.playAll(currentSongs, index)
        } else {
            musicServiceConnection.play(song)
        }
    }

    fun playAll(startIndex: Int = 0) {
        val songs = uiState.value.songs
        if (songs.isNotEmpty()) {
            musicServiceConnection.playAll(songs, startIndex)
        }
    }

    fun addToQueue(song: Song) {
        musicServiceConnection.addToQueue(song)
    }

    private fun matchesSearch(song: Song, query: String): Boolean {
        if (query.isBlank()) return true
        val lowerQuery = query.lowercase()
        return song.title.lowercase().contains(lowerQuery) ||
                song.artist.lowercase().contains(lowerQuery) ||
                song.album.lowercase().contains(lowerQuery)
    }

    private fun getSortComparator(option: SortOption): Comparator<Song> {
        return when (option) {
            SortOption.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortOption.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
            SortOption.ALBUM -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.album }
            SortOption.DURATION -> compareBy { it.duration }
        }
    }
}
