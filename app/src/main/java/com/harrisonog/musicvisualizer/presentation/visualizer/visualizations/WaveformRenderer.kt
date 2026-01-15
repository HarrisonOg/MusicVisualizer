package com.harrisonog.musicvisualizer.presentation.visualizer.visualizations

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.harrisonog.musicvisualizer.domain.model.VisualizerFrame
import com.harrisonog.musicvisualizer.presentation.visualizer.VisualizerRenderer

/**
 * Waveform visualization - smooth oscilloscope-style wave.
 */
class WaveformRenderer(
    private val lineWidth: Float = 3f,
    private val useGradient: Boolean = true,
    private val mirrorWave: Boolean = true
) : VisualizerRenderer {

    override val id = "waveform"
    override val displayName = "Waveform"

    private val primaryColor = Color(0xFF00D4FF) // Cyan
    private val secondaryColor = Color(0xFFFF00FF) // Magenta

    private var canvasSize = Size.Zero

    override fun onAttach(size: Size) {
        canvasSize = size
    }

    override fun onSizeChanged(size: Size) {
        canvasSize = size
    }

    override fun render(scope: DrawScope, frame: VisualizerFrame) {
        if (frame.magnitudes.isEmpty()) return

        val magnitudes = frame.magnitudes
        val width = scope.size.width
        val height = scope.size.height
        val centerY = height / 2

        // Create primary waveform path
        val path = Path()
        val pointCount = magnitudes.size.coerceAtMost(256)
        val stepX = width / (pointCount - 1)

        // Start path
        path.moveTo(0f, centerY)

        for (i in 0 until pointCount) {
            val x = i * stepX
            // Map magnitude to vertical displacement
            val magnitude = if (i < magnitudes.size) magnitudes[i] else 0f
            val displacement = magnitude * height * 0.4f
            val y = centerY - displacement

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                // Use quadratic bezier for smooth curves
                val prevX = (i - 1) * stepX
                val prevMagnitude = if (i - 1 < magnitudes.size) magnitudes[i - 1] else 0f
                val prevY = centerY - prevMagnitude * height * 0.4f
                val controlX = (prevX + x) / 2
                path.quadraticTo(controlX, prevY, x, y)
            }
        }

        // Draw primary wave
        val brush = if (useGradient) {
            Brush.horizontalGradient(
                colors = listOf(primaryColor, secondaryColor, primaryColor)
            )
        } else {
            Brush.linearGradient(colors = listOf(primaryColor, primaryColor))
        }

        scope.drawPath(
            path = path,
            brush = brush,
            style = Stroke(
                width = lineWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw mirrored wave if enabled
        if (mirrorWave) {
            val mirrorPath = Path()

            for (i in 0 until pointCount) {
                val x = i * stepX
                val magnitude = if (i < magnitudes.size) magnitudes[i] else 0f
                val displacement = magnitude * height * 0.4f
                val y = centerY + displacement // Mirror below center

                if (i == 0) {
                    mirrorPath.moveTo(x, y)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevMagnitude = if (i - 1 < magnitudes.size) magnitudes[i - 1] else 0f
                    val prevY = centerY + prevMagnitude * height * 0.4f
                    val controlX = (prevX + x) / 2
                    mirrorPath.quadraticTo(controlX, prevY, x, y)
                }
            }

            scope.drawPath(
                path = mirrorPath,
                brush = brush,
                alpha = 0.5f,
                style = Stroke(
                    width = lineWidth * 0.7f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Draw center line glow based on RMS
        val glowAlpha = (frame.rms * 0.5f).coerceIn(0.1f, 0.3f)
        scope.drawLine(
            color = primaryColor.copy(alpha = glowAlpha),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )
    }
}
