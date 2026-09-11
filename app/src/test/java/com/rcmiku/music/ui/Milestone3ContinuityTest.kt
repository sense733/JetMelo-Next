package com.rcmiku.music.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rcmiku.music.ui.screen.FULL_PLAYER
import com.rcmiku.music.ui.screen.LYRIC_VIEW
import com.rcmiku.music.ui.screen.PLAY_QUEUE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class Milestone3ContinuityTest {

    private fun computeMiniAlpha(progress: Float): Float {
        return ((0.32f - progress) / 0.32f).coerceIn(0f, 1f)
    }

    private fun computeFullControlsAlpha(progress: Float): Float {
        return ((progress - 0.28f) / 0.52f).coerceIn(0f, 1f)
    }

    private fun computeDefaultFullArtworkRect(
        screenWidthPx: Float,
        screenHeightPx: Float,
        statusBarInsetPx: Float,
        navBarInsetPx: Float,
        densityDpi: Float = 2.75f
    ): Rect {
        val dpToPx: (Float) -> Float = { it * densityDpi }
        val horizontalPaddingPx = dpToPx(40f)
        val availableWidthPx = screenWidthPx - horizontalPaddingPx
        val fullWidthPx = availableWidthPx * 0.88f
        val topBarHeightPx = dpToPx(56f)
        val bottomControlsHeightPx = dpToPx(286f)
        val boxVerticalPaddingPx = dpToPx(24f)

        val parentHeightPx = (screenHeightPx - statusBarInsetPx - navBarInsetPx).coerceAtLeast(0f)
        val maxContentHeightPx = (parentHeightPx - topBarHeightPx - bottomControlsHeightPx - boxVerticalPaddingPx).coerceAtLeast(0f)
        val artSizePx = if (maxContentHeightPx in 1f..<fullWidthPx) maxContentHeightPx else fullWidthPx

        val fullLeftPx = (screenWidthPx - artSizePx) / 2f
        val leftoverPx = (parentHeightPx - (topBarHeightPx + bottomControlsHeightPx + boxVerticalPaddingPx + artSizePx)).coerceAtLeast(0f)
        val gapPx = leftoverPx / 2f
        val fullTopPx = statusBarInsetPx + topBarHeightPx + dpToPx(12f) + gapPx
        return Rect(fullLeftPx, fullTopPx, fullLeftPx + artSizePx, fullTopPx + artSizePx)
    }

    private fun shouldSuppressStalePosition(
        nowMs: Long,
        seekTarget: Long?,
        seekLockUntilMs: Long,
        rawPos: Long
    ): Boolean {
        if (seekTarget == null) return false
        val diff = abs(rawPos - seekTarget)
        return nowMs < seekLockUntilMs && diff > 300L
    }

    private fun isDiscontinuity(currentPos: Float, rawPos: Float): Boolean {
        return abs(rawPos - currentPos) > 1500f
    }

    private fun computeLyricScrollDelta(itemOffset: Float, itemSize: Float, viewportHeight: Float): Float {
        val itemCenter = itemOffset + itemSize / 2f
        val viewportCenter = viewportHeight / 2f
        return itemCenter - viewportCenter
    }

    private fun computeHalfCenterPadding(viewportHeightDp: Dp): Dp {
        return if (viewportHeightDp > 0.dp) {
            (viewportHeightDp / 2f - 30.dp).coerceAtLeast(32.dp)
        } else {
            180.dp
        }
    }

    enum class BackDispatchAction {
        DISMISS_SHEET,
        SWITCH_TO_FULL_PLAYER,
        COLLAPSE_PLAYER
    }

    private fun dispatchBackAction(isSheetOpen: Boolean, currentView: Int): BackDispatchAction {
        return when {
            isSheetOpen -> BackDispatchAction.DISMISS_SHEET
            currentView != FULL_PLAYER -> BackDispatchAction.SWITCH_TO_FULL_PLAYER
            else -> BackDispatchAction.COLLAPSE_PLAYER
        }
    }

    @Test
    fun testPlayerTransformAlphaCurvesContinuity() {
        assertEquals(1f, computeMiniAlpha(0f), 1e-4f)
        assertEquals(0f, computeFullControlsAlpha(0f), 1e-4f)

        assertEquals(0f, computeMiniAlpha(1f), 1e-4f)
        assertEquals(1f, computeFullControlsAlpha(1f), 1e-4f)

        assertEquals(1f, computeFullControlsAlpha(0.80f), 1e-4f)
        assertEquals(1f, computeFullControlsAlpha(0.95f), 1e-4f)

        val miniAt28 = computeMiniAlpha(0.28f)
        val fullAt28 = computeFullControlsAlpha(0.28f)
        assertTrue(miniAt28 > 0f)
        assertEquals(0f, fullAt28, 1e-4f)

        val miniAt30 = computeMiniAlpha(0.30f)
        val fullAt30 = computeFullControlsAlpha(0.30f)
        assertTrue("Mini alpha must be visible at 0.30", miniAt30 > 0f)
        assertTrue("Full controls alpha must be visible at 0.30", fullAt30 > 0f)

        val miniAt32 = computeMiniAlpha(0.32f)
        val fullAt32 = computeFullControlsAlpha(0.32f)
        assertEquals(0f, miniAt32, 1e-4f)
        assertTrue(fullAt32 > 0f)

        var p = 0f
        while (p <= 1.0f) {
            val mini = computeMiniAlpha(p)
            val full = computeFullControlsAlpha(p)
            val combined = maxOf(mini, full)
            assertTrue("No blank card gap allowed at progress=$p", combined > 0f)
            p += 0.005f
        }
    }

    @Test
    fun testPrecomputedGeometricArtworkAnchors() {
        val screenWidth = 1080f
        val screenHeight = 2400f
        val statusBar = 120f
        val navBar = 96f

        val rect = computeDefaultFullArtworkRect(screenWidth, screenHeight, statusBar, navBar)

        val horizontalCenter = (rect.left + rect.right) / 2f
        assertEquals("Artwork must be centered horizontally on screen", screenWidth / 2f, horizontalCenter, 1e-3f)

        val width = rect.right - rect.left
        val height = rect.bottom - rect.top
        assertEquals("Artwork aspect ratio must be 1:1", width, height, 1e-3f)

        assertTrue("Artwork top must be below status bar and top row", rect.top >= statusBar + (56f * 2.75f))
        assertTrue("Artwork bottom must be above bottom controls and nav bar", rect.bottom <= screenHeight - navBar - (286f * 2.75f))
    }

    @Test
    fun testProgressSliderSeekGuardLockAndDiscontinuity() {
        val seekTarget = 45000L
        val seekLockUntilMs = 10600L

        val duringLockStale = shouldSuppressStalePosition(
            nowMs = 10100L,
            seekTarget = seekTarget,
            seekLockUntilMs = seekLockUntilMs,
            rawPos = 1200L
        )
        assertTrue("Stale position must be suppressed during guard lock window", duringLockStale)

        val duringLockSettled = shouldSuppressStalePosition(
            nowMs = 10250L,
            seekTarget = seekTarget,
            seekLockUntilMs = seekLockUntilMs,
            rawPos = 45150L
        )
        assertFalse("Settled position within 300ms tolerance must be accepted", duringLockSettled)

        val afterLockExpired = shouldSuppressStalePosition(
            nowMs = 10700L,
            seekTarget = seekTarget,
            seekLockUntilMs = seekLockUntilMs,
            rawPos = 1200L
        )
        assertFalse("Guard lock must expire and accept position after timeout", afterLockExpired)

        val noSeek = shouldSuppressStalePosition(
            nowMs = 10100L,
            seekTarget = null,
            seekLockUntilMs = seekLockUntilMs,
            rawPos = 1200L
        )
        assertFalse("Normal playback without seek must not suppress position", noSeek)

        assertFalse(isDiscontinuity(currentPos = 5000f, rawPos = 5100f))
        assertFalse(isDiscontinuity(currentPos = 5000f, rawPos = 6400f))
        assertTrue(isDiscontinuity(currentPos = 5000f, rawPos = 7000f))
        assertTrue(isDiscontinuity(currentPos = 50000f, rawPos = 2000f))
    }

    @Test
    fun testLyricViewportCenterAlignmentMath() {
        val viewportHeight = 1200f
        val itemSize = 100f

        val centeredOffset = (viewportHeight / 2f) - (itemSize / 2f)
        val centeredDelta = computeLyricScrollDelta(centeredOffset, itemSize, viewportHeight)
        assertEquals(0f, centeredDelta, 1e-4f)

        val belowCenterOffset = centeredOffset + 150f
        val belowDelta = computeLyricScrollDelta(belowCenterOffset, itemSize, viewportHeight)
        assertEquals(150f, belowDelta, 1e-4f)

        val aboveCenterOffset = centeredOffset - 80f
        val aboveDelta = computeLyricScrollDelta(aboveCenterOffset, itemSize, viewportHeight)
        assertEquals(-80f, aboveDelta, 1e-4f)

        val viewportHeightDp = 800.dp
        val halfPadding = computeHalfCenterPadding(viewportHeightDp)
        assertEquals(370.dp, halfPadding)

        val nominalLineHeight = 60.dp
        val lineZeroCenterFromTop = halfPadding + (nominalLineHeight / 2f)
        assertEquals(viewportHeightDp / 2f, lineZeroCenterFromTop)

        val zeroViewportPadding = computeHalfCenterPadding(0.dp)
        assertEquals(180.dp, zeroViewportPadding)

        val tinyViewportPadding = computeHalfCenterPadding(100.dp)
        assertEquals(32.dp, tinyViewportPadding)
    }

    @Test
    fun testBackHandlerDispatchOrderPriority() {
        assertEquals(
            BackDispatchAction.DISMISS_SHEET,
            dispatchBackAction(isSheetOpen = true, currentView = FULL_PLAYER)
        )
        assertEquals(
            BackDispatchAction.DISMISS_SHEET,
            dispatchBackAction(isSheetOpen = true, currentView = LYRIC_VIEW)
        )
        assertEquals(
            BackDispatchAction.DISMISS_SHEET,
            dispatchBackAction(isSheetOpen = true, currentView = PLAY_QUEUE)
        )

        assertEquals(
            BackDispatchAction.SWITCH_TO_FULL_PLAYER,
            dispatchBackAction(isSheetOpen = false, currentView = LYRIC_VIEW)
        )
        assertEquals(
            BackDispatchAction.SWITCH_TO_FULL_PLAYER,
            dispatchBackAction(isSheetOpen = false, currentView = PLAY_QUEUE)
        )

        assertEquals(
            BackDispatchAction.COLLAPSE_PLAYER,
            dispatchBackAction(isSheetOpen = false, currentView = FULL_PLAYER)
        )

        val scaleAt0 = 1f - (0f * 0.08f)
        val scaleAtHalf = 1f - (0.5f * 0.08f)
        val scaleAt1 = 1f - (1f * 0.08f)
        assertEquals(1.0f, scaleAt0, 1e-4f)
        assertEquals(0.96f, scaleAtHalf, 1e-4f)
        assertEquals(0.92f, scaleAt1, 1e-4f)
    }
}
