package com.rcmiku.music.ui.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rcmiku.music.constants.ListItemHeight
import com.rcmiku.music.constants.ListThumbnailSize
import com.rcmiku.music.ui.theme.AdaptiveArtworkShape
import com.rcmiku.music.constants.ThumbnailCornerRadius
import com.rcmiku.music.ui.theme.JetMeloShapes

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        if (description != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        if (actionText != null && onActionClick != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onActionClick,
                shape = JetMeloShapes.full
            ) {
                Text(text = actionText)
            }
        }
    }
}

@Composable
fun rememberShimmerBrush(enabled: Boolean = true): Brush {
    if (!enabled) {
        return Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
            )
        )
    }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.15f),
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim - 500f, y = translateAnim - 500f),
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

@Composable
fun SkeletonPlaceholder(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = JetMeloShapes.medium
) {
    val brush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Composable
fun HeroBannerSkeleton(
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(JetMeloShapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(JetMeloShapes.medium)
                        .background(brush)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(16.dp)
                                .clip(JetMeloShapes.full)
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(18.dp)
                                .clip(JetMeloShapes.small)
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(14.dp)
                                .clip(JetMeloShapes.small)
                                .background(brush)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(JetMeloShapes.full)
                                .background(brush)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistsRowSkeleton(
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush()
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(4) {
            Column(
                modifier = Modifier.width(160.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(JetMeloShapes.large)
                        .background(brush)
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(16.dp)
                        .clip(JetMeloShapes.extraSmall)
                        .background(brush)
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(14.dp)
                        .clip(JetMeloShapes.extraSmall)
                        .background(brush)
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DailySongsGridSkeleton(
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush()
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(4),
        modifier = modifier
            .fillMaxWidth()
            .height(ListItemHeight * 4),
        contentPadding = PaddingValues(horizontal = 10.dp),
        userScrollEnabled = false
    ) {
        items(8) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(340.dp)
                    .height(ListItemHeight)
                    .padding(horizontal = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(ListThumbnailSize)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                        .background(brush)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .clip(JetMeloShapes.extraSmall)
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(12.dp)
                            .clip(JetMeloShapes.extraSmall)
                            .background(brush)
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailSkeleton(
    modifier: Modifier = Modifier,
    showCoverSkeleton: Boolean = true,
    brush: Brush = rememberShimmerBrush(),
    bottomContentPadding: Dp = 0.dp
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
        userScrollEnabled = false
    ) {
        item(key = "skeleton_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showCoverSkeleton) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .shadow(elevation = 12.dp, shape = JetMeloShapes.medium)
                            .clip(JetMeloShapes.medium)
                            .background(brush)
                    )
                } else {
                    Spacer(Modifier.size(220.dp))
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(24.dp)
                        .clip(JetMeloShapes.small)
                        .background(brush)
                )

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(14.dp)
                        .clip(JetMeloShapes.extraSmall)
                        .background(brush)
                )

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(12.dp)
                        .clip(JetMeloShapes.extraSmall)
                        .background(brush)
                )

                Spacer(Modifier.height(16.dp))
            }
        }

        item(key = "skeleton_play_bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(44.dp)
                        .clip(JetMeloShapes.large)
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(JetMeloShapes.full)
                        .background(brush)
                )
            }
        }

        items(count = 8, key = { index -> "skeleton_track_$index" }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(AdaptiveArtworkShape)
                        .background(brush)
                )

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(16.dp)
                            .clip(JetMeloShapes.extraSmall)
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp)
                            .clip(JetMeloShapes.extraSmall)
                            .background(brush)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(JetMeloShapes.full)
                        .background(brush)
                )
            }
        }
    }
}
