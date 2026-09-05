package com.rcmiku.music.ui.components

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import com.rcmiku.music.ui.icons.Album
import com.rcmiku.music.ui.icons.Artist
import com.rcmiku.music.ui.icons.AudioLines
import com.rcmiku.music.ui.icons.ChevronDown
import com.rcmiku.music.ui.icons.Favorite
import com.rcmiku.music.ui.icons.FavoriteFill
import com.rcmiku.music.ui.icons.PauseFill
import com.rcmiku.music.ui.icons.Repeat
import com.rcmiku.music.ui.icons.RepeatOne
import com.rcmiku.music.ui.icons.Shuffle
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
    onBackPressed: () -> Unit = {},
    onClick: () -> Unit = {},
    onContainerClick: () -> Unit = {},
    navController: NavHostController,
    controlsAlpha: Float = 1f,
    controlsOffsetY: Dp = 0.dp,
    showArtwork: Boolean = true,
    showBackground: Boolean = true,
    onArtworkPositioned: ((Rect) -> Unit)? = null
) {
    val playerState = LocalPlayerState.current
    val mediaController = LocalPlayerController.current.controller
    val isPlaying = playerState?.isPlaying == true
    val repeatMode = playerState?.repeatMode ?: 0
    val repeatIcon = when (repeatMode) {
        0 -> Repeat
        1 -> RepeatOne
        else -> Repeat
    }
    val context = LocalContext.current.applicationContext
    val mediaId = playerState?.currentMediaItem?.mediaId
    val songIds by remember(context) {
        context.favoriteSongIdsDatastore.data.map { it.songIdsList.toSet() }
    }.collectAsStateWithLifecycle(emptySet())
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openPlayerBottomSheet by rememberSaveable { mutableStateOf(false) }
    val shuffleMode = playerState?.shuffleModeEnabled == true
    val artworkColors = LocalArtworkColors.current

    BackHandler(enabled = !openBottomSheet && !openPlayerBottomSheet) {
        onBackPressed()
    }

    LaunchedEffect(mediaId) {
        val songJson = playerState?.currentMediaItem?.mediaMetadata?.extras?.getString("song")
        currentSong = if (songJson != null) {
            runCatching { json.decodeFromString<Song>(songJson) }.getOrNull()
        } else {
            null
        }
    }

    val playerContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        alpha = controlsAlpha
                        translationY = controlsOffsetY.toPx()
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = ChevronDown,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = mediaMetadata.albumTitle?.toString() ?: "JetMelo",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { openPlayerBottomSheet = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            // Cover Artwork Area
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 12.dp)
            ) {
                val availableWidth = maxWidth * 0.88f
                val availableHeight = maxHeight
                val artSize = if (availableHeight > 0.dp && availableHeight < availableWidth) {
                    availableHeight
                } else {
                    availableWidth
                }
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
                    if (showArtwork) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(mediaMetadata.artworkUri)
                                .size(Size(1080, 1080))
                                .memoryCacheKey(mediaMetadata.artworkUri?.toString())
                                .diskCacheKey(mediaMetadata.artworkUri?.toString())
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = imageModifier
                                .fillMaxSize()
                                .shadow(elevation = 16.dp, shape = AdaptiveArtworkShape)
                                .clip(AdaptiveArtworkShape)
                                .clickable(enabled = controlsAlpha > 0.1f, onClick = onClick)
                        )
                    }
                }
            }

            // Metadata, Progress & Controls Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .graphicsLayer {
                        alpha = controlsAlpha
                        translationY = controlsOffsetY.toPx()
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title, Artist, and Favorite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        mediaMetadata.title?.let {
                            Text(
                                text = it.toString(),
                                maxLines = 1,
                                style = TitleHeroLarge,
                                color = Color.White,
                                modifier = Modifier
                                    .then(titleModifier)
                                    .basicMarquee()
                                    .clickable(enabled = controlsAlpha > 0.1f) { openBottomSheet = true }
                            )
                        }
                        mediaMetadata.artist?.let {
                            Text(
                                text = it.toString(),
                                maxLines = 1,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .then(artistModifier)
                                    .basicMarquee()
                                    .clickable(enabled = controlsAlpha > 0.1f) { openBottomSheet = true }
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
                            tint = if (isFav) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }



                PlayerProgressSlider(
                    accentColor = artworkColors.accentColor,
                    modifier = Modifier.fillMaxWidth()
                )

                // Playback Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            mediaController?.sendCommandWithFeedback(
                                context,
                                MediaSessionConstants.CommandToggleShuffle
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Shuffle,
                            contentDescription = null,
                            tint = if (shuffleMode) artworkColors.accentColor else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = { mediaController?.seekToPrevious() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = SkipPreviousFill,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // 72dp Circular Play/Pause Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(8.dp, shape = JetMeloShapes.full)
                            .clip(JetMeloShapes.full)
                            .background(artworkColors.accentColor)
                            .clickable(enabled = controlsAlpha > 0.1f) {
                                if (!isPlaying) mediaController?.play() else mediaController?.pause()
                            }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) PauseFill else com.rcmiku.music.ui.icons.PlayArrowFill,
                            contentDescription = null,
                            tint = artworkColors.onAccentColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = { mediaController?.seekToNext() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = SkipNextFill,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newMode = when (repeatMode) {
                                0 -> 2
                                1 -> 0
                                2 -> 1
                                else -> 0
                            }
                            mediaController?.repeatMode = newMode
                        }
                    ) {
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = null,
                            tint = if (repeatMode != 0) artworkColors.accentColor else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom Actions Row (Lyric, Queue)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = AudioLines,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = onContainerClick) {
                        Icon(
                            imageVector = PlaylistInsert,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
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
                    navController.navigate(AlbumNav(albumId = album.id))
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
                                    } catch (e: CancellationException) {
                                        throw e
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
                        shape = RoundedCornerShape(16.dp),
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
                                    } catch (e: CancellationException) {
                                        throw e
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
    modifier: Modifier = Modifier
) {
    val playerController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val playbackState = playerState?.playbackState
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId
    val lifecycleOwner = LocalLifecycleOwner.current

    var position by rememberSaveable(playerState) {
        mutableLongStateOf(playerState?.player?.currentPosition ?: 0L)
    }
    var duration by rememberSaveable(playerState) {
        mutableLongStateOf(playerState?.player?.duration ?: 0L)
    }
    var sliderPosition by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    val isDragging = sliderPosition != null

    LaunchedEffect(playbackState, isPlaying) {
        if (playbackState == STATE_READY && isPlaying) {
            while (isActive) {
                if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    delay(1000)
                    continue
                }
                if (!isDragging) {
                    position = playerState?.player?.currentPosition ?: 0L
                    val dur = playerState?.player?.duration ?: 0L
                    duration = if (dur > 0) dur else 0L
                }
                delay(100)
            }
        } else if (playbackState == STATE_READY) {
            if (!isDragging) {
                position = playerState?.player?.currentPosition ?: 0L
                val dur = playerState?.player?.duration ?: 0L
                duration = if (dur > 0) dur else 0L
            }
        }
    }

    LaunchedEffect(currentMediaId) {
        position = 0L
        duration = 0L
    }

    Column(modifier = modifier) {
        val interactionSource = remember { MutableInteractionSource() }
        val currentPos = sliderPosition ?: position
        val safeDuration = if (duration > 0 && duration != C.TIME_UNSET) duration else 0L
        val seekEnabled = safeDuration > 0 && playbackState != STATE_IDLE

        Slider(
            enabled = seekEnabled,
            value = currentPos.toFloat().coerceIn(0f, maxOf(1f, safeDuration.toFloat())),
            valueRange = 0f..maxOf(1f, safeDuration.toFloat()),
            onValueChange = { sliderPosition = it.toLong() },
            onValueChangeFinished = {
                sliderPosition?.let { newPos ->
                    position = newPos
                    playerController?.seekTo(newPos)
                }
                sliderPosition = null
            },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                disabledThumbColor = accentColor.copy(alpha = 0.38f),
                disabledActiveTrackColor = accentColor.copy(alpha = 0.38f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.12f)
            ),
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    enabled = seekEnabled,
                    thumbTrackGapSize = 2.dp,
                    modifier = Modifier.height(4.dp)
                )
            },
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    enabled = seekEnabled,
                    thumbSize = DpSize(6.dp, 18.dp)
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = makeTimeString(currentPos),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = makeTimeString(safeDuration),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
