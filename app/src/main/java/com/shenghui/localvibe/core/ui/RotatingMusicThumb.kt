package com.shenghui.localvibe.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun RotatingMusicThumb(
    artworkUri: String? = null,
    isRotating: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "music-thumb-rotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "music-thumb-angle"
    )
    val angle = if (isRotating) rotation else 0f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.86f)
                .graphicsLayer { rotationZ = angle }
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4B2E76),
                            Color(0xFF1B1630),
                            Color(0xFF07080F)
                        )
                    )
                )
                .border(
                    width = 1.8.dp,
                    color = Color(0xFFD6B8FF).copy(alpha = 0.56f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(7.dp)
            ) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val moonRadius = radius * 0.78f
                val coverRadius = moonRadius * 0.96f
                val coverCenter = Offset(
                    x = center.x + moonRadius * 0.43f,
                    y = center.y - moonRadius * 0.04f
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD7FF).copy(alpha = 0.96f),
                            Color(0xFFC792FF).copy(alpha = 0.92f),
                            Color(0xFF7B45EC).copy(alpha = 0.86f)
                        ),
                        center = Offset(center.x - moonRadius * 0.24f, center.y - moonRadius * 0.28f),
                        radius = moonRadius * 1.15f
                    ),
                    radius = moonRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF151020),
                    radius = coverRadius,
                    center = coverCenter
                )
            }
        }
    }
}
