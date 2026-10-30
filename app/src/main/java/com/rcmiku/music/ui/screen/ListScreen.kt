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
import androidx.compose.material3.Button
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.rcmiku.music.ui.design.PaletteExtractor
import com.rcmiku.music.ui.navigation.JetMeloBoundsTransform
import com.rcmiku.music.ui.navigation.PlaylistNav
import com.rcmiku.music.ui.navigation.Screen
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.viewModel.ExploreScreenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ListScreen(
    navController: NavHostController,
    exploreScreenViewModel: ExploreScreenViewModel = runCatching {
        val parentEntry = remember(navController) {
            navController.getBackStackEntry(Screen.Explore.route)
        }
        hiltViewModel<ExploreScreenViewModel>(parentEntry)
    }.getOrElse { hiltViewModel() },
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    bottomContentPadding: Dp = 0.dp
) {
    val topListState by exploreScreenViewModel.topList.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    with(sharedTransitionScope) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.top_list),
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
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            val topList = topListState?.getOrNull()
            val fallbackDominant = MaterialTheme.colorScheme.surfaceContainer
            val fallbackAccent = MaterialTheme.colorScheme.primary

            LaunchedEffect(topList, fallbackDominant, fallbackAccent) {
                val charts = topList?.list ?: return@LaunchedEffect
                val items = charts.take(15).map { it.cover to it.id.toString() }
                if (items.isNotEmpty()) {
                    PaletteExtractor.preloadBatch(
                        context = context,
                        items = items,
                        fallbackDominant = fallbackDominant,
                        fallbackAccent = fallbackAccent
                    )
                }
            }

            if (topList != null) {
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding()),
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp + bottomContentPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(topList.list, key = { it.id }) { chart ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(JetMeloShapes.medium)
                                .clickable(role = Role.Button) {
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
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(JetMeloShapes.medium)
                            ) {
                                val context = LocalContext.current
                                val coverKey = remember(chart.id) { "cover_${chart.id}" }
                                val imageRequest = remember(context, chart.cover, coverKey) {
                                    ImageRequest.Builder(context)
                                        .data(chart.cover.takeIf { it.isNotEmpty() })
                                        .memoryCacheKey(coverKey)
                                        .placeholderMemoryCacheKey(coverKey)
                                        .build()
                                }
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = chart.name,
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
                                text = chart.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = chart.description ?: stringResource(R.string.song_size, chart.trackCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else if (topListState?.isFailure == true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding()),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = { exploreScreenViewModel.fetchTopList() }) {
                        Text(text = "重试")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding()),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}