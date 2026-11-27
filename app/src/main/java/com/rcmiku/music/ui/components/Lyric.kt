package com.rcmiku.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player.STATE_READY
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.ui.design.ImmersiveBackground
import com.rcmiku.music.ui.design.LocalArtworkColors
import com.rcmiku.music.ui.design.PlayerContentColors
import com.rcmiku.music.ui.design.rememberPlayerContentColors
import com.rcmiku.music.ui.icons.ChevronDown
import com.rcmiku.music.ui.theme.AdaptiveArtworkShape
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.utils.ImagePreloadUtil
import com.rcmiku.music.utils.LrcLine
import com.rcmiku.music.utils.YrcWord
import com.rcmiku.music.viewModel.LyricViewModel
import com.rcmiku.music.viewModel.ParsedLyric
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.roundToInt

private const val LYRIC_SCROLL_LOOKAHEAD_MS = 220L
private const val LYRIC_FOCAL_ANCHOR_RATIO = 0.28f
private const val AUTO_SCROLL_RESUME_DELAY_MS = 4000L
private const val SKELETON_DISSOLVE_CUTOFF = 0.56f
private val COMPACT_HEADER_HEIGHT_DP = 68.dp

internal enum class LyricDisplayState {
    LOADING,
    CONTENT,
    EMPTY,
    ERROR
}

internal data class LyricRevealTransform(
    val alpha: Float,
    val translationYMultiplier: Float,
    val scale: Float
)

internal fun resolveLyricDisplayState(
    activeMusicId: Long?,
    activeParsedLyric: ParsedLyric?,
    lyricError: Throwable?
): LyricDisplayState = when {
    activeMusicId == null -> LyricDisplayState.EMPTY
    activeParsedLyric != null -> {
        if (activeParsedLyric.lines.isEmpty()) LyricDisplayState.EMPTY else LyricDisplayState.CONTENT
    }
    lyricError != null -> LyricDisplayState.ERROR
    else -> LyricDisplayState.LOADING
}

internal fun shouldPlayLyricSkeletonAnimation(
    isLoading: Boolean,
    isLyricActive: Boolean,
    isLifecycleStarted: Boolean
): Boolean = isLoading && isLyricActive && isLifecycleStarted

internal fun computeLyricLineRevealTransform(
    progress: Float,
    lineIndex: Int,
    anchorIndex: Int
): LyricRevealTransform {
    val p = progress.coerceIn(0f, 1f)
    if (p >= 1f) {
        return LyricRevealTransform(alpha = 1f, translationYMultiplier = 0f, scale = 1f)
    }
    val distance = abs(lineIndex - anchorIndex).coerceIn(0, 6)
    val start = 0.08f + distance * 0.055f
    val span = 0.50f
    val rawT = ((p - start) / span).coerceIn(0f, 1f)
    val inv = 1f - rawT
    val eased = 1f - inv * inv * inv
    return LyricRevealTransform(
        alpha = eased,
        translationYMultiplier = 1f - eased,
        scale = 0.94f + 0.06f * eased
    )
}

internal fun computeSkeletonDissolveAlpha(progress: Float, rowIndex: Int): Float {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0f) return 1f
    val rowDelay = rowIndex.coerceIn(0, 7) * 0.025f
    val localT = ((p - rowDelay) / 0.36f).coerceIn(0f, 1f)
    return 1f - localT
}

internal data class InitialLyricScrollTarget(
    val itemIndex: Int,
    val scrollOffsetPx: Int
)

internal fun resolveInitialLyricScrollTarget(
    currentIndex: Int,
    focalAnchorPx: Int
): InitialLyricScrollTarget {
    return if (currentIndex >= 0) {
        InitialLyricScrollTarget(
            itemIndex = currentIndex + 1,
            scrollOffsetPx = -focalAnchorPx.coerceAtLeast(0)
        )
    } else {
        InitialLyricScrollTarget(itemIndex = 0, scrollOffsetPx = 0)
    }
}

internal fun resolveLyricViewportHeightPx(
    measuredViewportHeightPx: Int,
    settledViewportHeightPx: Int
): Int = maxOf(measuredViewportHeightPx, settledViewportHeightPx).coerceAtLeast(0)

