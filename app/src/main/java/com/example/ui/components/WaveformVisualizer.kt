package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CosmicCardBorderAlt
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.VioletNeon
import kotlin.math.sin

@Composable
fun AudioVisualizerOrb(
    amplitude: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    isExecutingAction: Boolean = false,
    modifier: Modifier = Modifier
) {
    val smoothAmplitude by animateFloatAsState(
        targetValue = if (isListening) amplitude.coerceIn(0.08f, 1.0f) else 0.08f,
        animationSpec = tween(durationMillis = 100),
        label = "amplitude"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnim"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "spin"
    )

    // Dynamic scale factor based on real audio amplitude or speaking state
    val scale = when {
        isListening -> 1f + (smoothAmplitude * 0.35f)
        isSpeaking -> 1f + (pulseAnim * 0.12f)
        isProcessing || isExecutingAction -> pulseAnim
        else -> 1f
    }

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing Aura & Orbit Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.width * 0.34f
            val currentRadius = baseRadius * scale

            // Outer Aura Rings
            if (isListening || isSpeaking || isProcessing || isExecutingAction) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyanNeon.copy(alpha = if (isListening) 0.35f else 0.22f),
                            VioletNeon.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = currentRadius * 1.45f
                    ),
                    radius = currentRadius * 1.45f,
                    center = center
                )

                // Outer Glowing Energy Orbit Ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            CyanNeon,
                            VioletNeon,
                            PurpleAccent,
                            CyanNeon
                        ),
                        center = center
                    ),
                    radius = currentRadius * 1.08f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Branded Official BUSHRA AI Icon in Center with Circular Presentation & Border Glow
        val imageDiameter = (130 * scale).dp
        Box(
            modifier = Modifier
                .size(imageDiameter)
                .clip(CircleShape)
                .border(
                    width = 2.5.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(CyanNeon, VioletNeon, PurpleAccent, CyanNeon)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.bushra_app_icon),
                contentDescription = "BUSHRA AI Official Brand Icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun WaveformBars(
    amplitude: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 13
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing)
        ),
        label = "wave"
    )

    val smoothAmp by animateFloatAsState(
        targetValue = if (isListening) amplitude.coerceIn(0.08f, 1f) else 0.08f,
        animationSpec = tween(durationMillis = 80),
        label = "smoothAmp"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val width = size.width
        val maxHeight = size.height
        val barWidth = 6.dp.toPx()
        val spacing = (width - (barCount * barWidth)) / (barCount + 1)
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (i in 0 until barCount) {
            val x = spacing + i * (barWidth + spacing)

            val heightMultiplier = when {
                isListening -> {
                    val distFromCenter = Math.abs(i - (barCount / 2f)) / (barCount / 2f)
                    val factor = 1f - (distFromCenter * 0.4f)
                    (smoothAmp * factor).coerceIn(0.1f, 1f)
                }
                isSpeaking -> {
                    val phase = waveOffset + (i * 0.45f)
                    ((sin(phase) + 1f) / 2f * 0.8f + 0.2f).coerceIn(0.15f, 1f)
                }
                else -> 0.12f
            }

            val barHeight = (maxHeight * heightMultiplier).coerceAtLeast(6.dp.toPx())
            val top = (maxHeight - barHeight) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(CyanNeon, VioletNeon)
                ),
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}
