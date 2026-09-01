package com.rcmiku.music.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.constants.DURATION_ENTER
import com.rcmiku.music.constants.DURATION_EXIT_SHORT
import com.rcmiku.music.constants.ListItemHeight
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.extensions.playMediaAtId
import com.rcmiku.music.extensions.setPlaylist
import com.rcmiku.music.ui.components.SongListItem
import com.rcmiku.music.ui.components.SongMenuBottomSheet
import com.rcmiku.music.ui.design.DailySongsGridSkeleton
import com.rcmiku.music.ui.design.HeroBannerCard
import com.rcmiku.music.ui.design.HeroBannerSkeleton
import com.rcmiku.music.ui.design.PlaylistsRowSkeleton
import com.rcmiku.music.ui.design.SectionHeader
import com.rcmiku.music.ui.design.rememberShimmerBrush
import com.rcmiku.music.ui.navigation.PlaylistNav
import com.rcmiku.music.ui.navigation.Screen
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.utils.formatPlayCount
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.music.viewModel.HomeScreenViewModel
import com.rcmiku.ncmapi.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    bottomContentPadding: Dp = 0.dp
) {
    val recommendSongsState by homeScreenViewModel.recommendSongs.collectAsState()
    val recommendPlaylistState by homeScreenViewModel.recommendPlaylist.collectAsState()
    val personalizedPlaylistState by homeScreenViewModel.personalizedPlaylist.collectAsState()
    val gridState = rememberLazyGridState()
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val ncmCookie by rememberPreference(ncmCookieKey, "")
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectSong by remember { mutableStateOf<Song?>(null) }
    val context = LocalContext.current
    val state = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val songIds by context.favoriteSongIdsDatastore.data.map { it.songIdsList }
        .collectAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (currentHour) {
        in 5..11 -> stringResource(R.string.greeting_morning)
        in 12..17 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            homeScreenViewModel.refresh()
            delay(1000)
            isRefreshing = false
        }
    }

    with(sharedTransitionScope) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search)
                            )
                        }
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            PullToRefreshBox(
                modifier = Modifier
                    .padding(top = padding.calculateTopPadding())
                    .fillMaxSize(),
                state = state,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                indicator = {
                    Indicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = isRefreshing,
                        state = state
                    )
                }
            ) {
                val shimmerBrush = rememberShimmerBrush()
                val dailyData = recommendSongsState?.getOrNull()
                val recommendData = recommendPlaylistState?.getOrNull()
                val personalizedData = personalizedPlaylistState?.getOrNull()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomContentPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Featured Daily Hero Card (Fixed Slot with Crossfade)
                    item {
                        AnimatedContent(
                            targetState = dailyData,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(DURATION_ENTER)) togetherWith fadeOut(animationSpec = tween(DURATION_EXIT_SHORT))
                            },
                            label = "hero_banner_crossfade"
                        ) { data ->
                            if (data != null) {
                                val songs = data.data.dailySongs
                                val firstSong = songs.firstOrNull()
                                if (firstSong != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                    ) {
                                        HeroBannerCard(
                                            imageUrl = firstSong.al.picUrl,
                                            badgeText = stringResource(R.string.featured_today),
                                            title = stringResource(R.string.recommend_songs),
                                            subtitle = stringResource(R.string.daily_recommend_subtitle, songs.size),
                                            aspectRatio = 1.8f,
                                            onClick = {
                                                mediaController?.setPlaylist(songs)
                                                mediaController?.playMediaAtId(firstSong.id)
                                            },
                                            onPlayClick = {
                                                mediaController?.setPlaylist(songs)
                                                mediaController?.playMediaAtId(firstSong.id)
                                            }
                                        )
                                    }
                                } else {
                                    HeroBannerSkeleton(brush = shimmerBrush)
                                }
                            } else {
                                HeroBannerSkeleton(brush = shimmerBrush)
                            }
                        }
                    }

                    // 2. Recommended Playlists Horizontal Carousel (Fixed Slot with Crossfade)
                    item {
                        SectionHeader(
                            title = stringResource(R.string.personalized_playlist)
                        )

                        val playlistTarget = recommendData?.recommend ?: personalizedData?.result

                        AnimatedContent(
                            targetState = playlistTarget,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(DURATION_ENTER)) togetherWith fadeOut(animationSpec = tween(DURATION_EXIT_SHORT))
                            },
                            label = "playlists_crossfade"
                        ) { playlists ->
                            if (playlists != null) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(playlists, key = { it.id }) { playlist ->
                                        Column(
                                            modifier = Modifier
                                                .width(160.dp)
                                                .clip(JetMeloShapes.medium)
                                                .clickable {
                                                    navController.navigate(
                                                        PlaylistNav(
                                                            playlistId = playlist.id,
                                                            limit = playlist.trackCount
                                                        )
                                                    )
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(160.dp)
                                                    .clip(JetMeloShapes.large)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                            ) {
                                                AsyncImage(
                                                    model = playlist.cover,
                                                    contentDescription = playlist.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(JetMeloShapes.large)
                                                        .sharedElement(
                                                            sharedTransitionScope.rememberSharedContentState(
                                                                key = "cover_${playlist.id}"
                                                            ),
                                                            animatedVisibilityScope = animatedContentScope,
                                                            clipInOverlayDuringTransition = OverlayClip(JetMeloShapes.large)
                                                        )
                                                )
                                            }

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            val countText = playlist.playCount?.let { formatPlayCount(it) }
                                                ?: playlist.trackCount?.let { stringResource(R.string.song_size, it) }
                                                ?: ""
                                            Text(
                                                modifier = Modifier.padding(bottom = 8.dp),
                                                text = countText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            } else {
                                PlaylistsRowSkeleton(brush = shimmerBrush)
                            }
                        }
                    }

                    // 3. Daily Songs 4-Row Grid Section (Fixed Slot with Crossfade)
                    item {
                        SectionHeader(
                            title = stringResource(R.string.recommend_songs)
                        )

                        AnimatedContent(
                            targetState = dailyData,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(DURATION_ENTER)) togetherWith fadeOut(animationSpec = tween(DURATION_EXIT_SHORT))
                            },
                            label = "daily_songs_crossfade"
                        ) { data ->
                            if (data != null) {
                                val songs = data.data.dailySongs
                                LazyHorizontalGrid(
                                    rows = GridCells.Fixed(4),
                                    state = gridState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ListItemHeight * 4),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    flingBehavior = rememberSnapFlingBehavior(
                                        gridState,
                                        snapPosition = SnapPosition.Start
                                    )
                                ) {
                                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                                        SongListItem(
                                            isPlaying = isPlaying,
                                            isActive = currentMediaId == song.id,
                                            showLikedIcon = song.id in songIds,
                                            song = song,
                                            songIndex = index + 1,
                                            modifier = Modifier
                                                .clip(JetMeloShapes.small)
                                                .width(340.dp)
                                                .clickable {
                                                    mediaController?.setPlaylist(songs)
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
                            } else {
                                DailySongsGridSkeleton(brush = shimmerBrush)
                            }
                        }
                    }
                }
            }
        }
    }

    SongMenuBottomSheet(
        navController = navController,
        song = selectSong,
        onDismiss = { openBottomSheet = false },
        openBottomSheet = openBottomSheet
    )
}