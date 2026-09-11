package com.rcmiku.music.ui

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.snapshots.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Empirical Stress Test and Adversarial Verification Suite for Milestone 3:
 * - R3.1: Fluid Player Transformation & Alpha Curves
 * - R3.2: Progress Slider Micro-Easing, SeekGuardLock, Discontinuity, & Time Text Isolation
 */
class Milestone3ChallengerTest {

    private fun computeMiniAlpha(progress: Float): Float {
        return ((0.32f - progress) / 0.32f).coerceIn(0f, 1f)
    }

    private fun computeFullControlsAlpha(progress: Float): Float {
        return ((progress - 0.28f) / 0.52f).coerceIn(0f, 1f)
    }

    // ---------------------------------------------------------------------------------------------
    // Requirement 1: Alpha Curves 10,000-point Fine Step Verification
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testAlphaCurvesAt10000StepsNeverProduceZeroVisibilityGap() {
        val totalSteps = 10000
        var minCombinedVisibility = Float.MAX_VALUE
        var minProgress = 0f

        for (step in 0..totalSteps) {
            val progress = step.toFloat() / totalSteps.toFloat()
            val mini = computeMiniAlpha(progress)
            val full = computeFullControlsAlpha(progress)
            val combined = maxOf(mini, full)

            assertTrue(
                "Visibility gap found at progress=$progress! Both mini ($mini) and full ($full) are <= 0",
                combined > 0f
            )

            if (combined < minCombinedVisibility) {
                minCombinedVisibility = combined
                minProgress = progress
            }
        }

        // The intersection of ((0.32 - p) / 0.32) = ((p - 0.28) / 0.52) occurs at p = 32/105 ≈ 0.30476.
        // At that point, visibility is ≈ 0.047619 (> 4.7%).
        assertTrue(
            "Minimum visibility along the curve must be >= 4.5% (observed $minCombinedVisibility at $minProgress)",
            minCombinedVisibility >= 0.045f
        )
    }

    @Test
    fun testAlphaCurvesCrossoverSmoothnessAndDerivativeContinuity() {
        val epsilon = 1e-4f
        val crossoverStart = 0.28f
        val crossoverEnd = 0.32f

        // Within the open crossover interval (0.28, 0.32), verify that both functions have constant derivatives
        val samplePoints = listOf(0.285f, 0.290f, 0.295f, 0.300f, 0.305f, 0.310f, 0.315f)
        for (p in samplePoints) {
            val dMini = (computeMiniAlpha(p + epsilon) - computeMiniAlpha(p - epsilon)) / (2f * epsilon)
            val dFull = (computeFullControlsAlpha(p + epsilon) - computeFullControlsAlpha(p - epsilon)) / (2f * epsilon)

            // miniAlpha = (0.32 - p)/0.32 = 1 - p/0.32 => derivative is -1/0.32 = -3.125
            assertEquals("miniAlpha derivative must be constant -3.125", -3.125f, dMini, 1e-2f)

            // fullControlsAlpha = (p - 0.28)/0.52 => derivative is 1/0.52 ≈ 1.923077
            assertEquals("fullControlsAlpha derivative must be constant 1.923", 1.923f, dFull, 1e-2f)

            // Combined derivative is smooth and non-zero
            val dCombined = dMini + dFull
            assertEquals(-1.202f, dCombined, 1e-2f)
        }

        // Boundary continuity checks
        val miniAtStart = computeMiniAlpha(crossoverStart)
        val fullAtStart = computeFullControlsAlpha(crossoverStart)
        assertEquals(0.125f, miniAtStart, 1e-4f)
        assertEquals(0.0f, fullAtStart, 1e-4f)

        val miniAtEnd = computeMiniAlpha(crossoverEnd)
        val fullAtEnd = computeFullControlsAlpha(crossoverEnd)
        assertEquals(0.0f, miniAtEnd, 1e-4f)
        assertEquals(0.04f / 0.52f, fullAtEnd, 1e-4f)
    }

    // ---------------------------------------------------------------------------------------------
    // Requirement 2: SeekGuardLock & Discontinuity Stress Testing
    // ---------------------------------------------------------------------------------------------

