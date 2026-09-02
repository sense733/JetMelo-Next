package com.rcmiku.ncmapi.api.player

import com.rcmiku.ncmapi.model.MvUrlResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json

object MvApi {
    suspend fun mvUrl(id: Long, r: Int = 1080): Result<MvUrlResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/song/enhance/play/mv/url",
                data = mapOf(
                    "id" to id,
                    "r" to r
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(MvUrlResponse.serializer(), body)
        }
    }
}
