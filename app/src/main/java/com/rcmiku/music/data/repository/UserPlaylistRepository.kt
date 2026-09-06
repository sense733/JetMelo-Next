package com.rcmiku.music.data.repository

import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.account.UserPlaylistType
import com.rcmiku.ncmapi.model.UserPlaylistResponse
import javax.inject.Inject
import javax.inject.Singleton

data class UserPlaylistKey(val userId: Long, val type: String)

data class CachedUserPlaylistEntry(
    val response: UserPlaylistResponse,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(ttlMillis: Long = UserPlaylistRepository.DEFAULT_TTL_MS): Boolean =
        System.currentTimeMillis() - timestamp > ttlMillis
}

@Singleton
class UserPlaylistRepository @Inject constructor() {

    companion object {
        const val DEFAULT_TTL_MS = 5 * 60 * 1000L
        const val MAX_CACHE_SIZE = 20
    }

    private val cache = SimpleLruCache<UserPlaylistKey, CachedUserPlaylistEntry>(MAX_CACHE_SIZE)

    fun getCachedUserPlaylist(userId: Long, type: String): UserPlaylistResponse? {
        return cache.get(UserPlaylistKey(userId, type))?.response
    }

    fun putCachedUserPlaylist(userId: Long, type: String, response: UserPlaylistResponse) {
        cache.put(UserPlaylistKey(userId, type), CachedUserPlaylistEntry(response))
    }

    suspend fun getUserPlaylist(
        userId: Long,
        userPlaylistType: UserPlaylistType,
        forceRefresh: Boolean = false
    ): Result<UserPlaylistResponse> {
        val key = UserPlaylistKey(userId, userPlaylistType.type)
        val cached = cache.get(key)
        if (!forceRefresh && cached != null && !cached.isExpired()) {
            return Result.success(cached.response)
        }
        val result = AccountApi.userPlaylist(userId = userId, userPlaylistType = userPlaylistType)
        result.onSuccess { response ->
            cache.put(key, CachedUserPlaylistEntry(response))
        }
        return result
    }

    fun invalidate(userId: Long, type: String? = null) {
        if (type != null) {
            cache.remove(UserPlaylistKey(userId, type))
        } else {
            cache.snapshot().keys.filter { it.userId == userId }.forEach {
                cache.remove(it)
            }
        }
    }

    fun clear() {
        cache.clear()
    }
}
