package com.rcmiku.ncmapi.api.artist

import com.rcmiku.ncmapi.model.ArtistAlbumResponse
import com.rcmiku.ncmapi.model.ArtistDescResponse
import com.rcmiku.ncmapi.model.ArtistHeadInfoResponse
import com.rcmiku.ncmapi.model.ArtistTopSong
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json

object ArtistApi {
    suspend fun artistHeadInfo(id: Long): Result<ArtistHeadInfoResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/artist/head/info/get",
                data = mapOf(
                    "id" to id.toString(),
                    "t" to (System.currentTimeMillis() / 1000).toString(),
                    "header" to "{}",
                    "e_r" to false
                ),
                crypto = HttpManager.CryptoType.EAPI
            )
            json.decodeFromString(ArtistHeadInfoResponse.serializer(), body)
        }
    }

    suspend fun artistDesc(id: Long): Result<ArtistDescResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/artist/introduction",
                data = mapOf("id" to id.toString()),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(ArtistDescResponse.serializer(), body)
        }
    }
    
    suspend fun artistTopSong(id: Long): Result<ArtistTopSong> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/artist/top/song",
                data = mapOf("id" to id.toString()),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(ArtistTopSong.serializer(), body)
        }
    }
    
    suspend fun artistAlbum(id: Long, limit: Int = 30, offset: Int = 0): Result<ArtistAlbumResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/artist/albums/$id",
                data = mapOf(
                    "limit" to limit.toString(),
                    "offset" to offset.toString(),
                    "total" to "true"
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(ArtistAlbumResponse.serializer(), body)
        }
    }
}
