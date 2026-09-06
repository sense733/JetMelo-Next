package com.rcmiku.music.ui.design

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun ImmersiveBackground(
    modifier: Modifier = Modifier,
    artworkUri: Any? = null,
    dominantColor: Color? = null,
    scrimColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val artworkColors = LocalArtworkColors.current
    val effectiveDominant = dominantColor ?: artworkColors.dominantColor
    val effectiveScrim = scrimColor ?: artworkColors.surfaceScrim

    val animatedDominant by animateColorAsState(
        targetValue = effectiveDominant,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "immersive_dominant_color"
    )
    val animatedScrim by animateColorAsState(
        targetValue = effectiveScrim,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "immersive_scrim_color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Crossfade(
                targetState = artworkUri,
                animationSpec = tween(durationMillis = 600),
                label = "immersive_artwork_crossfade",
                modifier = Modifier.fillMaxSize()
            ) { uri ->
                if (uri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uri)
                            .size(128, 128)
                            .crossfade(600)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.2f)
                            .blur(28.dp)
                    )
                }
            }
        } else {
            val blurredBitmap = artworkColors.blurredBitmap
            Crossfade(
                targetState = blurredBitmap,
                animationSpec = tween(durationMillis = 600),
                label = "immersive_bitmap_crossfade",
                modifier = Modifier.fillMaxSize()
            ) { bitmap ->
                if (bitmap != null && !bitmap.isRecycled) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.2f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            animatedDominant.copy(alpha = 0.55f),
                            animatedDominant.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            animatedScrim.copy(alpha = 0.45f),
                            animatedScrim.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        content()
    }
}
