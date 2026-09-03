package com.rcmiku.music.ui.screen

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
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
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
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
import com.rcmiku.music.ui.theme.rememberDeviceCornerRadius
import kotlin.math.roundToInt

const val FULL_PLAYER = 0
const val PLAY_QUEUE = 1
const val MINI_PLAYER = 2
const val LYRIC_VIEW = 3

private fun lerpRect(start: Rect, stop: Rect, fraction: Float): Rect =
    Rect(
        androidx.compose.ui.util.lerp(start.left, stop.left, fraction),
        androidx.compose.ui.util.lerp(start.top, stop.top, fraction),
        androidx.compose.ui.util.lerp(start.right, stop.right, fraction),
        androidx.compose.ui.util.lerp(start.bottom, stop.bottom, fraction)
    )

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

    val playerState = LocalPlayerState.current
    val mediaController = LocalPlayerController.current.controller
    val artworkColors = LocalArtworkColors.current
    var fullArtworkRect by remember { mutableStateOf<Rect?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val statusBarInsetPx = WindowInsets.statusBars.getTop(density).toFloat()

        val miniHorizontalPaddingPx = with(density) { 12.dp.toPx() }
        val miniVerticalPaddingPx = with(density) { 6.dp.toPx() }
        val miniHeightPx = with(density) { (MiniPlayerHeight - 12.dp).toPx() }
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

        val defaultFullArtworkRect = remember(screenWidthPx, screenHeightPx, statusBarInsetPx) {
            val fullWidthPx = screenWidthPx * 0.88f
            val fullLeftPx = (screenWidthPx - fullWidthPx) / 2f
            val availableHeight = screenHeightPx - statusBarInsetPx - with(density) { (56.dp + 284.dp).toPx() }
            val fullTopPx = statusBarInsetPx + with(density) { 56.dp.toPx() } + ((availableHeight - fullWidthPx) / 2f).coerceAtLeast(0f)
            Rect(fullLeftPx, fullTopPx, fullLeftPx + fullWidthPx, fullTopPx + fullWidthPx)
        }
        val miniArtworkRect = remember(miniLeftPx, miniTopPx) {
            val sizePx = with(density) { 44.dp.toPx() }
            val leftPx = miniLeftPx + with(density) { 8.dp.toPx() }
            val topPx = miniTopPx + with(density) { 6.dp.toPx() }
            Rect(leftPx, topPx, leftPx + sizePx, topPx + sizePx)
        }

        val containerRect = lerpRect(miniRect, fullRect, transitionProgress)
        val deviceCornerRadius = rememberDeviceCornerRadius()
        val containerCornerRadius = androidx.compose.ui.unit.lerp(16.dp, deviceCornerRadius, transitionProgress)
        val containerElevation = androidx.compose.ui.unit.lerp(6.dp, 0.dp, transitionProgress)

        val targetArtworkRect = fullArtworkRect ?: defaultFullArtworkRect
        val currentArtworkRect = lerpRect(miniArtworkRect, targetArtworkRect, transitionProgress)
        val currentArtworkCorner = androidx.compose.ui.unit.lerp(8.dp, 28.dp, transitionProgress)
        val currentArtworkElevation = androidx.compose.ui.unit.lerp(0.dp, 16.dp, transitionProgress)

        // 1. 物理形变容器阴影（卡片背后投影，随进度向全屏扩展并逐渐淡出）
        if (transitionProgress < 1f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(containerRect.left.roundToInt(), containerRect.top.roundToInt()) }
                    .size(
                        width = with(density) { containerRect.width.toDp() },
                        height = with(density) { containerRect.height.toDp() }
                    )
                    .shadow(containerElevation, shape = RoundedCornerShape(containerCornerRadius))
            )
        }

        // 2. Mini 栏描边：严格跟随容器边界与圆角同步伸缩，展开 0%~15% 渐隐，收起 15%~0% 渐显
        val borderAlpha = (1f - transitionProgress / 0.15f).coerceIn(0f, 1f)
        if (borderAlpha > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(containerRect.left.roundToInt(), containerRect.top.roundToInt()) }
                    .size(
                        width = with(density) { containerRect.width.toDp() },
                        height = with(density) { containerRect.height.toDp() }
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.5f * borderAlpha
                        ),
                        shape = RoundedCornerShape(containerCornerRadius)
                    )
            )
        }

        // 3. 物理形变视口（Container Transform Window）：以全屏尺寸承载所有内容，通过动态裁剪窗扩展，彻底杜绝内容被高度挤压或尺寸坍缩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = transitionProgress < 1f
                    if (transitionProgress < 1f) {
                        shape = object : Shape {
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
                }
        ) {
            // 容器实底沉浸背景：严格受限在动态裁剪窗内部，实底且平滑揭示
            ImmersiveBackground(
                modifier = Modifier.fillMaxSize(),
                artworkUri = mediaMetadata.artworkUri
            ) {}

            // 折叠态及初段融合 surfaceContainerHigh，确保底栏色彩契合主题
            if (transitionProgress < 0.25f) {
                val surfaceAlpha = (1f - transitionProgress / 0.25f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = surfaceAlpha))
                )
            }

            // Mini 控件层：随容器顶部同步位移，展开 0%~15% 极速淡出
            val miniAlpha = (1f - transitionProgress / 0.15f).coerceIn(0f, 1f)
            if (miniAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(containerRect.left.roundToInt(), containerRect.top.roundToInt()) }
                        .size(
                            width = with(density) { containerRect.width.toDp() },
                            height = with(density) { miniHeightPx.toDp() }
                        )
                        .graphicsLayer { alpha = miniAlpha }
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
                                .padding(end = 6.dp)
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
                            }
                        ) {
                            Icon(
                                imageVector = if (playerState?.isPlaying == true) Pause else PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = { mediaController?.seekToNext() }) {
                            Icon(
                                imageVector = SkipNext,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    MiniPlayerProgressBar(
                        accentColor = artworkColors.accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            // Full 控件层：以完整全屏尺寸布局，展开 60%~100% 错峰浮入；收起 100%~70% 优先淡出
            val fullControlsAlpha = if (isExpanded) {
                ((transitionProgress - 0.60f) / 0.40f).coerceIn(0f, 1f)
            } else {
                ((transitionProgress - 0.70f) / 0.30f).coerceIn(0f, 1f)
            }
            val fullControlsOffsetY = 12.dp * (1f - fullControlsAlpha)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = fullControlsAlpha
                        translationY = fullControlsOffsetY.toPx()
                    }
            ) {
                when (currentView) {
                    FULL_PLAYER -> {
                        Player(
                            navController = navController,
                            mediaMetadata = mediaMetadata,
                            onBackPressed = onBackPressed,
                            onClick = { currentView = LYRIC_VIEW },
                            onContainerClick = { currentView = PLAY_QUEUE },
                            controlsAlpha = 1f,
                            controlsOffsetY = 0.dp,
                            showArtwork = false,
                            showBackground = false,
                            onArtworkPositioned = { rect ->
                                if (fullArtworkRect == null || fullArtworkRect != rect) {
                                    fullArtworkRect = rect
                                }
                            }
                        )
                    }
                    PLAY_QUEUE -> {
                        PlayerQueue(
                            mediaMetadata = mediaMetadata,
                            onBackPressed = { currentView = FULL_PLAYER },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    LYRIC_VIEW -> {
                        Lyric(
                            mediaMetadata = mediaMetadata,
                            onBackPressed = { currentView = FULL_PLAYER },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 折叠态点击展开拦截区（仅在完全折叠时响应点击）
            if (transitionProgress == 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(miniLeftPx.roundToInt(), miniTopPx.roundToInt()) }
                        .size(
                            width = with(density) { (miniRightPx - miniLeftPx).toDp() },
                            height = with(density) { miniHeightPx.toDp() }
                        )
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onClick
                        )
                )
            }
        }

        // 2. 单一物理封面：1:1 与容器同步位移与缩放，杜绝双封面重叠与视差脱节
        if (currentView == FULL_PLAYER) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentArtworkRect.left.roundToInt(), currentArtworkRect.top.roundToInt()) }
                    .size(
                        width = with(density) { currentArtworkRect.width.toDp() },
                        height = with(density) { currentArtworkRect.height.toDp() }
                    )
                    .shadow(currentArtworkElevation, shape = RoundedCornerShape(currentArtworkCorner))
                    .clip(RoundedCornerShape(currentArtworkCorner))
                    .then(
                        if (transitionProgress == 1f) {
                            Modifier.clickable { currentView = LYRIC_VIEW }
                        } else Modifier
                    )
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