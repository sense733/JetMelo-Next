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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Timeline
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.constants.MediaSessionConstants
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
import com.rcmiku.music.ui.icons.Remove
import com.rcmiku.music.ui.icons.Shuffle
import com.rcmiku.music.ui.theme.AdaptiveArtworkShape
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.utils.SongListUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PlayerQueue(
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    artistModifier: Modifier = Modifier,
    mediaMetadata: MediaMetadata,
    onBackPressed: () -> Unit = {},
) {
    val context = LocalContext.current
    val playerState = LocalPlayerState.current
    val mediaController = LocalPlayerController.current.controller
    val isPlaying = playerState?.isPlaying == true
    val repeatMode = playerState?.repeatMode ?: 0
    val shuffleMode = playerState?.shuffleModeEnabled == true
    val timeline = playerState?.timeline
    val currentMediaId = playerState?.currentMediaItem?.mediaId
    val currentIndex = playerState?.mediaItemIndex

    var timelineItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    LaunchedEffect(timeline) {
        timelineItems = if (timeline != null && timeline.windowCount > 0) {
            withContext(Dispatchers.Default) {
                val window = Timeline.Window()
                List(timeline.windowCount) {
                    timeline.getWindow(it, window).mediaItem
                }
            }
        } else {
            emptyList()
        }
    }

    var cacheMediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
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
                    Text(
                        text = stringResource(R.string.song_size, timelineItems.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            mediaController?.sendCommandWithFeedback(
                                context,
                                MediaSessionConstants.CommandToggleShuffle
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
            val initialIndex = remember {
                val idx = currentIndex ?: 0
                (idx - 1).coerceAtLeast(0)
            }
            val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
            var dragInfo by remember {
                mutableStateOf<Triple<Int, Int, Int>?>(null)
            }
            var lastManualScrollMs by remember { mutableLongStateOf(0L) }

            LaunchedEffect(lazyListState.isScrollInProgress) {
                if (lazyListState.isScrollInProgress) {
                    lastManualScrollMs = System.currentTimeMillis()
                }
            }

            LaunchedEffect(currentIndex) {
                val now = System.currentTimeMillis()
                if (currentIndex != null && !lazyListState.isScrollInProgress && now - lastManualScrollMs > 3000L) {
                    val visibleIndices = lazyListState.layoutInfo.visibleItemsInfo.map { it.index }
                    if (currentIndex !in visibleIndices) {
                        lazyListState.animateScrollToItem(currentIndex)
                    }
                }
            }

            val reorderableLazyListState =
                rememberReorderableLazyListState(lazyListState) { from, to ->
                    val currentItems = cacheMediaItems.toMutableList()
                    if (from.index in currentItems.indices && to.index in currentItems.indices) {
                        currentItems.add(to.index, currentItems.removeAt(from.index))
                        cacheMediaItems = currentItems
                        val currentDragInfo = dragInfo
                        val baseCount = mediaController?.mediaItemCount ?: 0
                        dragInfo = if (currentDragInfo == null) {
                            Triple(from.index, to.index, baseCount)
                        } else {
                            Triple(currentDragInfo.first, to.index, baseCount)
                        }
                    }
                    ViewCompat.performHapticFeedback(
                        view,
                        HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK
                    )
                }

            LaunchedEffect(reorderableLazyListState.isAnyItemDragging) {
                if (!reorderableLazyListState.isAnyItemDragging) {
                    dragInfo?.let { (from, to, baseCount) ->
                        val controller = mediaController
                        if (controller != null && controller.mediaItemCount == baseCount &&
                            from in 0 until baseCount && to in 0 until baseCount && from != to
                        ) {
                            controller.moveMediaItem(from, to)
                        }
                        dragInfo = null
                    }
                }
            }

            LaunchedEffect(timelineItems) {
                if (!reorderableLazyListState.isAnyItemDragging) {
                    cacheMediaItems = timelineItems
                }
            }

            val isQueueEmpty = (timeline?.windowCount ?: 0) == 0 || cacheMediaItems.isEmpty()
            val queueKeys = remember(cacheMediaItems) {
                val totalCounts = mutableMapOf<String, Int>()
                for (item in cacheMediaItems) {
                    totalCounts[item.mediaId] = (totalCounts[item.mediaId] ?: 0) + 1
                }
                val seenCounts = mutableMapOf<String, Int>()
                cacheMediaItems.mapIndexed { index, item ->
                    val id = item.mediaId
                    val count = totalCounts[id] ?: 1
                    if (count == 1) {
                        id
                    } else {
                        val n = (seenCounts[id] ?: 0) + 1
                        seenCounts[id] = n
                        "$id#$n"
                    }
                }
            }

            if (isQueueEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyText = if (mediaController == null || !mediaController.isConnected) {
                        "正在连接播放服务…"
                    } else {
                        "播放队列为空"
                    }
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    contentPadding = WindowInsets.navigationBars.asPaddingValues()
                ) {
                    itemsIndexed(
                        cacheMediaItems,
                        key = { index, _ -> queueKeys.getOrElse(index) { "$index" } }
                    ) { index, mediaItem ->
                        val itemKey = queueKeys.getOrElse(index) { "$index" }
                        val isCurrent = currentMediaId == mediaItem.mediaId
                        val density = LocalDensity.current
                        val minDeleteDistanceDp = 140.dp
                        val minDeleteDistancePx = with(density) { minDeleteDistanceDp.toPx() }

                        val coroutineScope = rememberCoroutineScope()
                        val offsetX = remember(itemKey) { Animatable(0f) }
                        var isDeleting by remember(itemKey) { mutableStateOf(false) }
                        var itemWidthPx by remember(itemKey) { mutableFloatStateOf(0f) }
                        var isThresholdReached by remember(itemKey) { mutableStateOf(false) }

                        val deleteIconScale by animateFloatAsState(
                            targetValue = if (isThresholdReached) 1.08f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "deleteScale"
                        )

                        val performDelete: () -> Unit = {
                            val controller = mediaController
                            if (controller != null) {
                                cacheMediaItems = cacheMediaItems.toMutableList().apply {
                                    if (index in indices && get(index).mediaId == mediaItem.mediaId) {
                                        removeAt(index)
                                    } else {
                                        val removeIdx = indexOfFirst { it.mediaId == mediaItem.mediaId }
                                        if (removeIdx >= 0) removeAt(removeIdx)
                                    }
                                }
                                if (index < controller.mediaItemCount && controller.getMediaItemAt(index).mediaId == mediaItem.mediaId) {
                                    controller.removeMediaItem(index)
                                    SongListUtil.removePlaylistItemAt(index, mediaItem.mediaId)
                                } else {
                                    controller.removeSong(mediaItem.mediaId)
                                }
                            }
                        }

                        val currentOffset = offsetX.value
                        val dragDistancePx = (-currentOffset).coerceAtLeast(0f)
                        val dragDistanceDp = with(density) { dragDistancePx.toDp() }
                        val maxBgWidthDp = with(density) { (if (itemWidthPx > 0f) itemWidthPx else Float.MAX_VALUE).toDp() }
                        val visualBackgroundWidthDp = dragDistanceDp.coerceAtMost(maxBgWidthDp)

                        ReorderableItem(
                            reorderableLazyListState,
                            key = itemKey,
                        ) { _ ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { size ->
                                        itemWidthPx = size.width.toFloat()
                                    }
                                    .pointerInput(itemKey) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (isDeleting) return@detectHorizontalDragGestures
                                                coroutineScope.launch {
                                                    if (-offsetX.value >= minDeleteDistancePx) {
                                                        isDeleting = true
                                                        ViewCompat.performHapticFeedback(
                                                            view,
                                                            HapticFeedbackConstantsCompat.GESTURE_END
                                                        )
                                                        val targetOffset = if (itemWidthPx > 0f) -itemWidthPx - 100f else -1500f
                                                        offsetX.animateTo(
                                                            targetValue = targetOffset,
                                                            animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                                                        )
                                                        performDelete()
                                                    } else {
                                                        isThresholdReached = false
                                                        offsetX.animateTo(
                                                            targetValue = 0f,
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        )
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                if (!isDeleting) {
                                                    isThresholdReached = false
                                                    coroutineScope.launch {
                                                        offsetX.animateTo(
                                                            targetValue = 0f,
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            )
                                                        )
                                                    }
                                                }
                                            },
                                            onHorizontalDrag = { change, dragAmount ->
                                                if (!isDeleting) {
                                                    if (offsetX.value == 0f && dragAmount > 0f) {
                                                        return@detectHorizontalDragGestures
                                                    }
                                                    change.consume()
                                                    val previousOffset = offsetX.value
                                                    val newOffset = (previousOffset + dragAmount).coerceIn(
                                                        minimumValue = if (itemWidthPx > 0f) -itemWidthPx else -2000f,
                                                        maximumValue = 0f
                                                    )
                                                    val nowOver = -newOffset >= minDeleteDistancePx
                                                    if (nowOver != isThresholdReached) {
                                                        isThresholdReached = nowOver
                                                        ViewCompat.performHapticFeedback(
                                                            view,
                                                            HapticFeedbackConstantsCompat.CLOCK_TICK
                                                        )
                                                    }
                                                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                                        offsetX.snapTo(newOffset)
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                if (visualBackgroundWidthDp > 0.dp) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .matchParentSize(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(visualBackgroundWidthDp)
                                                .padding(top = 2.dp, bottom = 2.dp, end = 8.dp)
                                                .clip(JetMeloShapes.medium)
                                                .background(Color(0xFFD32F2F)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (visualBackgroundWidthDp > 36.dp) {
                                                val contentAlpha = ((visualBackgroundWidthDp - 36.dp) / 24.dp).coerceIn(0f, 1f)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .graphicsLayer {
                                                            alpha = contentAlpha
                                                            scaleX = deleteIconScale
                                                            scaleY = deleteIconScale
                                                        }
                                                        .padding(horizontal = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Remove,
                                                        contentDescription = stringResource(R.string.delete),
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    if (visualBackgroundWidthDp > 72.dp) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = stringResource(R.string.delete),
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .clip(JetMeloShapes.medium)
                                        .background(
                                            if (isCurrent)
                                                artworkColors.accentColor.copy(alpha = 0.22f)
                                            else
                                                Color.Transparent
                                        )
                                        .clickable(enabled = !isDeleting && offsetX.value == 0f) {
                                            mediaController?.playMediaAt(index)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(mediaItem.mediaMetadata.artworkUri)
                                                .size(Size(176, 176))
                                                .memoryCacheKey(mediaItem.mediaMetadata.artworkUri?.toString())
                                                .diskCacheKey(mediaItem.mediaMetadata.artworkUri?.toString())
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = (if (isCurrent) imageModifier else Modifier)
                                                .size(44.dp)
                                                .clip(AdaptiveArtworkShape)
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
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = if (isCurrent) titleModifier else Modifier
                                            )
                                            Text(
                                                text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isCurrent)
                                                    artworkColors.accentColor.copy(alpha = 0.75f)
                                                else
                                                    Color.White.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = if (isCurrent) artistModifier else Modifier
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

                                        Box(
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
                                            contentAlignment = Alignment.Center
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