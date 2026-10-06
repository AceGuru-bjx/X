package com.unknown.security.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The living liquid every glass surface refracts: three deep color pools
 * (emerald, violet, amber) drift slowly across a near-black field.
 */
@Composable
fun LiquidBackground(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
) {
    val transition = rememberInfiniteTransition(label = "liquid")
    val driftA by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 26_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "driftA",
    )
    val driftB by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 34_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "driftB",
    )
    val driftC by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 21_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "driftC",
    )

    val base = if (darkTheme) Color(0xFF0A100D) else Color(0xFFEFF7F1)
    val pool1 = if (darkTheme) Color(0xFF12473A) else Color(0xFFBBEBD5)
    val pool2 = if (darkTheme) Color(0xFF35246B) else Color(0xFFDDD2F8)
    val pool3 = if (darkTheme) Color(0xFF5C4010) else Color(0xFFF8E3B5)

    Canvas(modifier = modifier) {
        drawRect(base)
        val w = size.width
        val h = size.height

        val centerA = Offset(w * (0.15f + 0.35f * driftA), h * (0.20f + 0.25f * driftB))
        val centerB = Offset(w * (0.85f - 0.30f * driftB), h * (0.75f - 0.35f * driftA))
        val centerC = Offset(w * driftC, h * (1.0f - 0.5f * driftC))

        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(pool1, Color.Transparent),
                    center = centerA,
                    radius = size.minDimension * 0.9f,
                ),
            radius = size.minDimension * 0.9f,
            center = centerA,
        )
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(pool2, Color.Transparent),
                    center = centerB,
                    radius = size.minDimension * 1.1f,
                ),
            radius = size.minDimension * 1.1f,
            center = centerB,
        )
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(pool3, Color.Transparent),
                    center = centerC,
                    radius = size.minDimension * 0.8f,
                ),
            radius = size.minDimension * 0.8f,
            center = centerC,
        )
    }
}
