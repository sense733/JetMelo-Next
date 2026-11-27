package com.rcmiku.music.ui.screen

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.rcmiku.music.constants.DURATION
import com.rcmiku.music.constants.DURATION_ENTER
import com.rcmiku.music.constants.DURATION_EXIT_SHORT
import com.rcmiku.music.constants.EmphasizedAccelerateEasing
import com.rcmiku.music.constants.EmphasizedDecelerateEasing
import com.rcmiku.music.constants.EmphasizedEasing
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import com.kyant.capsule.ContinuousRoundedRectangle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaMetadata
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size as CoilSize
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.constants.MiniPlayerHeight
import com.rcmiku.music.ui.components.Lyric
import com.rcmiku.music.ui.components.MiniPlayerProgressBar
import com.rcmiku.music.ui.components.Player
import com.rcmiku.music.ui.components.PlayerQueue
import com.rcmiku.music.ui.design.ImmersiveBackground
import com.rcmiku.music.ui.design.LocalArtworkColors
import com.rcmiku.music.ui.icons.Pause
import com.rcmiku.music.ui.icons.PlayArrow
import com.rcmiku.music.ui.icons.SkipNext
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.ui.theme.MiniPlayerProgressColor
import com.rcmiku.music.ui.theme.rememberDeviceCornerRadius
import com.rcmiku.music.viewModel.LyricViewModel
import kotlin.math.roundToInt

const val FULL_PLAYER = 0
const val PLAY_QUEUE = 1
const val MINI_PLAYER = 2
const val LYRIC_VIEW = 3

private const val ProgressEps = 1e-3f

private fun lerpRect(start: Rect, stop: Rect, fraction: Float): Rect =
    Rect(
        androidx.compose.ui.util.lerp(start.left, stop.left, fraction),
        androidx.compose.ui.util.lerp(start.top, stop.top, fraction),
        androidx.compose.ui.util.lerp(start.right, stop.right, fraction),
        androidx.compose.ui.util.lerp(start.bottom, stop.bottom, fraction)
    )

