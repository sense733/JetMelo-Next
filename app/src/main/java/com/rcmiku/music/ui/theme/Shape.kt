package com.rcmiku.music.ui.theme

import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val JetMeloM3Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object JetMeloShapes {
    val extraSmall = RoundedCornerShape(4.dp)
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(24.dp)
    val extraLarge = RoundedCornerShape(28.dp)
    val full = CircleShape
}

val AdaptiveArtworkShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val minPx = with(density) { 44.dp.toPx() }
        val maxPx = with(density) { 280.dp.toPx() }
        val fraction = if (maxPx > minPx) {
            ((size.width - minPx) / (maxPx - minPx)).coerceIn(0f, 1f)
        } else {
            1f
        }
        val cornerRadiusPx = with(density) {
            (8.dp.toPx() + (24.dp.toPx() - 8.dp.toPx()) * fraction)
        }
        return Outline.Rounded(
            RoundRect(
                rect = Rect(Offset.Zero, size),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
        )
    }
}

@Composable
fun rememberDeviceCornerRadius(defaultRadius: Dp = 32.dp): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    var cornerRadius by remember { mutableStateOf(defaultRadius) }

    DisposableEffect(view, density) {
        fun updateCorner() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val insets = view.rootWindowInsets
                val tl = insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
                val tr = insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0
                val bl = insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
                val br = insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
                val maxRadius = maxOf(tl, tr, bl, br)
                if (maxRadius > 0) {
                    cornerRadius = with(density) { maxRadius.toDp() }
                }
            }
        }

        updateCorner()
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                updateCorner()
            }

            override fun onViewDetachedFromWindow(v: View) {}
        }
        view.addOnAttachStateChangeListener(listener)
        onDispose {
            view.removeOnAttachStateChangeListener(listener)
        }
    }

    return cornerRadius
}
