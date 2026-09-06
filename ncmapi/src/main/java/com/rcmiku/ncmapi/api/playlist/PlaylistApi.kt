package com.rcmiku.ncmapi.api.playlist

import android.util.Log
import com.rcmiku.ncmapi.model.GeneralResponse
import com.rcmiku.ncmapi.model.PlaylistDetailResponse
import com.rcmiku.ncmapi.model.PlaylistInfoResponse
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.TopListResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.jsonObject

object PlaylistApi {
    private const val TAG = "PlaylistApi"
    private const val DEFAULT_TRACK_LIMIT = 1000
    private const val MAX_DETAIL_LIMIT = 1000

    suspend fun playlistDetail(id: Long): Result<PlaylistDetailResponse> {
        return playlistV3Detail(id)
    }

    suspend fun playlistDetail(id: Long, limit: Int): Result<PlaylistDetailResponse> {
        return playlistV6DetailEapi(id = id, n = limit, s = 5)
    }

    suspend fun playlistV3Detail(id: Long): Result<PlaylistDetailResponse> {
        val v3Result = runCatching {
            if (HttpManager.debugLogEnabled) Log.w(TAG, "playlistV3Detail request id=$id")
            val body = HttpManager.request(
                url = "/weapi/v3/playlist/detail",
                data = mapOf(
                    "id" to id.toString(),
                    "n" to MAX_DETAIL_LIMIT.toString(),
                    "s" to "8"
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(PlaylistDetailResponse.serializer(), body)
        }
        if (v3Result.isSuccess) {
            return v3Result
        }
        val v3Exception = v3Result.exceptionOrNull() ?: Exception("playlistV3Detail failed")
        if (HttpManager.debugLogEnabled) Log.w(TAG, "playlistV3Detail failed, fallback to v6. id=$id", v3Exception)
        return playlistV6Detail(id).fold(
            onSuccess = { Result.success(it) },
            onFailure = { v6Exception ->
                v3Exception.addSuppressed(v6Exception)
                Result.failure(v3Exception)
            }
        )
    }

    suspend fun playlistV6Detail(id: Long): Result<PlaylistDetailResponse> {
        return runCatching {
            if (HttpManager.debugLogEnabled) Log.w(TAG, "playlistV6Detail request id=$id")
            val body = HttpManager.request(
                url = "/api/v6/playlist/detail",
                data = mapOf(
                    "id" to id.toString(),
                    "n" to MAX_DETAIL_LIMIT.toString(),
                    "s" to "8"
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(PlaylistDetailResponse.serializer(), body)
        }
    }

    suspend fun playlistV6DetailEapi(id: Long, n: Int = DEFAULT_TRACK_LIMIT, s: Int = 5): Result<PlaylistDetailResponse> {
        return runCatching {
            if (HttpManager.debugLogEnabled) Log.w(TAG, "playlistV6DetailEapi request id=$id n=$n s=$s")
            val body = HttpManager.request(
                url = "/api/v6/playlist/detail",
                data = mapOf(
                    "id" to id.toString(),
                    "n" to n.toString(),
                    "s" to s.toString(),
                    "t" to (System.currentTimeMillis() / 1000).toString(),
                    "header" to "{}",
                    "e_r" to false
                ),
                crypto = HttpManager.CryptoType.EAPI
            )
            json.decodeFromString(PlaylistDetailResponse.serializer(), body)
        }
    }

    suspend fun playlistSub(id: Long, targetState: Boolean): Result<GeneralResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/weapi/playlist/subscribe",
                data = mapOf(
                    "id" to id.toString(),
                    "t" to (if (targetState) "1" else "2")
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(GeneralResponse.serializer(), body)
        }
    }

    suspend fun playlistTracksManipulate(
        op: String,
        pid: Long,
        trackIds: List<Long>
    ): Result<GeneralResponse> {
        return runCatching {
            require(op == "add" || op == "del") { "unsupported op: $op" }
            require(trackIds.isNotEmpty()) { "trackIds must not be empty" }
            val tracksParam = "[" + trackIds.joinToString(",") + "]"
            val body = HttpManager.request(
                url = "/weapi/playlist/manipulate/tracks",
                data = mapOf(
                    "op" to op,
                    "pid" to pid.toString(),
                    "trackIds" to tracksParam
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(GeneralResponse.serializer(), body)
        }
    }

    suspend fun topList(): Result<TopListResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/toplist",
                data = emptyMap(),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(TopListResponse.serializer(), body)
        }
    }
    
    suspend fun playlistInfo(id: Long): Result<PlaylistInfoResponse> {
         return runCatching {
             val detail = playlistDetail(id).getOrThrow()
             PlaylistInfoResponse(playlist = detail.playlist)
         }
    }

    suspend fun playlistTrackAll(id: Long, limit: Int = DEFAULT_TRACK_LIMIT, offset: Int = 0, s: Int = 8): Result<PlaylistDetailResponse> {
        return runCatching {
            val detail = playlistV3Detail(id).getOrThrow()
            val trackIds = detail.playlist.trackIds
            if (trackIds.isEmpty()) {
                return@runCatching detail
            }

            val slice = trackIds.drop(offset).take(limit)
            if (slice.isEmpty()) {
                return@runCatching detail.copy(playlist = detail.playlist.copy(tracks = emptyList()))
            }
            val c = "[" + slice.joinToString(",") { "{\"id\":${it.id}}" } + "]"
            val body = HttpManager.request(
                url = "/weapi/v3/song/detail",
                data = mapOf(
                    "c" to c
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )

            val root = json.parseToJsonElement(body).jsonObject
            val songsJson = root["songs"]
            val songs = if (songsJson != null) {
                json.decodeFromJsonElement(ListSerializer(Song.serializer()), songsJson)
            } else {
                emptyList()
            }

            val songMap = songs.associateBy { it.id }
            val orderedSongs = slice.mapNotNull { songMap[it.id] }

            detail.copy(playlist = detail.playlist.copy(tracks = orderedSongs))
        }
    }
}
