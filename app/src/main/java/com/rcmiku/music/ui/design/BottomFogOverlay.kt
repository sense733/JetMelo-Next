package com.rcmiku.music.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import com.rcmiku.music.constants.MiniPlayerHeight

@Composable
fun BottomFogOverlay(
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val baseColor = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding)
            .height(MiniPlayerHeight)
            .background(
                Brush.verticalGradient(
                    0.0f to baseColor.copy(alpha = 0f),
                    0.25f to baseColor.copy(alpha = 0.10f),
                    0.5f to baseColor.copy(alpha = 0.35f),
                    0.75f to baseColor.copy(alpha = 0.70f),
                    1.0f to baseColor.copy(alpha = 1f)
                )
            )
    )
}
