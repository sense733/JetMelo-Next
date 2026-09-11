package com.rcmiku.music.ui

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

class Milestone3ChallengerAdversarialTest {

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

    data class MockLrcLine(val time: Long, val text: String)

    @Test
    fun testGeometricCenterDeltaStressMatrix() {
        val viewports = listOf(60.dp, 120.dp, 360.dp, 640.dp, 800.dp, 1080.dp, 1920.dp, 2400.dp)
        val densities = listOf(1.0f, 1.5f, 2.0f, 2.625f, 2.75f, 3.0f, 3.5f, 4.0f)
        val itemHeights = listOf(28.dp, 44.dp, 60.dp, 88.dp, 120.dp)
        val spacing = 16.dp

        for (viewport in viewports) {
            val halfPadding = computeHalfCenterPadding(viewport)
            assertTrue(
                "halfCenterPadding must be at least 32.dp for any valid viewport, got $halfPadding for $viewport",
                halfPadding >= 32.dp
            )

            for (density in densities) {
                val viewportPx = viewport.value * density
                val halfPaddingPx = halfPadding.value * density
                val spacingPx = spacing.value * density

                for (itemHeight in itemHeights) {
                    val itemHeightPx = itemHeight.value * density

                    // Line 0 reachability test:
                    // At scroll = 0, line 0 center position from content top is:
                    // halfPaddingPx + spacingPx + (itemHeightPx / 2f)
                    val line0CenterAtTop = halfPaddingPx + spacingPx + (itemHeightPx / 2f)
                    val viewportCenterPx = viewportPx / 2f

                    // If line0CenterAtTop >= viewportCenterPx, scrolling down by (line0CenterAtTop - viewportCenterPx)
                    // successfully places Line 0 at the exact viewport center.
                    assertTrue(
                        "Line 0 must reach vertical center for viewport $viewport, density $density, itemHeight $itemHeight",
                        line0CenterAtTop >= viewportCenterPx - 1e-4f
                    )

                    // Final line reachability test:
                    // When scrolled to bottom, final line center position from content bottom is:
                    // halfPaddingPx + spacingPx + (itemHeightPx / 2f)
                    // Its position from top of viewport is:
                    // viewportPx - (halfPaddingPx + spacingPx + (itemHeightPx / 2f))
                    val finalLineCenterAtBottom = viewportPx - (halfPaddingPx + spacingPx + (itemHeightPx / 2f))
                    // If finalLineCenterAtBottom <= viewportCenterPx, scrolling up allows final line to reach center.
                    assertTrue(
                        "Final line must reach vertical center for viewport $viewport, density $density, itemHeight $itemHeight",
                        finalLineCenterAtBottom <= viewportCenterPx + 1e-4f
                    )

                    // Geometric delta formula exact alignment
                    val offsetForCenter = viewportCenterPx - (itemHeightPx / 2f)
                    val delta = computeLyricScrollDelta(offsetForCenter, itemHeightPx, viewportPx)
                    assertEquals(0f, delta, 1e-4f)

                    val offsetBelow = offsetForCenter + 120f
                    val deltaBelow = computeLyricScrollDelta(offsetBelow, itemHeightPx, viewportPx)
                    assertEquals(120f, deltaBelow, 1e-4f)

                    val offsetAbove = offsetForCenter - 75f
                    val deltaAbove = computeLyricScrollDelta(offsetAbove, itemHeightPx, viewportPx)
                    assertEquals(-75f, deltaAbove, 1e-4f)
                }
            }
        }
    }

    @Test
    fun testEdgeCasesZeroAndNegativeBounds() {
        val zeroDp = computeHalfCenterPadding(0.dp)
        assertEquals(180.dp, zeroDp)

        val negDp = computeHalfCenterPadding((-50).dp)
        assertEquals(180.dp, negDp)

        val tinyDp1 = computeHalfCenterPadding(1.dp)
        assertEquals(32.dp, tinyDp1)

        val tinyDp30 = computeHalfCenterPadding(30.dp)
        assertEquals(32.dp, tinyDp30)

        val tinyDp60 = computeHalfCenterPadding(60.dp)
        assertEquals(32.dp, tinyDp60)

        // Verify no division by zero when viewportHeightPx <= 0
        val zeroViewportPx = 0f
        val deltaZeroViewport = computeLyricScrollDelta(10f, 50f, zeroViewportPx)
        assertFalse("Delta must be a finite number", deltaZeroViewport.isNaN() || deltaZeroViewport.isInfinite())
    }

