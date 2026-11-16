package com.inkflow.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Production-grade Animated Entry Splash Screen for InkFlow Studio.
 * Plays on launch:
 * 1. Fades in a warm espresso grid backdrop.
 * 2. Springs the InkFlow Studio badge into place.
 * 3. Live-draws the golden fountain nib & calligraphic ink wave across the badge.
 * 4. Smoothly fades out into the user's Studio workspace.
 */
@Composable
fun InkFlowEntrySplash(
    onSplashFinished: () -> Unit
) {
    val badgeScale = remember { Animatable(0.55f) }
    val strokeProgress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            delay(150)
            strokeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(320)
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 480)
            )
        }
        delay(1550)
        screenAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 360)
        )
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141210),
                        Color(0xFF1C1917),
                        Color(0xFF292018)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle animated infinite grid lines in the background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 42.dp.toPx()
            val gridAlpha = 0.08f * strokeProgress.value
            var x = 0f
            while (x <= size.width) {
                drawLine(
                    color = Color(0xFFF59E0B).copy(alpha = gridAlpha),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(
                    color = Color(0xFFF59E0B).copy(alpha = gridAlpha),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += step
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Logo Emblem
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .scale(badgeScale.value)
                    .clip(RoundedCornerShape(34.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF292524),
                                Color(0xFF451A03),
                                Color(0xFFD97706)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Full calligraphic ink wave path
                    val fullWave = Path().apply {
                        moveTo(w * 0.16f, h * 0.72f)
                        cubicTo(
                            w * 0.36f, h * 0.22f,
                            w * 0.58f, h * 0.86f,
                            w * 0.82f, h * 0.30f
                        )
                    }

                    val measure = PathMeasure()
                    measure.setPath(fullWave, false)
                    val animatedWave = Path()
                    measure.getSegment(
                        startDistance = 0f,
                        stopDistance = measure.length * strokeProgress.value,
                        destination = animatedWave,
                        startWithMoveTo = true
                    )

                    drawPath(
                        path = animatedWave,
                        color = Color(0xFFFAF7F2),
                        style = Stroke(width = w * 0.095f, cap = StrokeCap.Round)
                    )

                    if (strokeProgress.value > 0.7f) {
                        val dotScale = ((strokeProgress.value - 0.7f) / 0.3f).coerceIn(0f, 1f)
                        drawCircle(
                            color = Color(0xFFFBBF24),
                            radius = (w * 0.11f) * dotScale,
                            center = Offset(w * 0.82f, h * 0.28f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "InkFlow",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp
                ),
                color = Color(0xFFFAF7F2),
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "INFINITE CANVAS • SMART STUDIO • ONENOTE SYNC",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 1.4.sp
                ),
                color = Color(0xFFFBBF24).copy(alpha = 0.90f),
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}
