package com.harrisonog.musicvisualizer.presentation.visualizer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import com.harrisonog.musicvisualizer.domain.model.VisualizerFrame
import com.harrisonog.musicvisualizer.presentation.visualizer.VisualizerRenderer

@Composable
fun VisualizerCanvas(
    frame: VisualizerFrame,
    renderer: VisualizerRenderer,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { newSize ->
                val size = newSize.toSize()
                if (size != canvasSize) {
                    canvasSize = size
                    renderer.onSizeChanged(size)
                }
            }
    ) {
        renderer.render(this, frame)
    }
}
