package com.harrisonog.musicvisualizer.service.visualizer

/**
 * A circular (ring) buffer for accumulating float audio samples.
 * Supports efficient read/write with overlap for FFT processing.
 */
class CircularFloatBuffer(capacity: Int) {
    private val buffer = FloatArray(capacity)
    private var writePos = 0
    private var readPos = 0
    private var size = 0

    /**
     * Write samples to the buffer.
     * If buffer is full, oldest samples are overwritten.
     */
    fun write(samples: FloatArray) {
        for (sample in samples) {
            buffer[writePos] = sample
            writePos = (writePos + 1) % buffer.size
            if (size < buffer.size) {
                size++
            } else {
                // Buffer full, advance read position
                readPos = (readPos + 1) % buffer.size
            }
        }
    }

    /**
     * Read [count] samples from the buffer without advancing the read position.
     * Use [advance] to move the read position after processing.
     */
    fun read(count: Int): FloatArray {
        require(count <= size) { "Not enough samples: requested $count, available $size" }
        val result = FloatArray(count)
        for (i in 0 until count) {
            result[i] = buffer[(readPos + i) % buffer.size]
        }
        return result
    }

    /**
     * Advance the read position by [count] samples.
     * Used for overlapping window processing (e.g., 50% overlap with hop size).
     */
    fun advance(count: Int) {
        require(count <= size) { "Cannot advance by $count, only $size samples available" }
        readPos = (readPos + count) % buffer.size
        size -= count
    }

    /**
     * Returns the number of samples available for reading.
     */
    fun available(): Int = size

    /**
     * Clear all samples from the buffer.
     */
    fun clear() {
        writePos = 0
        readPos = 0
        size = 0
    }

    /**
     * Returns true if the buffer has at least [count] samples available.
     */
    fun hasAvailable(count: Int): Boolean = size >= count
}
