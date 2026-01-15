package com.harrisonog.musicvisualizer.presentation.visualizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harrisonog.musicvisualizer.presentation.player.PlayerViewModel
import com.harrisonog.musicvisualizer.presentation.visualizer.components.VisualizerCanvas
import com.harrisonog.musicvisualizer.presentation.visualizer.components.VisualizerOverlay
import kotlinx.coroutines.delay

private const val SWIPE_THRESHOLD = 100f
private const val AUTO_HIDE_DELAY = 5000L

@Composable
fun VisualizerScreen(
    modifier: Modifier = Modifier,
    visualizerViewModel: VisualizerViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val frame by visualizerViewModel.visualizerFrame.collectAsStateWithLifecycle()
    val selectedRendererIndex by visualizerViewModel.selectedRendererIndex.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()

    var showOverlay by remember { mutableStateOf(true) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val currentRenderer = visualizerViewModel.renderers.getOrNull(selectedRendererIndex)
        ?: visualizerViewModel.renderers.first()

    // Attach renderer when selected
    DisposableEffect(selectedRendererIndex) {
        onDispose {
            currentRenderer.onDetach()
        }
    }

    // Auto-hide overlay after delay
    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(AUTO_HIDE_DELAY)
            showOverlay = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    showOverlay = !showOverlay
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragOffset = 0f },
                    onDragEnd = {
                        if (dragOffset > SWIPE_THRESHOLD) {
                            visualizerViewModel.previousRenderer()
                            showOverlay = true
                        } else if (dragOffset < -SWIPE_THRESHOLD) {
                            visualizerViewModel.nextRenderer()
                            showOverlay = true
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            }
    ) {
        // Visualization canvas
        VisualizerCanvas(
            frame = frame,
            renderer = currentRenderer,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with song info
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            VisualizerOverlay(
                currentSong = currentSong,
                visualizationName = currentRenderer.displayName
            )
        }
    }
}
