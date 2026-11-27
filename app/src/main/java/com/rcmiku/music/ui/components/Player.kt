package com.rcmiku.music.ui.components

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.kyant.capsule.ContinuousRoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import com.rcmiku.music.ui.icons.PlaylistInsert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.rcmiku.music.R
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.constants.MediaSessionConstants
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.ui.design.ImmersiveBackground
import com.rcmiku.music.ui.design.LocalArtworkColors
import com.rcmiku.music.ui.design.rememberPlayerContentColors
import com.rcmiku.music.ui.icons.Album
import com.rcmiku.music.ui.icons.Artist
import com.rcmiku.music.ui.icons.ChevronDown
import com.rcmiku.music.ui.icons.Favorite
import com.rcmiku.music.ui.icons.FavoriteFill
import com.rcmiku.music.ui.icons.PauseFill
import com.rcmiku.music.ui.icons.SkipNextFill
import com.rcmiku.music.ui.icons.SkipPreviousFill
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.ui.navigation.ArtistNav
import com.rcmiku.music.ui.theme.AdaptiveArtworkShape
import com.rcmiku.music.ui.theme.JetMeloShapes
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import com.rcmiku.music.ui.theme.TitleHeroLarge
import com.rcmiku.music.utils.getItemShape
import com.rcmiku.music.utils.makeTimeString
import com.rcmiku.ncmapi.model.Artist
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.SongAlbum
import com.rcmiku.ncmapi.utils.json
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

