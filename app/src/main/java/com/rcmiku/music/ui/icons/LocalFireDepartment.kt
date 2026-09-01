package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _LocalFireDepartment: ImageVector? = null

val LocalFireDepartment: ImageVector
    get() {
        if (_LocalFireDepartment != null) {
            return _LocalFireDepartment!!
        }
        _LocalFireDepartment = ImageVector.Builder(
            name = "LocalFireDepartment",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(480f, 880f)
                quadToRelative(-133f, 0f, -226.5f, -93.5f)
                reflectiveQuadTo(160f, 560f)
                quadToRelative(0f, -118f, 58f, -214f)
                reflectiveQuadToRelative(158f, -146f)
                quadToRelative(17f, -7f, 32.5f, 3.5f)
                reflectiveQuadTo(420f, 230f)
                quadToRelative(0f, 37f, 19.5f, 68.5f)
                reflectiveQuadTo(494f, 344f)
                quadToRelative(31f, 19f, 48.5f, 50.5f)
                reflectiveQuadTo(560f, 464f)
                quadToRelative(0f, 33f, -23.5f, 56.5f)
                reflectiveQuadTo(480f, 544f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(440f, 504f)
                quadToRelative(0f, -25f, -15f, -44.5f)
                reflectiveQuadTo(388f, 432f)
                quadToRelative(-36f, 33f, -58f, 79.5f)
                reflectiveQuadTo(308f, 608f)
                quadToRelative(0f, 72f, 50f, 122f)
                reflectiveQuadToRelative(122f, 50f)
                quadToRelative(72f, 0f, 122f, -50f)
                reflectiveQuadToRelative(50f, -122f)
                quadToRelative(0f, -44f, -16f, -83.5f)
                reflectiveQuadTo(600f, 452f)
                quadToRelative(-11f, -14f, -9f, -31.5f)
                reflectiveQuadToRelative(15f, -26.5f)
                quadToRelative(68f, 43f, 111f, 113.5f)
                reflectiveQuadTo(760f, 560f)
                quadToRelative(0f, 133f, -93.5f, 226.5f)
                reflectiveQuadTo(480f, 880f)
                close()
            }
        }.build()
        return _LocalFireDepartment!!
    }
