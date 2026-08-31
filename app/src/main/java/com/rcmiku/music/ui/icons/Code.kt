package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Code: ImageVector
    get() {
        if (_Code != null) {
            return _Code!!
        }
        _Code = ImageVector.Builder(
            name = "Code",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(320f, 680f)
                lineToRelative(-200f, -200f)
                lineToRelative(200f, -200f)
                lineToRelative(56f, 56f)
                lineToRelative(-144f, 144f)
                lineToRelative(144f, 144f)
                lineToRelative(-56f, 56f)
                close()
                moveTo(640f, 680f)
                lineToRelative(-56f, -56f)
                lineToRelative(144f, -144f)
                lineToRelative(-144f, -144f)
                lineToRelative(56f, -56f)
                lineToRelative(200f, 200f)
                lineToRelative(-200f, 200f)
                close()
            }
        }.build()

        return _Code!!
    }

@Suppress("ObjectPropertyName")
private var _Code: ImageVector? = null
