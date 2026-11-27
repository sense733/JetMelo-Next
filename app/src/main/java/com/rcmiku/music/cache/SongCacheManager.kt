package com.rcmiku.music.cache

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.SimpleCache
import com.rcmiku.ncmapi.api.player.SongLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * 歌曲音频磁盘缓存上限档位。
 */
enum class SongCacheLimit(val maxBytes: Long, val label: String) {
    GB_1(1L * 1024L * 1024L * 1024L, "1 GB"),
    GB_2(2L * 1024L * 1024L * 1024L, "2 GB"),
    GB_3(3L * 1024L * 1024L * 1024L, "3 GB"),
    GB_5(5L * 1024L * 1024L * 1024L, "5 GB")
}

/**
 * 管理全局唯一的 Media3 [SimpleCache] 实例，负责歌曲音频分片缓存、动态配额淘汰与清理。
 */
@UnstableApi
object SongCacheManager {

    private const val SONG_CACHE_DIR = "songs"

    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cache: SimpleCache? = null

    @Volatile
    private var evictor: DynamicLruCacheEvictor? = null

    private var databaseProvider: StandaloneDatabaseProvider? = null

    fun getCache(context: Context, initialLimit: SongCacheLimit = SongCacheLimit.GB_1): SimpleCache {
        cache?.let { return it }
        return synchronized(lock) {
            cache?.let { return@synchronized it }
            val appContext = context.applicationContext
            val cacheDir = File(appContext.cacheDir, SONG_CACHE_DIR).apply { mkdirs() }
            val dbProvider = databaseProvider ?: StandaloneDatabaseProvider(appContext).also {
                databaseProvider = it
            }
            val lruEvictor = DynamicLruCacheEvictor(initialLimit.maxBytes).also {
                evictor = it
            }
            SimpleCache(cacheDir, lruEvictor, dbProvider).also {
                cache = it
            }
        }
    }

    fun buildCacheKey(rawUri: String, audioQuality: SongLevel): String {
        return "${rawUri}_${audioQuality.name}"
    }

    fun updateLimit(context: Context, limit: SongCacheLimit) {
        val activeCache = getCache(context, limit)
        evictor?.maxBytes = limit.maxBytes
        scope.launch {
            evictor?.evictToCurrentLimit(activeCache)
        }
    }

    fun getUsedBytes(context: Context): Long {
        return getCache(context).cacheSpace
    }

    fun clearAll(context: Context) {
        val activeCache = getCache(context)
        val keys = activeCache.keys.toList()
        for (key in keys) {
            activeCache.removeResource(key)
        }
    }
}
