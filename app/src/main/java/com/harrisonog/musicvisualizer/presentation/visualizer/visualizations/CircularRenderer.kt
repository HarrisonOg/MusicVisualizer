package com.harrisonog.musicvisualizer.presentation.visualizer.visualizations

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.harrisonog.musicvisualizer.domain.model.VisualizerFrame
import com.harrisonog.musicvisualizer.presentation.visualizer.VisualizerRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Circular/radial spectrum visualization.
 * Bars emanate outward from center in a circle.
 */
class CircularRenderer(
    private val barCount: Int = 64,
    private val innerRadiusRatio: Float = 0.3f,
    private val maxBarLengthRatio: Float = 0.35f,
    private val lineWidth: Float = 3f,
    private val useGradient: Boolean = true,
    private val mirrorBars: Boolean = true
) : VisualizerRenderer {

    override val id = "circular"
    override val displayName = "Circular"

    private val gradientColors = listOf(
        Color(0xFF00D4FF), // Cyan
        Color(0xFF00FF88), // Green
        Color(0xFFFFFF00), // Yellow
        Color(0xFFFF8800), // Orange
        Color(0xFFFF0088)  // Pink
    )

    private var canvasSize = Size.Zero
    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var maxBarLength = 0f

    // Pre-computed angles for bars
    private var angles = FloatArray(barCount)

    override fun onAttach(size: Size) {
        updateDimensions(size)
    }

    override fun onSizeChanged(size: Size) {
        updateDimensions(size)
    }

    private fun updateDimensions(size: Size) {
        canvasSize = size
        centerX = size.width / 2
        centerY = size.height / 2
        val minDimension = min(size.width, size.height)
        baseRadius = minDimension * innerRadiusRatio
        maxBarLength = minDimension * maxBarLengthRatio

        // Pre-compute angles
        val angleStep = (2 * PI / barCount).toFloat()
        for (i in 0 until barCount) {
            angles[i] = i * angleStep - (PI / 2).toFloat() // Start from top
        }
    }

    override fun render(scope: DrawScope, frame: VisualizerFrame) {
        if (frame.magnitudes.isEmpty()) return

        val magnitudes = downsample(frame.magnitudes, barCount)

        // Draw center glow based on RMS
        drawCenterGlow(scope, frame.rms)

        // Draw bars
        magnitudes.forEachIndexed { index, magnitude ->
            val angle = angles[index]
            val barLength = magnitude * maxBarLength

            // Calculate start and end points
            val startX = centerX + cos(angle) * baseRadius
            val startY = centerY + sin(angle) * baseRadius
            val endX = centerX + cos(angle) * (baseRadius + barLength)
            val endY = centerY + sin(angle) * (baseRadius + barLength)

            // Get color based on position
            val color = if (useGradient) {
                interpolateColor(index.toFloat() / barCount)
            } else {
                Color.Cyan
            }

            // Draw outer bar
            scope.drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )

            // Draw mirrored inner bar if enabled
            if (mirrorBars) {
                val innerEndX = centerX + cos(angle) * (baseRadius - barLength * 0.5f)
                val innerEndY = centerY + sin(angle) * (baseRadius - barLength * 0.5f)

                scope.drawLine(
                    color = color.copy(alpha = 0.5f),
                    start = Offset(startX, startY),
                    end = Offset(innerEndX, innerEndY),
                    strokeWidth = lineWidth * 0.7f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw center circle outline
        scope.drawCircle(
            brush = Brush.sweepGradient(
                colors = gradientColors + gradientColors.first(),
                center = Offset(centerX, centerY)
            ),
            radius = baseRadius,
            center = Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }

    private fun drawCenterGlow(scope: DrawScope, rms: Float) {
        val glowRadius = baseRadius * 0.8f
        val glowAlpha = (rms * 0.4f).coerceIn(0.05f, 0.3f)

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00D4FF).copy(alpha = glowAlpha),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(centerX, centerY)
        )
    }

    private fun downsample(magnitudes: FloatArray, targetCount: Int): FloatArray {
        if (magnitudes.size <= targetCount) return magnitudes

        val result = FloatArray(targetCount)
        val binSize = magnitudes.size.toFloat() / targetCount

        for (i in 0 until targetCount) {
            val startIdx = (i * binSize).toInt()
            val endIdx = ((i + 1) * binSize).toInt().coerceAtMost(magnitudes.size)

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

    private fun interpolateColor(progress: Float): Color {
        val colorCount = gradientColors.size
        val scaledProgress = progress * (colorCount - 1)
        val lowerIndex = scaledProgress.toInt().coerceIn(0, colorCount - 2)
        val upperIndex = lowerIndex + 1
        val fraction = scaledProgress - lowerIndex

        return lerp(gradientColors[lowerIndex], gradientColors[upperIndex], fraction)
    }

    private fun lerp(start: Color, end: Color, fraction: Float): Color {
        return Color(
            red = start.red + (end.red - start.red) * fraction,
            green = start.green + (end.green - start.green) * fraction,
            blue = start.blue + (end.blue - start.blue) * fraction,
            alpha = start.alpha + (end.alpha - start.alpha) * fraction
        )
    }
}
