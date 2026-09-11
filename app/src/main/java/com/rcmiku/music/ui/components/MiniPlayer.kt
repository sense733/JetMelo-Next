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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.media3.common.Player.STATE_READY
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.rcmiku.music.R
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.constants.MiniPlayerHeight
import com.rcmiku.music.ui.design.LocalArtworkColors
import com.rcmiku.music.ui.icons.Pause
import com.rcmiku.music.ui.icons.PlayArrow
import com.rcmiku.music.ui.icons.SkipNext
import com.rcmiku.music.ui.theme.JetMeloShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun MiniPlayer(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    controlsAlpha: Float = 1f,
    controlsOffsetY: Dp = 0.dp,
    showArtwork: Boolean = true,
    onArtworkPositioned: ((Rect) -> Unit)? = null
) {
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val artworkColors = LocalArtworkColors.current

    val showMiniPlayer = (playerState?.timeline?.windowCount ?: 0) != 0

    if (showMiniPlayer) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .height(MiniPlayerHeight - 12.dp)
                .shadow(elevation = 6.dp, shape = JetMeloShapes.medium)
                .clip(JetMeloShapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = JetMeloShapes.medium
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = "展开播放器",
                    enabled = controlsAlpha > 0.1f,
                    onClick = onClick
                )
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
                        imageModifier = imageModifier,
                        controlsAlpha = controlsAlpha,
                        controlsOffsetY = controlsOffsetY,
                        showArtwork = showArtwork,
                        onArtworkPositioned = onArtworkPositioned
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        alpha = controlsAlpha
                        translationY = controlsOffsetY.toPx()
                    }
                ) {
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
                            contentDescription = stringResource(if (playerState?.isPlaying == true) R.string.pause else R.string.play),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            mediaController?.seekToNext()
                        }
                    ) {
                        Icon(
                            imageVector = SkipNext,
                            contentDescription = "下一首",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            MiniPlayerProgressBar(
                accentColor = artworkColors.accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 60.dp, end = 12.dp)
                    .height(2.5.dp)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        alpha = controlsAlpha
                    }
            )
        }
    }
}

@Composable
fun MiniMediaInfo(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    controlsAlpha: Float = 1f,
    controlsOffsetY: Dp = 0.dp,
    showArtwork: Boolean = true,
    onArtworkPositioned: ((Rect) -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) {
                        onArtworkPositioned?.invoke(coords.boundsInRoot())
                    }
                }
        ) {
            if (showArtwork) {
                val context = LocalContext.current
                val artworkUri = mediaMetadata.artworkUri
                val imageRequest = remember(context, artworkUri) {
                    val key = artworkUri?.toString()
                    ImageRequest.Builder(context)
                        .data(artworkUri)
                        .size(Size(176, 176))
                        .memoryCacheKey(key)
                        .diskCacheKey(key)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
                        .fillMaxSize()
                        .clip(JetMeloShapes.small)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .graphicsLayer {
                    alpha = controlsAlpha
                    translationY = controlsOffsetY.toPx()
                }
        ) {
            mediaMetadata.title?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@Composable
fun MiniPlayerProgressBar(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val playerState = LocalPlayerState.current
    val playbackState = playerState?.playbackState
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId
    val lifecycleOwner = LocalLifecycleOwner.current

    var position by remember(playerState?.player) {
        mutableLongStateOf(playerState?.player?.currentPosition ?: 0L)
    }
    var duration by remember(playerState?.player) {
        mutableLongStateOf(playerState?.player?.duration?.coerceAtLeast(0L) ?: 0L)
    }

    LaunchedEffect(playbackState, isPlaying) {
        if (playbackState == STATE_READY && isPlaying) {
            while (isActive) {
                if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    delay(1000)
                    continue
                }
                position = playerState?.player?.currentPosition ?: 0L
                val dur = playerState?.player?.duration ?: 0L
                duration = if (dur > 0) dur else 0L
                delay(200)
            }
        } else if (playbackState == STATE_READY) {
            position = playerState?.player?.currentPosition ?: 0L
            val dur = playerState?.player?.duration ?: 0L
            duration = if (dur > 0) dur else 0L
        }
    }

    LaunchedEffect(currentMediaId) {
        position = 0L
        duration = 0L
    }

    val progressTarget = if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        label = "mini_player_progress"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier,
        color = accentColor,
        trackColor = MaterialTheme.colorScheme.outlineVariant
    )
}
