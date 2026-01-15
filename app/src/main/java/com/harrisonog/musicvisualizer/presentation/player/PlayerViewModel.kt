package com.harrisonog.musicvisualizer.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.musicvisualizer.domain.model.PlaybackState
import com.harrisonog.musicvisualizer.domain.model.RepeatMode
import com.harrisonog.musicvisualizer.domain.model.Song
import com.harrisonog.musicvisualizer.service.MusicServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val serviceConnection: MusicServiceConnection
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = serviceConnection.playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlaybackState()
        )

    val currentSong: StateFlow<Song?> = serviceConnection.currentSong
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val queue: StateFlow<List<Song>> = serviceConnection.queue
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            serviceConnection.connect()
        }
    }

    fun play(song: Song) {
        serviceConnection.play(song)
    }

    fun playAll(songs: List<Song>, startIndex: Int = 0) {
        serviceConnection.playAll(songs, startIndex)
    }

    fun addToQueue(song: Song) {
        serviceConnection.addToQueue(song)
    }

    fun addToQueue(songs: List<Song>) {
        serviceConnection.addToQueue(songs)
    }

    fun togglePlayPause() {
        serviceConnection.togglePlayPause()
    }

    fun pause() {
        serviceConnection.pause()
    }

    fun resume() {
        serviceConnection.resume()
    }

    fun seekTo(position: Long) {
        serviceConnection.seekTo(position)
    }

    fun seekToProgress(progress: Float) {
        val duration = playbackState.value.duration
        if (duration > 0) {
            serviceConnection.seekTo((progress * duration).toLong())
        }
    }

    fun skipNext() {
        serviceConnection.skipNext()
    }

    fun skipPrevious() {
        serviceConnection.skipPrevious()
    }

    fun toggleShuffle() {
        serviceConnection.toggleShuffle()
    }

    fun cycleRepeatMode() {
        serviceConnection.cycleRepeatMode()
    }

    fun setRepeatMode(mode: RepeatMode) {
        serviceConnection.setRepeatMode(mode)
    }

    fun setShuffleMode(enabled: Boolean) {
        serviceConnection.setShuffleMode(enabled)
    }

    fun clearQueue() {
        serviceConnection.clearQueue()
    }

    fun removeFromQueue(index: Int) {
        serviceConnection.removeFromQueue(index)
    }

    fun moveQueueItem(from: Int, to: Int) {
        serviceConnection.moveQueueItem(from, to)
    }

    override fun onCleared() {
        super.onCleared()
        // Note: Don't disconnect here as other screens may still need the connection
        // The connection is a singleton and should persist
    }
}
