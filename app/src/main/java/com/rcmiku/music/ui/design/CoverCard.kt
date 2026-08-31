package com.rcmiku.music.ui.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rcmiku.music.ui.theme.JetMeloShapes

@Composable
fun CoverCard(
    imageUrl: Any?,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = JetMeloShapes.medium,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    overlayContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            )
            if (overlayContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    overlayContent()
                }
            }
        }
    }
}
