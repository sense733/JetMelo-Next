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
            // ref: api-enhanced-main module/album.js => /api/v1/album/{id}
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

            // ref: api-enhanced-main module/album_detail_dynamic.js => /api/album/detail/dynamic
            val dynamicBody = HttpManager.request(
                url = "/api/album/detail/dynamic",
                data = mapOf("id" to id.toString()),
                crypto = HttpManager.CryptoType.WEAPI
            )
            val dynamic = json.decodeFromString(AlbumDetailDynamicResponse.serializer(), dynamicBody)

            AlbumInfoResponse(
                album = detail.album,
                songs = detail.songs,
                isSub = dynamic.isSub
            )
        }
    }

    suspend fun albumSub(id: Long, targetState: Boolean): Result<GeneralResponse> {
        return runCatching {
            // ref: api-enhanced-main module/album_sub.js => /api/album/sub|unsub
            val action = if (targetState) "sub" else "unsub"
            val body = HttpManager.request(
                url = "/api/album/$action",
                data = mapOf("id" to id.toString()),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(GeneralResponse.serializer(), body)
        }
    }
}
