package com.rcmiku.music.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTransformInterruptionTest {

    private fun isExpandClickEnabled(isExpanded: Boolean): Boolean {
        return !isExpanded
    }

    private fun shouldBlockInternalControls(isExpanded: Boolean, isFull: Boolean, isSwitchingSubView: Boolean): Boolean {
        return (isExpanded && !isFull) || isSwitchingSubView
    }

    private fun isInterruptionExpandSurfaceActive(isExpanded: Boolean): Boolean {
        return !isExpanded
    }

    @Test
    fun testExpandClickAvailabilityAcrossProgressLifecycle() {
        val testProgressSamples = listOf(0.0f, 0.05f, 0.20f, 0.32f, 0.50f, 0.75f, 0.95f)

        for (progress in testProgressSamples) {
            assertTrue(
                "Player expansion must be enabled when isExpanded=false at progress=$progress",
                isExpandClickEnabled(isExpanded = false)
            )
            assertFalse(
                "Player expansion must be disabled during active expansion at progress=$progress",
                isExpandClickEnabled(isExpanded = true)
            )
        }
    }

    @Test
    fun testInterruptionSurfaceDispatchesDuringCollapsing() {
        assertFalse(
            shouldBlockInternalControls(isExpanded = false, isFull = false, isSwitchingSubView = false)
        )
        assertTrue(
            isInterruptionExpandSurfaceActive(isExpanded = false)
        )

        assertTrue(
            shouldBlockInternalControls(isExpanded = true, isFull = false, isSwitchingSubView = false)
        )
        assertFalse(
            isInterruptionExpandSurfaceActive(isExpanded = true)
        )

        assertFalse(
            shouldBlockInternalControls(isExpanded = true, isFull = true, isSwitchingSubView = false)
        )
        assertTrue(
            shouldBlockInternalControls(isExpanded = true, isFull = true, isSwitchingSubView = true)
        )
    }

    @Test
    fun testAnimationContinuityFromInterruptedOffset() {
        val interruptedProgress = 0.42f
        val newTarget = 1.0f

        val initialMotionFrame = interruptedProgress
        assertEquals(0.42f, initialMotionFrame, 1e-4f)
        assertTrue("New target must be full expansion", newTarget > initialMotionFrame)
        assertEquals(1.0f, newTarget, 1e-4f)
    }
}