private fun findLyricIndex(lines: List<LrcLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var low = 0
    var high = lines.lastIndex
    var result = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (lines[mid].time <= positionMs) {
            result = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return result
}

private fun computeLineProgress(
    positionMs: Long,
    lineStartMs: Long,
    nextLineStartMs: Long?,
    text: String
): Float {
    if (positionMs <= lineStartMs) return 0f
    val rawDuration = if (nextLineStartMs != null && nextLineStartMs > lineStartMs) {
        nextLineStartMs - lineStartMs
    } else {
        4000L
    }
    val charCount = text.count { !it.isWhitespace() }.coerceAtLeast(1)
    val maxNaturalDuration = (charCount * 380L + 600L).coerceIn(1200L, 8000L)
    val effectiveDuration = minOf(rawDuration, maxNaturalDuration).coerceAtLeast(350L)
    val sweepDuration = (effectiveDuration * 0.92f).coerceAtLeast(280f)
    return ((positionMs - lineStartMs) / sweepDuration).coerceIn(0f, 1f)
}

@Composable
fun Lyric(
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    artistModifier: Modifier = Modifier,
    mediaMetadata: MediaMetadata,
    onBackPressed: () -> Unit = {},
    showBackground: Boolean = true,
    isLyricActive: Boolean = true,
    lyricViewModel: LyricViewModel = hiltViewModel()
) {
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val playbackState = playerState?.playbackState
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId
    val activeMusicId = currentMediaId?.toLongOrNull()
    val parsedLyric by lyricViewModel.parsedLyric.collectAsStateWithLifecycle()
    val lyricError by lyricViewModel.lyricError.collectAsStateWithLifecycle()
    val errorMusicId by lyricViewModel.errorMusicId.collectAsStateWithLifecycle()
    val activeParsedLyric = when {
        activeMusicId == null -> null
        parsedLyric?.musicId == activeMusicId -> parsedLyric
        else -> lyricViewModel.peekCachedParsedLyric(activeMusicId)
    }
    val activeLyricError = if (activeMusicId != null && errorMusicId == activeMusicId) {
        lyricError
    } else {
        null
    }
    val displayState = resolveLyricDisplayState(
        activeMusicId = activeMusicId,
        activeParsedLyric = activeParsedLyric,
        lyricError = activeLyricError
    )
    val lrcLines = activeParsedLyric?.lines
    val translations = activeParsedLyric?.translations
    val playbackPositionState = remember {
        mutableLongStateOf(playerState?.player?.currentPosition ?: 0L)
    }
    var currentIndex by remember(activeMusicId, lrcLines) {
        val pos = playerState?.player?.currentPosition ?: 0L
        playbackPositionState.longValue = pos
        mutableIntStateOf(
            lrcLines?.let { findLyricIndex(it, pos + LYRIC_SCROLL_LOOKAHEAD_MS) } ?: -1
        )
    }
    var autoScrollEnabled by remember { mutableStateOf(true) }
    var hasSnappedInitial by remember(activeMusicId, lrcLines) { mutableStateOf(false) }
    val hadInitialLyrics = remember(activeMusicId) { displayState == LyricDisplayState.CONTENT }
    val revealAnimatable = remember(activeMusicId) {
        Animatable(if (hadInitialLyrics) 1f else 0f)
    }
    val isSkeletonRevealFinished by remember(activeMusicId) {
        derivedStateOf { revealAnimatable.value >= SKELETON_DISSOLVE_CUTOFF }
    }
    val revealAnchorIndex = remember(activeMusicId, lrcLines) {
        currentIndex.coerceAtLeast(0)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleStarted by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            isLifecycleStarted = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val artworkColors = LocalArtworkColors.current
    val contentColors = rememberPlayerContentColors(artworkColors)

    LaunchedEffect(activeMusicId, displayState, hasSnappedInitial) {
        if (!hadInitialLyrics && displayState == LyricDisplayState.CONTENT && hasSnappedInitial && revealAnimatable.value < 1f) {
            revealAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 540, easing = LinearEasing)
            )
        }
    }

    LaunchedEffect(activeMusicId, playbackState, isPlaying, lrcLines) {
        val lines = lrcLines ?: return@LaunchedEffect
        if (lines.isEmpty() || playbackState != STATE_READY) return@LaunchedEffect
        if (isPlaying) {
            while (isActive) {
                withFrameMillis {
                    val pos = playerState?.player?.currentPosition ?: 0L
                    playbackPositionState.longValue = pos
                    val index = findLyricIndex(lines, pos + LYRIC_SCROLL_LOOKAHEAD_MS)
                    if (index != currentIndex) {
                        currentIndex = index
                    }
                }
            }
        } else {
            while (isActive) {
                val pos = playerState?.player?.currentPosition ?: 0L
                playbackPositionState.longValue = pos
                val index = findLyricIndex(lines, pos + LYRIC_SCROLL_LOOKAHEAD_MS)
                if (index != currentIndex) {
                    currentIndex = index
                }
                delay(250L)
            }
        }
    }

    LaunchedEffect(currentMediaId) {
        val musicId = currentMediaId?.toLongOrNull()
        if (musicId != null) {
            lyricViewModel.fetchLyric(musicId)
        } else {
            lyricViewModel.clearLyric()
        }
        autoScrollEnabled = true
    }

    KeepScreenOn()

    val lyricContent: @Composable () -> Unit = {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val density = LocalDensity.current
            val compactHeaderHeightPx = with(density) { COMPACT_HEADER_HEIGHT_DP.toPx() }
            val settledViewportHeightPx = (constraints.maxHeight.toFloat() - compactHeaderHeightPx)
                .roundToInt()
                .coerceAtLeast(1)
            val currentSettledViewportHeightPx by rememberUpdatedState(settledViewportHeightPx)
            val settledViewportHeightDp = with(density) { settledViewportHeightPx.toDp() }
            val skeletonSweepWidthPx = (constraints.maxWidth.toFloat() - with(density) { 56.dp.toPx() })
                .coerceAtLeast(1f)

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = ChevronDown,
                        contentDescription = "返回",
                        tint = contentColors.primaryContent
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
                    val context = LocalContext.current
                    val artworkUri = mediaMetadata.artworkUri
                    val imageRequest = remember(context, artworkUri) {
                        val key = artworkUri?.toString()
                        ImageRequest.Builder(context)
                            .data(artworkUri)
                            .size(ImagePreloadUtil.ThumbnailRequestSizePx)
                            .apply {
                                if (key != null) {
                                    memoryCacheKey(key)
                                    diskCacheKey(key)
                                    placeholderMemoryCacheKey(key)
                                }
                            }
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = imageModifier
                            .size(44.dp)
                            .clip(AdaptiveArtworkShape)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mediaMetadata.title?.toString() ?: "",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColors.primaryContent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .then(titleModifier)
                                .basicMarquee()
                        )
                        Text(
                            text = mediaMetadata.artist?.toString() ?: "",
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColors.primaryContent.copy(alpha = 0.7f),
                            modifier = Modifier
                                .then(artistModifier)
                                .basicMarquee()
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (displayState) {
                    LyricDisplayState.CONTENT -> {
                        val lines = lrcLines.orEmpty()
                        val listState = key(activeMusicId, lines) {
                            rememberLazyListState(
                                initialFirstVisibleItemIndex = (currentIndex - 1).coerceAtLeast(0)
                            )
                        }
                        var programmaticScroll by remember { mutableStateOf(false) }

                        LaunchedEffect(listState) {
                            snapshotFlow { listState.isScrollInProgress }
                                .collect { inProgress ->
                                    if (!programmaticScroll && inProgress) {
                                        autoScrollEnabled = false
                                    }
                                }
                        }

                        LaunchedEffect(autoScrollEnabled, listState.isScrollInProgress) {
                            if (!autoScrollEnabled && !listState.isScrollInProgress) {
                                delay(AUTO_SCROLL_RESUME_DELAY_MS)
                                autoScrollEnabled = true
                            }
                        }

                        val bottomSpacerHeight = (settledViewportHeightDp * 0.65f).coerceAtLeast(240.dp)

                        LaunchedEffect(activeMusicId, lines, currentIndex, autoScrollEnabled) {
                            if (lines.isEmpty()) {
                                hasSnappedInitial = true
                                return@LaunchedEffect
                            }

                            val measuredViewportHeight = listState.layoutInfo.viewportSize.height.takeIf { it > 0 }
                                ?: snapshotFlow { listState.layoutInfo.viewportSize.height }.first { it > 0 }
                            val resolvedViewportHeight = resolveLyricViewportHeightPx(
                                measuredViewportHeightPx = measuredViewportHeight,
                                settledViewportHeightPx = currentSettledViewportHeightPx
                            )
                            val focalAnchorPx = (resolvedViewportHeight * LYRIC_FOCAL_ANCHOR_RATIO).toInt()

                            if (!hasSnappedInitial) {
                                programmaticScroll = true
                                try {
                                    val target = resolveInitialLyricScrollTarget(
                                        currentIndex = currentIndex,
                                        focalAnchorPx = focalAnchorPx
                                    )
                                    listState.scrollToItem(
                                        index = target.itemIndex,
                                        scrollOffset = target.scrollOffsetPx
                                    )
                                    hasSnappedInitial = true
                                } finally {
                                    programmaticScroll = false
                                }
                                return@LaunchedEffect
                            }

                            if (!autoScrollEnabled || currentIndex < 0) return@LaunchedEffect

                            val targetItemIndex = currentIndex + 1
                            programmaticScroll = true
                            try {
                                val itemInfo = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.index == targetItemIndex }
                                if (itemInfo == null) {
                                    listState.scrollToItem(
                                        index = targetItemIndex,
                                        scrollOffset = -focalAnchorPx
                                    )
                                } else {
                                    val delta = itemInfo.offset - focalAnchorPx.toFloat()
                                    val atTopBoundary = delta < 0f && !listState.canScrollBackward
                                    val atBottomBoundary = delta > 0f && !listState.canScrollForward
                                    if (!atTopBoundary && !atBottomBoundary && abs(delta) > 1f) {
                                        listState.animateScrollBy(
                                            delta,
                                            animationSpec = tween(
                                                durationMillis = 480,
                                                easing = EaseInOutCubic
                                            )
                                        )
                                    }
                                }
                            } finally {
                                programmaticScroll = false
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    alpha = if (hasSnappedInitial) 1f else 0f
                                }
                                .lyricVerticalEdgeFade(),
                            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            item(key = "lyric_top_spacer") {
                                Spacer(Modifier.height(24.dp))
                            }

                            items(
                                count = lines.size,
                                key = { "${lines[it].time}_$it" }
                            ) { index ->
                                val isCurrent = index == currentIndex
                                val line = lines[index]
                                val currentText = line.text.isNotEmpty()
                                val nextLineTime = lines.getOrNull(index + 1)?.time

                                if (currentText) {
                                    val translation = translations?.getOrNull(index)

                                    val targetAlpha = when {
                                        isCurrent -> 1f
                                        !autoScrollEnabled -> 0.62f
                                        currentIndex >= 0 && abs(index - currentIndex) == 1 -> 0.52f
                                        else -> 0.36f
                                    }
                                    val animatedAlphaState = animateFloatAsState(
                                        targetValue = targetAlpha,
                                        animationSpec = tween(durationMillis = 320),
                                        label = "lyric_line_alpha"
                                    )
                                    val animatedScaleState = animateFloatAsState(
                                        targetValue = if (isCurrent) 1f else 0.86f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        label = "lyric_line_scale"
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(JetMeloShapes.medium)
                                            .clickable(role = Role.Button) {
                                                mediaController?.seekTo(line.time)
                                                playbackPositionState.longValue = line.time
                                                currentIndex = index
                                                autoScrollEnabled = true
                                            }
                                            .padding(vertical = 14.dp, horizontal = 4.dp)
                                            .graphicsLayer {
                                                val reveal = computeLyricLineRevealTransform(
                                                    progress = revealAnimatable.value,
                                                    lineIndex = index,
                                                    anchorIndex = revealAnchorIndex
                                                )
                                                val s = animatedScaleState.value * reveal.scale
                                                scaleX = s
                                                scaleY = s
                                                transformOrigin = TransformOrigin(0f, 0.5f)
                                                translationY = 14.dp.toPx() * reveal.translationYMultiplier
                                                alpha = animatedAlphaState.value * reveal.alpha
                                            }
                                    ) {
                                        ProgressiveLyricText(
                                            text = line.text,
                                            words = line.words,
                                            isCurrent = isCurrent,
                                            lineStartMs = line.time,
                                            nextLineStartMs = nextLineTime,
                                            positionProvider = { playbackPositionState.longValue },
                                            activeLitColor = contentColors.primaryContent,
                                            activeUnlitColor = contentColors.primaryContent.copy(alpha = 0.38f),
                                            inactiveColor = contentColors.primaryContent.copy(alpha = 0.85f)
                                        )
                                        if (!translation.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = translation,
                                                color = if (isCurrent) {
                                                    contentColors.primaryContent.copy(alpha = 0.82f)
                                                } else {
                                                    contentColors.primaryContent.copy(alpha = 0.65f)
                                                },
                                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                                fontSize = 17.sp,
                                                lineHeight = 1.25.em
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .graphicsLayer {
                                                val reveal = computeLyricLineRevealTransform(
                                                    progress = revealAnimatable.value,
                                                    lineIndex = index,
                                                    anchorIndex = revealAnchorIndex
                                                )
                                                scaleX = reveal.scale
                                                scaleY = reveal.scale
                                                transformOrigin = TransformOrigin(0f, 0.5f)
                                                translationY = 14.dp.toPx() * reveal.translationYMultiplier
                                                alpha = reveal.alpha
                                            }
                                    ) {
                                        if (isCurrent) {
                                            nextLineTime?.let { time ->
                                                ThreeDotsAnimation(
                                                    times = line.time to time,
                                                    dotColor = contentColors.accent
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "· · ·",
                                                color = contentColors.primaryContent.copy(alpha = 0.35f),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            item(key = "lyric_bottom_spacer") {
                                Spacer(Modifier.height(bottomSpacerHeight))
                            }
                        }
                    }

                    LyricDisplayState.EMPTY -> {
                        val emptyAlpha = remember(activeMusicId) { Animatable(0f) }
                        LaunchedEffect(activeMusicId) {
                            emptyAlpha.animateTo(1f, animationSpec = tween(durationMillis = 220))
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp)
                                .graphicsLayer { alpha = emptyAlpha.value }
                        ) {
                            Text(
                                text = stringResource(R.string.no_lyrics),
                                style = MaterialTheme.typography.titleMedium,
                                color = contentColors.secondaryContent
                            )
                        }
                    }

                    LyricDisplayState.ERROR -> {
                        val errorAlpha = remember(activeMusicId) { Animatable(0f) }
                        LaunchedEffect(activeMusicId) {
                            errorAlpha.animateTo(1f, animationSpec = tween(durationMillis = 220))
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp)
                                .graphicsLayer { alpha = errorAlpha.value }
                        ) {
                            Text(
                                text = stringResource(R.string.lyrics_load_failed),
                                style = MaterialTheme.typography.titleMedium,
                                color = contentColors.secondaryContent
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    activeMusicId?.let { lyricViewModel.fetchLyric(it) }
                                },
                                shape = JetMeloShapes.full,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = contentColors.activeItemContainer,
                                    contentColor = contentColors.primaryContent
                                )
                            ) {
                                Text(text = stringResource(R.string.retry))
                            }
                        }
                    }

                    LyricDisplayState.LOADING -> Unit
                }

                val showSkeletonOverlay = displayState == LyricDisplayState.LOADING ||
                    (displayState == LyricDisplayState.CONTENT && !hadInitialLyrics && (!hasSnappedInitial || !isSkeletonRevealFinished))

                if (showSkeletonOverlay) {
                    val shimmerEnabled = shouldPlayLyricSkeletonAnimation(
                        isLoading = displayState == LyricDisplayState.LOADING,
                        isLyricActive = isLyricActive,
                        isLifecycleStarted = isLifecycleStarted
                    )
                    LyricSkeleton(
                        settledViewportHeightDp = settledViewportHeightDp,
                        sweepWidthPx = skeletonSweepWidthPx,
                        contentColors = contentColors,
                        shimmerEnabled = shimmerEnabled,
                        dissolveProgressProvider = {
                            if (displayState == LyricDisplayState.CONTENT && hasSnappedInitial) {
                                revealAnimatable.value
                            } else {
                                0f
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            }
        }
    }

    if (showBackground) {
        ImmersiveBackground(
            modifier = modifier.fillMaxSize(),
            artworkUri = mediaMetadata.artworkUri,
            enableAmbientDrift = true
        ) {
            lyricContent()
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            lyricContent()
        }
    }
}

private fun computeYrcLineSweep(
    positionMs: Long,
    words: List<YrcWord>,
    text: String,
    layout: TextLayoutResult,
    lineCount: Int
): YrcLineSweep {
    if (text.isEmpty() || words.isEmpty() || lineCount <= 0) {
        return YrcLineSweep(mode = YrcSweepMode.UNLIT)
    }
    if (positionMs < words.first().startMs) {
        return YrcLineSweep(mode = YrcSweepMode.UNLIT)
    }

    var currentWordIdx = -1
    for (idx in words.indices) {
        if (words[idx].startMs <= positionMs) {
            currentWordIdx = idx
        } else {
            break
        }
    }
    if (currentWordIdx < 0) {
        return YrcLineSweep(mode = YrcSweepMode.UNLIT)
    }

    val word = words[currentWordIdx]
    val wordFraction = if (word.durationMs > 0L) {
        ((positionMs - word.startMs).toFloat() / word.durationMs).coerceIn(0f, 1f)
    } else {
        1f
    }

    if (currentWordIdx == words.lastIndex && wordFraction >= 1f) {
        return YrcLineSweep(mode = YrcSweepMode.FULL_LIT)
    }

    val safeStart = word.startIndex.coerceIn(0, text.length - 1)
    val safeEnd = word.endIndex.coerceIn(safeStart + 1, text.length)
    val targetLine: Int
    val frontierX: Float
    if (wordFraction >= 1f) {
        val charIdx = safeEnd - 1
        targetLine = layout.getLineForOffset(charIdx).coerceIn(0, lineCount - 1)
        frontierX = layout.getBoundingBox(charIdx).right
    } else {
        val exactChar = safeStart + (safeEnd - safeStart) * wordFraction
        val charIdx = exactChar.toInt().coerceIn(safeStart, safeEnd - 1)
        val charFrac = (exactChar - charIdx).coerceIn(0f, 1f)
        targetLine = layout.getLineForOffset(charIdx).coerceIn(0, lineCount - 1)
        val box = layout.getBoundingBox(charIdx)
        frontierX = box.left + (box.right - box.left) * charFrac
    }
    return YrcLineSweep(
        mode = YrcSweepMode.PARTIAL,
        targetLine = targetLine,
        frontierX = frontierX
    )
}

private enum class YrcSweepMode {
    UNLIT,
    PARTIAL,
    FULL_LIT
}

private class YrcLineSweep(
    val mode: YrcSweepMode,
    val targetLine: Int = 0,
    val frontierX: Float = 0f
)

@Composable
private fun ProgressiveLyricText(
    text: String,
    words: List<YrcWord> = emptyList(),
    isCurrent: Boolean,
    lineStartMs: Long,
    nextLineStartMs: Long?,
    positionProvider: () -> Long,
    activeLitColor: Color,
    activeUnlitColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    var textLayoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        color = if (isCurrent) activeLitColor else inactiveColor,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 1.28.em,
        onTextLayout = { textLayoutResult = it },
        modifier = modifier.then(
            if (isCurrent) {
                Modifier
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        val layout = textLayoutResult ?: return@drawWithContent
                        val lineCount = layout.lineCount
                        if (lineCount <= 0 || text.isEmpty()) return@drawWithContent

                        val positionMs = positionProvider()
                        if (words.isNotEmpty()) {
                            val sweep = computeYrcLineSweep(
                                positionMs = positionMs,
                                words = words,
                                text = text,
                                layout = layout,
                                lineCount = lineCount
                            )
                            val featherPx = 18.dp.toPx()
                            for (i in 0 until lineCount) {
                                val rectTop = if (i == 0) 0f else layout.getLineTop(i)
                                val rectBottom = if (i == lineCount - 1) size.height else layout.getLineBottom(i)
                                val rectHeight = (rectBottom - rectTop).coerceAtLeast(0f)
                                if (rectHeight <= 0f) continue

                                when (sweep.mode) {
                                    YrcSweepMode.UNLIT -> {
                                        drawRect(
                                            color = activeUnlitColor,
                                            topLeft = Offset(0f, rectTop),
                                            size = Size(size.width, rectHeight),
                                            blendMode = BlendMode.SrcIn
                                        )
                                    }
                                    YrcSweepMode.FULL_LIT -> {
                                        drawRect(
                                            color = activeLitColor,
                                            topLeft = Offset(0f, rectTop),
                                            size = Size(size.width, rectHeight),
                                            blendMode = BlendMode.SrcIn
                                        )
                                    }
                                    YrcSweepMode.PARTIAL -> {
                                        when {
                                            i < sweep.targetLine -> {
                                                drawRect(
                                                    color = activeLitColor,
                                                    topLeft = Offset(0f, rectTop),
                                                    size = Size(size.width, rectHeight),
                                                    blendMode = BlendMode.SrcIn
                                                )
                                            }
                                            i > sweep.targetLine -> {
                                                drawRect(
                                                    color = activeUnlitColor,
                                                    topLeft = Offset(0f, rectTop),
                                                    size = Size(size.width, rectHeight),
                                                    blendMode = BlendMode.SrcIn
                                                )
                                            }
                                            else -> {
                                                val startX = sweep.frontierX - featherPx * 0.75f
                                                val endX = sweep.frontierX + featherPx * 0.25f
                                                drawRect(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(activeLitColor, activeUnlitColor),
                                                        startX = startX,
                                                        endX = endX
                                                    ),
                                                    topLeft = Offset(0f, rectTop),
                                                    size = Size(size.width, rectHeight),
                                                    blendMode = BlendMode.SrcIn
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            return@drawWithContent
                        }

                        val progress = computeLineProgress(
                            positionMs = positionMs,
                            lineStartMs = lineStartMs,
                            nextLineStartMs = nextLineStartMs,
                            text = text
                        )

                        var totalWidth = 0f
                        for (i in 0 until lineCount) {
                            totalWidth += (layout.getLineRight(i) - layout.getLineLeft(i)).coerceAtLeast(0f)
                        }
                        if (totalWidth <= 0f) return@drawWithContent

                        val filledDistance = totalWidth * progress
                        val featherPx = 28.dp.toPx()
                        var accumulated = 0f

                        for (i in 0 until lineCount) {
                            val lineLeft = layout.getLineLeft(i)
                            val lineRight = layout.getLineRight(i)
                            val lineWidth = (lineRight - lineLeft).coerceAtLeast(0f)
                            val rectTop = if (i == 0) 0f else layout.getLineTop(i)
                            val rectBottom = if (i == lineCount - 1) size.height else layout.getLineBottom(i)
                            val rectHeight = (rectBottom - rectTop).coerceAtLeast(0f)

                            if (rectHeight > 0f) {
                                when {
                                    filledDistance >= accumulated + lineWidth -> {
                                        drawRect(
                                            color = activeLitColor,
                                            topLeft = Offset(0f, rectTop),
                                            size = Size(size.width, rectHeight),
                                            blendMode = BlendMode.SrcIn
                                        )
                                    }
                                    filledDistance <= accumulated -> {
                                        drawRect(
                                            color = activeUnlitColor,
                                            topLeft = Offset(0f, rectTop),
                                            size = Size(size.width, rectHeight),
                                            blendMode = BlendMode.SrcIn
                                        )
                                    }
                                    else -> {
                                        val localX = lineLeft + (filledDistance - accumulated)
                                        val startX = localX - featherPx * 0.8f
                                        val endX = localX + featherPx * 0.2f
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(activeLitColor, activeUnlitColor),
                                                startX = startX,
                                                endX = endX
                                            ),
                                            topLeft = Offset(0f, rectTop),
                                            size = Size(size.width, rectHeight),
                                            blendMode = BlendMode.SrcIn
                                        )
                                    }
                                }
                            }
                            accumulated += lineWidth
                        }
                    }
            } else {
                Modifier
            }
        )
    )
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
    val animDur = minOf((duration / 2).toInt(), 2000).coerceAtLeast(800)
    val transition = rememberInfiniteTransition()
    val scaleState = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = animDur
                1.3f at 200 using EaseInOutCubic
                1f at 400 using EaseInOutCubic
            },
            repeatMode = RepeatMode.Restart
        ),
    )

    val scale2State = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = animDur
                1.3f at 400 using EaseInOutCubic
                1f at 600 using EaseInOutCubic
            },
            repeatMode = RepeatMode.Restart
        ),
    )

    val scale3State = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = animDur
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
                radius = (size.minDimension / 2) * scaleState.value,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
        Canvas(modifier = Modifier.size(dotSize)) {
            drawCircle(
                color = dotColor,
                radius = (size.minDimension / 2) * scale2State.value,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
        Canvas(modifier = Modifier.size(dotSize)) {
            drawCircle(
                color = dotColor,
                radius = (size.minDimension / 2) * scale3State.value,
                center = Offset(size.width / 2, size.height / 2)
            )
        }
    }
}

private fun Modifier.lyricVerticalEdgeFade(): Modifier = drawWithContent {
    drawContent()
    val topFadePx = 28.dp.toPx().coerceAtMost(size.height * 0.15f)
    val bottomFadePx = 56.dp.toPx().coerceAtMost(size.height * 0.25f)
    if (topFadePx > 0f) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = topFadePx
            ),
            size = Size(size.width, topFadePx),
            blendMode = BlendMode.DstIn
        )
    }
    if (bottomFadePx > 0f) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = size.height - bottomFadePx,
                endY = size.height
            ),
            topLeft = Offset(0f, size.height - bottomFadePx),
            size = Size(size.width, bottomFadePx),
            blendMode = BlendMode.DstIn
        )
    }
}

