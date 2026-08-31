package com.rcmiku.music.ui.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rcmiku.music.R
import com.rcmiku.music.ui.design.HeroBannerCard
import com.rcmiku.music.ui.design.SectionHeader
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.ui.navigation.PlaylistNav
import com.rcmiku.music.ui.navigation.Screen
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.viewModel.ExploreScreenViewModel

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    navController: NavHostController,
    exploreScreenViewModel: ExploreScreenViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    val topListState by exploreScreenViewModel.topList.collectAsState()
    val newAlbumState by exploreScreenViewModel.newAlbum.collectAsState()

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Search Bar Entry Pill
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(JetMeloShapes.full)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { navController.navigate(Screen.Search.route) }
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

                // 2. Music Charts Section (Top Lists)
                item {
                    val topList = topListState?.getOrNull()
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
                                    HeroBannerCard(
                                        imageUrl = chart.picUrl,
                                        badgeText = stringResource(R.string.top_list),
                                        title = chart.name,
                                        subtitle = chart.description ?: stringResource(R.string.song_size, chart.trackCount ?: 0),
                                        aspectRatio = 1.7f,
                                        onClick = {
                                            navController.navigate(
                                                PlaylistNav(
                                                    playlistId = chart.id,
                                                    limit = chart.trackCount
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. New Releases - Week
                item {
                    val weekData = newAlbumState?.weekData
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
                                        .clickable {
                                            navController.navigate(AlbumNav(albumId = album.id))
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(JetMeloShapes.medium)
                                    ) {
                                        AsyncImage(
                                            model = album.picUrl,
                                            contentDescription = album.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .sharedElement(
                                                    sharedTransitionScope.rememberSharedContentState(
                                                        key = "cover_${album.id}"
                                                    ),
                                                    animatedVisibilityScope = animatedContentScope
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

                // 4. New Releases - Month
                item {
                    val monthData = newAlbumState?.monthData
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
                                        .clickable {
                                            navController.navigate(AlbumNav(albumId = album.id))
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(JetMeloShapes.medium)
                                    ) {
                                        AsyncImage(
                                            model = album.picUrl,
                                            contentDescription = album.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .sharedElement(
                                                    sharedTransitionScope.rememberSharedContentState(
                                                        key = "cover_${album.id}"
                                                    ),
                                                    animatedVisibilityScope = animatedContentScope
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
            }
        }
    }
}