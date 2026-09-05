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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.extensions.playMediaAt
import com.rcmiku.music.extensions.playMediaAtId
import com.rcmiku.music.extensions.setRadioPlaylist
import com.rcmiku.music.ui.components.RadioListItem
import com.rcmiku.music.ui.components.RadioThumbnailImage
import com.rcmiku.music.ui.icons.PlayArrow
import com.rcmiku.music.utils.formatPlayCount
import com.rcmiku.music.viewModel.ProgramRadioScreenViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProgramRadioScreen(
    navController: NavController,
    programRadioScreenViewModel: ProgramRadioScreenViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    bottomContentPadding: Dp = 0.dp
) {

    val radioInfo by programRadioScreenViewModel.radioInfo.collectAsStateWithLifecycle()
    val radioInfoError by programRadioScreenViewModel.radioInfoError.collectAsStateWithLifecycle()
    val radioList = programRadioScreenViewModel.radioList.collectAsLazyPagingItems()
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()
    val listState = rememberLazyListState()


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.voice_list),
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
        }
    ) { padding ->
        if (radioInfo != null) {
            val currentRadioInfo = radioInfo!!
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = bottomContentPadding
                ),
                state = listState,
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            RadioThumbnailImage(
                                url = currentRadioInfo.data.picUrl,
                            )
                            FilledIconButton(
                                modifier = Modifier.size(48.dp),
                                onClick = {
                                    val snapshot = radioList.itemSnapshotList.items
                                    // 截断上限避免超大列表快照拷贝阻塞主线程
                                    val queue = if (snapshot.size > 500) snapshot.take(500) else snapshot
                                    mediaController?.setRadioPlaylist(queue)
                                    mediaController?.playMediaAt()
                                },
                            ) {
                                Icon(
                                    PlayArrow,
                                    contentDescription = stringResource(R.string.play),
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = currentRadioInfo.data.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                R.string.total_play_count,
                                formatPlayCount(currentRadioInfo.data.playCount)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        currentRadioInfo.data.desc?.let { description ->
                            Text(
                                text = description,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 4,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                    }

                }

                items(
                    count = radioList.itemCount,
                    key = { index -> "${radioList.peek(index)?.id}_$index" }
                ) { index ->
                    radioList[index]?.let { item ->
                        RadioListItem(
                            radio = item,
                            isPlaying = isPlaying,
                            isActive = currentMediaId == item.mainSong.id,
                            modifier = Modifier.clickable {
                                val snapshot = radioList.itemSnapshotList.items
                                // 截断上限避免超大列表快照拷贝阻塞主线程
                                val queue = if (snapshot.size > 500) snapshot.take(500) else snapshot
                                mediaController?.setRadioPlaylist(queue)
                                mediaController?.playMediaAtId(item.mainSong.id)
                            })
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (radioInfoError != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = radioInfoError?.localizedMessage ?: "加载失败",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { programRadioScreenViewModel.retryRadioInfo() }) {
                            Text("重试")
                        }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}