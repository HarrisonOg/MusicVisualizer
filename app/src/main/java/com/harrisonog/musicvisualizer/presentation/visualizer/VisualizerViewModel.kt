package com.harrisonog.musicvisualizer.presentation.visualizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harrisonog.musicvisualizer.domain.model.VisualizerFrame
import com.harrisonog.musicvisualizer.presentation.visualizer.visualizations.CircularRenderer
import com.harrisonog.musicvisualizer.presentation.visualizer.visualizations.ParticleRenderer
import com.harrisonog.musicvisualizer.presentation.visualizer.visualizations.SpectrumRenderer
import com.harrisonog.musicvisualizer.presentation.visualizer.visualizations.WaveformRenderer
import com.harrisonog.musicvisualizer.service.visualizer.VisualizerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class VisualizerViewModel @Inject constructor(
    private val visualizerEngine: VisualizerEngine
) : ViewModel() {

    /**
     * Smoothed visualizer frames for rendering.
     */
    val visualizerFrame: StateFlow<VisualizerFrame> = visualizerEngine.smoothedFrames
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VisualizerFrame.EMPTY
        )

    /**
     * Available renderers.
     */
    val renderers: List<VisualizerRenderer> = listOf(
        SpectrumRenderer(),
        WaveformRenderer(),
        CircularRenderer(),
        ParticleRenderer()
    )

    private val _selectedRendererIndex = MutableStateFlow(0)
    val selectedRendererIndex: StateFlow<Int> = _selectedRendererIndex.asStateFlow()

    /**
     * Currently selected renderer.
     */
    val currentRenderer: VisualizerRenderer
        get() = renderers[_selectedRendererIndex.value]

    /**
     * Select a renderer by index.
     */
    fun selectRenderer(index: Int) {
        if (index in renderers.indices) {
            _selectedRendererIndex.value = index
        }
    }

    /**
     * Cycle to the next renderer.
     */
    fun nextRenderer() {
        _selectedRendererIndex.value = (_selectedRendererIndex.value + 1) % renderers.size
    }

    /**
     * Cycle to the previous renderer.
     */
    fun previousRenderer() {
        _selectedRendererIndex.value = if (_selectedRendererIndex.value > 0) {
            _selectedRendererIndex.value - 1
        } else {
            renderers.size - 1
        }
    }

    /**
     * Select a renderer by type.
     */
    fun selectRenderer(type: VisualizationType) {
        val index = renderers.indexOfFirst { it.id == type.id }
        if (index >= 0) {
            _selectedRendererIndex.value = index
        }
    }
}
