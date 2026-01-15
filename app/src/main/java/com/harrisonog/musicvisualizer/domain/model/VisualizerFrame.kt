package com.harrisonog.musicvisualizer.domain.model

/**
 * Represents a frame of visualizer data.
 *
 * @property magnitudes The magnitude values for each frequency bin.
 * @property rms The root-mean-square (RMS) value
 * @property timestampMs The timestamp in milliseconds.
 */
data class VisualizerFrame(
    val magnitudes: FloatArray,
    val rms: Float,
    val timestampMs: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VisualizerFrame

        if (!magnitudes.contentEquals(other.magnitudes)) return false
        if (rms != other.rms) return false
        if (timestampMs != other.timestampMs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = magnitudes.contentHashCode()
        result = 31 * result + rms.hashCode()
        result = 31 * result + timestampMs.hashCode()
        return result
    }

    companion object {
        val EMPTY = VisualizerFrame(
            magnitudes = FloatArray(0),
            rms = 0f,
            timestampMs = 0L
        )
    }
}
