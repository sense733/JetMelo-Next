package com.rcmiku.ncmapi.api.artist

import com.rcmiku.ncmapi.model.ArtistVideoResponse
import com.rcmiku.ncmapi.model.NcmApiException
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object ArtistMvApi {
    suspend fun artistVideo(
        artistId: Long,
        size: Int = 20,
        cursor: String = "0",
        order: Int = 0,
        tab: Int = 0,
    ): Result<ArtistVideoResponse> {
        return runCatching {
            val pageParam = buildJsonObject {
                put("size", size)
                put("cursor", cursor)
            }.toString()

            val body = HttpManager.request(
                url = "/api/mlog/artist/video",
                data = mapOf(
                    "artistId" to artistId,
                    "page" to pageParam,
                    "tab" to tab,
                    "order" to order,
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            val root = json.parseToJsonElement(body).jsonObject
            val code = root["code"]?.jsonPrimitive?.intOrNull
                ?: return Result.failure(NcmApiException(-1, "artist video response missing code"))
            if (code != 200) {
                val message = root["message"]?.jsonPrimitive?.contentOrNull ?: "NCM API error code: $code"
                return Result.failure(NcmApiException(code, message))
            }
            json.decodeFromString(ArtistVideoResponse.serializer(), body)
        }
    }
}
