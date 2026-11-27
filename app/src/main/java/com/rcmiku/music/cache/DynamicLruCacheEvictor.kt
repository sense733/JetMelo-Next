package com.rcmiku.music.cache

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import java.util.TreeSet

/**
 * 支持运行期动态调整容量上限的 Media3 LRU 缓存淘汰器。
 */
@UnstableApi
class DynamicLruCacheEvictor(
    @Volatile var maxBytes: Long
) : CacheEvictor {

    private val lock = Any()
    private val leastRecentlyUsed = TreeSet<CacheSpan> { lhs, rhs ->
        val delta = lhs.lastTouchTimestamp - rhs.lastTouchTimestamp
        if (delta == 0L) {
            lhs.compareTo(rhs)
        } else {
            if (delta < 0) -1 else 1
        }
    }
    private var currentSize = 0L

    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() = Unit

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        if (length != -1L) {
            evictCache(cache, length)
        }
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        synchronized(lock) {
            leastRecentlyUsed.add(span)
            currentSize += span.length
        }
        evictCache(cache, 0L)
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        synchronized(lock) {
            leastRecentlyUsed.remove(span)
            currentSize -= span.length
        }
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    fun evictToCurrentLimit(cache: Cache) {
        evictCache(cache, 0L)
    }

    private fun evictCache(cache: Cache, requiredSpace: Long) {
        while (true) {
            val victim = synchronized(lock) {
                if (currentSize + requiredSpace <= maxBytes) {
                    null
                } else {
                    leastRecentlyUsed.firstOrNull()
                }
            } ?: break
            val sizeBefore = synchronized(lock) { currentSize }
            cache.removeSpan(victim)
            val sizeAfter = synchronized(lock) { currentSize }
            if (sizeAfter >= sizeBefore) {
                break
            }
        }
    }
}
