package com.harrisonog.musicvisualizer.service.visualizer

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.harrisonog.musicvisualizer.domain.model.VisualizerFrame
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.jtransforms.fft.DoubleFFT_1D
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Audio processor that performs FFT analysis on audio samples.
 * Runs on ExoPlayer's audio thread - must be non-blocking.
 */
@Singleton
@OptIn(UnstableApi::class)
class FftAudioProcessor @Inject constructor() : BaseAudioProcessor() {

    private val fftSize: Int = 2048
    private val hopSize: Int = 1024 // 50% overlap

    // JTransforms FFT instance
    private val fft = DoubleFFT_1D(fftSize.toLong())

    // Pre-computed Hann window for reducing spectral leakage
    private val window = FloatArray(fftSize) { i ->
        (0.5 * (1 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
    }

    // Ring buffer for accumulating samples
    private val sampleBuffer = CircularFloatBuffer(fftSize * 4)

    // Non-blocking SharedFlow for emitting frames
    // CRITICAL: Use trySend(), never emit() on audio thread
    private val _frames = MutableSharedFlow<VisualizerFrame>(
        replay = 1,
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val frames: SharedFlow<VisualizerFrame> = _frames.asSharedFlow()

    private var inputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var isActive = false

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {
        this.inputAudioFormat = inputAudioFormat

        Log.d(TAG, "Audio format: ${inputAudioFormat.sampleRate}Hz, " +
                "${inputAudioFormat.channelCount}ch, " +
                "encoding=${inputAudioFormat.encoding}")

        // Validate encoding - must be PCM 16-bit or PCM Float
        isActive = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
                inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT

        if (!isActive) {
            Log.w(TAG, "Unsupported encoding: ${inputAudioFormat.encoding}, FFT disabled")
        }

        // Return input format unchanged (passthrough)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive) {
            return
        }

        try {
            // 1. Convert to float samples based on encoding
            val samples = when (inputAudioFormat.encoding) {
                C.ENCODING_PCM_16BIT -> convertPcm16ToFloat(inputBuffer)
                C.ENCODING_PCM_FLOAT -> convertPcmFloatToFloat(inputBuffer)
                else -> return
            }

            // 2. Handle stereo -> mono conversion
            val monoSamples = if (inputAudioFormat.channelCount == 2) {
                convertStereoToMono(samples)
            } else {
                samples
            }

            // 3. Accumulate in ring buffer
            sampleBuffer.write(monoSamples)

            // 4. Process when we have enough samples
            while (sampleBuffer.hasAvailable(fftSize)) {
                processFrame()
                sampleBuffer.advance(hopSize) // Overlap
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio input", e)
        }
    }

    private fun processFrame() {
        try {
            // Read samples from buffer
            val samples = sampleBuffer.read(fftSize)

            // Validate input
            if (samples.any { it.isNaN() || it.isInfinite() }) {
                Log.w(TAG, "Invalid samples detected, skipping frame")
                return
            }

            // Apply window and convert to double for FFT
            val windowed = DoubleArray(fftSize) { i ->
                (samples[i] * window[i]).toDouble()
            }

            // Perform FFT (in-place)
            fft.realForward(windowed)

            // Compute magnitudes
            val magnitudes = computeMagnitudes(windowed)

            // Validate output
            if (magnitudes.any { it.isNaN() || it.isInfinite() }) {
                Log.w(TAG, "Invalid FFT output, skipping frame")
                return
            }

            // Calculate RMS (overall energy)
            val rms = sqrt(samples.map { it * it }.average()).toFloat()
                .coerceIn(0f, 1f)

            // Emit frame (non-blocking!)
            val frame = VisualizerFrame(
                magnitudes = magnitudes,
                rms = rms,
                timestampMs = System.currentTimeMillis()
            )

            // NEVER use emit() - would block audio thread!
            _frames.tryEmit(frame)

        } catch (e: Exception) {
            Log.e(TAG, "FFT processing error", e)
        }
    }

    private fun computeMagnitudes(fftOutput: DoubleArray): FloatArray {
        val numBins = fftSize / 2
        val magnitudes = FloatArray(numBins)

        // DC component (index 0)
        magnitudes[0] = abs(fftOutput[0]).toFloat()

        // Nyquist component (stored at index 1 for real FFT)
        if (numBins > 1) {
            magnitudes[numBins - 1] = abs(fftOutput[1]).toFloat()
        }

        // Regular bins (paired real/imaginary)
        for (i in 1 until numBins - 1) {
            val real = fftOutput[2 * i]
            val imag = fftOutput[2 * i + 1]
            magnitudes[i] = sqrt(real * real + imag * imag).toFloat()
        }

        // Normalize to 0-1 range
        val maxMagnitude = magnitudes.maxOrNull() ?: 1f
        if (maxMagnitude > 0f) {
            for (i in magnitudes.indices) {
                magnitudes[i] = (magnitudes[i] / maxMagnitude).coerceIn(0f, 1f)
            }
        }

        return magnitudes
    }

    private fun convertPcm16ToFloat(buffer: ByteBuffer): FloatArray {
        val remaining = buffer.remaining()
        if (remaining < 2) return FloatArray(0)

        val samples = FloatArray(remaining / 2)
        val shortBuffer = buffer.asShortBuffer()
        for (i in samples.indices) {
            samples[i] = shortBuffer.get() / 32768f // Normalize to [-1, 1]
        }
        // Advance the original buffer position
        buffer.position(buffer.position() + remaining)
        return samples
    }

    private fun convertPcmFloatToFloat(buffer: ByteBuffer): FloatArray {
        val remaining = buffer.remaining()
        if (remaining < 4) return FloatArray(0)

        val samples = FloatArray(remaining / 4)
        val floatBuffer = buffer.asFloatBuffer()
        for (i in samples.indices) {
            samples[i] = floatBuffer.get()
        }
        // Advance the original buffer position
        buffer.position(buffer.position() + remaining)
        return samples
    }

    private fun convertStereoToMono(stereo: FloatArray): FloatArray {
        if (stereo.size < 2) return stereo
        val mono = FloatArray(stereo.size / 2)
        for (i in mono.indices) {
            mono[i] = (stereo[i * 2] + stereo[i * 2 + 1]) / 2f
        }
        return mono
    }

    override fun onFlush() {
        super.onFlush()
        sampleBuffer.clear()
    }

    override fun onReset() {
        super.onReset()
        sampleBuffer.clear()
        isActive = false
    }

    override fun isActive(): Boolean = isActive

    companion object {
        private const val TAG = "FftAudioProcessor"
    }
}
