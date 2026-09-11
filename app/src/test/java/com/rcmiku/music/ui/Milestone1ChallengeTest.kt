package com.rcmiku.music.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Milestone1ChallengeTest {

    private class SimpleFrameClock(private val stepNanos: Long = 16_666_667L) : MonotonicFrameClock {
        private var currentNanos = 0L
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            currentNanos += stepNanos
            return onFrame(currentNanos)
        }
    }

    private fun shouldApplyScaffoldBlur(progress: Float, sdkInt: Int): Boolean {
        return progress > 0.05f && progress < 1f && sdkInt >= 31
    }

    private fun calculateBlurRadius(progress: Float): Float {
        return progress * 12f
    }

    @Test
    fun testScaffoldBlurEdgeCases() {
        // Condition: p > 0.05f && p < 1f && Build.VERSION.SDK_INT >= 31

        // 1. User drags sheet to intermediate states (0.5f, 0.9f) on SDK 31+
        assertTrue("Blur must be active at p = 0.5f", shouldApplyScaffoldBlur(0.5f, 31))
        assertEquals(6.0f, calculateBlurRadius(0.5f), 1e-4f)

        assertTrue("Blur must be active at p = 0.9f", shouldApplyScaffoldBlur(0.9f, 31))
        assertEquals(10.8f, calculateBlurRadius(0.9f), 1e-4f)

        // 2. Boundary: exactly 1.0f (fully expanded steady state)
        assertFalse(
            "Blur must be bypassed at exact 1.0f to eliminate offscreen RenderNode overhead",
            shouldApplyScaffoldBlur(1.0f, 31)
        )

        // 3. Boundary: near 1.0f (e.g. 0.999f while settling or dragging)
        assertTrue("Blur must remain active just below 1.0f", shouldApplyScaffoldBlur(0.999f, 31))

        // 4. Boundary: exactly 0.05f
        assertFalse(
            "Blur must NOT be active at exact 0.05f (threshold cutoff)",
            shouldApplyScaffoldBlur(0.05f, 31)
        )

        // 5. Boundary: just above 0.05f
        assertTrue("Blur activates immediately above 0.05f", shouldApplyScaffoldBlur(0.0501f, 31))

        // 6. Boundary: 0.0f (collapsed miniplayer)
        assertFalse("Blur must be inactive at 0.0f", shouldApplyScaffoldBlur(0.0f, 31))

        // 7. SDK compatibility check: Android 11 and below (SDK < 31) must never trigger RenderEffect blur
        assertFalse("Blur must not activate on SDK 30 even with 0.5f", shouldApplyScaffoldBlur(0.5f, 30))
        assertFalse("Blur must not activate on SDK 28", shouldApplyScaffoldBlur(0.9f, 28))
    }

    @Test
    fun testComposeTweenWithDisabledAnimationScale0x() = runBlocking {
        val zeroScale = object : MotionDurationScale {
            override val scaleFactor: Float get() = 0f
        }
        val clock = SimpleFrameClock()

        var animatedValues = mutableListOf<Float>()

        withContext(zeroScale + clock) {
            val animatable = Animatable(0f)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400)
            ) {
                animatedValues.add(value)
            }
            // With 0.0x scale factor, animation completes on the first frame at the target value
            assertEquals(1f, animatable.value, 1e-4f)
        }

        // Must complete safely without crashing or hanging
        assertTrue("Animation with 0x scale must complete", animatedValues.isNotEmpty())
        assertEquals(1f, animatedValues.last(), 1e-4f)
        // Verified: 0.0x scale finishes in exactly 1 frame without dividing by zero!
        assertEquals("0.0x animation finishes in exactly 1 frame", 1, animatedValues.size)
    }

    @Test
    fun testComposeTweenWithExtendedAnimationScale2x() = runBlocking {
        val doubleScale = object : MotionDurationScale {
            override val scaleFactor: Float get() = 2f
        }
        val clock = SimpleFrameClock()

        var animatedValues = mutableListOf<Float>()

        withContext(doubleScale + clock) {
            val animatable = Animatable(0f)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400)
            ) {
                animatedValues.add(value)
            }
            assertEquals(1f, animatable.value, 1e-4f)
        }

        // With 2.0x scale, 400ms duration becomes 800ms, running ~48 frames at 16.6ms/frame
        assertTrue("Animation with 2x scale must complete smoothly across multiple frames", animatedValues.size > 20)
        assertEquals(1f, animatedValues.last(), 1e-4f)
    }

    @Test
    fun testTargetBasedAnimationScaleHandling() {
        val anim = TargetBasedAnimation(
            animationSpec = tween<Float>(durationMillis = 400),
            typeConverter = Float.VectorConverter,
            initialValue = 0f,
            targetValue = 1f
        )

        // At 0ms -> initial
        assertEquals(0f, anim.getValueFromNanos(0L), 1e-4f)
        // At 200ms -> intermediate
        val mid = anim.getValueFromNanos(200_000_000L)
        assertTrue("Midpoint must be strictly between 0 and 1", mid in 0.001f..0.999f)
        // At 400ms -> 1f
        assertEquals(1f, anim.getValueFromNanos(400_000_000L), 1e-4f)
        // Beyond duration -> clamp to 1f
        assertEquals(1f, anim.getValueFromNanos(800_000_000L), 1e-4f)
    }
}
