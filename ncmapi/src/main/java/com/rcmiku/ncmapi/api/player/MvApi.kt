package com.rcmiku.ncmapi.api.player

import com.rcmiku.ncmapi.model.MvUrlResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json

object MvApi {

    private val VALID_RESOLUTIONS = setOf(240, 360, 480, 720, 1080)

    suspend fun mvUrl(id: Long, r: Int = 1080): Result<MvUrlResponse> {
        return runCatching {
            require(id > 0) { "MV ID must be greater than 0" }
            require(r in VALID_RESOLUTIONS) { "Unsupported resolution: $r. Must be one of $VALID_RESOLUTIONS" }

            val body = HttpManager.request(
                url = "/weapi/song/enhance/play/mv/url",
                data = mapOf(
                    "id" to id.toString(),
                    "r" to r.toString()
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            val resp = json.decodeFromString(MvUrlResponse.serializer(), body)
            if (resp.code != 200) {
                error("mvUrl failed with response code ${resp.code}")
            }
            resp
        }
    }
}
