package com.rcmiku.music.data.repository

import com.rcmiku.ncmapi.api.playlist.PlaylistApi
import com.rcmiku.ncmapi.model.PlaylistDetailResponse
import com.rcmiku.ncmapi.model.PlaylistInfoResponse
import com.rcmiku.ncmapi.model.Song
import javax.inject.Inject
import javax.inject.Singleton

data class CachedPlaylistEntry(
    val detail: PlaylistDetailResponse,
    val info: PlaylistInfoResponse? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(ttlMillis: Long = PlaylistRepository.DEFAULT_TTL_MS): Boolean =
        System.currentTimeMillis() - timestamp > ttlMillis
}

@Singleton
class PlaylistRepository @Inject constructor() {

    companion object {
        const val DEFAULT_TTL_MS = 5 * 60 * 1000L
        const val MAX_CACHE_SIZE = 50
    }

    private val cache = SimpleLruCache<Long, CachedPlaylistEntry>(MAX_CACHE_SIZE)

    fun getCachedPlaylist(id: Long): CachedPlaylistEntry? {
        return cache.get(id)
    }

    fun putCachedDetail(id: Long, detail: PlaylistDetailResponse) {
        val existing = cache.get(id)
        cache.put(id, CachedPlaylistEntry(detail = detail, info = existing?.info))
    }

    fun putCachedInfo(id: Long, info: PlaylistInfoResponse) {
        val existing = cache.get(id)
        if (existing != null) {
            cache.put(id, existing.copy(info = info))
        }
    }

    suspend fun getPlaylistDetail(
        id: Long,
        limit: Int,
        forceRefresh: Boolean = false
    ): Result<PlaylistDetailResponse> {
        val cached = cache.get(id)
        if (!forceRefresh && cached != null && !cached.isExpired()) {
            return Result.success(cached.detail)
        }
        val result = PlaylistApi.playlistDetail(id = id, limit = limit)
        result.onSuccess { detail ->
            putCachedDetail(id, detail)
        }
        return result
    }

    suspend fun getPlaylistV6DetailEapi(
        id: Long,
        limit: Int
    ): Result<PlaylistDetailResponse> {
        val result = PlaylistApi.playlistV6DetailEapi(id = id, n = limit)
        result.onSuccess { detail ->
            putCachedDetail(id, detail)
        }
        return result
    }

    suspend fun getPlaylistInfo(
        id: Long,
        forceRefresh: Boolean = false
    ): Result<PlaylistInfoResponse> {
        val cached = cache.get(id)
        if (!forceRefresh && cached?.info != null && !cached.isExpired()) {
            return Result.success(cached.info)
        }
        val result = PlaylistApi.playlistInfo(id)
        result.onSuccess { info ->
            putCachedInfo(id, info)
        }
        return result
    }

    fun updateTracks(id: Long, tracks: List<Song>) {
        val existing = cache.get(id) ?: return
        val currentPlaylist = existing.detail.playlist
        val updatedPlaylist = currentPlaylist.copy(
            tracks = tracks,
            trackCount = tracks.size
        )
        val updatedDetail = existing.detail.copy(playlist = updatedPlaylist)
        cache.put(id, existing.copy(detail = updatedDetail))
    }

    fun updateSubscribed(id: Long, subscribed: Boolean) {
        val existing = cache.get(id) ?: return
        val currentPlaylist = existing.detail.playlist
        val newSubCount = if (subscribed) {
            currentPlaylist.subscribedCount + 1
        } else {
            (currentPlaylist.subscribedCount - 1).coerceAtLeast(0)
        }
        val updatedPlaylist = currentPlaylist.copy(
            subscribed = subscribed,
            subscribedCount = newSubCount
        )
        val updatedDetail = existing.detail.copy(playlist = updatedPlaylist)
        val updatedInfo = existing.info?.copy(
            subscribed = subscribed,
            playlist = updatedPlaylist
        )
        cache.put(id, existing.copy(detail = updatedDetail, info = updatedInfo))
    }

    fun invalidate(id: Long) {
        cache.remove(id)
    }

    fun clear() {
        cache.clear()
    }
}