private data class LyricSkeletonLineSpec(
    val widthFraction: Float,
    val translationFraction: Float = 0f,
    val isFocal: Boolean = false
)

private val LyricSkeletonLines = listOf(
    LyricSkeletonLineSpec(widthFraction = 0.56f, translationFraction = 0.34f),
    LyricSkeletonLineSpec(widthFraction = 0.72f),
    LyricSkeletonLineSpec(widthFraction = 0.84f, translationFraction = 0.48f, isFocal = true),
    LyricSkeletonLineSpec(widthFraction = 0.64f),
    LyricSkeletonLineSpec(widthFraction = 0.48f, translationFraction = 0.30f),
    LyricSkeletonLineSpec(widthFraction = 0.78f),
    LyricSkeletonLineSpec(widthFraction = 0.58f),
    LyricSkeletonLineSpec(widthFraction = 0.42f)
)

private fun Modifier.skeletonShimmerFill(
    baseColor: Color,
    highlightColor: Color,
    sweepWidthPx: Float,
    shimmerProgressProvider: () -> Float?
): Modifier = drawBehind {
    val progress = shimmerProgressProvider()
    if (progress == null) {
        drawRect(color = baseColor)
    } else {
        val bandWidth = (sweepWidthPx * 0.48f).coerceAtLeast(220f)
        val startX = -bandWidth + (sweepWidthPx + bandWidth * 2f) * progress
        val start = Offset(x = startX, y = startX * 0.25f)
        val end = Offset(x = startX + bandWidth, y = (startX + bandWidth) * 0.25f)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(baseColor, highlightColor, baseColor),
                start = start,
                end = end
            )
        )
    }
}

