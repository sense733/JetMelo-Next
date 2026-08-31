package com.rcmiku.music.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val UserRound: ImageVector
    get() {
        if (_UserRound != null) {
            return _UserRound!!
        }
        _UserRound = ImageVector.Builder(
            name = "UserRound",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color(0xFF000000))) {
                moveTo(480f, 480f)
                quadToRelative(-66f, 0f, -113f, -47f)
                reflectiveQuadToRelative(-47f, -113f)
                quadToRelative(0f, -66f, 47f, -113f)
                reflectiveQuadToRelative(113f, -47f)
                quadToRelative(66f, 0f, 113f, 47f)
                reflectiveQuadToRelative(47f, 113f)
                quadToRelative(0f, 66f, -47f, 113f)
                reflectiveQuadToRelative(-113f, 47f)
                close()
                moveTo(480f, 400f)
                quadToRelative(33f, 0f, 56.5f, -23.5f)
                reflectiveQuadTo(560f, 320f)
                quadToRelative(0f, -33f, -23.5f, -56.5f)
                reflectiveQuadTo(480f, 240f)
                quadToRelative(-33f, 0f, -56.5f, 23.5f)
                reflectiveQuadTo(400f, 320f)
                quadToRelative(0f, 33f, 23.5f, 56.5f)
                reflectiveQuadTo(480f, 400f)
                close()
                moveTo(480f, 880f)
                quadToRelative(-140f, 0f, -243f, -64.5f)
                reflectiveQuadTo(100f, 640f)
                quadToRelative(0f, -43f, 24.5f, -79.5f)
                reflectiveQuadToRelative(65.5f, -56.5f)
                quadToRelative(67f, -32f, 141.5f, -48f)
                reflectiveQuadTo(480f, 440f)
                quadToRelative(74f, 0f, 148.5f, 16f)
                reflectiveQuadToRelative(141.5f, 48f)
                quadToRelative(41f, 20f, 65.5f, 56.5f)
                reflectiveQuadTo(860f, 640f)
                quadToRelative(0f, 111f, -103f, 175.5f)
                reflectiveQuadTo(480f, 880f)
                close()
            }
        }.build()

        return _UserRound!!
    }

@Suppress("ObjectPropertyName")
private var _UserRound: ImageVector? = null