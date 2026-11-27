package com.rcmiku.music.ui.design

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Renders a light, theme-adaptive ambient player background consisting of a subtly tinted base,
 * an extended top-to-mid blurred artwork wash, and dual radial color glows.
 *
 * @param enableAmbientDrift Enables gentle, low-amplitude glow motion intended for lyric view.
 */
@Composable
fun ImmersiveBackground(
    modifier: Modifier = Modifier,
    artworkUri: Any? = null,
    dominantColor: Color? = null,
    @Suppress("UNUSED_PARAMETER") scrimColor: Color? = null,
    enableAmbientDrift: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val artworkColors = LocalArtworkColors.current
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.background.luminance() < 0.5f
    val baseSurfaceColor = if (isDarkTheme) colorScheme.background else colorScheme.surface
    val baseBlendFactor = if (isDarkTheme) 0.10f else 0.07f

    val effectiveAmbient = dominantColor ?: artworkColors.ambientColor
    val animatedAmbientState = animateColorAsState(
        targetValue = effectiveAmbient,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "immersive_ambient_color"
    )
    val animatedAccentState = animateColorAsState(
        targetValue = artworkColors.accentColor,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "immersive_accent_color"
    )

    val driftStrengthState = animateFloatAsState(
        targetValue = if (enableAmbientDrift) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "ambient_drift_strength"
    )

    val shouldRunDriftTransition by remember(enableAmbientDrift) {
        derivedStateOf { enableAmbientDrift || driftStrengthState.value > 0.001f }
    }

    val phaseAState: State<Float>?
    val phaseBState: State<Float>?
    if (shouldRunDriftTransition) {
        val infiniteTransition = rememberInfiniteTransition(label = "lyric_ambient_drift")
        val twoPi = (2.0 * PI).toFloat()
        phaseAState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = twoPi,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 15000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ambient_phase_a"
        )
        phaseBState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = twoPi,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 21000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ambient_phase_b"
        )
    } else {
        phaseAState = null
        phaseBState = null
    }

    val context = LocalContext.current
    val washBitmap by produceState<ImageBitmap?>(
        initialValue = PaletteExtractor.getCachedBlurredWash(artworkUri = artworkUri, songId = null),
        key1 = artworkUri,
        key2 = context
    ) {
        value = PaletteExtractor.getCachedBlurredWash(artworkUri = artworkUri, songId = null)
            ?: PaletteExtractor.getOrCreateBlurredWash(
                context = context,
                artworkUri = artworkUri,
                songId = null
            )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val tintedBase = lerp(baseSurfaceColor, animatedAmbientState.value, baseBlendFactor)
                drawRect(color = tintedBase)
            }
    ) {
        Crossfade(
            targetState = washBitmap,
            animationSpec = tween(durationMillis = 600),
            label = "immersive_wash_crossfade",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val ambientLum = animatedAmbientState.value.luminance()
                    val deepCoverScale = 0.76f + (ambientLum / 0.30f).coerceIn(0f, 1f) * 0.24f
                    alpha = (if (isDarkTheme) 0.34f else 0.38f) * deepCoverScale
                }
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val baseSpan = max(w, h)
            if (baseSpan <= 0f) return@Canvas

            val animatedAmbient = animatedAmbientState.value
            val animatedAccent = animatedAccentState.value
            val tintedBase = lerp(baseSurfaceColor, animatedAmbient, baseBlendFactor)
            val ambientLum = animatedAmbient.luminance()
            val deepCoverScale = 0.76f + (ambientLum / 0.30f).coerceIn(0f, 1f) * 0.24f

            val driftStrength = driftStrengthState.value
            val phaseA = phaseAState?.value ?: 0f
            val phaseB = phaseBState?.value ?: 0f
            val driftX1 = sin(phaseA) * driftStrength
            val driftY1 = cos(phaseA * 0.5f + 0.6f) * driftStrength
            val driftX2 = cos(phaseB) * driftStrength
            val driftY2 = sin(phaseB + 1.1f) * driftStrength
            val breathe = sin(phaseA + phaseB * 0.5f) * driftStrength

            val primaryRadius = max(1f, baseSpan * (0.72f + 0.05f * breathe))
            val primaryCenter = Offset(
                x = w * (0.48f + 0.10f * driftX1),
                y = h * (0.20f + 0.05f * driftY1)
            )
            val primaryPeak = (if (isDarkTheme) 0.34f else 0.38f) * deepCoverScale
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to animatedAmbient.copy(alpha = primaryPeak),
                    0.40f to animatedAmbient.copy(alpha = primaryPeak * 0.48f),
                    0.72f to animatedAmbient.copy(alpha = primaryPeak * 0.14f),
                    1.0f to Color.Transparent,
                    center = primaryCenter,
                    radius = primaryRadius
                ),
                radius = primaryRadius,
                center = primaryCenter
            )

            val secondaryRadius = max(1f, baseSpan * 0.54f)
            val secondaryCenter = Offset(
                x = w * (0.80f - 0.12f * driftX2),
                y = h * (0.15f + 0.06f * driftY2)
            )
            val secondaryPeakAlpha = (if (isDarkTheme) 0.18f else 0.20f) * deepCoverScale
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to animatedAccent.copy(alpha = secondaryPeakAlpha),
                    0.52f to animatedAccent.copy(alpha = secondaryPeakAlpha * 0.35f),
                    1.0f to Color.Transparent,
                    center = secondaryCenter,
                    radius = secondaryRadius
                ),
                radius = secondaryRadius,
                center = secondaryCenter
            )

            drawRect(
                brush = Brush.verticalGradient(
                    0.52f to Color.Transparent,
                    0.74f to tintedBase.copy(alpha = 0.78f),
                    1.0f to tintedBase
                )
            )
        }

        content()
    }
}
