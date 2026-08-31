package com.rcmiku.music.ui.design

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ImmersiveBackground(
    modifier: Modifier = Modifier,
    artworkUri: Uri? = null,
    dominantColor: Color? = null,
    scrimColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val artworkColors = LocalArtworkColors.current
    val effectiveDominant = dominantColor ?: artworkColors.dominantColor
    val effectiveScrim = scrimColor ?: artworkColors.surfaceScrim

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (artworkUri != null) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.2f)
                        .blur(28.dp)
                )
            }
        } else {
            val blurredBitmap = artworkColors.blurredBitmap
            if (blurredBitmap != null && !blurredBitmap.isRecycled) {
                Image(
                    bitmap = blurredBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.2f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            effectiveDominant.copy(alpha = 0.55f),
                            effectiveDominant.copy(alpha = 0.85f),
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
                            effectiveScrim.copy(alpha = 0.45f),
                            effectiveScrim.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        content()
    }
}
