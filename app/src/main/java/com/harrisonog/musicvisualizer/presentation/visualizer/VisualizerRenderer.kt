package com.harrisonog.musicvisualizer.presentation.visualizer

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.harrisonog.musicvisualizer.domain.model.VisualizerFrame

/**
 * Plugin interface for visualization renderers.
 * Each renderer implements a different visualization style.
 */
interface VisualizerRenderer {
    /**
     * Unique identifier for this renderer.
     */
    val id: String

    /**
     * Display name shown to users.
     */
    val displayName: String

    /**
     * Called when this renderer is selected/attached.
     * Use for pre-computing lookup tables, initializing state.
     */
    fun onAttach(size: Size) {}

    /**
     * Called when this renderer is deselected/detached.
     * Use for releasing resources, cleanup.
     */
    fun onDetach() {}

    /**
     * Called when canvas size changes (rotation, split-screen, etc).
     * Use for recalculating layout-dependent values.
     */
    fun onSizeChanged(size: Size) {}

    /**
     * Render the visualization frame.
     * This is called on every frame - must be efficient!
     *
     * @param scope The DrawScope for drawing operations
     * @param frame The current visualizer frame data
     */
    fun render(scope: DrawScope, frame: VisualizerFrame)
}

/**
 * Enumeration of available visualization types.
 */
enum class VisualizationType(val id: String, val displayName: String) {
    SPECTRUM("spectrum", "Spectrum"),
    WAVEFORM("waveform", "Waveform"),
    CIRCULAR("circular", "Circular"),
    PARTICLES("particles", "Particles")
}
