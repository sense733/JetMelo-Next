package com.rcmiku.ncmapi.api.recommend

import com.rcmiku.ncmapi.model.DailySongsResponse
import com.rcmiku.ncmapi.model.NewAlbumResponse
import com.rcmiku.ncmapi.model.PersonalizedPlaylistResponse
import com.rcmiku.ncmapi.model.RecommendPlaylistResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json
import android.util.Log

object RecommendApi {
    private const val TAG = "NcmApiParse"

    suspend fun dailySongs(): Result<DailySongsResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/v3/discovery/recommend/songs",
                data = emptyMap(),
                crypto = HttpManager.CryptoType.WEAPI
            )
            runCatching {
                json.decodeFromString(DailySongsResponse.serializer(), body)
            }.getOrElse { e ->
                Log.w(TAG, "decode failed: /api/v3/discovery/recommend/songs bodyPrefix=${body.take(400)}", e)
                throw e
            }
        }
    }

    suspend fun recommendSongs(): Result<DailySongsResponse> {
        return dailySongs()
    }

    suspend fun newAlbum(): Result<NewAlbumResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/discovery/newAlbum",
                data = emptyMap(),
                crypto = HttpManager.CryptoType.WEAPI
            )
            runCatching {
                json.decodeFromString(NewAlbumResponse.serializer(), body)
            }.getOrElse { e ->
                Log.w(TAG, "decode failed: /api/discovery/newAlbum bodyPrefix=${body.take(400)}", e)
                throw e
            }
        }
    }
    
    suspend fun personalizedPlaylist(limit: Int = 30): Result<PersonalizedPlaylistResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/personalized/playlist",
                data = mapOf(
                    "limit" to limit
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            runCatching {
                json.decodeFromString(PersonalizedPlaylistResponse.serializer(), body)
            }.getOrElse { e ->
                Log.w(TAG, "decode failed: /api/personalized/playlist bodyPrefix=${body.take(400)}", e)
                throw e
            }
        }
    }
    
    suspend fun recommendPlaylist(): Result<RecommendPlaylistResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/v1/discovery/recommend/resource",
                data = emptyMap(),
                crypto = HttpManager.CryptoType.WEAPI
            )
            runCatching {
                json.decodeFromString(RecommendPlaylistResponse.serializer(), body)
            }.getOrElse { e ->
                Log.w(TAG, "decode failed: /api/v1/discovery/recommend/resource bodyPrefix=${body.take(400)}", e)
                throw e
            }
        }
    }
}
