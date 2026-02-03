package com.harrisonog.musicvisualizer.service

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import java.io.IOException

/**
 * Debug analytics listener for detailed ExoPlayer logging.
 * Helps diagnose playback issues like buffering loops.
 */
@OptIn(UnstableApi::class)
class DebugAnalyticsListener : AnalyticsListener {

    override fun onPlayerError(
        eventTime: AnalyticsListener.EventTime,
        error: PlaybackException
    ) {
        Log.e(TAG, "=== ANALYTICS: PLAYER ERROR ===")
        Log.e(TAG, "EventTime: window=${eventTime.windowIndex}, pos=${eventTime.eventPlaybackPositionMs}ms")
        Log.e(TAG, "Error code: ${error.errorCode} (${error.errorCodeName})")
        Log.e(TAG, "Message: ${error.message}")
        error.cause?.let { cause ->
            Log.e(TAG, "Cause: ${cause::class.java.simpleName}: ${cause.message}")
        }
    }

    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean
    ) {
        Log.e(TAG, "=== ANALYTICS: LOAD ERROR ===")
        Log.e(TAG, "URI: ${loadEventInfo.uri}")
        Log.e(TAG, "Data type: ${mediaLoadData.dataType}")
        Log.e(TAG, "Track type: ${mediaLoadData.trackType}")
        Log.e(TAG, "Error: ${error::class.java.simpleName}: ${error.message}")
        Log.e(TAG, "Was canceled: $wasCanceled")
    }

    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        Log.d(TAG, "Load started: ${loadEventInfo.uri}, dataType=${mediaLoadData.dataType}")
    }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        Log.d(TAG, "Load completed: ${loadEventInfo.uri}, bytes=${loadEventInfo.bytesLoaded}")
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: androidx.media3.common.Format,
        decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
    ) {
        Log.d(TAG, "Audio format: ${format.sampleMimeType}, ${format.sampleRate}Hz, ${format.channelCount}ch")
    }

    override fun onAudioDisabled(
        eventTime: AnalyticsListener.EventTime,
        decoderCounters: androidx.media3.exoplayer.DecoderCounters
    ) {
        Log.w(TAG, "=== ANALYTICS: AUDIO DISABLED ===")
        Log.w(TAG, "Rendered: ${decoderCounters.renderedOutputBufferCount}")
        Log.w(TAG, "Skipped: ${decoderCounters.skippedOutputBufferCount}")
        Log.w(TAG, "Dropped: ${decoderCounters.droppedBufferCount}")
    }

    override fun onAudioSinkError(
        eventTime: AnalyticsListener.EventTime,
        audioSinkError: Exception
    ) {
        Log.e(TAG, "=== ANALYTICS: AUDIO SINK ERROR ===")
        Log.e(TAG, "Error: ${audioSinkError::class.java.simpleName}: ${audioSinkError.message}")
        Log.e(TAG, "Stacktrace:", audioSinkError)
    }

    override fun onAudioCodecError(
        eventTime: AnalyticsListener.EventTime,
        audioCodecError: Exception
    ) {
        Log.e(TAG, "=== ANALYTICS: AUDIO CODEC ERROR ===")
        Log.e(TAG, "Error: ${audioCodecError::class.java.simpleName}: ${audioCodecError.message}")
    }

    companion object {
        private const val TAG = "DebugAnalytics"
    }
}
