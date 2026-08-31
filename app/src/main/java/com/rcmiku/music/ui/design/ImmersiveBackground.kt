package com.rcmiku.music.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun ImmersiveBackground(
    modifier: Modifier = Modifier,
    dominantColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    scrimColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        dominantColor,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            scrimColor
                        )
                    )
                )
        )
        content()
    }
}
