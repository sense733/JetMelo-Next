package com.rcmiku.music.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.media3.common.Player
import com.rcmiku.music.ui.components.PlayMode
import com.rcmiku.music.ui.components.nextPlayMode
import com.rcmiku.music.ui.components.resolvePlayMode
import com.rcmiku.music.ui.design.PaletteExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PlayerAccentRefinementTest {

    private fun saturationOf(color: Color): Float {
        val maxC = max(color.red, max(color.green, color.blue))
        val minC = min(color.red, min(color.green, color.blue))
        val d = maxC - minC
        if (d <= 1e-6f) return 0f
        val l = (maxC + minC) * 0.5f
        return if (l > 0.5f) d / (2f - maxC - minC) else d / (maxC + minC)
    }

    @Test
    fun refinePlayerAccent_lightTheme_producesRestrainedColorAndWhiteGlyphSupport() {
        val lightSurface = Color(0xFFFAF9FB)
        val veryDarkRed = Color(0xFF2B0305)
        val oliveYellow = Color(0xFFA8A032)
        val neonBlue = Color(0xFF002BFF)

        val refinedRed = PaletteExtractor.refinePlayerAccent(
            candidate = veryDarkRed,
            ambientFallback = veryDarkRed,
            background = lightSurface,
            isDarkTheme = false,
            fallback = Color(0xFF6750A4)
        )
        val refinedYellow = PaletteExtractor.refinePlayerAccent(
            candidate = oliveYellow,
            ambientFallback = oliveYellow,
            background = lightSurface,
            isDarkTheme = false,
            fallback = Color(0xFF6750A4)
        )
        val refinedBlue = PaletteExtractor.refinePlayerAccent(
            candidate = neonBlue,
            ambientFallback = neonBlue,
            background = lightSurface,
            isDarkTheme = false,
            fallback = Color(0xFF6750A4)
        )

        listOf(refinedRed, refinedYellow, refinedBlue).forEach { refined ->
            assertTrue("Light accent saturation must stay <= 0.50f", saturationOf(refined) <= 0.50f)
            assertTrue("Light accent luminance must stay below 0.50f for white glyph", refined.luminance() < 0.50f)
            assertEquals(Color.White, PaletteExtractor.getAccessibleTextColor(refined))
        }
    }

    @Test
    fun refinePlayerAccent_darkTheme_producesSoftPastelAccent() {
        val darkBackground = Color(0xFF121216)
        val deepEmerald = Color(0xFF005A2B)

        val refined = PaletteExtractor.refinePlayerAccent(
            candidate = deepEmerald,
            ambientFallback = deepEmerald,
            background = darkBackground,
            isDarkTheme = true,
            fallback = Color(0xFFD0BCFF)
        )

        assertTrue("Dark theme accent should be luminous enough to read cleanly", refined.luminance() >= 0.35f)
        assertTrue("Dark theme accent saturation should stay restrained", saturationOf(refined) <= 0.52f)
    }

    @Test
    fun refinePlayerAccent_monochromeCover_producesCharcoalAndSoftGrayWithoutColorCast() {
        val lightSurface = Color(0xFFFAF9FB)
        val darkBackground = Color(0xFF121216)
        val gray = Color(0xFF707070)

        val lightNeutral = PaletteExtractor.refinePlayerAccent(
            candidate = gray,
            ambientFallback = gray,
            background = lightSurface,
            isDarkTheme = false,
            fallback = Color(0xFF4FB3BF)
        )
        val darkNeutral = PaletteExtractor.refinePlayerAccent(
            candidate = gray,
            ambientFallback = gray,
            background = darkBackground,
            isDarkTheme = true,
            fallback = Color(0xFF4FB3BF)
        )

        assertEquals(Color(0xFF2C2C30), lightNeutral)
        assertEquals(Color(0xFFE6E6EA), darkNeutral)
        assertTrue(abs(lightNeutral.red - lightNeutral.blue) < 0.05f)
    }

    @Test
    fun playMode_resolveAndCycle_enforcesMutualExclusion() {
        assertEquals(PlayMode.LOOP_ALL, resolvePlayMode(Player.REPEAT_MODE_ALL, false))
        assertEquals(PlayMode.LOOP_ALL, resolvePlayMode(Player.REPEAT_MODE_OFF, false))
        assertEquals(PlayMode.SHUFFLE, resolvePlayMode(Player.REPEAT_MODE_ALL, true))
        assertEquals(PlayMode.SHUFFLE, resolvePlayMode(Player.REPEAT_MODE_OFF, true))
        assertEquals(PlayMode.REPEAT_ONE, resolvePlayMode(Player.REPEAT_MODE_ONE, false))
        assertEquals(PlayMode.REPEAT_ONE, resolvePlayMode(Player.REPEAT_MODE_ONE, true))

        assertEquals(PlayMode.SHUFFLE, nextPlayMode(PlayMode.LOOP_ALL))
        assertEquals(PlayMode.REPEAT_ONE, nextPlayMode(PlayMode.SHUFFLE))
        assertEquals(PlayMode.LOOP_ALL, nextPlayMode(PlayMode.REPEAT_ONE))
    }
}