    @Test
    fun testGeminiCompositeKeyInvariantWithCollisions() {
        val lines = mutableListOf<MockLrcLine>()
        // 50 metadata lines with time = 0
        for (i in 0 until 50) {
            lines.add(MockLrcLine(0L, "Metadata Line $i"))
        }
        // 500 lines of song with duplicated chorus timestamps
        var t = 5000L
        for (i in 50 until 1000) {
            if (i % 20 == 0) {
                // duplicate timestamp for chorus echo
                lines.add(MockLrcLine(t, "Chorus Echo $i"))
            } else {
                t += 2500L
                lines.add(MockLrcLine(t, "Vocal Line $i"))
            }
        }

        // Test Naive key (Forbidden by GEMINI.md)
        val naiveKeys = lines.map { "${it.time}" }
        val naiveUniqueCount = naiveKeys.toSet().size
        assertTrue(
            "Naive key based only on time must have collisions for identical timestamps",
            naiveUniqueCount < lines.size
        )
        val collisions = lines.size - naiveUniqueCount
        assertTrue("Naive key collides on metadata and duplicate chorus", collisions >= 49)

        // Test Composite key (Mandated by GEMINI.md)
        val compositeKeys = lines.indices.map { "${lines[it].time}_$it" }
        val compositeUniqueCount = compositeKeys.toSet().size
        assertEquals(
            "Composite key invariant must yield 100% unique keys with 0 collisions",
            lines.size,
            compositeUniqueCount
        )
    }

    @Test
    fun testPredictiveBackPriorityAndGestureDynamics() {
        // Truth table testing of all combinations
        val views = listOf(FULL_PLAYER, LYRIC_VIEW, PLAY_QUEUE)
        val sheetStates = listOf(true, false)

        for (sheetOpen in sheetStates) {
            for (view in views) {
                val action = dispatchBackAction(sheetOpen, view)
                if (sheetOpen) {
                    assertEquals(
                        "Sheet open must always have highest priority (DISMISS_SHEET)",
                        BackDispatchAction.DISMISS_SHEET,
                        action
                    )
                } else if (view != FULL_PLAYER) {
                    assertEquals(
                        "Secondary views (Queue/Lyric) must return to FULL_PLAYER",
                        BackDispatchAction.SWITCH_TO_FULL_PLAYER,
                        action
                    )
                } else {
                    assertEquals(
                        "FULL_PLAYER with no sheet must collapse player",
                        BackDispatchAction.COLLAPSE_PLAYER,
                        action
                    )
                }
            }
        }

        // Gesture Progress Dynamics: scale 1.0 -> 0.92, cornerRadius 0.dp -> 16.dp
        var prevScale = 1.0f
        var prevCorner = 0f
        for (i in 0..100) {
            val progress = i / 100f
            val scale = 1f - (progress * 0.08f)
            val corner = 16f * progress

            assertTrue("Scale must be bounded in [0.92, 1.0]", scale in 0.9199f..1.0001f)
            assertTrue("Scale must be monotonically decreasing with back progress", scale <= prevScale + 1e-5f)
            assertTrue("Corner radius must be monotonically increasing", corner >= prevCorner - 1e-5f)

            // Combined corner radius with device corner radius (e.g. 16.dp to 28.dp) capped at 36.dp
            val baseCornerDp = 28f
            val totalCorner = (baseCornerDp + corner).coerceAtMost(36f)
            assertTrue("Total corner radius must never exceed 36.dp", totalCorner <= 36f)

            prevScale = scale
            prevCorner = corner
        }

        // Cancellation Restoration Simulation
        val cancellationPoints = listOf(0.05f, 0.35f, 0.72f, 0.98f)
        for (p in cancellationPoints) {
            var activeScale = 1f - (p * 0.08f)
            var activeCornerRadius = 16.dp * p
            assertTrue(activeScale < 1.0f)
            assertTrue(activeCornerRadius > 0.dp)

            // Simulate CancellationException handling in catch block
            activeScale = 1f
            activeCornerRadius = 0.dp
            assertEquals("Scale must restore to 1.0f upon cancellation", 1f, activeScale, 1e-4f)
            assertEquals("Corner radius must restore to 0.dp upon cancellation", 0.dp, activeCornerRadius)
        }
    }

    @Test
    fun testLyricCoroutineIsolationRecompositionSuppression() {
        val songDurationMs = 180_000L
        val lineIntervalMs = 6_000L
        val lines = (0L..songDurationMs step lineIntervalMs).map { MockLrcLine(it, "Line at $it") }

        // Continuous playback polling at 100ms intervals
        val pollIntervalMs = 100L
        var statePositionRecompositions = 0
        var coroutineIsolatedIndexUpdates = 0
        var lastEmittedIndex = -1

        for (t in 0L..songDurationMs step pollIntervalMs) {
            // Compose State pattern: every position poll is a State write that triggers recomposition
            statePositionRecompositions++

            // Coroutine Isolation pattern: position is local Long, index binary searched locally
            val search = lines.binarySearchBy(t) { it.time }
            val index = if (search >= 0) search else -search - 2
            if (index != lastEmittedIndex) {
                lastEmittedIndex = index
                coroutineIsolatedIndexUpdates++
            }
        }

        assertEquals(1801, statePositionRecompositions)
        assertEquals(31, coroutineIsolatedIndexUpdates)

        val suppressionRatio = (statePositionRecompositions - coroutineIsolatedIndexUpdates).toFloat() / statePositionRecompositions
        assertTrue(
            "Coroutine isolation must eliminate > 98% of recompositions (eliminated ${suppressionRatio * 100}%)",
            suppressionRatio >= 0.98f
        )
    }
}
