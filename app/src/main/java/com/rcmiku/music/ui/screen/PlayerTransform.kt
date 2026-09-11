package com.rcmiku.music.ui.screen

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
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
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
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
import com.rcmiku.music.ui.theme.AdaptiveArtworkShape
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.ui.theme.rememberDeviceCornerRadius
import kotlin.coroutines.cancellation.CancellationException
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
    transitionProgress: Float = 0f,
    dockedBottomPadding: Dp = 0.dp,
) {
    val progress = transitionProgress.coerceIn(0f, 1f)
    val isCollapsed = progress <= ProgressEps
    val isFull = progress >= 1f - ProgressEps

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
    var predictiveScale by remember { mutableFloatStateOf(1f) }
    var predictiveCornerRadius by remember { mutableStateOf(0.dp) }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            currentView = FULL_PLAYER
        }
    }

    PredictiveBackHandler(enabled = isExpanded || !isCollapsed) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                if (currentView == FULL_PLAYER && !isSheetOpen) {
                    predictiveScale = 1f - (backEvent.progress * 0.08f)
                    predictiveCornerRadius = 16.dp * backEvent.progress
                }
            }
            predictiveScale = 1f
            predictiveCornerRadius = 0.dp
            if (isSheetOpen) {
                dismissSheetsRequested = true
            } else if (currentView != FULL_PLAYER) {
                safeSwitchView(FULL_PLAYER)
            } else {
                onBackPressed()
            }
        } catch (e: CancellationException) {
            predictiveScale = 1f
            predictiveCornerRadius = 0.dp
        }
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val view = LocalView.current
    val shouldDarkenBars = isExpanded || progress > 0.5f

    DisposableEffect(shouldDarkenBars, isDarkTheme) {
        val window = (view.context as? Activity)?.window
        if (window != null && !view.isInEditMode) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (shouldDarkenBars) {
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            } else {
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
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
    val mediaController = LocalPlayerController.current.controller
    val artworkColors = LocalArtworkColors.current
    var fullArtworkRect by remember { mutableStateOf<Rect?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val statusBarInsetPx = WindowInsets.statusBars.getTop(density).toFloat()
        val navBarInsetPx = WindowInsets.navigationBars.getBottom(density).toFloat()

        val miniHorizontalPaddingPx = with(density) { 12.dp.toPx() }
        val miniVerticalPaddingPx = with(density) { 8.dp.toPx() }
        val miniHeightPx = with(density) { MiniPlayerHeight.toPx() }
        val dockedBottomPx = with(density) { dockedBottomPadding.toPx() }

        val miniLeftPx = miniHorizontalPaddingPx
        val miniRightPx = screenWidthPx - miniHorizontalPaddingPx
        val miniBottomPx = screenHeightPx - dockedBottomPx - miniVerticalPaddingPx
        val miniTopPx = miniBottomPx - miniHeightPx

        val miniRect = remember(miniLeftPx, miniTopPx, miniRightPx, miniBottomPx) {
            Rect(miniLeftPx, miniTopPx, miniRightPx, miniBottomPx)
        }
        val fullRect = remember(screenWidthPx, screenHeightPx) {
            Rect(0f, 0f, screenWidthPx, screenHeightPx)
        }

        val defaultFullArtworkRect = remember(screenWidthPx, screenHeightPx, statusBarInsetPx, navBarInsetPx) {
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
        val miniArtworkRect = remember(miniLeftPx, miniTopPx) {
            val sizePx = with(density) { 44.dp.toPx() }
            val leftPx = miniLeftPx + with(density) { 8.dp.toPx() }
            val topPx = miniTopPx + with(density) { 6.dp.toPx() }
            Rect(leftPx, topPx, leftPx + sizePx, topPx + sizePx)
        }

        val containerRect = lerpRect(miniRect, fullRect, progress)
        val deviceCornerRadius = rememberDeviceCornerRadius()
        val baseCornerRadius = androidx.compose.ui.unit.lerp(16.dp, deviceCornerRadius, progress)
        val containerCornerRadius = (baseCornerRadius + predictiveCornerRadius).coerceAtMost(36.dp)
        val containerElevation = androidx.compose.ui.unit.lerp(6.dp, 0.dp, progress)

        val targetArtworkRect = fullArtworkRect ?: defaultFullArtworkRect
        val artworkProgress = progress
        val currentArtworkRect = lerpRect(miniArtworkRect, targetArtworkRect, artworkProgress)
        val currentArtworkCorner = androidx.compose.ui.unit.lerp(8.dp, 24.dp, artworkProgress)
        val currentArtworkElevation = androidx.compose.ui.unit.lerp(0.dp, 16.dp, artworkProgress)

        if (!isFull) {
            val borderAlpha = ((0.32f - progress) / 0.32f).coerceIn(0f, 1f)
            val containerShape = RoundedCornerShape(containerCornerRadius)
            val shadowModifier = if (containerElevation > 0.dp) {
                Modifier.shadow(containerElevation, shape = containerShape)
            } else Modifier

            val borderModifier = if (borderAlpha > 0f) {
                Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.5f * borderAlpha
                    ),
                    shape = containerShape
                )
            } else Modifier

            Box(
                modifier = Modifier
                    .offset { IntOffset(containerRect.left.roundToInt(), containerRect.top.roundToInt()) }
                    .size(
                        width = with(density) { containerRect.width.toDp() },
                        height = with(density) { containerRect.height.toDp() }
                    )
                    .then(shadowModifier)
                    .then(borderModifier)
            )
        }

        val clipShape = remember(containerRect, containerCornerRadius) {
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
                    return Outline.Rounded(
                        RoundRect(
                            rect = containerRect,
                            cornerRadius = CornerRadius(with(density) { containerCornerRadius.toPx() })
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = !isFull || predictiveCornerRadius > 0.dp
                    if (!isFull || predictiveCornerRadius > 0.dp) {
                        shape = clipShape
                    }
                    scaleX = predictiveScale
                    scaleY = predictiveScale
                }
        ) {
            if (!isCollapsed) {
                ImmersiveBackground(
                    modifier = Modifier.fillMaxSize(),
                    artworkUri = effectiveArtworkUri
                ) {}
            }

            if (progress < 0.32f) {
                val surfaceAlpha = if (isCollapsed) 1f else ((0.32f - progress) / 0.32f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = surfaceAlpha))
                )
            }

            val miniAlpha = ((0.32f - progress) / 0.32f).coerceIn(0f, 1f)
            if (miniAlpha > 0f) {
                val miniTranslationY = (-10.dp * (1f - miniAlpha))
                Box(
                    modifier = Modifier
                        .offset { IntOffset(containerRect.left.roundToInt(), containerRect.top.roundToInt()) }
                        .size(
                            width = with(density) { containerRect.width.toDp() },
                            height = with(density) { miniHeightPx.toDp() }
                        )
                        .graphicsLayer {
                            alpha = miniAlpha
                            translationY = with(density) { miniTranslationY.toPx() }
                        }
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "展开播放器",
                            interactionSource = null,
                            indication = null,
                            enabled = isCollapsed,
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
                                .padding(end = 4.dp)
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
                        accentColor = artworkColors.accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 60.dp, end = 12.dp)
                            .height(2.5.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            val fullControlsAlpha = ((progress - 0.28f) / 0.52f).coerceIn(0f, 1f)
            val fullControlsOffsetY = 16.dp * (1f - fullControlsAlpha)

            val coverKey = "player_active_cover"
            val titleKey = "player_active_title"
            val artistKey = "player_active_artist"

            if (!isCollapsed) {
                SharedTransitionLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = fullControlsAlpha
                        }
                ) {
                    val subViewTransition = updateTransition(
                        targetState = currentView,
                        label = "player_subview_shared_transition"
                    )

                    val artworkElevation by subViewTransition.animateDp(
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

                    Box(modifier = Modifier.fillMaxSize()) {
                        subViewTransition.AnimatedContent(
                            transitionSpec = {
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
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { targetView ->
                            val sharedImageModifier = Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = coverKey),
                                animatedVisibilityScope = this,
                                placeHolderSize = SharedTransitionScope.PlaceHolderSize.contentSize,
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
                                        artworkElevation = artworkElevation,
                                        onBackPressed = onBackPressed,
                                        onClick = { safeSwitchView(LYRIC_VIEW) },
                                        onContainerClick = { safeSwitchView(PLAY_QUEUE) },
                                        controlsAlpha = 1f,
                                        controlsOffsetY = fullControlsOffsetY,
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
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        if (!isFull || subViewTransition.currentState != subViewTransition.targetState) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        if (!isFull) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentArtworkRect.left.roundToInt(), currentArtworkRect.top.roundToInt()) }
                    .size(
                        width = with(density) { currentArtworkRect.width.toDp() },
                        height = with(density) { currentArtworkRect.height.toDp() }
                    )
                    .shadow(currentArtworkElevation, shape = RoundedCornerShape(currentArtworkCorner))
                    .clip(RoundedCornerShape(currentArtworkCorner))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "展开播放器",
                        interactionSource = null,
                        indication = null,
                        enabled = isCollapsed,
                        onClick = onClick
                    )
            ) {
                val context = LocalContext.current
                val imageRequest = remember(context, effectiveArtworkUri) {
                    val key = effectiveArtworkUri?.toString()
                    ImageRequest.Builder(context)
                        .data(effectiveArtworkUri)
                        .size(CoilSize(1080, 1080))
                        .memoryCacheKey(key)
                        .diskCacheKey(key)
                        .crossfade(true)
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