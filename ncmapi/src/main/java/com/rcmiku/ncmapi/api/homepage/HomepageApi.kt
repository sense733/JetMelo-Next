package com.rcmiku.ncmapi.api.homepage

import android.util.Log
import com.rcmiku.ncmapi.model.HomepageBlockResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json

object HomepageApi {
    private const val TAG = "HomepageApi"

    /**
     * APP 首页发现页 block 流（/api/homepage/block/page，WEAPI）。
     *
     * @param cursor 翻页游标，为空取第一页；用上一次响应 `data.cursor` 继续翻页
     * @param refresh 是否刷新数据（官方默认 false）
     */
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
