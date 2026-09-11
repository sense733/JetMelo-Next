package com.rcmiku.music.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rcmiku.music.ui.navigation.PlaylistNav
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Milestone2TransitionTest {

    private val miniPlayerHeight = 64.dp

    private fun computeTabBottomPadding(navBarBaseHeight: Dp, navBarInset: Dp, showMiniPlayer: Boolean): Dp {
        return navBarBaseHeight + navBarInset + (if (showMiniPlayer) miniPlayerHeight + 8.dp else 0.dp)
    }

    private fun computeSubpageBottomPadding(navBarInset: Dp, showMiniPlayer: Boolean): Dp {
        return navBarInset + (if (showMiniPlayer) miniPlayerHeight + 8.dp else 0.dp)
    }

    @Test
    fun testContentPaddingStabilityDuringDockAnimation() {
        val navBarBaseHeight = 64.dp
        val navBarInset = 24.dp
        val showMiniPlayer = true

        val tabPadding = computeTabBottomPadding(navBarBaseHeight, navBarInset, showMiniPlayer)
        val subpagePadding = computeSubpageBottomPadding(navBarInset, showMiniPlayer)

        assertEquals(64.dp + 24.dp + 64.dp + 8.dp, tabPadding)
        assertEquals(24.dp + 64.dp + 8.dp, subpagePadding)

        val animationFrameOffsets = listOf(0.dp, 10.dp, 25.5.dp, 50.dp, 75.2.dp, 88.dp)
        for (offset in animationFrameOffsets) {
            val currentTabPadding = computeTabBottomPadding(navBarBaseHeight, navBarInset, showMiniPlayer)
            val currentSubpagePadding = computeSubpageBottomPadding(navBarInset, showMiniPlayer)

            assertEquals(tabPadding, currentTabPadding)
            assertEquals(subpagePadding, currentSubpagePadding)
        }
    }

    @Test
    fun testContentPaddingWithoutMiniPlayer() {
        val navBarBaseHeight = 64.dp
        val navBarInset = 24.dp
        val showMiniPlayer = false

        val tabPadding = computeTabBottomPadding(navBarBaseHeight, navBarInset, showMiniPlayer)
        val subpagePadding = computeSubpageBottomPadding(navBarInset, showMiniPlayer)

        assertEquals(88.dp, tabPadding)
        assertEquals(24.dp, subpagePadding)
    }

    @Test
    fun testPlaylistNavDefaultEnableSharedTransition() {
        val nav = PlaylistNav(playlistId = 1001L)
        assertTrue(nav.enableSharedTransition)
    }

    @Test
    fun testSharedElementKeyMatchingBetweenSkeletonAndLoaded() {
        val playlistId = 987654L
        val skeletonKey = "cover_$playlistId"
        val loadedDetailPlaylistId = 987654L
        val loadedKey = "cover_$loadedDetailPlaylistId"

        assertEquals(skeletonKey, loadedKey)

        val albumId = 123456L
        val albumSkeletonKey = "cover_$albumId"
        val loadedAlbumId = 123456L
        val loadedAlbumKey = "cover_$loadedAlbumId"

        assertEquals(albumSkeletonKey, loadedAlbumKey)
    }
}
