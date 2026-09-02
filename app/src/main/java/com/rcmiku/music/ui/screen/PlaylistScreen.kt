package com.rcmiku.music.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.constants.userIdKey
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.extensions.playMediaAt
import com.rcmiku.music.extensions.playMediaAtId
import com.rcmiku.music.extensions.setPlaylist
import com.rcmiku.music.ui.components.SongListItem
import com.rcmiku.music.ui.components.SongMenuBottomSheet
import com.rcmiku.music.ui.design.rememberArtworkColors
import com.rcmiku.music.ui.icons.LibraryAdd
import com.rcmiku.music.ui.icons.LibraryAddCheck
import com.rcmiku.music.ui.icons.PlayArrowFill
import com.rcmiku.music.ui.icons.Remove
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.utils.formatPlayCount
import com.rcmiku.music.utils.formatTimestamp
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.music.viewModel.PlaylistScreenViewModel
import com.rcmiku.ncmapi.model.Song
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlaylistScreen(
    navController: NavHostController,
    playlistScreenViewModel: PlaylistScreenViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    bottomContentPadding: Dp = 0.dp
) {
    val playlistDetailState by playlistScreenViewModel.playlistDetail.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val showPlaylistTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val playlistTitle = playlistDetailState?.playlist?.name.orEmpty()
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()
    val playlistInfoState by playlistScreenViewModel.playlistInfo.collectAsStateWithLifecycle()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectSong by remember { mutableStateOf<Song?>(null) }
    val context = LocalContext.current
    val songIds by remember(context) {
        context.favoriteSongIdsDatastore.data.map { it.songIdsList.toSet() }
    }.collectAsStateWithLifecycle(emptySet())
    val currentUserId by rememberPreference(userIdKey, 0L)

    with(sharedTransitionScope) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (showPlaylistTitle) playlistTitle else stringResource(R.string.playlist),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (showPlaylistTitle)
                            MaterialTheme.colorScheme.surfaceContainer
                        else
                            Color.Transparent
                    )
                )
            }
        ) { padding ->
            playlistDetailState?.let { detail ->
                val isOwner = detail.playlist.userId == currentUserId && currentUserId != 0L
                var tracks by remember(detail.playlist.tracks) { mutableStateOf(detail.playlist.tracks) }

                val pageArtworkColors = rememberArtworkColors(
                    artworkUri = detail.playlist.coverImgUrl,
                    songId = detail.playlist.id.toString()
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomContentPadding),
                    state = listState
                ) {
                    // 1. Solaris Immersive Hero Header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            pageArtworkColors.dominantColor.copy(alpha = 0.45f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                                .padding(top = padding.calculateTopPadding())
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Cover Artwork
                                Box(
                                    modifier = Modifier
                                        .size(220.dp)
                                        .shadow(elevation = 12.dp, shape = JetMeloShapes.medium)
                                        .clip(JetMeloShapes.medium)
                                ) {
                                    AsyncImage(
                                        model = detail.playlist.coverImgUrl,
                                        contentDescription = detail.playlist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .sharedElement(
                                                sharedTransitionScope.rememberSharedContentState(
                                                    key = "cover_${detail.playlist.id}"
                                                ),
                                                animatedVisibilityScope = animatedContentScope
                                            )
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                // Title
                                Text(
                                    text = detail.playlist.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(6.dp))

                                // Meta (Play count & Update time)
                                Text(
                                    text = stringResource(
                                        R.string.total_play_count,
                                        formatPlayCount(detail.playlist.playCount)
                                    ) + " • " + formatTimestamp(detail.playlist.trackUpdateTime),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                val description = detail.playlist.description
                                if (!description.isNullOrEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                // Action Buttons Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play All Button (CTA bound to page Artwork Accent Color)
                                    Button(
                                        onClick = {
                                            mediaController?.setPlaylist(tracks)
                                            mediaController?.playMediaAt(0)
                                        },
                                        shape = JetMeloShapes.full,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = pageArtworkColors.accentColor,
                                            contentColor = pageArtworkColors.onAccentColor
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = PlayArrowFill,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.play_all),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Subscribe / Collect Button
                                    FilledTonalIconButton(
                                        onClick = {
                                            playlistInfoState?.subscribed?.let {
                                                playlistScreenViewModel.playlistSub(isSub = it)
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (playlistInfoState?.subscribed == true)
                                                LibraryAddCheck
                                            else
                                                LibraryAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Track List Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.song_size, tracks.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 3. Track Items (Swipe to dismiss enabled only when user is playlist creator)
                    itemsIndexed(tracks, key = { _, song -> song.id }) { index, song ->
                        if (isOwner) {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                        val currentIndex = tracks.indexOfFirst { it.id == song.id }
                                        if (currentIndex != -1) {
                                            val removedSong = song
                                            val removedIndex = currentIndex
                                            tracks = tracks.toMutableList().apply { removeAt(currentIndex) }
                                            playlistScreenViewModel.deleteTrack(song.id) { success ->
                                                if (!success) {
                                                    tracks = tracks.toMutableList().apply {
                                                        add(minOf(removedIndex, size), removedSong)
                                                    }
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.delete_track_failed),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .clip(JetMeloShapes.small)
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Remove,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                    }
                                }
                            ) {
                                SongListItem(
                                    song = song,
                                    isPlaying = isPlaying,
                                    showLikedIcon = song.id in songIds,
                                    isActive = currentMediaId == song.id,
                                    songIndex = index + 1,
                                    modifier = Modifier
                                        .clip(JetMeloShapes.small)
                                        .clickable {
                                            mediaController?.setPlaylist(tracks)
                                            mediaController?.playMediaAtId(song.id)
                                        },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            selectSong = song
                                            openBottomSheet = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = stringResource(R.string.more)
                                            )
                                        }
                                    }
                                )
                            }
                        } else {
                            SongListItem(
                                song = song,
                                isPlaying = isPlaying,
                                showLikedIcon = song.id in songIds,
                                isActive = currentMediaId == song.id,
                                songIndex = index + 1,
                                modifier = Modifier
                                    .clip(JetMeloShapes.small)
                                    .clickable {
                                        mediaController?.setPlaylist(tracks)
                                        mediaController?.playMediaAtId(song.id)
                                    },
                                trailingContent = {
                                    IconButton(onClick = {
                                        selectSong = song
                                        openBottomSheet = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.more)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

        // 旋转/进程恢复兜底：openBottomSheet 为 saveable 而 selectSong 仅 remember（4.9 同款）
    LaunchedEffect(openBottomSheet, selectSong) {
        if (openBottomSheet && selectSong == null) openBottomSheet = false
    }

    SongMenuBottomSheet(
        navController = navController,
        song = selectSong,
        onDismiss = { openBottomSheet = false },
        openBottomSheet = openBottomSheet
    )
}