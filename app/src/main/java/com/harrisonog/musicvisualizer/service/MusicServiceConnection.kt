package com.harrisonog.musicvisualizer.service

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.harrisonog.musicvisualizer.domain.model.PlaybackState
import com.harrisonog.musicvisualizer.domain.model.RepeatMode
import com.harrisonog.musicvisualizer.domain.model.Song
import com.harrisonog.musicvisualizer.service.player.MediaItemMapper.toMediaItem
import com.harrisonog.musicvisualizer.service.player.MediaItemMapper.toMediaItems
import com.harrisonog.musicvisualizer.service.player.MediaItemMapper.toSongWithDuration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(UnstableApi::class)
class MusicServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private var positionUpdateJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentSong()
            updatePlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            updatePlaybackState()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            updatePlaybackState()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            updateCurrentSong()
        }
    }

    fun connect() {
        if (mediaController != null) return

        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(playerListener)
                updatePlaybackState()
                updateCurrentSong()
                updateQueue()
            },
            MoreExecutors.directExecutor()
        )
    }

    fun disconnect() {
        stopPositionUpdates()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
    }

    fun play(song: Song) {
        mediaController?.run {
            setMediaItem(song.toMediaItem())
            prepare()
            play()
        }
    }

    fun playAll(songs: List<Song>, startIndex: Int = 0) {
        mediaController?.run {
            setMediaItems(songs.toMediaItems(), startIndex, 0L)
            prepare()
            play()
        }
        updateQueue()
    }

    fun addToQueue(song: Song) {
        mediaController?.addMediaItem(song.toMediaItem())
        updateQueue()
    }

    fun addToQueue(songs: List<Song>) {
        mediaController?.addMediaItems(songs.toMediaItems())
        updateQueue()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        mediaController?.play()
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        updatePlaybackState()
    }

    fun skipNext() {
        mediaController?.let { controller ->
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
            }
        }
    }

    fun skipPrevious() {
        mediaController?.let { controller ->
            if (controller.currentPosition > 3000) {
                // If more than 3 seconds into song, restart current
                controller.seekTo(0)
            } else if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
            } else {
                controller.seekTo(0)
            }
        }
    }

    fun setShuffleMode(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
    }

    fun toggleShuffle() {
        mediaController?.let { controller ->
            controller.shuffleModeEnabled = !controller.shuffleModeEnabled
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        mediaController?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun cycleRepeatMode() {
        mediaController?.let { controller ->
            controller.repeatMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun clearQueue() {
        mediaController?.clearMediaItems()
        updateQueue()
    }

    fun removeFromQueue(index: Int) {
        mediaController?.removeMediaItem(index)
        updateQueue()
    }

    fun moveQueueItem(from: Int, to: Int) {
        mediaController?.moveMediaItem(from, to)
        updateQueue()
    }

    private fun updatePlaybackState() {
        mediaController?.let { controller ->
            val repeatMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> RepeatMode.OFF
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            }

            _playbackState.value = PlaybackState(
                isPlaying = controller.isPlaying,
                currentSong = _currentSong.value,
                currentPosition = controller.currentPosition,
                duration = controller.duration.coerceAtLeast(0),
                repeatMode = repeatMode,
                shuffleEnabled = controller.shuffleModeEnabled,
                isBuffering = controller.playbackState == Player.STATE_BUFFERING
            )
        }
    }

    private fun updateCurrentSong() {
        mediaController?.let { controller ->
            val currentItem = controller.currentMediaItem
            val duration = controller.duration.coerceAtLeast(0)
            _currentSong.value = currentItem?.toSongWithDuration(duration)

            // Update playback state with current song
            _playbackState.value = _playbackState.value.copy(
                currentSong = _currentSong.value,
                duration = duration
            )
        }
    }

    private fun updateQueue() {
        mediaController?.let { controller ->
            val songs = mutableListOf<Song>()
            for (i in 0 until controller.mediaItemCount) {
                controller.getMediaItemAt(i).toSongWithDuration(0)?.let { songs.add(it) }
            }
            _queue.value = songs
        }
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = scope.launch {
            while (isActive) {
                updatePlaybackState()
                delay(1000) // Update every second
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    val isConnected: Boolean
        get() = mediaController != null

    val hasNextTrack: Boolean
        get() = mediaController?.hasNextMediaItem() == true

    val hasPreviousTrack: Boolean
        get() = mediaController?.hasPreviousMediaItem() == true
}
