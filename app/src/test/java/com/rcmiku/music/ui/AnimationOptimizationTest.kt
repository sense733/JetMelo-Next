package com.rcmiku.music.ui

import com.rcmiku.music.ui.components.InitialLyricScrollTarget
import com.rcmiku.music.ui.components.resolveInitialLyricScrollTarget
import com.rcmiku.music.ui.components.resolveLyricViewportHeightPx
import com.rcmiku.music.ui.design.computeWashVerticalFeatherAlpha
import com.rcmiku.music.ui.screen.QUANTIZED_BLUR_LEVELS
import com.rcmiku.music.ui.screen.quantizedBlurLevelIndex
import com.rcmiku.music.utils.LrcLine
import com.rcmiku.music.viewModel.matchLyricTranslations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationOptimizationTest {

    @Test
    fun quantizedBlurLevelIndex_respectsBoundariesAndMonotonicity() {
        assertEquals(-1, quantizedBlurLevelIndex(-0.1f))
        assertEquals(-1, quantizedBlurLevelIndex(0f))
        assertEquals(-1, quantizedBlurLevelIndex(0.05f))
        assertEquals(0, quantizedBlurLevelIndex(0.0501f))
        assertEquals(QUANTIZED_BLUR_LEVELS - 1, quantizedBlurLevelIndex(0.999f))
        assertEquals(-1, quantizedBlurLevelIndex(1.0f))
        assertEquals(-1, quantizedBlurLevelIndex(1.2f))

        var previous = -1
        for (step in 51..999 step 10) {
            val idx = quantizedBlurLevelIndex(step / 1000f)
            assertTrue("Index must be within 0..${QUANTIZED_BLUR_LEVELS - 1}", idx in 0 until QUANTIZED_BLUR_LEVELS)
            assertTrue("Index must increase monotonically", idx >= previous)
            previous = idx
        }
    }

    @Test
    fun computeWashVerticalFeatherAlpha_matchesPiecewiseCurveAndClamps() {
        assertEquals(1.0f, computeWashVerticalFeatherAlpha(-0.2f), 1e-4f)
        assertEquals(1.0f, computeWashVerticalFeatherAlpha(0.0f), 1e-4f)
        assertEquals(0.85f, computeWashVerticalFeatherAlpha(0.26f), 1e-4f)
        assertEquals(0.36f, computeWashVerticalFeatherAlpha(0.46f), 1e-4f)
        assertEquals(0.0f, computeWashVerticalFeatherAlpha(0.64f), 1e-4f)
        assertEquals(0.0f, computeWashVerticalFeatherAlpha(1.0f), 1e-4f)
        assertEquals(0.0f, computeWashVerticalFeatherAlpha(1.5f), 1e-4f)

        var previous = 1.0f
        for (step in 1..100) {
            val current = computeWashVerticalFeatherAlpha(step / 100f)
            assertTrue("Feather alpha must never increase downward", current <= previous + 1e-5f)
            assertTrue("Feather alpha must remain within 0..1", current in 0f..1f)
            previous = current
        }
    }

    @Test
    fun matchLyricTranslations_matchesExactAndToleranceAndSkipsBlankLines() {
        val lines = listOf(
            LrcLine(time = 1000L, text = "First line"),
            LrcLine(time = 2000L, text = ""),
            LrcLine(time = 3000L, text = "Tolerance line"),
            LrcLine(time = 4500L, text = "Out of window line")
        )
        val translations = listOf(
            LrcLine(time = 1000L, text = "第一行"),
            LrcLine(time = 2000L, text = "空白不应显示"),
            LrcLine(time = 2920L, text = "远一点的容差"),
            LrcLine(time = 3050L, text = "更近的容差"),
            LrcLine(time = 4650L, text = "超出100ms")
        )

        val matched = matchLyricTranslations(lines, translations)
        assertEquals(4, matched.size)
        assertEquals("第一行", matched[0])
        assertNull(matched[1])
        assertEquals("更近的容差", matched[2])
        assertNull(matched[3])

        val emptyMatched = matchLyricTranslations(lines, emptyList())
        assertEquals(listOf(null, null, null, null), emptyMatched)
    }

    @Test
    fun resolveInitialLyricScrollTarget_handlesZeroMidAndPreStartPositions() {
        assertEquals(
            InitialLyricScrollTarget(itemIndex = 0, scrollOffsetPx = 0),
            resolveInitialLyricScrollTarget(currentIndex = -1, focalAnchorPx = 560)
        )
        assertEquals(
            InitialLyricScrollTarget(itemIndex = 1, scrollOffsetPx = -560),
            resolveInitialLyricScrollTarget(currentIndex = 0, focalAnchorPx = 560)
        )
        assertEquals(
            InitialLyricScrollTarget(itemIndex = 21, scrollOffsetPx = -560),
            resolveInitialLyricScrollTarget(currentIndex = 20, focalAnchorPx = 560)
        )
        assertEquals(
            InitialLyricScrollTarget(itemIndex = 3, scrollOffsetPx = 0),
            resolveInitialLyricScrollTarget(currentIndex = 2, focalAnchorPx = -100)
        )
    }

    @Test
    fun resolveLyricViewportHeightPx_prefersSettledHeightDuringAnimatedHeaderShrink() {
        assertEquals(2096, resolveLyricViewportHeightPx(measuredViewportHeightPx = 1238, settledViewportHeightPx = 2096))
        assertEquals(2120, resolveLyricViewportHeightPx(measuredViewportHeightPx = 2120, settledViewportHeightPx = 2096))
        assertEquals(0, resolveLyricViewportHeightPx(measuredViewportHeightPx = -10, settledViewportHeightPx = -5))
    }
}
