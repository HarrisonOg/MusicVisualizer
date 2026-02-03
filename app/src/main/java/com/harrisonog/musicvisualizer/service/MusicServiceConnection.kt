package com.harrisonog.musicvisualizer.service

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
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
import kotlinx.coroutines.flow.first
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

    private val _isConnected = MutableStateFlow(false)
    val isConnectedFlow: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var positionUpdateJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d(TAG, "onMediaItemTransition: ${mediaItem?.mediaMetadata?.title}")
            updateCurrentSong()
            updatePlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateName = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }
            Log.d(TAG, "=== PLAYBACK STATE: $stateName ===")

            // Log additional context for debugging
            mediaController?.let { controller ->
                Log.d(TAG, "  currentPosition: ${controller.currentPosition}ms")
                Log.d(TAG, "  duration: ${controller.duration}ms")
                Log.d(TAG, "  playWhenReady: ${controller.playWhenReady}")
                Log.d(TAG, "  currentMediaItem: ${controller.currentMediaItem?.mediaId}")
                Log.d(TAG, "  currentMediaItemUri: ${controller.currentMediaItem?.localConfiguration?.uri}")
            }

            updatePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "onIsPlayingChanged: $isPlaying")
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
            Log.d(TAG, "onMediaMetadataChanged: ${mediaMetadata.title}")
            updateCurrentSong()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "=== PLAYER ERROR ===")
            Log.e(TAG, "Error code: ${error.errorCode}")
            Log.e(TAG, "Error code name: ${error.errorCodeName}")
            Log.e(TAG, "Message: ${error.message}")
            Log.e(TAG, "Cause: ${error.cause}")
            error.cause?.let { cause ->
                Log.e(TAG, "Cause message: ${cause.message}")
                Log.e(TAG, "Cause stacktrace:", cause)
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            val reasonName = when (reason) {
                Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> "AUTO_TRANSITION"
                Player.DISCONTINUITY_REASON_SEEK -> "SEEK"
                Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> "SEEK_ADJUSTMENT"
                Player.DISCONTINUITY_REASON_SKIP -> "SKIP"
                Player.DISCONTINUITY_REASON_REMOVE -> "REMOVE"
                Player.DISCONTINUITY_REASON_INTERNAL -> "INTERNAL"
                else -> "UNKNOWN($reason)"
            }
            Log.d(TAG, "=== POSITION DISCONTINUITY ===")
            Log.d(TAG, "Reason: $reasonName")
            Log.d(TAG, "Old: mediaIdx=${oldPosition.mediaItemIndex}, pos=${oldPosition.positionMs}ms")
            Log.d(TAG, "New: mediaIdx=${newPosition.mediaItemIndex}, pos=${newPosition.positionMs}ms")
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val reasonName = when (reason) {
                Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED -> "PLAYLIST_CHANGED"
                Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE -> "SOURCE_UPDATE"
                else -> "UNKNOWN($reason)"
            }
            Log.d(TAG, "Timeline changed: $reasonName, windowCount=${timeline.windowCount}")
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            val reasonName = when (reason) {
                Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> "USER_REQUEST"
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> "AUDIO_FOCUS_LOSS"
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> "AUDIO_BECOMING_NOISY"
                Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> "REMOTE"
                Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> "END_OF_MEDIA_ITEM"
                Player.PLAY_WHEN_READY_CHANGE_REASON_SUPPRESSED_TOO_LONG -> "SUPPRESSED_TOO_LONG"
                else -> "UNKNOWN($reason)"
            }
            Log.d(TAG, "PlayWhenReady changed: $playWhenReady, reason: $reasonName")
        }
    }

    fun connect() {
        if (mediaController != null) {
            Log.d(TAG, "Already connected")
            return
        }

        Log.d(TAG, "Connecting to MusicService...")
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                try {
                    mediaController = controllerFuture?.get()
                    if (mediaController != null) {
                        Log.d(TAG, "Connected to MusicService successfully")
                        mediaController?.addListener(playerListener)
                        _isConnected.value = true
                        updatePlaybackState()
                        updateCurrentSong()
                        updateQueue()
                    } else {
                        Log.e(TAG, "MediaController is null after connection")
                        _isConnected.value = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect to MusicService", e)
                    _isConnected.value = false
                    mediaController = null
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting from MusicService")
        stopPositionUpdates()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
        _isConnected.value = false
    }

    /**
     * Suspends until the MediaController is connected.
     * Use this before calling play commands to avoid race conditions.
     */
    suspend fun awaitConnection() {
        if (mediaController != null) return
        _isConnected.first { it }
    }

    fun play(song: Song) {
        val controller = mediaController
        if (controller == null) {
            Log.w(TAG, "Cannot play: MediaController not connected")
            return
        }

        Log.d(TAG, "Playing song: ${song.title}, URI: ${song.contentUri}")
        val mediaItem = song.toMediaItem()
        Log.d(TAG, "MediaItem URI: ${mediaItem.localConfiguration?.uri}")

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    /**
     * Suspending version of play that awaits connection first.
     * Use this from ViewModels to avoid race conditions.
     */
    suspend fun playSuspending(song: Song) {
        awaitConnection()
        play(song)
    }

    fun playAll(songs: List<Song>, startIndex: Int = 0) {
        val controller = mediaController
        if (controller == null) {
            Log.w(TAG, "Cannot playAll: MediaController not connected")
            return
        }

        Log.d(TAG, "Playing ${songs.size} songs starting at index $startIndex")
        val mediaItems = songs.toMediaItems()
        mediaItems.forEachIndexed { index, item ->
            Log.d(TAG, "  Item $index: ${item.mediaMetadata.title}, URI: ${item.localConfiguration?.uri}")
        }

        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
        updateQueue()
    }

    /**
     * Suspending version of playAll that awaits connection first.
     * Use this from ViewModels to avoid race conditions.
     */
    suspend fun playAllSuspending(songs: List<Song>, startIndex: Int = 0) {
        awaitConnection()
        playAll(songs, startIndex)
    }

    fun addToQueue(song: Song) {
        val controller = mediaController
        if (controller == null) {
            Log.w(TAG, "Cannot addToQueue: MediaController not connected")
            return
        }
        controller.addMediaItem(song.toMediaItem())
        updateQueue()
    }

    fun addToQueue(songs: List<Song>) {
        val controller = mediaController
        if (controller == null) {
            Log.w(TAG, "Cannot addToQueue: MediaController not connected")
            return
        }
        controller.addMediaItems(songs.toMediaItems())
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

    companion object {
        private const val TAG = "MusicServiceConnection"
    }
}
