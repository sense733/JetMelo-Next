package com.rcmiku.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * 遵循 Android 13 (Tiramisu) 媒体控制中心规范的正弦波浪形媒体进度条轨道。
 *
 * 在播放状态下已播放区域呈现动态流动的正弦波浪，暂停或拖拽交互时波浪平滑拉直为平线；
 * 未播放区域为半透明平直轨道，波浪两端带有平滑渐隐衰减窗口，确保与起点和滑块平滑对接。
 *
 * @param sliderState Material 3 Slider 状态对象，用于读取当前进度比例
 * @param activeColor 已播放区域（波浪轨迹）的主色
 * @param inactiveColor 未播放区域（平直轨迹）的底色
 * @param isPlaying 是否处于播放状态，用于驱动流动动效与波浪展开/平复过渡
 * @param isDragging 用户当前是否正在拖拽滑块，拖拽时波浪平滑平复为直线
 * @param enabled 进度条是否处于可用交互状态
 * @param modifier 容器修饰符
 * @param waveLength 波长周期
 * @param waveHeight 波浪峰峰值高度
 * @param strokeWidth 轨道线条粗细
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavySliderTrack(
    sliderState: SliderState,
    activeColor: Color,
    inactiveColor: Color,
    isPlaying: Boolean,
    isDragging: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    waveLength: Dp = 20.dp,
    waveHeight: Dp = 5.dp,
    strokeWidth: Dp = 4.dp
) {
    val density = LocalDensity.current
    val amplitudePx = with(density) { (waveHeight / 2f).toPx() }
    val waveLengthPx = with(density) { waveLength.toPx() }
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    val targetAmplitude = if (isPlaying && !isDragging && enabled) amplitudePx else 0f
    val currentAmplitudeState = animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "wavy_slider_amplitude"
    )

    val isWaveActive by remember(isPlaying, isDragging, enabled) {
        derivedStateOf {
            (isPlaying && !isDragging && enabled) || currentAmplitudeState.value > 0.05f
        }
    }

    val phaseProgressState: State<Float>?
    if (isWaveActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "wavy_slider_phase_transition")
        phaseProgressState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavy_slider_phase_progress"
        )
    } else {
        phaseProgressState = null
    }

    val effectiveActiveColor = if (enabled) activeColor else activeColor.copy(alpha = 0.38f)
    val effectiveInactiveColor = if (enabled) inactiveColor else inactiveColor.copy(alpha = 0.12f)

    val wavePath = remember { Path() }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val currentAmplitude = currentAmplitudeState.value
        val phaseProgress = phaseProgressState?.value ?: 0f

        val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
        val fraction = if (range > 0f) {
            ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
        } else {
            0f
        }
        val progressX = (width * fraction).coerceIn(0f, width)

        // 1. 绘制未播放区域（平直平滑轨道）
        if (progressX < width) {
            drawLine(
                color = effectiveInactiveColor,
                start = Offset(progressX, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }

        // 2. 绘制已播放区域（动态正弦波浪或平线）
        if (progressX > 0f) {
            if (currentAmplitude <= 0.05f) {
                drawLine(
                    color = effectiveActiveColor,
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            } else {
                wavePath.rewind()
                wavePath.moveTo(0f, centerY)

                val stepPx = with(density) { 2.dp.toPx() }.coerceAtLeast(2f)
                val phaseRad = phaseProgress * 2f * PI.toFloat()
                val taperDistance = (waveLengthPx / 2f).coerceAtMost(progressX / 2f)

                var currentX = 0f
                while (currentX < progressX) {
                    currentX = (currentX + stepPx).coerceAtMost(progressX)

                    val taperFactor = if (taperDistance > 0f) {
                        val startFactor = (currentX / taperDistance).coerceIn(0f, 1f)
                        val endFactor = ((progressX - currentX) / taperDistance).coerceIn(0f, 1f)
                        (sin(startFactor * (PI.toFloat() / 2f)) * sin(endFactor * (PI.toFloat() / 2f)))
                    } else {
                        0f
                    }

                    val angle = (2f * PI.toFloat() * currentX / waveLengthPx) - phaseRad
                    val y = centerY + currentAmplitude * taperFactor * sin(angle)
                    wavePath.lineTo(currentX, y)
                }

                drawPath(
                    path = wavePath,
                    color = effectiveActiveColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}
