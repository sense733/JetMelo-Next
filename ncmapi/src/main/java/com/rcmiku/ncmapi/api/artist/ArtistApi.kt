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
            // ref: api-enhanced-main module/artist_detail.js => /api/artist/head/info/get
            val body = HttpManager.request(
                url = "/api/artist/head/info/get",
                data = mapOf("id" to id.toString()),
                crypto = HttpManager.CryptoType.EAPI
            )
            json.decodeFromString(ArtistHeadInfoResponse.serializer(), body)
        }
    }

    suspend fun artistDesc(id: Long): Result<ArtistDescResponse> {
        return runCatching {
            // ref: api-enhanced-main module/artist_desc.js => /api/artist/introduction
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
            // ref: api-enhanced-main module/artist_top_song.js => /api/artist/top/song
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
            // ref: api-enhanced-main module/artist_album.js => /api/artist/albums/{id}
            val body = HttpManager.request(
                url = "/api/artist/albums/$id",
                data = mapOf(
                    "limit" to limit,
                    "offset" to offset,
                    "total" to true
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(ArtistAlbumResponse.serializer(), body)
        }
    }
}
