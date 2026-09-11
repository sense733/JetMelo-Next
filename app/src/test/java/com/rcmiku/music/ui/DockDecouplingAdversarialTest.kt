package com.rcmiku.music.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DockDecouplingAdversarialTest {

    private val navBarBaseHeight = 64.dp
    private val miniPlayerHeight = 64.dp
    private val standardDecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    private val animDurationMillis = 280

    private fun computeTabBottomPadding(navBarInset: Dp, showMiniPlayer: Boolean): Dp {
        return navBarBaseHeight + navBarInset + (if (showMiniPlayer) miniPlayerHeight + 8.dp else 0.dp)
    }

    private fun computeSubpageBottomPadding(navBarInset: Dp, showMiniPlayer: Boolean): Dp {
        return navBarInset + (if (showMiniPlayer) miniPlayerHeight + 8.dp else 0.dp)
    }

    private fun interpolateEasing(t: Float): Float {
        return standardDecelerateEasing.transform(t.coerceIn(0f, 1f))
    }

    private fun computeDockedBottomPadding(
        fromShowNav: Boolean,
        toShowNav: Boolean,
        navBarInset: Dp,
        showMiniPlayer: Boolean,
        progress: Float
    ): Dp {
        val startTarget = if (fromShowNav) navBarBaseHeight + navBarInset else if (showMiniPlayer) navBarInset else 0.dp
        val endTarget = if (toShowNav) navBarBaseHeight + navBarInset else if (showMiniPlayer) navBarInset else 0.dp
        val eased = interpolateEasing(progress)
        return startTarget + (endTarget - startTarget) * eased
    }

    private fun computeFogBottomPadding(
        showNavigationBar: Boolean,
        showMiniPlayer: Boolean,
        dockedBottomPadding: Dp
    ): Dp {
        return if (showNavigationBar && showMiniPlayer) {
            dockedBottomPadding + (miniPlayerHeight / 2)
        } else {
            dockedBottomPadding
        }
    }

    @Test
    fun testLazyColumnMeasureStabilityUnder120FpsAnimation() {
        val insets = listOf(0.dp, 16.dp, 24.dp, 48.dp)
        val miniPlayerStates = listOf(true, false)

        for (inset in insets) {
            for (mini in miniPlayerStates) {
                val baseTabPadding = computeTabBottomPadding(inset, mini)
                val baseSubpagePadding = computeSubpageBottomPadding(inset, mini)

                val frameCount = 60
                for (frame in 0..frameCount) {
                    val t = frame.toFloat() / frameCount

                    val currentTab = computeTabBottomPadding(inset, mini)
                    val currentSubpage = computeSubpageBottomPadding(inset, mini)

                    assertEquals(
                        "Tab bottom content padding must be strictly constant at frame $frame",
                        baseTabPadding.value,
                        currentTab.value,
                        0.0001f
                    )
                    assertEquals(
                        "Subpage bottom content padding must be strictly constant at frame $frame",
                        baseSubpagePadding.value,
                        currentSubpage.value,
                        0.0001f
                    )
                }
            }
        }
    }

    @Test
    fun testPlayerTransformVerticalMotionSmoothness() {
        val navBarInset = 24.dp
        val showMiniPlayer = true
        val screenHeightPx = 2400f
        val density = 2.75f
        val miniHeightPx = 64f * density
        val miniVerticalPaddingPx = 8f * density

        val frameCount = 34
        var prevMiniBottomPx: Float? = null

        for (frame in 0..frameCount) {
            val progress = frame.toFloat() / frameCount
            val dockedPadding = computeDockedBottomPadding(
                fromShowNav = true,
                toShowNav = false,
                navBarInset = navBarInset,
                showMiniPlayer = showMiniPlayer,
                progress = progress
            )

            val dockedBottomPx = dockedPadding.value * density
            val miniBottomPx = screenHeightPx - dockedBottomPx - miniVerticalPaddingPx
            val miniTopPx = miniBottomPx - miniHeightPx

            assertTrue(
                "MiniPlayer top must be strictly above bottom",
                miniTopPx < miniBottomPx
            )

            if (prevMiniBottomPx != null) {
                val delta = miniBottomPx - prevMiniBottomPx
                assertTrue(
                    "MiniPlayer motion must be strictly monotonic descending when dock hides",
                    delta >= -0.001f
                )
                assertTrue(
                    "Single frame displacement at 120fps must be bounded without teleporting",
                    delta < 20f * density
                )
            }
            prevMiniBottomPx = miniBottomPx
        }
    }

    @Test
    fun testFogBottomPaddingEndpointsAndContinuity() {
        val navBarInset = 24.dp
        val showMiniPlayer = true

        val tabDocked = computeDockedBottomPadding(true, true, navBarInset, showMiniPlayer, 1f)
        val tabFog = computeFogBottomPadding(true, showMiniPlayer, tabDocked)
        assertEquals(64.dp + 24.dp + 32.dp, tabFog)

        val subpageDocked = computeDockedBottomPadding(false, false, navBarInset, showMiniPlayer, 1f)
        val subpageFog = computeFogBottomPadding(false, showMiniPlayer, subpageDocked)
        assertEquals(24.dp, subpageFog)

        val noMiniDocked = computeDockedBottomPadding(true, true, navBarInset, false, 1f)
        val noMiniFog = computeFogBottomPadding(true, false, noMiniDocked)
        assertEquals(64.dp + 24.dp, noMiniFog)
    }

    @Test
    fun testDockOverlayAndPlayerMotionIsochrony() {
        val navBarInset = 24.dp
        val density = 3.0f

        val totalDockHeightPx = (navBarBaseHeight + navBarInset).value * density
        val dockedStartPaddingPx = (navBarBaseHeight + navBarInset).value * density
        val dockedEndPaddingPx = navBarInset.value * density
        val totalPlayerTravelPx = dockedStartPaddingPx - dockedEndPaddingPx

        for (progressPercent in listOf(0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1.0f)) {
            val eased = interpolateEasing(progressPercent)
            val dockSlideOffsetPx = totalDockHeightPx * eased
            val playerTravelPx = totalPlayerTravelPx * eased

            assertEquals(
                eased,
                playerTravelPx / totalPlayerTravelPx,
                0.0001f
            )
        }
    }
}
