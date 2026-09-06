package com.rcmiku.music.data.repository

import com.rcmiku.ncmapi.api.album.AlbumApi
import com.rcmiku.ncmapi.model.AlbumDetailResponse
import com.rcmiku.ncmapi.model.AlbumInfoResponse
import javax.inject.Inject
import javax.inject.Singleton

data class CachedAlbumEntry(
    val detail: AlbumDetailResponse,
    val info: AlbumInfoResponse? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(ttlMillis: Long = AlbumRepository.DEFAULT_TTL_MS): Boolean =
        System.currentTimeMillis() - timestamp > ttlMillis
}

@Singleton
class AlbumRepository @Inject constructor() {

    companion object {
        const val DEFAULT_TTL_MS = 5 * 60 * 1000L
        const val MAX_CACHE_SIZE = 50
    }

    private val cache = SimpleLruCache<Long, CachedAlbumEntry>(MAX_CACHE_SIZE)

    fun getCachedAlbum(id: Long): CachedAlbumEntry? {
        return cache.get(id)
    }

    fun putCachedDetail(id: Long, detail: AlbumDetailResponse) {
        val existing = cache.get(id)
        cache.put(id, CachedAlbumEntry(detail = detail, info = existing?.info))
    }

    fun putCachedInfo(id: Long, info: AlbumInfoResponse) {
        val existing = cache.get(id)
        if (existing != null) {
            cache.put(id, existing.copy(info = info))
        }
    }

    suspend fun getAlbumDetail(id: Long, forceRefresh: Boolean = false): Result<AlbumDetailResponse> {
        val cached = cache.get(id)
        if (!forceRefresh && cached != null && !cached.isExpired()) {
            return Result.success(cached.detail)
        }
        val result = AlbumApi.albumDetail(id)
        result.onSuccess { detail ->
            putCachedDetail(id, detail)
        }
        return result
    }

    suspend fun getAlbumInfo(id: Long, forceRefresh: Boolean = false): Result<AlbumInfoResponse> {
        val cached = cache.get(id)
        if (!forceRefresh && cached?.info != null && !cached.isExpired()) {
            return Result.success(cached.info)
        }
        val result = AlbumApi.albumInfo(id)
        result.onSuccess { info ->
            putCachedInfo(id, info)
        }
        return result
    }

    fun updateSubscribed(id: Long, isSub: Boolean) {
        val existing = cache.get(id) ?: return
        val updatedInfo = existing.info?.copy(isSub = isSub)
        cache.put(id, existing.copy(info = updatedInfo))
    }

    fun invalidate(id: Long) {
        cache.remove(id)
    }

    fun clear() {
        cache.clear()
    }
}
