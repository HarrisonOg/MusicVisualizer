package com.harrisonog.musicvisualizer.service.visualizer

import com.harrisonog.musicvisualizer.domain.model.VisualizerFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine that processes raw FFT frames and applies smoothing/normalization.
 * Converts raw SharedFlow from FftAudioProcessor into a smoothed StateFlow for UI.
 */
@Singleton
class VisualizerEngine @Inject constructor(
    private val fftAudioProcessor: FftAudioProcessor
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    // Smoothing factor for exponential moving average (0-1)
    // Higher = more responsive, Lower = smoother
    private val smoothingFactor = 0.3f

    /**
     * Smoothed visualizer frames ready for UI consumption.
     * Uses exponential moving average for temporal smoothing.
     */
    val smoothedFrames: StateFlow<VisualizerFrame> = fftAudioProcessor.frames
        .runningFold<VisualizerFrame, VisualizerFrame?>(null) { previous, current ->
            if (previous == null) {
                current
            } else {
                smoothFrames(previous, current)
            }
        }
        .map { it ?: VisualizerFrame.EMPTY }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VisualizerFrame.EMPTY
        )

    /**
     * Raw unsmoothed frames for use cases that need immediate response.
     */
    val rawFrames: Flow<VisualizerFrame> = fftAudioProcessor.frames

    /**
     * Apply exponential moving average smoothing between frames.
     */
    private fun smoothFrames(previous: VisualizerFrame, current: VisualizerFrame): VisualizerFrame {
        // Handle different magnitude array sizes
        val prevMagnitudes = previous.magnitudes
        val currMagnitudes = current.magnitudes

        if (prevMagnitudes.isEmpty()) return current
        if (currMagnitudes.isEmpty()) return previous

        // Use the size of current frame
        val smoothedMagnitudes = FloatArray(currMagnitudes.size) { i ->
            if (i < prevMagnitudes.size) {
                // Exponential moving average
                prevMagnitudes[i] * (1 - smoothingFactor) + currMagnitudes[i] * smoothingFactor
            } else {
                currMagnitudes[i]
            }
        }

        // Smooth RMS as well
        val smoothedRms = previous.rms * (1 - smoothingFactor) + current.rms * smoothingFactor

        return VisualizerFrame(
            magnitudes = smoothedMagnitudes,
            rms = smoothedRms,
            timestampMs = current.timestampMs
        )
    }

    /**
     * Get a downsampled version of magnitudes for visualization.
     * Useful for reducing the number of bars in spectrum analyzer.
     */
    fun downsampleMagnitudes(magnitudes: FloatArray, targetBars: Int): FloatArray {
        if (magnitudes.isEmpty() || targetBars <= 0) return FloatArray(0)
        if (magnitudes.size <= targetBars) return magnitudes.copyOf()

        val result = FloatArray(targetBars)
        val binSize = magnitudes.size.toFloat() / targetBars

        for (i in 0 until targetBars) {
            val startIdx = (i * binSize).toInt()
            val endIdx = ((i + 1) * binSize).toInt().coerceAtMost(magnitudes.size)

            // Take the maximum value in each bin for more visual impact
            var maxValue = 0f
            for (j in startIdx until endIdx) {
                if (magnitudes[j] > maxValue) {
                    maxValue = magnitudes[j]
                }
            }
            result[i] = maxValue
        }

        return result
    }

    /**
     * Apply logarithmic scaling to magnitudes for more musical visualization.
     * Low frequencies get more visual weight.
     */
    fun applyLogScale(magnitudes: FloatArray): FloatArray {
        if (magnitudes.isEmpty()) return magnitudes

        return FloatArray(magnitudes.size) { i ->
            // Apply log scaling with offset to avoid log(0)
            val scaled = kotlin.math.log10(1 + magnitudes[i] * 9) // Maps 0-1 to 0-1 with log curve
            scaled.coerceIn(0f, 1f)
        }
    }
}
