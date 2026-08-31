package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PlayArrowFill: ImageVector
    get() {
        if (_PlayArrowFill != null) {
            return _PlayArrowFill!!
        }
        _PlayArrowFill = ImageVector.Builder(
            name = "PlayArrowFill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF1F1F1F))) {
                moveTo(320f, 760f)
                verticalLineToRelative(-560f)
                lineToRelative(440f, 280f)
                lineToRelative(-440f, 280f)
                close()
            }
        }.build()

        return _PlayArrowFill!!
    }

@Suppress("ObjectPropertyName")
private var _PlayArrowFill: ImageVector? = null