internal fun MediaController.sendCommandWithFeedback(context: Context, command: SessionCommand) {
    if (!isConnected) return
    val future = sendCustomCommand(command, Bundle.EMPTY)
    Futures.addCallback(
        future,
        object : FutureCallback<SessionResult> {
            override fun onSuccess(result: SessionResult) {
                if (result.resultCode != SessionResult.RESULT_SUCCESS) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.operation_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(t: Throwable) {
                Toast.makeText(
                    context,
                    context.getString(R.string.operation_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        ContextCompat.getMainExecutor(context)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Player(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    artistModifier: Modifier = Modifier,
    artworkElevation: Dp = 16.dp,
    artworkElevationProvider: (() -> Dp)? = null,
    onBackPressed: () -> Unit = {},
    onClick: () -> Unit = {},
    onContainerClick: () -> Unit = {},
    navController: NavHostController,
    controlsAlpha: Float = 1f,
    controlsOffsetY: Dp = 0.dp,
    controlsOffsetYProvider: (() -> Float)? = null,
    showArtwork: Boolean = true,
    showBackground: Boolean = true,
    onArtworkPositioned: ((Rect) -> Unit)? = null,
    onSheetOpenChange: (Boolean) -> Unit = {},
    dismissSheets: Boolean = false,
    onSheetsDismissed: () -> Unit = {}
) {
    val playerState = LocalPlayerState.current
    val mediaController = LocalPlayerController.current.controller
    val isPlaying = playerState?.isPlaying == true
    val repeatMode = playerState?.repeatMode ?: 0
    val context = LocalContext.current.applicationContext
    val mediaId = playerState?.currentMediaItem?.mediaId
    val songIds by remember(context) {
        context.favoriteSongIdsDatastore.data.map { it.songIdsList.toSet() }
    }.collectAsStateWithLifecycle(emptySet())
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openPlayerBottomSheet by rememberSaveable { mutableStateOf(false) }
    val shuffleMode = playerState?.shuffleModeEnabled == true
    val playMode = resolvePlayMode(repeatMode, shuffleMode)
    val artworkColors = LocalArtworkColors.current
    val contentColors = rememberPlayerContentColors(artworkColors)
    val animatedAccentColor by animateColorAsState(
        targetValue = contentColors.accent,
        animationSpec = tween(durationMillis = 350),
        label = "player_accent_color"
    )
    val animatedOnAccentColor by animateColorAsState(
        targetValue = contentColors.onAccent,
        animationSpec = tween(durationMillis = 350),
        label = "player_on_accent_color"
    )

    LaunchedEffect(dismissSheets) {
        if (dismissSheets) {
            openBottomSheet = false
            openPlayerBottomSheet = false
            onSheetsDismissed()
        }
    }

    LaunchedEffect(openBottomSheet, openPlayerBottomSheet) {
        onSheetOpenChange(openBottomSheet || openPlayerBottomSheet)
    }

    LaunchedEffect(mediaId) {
        val songJson = playerState?.currentMediaItem?.mediaMetadata?.extras?.getString("song")
        currentSong = if (songJson != null) {
            withContext(Dispatchers.Default) {
                runCatching { json.decodeFromString<Song>(songJson) }.getOrNull()
            }
        } else {
            null
        }
    }

    val playerContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        alpha = controlsAlpha
                        translationY = (-10.dp * (1f - controlsAlpha)).toPx()
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier.size(width = 56.dp, height = 56.dp)
                ) {
                    Icon(
                        imageVector = ChevronDown,
                        contentDescription = null,
                        tint = contentColors.primaryContent
                    )
                }

                IconButton(
                    onClick = { openPlayerBottomSheet = true },
                    modifier = Modifier.size(width = 56.dp, height = 56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        tint = contentColors.primaryContent
                    )
                }
            }

            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                val availableWidth = maxWidth * 0.88f
                val availableHeight = maxHeight
                val artSize = if (availableHeight > 0.dp && availableHeight < availableWidth) {
                    availableHeight
                } else {
                    availableWidth
                }
                val effectiveArtworkUri = playerState?.currentMediaItem?.mediaMetadata?.artworkUri ?: mediaMetadata.artworkUri
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .aspectRatio(1f)
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                onArtworkPositioned?.invoke(coords.boundsInRoot())
                            }
                        }
                ) {
                    Crossfade(
                        targetState = effectiveArtworkUri,
                        animationSpec = tween(durationMillis = 350),
                        label = "player_artwork_crossfade",
                        modifier = imageModifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = if (showArtwork) 1f else 0f
                                val effectiveElevation = artworkElevationProvider?.invoke() ?: artworkElevation
                                shadowElevation = if (showArtwork) effectiveElevation.toPx() else 0f
                                shape = AdaptiveArtworkShape
                                clip = true
                            }
                            .clickable(enabled = controlsAlpha > 0.1f && showArtwork, onClick = onClick)
                    ) { uri ->
                        val context = LocalContext.current
                        val imageRequest = remember(context, uri) {
                            val key = uri?.toString()
                            ImageRequest.Builder(context)
                                .data(uri)
                                .size(Size(1080, 1080))
                                .apply {
                                    if (key != null) {
                                        memoryCacheKey(key)
                                        diskCacheKey(key)
                                        placeholderMemoryCacheKey(key)
                                    }
                                }
                                .crossfade(false)
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
                    .graphicsLayer {
                        alpha = controlsAlpha
                        translationY = controlsOffsetYProvider?.invoke() ?: controlsOffsetY.toPx()
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                            .clip(JetMeloShapes.medium)
                            .clickable(enabled = controlsAlpha > 0.1f) { openBottomSheet = true }
                            .padding(vertical = 4.dp)
                    ) {
                        mediaMetadata.title?.let {
                            Text(
                                text = it.toString(),
                                maxLines = 1,
                                style = TitleHeroLarge,
                                color = contentColors.primaryContent,
                                modifier = Modifier
                                    .then(titleModifier)
                                    .basicMarquee()
                            )
                        }
                        mediaMetadata.artist?.let {
                            Text(
                                text = it.toString(),
                                maxLines = 1,
                                style = MaterialTheme.typography.titleMedium,
                                color = contentColors.secondaryContent,
                                modifier = Modifier
                                    .then(artistModifier)
                                    .basicMarquee()
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            mediaController?.sendCommandWithFeedback(
                                context,
                                MediaSessionConstants.CommandToggleLike
                            )
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        val isFav = songIds.contains(mediaId?.toLongOrNull())
                        Icon(
                            imageVector = if (isFav) FavoriteFill else Favorite,
                            contentDescription = null,
                            tint = if (isFav) MaterialTheme.colorScheme.error else contentColors.secondaryContent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }



                PlayerProgressSlider(
                    accentColor = animatedAccentColor,
                    inactiveTrackColor = contentColors.trackInactive,
                    timeTextColor = contentColors.secondaryContent,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { mediaController?.seekToPrevious() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = SkipPreviousFill,
                            contentDescription = null,
                            tint = contentColors.primaryContent,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(
                                elevation = 3.dp,
                                shape = JetMeloShapes.full,
                                ambientColor = animatedAccentColor.copy(alpha = 0.22f),
                                spotColor = animatedAccentColor.copy(alpha = 0.24f)
                            )
                            .clip(JetMeloShapes.full)
                            .background(animatedAccentColor)
                            .clickable(enabled = controlsAlpha > 0.1f) {
                                if (!isPlaying) mediaController?.play() else mediaController?.pause()
                            }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) PauseFill else com.rcmiku.music.ui.icons.PlayArrowFill,
                            contentDescription = null,
                            tint = animatedOnAccentColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = { mediaController?.seekToNext() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = SkipNextFill,
                            contentDescription = null,
                            tint = contentColors.primaryContent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PlayModeToggleButton(
                        playMode = playMode,
                        tint = contentColors.secondaryContent,
                        rippleColor = animatedAccentColor,
                        onToggle = { applyPlayMode(mediaController, it) },
                        modifier = Modifier.size(width = 64.dp, height = 56.dp)
                    )

                    IconButton(
                        onClick = onContainerClick,
                        modifier = Modifier.size(width = 64.dp, height = 56.dp)
                    ) {
                        Icon(
                            imageVector = PlaylistInsert,
                            contentDescription = null,
                            tint = contentColors.secondaryContent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        currentSong?.let {
            ArtistBottomSheet(
                currentSong = it,
                onClick = { artist ->
                    navController.navigate(ArtistNav(artistId = artist.id))
                    onBackPressed()
                },
                onDismiss = { openBottomSheet = false },
                openBottomSheet = openBottomSheet,
                onAlbumClick = { album ->
                    navController.navigate(
                        AlbumNav(
                            albumId = album.id,
                            coverImgUrl = album.picUrl,
                            title = album.name
                        )
                    )
                    onBackPressed()
                }
            )
        }

        PlayerMenuBottomSheet(
            currentSong = currentSong,
            onDismiss = { openPlayerBottomSheet = false },
            openBottomSheet = openPlayerBottomSheet
        )
    }

    if (showBackground) {
        ImmersiveBackground(
            modifier = modifier.fillMaxSize(),
            artworkUri = mediaMetadata.artworkUri
        ) {
            playerContent()
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            playerContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistBottomSheet(
    currentSong: Song,
    onClick: (Artist) -> Unit,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit,
    onAlbumClick: (SongAlbum) -> Unit,
) {
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(openBottomSheet) {
        if (openBottomSheet) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    if (openBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState
        ) {
            LazyColumn(
                Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(currentSong.ar) { index, artist ->
                    val shape = getItemShape(
                        prevItem = currentSong.ar.getOrNull(index - 1),
                        nextItem = currentSong.ar.getOrNull(index + 1),
                        corner = 16.dp,
                        subCorner = 4.dp,
                    )
                    Card(
                        shape = shape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    onDismiss()
                                    try {
                                        onClick(artist)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.operation_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Artist,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(text = artist.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(4.dp))
                }
                item {
                    Card(
                        shape = ContinuousRoundedRectangle(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    onDismiss()
                                    try {
                                        onAlbumClick(currentSong.al)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.operation_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Album,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = currentSong.al.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerProgressSlider(
    accentColor: Color,
    modifier: Modifier = Modifier,
    inactiveTrackColor: Color = Color.White.copy(alpha = 0.25f),
    timeTextColor: Color = Color.White.copy(alpha = 0.7f)
) {
    val playerController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val playbackState = playerState?.playbackState
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val initialPosition = remember(currentMediaId) {
        (playerState?.player?.currentPosition ?: 0L).toFloat()
    }
    val animatablePosition = remember(currentMediaId) { Animatable(initialPosition) }

    var duration by remember(currentMediaId) {
        val initialDur = playerState?.player?.duration ?: 0L
        mutableLongStateOf(if (initialDur > 0 && initialDur != C.TIME_UNSET) initialDur else 0L)
    }
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }
    val isDragging by remember {
        derivedStateOf { sliderPosition != null }
    }

    var pendingSeekTarget by remember(currentMediaId) { mutableStateOf<Long?>(null) }
    var seekLockUntilMs by remember(currentMediaId) { mutableLongStateOf(0L) }

    val pollIntervalMs = 100L

    LaunchedEffect(playbackState, isPlaying, currentMediaId) {
        while (isActive) {
            if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                delay(1000)
                continue
            }
            val rawPos = playerState?.player?.currentPosition ?: 0L
            val dur = playerState?.player?.duration ?: 0L
            if (dur > 0 && dur != C.TIME_UNSET) {
                duration = dur
            }

            if (!isDragging) {
                val now = SystemClock.uptimeMillis()
                val target = pendingSeekTarget
                if (target != null) {
                    val diff = kotlin.math.abs(rawPos - target)
                    if (now < seekLockUntilMs && diff > 300L) {
                        delay(pollIntervalMs)
                        continue
                    } else {
                        pendingSeekTarget = null
                    }
                }

                val currentAnimVal = animatablePosition.value
                val diff = kotlin.math.abs(rawPos.toFloat() - currentAnimVal)
                if (diff > 1500f) {
                    animatablePosition.snapTo(rawPos.toFloat())
                } else if (isPlaying && playbackState == STATE_READY) {
                    val maxTarget = if (duration > 0) duration.toFloat() else Float.MAX_VALUE
                    val targetPos = (rawPos + pollIntervalMs).toFloat().coerceAtMost(maxTarget)
                    try {
                        animatablePosition.animateTo(
                            targetValue = targetPos,
                            animationSpec = tween(
                                durationMillis = pollIntervalMs.toInt(),
                                easing = LinearEasing
                            )
                        )
                        continue
                    } catch (e: CancellationException) {
                        if (!isActive) throw e
                    }
                } else {
                    animatablePosition.snapTo(rawPos.toFloat())
                }
            }
            delay(pollIntervalMs)
        }
    }

    Column(modifier = modifier) {
        val interactionSource = remember { MutableInteractionSource() }
        val safeDuration = if (duration > 0 && duration != C.TIME_UNSET) duration else 0L
        val seekEnabled = safeDuration > 0 && playbackState != STATE_IDLE

        val sliderColors = SliderDefaults.colors(
            thumbColor = accentColor,
            activeTrackColor = accentColor,
            inactiveTrackColor = inactiveTrackColor,
            disabledThumbColor = accentColor.copy(alpha = 0.38f),
            disabledActiveTrackColor = accentColor.copy(alpha = 0.38f),
            disabledInactiveTrackColor = inactiveTrackColor.copy(alpha = 0.5f)
        )
        val positionProvider = remember(animatablePosition) {
            { sliderPosition?.toFloat() ?: animatablePosition.value }
        }
        val positionMsProvider = remember(animatablePosition) {
            { sliderPosition ?: animatablePosition.value.toLong() }
        }

        PlayerSliderBar(
            positionProvider = positionProvider,
            safeDuration = safeDuration,
            seekEnabled = seekEnabled,
            isPlaying = isPlaying,
            isDragging = isDragging,
            accentColor = accentColor,
            inactiveTrackColor = inactiveTrackColor,
            sliderColors = sliderColors,
            interactionSource = interactionSource,
            onValueChange = { sliderPosition = it.toLong() },
            onValueChangeFinished = {
                sliderPosition?.let { newPos ->
                    pendingSeekTarget = newPos
                    seekLockUntilMs = SystemClock.uptimeMillis() + 600L
                    coroutineScope.launch {
                        animatablePosition.snapTo(newPos.toFloat())
                    }
                    playerController?.seekTo(newPos)
                }
                sliderPosition = null
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PlayerTimeText(
                positionMsProvider = positionMsProvider,
                color = timeTextColor
            )
            Text(
                text = makeTimeString(safeDuration),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = timeTextColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSliderBar(
    positionProvider: () -> Float,
    safeDuration: Long,
    seekEnabled: Boolean,
    isPlaying: Boolean,
    isDragging: Boolean,
    accentColor: Color,
    inactiveTrackColor: Color,
    sliderColors: SliderColors,
    interactionSource: MutableInteractionSource,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val maxVal = maxOf(1f, safeDuration.toFloat())
    Slider(
        enabled = seekEnabled,
        value = positionProvider().coerceIn(0f, maxVal),
        valueRange = 0f..maxVal,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        colors = sliderColors,
        track = { sliderState ->
            WavySliderTrack(
                sliderState = sliderState,
                activeColor = accentColor,
                inactiveColor = inactiveTrackColor,
                isPlaying = isPlaying,
                isDragging = isDragging,
                enabled = seekEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )
        },
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                colors = sliderColors,
                enabled = seekEnabled,
                thumbSize = DpSize(6.dp, 18.dp)
            )
        }
    )
}

@Composable
private fun PlayerTimeText(
    positionMsProvider: () -> Long,
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.7f)
) {
    val currentSecond by remember {
        derivedStateOf { (positionMsProvider() / 1000L).coerceAtLeast(0L) }
    }
    Text(
        text = makeTimeString(currentSecond * 1000L),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
    )
}
