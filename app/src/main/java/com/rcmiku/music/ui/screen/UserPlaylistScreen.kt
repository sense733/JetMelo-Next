package com.rcmiku.music.ui.screen

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.rcmiku.music.R
import com.rcmiku.music.ui.design.PaletteExtractor
import com.rcmiku.music.ui.navigation.JetMeloBoundsTransform
import com.rcmiku.music.ui.navigation.PlaylistNav
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.utils.ImagePreloadUtil
import com.rcmiku.music.viewModel.UserPlaylistScreenViewModel
import com.rcmiku.ncmapi.api.account.UserPlaylistType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun UserPlaylistScreen(
    navController: NavHostController,
    userPlaylistScreenViewModel: UserPlaylistScreenViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    bottomContentPadding: Dp = 0.dp
) {
    val type = userPlaylistScreenViewModel.userPlaylistType
    val playlistState by userPlaylistScreenViewModel.playlist.collectAsStateWithLifecycle()
    val isLoading by userPlaylistScreenViewModel.isLoading.collectAsStateWithLifecycle()
    val loadError by userPlaylistScreenViewModel.loadError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    with(sharedTransitionScope) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (type) {
                                UserPlaylistType.CREATE -> stringResource(R.string.create_playlist)
                                UserPlaylistType.COLLECT -> stringResource(R.string.collect_playlist)
                                null -> stringResource(R.string.my_playlists)
                            },
                            style = MaterialTheme.typography.titleLarge,
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
                    }
                )
            }
        ) { padding ->
            val playlistResponse = playlistState
            val fallbackDominant = MaterialTheme.colorScheme.surfaceContainer
            val fallbackAccent = MaterialTheme.colorScheme.primary

            LaunchedEffect(playlistResponse, fallbackDominant, fallbackAccent) {
                val playlists = playlistResponse?.data?.playlist ?: return@LaunchedEffect
                val items = playlists.take(15).map { it.cover to it.id.toString() }
                if (items.isNotEmpty()) {
                    PaletteExtractor.preloadBatch(
                        context = context,
                        items = items,
                        fallbackDominant = fallbackDominant,
                        fallbackAccent = fallbackAccent
                    )
                }
            }

            LaunchedEffect(playlistResponse) {
                playlistResponse?.data?.playlist?.let { playlists ->
                    if (playlists.isNotEmpty()) {
                        ImagePreloadUtil.preloadPlaylistCovers(context, playlists)
                    }
                }
            }

            when {
                playlistResponse == null && isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding()),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                playlistResponse == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding())
                            .clickable(role = Role.Button) {
                                userPlaylistScreenViewModel.retry()
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
                    LazyVerticalGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding()),
                        columns = GridCells.Adaptive(150.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 12.dp,
                            end = 16.dp,
                            bottom = 12.dp + bottomContentPadding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(playlistResponse.data.playlist, key = { it.id }) { playlist ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(JetMeloShapes.medium)
                                    .clickable(role = Role.Button) {
                                        coroutineScope.launch {
                                            PaletteExtractor.preload(
                                                context = context,
                                                artworkUri = playlist.cover,
                                                songId = playlist.id.toString(),
                                                fallbackDominant = fallbackDominant,
                                                fallbackAccent = fallbackAccent
                                            )
                                        }
                                        navController.navigate(
                                            PlaylistNav(
                                                playlistId = playlist.id,
                                                limit = playlist.trackCount.takeIf { it > 0 } ?: 999,
                                                coverImgUrl = playlist.cover,
                                                title = playlist.name,
                                                enableSharedTransition = true
                                            )
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(JetMeloShapes.medium)
                                ) {
                                    val context = LocalContext.current
                                    val coverKey = remember(playlist.id) { "cover_${playlist.id}" }
                                    val imageRequest = remember(context, playlist.cover, coverKey) {
                                        ImageRequest.Builder(context)
                                            .data(playlist.cover.takeIf { it.isNotEmpty() })
                                            .memoryCacheKey(coverKey)
                                            .placeholderMemoryCacheKey(coverKey)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = imageRequest,
                                        contentDescription = playlist.name,
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
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = stringResource(R.string.song_size, playlist.trackCount),
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