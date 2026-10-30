package com.rcmiku.music.ui.screen

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.rcmiku.music.ui.design.PaletteExtractor
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.rcmiku.music.R
import com.rcmiku.music.ui.design.HeroBannerCard
import com.rcmiku.music.ui.design.SectionHeader
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.ui.navigation.JetMeloBoundsTransform
import com.rcmiku.music.ui.navigation.PlaylistNav
import com.rcmiku.music.ui.navigation.Screen
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.utils.ImagePreloadUtil
import com.rcmiku.music.viewModel.ExploreScreenViewModel

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    navController: NavHostController,
    exploreScreenViewModel: ExploreScreenViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    bottomContentPadding: Dp = 0.dp
) {
    val topListState by exploreScreenViewModel.topList.collectAsStateWithLifecycle()
    val newAlbumState by exploreScreenViewModel.newAlbum.collectAsStateWithLifecycle()
    val allNewAlbumState by exploreScreenViewModel.allNewAlbum.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    with(sharedTransitionScope) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.explore),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                    },
                    actions = {
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
            val topList = topListState?.getOrNull()
            val newAlbum = newAlbumState?.getOrNull()
            val allNewAlbum = allNewAlbumState?.getOrNull()

            val context = LocalContext.current
            val fallbackDominant = MaterialTheme.colorScheme.surfaceContainer
            val fallbackAccent = MaterialTheme.colorScheme.primary

            LaunchedEffect(topList, newAlbum, allNewAlbum, fallbackDominant, fallbackAccent) {
                val items = mutableListOf<Pair<Any?, String?>>()
                topList?.list?.take(5)?.forEach { items.add(it.cover to it.id.toString()) }
                newAlbum?.albums?.take(6)?.forEach { items.add(it.picUrl to it.id.toString()) }
                allNewAlbum?.albums?.take(6)?.forEach { items.add(it.picUrl to it.id.toString()) }
                if (items.isNotEmpty()) {
                    PaletteExtractor.preloadBatch(
                        context = context,
                        items = items,
                        fallbackDominant = fallbackDominant,
                        fallbackAccent = fallbackAccent
                    )
                }
            }

            LaunchedEffect(topList) {
                topList?.list?.let { charts ->
                    if (charts.isNotEmpty()) {
                        ImagePreloadUtil.preloadPlaylistCovers(context, charts)
                    }
                }
            }

            LaunchedEffect(newAlbum, allNewAlbum) {
                newAlbum?.albums?.let { albums ->
                    if (albums.isNotEmpty()) {
                        ImagePreloadUtil.preloadAlbumCovers(context, albums)
                    }
                }
                allNewAlbum?.albums?.let { albums ->
                    if (albums.isNotEmpty()) {
                        ImagePreloadUtil.preloadAlbumCovers(context, albums)
                    }
                }
            }

            val weekData = newAlbum?.albums
            val weekIds = remember(weekData) { weekData?.map { it.id }?.toSet().orEmpty() }
            val monthData = remember(allNewAlbum, weekIds) {
                allNewAlbum?.albums?.filterNot { it.id in weekIds }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(JetMeloShapes.full)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(role = Role.Button) { navController.navigate(Screen.Search.route) }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    if (topList != null && topList.list.isNotEmpty()) {
                        SectionHeader(
                            title = stringResource(R.string.top_list),
                            actionText = stringResource(R.string.view_all),
                            onActionClick = { navController.navigate(Screen.TopList.route) }
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(topList.list.take(5), key = { it.id }) { chart ->
                                Box(
                                    modifier = Modifier.width(280.dp)
                                ) {
                                    val coverKey = remember(chart.id) { "cover_${chart.id}" }
                                    HeroBannerCard(
                                        imageUrl = chart.cover,
                                        cacheKey = coverKey,
                                        badgeText = stringResource(R.string.top_list),
                                        title = chart.name,
                                        subtitle = chart.description ?: stringResource(R.string.song_size, chart.trackCount),
                                        aspectRatio = 1.7f,
                                        imageModifier = Modifier.sharedElement(
                                            sharedTransitionScope.rememberSharedContentState(
                                                key = coverKey
                                            ),
                                            animatedVisibilityScope = animatedContentScope,
                                            boundsTransform = JetMeloBoundsTransform,
                                            placeHolderSize = SharedTransitionScope.PlaceHolderSize.contentSize,
                                            clipInOverlayDuringTransition = OverlayClip(JetMeloShapes.medium)
                                        ),
                                        onClick = {
                                            coroutineScope.launch {
                                                PaletteExtractor.preload(
                                                    context = context,
                                                    artworkUri = chart.cover,
                                                    songId = chart.id.toString(),
                                                    fallbackDominant = fallbackDominant,
                                                    fallbackAccent = fallbackAccent
                                                )
                                            }
                                            navController.navigate(
                                                PlaylistNav(
                                                    playlistId = chart.id,
                                                    limit = chart.trackCount.takeIf { it > 0 } ?: 999,
                                                    coverImgUrl = chart.cover,
                                                    title = chart.name,
                                                    enableSharedTransition = true
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {

                    if (!weekData.isNullOrEmpty()) {
                        SectionHeader(
                            title = stringResource(R.string.newest_album_week)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(weekData, key = { it.id }) { album ->
                                Column(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .clip(JetMeloShapes.medium)
                                        .clickable(role = Role.Button) {
                                            coroutineScope.launch {
                                                PaletteExtractor.preload(
                                                    context = context,
                                                    artworkUri = album.picUrl,
                                                    songId = album.id.toString(),
                                                    fallbackDominant = fallbackDominant,
                                                    fallbackAccent = fallbackAccent
                                                )
                                            }
                                            navController.navigate(
                                                AlbumNav(
                                                    albumId = album.id,
                                                    coverImgUrl = album.picUrl,
                                                    title = album.name
                                                )
                                            )
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(JetMeloShapes.medium)
                                    ) {
                                        val context = LocalContext.current
                                        val coverKey = remember(album.id) { "cover_${album.id}" }
                                        val imageRequest = remember(context, album.picUrl, coverKey) {
                                            ImageRequest.Builder(context)
                                                .data(album.picUrl.takeIf { it.isNotEmpty() })
                                                .memoryCacheKey(coverKey)
                                                .placeholderMemoryCacheKey(coverKey)
                                                .build()
                                        }
                                        AsyncImage(
                                            model = imageRequest,
                                            contentDescription = album.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(JetMeloShapes.medium)
                                                .sharedElement(
                                                    sharedTransitionScope.rememberSharedContentState(
                                                        key = coverKey
                                                    ),
                                                    animatedVisibilityScope = animatedContentScope,
                                                    boundsTransform = JetMeloBoundsTransform,
                                                    placeHolderSize = SharedTransitionScope.PlaceHolderSize.contentSize,
                                                    clipInOverlayDuringTransition = OverlayClip(JetMeloShapes.medium)
                                                )
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        text = album.artist.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    if (!monthData.isNullOrEmpty()) {
                        SectionHeader(
                            title = stringResource(R.string.newest_album_month)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(monthData, key = { it.id }) { album ->
                                Column(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .clip(JetMeloShapes.medium)
                                        .clickable(role = Role.Button) {
                                            coroutineScope.launch {
                                                PaletteExtractor.preload(
                                                    context = context,
                                                    artworkUri = album.picUrl,
                                                    songId = album.id.toString(),
                                                    fallbackDominant = fallbackDominant,
                                                    fallbackAccent = fallbackAccent
                                                )
                                            }
                                            navController.navigate(
                                                AlbumNav(
                                                    albumId = album.id,
                                                    coverImgUrl = album.picUrl,
                                                    title = album.name
                                                )
                                            )
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(JetMeloShapes.medium)
                                    ) {
                                        val context = LocalContext.current
                                        val coverKey = remember(album.id) { "cover_${album.id}" }
                                        val imageRequest = remember(context, album.picUrl, coverKey) {
                                            ImageRequest.Builder(context)
                                                .data(album.picUrl.takeIf { it.isNotEmpty() })
                                                .memoryCacheKey(coverKey)
                                                .placeholderMemoryCacheKey(coverKey)
                                                .build()
                                        }
                                        AsyncImage(
                                            model = imageRequest,
                                            contentDescription = album.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(JetMeloShapes.medium)
                                                .sharedElement(
                                                    sharedTransitionScope.rememberSharedContentState(
                                                        key = coverKey
                                                    ),
                                                    animatedVisibilityScope = animatedContentScope,
                                                    boundsTransform = JetMeloBoundsTransform,
                                                    placeHolderSize = SharedTransitionScope.PlaceHolderSize.contentSize,
                                                    clipInOverlayDuringTransition = OverlayClip(JetMeloShapes.medium)
                                                )
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        text = album.artist.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (topListState?.isFailure == true && newAlbumState?.isFailure == true && allNewAlbumState?.isFailure == true) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(onClick = { exploreScreenViewModel.refresh() }) {
                                Text(text = "重试")
                            }
                        }
                    }
                }
            }
        }
    }
}