@OptIn(ExperimentalSharedTransitionApi::class)
private val AlbumArtBoundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerTransform(
    onClick: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    mediaMetadata: MediaMetadata,
    navController: NavHostController,
    isExpanded: Boolean = false,
    transitionProgressProvider: () -> Float = { 0f },
    dockedBottomPaddingProvider: () -> Dp = { 0.dp },
) {
    val isCollapsed by remember(transitionProgressProvider) {
        derivedStateOf { transitionProgressProvider().coerceIn(0f, 1f) <= ProgressEps }
    }
    val isFull by remember(transitionProgressProvider) {
        derivedStateOf { transitionProgressProvider().coerceIn(0f, 1f) >= 1f - ProgressEps }
    }
    val showMiniLayer by remember(transitionProgressProvider) {
        derivedStateOf { transitionProgressProvider().coerceIn(0f, 1f) < 0.32f }
    }

    var currentView by rememberSaveable {
        mutableIntStateOf(FULL_PLAYER)
    }

    var lastViewSwitchTime by remember { mutableLongStateOf(0L) }
    val safeSwitchView: (Int) -> Unit = { target ->
        val now = SystemClock.uptimeMillis()
        if (now - lastViewSwitchTime > 400L && currentView != target) {
            lastViewSwitchTime = now
            currentView = target
        }
    }

    var isSheetOpen by remember { mutableStateOf(false) }
    var dismissSheetsRequested by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            currentView = FULL_PLAYER
        }
    }

    if (isExpanded || !isCollapsed) {
        BackHandler {
            if (isSheetOpen) {
                dismissSheetsRequested = true
            } else if (currentView != FULL_PLAYER) {
                safeSwitchView(FULL_PLAYER)
            } else {
                onBackPressed()
            }
        }
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val view = LocalView.current

    DisposableEffect(isDarkTheme) {
        val window = (view.context as? Activity)?.window
        if (window != null && !view.isInEditMode) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            insetsController.isAppearanceLightNavigationBars = !isDarkTheme
        }
        onDispose {
            if (window != null && !view.isInEditMode) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    val playerState = LocalPlayerState.current
    val effectiveArtworkUri = playerState?.currentMediaItem?.mediaMetadata?.artworkUri ?: mediaMetadata.artworkUri
    val currentMediaId = playerState?.currentMediaItem?.mediaId
    val mediaController = LocalPlayerController.current.controller
    val lyricViewModel: LyricViewModel = hiltViewModel()
    var fullArtworkRect by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(currentMediaId) {
        val musicId = currentMediaId?.toLongOrNull()
        if (musicId != null) {
            lyricViewModel.fetchLyric(musicId)
        } else {
            lyricViewModel.clearLyric()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val statusBarInsetPx = WindowInsets.statusBars.getTop(density).toFloat()
        val navBarInsetPx = WindowInsets.navigationBars.getBottom(density).toFloat()

        val miniHorizontalPaddingPx = with(density) { 12.dp.toPx() }
        val miniVerticalPaddingPx = with(density) { 8.dp.toPx() }
        val miniHeightPx = with(density) { MiniPlayerHeight.toPx() }
        val miniArtworkSizePx = with(density) { 44.dp.toPx() }
        val miniArtworkOffsetLeftPx = with(density) { 8.dp.toPx() }
        val miniArtworkOffsetTopPx = with(density) { 6.dp.toPx() }

        val miniLeftPx = miniHorizontalPaddingPx
        val miniRightPx = screenWidthPx - miniHorizontalPaddingPx
        val miniWidthPx = (miniRightPx - miniLeftPx).coerceAtLeast(0f)

        val fullRect = remember(screenWidthPx, screenHeightPx) {
            Rect(0f, 0f, screenWidthPx, screenHeightPx)
        }

        fun miniRectFor(dockedBottomPx: Float): Rect {
            val miniBottomPx = screenHeightPx - dockedBottomPx - miniVerticalPaddingPx
            val miniTopPx = miniBottomPx - miniHeightPx
            return Rect(miniLeftPx, miniTopPx, miniRightPx, miniBottomPx)
        }

        fun containerRectFor(p: Float, dockedBottomPx: Float): Rect =
            lerpRect(miniRectFor(dockedBottomPx), fullRect, p)

        fun miniArtworkRectFor(dockedBottomPx: Float): Rect {
            val miniBottomPx = screenHeightPx - dockedBottomPx - miniVerticalPaddingPx
            val miniTopPx = miniBottomPx - miniHeightPx
            val leftPx = miniLeftPx + miniArtworkOffsetLeftPx
            val topPx = miniTopPx + miniArtworkOffsetTopPx
            return Rect(leftPx, topPx, leftPx + miniArtworkSizePx, topPx + miniArtworkSizePx)
        }

        val defaultFullArtworkRect = remember(screenWidthPx, screenHeightPx, statusBarInsetPx, navBarInsetPx, density) {
            val horizontalPaddingPx = with(density) { 40.dp.toPx() }
            val availableWidthPx = screenWidthPx - horizontalPaddingPx
            val fullWidthPx = availableWidthPx * 0.88f
            val topBarHeightPx = with(density) { 56.dp.toPx() }
            val bottomControlsHeightPx = with(density) { 286.dp.toPx() }
            val boxVerticalPaddingPx = with(density) { 24.dp.toPx() }

            val parentHeightPx = (screenHeightPx - statusBarInsetPx - navBarInsetPx).coerceAtLeast(0f)
            val maxContentHeightPx = (parentHeightPx - topBarHeightPx - bottomControlsHeightPx - boxVerticalPaddingPx).coerceAtLeast(0f)
            val artSizePx = if (maxContentHeightPx in 1f..<fullWidthPx) maxContentHeightPx else fullWidthPx

            val fullLeftPx = (screenWidthPx - artSizePx) / 2f
            val leftoverPx = (parentHeightPx - (topBarHeightPx + bottomControlsHeightPx + boxVerticalPaddingPx + artSizePx)).coerceAtLeast(0f)
            val gapPx = leftoverPx / 2f
            val fullTopPx = statusBarInsetPx + topBarHeightPx + with(density) { 12.dp.toPx() } + gapPx
            Rect(fullLeftPx, fullTopPx, fullLeftPx + artSizePx, fullTopPx + artSizePx)
        }

        val deviceCornerRadius = rememberDeviceCornerRadius()
        val targetArtworkRect = fullArtworkRect ?: defaultFullArtworkRect
        val surfaceContainerHighColor = MaterialTheme.colorScheme.surfaceContainerHigh
        val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
        val scrimColor = MaterialTheme.colorScheme.scrim

        if (!isFull) {
            if (isCollapsed) {
                Box(
                    modifier = Modifier
                        .offset {
                            val dockedPx = with(density) { dockedBottomPaddingProvider().toPx() }
                            val rect = miniRectFor(dockedPx)
                            IntOffset(rect.left.roundToInt(), rect.top.roundToInt())
                        }
                        .size(
                            width = with(density) { miniWidthPx.toDp() },
                            height = MiniPlayerHeight
                        )
                        .shadow(
                            elevation = 6.dp,
                            shape = JetMeloShapes.medium,
                            clip = false,
                            ambientColor = scrimColor.copy(alpha = 0.35f),
                            spotColor = scrimColor.copy(alpha = 0.45f)
                        )
                        .background(
                            color = surfaceContainerHighColor,
                            shape = JetMeloShapes.medium
                        )
                        .border(
                            width = 1.dp,
                            color = outlineVariantColor.copy(alpha = 0.5f),
                            shape = JetMeloShapes.medium
                        )
                )
            } else if (showMiniLayer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val p = transitionProgressProvider().coerceIn(0f, 1f)
                            val borderAlpha = ((0.32f - p) / 0.32f).coerceIn(0f, 1f)
                            if (borderAlpha > 0f) {
                                val dockedPx = dockedBottomPaddingProvider().toPx()
                                val rect = containerRectFor(p, dockedPx)
                                val cornerRadiusPx = androidx.compose.ui.unit.lerp(16.dp, deviceCornerRadius, p)
                                    .coerceAtMost(36.dp)
                                    .toPx()
                                val cornerRadius = CornerRadius(cornerRadiusPx)
                                drawRoundRect(
                                    color = surfaceContainerHighColor.copy(alpha = borderAlpha),
                                    topLeft = rect.topLeft,
                                    size = rect.size,
                                    cornerRadius = cornerRadius
                                )
                                drawRoundRect(
                                    color = outlineVariantColor.copy(alpha = 0.5f * borderAlpha),
                                    topLeft = rect.topLeft,
                                    size = rect.size,
                                    cornerRadius = cornerRadius,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                )
            }
        }

        val clipShape = remember(
            screenWidthPx,
            screenHeightPx,
            deviceCornerRadius,
            density,
            transitionProgressProvider,
            dockedBottomPaddingProvider
        ) {
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
                    val p = transitionProgressProvider().coerceIn(0f, 1f)
                    val dockedPx = with(density) { dockedBottomPaddingProvider().toPx() }
                    val rect = containerRectFor(p, dockedPx)
                    val cornerRadius = androidx.compose.ui.unit.lerp(16.dp, deviceCornerRadius, p)
                        .coerceAtMost(36.dp)
                    return Outline.Rounded(
                        RoundRect(
                            rect = rect,
                            cornerRadius = CornerRadius(with(density) { cornerRadius.toPx() })
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val shouldClip = !isFull
                    clip = shouldClip
                    if (shouldClip) {
                        shape = clipShape
                    }
                }
        ) {
            if (!isCollapsed) {
                ImmersiveBackground(
                    modifier = Modifier.fillMaxSize(),
                    artworkUri = effectiveArtworkUri,
                    enableAmbientDrift = currentView == LYRIC_VIEW
                ) {}
            }

            if (showMiniLayer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val p = transitionProgressProvider().coerceIn(0f, 1f)
                            val surfaceAlpha = if (p <= ProgressEps) 1f else ((0.32f - p) / 0.32f).coerceIn(0f, 1f)
                            if (surfaceAlpha > 0f) {
                                drawRect(color = surfaceContainerHighColor.copy(alpha = surfaceAlpha))
                            }
                        }
                )
            }

            val coverKey = "player_active_cover"
            val titleKey = "player_active_title"
            val artistKey = "player_active_artist"

            if (!isCollapsed) {
                SharedTransitionLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = transitionProgressProvider().coerceIn(0f, 1f)
                            alpha = ((p - 0.28f) / 0.52f).coerceIn(0f, 1f)
                        }
                ) {
                    val subViewTransition = updateTransition(
                        targetState = currentView,
                        label = "player_subview_shared_transition"
                    )

                    val artworkElevationState = subViewTransition.animateDp(
                        transitionSpec = {
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        },
                        label = "player_artwork_elevation"
                    ) { view ->
                        if (view == FULL_PLAYER) 16.dp else 0.dp
                    }

                    val queueIndicatorAlphaState = subViewTransition.animateFloat(
                        transitionSpec = {
                            tween(
                                durationMillis = DURATION_ENTER,
                                easing = EmphasizedDecelerateEasing
                            )
                        },
                        label = "player_queue_indicator_alpha"
                    ) { view ->
                        if (view == PLAY_QUEUE) 1f else 0f
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        subViewTransition.AnimatedContent(
                            transitionSpec = {
                                if (targetState == LYRIC_VIEW) {
                                    fadeIn(
                                        animationSpec = tween(
                                            delayMillis = 30,
                                            durationMillis = 220,
                                            easing = EmphasizedDecelerateEasing
                                        )
                                    ) togetherWith fadeOut(
                                        animationSpec = tween(
                                            durationMillis = 90,
                                            easing = EmphasizedDecelerateEasing
                                        )
                                    )
                                } else {
                                    fadeIn(
                                        animationSpec = tween(
                                            delayMillis = DURATION_EXIT_SHORT,
                                            durationMillis = DURATION_ENTER,
                                            easing = EmphasizedDecelerateEasing
                                        )
                                    ) togetherWith fadeOut(
                                        animationSpec = tween(
                                            durationMillis = DURATION_EXIT_SHORT,
                                            easing = EmphasizedAccelerateEasing
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { targetView ->
                            val sharedImageModifier = Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = coverKey),
                                animatedVisibilityScope = this,
                                placeholderSize = if (targetView == LYRIC_VIEW) {
                                    SharedTransitionScope.PlaceholderSize.AnimatedSize
                                } else {
                                    SharedTransitionScope.PlaceholderSize.ContentSize
                                },
                                boundsTransform = AlbumArtBoundsTransform
                            )

                            val sharedTitleModifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = titleKey),
                                animatedVisibilityScope = this,
                                boundsTransform = AlbumArtBoundsTransform
                            )

                            val sharedArtistModifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = artistKey),
                                animatedVisibilityScope = this,
                                boundsTransform = AlbumArtBoundsTransform
                            )

                            when (targetView) {
                                FULL_PLAYER -> {
                                    Player(
                                        navController = navController,
                                        mediaMetadata = mediaMetadata,
                                        imageModifier = sharedImageModifier,
                                        titleModifier = sharedTitleModifier,
                                        artistModifier = sharedArtistModifier,
                                        artworkElevationProvider = { artworkElevationState.value },
                                        onBackPressed = onBackPressed,
                                        onClick = { safeSwitchView(LYRIC_VIEW) },
                                        onContainerClick = { safeSwitchView(PLAY_QUEUE) },
                                        controlsAlpha = 1f,
                                        controlsOffsetYProvider = {
                                            val p = transitionProgressProvider().coerceIn(0f, 1f)
                                            val fullControlsAlpha = ((p - 0.28f) / 0.52f).coerceIn(0f, 1f)
                                            with(density) { (16.dp * (1f - fullControlsAlpha)).toPx() }
                                        },
                                        showArtwork = isFull,
                                        showBackground = false,
                                        onArtworkPositioned = { rect ->
                                            val current = fullArtworkRect
                                            if (current == null || kotlin.math.abs(current.top - rect.top) > 0.5f || kotlin.math.abs(current.left - rect.left) > 0.5f) {
                                                fullArtworkRect = rect
                                            }
                                        },
                                        onSheetOpenChange = { isSheetOpen = it },
                                        dismissSheets = dismissSheetsRequested,
                                        onSheetsDismissed = { dismissSheetsRequested = false }
                                    )
                                }
                                PLAY_QUEUE -> {
                                    PlayerQueue(
                                        mediaMetadata = mediaMetadata,
                                        imageModifier = sharedImageModifier,
                                        titleModifier = sharedTitleModifier,
                                        artistModifier = sharedArtistModifier,
                                        indicatorAlphaProvider = { queueIndicatorAlphaState.value },
                                        onBackPressed = { safeSwitchView(FULL_PLAYER) },
                                        showBackground = false,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                LYRIC_VIEW -> {
                                    Lyric(
                                        mediaMetadata = mediaMetadata,
                                        imageModifier = sharedImageModifier,
                                        titleModifier = sharedTitleModifier,
                                        artistModifier = sharedArtistModifier,
                                        onBackPressed = { safeSwitchView(FULL_PLAYER) },
                                        showBackground = false,
                                        isLyricActive = isExpanded && !isCollapsed && currentView == LYRIC_VIEW,
                                        lyricViewModel = lyricViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        if (!isFull || subViewTransition.currentState != subViewTransition.targetState) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                        enabled = !isExpanded,
                                        onClick = onClick
                                    )
                                    .pointerInput(isExpanded) {
                                        if (isExpanded) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                                    event.changes.forEach { it.consume() }
                                                }
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            if (showMiniLayer) {
                Box(
                    modifier = Modifier
                        .size(
                            width = with(density) { miniWidthPx.toDp() },
                            height = MiniPlayerHeight
                        )
                        .graphicsLayer {
                            val p = transitionProgressProvider().coerceIn(0f, 1f)
                            val miniAlpha = ((0.32f - p) / 0.32f).coerceIn(0f, 1f)
                            val dockedPx = dockedBottomPaddingProvider().toPx()
                            val rect = containerRectFor(p, dockedPx)
                            val miniTranslationYPx = (-10.dp * (1f - miniAlpha)).toPx()
                            alpha = miniAlpha
                            translationX = (screenWidthPx - miniWidthPx) * 0.5f
                            translationY = rect.top + miniTranslationYPx
                        }
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "展开播放器",
                            interactionSource = null,
                            indication = null,
                            enabled = !isExpanded,
                            onClick = onClick
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 60.dp, end = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            mediaMetadata.title?.let {
                                Text(
                                    text = it.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                            mediaMetadata.artist?.let {
                                Text(
                                    text = it.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (playerState?.isPlaying == true) mediaController?.pause()
                                else mediaController?.play()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (playerState?.isPlaying == true) Pause else PlayArrow,
                                contentDescription = stringResource(if (playerState?.isPlaying == true) R.string.pause else R.string.play),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { mediaController?.seekToNext() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = SkipNext,
                                contentDescription = "下一首",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    MiniPlayerProgressBar(
                        accentColor = MiniPlayerProgressColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 60.dp, end = 12.dp)
                            .height(2.5.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }

        if (!isFull) {
            val targetArtWidthPx = targetArtworkRect.width.coerceAtLeast(1f)
            val targetArtHeightPx = targetArtworkRect.height.coerceAtLeast(1f)
            val artworkClipShape = remember(
                screenWidthPx,
                screenHeightPx,
                density,
                targetArtworkRect,
                transitionProgressProvider,
                dockedBottomPaddingProvider
            ) {
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density
                    ): Outline {
                        val p = transitionProgressProvider().coerceIn(0f, 1f)
                        val dockedPx = with(density) { dockedBottomPaddingProvider().toPx() }
                        val miniArt = miniArtworkRectFor(dockedPx)
                        val currentArt = lerpRect(miniArt, targetArtworkRect, p)
                        val scale = (currentArt.width / size.width.coerceAtLeast(1f)).coerceAtLeast(0.01f)
                        val targetCornerPx = with(density) {
                            androidx.compose.ui.unit.lerp(8.dp, 24.dp, p).toPx()
                        }
                        val compensatedCornerPx = (targetCornerPx / scale)
                            .coerceAtMost(size.minDimension * 0.5f)
                        return Outline.Rounded(
                            RoundRect(
                                left = 0f,
                                top = 0f,
                                right = size.width,
                                bottom = size.height,
                                cornerRadius = CornerRadius(compensatedCornerPx)
                            )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(
                        width = with(density) { targetArtWidthPx.toDp() },
                        height = with(density) { targetArtHeightPx.toDp() }
                    )
                    .graphicsLayer {
                        val p = transitionProgressProvider().coerceIn(0f, 1f)
                        val dockedPx = dockedBottomPaddingProvider().toPx()
                        val miniArt = miniArtworkRectFor(dockedPx)
                        val currentArt = lerpRect(miniArt, targetArtworkRect, p)
                        val sx = (currentArt.width / targetArtWidthPx).coerceAtLeast(0.01f)
                        val sy = (currentArt.height / targetArtHeightPx).coerceAtLeast(0.01f)
                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = currentArt.left
                        translationY = currentArt.top
                        scaleX = sx
                        scaleY = sy
                        shadowElevation = androidx.compose.ui.unit.lerp(0.dp, 16.dp, p).toPx()
                        shape = artworkClipShape
                        clip = true
                    }
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "展开播放器",
                        interactionSource = null,
                        indication = null,
                        enabled = !isExpanded,
                        onClick = onClick
                    )
            ) {
                val context = LocalContext.current
                val imageRequest = remember(context, effectiveArtworkUri) {
                    val key = effectiveArtworkUri?.toString()
                    ImageRequest.Builder(context)
                        .data(effectiveArtworkUri)
                        .size(CoilSize(1080, 1080))
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
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}