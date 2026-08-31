package com.rcmiku.music.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaMetadata
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.ui.design.ImmersiveBackground
import com.rcmiku.music.ui.design.LocalArtworkColors
import com.rcmiku.music.ui.design.QualityBadge
import com.rcmiku.music.ui.icons.ChevronDown
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.utils.parseLrc
import com.rcmiku.music.viewModel.LyricViewModel
import kotlinx.coroutines.launch

@Composable
fun Lyric(
    position: Long,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    mediaMetadata: MediaMetadata,
    onBackPressed: () -> Unit = {},
    lyricViewModel: LyricViewModel = hiltViewModel()
) {
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val currentMediaId = playerState?.currentMediaItem?.mediaId
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val lyric by lyricViewModel.lyric.collectAsState()
    val lrcLine = lyric?.lrc?.lyric?.parseLrc()
    var currentIndex by remember { mutableIntStateOf(0) }
    var autoScrollEnabled by remember { mutableStateOf(true) }
    val artworkColors = LocalArtworkColors.current

    LaunchedEffect(currentMediaId) {
        currentIndex = 0
        coroutineScope.launch {
            currentMediaId?.let {
                lyricViewModel.fetchLyric(it.toLong())
            }
        }
    }

    BackHandler {
        onBackPressed()
    }

    KeepScreenOn()

    ImmersiveBackground(
        modifier = modifier.fillMaxSize(),
        artworkUri = mediaMetadata.artworkUri
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar with artwork and song info
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(JetMeloShapes.small)
                        .clickable(onClick = onBackPressed)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    AsyncImage(
                        model = mediaMetadata.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = imageModifier
                            .size(44.dp)
                            .clip(JetMeloShapes.small)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mediaMetadata.title?.toString() ?: "",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.basicMarquee()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = mediaMetadata.artist?.toString() ?: "",
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.basicMarquee()
                            )
                            QualityBadge(qualityText = "HI-RES")
                        }
                    }
                }
            }

            // Lyric Scrolling Content
            lrcLine?.let { lrcLines ->
                LaunchedEffect(listState.isScrollInProgress) {
                    autoScrollEnabled = !listState.isScrollInProgress
                }

                LaunchedEffect(position) {
                    val index = lrcLines.indexOfLast { it.time <= position }
                    if (index != currentIndex) {
                        currentIndex = index
                        if (autoScrollEnabled) {
                            coroutineScope.launch {
                                if (index > 0) {
                                    val targetIndex = maxOf(currentIndex - 2, 0)
                                    val visibleItem =
                                        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                                    if (visibleItem == null) {
                                        listState.scrollToItem(targetIndex)
                                    } else {
                                        val itemOffset = visibleItem.offset
                                        listState.animateScrollBy(
                                            itemOffset.toFloat(),
                                            animationSpec = tween(
                                                durationMillis = 500,
                                                easing = EaseInOutCubic
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(Modifier.height(32.dp))
                    }

                    items(
                        count = lrcLines.size,
                    ) { index ->
                        val isCurrent = index == currentIndex
                        val currentText = lrcLines[index].text.isNotEmpty()

                        if (currentText) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(JetMeloShapes.medium)
                                    .clickable {
                                        lrcLines[index].time.let {
                                            mediaController?.seekTo(it)
                                            coroutineScope.launch {
                                                currentIndex = index
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                    .alpha(if (isCurrent) 1f else 0.45f)
                            ) {
                                Text(
                                    text = lrcLines[index].text,
                                    color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.85f),
                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = if (isCurrent) 28.sp else 22.sp,
                                    lineHeight = 1.25.em
                                )
                            }
                        } else {
                            Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                                if (isCurrent) {
                                    lrcLines.getOrNull(index + 1)?.time?.let { time ->
                                        ThreeDotsAnimation(
                                            times = lrcLines[index].time to time,
                                            dotColor = artworkColors.accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(120.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ThreeDotsAnimation(
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 10.dp,
    times: Pair<Long, Long>,
) {
    if (times.let { it.second - it.first } < 6000L)
        return

    val duration = times.second - times.first
    val transition = rememberInfiniteTransition()
    val scale by
    transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = (duration / 2).toInt()
                1.3f at 200 using EaseInOutCubic
                1f at 400 using EaseInOutCubic
            },
            repeatMode = RepeatMode.Restart
        ),
    )

    val scale2 by
    transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = (duration / 2).toInt()
                1.3f at 400 using EaseInOutCubic
                1f at 600 using EaseInOutCubic
            },
            repeatMode = RepeatMode.Restart
        ),
    )

    val scale3 by
    transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = (duration / 2).toInt()
                1.3f at 600 using EaseInOutCubic
                1f at 800 using EaseInOutCubic
            },
            repeatMode = RepeatMode.Restart
        ),
    )

    Row(
        modifier = modifier
            .padding(vertical = 12.dp)
            .height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(dotSize)) {
            drawCircle(
                color = dotColor,
                radius = (size.minDimension / 2) * scale,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
        Canvas(modifier = Modifier.size(dotSize)) {
            drawCircle(
                color = dotColor,
                radius = (size.minDimension / 2) * scale2,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
        Canvas(modifier = Modifier.size(dotSize)) {
            drawCircle(
                color = dotColor,
                radius = (size.minDimension / 2) * scale3,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
    }
}
