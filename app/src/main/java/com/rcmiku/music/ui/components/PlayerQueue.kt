package com.rcmiku.music.ui.components

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.media3.common.MediaMetadata
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.constants.MediaSessionConstants
import com.rcmiku.music.extensions.currentMediaItems
import com.rcmiku.music.extensions.playMediaAt
import com.rcmiku.music.extensions.playMediaAtMediaId
import com.rcmiku.music.extensions.removeSong
import com.rcmiku.music.ui.design.ImmersiveBackground
import com.rcmiku.music.ui.design.LocalArtworkColors
import com.rcmiku.music.ui.icons.AudioLines
import com.rcmiku.music.ui.icons.ChevronDown
import com.rcmiku.music.ui.icons.DragHandle
import com.rcmiku.music.ui.icons.Repeat
import com.rcmiku.music.ui.icons.RepeatOne
import com.rcmiku.music.ui.icons.Shuffle
import com.rcmiku.music.ui.theme.JetMeloShapes
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PlayerQueue(
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    mediaMetadata: MediaMetadata,
    onBackPressed: () -> Unit = {},
) {
    val playerState = LocalPlayerState.current
    val mediaController = LocalPlayerController.current.controller
    val isPlaying = playerState?.isPlaying == true
    val repeatMode = playerState?.repeatMode ?: 0
    val shuffleMode = playerState?.shuffleModeEnabled == true
    val currentMediaItems = playerState?.player?.currentMediaItems
    val currentMediaId = playerState?.player?.currentMediaItem?.mediaId
    val currentIndex = playerState?.player?.currentMediaItemIndex
    var cacheMediaItems by remember { mutableStateOf(currentMediaItems) }
    val artworkColors = LocalArtworkColors.current

    val repeatIcon = when (repeatMode) {
        0 -> Repeat
        1 -> RepeatOne
        else -> Repeat
    }

    BackHandler {
        onBackPressed()
    }

    ImmersiveBackground(
        modifier = modifier.fillMaxSize(),
        artworkUri = mediaMetadata.artworkUri
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = ChevronDown,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.playing_list),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    currentMediaItems?.size?.let { size ->
                        Text(
                            text = stringResource(R.string.song_size, size),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            mediaController?.sendCustomCommand(
                                MediaSessionConstants.CommandToggleShuffle,
                                Bundle.EMPTY
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Shuffle,
                            contentDescription = null,
                            tint = if (shuffleMode) artworkColors.accentColor else Color.White.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newMode = when (repeatMode) {
                                0 -> 2
                                1 -> 0
                                2 -> 1
                                else -> 0
                            }
                            mediaController?.repeatMode = newMode
                        }
                    ) {
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = null,
                            tint = if (repeatMode != 0) artworkColors.accentColor else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            val view = LocalView.current
            val lazyListState = rememberLazyListState()
            var dragInfo by remember {
                mutableStateOf<Pair<Int, Int>?>(null)
            }

            LaunchedEffect(Unit) {
                if (currentIndex != null) {
                    lazyListState.scrollToItem(currentIndex)
                }
            }

            val reorderableLazyListState =
                rememberReorderableLazyListState(lazyListState) { from, to ->
                    cacheMediaItems = cacheMediaItems?.toMutableList()?.apply {
                        add(to.index, removeAt(from.index))
                        val currentDragInfo = dragInfo
                        dragInfo = if (currentDragInfo == null) {
                            from.index to to.index
                        } else {
                            currentDragInfo.first to to.index
                        }
                    }
                    ViewCompat.performHapticFeedback(
                        view,
                        HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK
                    )
                }

            LaunchedEffect(reorderableLazyListState.isAnyItemDragging) {
                if (!reorderableLazyListState.isAnyItemDragging) {
                    dragInfo?.let { (from, to) ->
                        mediaController?.moveMediaItem(from, to)
                        dragInfo = null
                    }
                }
            }

            LaunchedEffect(playerState?.timeline) {
                if (!reorderableLazyListState.isAnyItemDragging) {
                    cacheMediaItems = currentMediaItems
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = WindowInsets.navigationBars.asPaddingValues()
            ) {
                cacheMediaItems?.let { mediaItemList ->
                    itemsIndexed(mediaItemList, key = { _, mediaItem ->
                        mediaItem.mediaId
                    }) { index, mediaItem ->

                        val isCurrent = currentMediaId == mediaItem.mediaId
                        val dismissState = rememberSwipeToDismissBoxState(
                            positionalThreshold = { totalDistance -> totalDistance },
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    cacheMediaItems = cacheMediaItems?.toMutableList()?.apply {
                                        removeIf { it.mediaId == mediaItem.mediaId }
                                    }
                                    mediaController?.removeSong(mediaItem.mediaId)
                                }
                                true
                            }
                        )

                        ReorderableItem(
                            reorderableLazyListState,
                            key = mediaItem.mediaId,
                        ) { _ ->
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .clip(JetMeloShapes.medium)
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = "删除",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                    }
                                }
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                        .clickable {
                                            mediaController?.playMediaAtMediaId(mediaItem.mediaId)
                                        },
                                    shape = JetMeloShapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent)
                                            artworkColors.accentColor.copy(alpha = 0.25f)
                                        else
                                            Color.White.copy(alpha = 0.08f)
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        AsyncImage(
                                            model = mediaItem.mediaMetadata.artworkUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = (if (isCurrent) imageModifier else Modifier)
                                                .size(44.dp)
                                                .clip(JetMeloShapes.small)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = mediaItem.mediaMetadata.title?.toString() ?: "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isCurrent) artworkColors.accentColor else Color.White,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isCurrent && isPlaying) {
                                            Icon(
                                                imageVector = AudioLines,
                                                contentDescription = null,
                                                tint = artworkColors.accentColor,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .padding(end = 6.dp)
                                            )
                                        }

                                        IconButton(
                                            modifier = Modifier
                                                .draggableHandle(
                                                    onDragStarted = {
                                                        ViewCompat.performHapticFeedback(
                                                            view,
                                                            HapticFeedbackConstantsCompat.GESTURE_START
                                                        )
                                                    },
                                                    onDragStopped = {
                                                        ViewCompat.performHapticFeedback(
                                                            view,
                                                            HapticFeedbackConstantsCompat.GESTURE_END
                                                        )
                                                    }
                                                )
                                                .size(36.dp),
                                            onClick = {},
                                        ) {
                                            Icon(
                                                imageVector = DragHandle,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.5f)
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
    }
}