@Composable
private fun LyricSkeleton(
    settledViewportHeightDp: Dp,
    sweepWidthPx: Float,
    contentColors: PlayerContentColors,
    shimmerEnabled: Boolean,
    dissolveProgressProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    val normalBase = contentColors.primaryContent.copy(alpha = 0.09f)
    val normalHighlight = contentColors.primaryContent.copy(alpha = 0.22f)
    val focalBase = contentColors.primaryContent.copy(alpha = 0.16f)
    val focalHighlight = contentColors.primaryContent.copy(alpha = 0.34f)

    val shimmerProgressState = if (shimmerEnabled) {
        val transition = rememberInfiniteTransition(label = "lyric_skeleton_shimmer")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1350, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "lyric_skeleton_shimmer_progress"
        )
    } else {
        null
    }
    val shimmerProgressProvider: () -> Float? = { shimmerProgressState?.value }

    val topOffset = ((settledViewportHeightDp * LYRIC_FOCAL_ANCHOR_RATIO) - 152.dp).coerceAtLeast(24.dp)

    Column(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .lyricVerticalEdgeFade(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(topOffset))

        LyricSkeletonLines.forEachIndexed { index, spec ->
            val baseColor = if (spec.isFocal) focalBase else normalBase
            val highlightColor = if (spec.isFocal) focalHighlight else normalHighlight
            val baseScale = if (spec.isFocal) 1f else 0.86f
            val baseAlpha = when {
                spec.isFocal -> 1f
                abs(index - 2) == 1 -> 0.72f
                else -> 0.52f
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .graphicsLayer {
                        val rowAlpha = computeSkeletonDissolveAlpha(dissolveProgressProvider(), index)
                        val scale = baseScale * (0.94f + 0.06f * rowAlpha)
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        translationY = -10.dp.toPx() * (1f - rowAlpha)
                        alpha = baseAlpha * rowAlpha
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(spec.widthFraction)
                        .height(26.dp)
                        .clip(JetMeloShapes.full)
                        .skeletonShimmerFill(
                            baseColor = baseColor,
                            highlightColor = highlightColor,
                            sweepWidthPx = sweepWidthPx,
                            shimmerProgressProvider = shimmerProgressProvider
                        )
                )
                if (spec.translationFraction > 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(spec.translationFraction)
                            .height(14.dp)
                            .clip(JetMeloShapes.full)
                            .skeletonShimmerFill(
                                baseColor = baseColor,
                                highlightColor = highlightColor,
                                sweepWidthPx = sweepWidthPx,
                                shimmerProgressProvider = shimmerProgressProvider
                            )
                    )
                }
            }
        }
    }
}
