package com.rcmiku.ncmapi.api.album

import com.rcmiku.ncmapi.model.AlbumDetailResponse
import com.rcmiku.ncmapi.model.AlbumDetailDynamicResponse
import com.rcmiku.ncmapi.model.AlbumInfoResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json

import com.rcmiku.ncmapi.model.GeneralResponse

object AlbumApi {
    suspend fun albumDetail(id: Long): Result<AlbumDetailResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/v1/album/$id",
                data = emptyMap(),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(AlbumDetailResponse.serializer(), body)
        }
    }

    suspend fun albumInfo(id: Long): Result<AlbumInfoResponse> {
        return runCatching {
            val detail = albumDetail(id).getOrThrow()

            val dynamic = runCatching {
                val dynamicBody = HttpManager.request(
                    url = "/api/album/detail/dynamic",
                    data = mapOf("id" to id.toString()),
                    crypto = HttpManager.CryptoType.WEAPI
                )
                json.decodeFromString(AlbumDetailDynamicResponse.serializer(), dynamicBody)
            }.getOrNull()

            AlbumInfoResponse(
                album = detail.album,
                songs = detail.songs,
                isSub = dynamic?.isSub ?: false
            )
        }
    }

    suspend fun albumSub(id: Long, targetState: Boolean): Result<GeneralResponse> {
        return runCatching {
            val url = when {
                targetState -> "/api/album/sub"
                else -> "/api/album/unsub"
            }
            val body = HttpManager.request(
                url = url,
                data = mapOf("id" to id.toString()),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(GeneralResponse.serializer(), body)
        }
    }
}
