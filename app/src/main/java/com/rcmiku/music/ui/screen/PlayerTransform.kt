package com.rcmiku.music.ui.screen

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaMetadata
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rcmiku.music.ui.components.Lyric
import com.rcmiku.music.ui.components.MiniPlayer
import com.rcmiku.music.ui.components.Player
import com.rcmiku.music.ui.components.PlayerQueue
import com.rcmiku.music.ui.design.ImmersiveBackground
import kotlin.math.roundToInt

const val FULL_PLAYER = 0
const val PLAY_QUEUE = 1
const val MINI_PLAYER = 2
const val LYRIC_VIEW = 3

private val PlayerMotionEasing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.0f)

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
    var currentView by rememberSaveable {
        mutableIntStateOf(FULL_PLAYER)
    }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            currentView = FULL_PLAYER
        }
    }

    BackHandler(enabled = isExpanded || transitionProgress > 0f) {
        if (currentView != FULL_PLAYER) {
            currentView = FULL_PLAYER
        } else {
            onBackPressed()
        }
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val view = LocalView.current
    val shouldDarkenBars = isExpanded || transitionProgress > 0.5f

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

    var miniArtworkRect by remember { mutableStateOf<Rect?>(null) }
    var fullArtworkRect by remember { mutableStateOf<Rect?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val statusBarInsetPx = WindowInsets.statusBars.getTop(density).toFloat()

        val defaultMiniRect = remember(screenWidthPx, screenHeightPx, dockedBottomPadding) {
            val miniSizePx = with(density) { 44.dp.toPx() }
            val miniLeftPx = with(density) { 20.dp.toPx() }
            val dockedBottomPx = with(density) { dockedBottomPadding.toPx() }
            val miniBottomPx = screenHeightPx - dockedBottomPx - with(density) { 10.dp.toPx() }
            Rect(miniLeftPx, miniBottomPx - miniSizePx, miniLeftPx + miniSizePx, miniBottomPx)
        }

        val defaultFullRect = remember(screenWidthPx, screenHeightPx, statusBarInsetPx) {
            val fullWidthPx = screenWidthPx * 0.88f
            val fullLeftPx = (screenWidthPx - fullWidthPx) / 2f
            val fullTopPx = statusBarInsetPx + with(density) { (56.dp + 16.dp).toPx() }
            Rect(fullLeftPx, fullTopPx, fullLeftPx + fullWidthPx, fullTopPx + fullWidthPx)
        }

        val startRect = miniArtworkRect ?: defaultMiniRect
        val endRect = fullArtworkRect ?: defaultFullRect

        // 1. 全屏沉浸式底色：展开 0%~20% 快速铺满；收起 20%~0% 渐隐
        val bgAlpha = (transitionProgress / 0.20f).coerceIn(0f, 1f)
        if (transitionProgress > 0f) {
            ImmersiveBackground(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = bgAlpha },
                artworkUri = mediaMetadata.artworkUri
            ) {}
        }

        // 2. Mini 控件层：展开 0%~15% 快速淡出并微下沉；收起 15%~0% 渐显
        val miniAlpha = (1f - transitionProgress / 0.15f).coerceIn(0f, 1f)
        val miniOffsetY = 8.dp * (transitionProgress / 0.15f).coerceIn(0f, 1f)
        if (transitionProgress < 0.20f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = dockedBottomPadding)
                    .graphicsLayer {
                        alpha = miniAlpha
                        translationY = miniOffsetY.toPx()
                    }
            ) {
                MiniPlayer(
                    mediaMetadata = mediaMetadata,
                    onClick = onClick,
                    controlsAlpha = miniAlpha,
                    controlsOffsetY = miniOffsetY,
                    showArtwork = (transitionProgress == 0f),
                    onArtworkPositioned = { miniArtworkRect = it }
                )
            }
        }

        // 3. 全屏播放器控件层：展开 40%~100% 错峰浮入；收起 100%~60% 优先淡出
        val fullControlsAlpha = if (isExpanded) {
            ((transitionProgress - 0.40f) / 0.60f).coerceIn(0f, 1f)
        } else {
            ((transitionProgress - 0.60f) / 0.40f).coerceIn(0f, 1f)
        }
        val fullControlsOffsetY = 16.dp * (1f - fullControlsAlpha)

        if (transitionProgress > 0f) {
            when (currentView) {
                FULL_PLAYER -> {
                    Player(
                        navController = navController,
                        mediaMetadata = mediaMetadata,
                        onBackPressed = onBackPressed,
                        onClick = { currentView = LYRIC_VIEW },
                        onContainerClick = { currentView = PLAY_QUEUE },
                        controlsAlpha = fullControlsAlpha,
                        controlsOffsetY = fullControlsOffsetY,
                        showArtwork = (transitionProgress == 1f),
                        showBackground = false,
                        onArtworkPositioned = { fullArtworkRect = it }
                    )
                }
                PLAY_QUEUE -> {
                    PlayerQueue(
                        mediaMetadata = mediaMetadata,
                        onBackPressed = { currentView = FULL_PLAYER },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = fullControlsAlpha }
                    )
                }
                LYRIC_VIEW -> {
                    Lyric(
                        mediaMetadata = mediaMetadata,
                        onBackPressed = { currentView = FULL_PLAYER },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = fullControlsAlpha }
                    )
                }
            }
        }

        // 4. 单一物理浮动封面：转场期间沿贝塞尔曲线唯一运动，杜绝双封面重叠与圆角失真
        if (transitionProgress > 0f && transitionProgress < 1f) {
            val easedT = PlayerMotionEasing.transform(transitionProgress)

            val leftPx = androidx.compose.ui.util.lerp(startRect.left, endRect.left, easedT)
            val topPx = androidx.compose.ui.util.lerp(startRect.top, endRect.top, easedT)
            val widthPx = androidx.compose.ui.util.lerp(startRect.width, endRect.width, easedT)
            val heightPx = androidx.compose.ui.util.lerp(startRect.height, endRect.height, easedT)
            val cornerRadius = androidx.compose.ui.unit.lerp(8.dp, 28.dp, easedT)
            val shadowElevation = androidx.compose.ui.unit.lerp(6.dp, 16.dp, easedT)

            Box(
                modifier = Modifier
                    .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                    .size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
                    .shadow(shadowElevation, shape = RoundedCornerShape(cornerRadius))
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                AsyncImage(
                    model = mediaMetadata.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}