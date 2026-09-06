package com.rcmiku.ncmapi.api.player

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import com.rcmiku.ncmapi.model.LyricResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json

enum class SongLevel(val level: String) {
    STANDARD("standard"),
    HIGHER("higher"),
    EXHIGH("exhigh"),
    LOSSLESS("lossless"),
    HIRES("hires"),
    SKY("sky")
}

@Serializable
data class SongUrl(
    val id: Long = 0,
    val url: String? = null,
    val br: Int = 0,
    val size: Long = 0,
    val md5: String? = null,
    val code: Int = 200,
    val expi: Int = 0,
    val type: String? = null,
    val gain: Double = 0.0,
    val fee: Int = 0,
    val uf: String? = null,
    val payed: Int = 0,
    val flag: Int = 0,
    val canExtend: Boolean = false,
    val freeTrialInfo: JsonElement? = null,
    val level: String? = null,
    val encodeType: String? = null,
    val freeTrialPrivilege: FreeTrialPrivilege? = null,
    val freeTimeTrialPrivilege: FreeTimeTrialPrivilege? = null,
    val urlSource: Int = 0,
    val rightSource: Int = 0,
    val podcastCtrp: String? = null,
    val effectTypes: String? = null,
    val time: Int = 0
)

@Serializable
data class FreeTrialPrivilege(
    val resConsumable: Boolean = false,
    val userConsumable: Boolean = false,
    val listenType: Int? = null,
    val cannotListenReason: Int? = null,
    val playReason: String? = null,
    val freeLimitTagType: String? = null
)

@Serializable
data class FreeTimeTrialPrivilege(
    val resConsumable: Boolean = false,
    val userConsumable: Boolean = false,
    val type: Int? = null,
    val remainTime: Int? = null
)

@Serializable
data class SongUrlResponse(
    val code: Int = 200,
    val data: List<SongUrl> = emptyList()
)

object PlayerApi {
    /**
     * 获取歌曲播放链接 (v1)。
     *
     * 传参逻辑：
     * - [songLevel] 优先级高于 [level]，最终音质参数降级回退至 [level]。
     * - [encodeType] 固定为 "flac"。
     * - 仅当音质为 "sky"（沉浸声）时追加 "immerseType" = "c51"。
     *
     * @param id 歌曲 ID
     * @param level 字符串音质级别，默认为 "standard"
     * @param songLevel 枚举音质级别，若提供则覆盖 level
     * @return [Result] 包含播放链接响应，若业务状态码异常或链接为空则封装为失败
     */
    suspend fun songPlayUrlV1(id: String, level: String = "standard", songLevel: SongLevel? = null): Result<SongUrlResponse> {
        return runCatching {
            require(id.isNotBlank()) { "Song ID must not be blank" }
            val finalLevel = songLevel?.level ?: level
            require(finalLevel.isNotBlank()) { "Song level must not be blank" }

            val data = mutableMapOf(
                "ids" to "[$id]",
                "level" to finalLevel,
                "encodeType" to "flac"
            )
            if (finalLevel == "sky") {
                data["immerseType"] = "c51"
            }
            val body = HttpManager.request(
                url = "/weapi/song/enhance/player/url/v1",
                data = data,
                crypto = HttpManager.CryptoType.WEAPI
            )
            val resp = json.decodeFromString(SongUrlResponse.serializer(), body)
            val first = resp.data.firstOrNull()
            if (HttpManager.debugLogEnabled) {
                Log.w("PlayerApi", "songPlayUrlV1 id=$id level=$finalLevel code=${resp.code} urlPresent=${first?.url != null}")
            }
            if (resp.code != 200) {
                error("songPlayUrlV1 failed with response code ${resp.code}")
            }
            if (first == null) {
                error("songPlayUrlV1 returned empty song url list for id=$id")
            }
            if (first.code != 200) {
                error("songPlayUrlV1 item failed with item code ${first.code} for id=$id")
            }
            if (first.url.isNullOrBlank()) {
                error("songPlayUrlV1 returned blank url for id=$id")
            }
            resp
        }
    }

    /**
     * 获取歌曲歌词。
     *
     * @param musicId 歌曲 ID
     * @return [Result] 包含歌词响应
     */
    suspend fun songLyric(musicId: Long): Result<LyricResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/weapi/song/lyric",
                data = mapOf(
                    "id" to musicId.toString(),
                    "lv" to "-1",
                    "tv" to "-1",
                    "rv" to "-1",
                    "kv" to "-1",
                    "_nmclfl" to "1"
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(LyricResponse.serializer(), body)
        }
    }
}
