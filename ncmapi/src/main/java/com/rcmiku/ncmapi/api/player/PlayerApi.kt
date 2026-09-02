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
    suspend fun songPlayUrlV1(id: String, level: String = "standard", songLevel: SongLevel? = null): Result<SongUrlResponse> {
        return runCatching {
            // ref: NeteaseCloudMusicApi -> /api/song/enhance/player/url/v1
            val finalLevel = songLevel?.level ?: level
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
            Log.w("PlayerApi", "songPlayUrlV1 id=$id level=$finalLevel code=${resp.code} urlPresent=${first?.url != null}")
            resp
        }
    }

    suspend fun songLyric(musicId: Long): Result<LyricResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/weapi/song/lyric",
                data = mapOf(
                    "id" to musicId,
                    "lv" to -1,
                    "tv" to -1,
                    "rv" to -1,
                    "kv" to -1,
                    "_nmclfl" to 1
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(LyricResponse.serializer(), body)
        }
    }
}
