package com.harrisonog.musicvisualizer.presentation.visualizer.visualizations

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.harrisonog.musicvisualizer.domain.model.VisualizerFrame
import com.harrisonog.musicvisualizer.presentation.visualizer.VisualizerRenderer
import kotlin.math.log10

/**
 * Spectrum analyzer visualization - frequency bars.
 */
class SpectrumRenderer(
    private val barCount: Int = 64,
    private val barSpacing: Float = 0.15f, // 15% gap between bars
    private val cornerRadius: Float = 4f,
    private val useGradient: Boolean = true
) : VisualizerRenderer {

    override val id = "spectrum"
    override val displayName = "Spectrum"

    // Pre-computed colors for gradient effect
    private val gradientColors = listOf(
        Color(0xFF00D4FF), // Cyan
        Color(0xFF00FF88), // Green
        Color(0xFFFFFF00), // Yellow
        Color(0xFFFF8800), // Orange
        Color(0xFFFF0088)  // Pink
    )

    private var canvasSize = Size.Zero

    override fun onAttach(size: Size) {
        canvasSize = size
    }

    override fun onSizeChanged(size: Size) {
        canvasSize = size
    }

    override fun render(scope: DrawScope, frame: VisualizerFrame) {
        if (frame.magnitudes.isEmpty()) return

        val magnitudes = downsample(frame.magnitudes, barCount)
        val barWidth = scope.size.width / barCount
        val barWidthWithGap = barWidth * (1 - barSpacing)
        val maxHeight = scope.size.height * 0.95f

        magnitudes.forEachIndexed { index, magnitude ->
            // Apply logarithmic scaling for more musical visualization
            val scaledMagnitude = applyLogScale(magnitude)
            val barHeight = (scaledMagnitude * maxHeight).coerceAtLeast(2f)

            val x = index * barWidth + (barWidth - barWidthWithGap) / 2
            val y = scope.size.height - barHeight

            if (useGradient) {
                // Gradient based on bar position (frequency)
                val colorProgress = index.toFloat() / barCount
                val color = interpolateColor(colorProgress)

                // Draw bar with vertical gradient
                scope.drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.3f)),
                        startY = y,
                        endY = scope.size.height
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidthWithGap, barHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            } else {
                scope.drawRoundRect(
                    color = Color.Cyan,
                    topLeft = Offset(x, y),
                    size = Size(barWidthWithGap, barHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }
    }

    private fun downsample(magnitudes: FloatArray, targetCount: Int): FloatArray {
        if (magnitudes.size <= targetCount) return magnitudes

        val result = FloatArray(targetCount)
        val binSize = magnitudes.size.toFloat() / targetCount

        for (i in 0 until targetCount) {
            val startIdx = (i * binSize).toInt()
            val endIdx = ((i + 1) * binSize).toInt().coerceAtMost(magnitudes.size)

            // Take maximum value in bin for visual impact
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

    private fun applyLogScale(value: Float): Float {
        // Logarithmic scaling for more musical representation
        return (log10(1 + value * 9) / 1f).coerceIn(0f, 1f)
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