    class SeekGuardStateMachine(
        var duration: Long = 180000L,
        var currentAnimPosition: Float = 0f,
        var isPlaying: Boolean = true
    ) {
        var sliderPosition: Long? = null
        var pendingSeekTarget: Long? = null
        var seekLockUntilMs: Long = 0L

        fun startDrag(initialScrubPos: Long) {
            sliderPosition = initialScrubPos
        }

        fun updateDrag(newScrubPos: Long) {
            sliderPosition = newScrubPos
        }

        fun finishDrag(nowMs: Long, seekTarget: Long) {
            sliderPosition = seekTarget
            pendingSeekTarget = seekTarget
            seekLockUntilMs = nowMs + 600L
            currentAnimPosition = seekTarget.toFloat()
            sliderPosition = null
        }

        fun displayedPosition(): Long {
            return sliderPosition ?: currentAnimPosition.toLong()
        }

        enum class ActionTaken {
            SUPPRESSED_STALE,
            RELEASED_LOCK_AND_ANIMATED,
            RELEASED_LOCK_AND_SNAPPED,
            NORMAL_ANIMATE,
            NORMAL_SNAP
        }

        fun processPlaybackSample(nowMs: Long, rawPos: Long): ActionTaken {
            if (sliderPosition != null) {
                return ActionTaken.SUPPRESSED_STALE
            }

            val target = pendingSeekTarget
            if (target != null) {
                val diff = abs(rawPos - target)
                if (nowMs < seekLockUntilMs && diff > 300L) {
                    return ActionTaken.SUPPRESSED_STALE
                } else {
                    pendingSeekTarget = null
                }
            }

            val diff = abs(rawPos.toFloat() - currentAnimPosition)
            return if (diff > 1500f) {
                currentAnimPosition = rawPos.toFloat()
                ActionTaken.NORMAL_SNAP
            } else if (isPlaying) {
                currentAnimPosition = (rawPos + 100L).toFloat().coerceAtMost(duration.toFloat())
                ActionTaken.NORMAL_ANIMATE
            } else {
                currentAnimPosition = rawPos.toFloat()
                ActionTaken.NORMAL_SNAP
            }
        }
    }

    @Test
    fun testRapidScrubbingDoesNotRubberband() {
        val sm = SeekGuardStateMachine(currentAnimPosition = 5000f)
        var simulatedTime = 10000L

        // Step 1: User scrubs to 10s and releases
        sm.startDrag(8000L)
        sm.finishDrag(simulatedTime, 10000L)
        assertEquals(10000L, sm.displayedPosition())

        // ExoPlayer still reports old 5000ms
        var action = sm.processPlaybackSample(simulatedTime + 50L, 5000L)
        assertEquals(SeekGuardStateMachine.ActionTaken.SUPPRESSED_STALE, action)
        assertEquals(10000L, sm.displayedPosition())

        // Step 2: Before 600ms expires, user quickly scrubs again to 25s and releases
        simulatedTime += 200L // 10200ms
        sm.startDrag(15000L)
        assertEquals(15000L, sm.displayedPosition())
        sm.finishDrag(simulatedTime, 25000L)
        assertEquals(25000L, sm.displayedPosition())

        // ExoPlayer now reports 10000ms (the previous seek target, but stale compared to 25s)
        action = sm.processPlaybackSample(simulatedTime + 50L, 10000L)
        assertEquals(SeekGuardStateMachine.ActionTaken.SUPPRESSED_STALE, action)
        assertEquals(25000L, sm.displayedPosition())

        // Step 3: Before 600ms expires, user scrubs to 50s and releases
        simulatedTime += 150L // 10350ms
        sm.startDrag(30000L)
        sm.finishDrag(simulatedTime, 50000L)
        assertEquals(50000L, sm.displayedPosition())

        // ExoPlayer reports intermediate positions (25000ms, 25100ms)
        action = sm.processPlaybackSample(simulatedTime + 100L, 25000L)
        assertEquals(SeekGuardStateMachine.ActionTaken.SUPPRESSED_STALE, action)
        assertEquals(50000L, sm.displayedPosition())

        // Finally ExoPlayer catches up to 50050ms (diff = 50ms <= 300ms)
        action = sm.processPlaybackSample(simulatedTime + 300L, 50050L)
        assertEquals(SeekGuardStateMachine.ActionTaken.NORMAL_ANIMATE, action)
        // Position smoothly continues forward, NO rubberbanding backward!
        assertTrue(sm.displayedPosition() >= 50000L)
    }

