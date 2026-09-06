package com.rcmiku.ncmapi.api.homepage

import android.util.Log
import com.rcmiku.ncmapi.model.HomepageBlockResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json

object HomepageApi {
    private const val TAG = "HomepageApi"

    suspend fun blockPage(cursor: String? = null, refresh: Boolean = false): Result<HomepageBlockResponse> {
        return runCatching {
            val data = mutableMapOf<String, Any>("refresh" to refresh)
            if (!cursor.isNullOrEmpty()) {
                data["cursor"] = cursor
            }
            val body = HttpManager.request(
                url = "/api/homepage/block/page",
                data = data,
                crypto = HttpManager.CryptoType.WEAPI
            )
            runCatching {
                json.decodeFromString(HomepageBlockResponse.serializer(), body)
            }.getOrElse { e ->
                if (HttpManager.debugLogEnabled) {
                    Log.w(TAG, "decode failed: /api/homepage/block/page bodyPrefix=${body.take(400)}", e)
                }
                throw e
            }
        }
    }
}
