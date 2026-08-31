package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Down: ImageVector
    get() {
        if (_Down != null) {
            return _Down!!
        }
        _Down = ImageVector.Builder(
            name = "Down",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF5F6368))) {
                moveTo(480f, 640f)
                lineTo(240f, 400f)
                lineToRelative(56f, -56f)
                lineToRelative(144f, 144f)
                verticalLineToRelative(-328f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(328f)
                lineToRelative(144f, -144f)
                lineToRelative(56f, 56f)
                lineToRelative(-240f, 240f)
                close()
                moveTo(240f, 800f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(160f, 720f)
                verticalLineToRelative(-120f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(480f)
                verticalLineToRelative(-120f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(120f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(720f, 800f)
                horizontalLineTo(240f)
                close()
            }
        }.build()

        return _Down!!
    }

@Suppress("ObjectPropertyName")
private var _Down: ImageVector? = null
