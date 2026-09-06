package com.rcmiku.music.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
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
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.extensions.playMediaAt
import com.rcmiku.music.extensions.playMediaAtId
import com.rcmiku.music.extensions.setPlaylist
import com.rcmiku.music.ui.components.SongListItem
import com.rcmiku.music.ui.components.SongMenuBottomSheet
import com.rcmiku.music.ui.components.StickyPlayAllBar
import com.rcmiku.music.ui.design.rememberArtworkColors
import com.rcmiku.music.ui.icons.LibraryAdd
import com.rcmiku.music.ui.icons.LibraryAddCheck
import com.rcmiku.music.ui.navigation.ArtistNav
import com.rcmiku.music.ui.navigation.JetMeloBoundsTransform
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.utils.formatTimestamp
import com.rcmiku.music.viewModel.AlbumScreenViewModel
import kotlinx.coroutines.flow.map


@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumScreen(
    navController: NavHostController,
    albumScreenViewModel: AlbumScreenViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    bottomContentPadding: Dp = 0.dp
) {
    val albumDetailState by albumScreenViewModel.albumDetail.collectAsStateWithLifecycle()
    val isLoading by albumScreenViewModel.isLoading.collectAsStateWithLifecycle()
    val loadError by albumScreenViewModel.loadError.collectAsStateWithLifecycle()
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
    val albumInfoState by albumScreenViewModel.albumInfo.collectAsStateWithLifecycle()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSongId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectSong = albumDetailState?.getOrNull()?.songs?.firstOrNull { it.id == selectedSongId }
    val context = LocalContext.current
    val songIds by remember(context) {
        context.favoriteSongIdsDatastore.data.map { it.songIdsList.toSet() }
    }.collectAsStateWithLifecycle(emptySet())

    LaunchedEffect(Unit) {
        albumScreenViewModel.actionError.collect {
            Toast.makeText(context, context.getString(R.string.operation_failed), Toast.LENGTH_SHORT).show()
        }
    }

    val pageArtworkColors = rememberArtworkColors(
        artworkUri = albumDetailState?.getOrNull()?.album?.picUrl,
        songId = albumDetailState?.getOrNull()?.album?.id?.toString()
    )
    val baseColor = MaterialTheme.colorScheme.background
    val dominantTopColor = pageArtworkColors.dominantColor.copy(alpha = 0.65f)

    with(sharedTransitionScope) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(44.dp)
                ) {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }

                    val album = albumDetailState?.getOrNull()?.album
                    if (album != null) {
                        val density = LocalDensity.current
                        val targetDistancePx = remember(density) { with(density) { 296.dp.toPx() } }

                        val currentScroll by remember {
                            derivedStateOf {
                                if (listState.firstVisibleItemIndex == 0) {
                                    listState.firstVisibleItemScrollOffset.toFloat()
                                } else {
                                    targetDistancePx
                                }
                            }
                        }

                        val travelFraction by remember {
                            derivedStateOf {
                                (currentScroll / targetDistancePx).coerceIn(0f, 1f)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 52.dp)
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .graphicsLayer {
                                        val s = 1f + (1f - travelFraction) * 0.571f
                                        translationY = (targetDistancePx - currentScroll).coerceAtLeast(0f)
                                        scaleX = s
                                        scaleY = s
                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    }
                            )
                        }
                    }
                }
            },
        ) { padding ->
            val result = albumDetailState
            val detail = result?.getOrNull()

            when {
                result == null && isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding()),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                result == null || detail == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding())
                            .clickable(role = Role.Button) {
                                albumScreenViewModel.retry()
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(baseColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                                .graphicsLayer {
                                    alpha = (1f - (collapseFraction / 0.70f)).coerceIn(0f, 1f)
                                }
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            dominantTopColor,
                                            baseColor
                                        )
                                    )
                                )
                        )

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
                                        .onSizeChanged { size ->
                                            if (size.height > 0) {
                                                headerHeightPx = size.height.toFloat()
                                            }
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
                                                .graphicsLayer {
                                                    val offset = if (listState.firstVisibleItemIndex == 0) {
                                                        listState.firstVisibleItemScrollOffset.toFloat()
                                                    } else {
                                                        0f
                                                    }
                                                    translationY = offset * 0.85f
                                                    val fraction = collapseFraction
                                                    scaleX = (1f - fraction * 0.25f).coerceIn(0.75f, 1f)
                                                    scaleY = scaleX
                                                    alpha = (1f - (fraction / 0.70f)).coerceIn(0f, 1f)
                                                }
                                                .shadow(elevation = 12.dp, shape = JetMeloShapes.medium)
                                                .clip(JetMeloShapes.medium)
                                        ) {
                                            AsyncImage(
                                                model = detail.album.picUrl,
                                                contentDescription = detail.album.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .sharedElement(
                                                        sharedTransitionScope.rememberSharedContentState(
                                                            key = "cover_${detail.album.id}"
                                                        ),
                                                        animatedVisibilityScope = animatedContentScope,
                                                        boundsTransform = JetMeloBoundsTransform,
                                                        placeHolderSize = SharedTransitionScope.PlaceHolderSize.contentSize,
                                                        clipInOverlayDuringTransition = OverlayClip(JetMeloShapes.medium)
                                                    )
                                            )
                                        }

                                        Spacer(Modifier.height(16.dp))

                                        // Title Container with Permanent Layout Spacer to prevent jumping
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = detail.album.name,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    lineHeight = 28.sp
                                                ),
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.alpha(0f)
                                            )
                                        }

                                        Spacer(Modifier.height(4.dp))

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.graphicsLayer {
                                                alpha = (1f - (collapseFraction / 0.70f)).coerceIn(0f, 1f)
                                            }
                                        ) {
                                            // Artist Subtitle
                                            Text(
                                                text = detail.album.artist.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .clip(JetMeloShapes.small)
                                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                    .clickable(role = Role.Button) {
                                                        navController.navigate(ArtistNav(artistId = detail.album.artist.id))
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            )

                                            Spacer(Modifier.height(4.dp))

                                            // Release Date
                                            Text(
                                                text = formatTimestamp(detail.album.publishTime),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. 粘性「播放全部」操作条 (Sticky Play All Bar)
                            stickyHeader(key = "sticky_play_all") {
                                StickyPlayAllBar(
                                    trackCount = detail.songs.size,
                                    onPlayAllClick = {
                                        if (detail.songs.isNotEmpty()) {
                                            mediaController?.setPlaylist(detail.songs)
                                            mediaController?.playMediaAt(0)
                                        }
                                    },
                                    isSticky = isSticky,
                                    collapseFraction = collapseFraction,
                                    accentColor = pageArtworkColors.accentColor,
                                    onAccentColor = pageArtworkColors.onAccentColor,
                                    isLoading = isLoading,
                                    trailingContent = {
                                        FilledTonalIconButton(
                                            onClick = {
                                                albumInfoState?.isSub?.let { isSub ->
                                                    albumScreenViewModel.albumSub(isSub = isSub)
                                                }
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (albumInfoState?.isSub == true)
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

                            // 3. Track Items (Read-only list with composite key)
                            itemsIndexed(
                                detail.songs,
                                key = { index, song -> "${song.id}_$index" }
                            ) { index, song ->
                                SongListItem(
                                    song = song,
                                    isPlaying = isPlaying,
                                    showLikedIcon = song.id in songIds,
                                    isActive = currentMediaId == song.id,
                                    albumIndex = index + 1,
                                    modifier = Modifier
                                        .clip(JetMeloShapes.small)
                                        .clickable {
                                            mediaController?.setPlaylist(detail.songs)
                                            mediaController?.playMediaAtId(song.id)
                                        },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            selectedSongId = song.id
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