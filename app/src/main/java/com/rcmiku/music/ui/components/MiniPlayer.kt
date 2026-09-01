package com.rcmiku.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.constants.MiniPlayerHeight
import com.rcmiku.music.ui.design.LocalArtworkColors
import com.rcmiku.music.ui.icons.Pause
import com.rcmiku.music.ui.icons.PlayArrow
import com.rcmiku.music.ui.icons.SkipNext
import com.rcmiku.music.ui.theme.JetMeloShapes

@Composable
fun MiniPlayer(
    mediaMetadata: MediaMetadata,
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val artworkColors = LocalArtworkColors.current

    val showMiniPlayer = (playerState?.player?.mediaItemCount ?: 0) != 0

    val progressTarget = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        label = "mini_player_progress"
    )

    if (showMiniPlayer) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .height(MiniPlayerHeight - 12.dp)
                .shadow(elevation = 6.dp, shape = JetMeloShapes.medium)
                .clip(JetMeloShapes.medium)
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = Color(0x1F000000),
                    shape = JetMeloShapes.medium
                )
                .clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 4.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    MiniMediaInfo(
                        mediaMetadata = mediaMetadata,
                        imageModifier = imageModifier
                    )
                }

                IconButton(
                    onClick = {
                        if (playerState?.isPlaying == true)
                            mediaController?.pause()
                        else
                            mediaController?.play()
                    }
                ) {
                    Icon(
                        imageVector = if (playerState?.isPlaying == true) Pause else PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF1C1B1F)
                    )
                }

                IconButton(
                    onClick = {
                        mediaController?.seekToNext()
                    }
                ) {
                    Icon(
                        imageVector = SkipNext,
                        contentDescription = null,
                        tint = Color(0xFF1C1B1F)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.BottomCenter),
                color = artworkColors.accentColor,
                trackColor = Color(0x1F000000)
            )
        }
    }
}

@Composable
fun MiniMediaInfo(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        AsyncImage(
            model = mediaMetadata.artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = imageModifier
                .size(44.dp)
                .clip(JetMeloShapes.small)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            mediaMetadata.title?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1C1B1F),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
            }
            mediaMetadata.artist?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF49454F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}
