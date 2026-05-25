package com.example.medreminder.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import kotlin.random.Random

data class ConfettiParticle(
    val x: Float, // Normalized x
    val y: Float, // Normalized y
    val color: Color,
    val size: Float,
    val speedX: Float,
    val speedY: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val isCircle: Boolean
)

@Composable
fun ConfettiEffect(
    trigger: Boolean,
    onAnimationEnd: () -> Unit
) {
    if (!trigger) return

    val colors = remember {
        listOf(
            Color(0xFF80CBC4), // Pill Mint
            Color(0xFFB39DDB), // Pill Lavender
            Color(0xFF90CAF9), // Pill Blue
            Color(0xFFF48FB1), // Pill Pink
            Color(0xFFFFCC80), // Pill Orange
            Color(0xFFFFEE58)  // Calm Yellow
        )
    }

    val particles = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            repeat(100) {
                add(
                    ConfettiParticle(
                        x = 0.5f,
                        y = 0.7f,
                        color = colors.random(),
                        size = Random.nextFloat() * 15f + 12f,
                        speedX = (Random.nextFloat() - 0.5f) * 0.05f,
                        speedY = -(Random.nextFloat() * 0.08f + 0.05f),
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 12f,
                        isCircle = Random.nextBoolean()
                    )
                )
            }
        }
    }

    var frame by remember { mutableStateOf(0) }

    LaunchedEffect(trigger) {
        // Runs simulation for 2.5 seconds at roughly 60 fps
        repeat(150) {
            delay(16)
            for (i in particles.indices) {
                val p = particles[i]
                particles[i] = p.copy(
                    x = p.x + p.speedX,
                    y = p.y + p.speedY,
                    speedY = p.speedY + 0.0018f, // Gravity effect
                    rotation = p.rotation + p.rotationSpeed
                )
            }
            frame++
        }
        onAnimationEnd()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            val px = p.x * width
            val py = p.y * height

            // Draw only if it is visible on screen
            if (px in 0f..width && py in 0f..height) {
                rotate(p.rotation, pivot = Offset(px, py)) {
                    if (p.isCircle) {
                        drawCircle(
                            color = p.color,
                            radius = p.size / 2,
                            center = Offset(px, py)
                        )
                    } else {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(px - p.size / 2, py - p.size / 2),
                            size = Size(p.size, p.size * 0.5f)
                        )
                    }
                }
            }
        }
    }
}
