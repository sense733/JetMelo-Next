package com.rcmiku.music.ui.screen

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.rcmiku.music.R
import com.rcmiku.music.ui.components.AlbumListItem
import com.rcmiku.music.ui.components.ListThumbnailImage
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.viewModel.AlbumSublistScreenViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumSublistScreen(
    navController: NavHostController,
    albumSublistScreenViewModel: AlbumSublistScreenViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    bottomContentPadding: Dp = 0.dp
) {
    val albumSublist = albumSublistScreenViewModel.albumSublist.collectAsLazyPagingItems()

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.my_album),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    navController.navigateUp()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            },
        )
    }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = bottomContentPadding
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val loadState = albumSublist.loadState
            when {
                loadState.refresh is LoadState.Loading && albumSublist.itemCount == 0 -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                loadState.refresh is LoadState.Error && albumSublist.itemCount == 0 -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(onClick = { albumSublist.retry() }) {
                                Text(text = stringResource(R.string.operation_failed))
                            }
                        }
                    }
                }
                loadState.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached && albumSublist.itemCount == 0 -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_brief),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    items(
                        count = albumSublist.itemCount,
                        key = { index -> index }
                    ) { index ->
                        albumSublist[index]?.let {
                            with(sharedTransitionScope) {
                                AlbumListItem(
                                    album = it,
                                    thumbnailContent = {
                                        ListThumbnailImage(
                                            url = it.picUrl,
                                            modifier = Modifier.sharedElement(
                                                sharedTransitionScope.rememberSharedContentState(
                                                    key = "album_sublist_cover_${it.id}"
                                                ),
                                                animatedVisibilityScope = animatedContentScope
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .clip(JetMeloShapes.medium)
                                        .clickable(role = Role.Button) {
                                            navController.navigate(AlbumNav(albumId = it.id))
                                        }
                                )
                            }
                        }
                    }
                    if (loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}