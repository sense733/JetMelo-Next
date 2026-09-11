package com.rcmiku.music.data.repository

import com.rcmiku.ncmapi.api.account.UserPlaylistType
import com.rcmiku.ncmapi.model.Album
import com.rcmiku.ncmapi.model.AlbumDetailResponse
import com.rcmiku.ncmapi.model.AlbumInfoResponse
import com.rcmiku.ncmapi.model.Playlist
import com.rcmiku.ncmapi.model.PlaylistDetailResponse
import com.rcmiku.ncmapi.model.PlaylistInfoResponse
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.UserPlaylistResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryTest {

    @Test
    fun testSimpleLruCacheEviction() {
        val cache = SimpleLruCache<Int, String>(3)
        cache.put(1, "one")
        cache.put(2, "two")
        cache.put(3, "three")

        assertEquals(3, cache.size())
        assertEquals("one", cache.get(1))
        cache.put(4, "four")

        assertNotNull(cache.get(1))
        assertNull(cache.get(2))
        assertNotNull(cache.get(3))
        assertNotNull(cache.get(4))
    }

    @Test
    fun testPlaylistRepositoryCacheAndMutation() = runBlocking {
        val repo = PlaylistRepository()

        val song1 = Song(id = 101, name = "Song 1", ar = emptyList())
        val song2 = Song(id = 102, name = "Song 2", ar = emptyList())
        val initialPlaylist = Playlist(
            id = 1,
            name = "My Playlist",
            tracks = listOf(song1, song2),
            trackCount = 2,
            subscribed = false,
            subscribedCount = 10
        )
        val initialDetail = PlaylistDetailResponse(playlist = initialPlaylist)
        val initialInfo = PlaylistInfoResponse(playlist = initialPlaylist, subscribed = false)

        repo.putCachedDetail(1L, initialDetail)
        repo.putCachedInfo(1L, initialInfo)

        val cached = repo.getCachedPlaylist(1L)
        assertNotNull(cached)
        assertEquals(2, cached?.detail?.playlist?.tracks?.size)
        assertFalse(cached?.detail?.playlist?.subscribed ?: true)

        val song3 = Song(id = 103, name = "Song 3", ar = emptyList())
        repo.updateTracks(1L, listOf(song1, song2, song3))

        val updatedCached = repo.getCachedPlaylist(1L)
        assertEquals(3, updatedCached?.detail?.playlist?.tracks?.size)
        assertEquals(3, updatedCached?.detail?.playlist?.trackCount)

        repo.updateSubscribed(1L, true)
        val subCached = repo.getCachedPlaylist(1L)
        assertTrue(subCached?.detail?.playlist?.subscribed ?: false)
        assertEquals(11L, subCached?.detail?.playlist?.subscribedCount)
        assertTrue(subCached?.info?.subscribed ?: false)

        repo.invalidate(1L)
        assertNull(repo.getCachedPlaylist(1L))
    }

    @Test
    fun testAlbumRepositoryCacheAndMutation() {
        val repo = AlbumRepository()

        val album = Album(id = 100, name = "Test Album")
        val detail = AlbumDetailResponse(album = album, songs = listOf(Song(id = 1, name = "A", ar = emptyList())))
        val info = AlbumInfoResponse(album = album, isSub = false)

        repo.putCachedDetail(100L, detail)
        repo.putCachedInfo(100L, info)

        val cached = repo.getCachedAlbum(100L)
        assertNotNull(cached)
        assertEquals("Test Album", cached?.detail?.album?.name)
        assertFalse(cached?.info?.isSub ?: true)

        repo.updateSubscribed(100L, true)
        val updated = repo.getCachedAlbum(100L)
        assertTrue(updated?.info?.isSub ?: false)

        repo.invalidate(100L)
        assertNull(repo.getCachedAlbum(100L))
    }

    @Test
    fun testUserPlaylistRepositoryCacheAndInvalidate() {
        val repo = UserPlaylistRepository()

        val playlists = listOf(Playlist(id = 1, name = "Fav"))
        val response = UserPlaylistResponse(playlist = playlists)

        repo.putCachedUserPlaylist(999L, UserPlaylistType.CREATE.type, response)

        val cached = repo.getCachedUserPlaylist(999L, UserPlaylistType.CREATE.type)
        assertNotNull(cached)
        assertEquals(1, cached?.effectivePlaylists?.size)

        repo.invalidate(999L, UserPlaylistType.CREATE.type)
        assertNull(repo.getCachedUserPlaylist(999L, UserPlaylistType.CREATE.type))
    }
}
