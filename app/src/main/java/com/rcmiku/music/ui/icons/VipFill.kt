package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val VipFill: ImageVector
    get() {
        if (_VipFill != null) {
            return _VipFill!!
        }
        _VipFill = ImageVector.Builder(
            name = "VipFill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(240f, 360f)
                lineToRelative(120f, -200f)
                horizontalLineToRelative(240f)
                lineToRelative(120f, 200f)
                lineTo(480f, 800f)
                lineTo(240f, 360f)
                close()
            }
        }.build()

        return _VipFill!!
    }

@Suppress("ObjectPropertyName")
private var _VipFill: ImageVector? = null