    @Test
    fun testEdgeCaseSeekNearStartAndNearEnd() {
        val duration = 200000L
        val sm = SeekGuardStateMachine(duration = duration, currentAnimPosition = 95000f)
        var simulatedTime = 20000L

        // Edge case: Seek to 0ms (beginning of track)
        sm.finishDrag(simulatedTime, 0L)
        assertEquals(0L, sm.displayedPosition())

        // Stale report of 95000ms suppressed
        var action = sm.processPlaybackSample(simulatedTime + 100L, 95000L)
        assertEquals(SeekGuardStateMachine.ActionTaken.SUPPRESSED_STALE, action)
        assertEquals(0L, sm.displayedPosition())

        // Settled report of 50ms accepted (diff = 50ms <= 300ms)
        action = sm.processPlaybackSample(simulatedTime + 250L, 50L)
        assertEquals(SeekGuardStateMachine.ActionTaken.NORMAL_ANIMATE, action)

        // Edge case: Seek to duration - 50ms (near track end)
        simulatedTime += 1000L
        val nearEndTarget = duration - 50L
        sm.finishDrag(simulatedTime, nearEndTarget)
        assertEquals(nearEndTarget, sm.displayedPosition())

        // Stale report of 1000ms suppressed
        action = sm.processPlaybackSample(simulatedTime + 100L, 1000L)
        assertEquals(SeekGuardStateMachine.ActionTaken.SUPPRESSED_STALE, action)
        assertEquals(nearEndTarget, sm.displayedPosition())

        // Settled report of duration - 100ms (diff = 50ms <= 300ms) accepted
        action = sm.processPlaybackSample(simulatedTime + 300L, duration - 100L)
        assertEquals(SeekGuardStateMachine.ActionTaken.NORMAL_ANIMATE, action)
    }

    @Test
    fun testStalePositionTimeoutFallbackPreventsPermanentFreeze() {
        val sm = SeekGuardStateMachine(currentAnimPosition = 10000f)
        val seekTime = 10000L
        sm.finishDrag(seekTime, 80000L)

        // For first 600ms, stale reports (>300ms diff) are strictly suppressed
        for (offset in 50L..550L step 100L) {
            val action = sm.processPlaybackSample(seekTime + offset, 10000L)
            assertEquals(SeekGuardStateMachine.ActionTaken.SUPPRESSED_STALE, action)
            assertEquals(80000L, sm.displayedPosition())
        }

        // At 601ms+, the 600ms guard lock expires and allows fallback to avoid permanent lock
        val expiredAction = sm.processPlaybackSample(seekTime + 650L, 10000L)
        // Since diff is 70000 > 1500, it snaps without rubberband animation
        assertEquals(SeekGuardStateMachine.ActionTaken.NORMAL_SNAP, expiredAction)
        assertEquals(10000L, sm.displayedPosition())
    }

    @Test
    fun testDiscontinuityThresholdSnapVsAnimate() {
        val sm = SeekGuardStateMachine(currentAnimPosition = 5000f, isPlaying = true)

        // Within 1500ms diff -> Linear animation (no snap)
        val normalStep = sm.processPlaybackSample(1000L, 5100L)
        assertEquals(SeekGuardStateMachine.ActionTaken.NORMAL_ANIMATE, normalStep)

        // Track change or large skip: diff > 1500ms -> Instantaneous Snap
        val skipStep = sm.processPlaybackSample(1100L, 20000L)
        assertEquals(SeekGuardStateMachine.ActionTaken.NORMAL_SNAP, skipStep)
        assertEquals(20000L, sm.displayedPosition())
    }

    // ---------------------------------------------------------------------------------------------
    // Requirement 3: Time String Isolation Verification
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testTimeStringDerivedStateProducesZeroChangesWithinSecond() {
        val positionState = mutableLongStateOf(1000L)
        val currentSecond by derivedStateOf {
            (positionState.longValue / 1000L).coerceAtLeast(0L)
        }

        // Verify initial reading
        assertEquals(1L, currentSecond)

        val observedSeconds = mutableListOf<Long>()
        // Simulate high-frequency 120Hz slider updates across 1 whole second (1000ms to 1999ms)
        // 125 frames at 8ms interval
        for (stepMs in 1000L..1999L step 8L) {
            Snapshot.withMutableSnapshot {
                positionState.longValue = stepMs
            }
            // Read the derived state at each animation frame
            val sec = currentSecond
            observedSeconds.add(sec)
        }

        // Assert all 125 frames yielded exactly 1L without a single change
        assertEquals(125, observedSeconds.size)
        assertTrue("Every single sample within second 1 must be 1L", observedSeconds.all { it == 1L })

        // Cross into next second at 2000ms
        Snapshot.withMutableSnapshot {
            positionState.longValue = 2000L
        }
        assertEquals(2L, currentSecond)
    }
}
