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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import com.rcmiku.music.ui.components.StickyPlayAllBar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.constants.userIdKey
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.extensions.playMediaAt
import com.rcmiku.music.extensions.playMediaAtId
import com.rcmiku.music.extensions.setPlaylist
import com.rcmiku.music.ui.components.SongMenuBottomSheet
import com.rcmiku.music.ui.design.rememberArtworkColors
import com.rcmiku.music.ui.icons.AudioLines
import com.rcmiku.music.ui.icons.FavoriteFill
import com.rcmiku.music.ui.icons.LibraryAdd
import com.rcmiku.music.ui.icons.LibraryAddCheck
import com.rcmiku.music.ui.icons.PlayArrowFill
import com.rcmiku.music.ui.theme.AdaptiveArtworkShape
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.utils.formatPlayCount
import com.rcmiku.music.utils.formatTimestamp
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.music.viewModel.PlaylistScreenViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
    val tracks by playlistScreenViewModel.tracks.collectAsStateWithLifecycle()
    val isLoading by playlistScreenViewModel.isLoading.collectAsStateWithLifecycle()
    val loadError by playlistScreenViewModel.loadError.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var headerHeightPx by remember { mutableFloatStateOf(1f) }
    val collapseFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else if (headerHeightPx > 0f) {
                (listState.firstVisibleItemScrollOffset.toFloat() / headerHeightPx).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }
    val isSticky by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || collapseFraction >= 0.99f
        }
    }
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()
    val playlistInfoState by playlistScreenViewModel.playlistInfo.collectAsStateWithLifecycle()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSongId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectSong = tracks.firstOrNull { it.id == selectedSongId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val songIds by remember(context) {
        context.favoriteSongIdsDatastore.data.map { it.songIdsList.toSet() }
    }.collectAsStateWithLifecycle(emptySet())
    val currentUserId by rememberPreference(userIdKey, 0L)

    LaunchedEffect(Unit) {
        playlistScreenViewModel.actionError.collect {
            Toast.makeText(context, context.getString(R.string.operation_failed), Toast.LENGTH_SHORT).show()
        }
    }

    val pageArtworkColors = rememberArtworkColors(
        artworkUri = playlistDetailState?.playlist?.coverImgUrl,
        songId = playlistDetailState?.playlist?.id?.toString()
    )

    with(sharedTransitionScope) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                val baseColor = MaterialTheme.colorScheme.background
                val dominantTopColor = pageArtworkColors.dominantColor.copy(alpha = 0.65f)
                val topBarBgColor = if (isSticky) {
                    baseColor
                } else {
                    androidx.compose.ui.graphics.lerp(dominantTopColor, baseColor, collapseFraction.coerceIn(0f, 1f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(topBarBgColor)
                        .statusBarsPadding()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            val detail = playlistDetailState
            when {
                detail == null && isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding()),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                detail == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding())
                            .clickable(role = Role.Button) {
                                playlistScreenViewModel.retry()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.operation_failed),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val isOwner = detail.playlist.userId == currentUserId && currentUserId != 0L

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding()),
                        contentPadding = PaddingValues(
                            bottom = bottomContentPadding
                        ),
                        state = listState
                    ) {
                        // 1. Solaris Immersive Hero Header (随滚顶出 + 渐隐)
                        item(key = "hero_header") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                pageArtworkColors.dominantColor.copy(alpha = 0.65f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                                    .onSizeChanged { size ->
                                        if (size.height > 0) {
                                            headerHeightPx = size.height.toFloat()
                                        }
                                    }
                                    .graphicsLayer {
                                        alpha = (1f - collapseFraction).coerceIn(0f, 1f)
                                    }
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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(
                                                    R.string.total_play_count,
                                                    formatPlayCount(detail.playlist.playCount)
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = " • ",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatTimestamp(detail.playlist.trackUpdateTime),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

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
                                    }
                                }
                            }

                            // 2. 粘性「播放全部」操作条 (Sticky Play All Bar)
                            stickyHeader(key = "sticky_play_all") {
                                StickyPlayAllBar(
                                    trackCount = tracks.size,
                                    onPlayAllClick = {
                                        if (tracks.isNotEmpty()) {
                                            mediaController?.setPlaylist(tracks)
                                            mediaController?.playMediaAt(0)
                                        }
                                    },
                                    isSticky = isSticky,
                                    accentColor = pageArtworkColors.accentColor,
                                    onAccentColor = pageArtworkColors.onAccentColor,
                                    isLoading = isLoading,
                                    trailingContent = {
                                        FilledTonalIconButton(
                                            onClick = {
                                                playlistInfoState?.subscribed?.let {
                                                    playlistScreenViewModel.playlistSub(isSub = it)
                                                }
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (playlistInfoState?.subscribed == true)
                                                    LibraryAddCheck
                                                else
                                                    LibraryAdd,
                                                contentDescription = stringResource(R.string.favorite),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                )
                            }

                        // 3. Track Items (Clean fluid list with dedicated index & active indicator)
                        itemsIndexed(
                            tracks,
                            key = { index, song -> "${song.id}_$index" }
                        ) { index, song ->
                            val isActive = currentMediaId == song.id
                            val isItemPlaying = isActive && isPlaying

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .clip(JetMeloShapes.medium)
                                    .background(
                                        if (isActive)
                                            pageArtworkColors.accentColor.copy(alpha = 0.16f)
                                        else
                                            Color.Transparent
                                    )
                                    .clickable {
                                        mediaController?.setPlaylist(tracks)
                                        mediaController?.playMediaAtId(song.id)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Dedicated Track Number or Playing Waveform
                                    Box(
                                        modifier = Modifier.width(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isItemPlaying) {
                                            Icon(
                                                imageVector = AudioLines,
                                                contentDescription = null,
                                                tint = pageArtworkColors.accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = (index + 1).toString(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isActive)
                                                    pageArtworkColors.accentColor
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Artwork Thumbnail
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(song.al.picUrl)
                                            .size(Size(176, 176))
                                            .memoryCacheKey(song.al.picUrl.ifEmpty { song.id.toString() })
                                            .diskCacheKey(song.al.picUrl.ifEmpty { song.id.toString() })
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = song.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(AdaptiveArtworkShape)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Title & Artist
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = song.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isActive)
                                                pageArtworkColors.accentColor
                                            else
                                                MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (song.id in songIds) {
                                                Icon(
                                                    imageVector = FavoriteFill,
                                                    contentDescription = stringResource(R.string.like),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .padding(end = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = song.ar.joinToString("/") { it.name },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isActive)
                                                    pageArtworkColors.accentColor.copy(alpha = 0.8f)
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // More Options Action (⋮)
                                    IconButton(
                                        onClick = {
                                            selectedSongId = song.id
                                            openBottomSheet = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.more),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
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