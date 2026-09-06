package com.rcmiku.music.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.extensions.playMediaAtId
import com.rcmiku.music.extensions.setCloudSongPlaylist
import com.rcmiku.music.ui.components.CloudSongListItem
import com.rcmiku.music.viewModel.CloudSongScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSongScreen(
    navController: NavHostController,
    cloudSongScreenViewModel: CloudSongScreenViewModel = hiltViewModel(),
    bottomContentPadding: Dp = 0.dp
) {

    val cloudSong = cloudSongScreenViewModel.cloudSong.collectAsLazyPagingItems()
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()
    val uid = cloudSongScreenViewModel.uid

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.cloud_music),
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
                        contentDescription = "返回"
                    )
                }
            },
        )
    }) { padding ->
        val refresh = cloudSong.loadState.refresh
        when {
            refresh is LoadState.Loading && cloudSong.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            refresh is LoadState.Error && cloudSong.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = refresh.error.localizedMessage ?: "加载失败",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { cloudSong.retry() }) {
                            Text(text = "重试")
                        }
                    }
                }
            }
            refresh is LoadState.NotLoading && cloudSong.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.song_size, 0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = bottomContentPadding
                    ),
                ) {
                    items(
                        count = cloudSong.itemCount,
                        key = { index -> "${cloudSong.peek(index)?.simpleSong?.id}_$index" }
                    ) { index ->
                        cloudSong[index]?.let { item ->
                            CloudSongListItem(
                                songIndex = index + 1,
                                cloudSong = item,
                                isPlaying = isPlaying,
                                isActive = currentMediaId == item.simpleSong.id,
                                modifier = Modifier.clickable {
                                    if (uid != null) {
                                        val snapshot = cloudSong.itemSnapshotList.items
                                        val queue = if (snapshot.size > 500) snapshot.take(500) else snapshot
                                        mediaController?.setCloudSongPlaylist(
                                            uid = uid,
                                            cloudSongs = queue
                                        )
                                        mediaController?.playMediaAtId(item.simpleSong.id)
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