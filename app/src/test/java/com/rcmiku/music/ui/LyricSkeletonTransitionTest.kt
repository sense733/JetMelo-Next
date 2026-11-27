package com.rcmiku.music.ui

import com.rcmiku.music.ui.components.LyricDisplayState
import com.rcmiku.music.ui.components.computeLyricLineRevealTransform
import com.rcmiku.music.ui.components.computeSkeletonDissolveAlpha
import com.rcmiku.music.ui.components.resolveLyricDisplayState
import com.rcmiku.music.ui.components.shouldPlayLyricSkeletonAnimation
import com.rcmiku.music.utils.LrcLine
import com.rcmiku.music.viewModel.ParsedLyric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricSkeletonTransitionTest {

    @Test
    fun resolveLyricDisplayState_distinguishesLoadingContentEmptyAndError() {
        assertEquals(
            LyricDisplayState.EMPTY,
            resolveLyricDisplayState(activeMusicId = null, activeParsedLyric = null, lyricError = null)
        )
        assertEquals(
            LyricDisplayState.LOADING,
            resolveLyricDisplayState(activeMusicId = 10L, activeParsedLyric = null, lyricError = null)
        )
        assertEquals(
            LyricDisplayState.ERROR,
            resolveLyricDisplayState(
                activeMusicId = 10L,
                activeParsedLyric = null,
                lyricError = IllegalStateException("network error")
            )
        )
        assertEquals(
            LyricDisplayState.EMPTY,
            resolveLyricDisplayState(
                activeMusicId = 10L,
                activeParsedLyric = ParsedLyric(musicId = 10L, lines = emptyList(), translations = emptyList()),
                lyricError = null
            )
        )
        assertEquals(
            LyricDisplayState.CONTENT,
            resolveLyricDisplayState(
                activeMusicId = 10L,
                activeParsedLyric = ParsedLyric(
                    musicId = 10L,
                    lines = listOf(LrcLine(time = 0L, text = "Hello")),
                    translations = listOf(null)
                ),
                lyricError = null
            )
        )
    }

    @Test
    fun shouldPlayLyricSkeletonAnimation_neverPlaysOutsideActiveLyricPageOrWhenStopped() {
        assertTrue(
            shouldPlayLyricSkeletonAnimation(
                isLoading = true,
                isLyricActive = true,
                isLifecycleStarted = true
            )
        )
        assertFalse(
            shouldPlayLyricSkeletonAnimation(
                isLoading = true,
                isLyricActive = false,
                isLifecycleStarted = true
            )
        )
        assertFalse(
            shouldPlayLyricSkeletonAnimation(
                isLoading = true,
                isLyricActive = true,
                isLifecycleStarted = false
            )
        )
        assertFalse(
            shouldPlayLyricSkeletonAnimation(
                isLoading = false,
                isLyricActive = true,
                isLifecycleStarted = true
            )
        )
    }

    @Test
    fun computeLyricLineRevealTransform_staggersFromFocalLineAndSettlesCleanly() {
        val focalAtStart = computeLyricLineRevealTransform(progress = 0f, lineIndex = 5, anchorIndex = 5)
        assertEquals(0f, focalAtStart.alpha, 1e-4f)
        assertEquals(1f, focalAtStart.translationYMultiplier, 1e-4f)
        assertEquals(0.94f, focalAtStart.scale, 1e-4f)

        val focalMid = computeLyricLineRevealTransform(progress = 0.25f, lineIndex = 5, anchorIndex = 5)
        val neighborMid = computeLyricLineRevealTransform(progress = 0.25f, lineIndex = 7, anchorIndex = 5)
        val distantMid = computeLyricLineRevealTransform(progress = 0.25f, lineIndex = 11, anchorIndex = 5)

        assertTrue("Focal line must reveal ahead of neighbors", focalMid.alpha > neighborMid.alpha)
        assertTrue("Closer neighbor must reveal ahead of distant lines", neighborMid.alpha >= distantMid.alpha)
        assertTrue("Focal line scale must advance ahead of neighbors", focalMid.scale > neighborMid.scale)

        for (index in 0..20) {
            val settled = computeLyricLineRevealTransform(progress = 1f, lineIndex = index, anchorIndex = 5)
            assertEquals(1f, settled.alpha, 1e-4f)
            assertEquals(0f, settled.translationYMultiplier, 1e-4f)
            assertEquals(1f, settled.scale, 1e-4f)
        }
    }

    @Test
    fun computeSkeletonDissolveAlpha_dissolvesAllRowsByCutoff() {
        for (row in 0..7) {
            assertEquals(1f, computeSkeletonDissolveAlpha(progress = 0f, rowIndex = row), 1e-4f)
            assertEquals(0f, computeSkeletonDissolveAlpha(progress = 0.56f, rowIndex = row), 1e-4f)
        }
        val topRowMid = computeSkeletonDissolveAlpha(progress = 0.25f, rowIndex = 0)
        val bottomRowMid = computeSkeletonDissolveAlpha(progress = 0.25f, rowIndex = 7)
        assertTrue("Top rows should dissolve earlier than bottom rows", topRowMid < bottomRowMid)
    }
}
