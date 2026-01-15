package com.harrisonog.musicvisualizer.domain.repository

import com.harrisonog.musicvisualizer.domain.model.RepeatMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user preferences.
 *
 * @property repeatMode The current repeat mode.
 * @property shuffleEnabled Whether shuffling is enabled.
 * @property visualizerEnabled Whether the visualizer is enabled.
 * @property visualizerSensitivity The sensitivity of the visualizer.
 */
interface PreferencesRepository {
    val repeatMode: Flow<RepeatMode>
    val shuffleEnabled: Flow<Boolean>
    val visualizerEnabled: Flow<Boolean>
    val visualizerSensitivity: Flow<Int>

    suspend fun setRepeatMode(mode: RepeatMode)
    suspend fun setShuffleEnabled(enabled: Boolean)
    suspend fun setVisualizerEnabled(enabled: Boolean)
    suspend fun setVisualizerSensitivity(sensitivity: Int)
    suspend fun saveLastPlaybackState(songId: Long, positionMs: Long)
    suspend fun getLastPlaybackState(): Pair<Long, Long>?
}
