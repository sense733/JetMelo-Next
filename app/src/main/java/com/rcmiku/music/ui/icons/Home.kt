package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val HomeMusic: ImageVector
    get() {
        if (_Home != null) {
            return _Home!!
        }
        _Home = ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF5F6368))) {
                moveTo(240f, 800f)
                verticalLineToRelative(-360f)
                lineToRelative(-80f, 0f)
                lineToRelative(320f, -280f)
                lineToRelative(320f, 280f)
                lineToRelative(-80f, 0f)
                verticalLineToRelative(360f)
                horizontalLineToRelative(-200f)
                verticalLineToRelative(-240f)
                horizontalLineToRelative(-160f)
                verticalLineToRelative(240f)
                horizontalLineTo(240f)
                close()
                moveTo(320f, 720f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-240f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(240f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(-310f)
                lineTo(480f, 258f)
                lineTo(320f, 410f)
                verticalLineToRelative(310f)
                close()
            }
        }.build()
        return _Home!!
    }

private var _Home: ImageVector? = null
