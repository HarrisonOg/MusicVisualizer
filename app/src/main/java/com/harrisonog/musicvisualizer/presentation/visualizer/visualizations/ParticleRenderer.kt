package com.harrisonog.musicvisualizer.presentation.visualizer.visualizations

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.harrisonog.musicvisualizer.domain.model.VisualizerFrame
import com.harrisonog.musicvisualizer.presentation.visualizer.VisualizerRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Reactive particle system visualization.
 * Particles spawn and move based on audio intensity.
 */
class ParticleRenderer(
    private val maxParticles: Int = 200,
    private val baseSpeed: Float = 2f,
    private val particleSize: Float = 4f
) : VisualizerRenderer {

    override val id = "particles"
    override val displayName = "Particles"

    private val particles = mutableListOf<Particle>()
    private var canvasSize = Size.Zero
    private var centerX = 0f
    private var centerY = 0f

    private val colors = listOf(
        Color(0xFF00D4FF), // Cyan
        Color(0xFF00FF88), // Green
        Color(0xFFFFFF00), // Yellow
        Color(0xFFFF8800), // Orange
        Color(0xFFFF0088)  // Pink/Magenta
    )

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float, // 0-1, decays over time
        var color: Color,
        var size: Float
    )

    override fun onAttach(size: Size) {
        updateDimensions(size)
        particles.clear()
    }

    override fun onDetach() {
        particles.clear()
    }

    override fun onSizeChanged(size: Size) {
        updateDimensions(size)
    }

    private fun updateDimensions(size: Size) {
        canvasSize = size
        centerX = size.width / 2
        centerY = size.height / 2
    }

    override fun render(scope: DrawScope, frame: VisualizerFrame) {
        // Spawn new particles based on audio intensity
        spawnParticles(frame)

        // Update existing particles
        updateParticles(frame)

        // Remove dead particles
        particles.removeAll { it.life <= 0 }

        // Draw particles efficiently using drawPoints
        if (particles.isNotEmpty()) {
            drawParticles(scope)
        }

        // Draw center glow based on RMS
        drawCenterGlow(scope, frame.rms)
    }

    private fun spawnParticles(frame: VisualizerFrame) {
        if (frame.magnitudes.isEmpty()) return

        // Calculate spawn rate based on RMS (audio intensity)
        val spawnRate = (frame.rms * 10).toInt().coerceIn(1, 8)

        // Don't exceed max particles
        if (particles.size >= maxParticles) return

        repeat(spawnRate) {
            if (particles.size >= maxParticles) return@repeat

            // Pick a random frequency band to influence the particle
            val bandIndex = Random.nextInt(frame.magnitudes.size)
            val bandMagnitude = frame.magnitudes[bandIndex]

            // Skip if magnitude is too low
            if (bandMagnitude < 0.1f) return@repeat

            // Random angle for emission direction
            val angle = Random.nextFloat() * 2 * PI.toFloat()

            // Speed influenced by magnitude
            val speed = baseSpeed * (0.5f + bandMagnitude * 2f)

            // Color based on frequency band
            val colorProgress = bandIndex.toFloat() / frame.magnitudes.size
            val color = interpolateColor(colorProgress)

            // Spawn from center with some randomness
            val spawnRadius = 20f + Random.nextFloat() * 30f
            particles.add(
                Particle(
                    x = centerX + cos(angle) * spawnRadius,
                    y = centerY + sin(angle) * spawnRadius,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    life = 1f,
                    color = color,
                    size = particleSize * (0.5f + bandMagnitude)
                )
            )
        }
    }

    private fun updateParticles(frame: VisualizerFrame) {
        val speedMultiplier = 1f + frame.rms * 0.5f

        particles.forEach { particle ->
            // Update position
            particle.x += particle.vx * speedMultiplier
            particle.y += particle.vy * speedMultiplier

            // Decay life
            particle.life -= 0.015f

            // Add slight outward acceleration
            val dx = particle.x - centerX
            val dy = particle.y - centerY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            particle.vx += (dx / dist) * 0.05f
            particle.vy += (dy / dist) * 0.05f

            // Apply drag
            particle.vx *= 0.98f
            particle.vy *= 0.98f
        }
    }

    private fun drawParticles(scope: DrawScope) {
        // Group particles by approximate color for batch drawing
        // For simplicity, draw all particles individually but efficiently

        particles.forEach { particle ->
            val alpha = (particle.life * 0.9f).coerceIn(0f, 1f)
            scope.drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size * particle.life,
                center = Offset(particle.x, particle.y)
            )
        }

        // Draw particle trails (optional glow effect)
        val trailPoints = particles.filter { it.life > 0.3f }.map { particle ->
            Offset(
                particle.x - particle.vx * 2,
                particle.y - particle.vy * 2
            )
        }

        if (trailPoints.isNotEmpty()) {
            scope.drawPoints(
                points = trailPoints,
                pointMode = PointMode.Points,
                color = Color.White.copy(alpha = 0.2f),
                strokeWidth = particleSize * 0.5f
            )
        }
    }

    private fun drawCenterGlow(scope: DrawScope, rms: Float) {
        val glowRadius = 50f + rms * 100f
        val glowAlpha = (rms * 0.3f).coerceIn(0.05f, 0.25f)

        scope.drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00D4FF).copy(alpha = glowAlpha),
                    Color(0xFFFF00FF).copy(alpha = glowAlpha * 0.5f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(centerX, centerY)
        )
    }

    private fun interpolateColor(progress: Float): Color {
        val colorCount = colors.size
        val scaledProgress = progress * (colorCount - 1)
        val lowerIndex = scaledProgress.toInt().coerceIn(0, colorCount - 2)
        val upperIndex = lowerIndex + 1
        val fraction = scaledProgress - lowerIndex

        return lerp(colors[lowerIndex], colors[upperIndex], fraction)
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
