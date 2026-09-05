package com.rcmiku.music.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TopFogOverlay(
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    baseColor: Color = MaterialTheme.colorScheme.background
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    0.0f to baseColor.copy(alpha = 1f),
                    0.25f to baseColor.copy(alpha = 0.70f),
                    0.5f to baseColor.copy(alpha = 0.35f),
                    0.75f to baseColor.copy(alpha = 0.10f),
                    1.0f to baseColor.copy(alpha = 0f)
                )
            )
    )
}
