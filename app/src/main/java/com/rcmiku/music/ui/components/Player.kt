package com.rcmiku.music.ui.components

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
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
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.ui.theme.TitleHeroLarge
import com.rcmiku.music.utils.getItemShape
import com.rcmiku.music.utils.makeTimeString
import com.rcmiku.ncmapi.model.Artist
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.SongAlbum
import com.rcmiku.ncmapi.utils.json
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Player(
    mediaMetadata: MediaMetadata,
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onClick: () -> Unit = {},
    onContainerClick: () -> Unit = {},
    onPositionUpdate: (Long) -> Unit,
    navController: NavHostController
) {
    BackHandler {
        onBackPressed()
    }

    var sliderPosition by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

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

    LaunchedEffect(mediaId) {
        val songJson = playerState?.currentMediaItem?.mediaMetadata?.extras?.getString("song")
        currentSong = if (songJson != null) {
            runCatching { json.decodeFromString<Song>(songJson) }.getOrNull()
        } else {
            null
        }
    }

    ImmersiveBackground(
        modifier = modifier.fillMaxSize(),
        artworkUri = mediaMetadata.artworkUri
    ) {
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
                    .height(56.dp),
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
                        text = "正在播放",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = mediaMetadata.albumTitle?.toString() ?: "JetMelo",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
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
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(1f)
                        .shadow(elevation = 16.dp, shape = JetMeloShapes.large)
                        .clip(JetMeloShapes.large)
                        .clickable(onClick = onClick)
                ) {
                    AsyncImage(
                        model = mediaMetadata.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = imageModifier
                            .fillMaxSize()
                            .clip(JetMeloShapes.large)
                    )
                }
            }

            // Metadata, Progress & Controls Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
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
                                    .basicMarquee()
                                    .clickable { openBottomSheet = true }
                            )
                        }
                        mediaMetadata.artist?.let {
                            Text(
                                text = it.toString(),
                                maxLines = 1,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .basicMarquee()
                                    .clickable { openBottomSheet = true }
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            mediaController?.sendCustomCommand(
                                MediaSessionConstants.CommandToggleLike,
                                Bundle.EMPTY
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



                // Progress Slider Section
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val currentPos = sliderPosition ?: position
                    val safeDuration = if (duration > 0 && duration != C.TIME_UNSET) duration else 0L

                    Slider(
                        value = currentPos.toFloat().coerceIn(0f, maxOf(1f, safeDuration.toFloat())),
                        valueRange = 0f..maxOf(1f, safeDuration.toFloat()),
                        onValueChange = { sliderPosition = it.toLong() },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                mediaController?.seekTo(it)
                                onPositionUpdate(it)
                            }
                            sliderPosition = null
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = artworkColors.accentColor,
                            activeTrackColor = artworkColors.accentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                thumbTrackGapSize = 2.dp,
                                modifier = Modifier.height(4.dp)
                            )
                        },
                        thumb = {
                            SliderDefaults.Thumb(
                                interactionSource = interactionSource,
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

                // Playback Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            mediaController?.sendCustomCommand(
                                MediaSessionConstants.CommandToggleShuffle,
                                Bundle.EMPTY
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
                            .clickable {
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
                                    onClick(artist)
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
                                    onAlbumClick(currentSong.al)
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
