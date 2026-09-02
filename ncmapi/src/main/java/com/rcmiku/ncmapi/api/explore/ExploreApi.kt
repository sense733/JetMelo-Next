package com.rcmiku.ncmapi.api.explore

import com.rcmiku.ncmapi.api.playlist.PlaylistApi
import com.rcmiku.ncmapi.model.NewAlbumResponse
import com.rcmiku.ncmapi.model.TopListResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json

object ExploreApi {
    suspend fun topList(): Result<TopListResponse> = PlaylistApi.topList()
    
    suspend fun newAlbum(limit: Int = 30, offset: Int = 0): Result<NewAlbumResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/album/new",
                data = mapOf(
                    "limit" to limit,
                    "offset" to offset
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(NewAlbumResponse.serializer(), body)
        }
    }
}
