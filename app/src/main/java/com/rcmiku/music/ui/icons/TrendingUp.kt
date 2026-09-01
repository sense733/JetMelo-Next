package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _TrendingUp: ImageVector? = null

val TrendingUp: ImageVector
    get() {
        if (_TrendingUp != null) {
            return _TrendingUp!!
        }
        _TrendingUp = ImageVector.Builder(
            name = "TrendingUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(640f, 320f)
                lineToRelative(94f, 94f)
                lineToRelative(-214f, 214f)
                lineToRelative(-160f, -160f)
                lineTo(120f, 708f)
                lineToRelative(56f, 56f)
                lineToRelative(184f, -184f)
                lineToRelative(160f, 160f)
                lineToRelative(270f, -270f)
                lineToRelative(90f, 90f)
                verticalLineToRelative(-240f)
                horizontalLineTo(640f)
                close()
            }
        }.build()
        return _TrendingUp!!
    }
