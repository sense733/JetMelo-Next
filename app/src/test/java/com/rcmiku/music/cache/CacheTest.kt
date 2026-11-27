package com.rcmiku.music.cache

import com.rcmiku.ncmapi.api.player.SongLevel
import com.rcmiku.ncmapi.model.LyricData
import com.rcmiku.ncmapi.model.LyricResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files

class CacheTest {

    @Test
    fun songCacheLimitValuesMatchExpectedGigabytes() {
        val gb = 1024L * 1024L * 1024L
        assertEquals(1L * gb, SongCacheLimit.GB_1.maxBytes)
        assertEquals(2L * gb, SongCacheLimit.GB_2.maxBytes)
        assertEquals(3L * gb, SongCacheLimit.GB_3.maxBytes)
        assertEquals(5L * gb, SongCacheLimit.GB_5.maxBytes)
    }

    @Test
    fun buildCacheKeyIncludesUriAndAudioQuality() {
        assertEquals(
            "123456_STANDARD",
            SongCacheManager.buildCacheKey("123456", SongLevel.STANDARD)
        )
        assertEquals(
            "123456_999_LOSSLESS",
            SongCacheManager.buildCacheKey("123456_999", SongLevel.LOSSLESS)
        )
    }

    @Test
    fun lyricFileStorePersistsReadsAndClears() {
        val tempDir = Files.createTempDirectory("lyric-cache-test").toFile()
        try {
            val store = LyricFileStore(tempDir, maxFiles = 2)
            val sample = LyricResponse(
                lrc = LyricData(lyric = "[00:00.00]Hello"),
                tlyric = LyricData(lyric = "[00:00.00]你好")
            )
            store.put(1001L, sample)
            val loaded = store.get(1001L)
            assertNotNull(loaded)
            assertEquals("[00:00.00]Hello", loaded?.lrc?.lyric)
            assertEquals("[00:00.00]你好", loaded?.tlyric?.lyric)

            store.put(1002L, sample)
            store.put(1003L, sample)
            val remainingCount = tempDir.listFiles { f -> f.name.endsWith(".json") }?.size ?: 0
            assertEquals(2, remainingCount)

            store.clear()
            assertNull(store.get(1003L))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
