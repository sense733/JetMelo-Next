package com.rcmiku.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.rcmiku.music.ui.icons.Repeat
import com.rcmiku.music.ui.icons.RepeatOne
import com.rcmiku.music.ui.icons.Shuffle
import kotlinx.coroutines.launch

enum class PlayMode {
    LOOP_ALL,
    SHUFFLE,
    REPEAT_ONE
}

/**
 * Maps Media3 playback flags into a single mutually exclusive [PlayMode].
 */
fun resolvePlayMode(repeatMode: Int, shuffleModeEnabled: Boolean): PlayMode = when {
    repeatMode == Player.REPEAT_MODE_ONE -> PlayMode.REPEAT_ONE
    shuffleModeEnabled -> PlayMode.SHUFFLE
    else -> PlayMode.LOOP_ALL
}

/**
 * Returns the next [PlayMode] in the cycle: loop all -> shuffle -> repeat one.
 */
fun nextPlayMode(current: PlayMode): PlayMode = when (current) {
    PlayMode.LOOP_ALL -> PlayMode.SHUFFLE
    PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
    PlayMode.REPEAT_ONE -> PlayMode.LOOP_ALL
}

/**
 * Applies a mutually exclusive [target] mode to [mediaController], keeping repeat and shuffle
 * flags consistent across player UI and MediaSession state.
 */
fun applyPlayMode(mediaController: MediaController?, target: PlayMode) {
    val controller = mediaController ?: return
    when (target) {
        PlayMode.LOOP_ALL -> {
            if (controller.shuffleModeEnabled) controller.shuffleModeEnabled = false
            if (controller.repeatMode != Player.REPEAT_MODE_ALL) {
                controller.repeatMode = Player.REPEAT_MODE_ALL
            }
        }
        PlayMode.SHUFFLE -> {
            if (controller.repeatMode != Player.REPEAT_MODE_ALL) {
                controller.repeatMode = Player.REPEAT_MODE_ALL
            }
            if (!controller.shuffleModeEnabled) controller.shuffleModeEnabled = true
        }
        PlayMode.REPEAT_ONE -> {
            if (controller.shuffleModeEnabled) controller.shuffleModeEnabled = false
            if (controller.repeatMode != Player.REPEAT_MODE_ONE) {
                controller.repeatMode = Player.REPEAT_MODE_ONE
            }
        }
    }
}

/**
 * Unified three-state play mode toggle button with elastic press bounce, expanding halo ring,
 * and rotating morph transition between loop-all, shuffle, and repeat-one icons.
 */
@Composable
fun PlayModeToggleButton(
    playMode: PlayMode,
    tint: Color,
    rippleColor: Color,
    onToggle: (PlayMode) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val buttonScale = remember { Animatable(1f) }
    val iconRotation = remember { Animatable(0f) }
    val pulseProgress = remember { Animatable(0f) }

    val contentDescription = when (playMode) {
        PlayMode.LOOP_ALL -> "列表循环"
        PlayMode.SHUFFLE -> "随机播放"
        PlayMode.REPEAT_ONE -> "单曲循环"
    }

    IconButton(
        onClick = {
            ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CLOCK_TICK)
            val next = nextPlayMode(playMode)
            onToggle(next)

            scope.launch {
                buttonScale.snapTo(1f)
                buttonScale.animateTo(0.82f, animationSpec = tween(durationMillis = 70))
                buttonScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            scope.launch {
                iconRotation.snapTo(-26f)
                iconRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            scope.launch {
                pulseProgress.snapTo(0f)
                pulseProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
                )
            }
        },
        modifier = modifier.graphicsLayer {
            scaleX = buttonScale.value
            scaleY = buttonScale.value
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(iconSize + 14.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val progress = pulseProgress.value
                if (progress > 0f && progress < 1f) {
                    val radiusScale = 0.65f + 0.70f * progress
                    val alpha = (1f - progress) * 0.28f
                    drawCircle(
                        color = rippleColor.copy(alpha = alpha),
                        radius = (size.minDimension * 0.5f) * radiusScale,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            AnimatedContent(
                targetState = playMode,
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.62f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(durationMillis = 180))) togetherWith
                        (scaleOut(
                            targetScale = 0.62f,
                            animationSpec = tween(durationMillis = 130)
                        ) + fadeOut(animationSpec = tween(durationMillis = 130)))
                },
                label = "play_mode_icon_transition"
            ) { mode ->
                val icon = when (mode) {
                    PlayMode.LOOP_ALL -> Repeat
                    PlayMode.SHUFFLE -> Shuffle
                    PlayMode.REPEAT_ONE -> RepeatOne
                }
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            rotationZ = iconRotation.value
                        }
                )
            }
        }
    }
}
