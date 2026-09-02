package com.rcmiku.ncmapi.utils

import android.util.Log
import com.rcmiku.ncmapi.model.NcmApiException
import com.rcmiku.ncmapi.model.NcmHttpException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.security.SecureRandom
import java.util.zip.GZIPInputStream

object HttpManager {
    private const val BASE_URL = "https://interface.163.com"
    private const val API_BASE_URL = "https://music.163.com"
    private const val EAPI_BASE_URL = "https://interface.music.163.com"

    private const val DEFAULT_NETEASE_UA = "NeteaseMusic/3.7.01.250103035128(3007001);Dalvik/2.1.0 (Linux; U; Android 11; Redmi 6A Build/RQ3A.211001.001)"

    private const val TAG = "NcmApi"
    private const val TAG_REQ = "NcmApiReq"
    private const val TAG_RESP = "NcmApiResp"
    private const val TAG_ERR = "NcmApiErr"
    private const val TAG_HDR = "NcmApiHdr"
    private const val TAG_BODY = "NcmApiBody"
    private const val TAG_PLAIN = "NcmApiPlain"

    private const val LOG_CHUNK_SIZE = 800
    // 32MB：兼容 n=100000 全量歌单详情等超大响应（万曲级解压后可达 20MB+），
    // 同时保留对解压炸弹的防护上限
    private const val MAX_DECOMPRESSED_SIZE = 32 * 1024 * 1024L
    private val warnedDebugDisabled = java.util.concurrent.atomic.AtomicBoolean(false)
    private val secureRandom = SecureRandom()

    var hostPackageName: String = "com.rcmiku.music"

    // API 层调试日志的统一门控（与 request() 的 debugEnabled 同源）
    internal val debugLogEnabled: Boolean
        get() = defaultDebugEnabled

    private val isDebugHost: Boolean by lazy {
        runCatching {
            val candidates = listOf(
                "$hostPackageName.BuildConfig",
                "$hostPackageName.debug.BuildConfig",
                "com.rcmiku.music.BuildConfig",
                "com.rcmiku.music.debug.BuildConfig"
            )
            for (className in candidates) {
                runCatching {
                    val clazz = Class.forName(className)
                    val field = clazz.getDeclaredField("DEBUG")
                    field.isAccessible = true
                    return@runCatching field.getBoolean(null)
                }.getOrNull()?.let { return@runCatching it }
            }
            false
        }.getOrDefault(false)
    }

    private val defaultDebugEnabled: Boolean by lazy {
        val prop = System.getProperty("ncmapi.debug")
        val env = System.getenv("NCMAPI_DEBUG")
        when {
            prop != null -> prop == "true"
            env != null -> env == "true"
            else -> isDebugHost
        }
    }

    private val defaultEnvFullDump: Boolean by lazy {
        val prop = System.getProperty("ncmapi.dumpFull")
        val env = System.getenv("NCMAPI_DUMP_FULL")
        prop == "true" || env == "true"
    }

    private val defaultEapiMinimalCookie: Boolean by lazy {
        val prop = System.getProperty("ncmapi.eapiMinimalCookie")
        val env = System.getenv("NCMAPI_EAPI_MINIMAL_COOKIE")
        when {
            prop != null -> prop != "false"
            env != null -> env != "false"
            else -> true
        }
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        followRedirects = true
    }

    suspend fun request(
        url: String,
        data: Map<String, Any> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
        crypto: CryptoType = CryptoType.WEAPI,
        ua: String? = null,
        fullDump: Boolean = false,
    ): String {
        val debugEnabled = defaultDebugEnabled
        val envFullDump = defaultEnvFullDump
        val isFullDump = fullDump || envFullDump
        val eapiMinimalCookie = defaultEapiMinimalCookie
        val logMaxLen = if (isFullDump) Int.MAX_VALUE else 50000

        val headers = mutableMapOf<String, String>()
        val defaultUa = UserAgentProvider.get().let { current ->
            if (current.startsWith("Mozilla/")) DEFAULT_NETEASE_UA else current
        }
        headers["User-Agent"] = ua ?: defaultUa

        if (crypto == CryptoType.EAPI) {
            headers["Accept"] = "application/json"
            headers["Accept-Charset"] = "UTF-8"
            headers["Accept-Encoding"] = "gzip"
            headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8"
        }
        
        // Merge cookies
        val currentCookies = CookieProvider.get().toMutableMap()
        currentCookies.putAll(cookies)

        if (crypto == CryptoType.EAPI) {
            // Try to match official client cookie flags observed in captures.
            currentCookies.putIfAbsent("EVNSM", "1.0.0")
            currentCookies.putIfAbsent("versioncode", "3007001")
            currentCookies.putIfAbsent("buildver", "250103035128")
            currentCookies.putIfAbsent("resolution", "2269x1080")
            currentCookies.putIfAbsent("channel", "netease")
            currentCookies.putIfAbsent("os", "andrcar")
            currentCookies.putIfAbsent("modelCode", "netease")
            currentCookies.putIfAbsent("distributeChannel", "andrcar%24%7B%22channel%22%3A%22netease%22%7D")
            currentCookies.putIfAbsent("screenType", "other")
            currentCookies.putIfAbsent("appver", "3.7.01")
            currentCookies.putIfAbsent("packageType", "release")
        }
        
        // Build cookie string
        val cookieHeader = StringBuilder()
        if (crypto != CryptoType.EAPI) {
            currentCookies.getOrPut("os") { "pc" }
            currentCookies.getOrPut("appver") { "3.0.18.203152" }
            currentCookies.getOrPut("osver") { "Microsoft-Windows-10" }
            currentCookies.getOrPut("channel") { "netease" }

            currentCookies.getOrPut("versioncode") { "3007001" }
            currentCookies.getOrPut("buildver") { "250103035128" }
            currentCookies.getOrPut("resolution") { "2269x1080" }
            currentCookies.getOrPut("packageType") { "release" }
        }

        fun randomHex(len: Int): String {
            val chars = "0123456789abcdef"
            return buildString(len) {
                repeat(len) { append(chars[secureRandom.nextInt(chars.length)]) }
            }
        }

        fun randomLower(len: Int): String {
            val chars = "abcdefghijklmnopqrstuvwxyz"
            return buildString(len) {
                repeat(len) { append(chars[secureRandom.nextInt(chars.length)]) }
            }
        }

        currentCookies.getOrPut("_ntes_nuid") { randomHex(32) }
        currentCookies.getOrPut("_ntes_nnid") {
            val nuid = currentCookies["_ntes_nuid"] ?: randomHex(32)
            "$nuid,${System.currentTimeMillis()}"
        }
        if (crypto == CryptoType.EAPI) {
            // Match official client format observed in captures: vsfmic.1740907910255.01.4
            currentCookies.getOrPut("WNMCID") { "${randomLower(6)}.${System.currentTimeMillis()}.01.4" }
            currentCookies.getOrPut("NMCID") { currentCookies["WNMCID"] ?: "${randomLower(6)}.${System.currentTimeMillis()}.01.4" }
        } else {
            currentCookies.getOrPut("WNMCID") { "${randomHex(6)}.${System.currentTimeMillis()}.01.0" }
            currentCookies.getOrPut("NMCID") { currentCookies["WNMCID"] ?: "${randomHex(6)}.${System.currentTimeMillis()}.01.0" }
        }
        currentCookies.getOrPut("WEVNSM") { "1.0.0" }
        currentCookies.getOrPut("__remember_me") { "true" }
        currentCookies.getOrPut("ntes_kaola_ad") { "1" }
        currentCookies.getOrPut("NMTID") { randomHex(16) }

        if (!currentCookies.containsKey("MUSIC_U") && !currentCookies.containsKey("MUSIC_A")) {
            currentCookies["MUSIC_A"] = "guest_${randomHex(24)}"
        }

        CookieProvider.update { persistent ->
            listOf("_ntes_nuid", "_ntes_nnid", "WNMCID", "NMCID", "NMTID", "MUSIC_A", "WEVNSM", "__remember_me", "ntes_kaola_ad").forEach { k ->
                currentCookies[k]?.let { persistent.putIfAbsent(k, it) }
            }
        }
        
        val cookieToSend: Map<String, String> = if (crypto == CryptoType.EAPI && eapiMinimalCookie) {
            val allow = linkedSetOf(
                "EVNSM",
                "versioncode",
                "buildver",
                "resolution",
                "MUSIC_U",
                "MUSIC_A",
                "__csrf",
                "JSESSIONID-WYYY",
                "NTES_YD_SESS",
                "P_INFO",
                "S_INFO",
                "NMCID",
                "channel",
                "os",
                "modelCode",
                "distributeChannel",
                "screenType",
                "appver",
                "packageType"
            )
            currentCookies.filterKeys { allow.contains(it) }
        } else {
            currentCookies
        }

        cookieToSend.forEach { (k, v) ->
            cookieHeader.append("$k=$v; ")
        }
        headers["Cookie"] = cookieHeader.toString()

        // Match api-enhanced-main request.js behavior:
        // - WEAPI: /api/xxx is transported as /weapi/xxx
        // - EAPI:  /api/xxx is transported as /eapi/xxx (signature still uses original /api/xxx)
        val transportPath = when {
            crypto == CryptoType.WEAPI && url.startsWith("/api/") -> "/weapi/" + url.removePrefix("/api/")
            crypto == CryptoType.EAPI && url.startsWith("/api/") -> "/eapi/" + url.removePrefix("/api/")
            else -> url
        }

        val finalUrl = if (url.startsWith("http")) {
            url
        } else {
            val base = when {
                crypto == CryptoType.EAPI -> EAPI_BASE_URL
                transportPath.startsWith("/eapi") -> EAPI_BASE_URL
                transportPath.startsWith("/weapi") || transportPath.startsWith("/api") -> API_BASE_URL
                else -> BASE_URL
            }
            "$base$transportPath"
        }

        if (crypto != CryptoType.EAPI) {
            headers["Referer"] = when {
                finalUrl.startsWith(API_BASE_URL) -> API_BASE_URL
                finalUrl.startsWith(EAPI_BASE_URL) -> EAPI_BASE_URL
                else -> BASE_URL
            }
        }

        val urlHost = runCatching { io.ktor.http.Url(finalUrl).host }.getOrDefault("<unknown>")

        if (debugEnabled) {
            val cookieKeys = currentCookies.keys.sorted().joinToString(",")
            Log.d(
                TAG,
                "request crypto=$crypto url=$finalUrl host=$urlHost referer=${headers["Referer"]} ua=${headers["User-Agent"]} cookieKeys=[$cookieKeys] dataKeys=${data.keys.sorted()}"
            )
            Log.d(TAG_REQ, "POST crypto=$crypto debug=$debugEnabled url=$finalUrl")
            logChunked(TAG_PLAIN, "cookieKeys=[$cookieKeys] cookieLen=${headers["Cookie"]?.length ?: 0}", debugEnabled)

            val plainDump = data.entries
                .sortedBy { it.key }
                .joinToString(" | ") { (k, v) ->
                    val vv = maskIfSensitive(k, v.toString())
                    "$k=$vv"
                }
            logChunked(TAG_PLAIN, "reqData $plainDump", debugEnabled)

            val headerDump = headers.entries
                .sortedBy { it.key.lowercase() }
                .joinToString(" | ") { (k, v) ->
                    if (k.equals("Cookie", ignoreCase = true)) {
                        if (isFullDump) {
                            "$k=$v"
                        } else {
                            val maskedCookie = v.split(";").joinToString("; ") { part ->
                                val trimmed = part.trim()
                                val eqIdx = trimmed.indexOf('=')
                                if (eqIdx != -1) {
                                    val cKey = trimmed.substring(0, eqIdx)
                                    val cVal = trimmed.substring(eqIdx + 1)
                                    "$cKey=${maskIfSensitive(cKey, cVal)}"
                                } else {
                                    trimmed
                                }
                            }
                            "$k(len=${v.length},masked=$maskedCookie)"
                        }
                    } else {
                        val vv = maskIfSensitive(k, if (v.length > 200) v.take(200) + "..." else v)
                        "$k=$vv"
                    }
                }
            logChunked(TAG_HDR, "reqHeaders $headerDump", debugEnabled)
        } else {
            if (warnedDebugDisabled.compareAndSet(false, true)) {
                Log.w(TAG, "debug disabled (set -Dncmapi.debug=true or env NCMAPI_DEBUG=true)")
            }
        }

        val response: HttpResponse = try {
            val encryptedData = when (crypto) {
                CryptoType.WEAPI -> {
                    val dataWithCsrf = data.toMutableMap()
                    val csrfToken = currentCookies["__csrf"] ?: ""
                    dataWithCsrf["csrf_token"] = csrfToken
                    CryptoUtils.weapi(toJsonString(dataWithCsrf))
                }
                CryptoType.LINUXAPI -> {
                    CryptoUtils.linuxapi(toJsonString(data))
                }
                CryptoType.EAPI -> {
                    // Important: use the original request path for EAPI signature.
                    // Using encodedPath from finalUrl may differ if host/base changes.
                    CryptoUtils.eapi(url, toJsonString(data))
                }
            }

            val bodyDump = encryptedData.entries.joinToString(" | ") { (k, v) ->
                if (isFullDump) {
                    "$k=$v"
                } else {
                    val prefix = if (v.length > 120) v.take(120) + "..." else v
                    "$k(len=${v.length},prefix=$prefix)"
                }
            }
            logChunked(TAG_BODY, "reqBodyForm $bodyDump", debugEnabled)

            if (debugEnabled) {
                val summary = encryptedData.entries.joinToString(", ") { (k, v) ->
                    val prefix = if (v.length > 24) v.take(24) + "..." else v
                    "$k(len=${v.length},prefix=$prefix)"
                }
                Log.d(TAG, "cryptoPayload {$summary}")
            }

            client.post(finalUrl) {
                headers.forEach { (k, v) -> header(k, v) }
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    encryptedData.forEach { (k, v) ->
                        append(k, v)
                    }
                }))
            }
        } catch (t: Throwable) {
            if (debugEnabled) {
                Log.e(TAG_ERR, "request failed crypto=$crypto url=$finalUrl", t)
            }
            throw t
        }

        if (response.status.value !in 200..299) {
            throw NcmHttpException(response.status.value, "HTTP ${response.status.value} for $finalUrl")
        }

        val contentEncoding = response.headers["Content-Encoding"]
        val rawBytes = runCatching { response.readRawBytes() }.getOrNull()
        if (rawBytes != null && debugEnabled) {
            Log.d(TAG_RESP, "rawByteLen=${rawBytes.size} contentEncoding=$contentEncoding")
        }
        val body: String = if (rawBytes != null) {
            decodeResponseBytes(rawBytes, contentEncoding)
        } else {
            response.body()
        }

        if (debugEnabled) {
            val respHeaderDump = response.headers.entries()
                .sortedBy { it.key.lowercase() }
                .joinToString(" | ") { (k, vs) ->
                    if (k.equals("Set-Cookie", ignoreCase = true)) {
                        val masked = vs.joinToString("; ") { cookieStr ->
                            val parts = cookieStr.split(";").map { it.trim() }
                            val first = parts.firstOrNull() ?: ""
                            val eqIdx = first.indexOf('=')
                            if (eqIdx != -1) {
                                val cKey = first.substring(0, eqIdx)
                                "$cKey=<masked>"
                            } else {
                                "<masked>"
                            }
                        }
                        "$k=$masked"
                    } else {
                        val joined = vs.joinToString(";")
                        val vv = if (joined.length > 200) joined.take(200) + "..." else joined
                        "$k=$vv"
                    }
                }
            logChunked(TAG_HDR, "respHeaders $respHeaderDump", debugEnabled)

            val contentType = response.headers["Content-Type"]
            Log.d(TAG_RESP, "status=${response.status.value} url=$finalUrl contentType=$contentType len=${body.length}")
            val bodyForLog = decodeBodyForLog(body, maxLen = logMaxLen)
            logChunked(TAG_BODY, "respBody $bodyForLog", debugEnabled)
        }

        checkApiResponseCode(body)
        return body
    }

    internal fun checkApiResponseCode(body: String) {
        val trimmed = body.trimStart()
        if (!trimmed.startsWith("{")) return
        val element = runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return
        val codeElement = element["code"] as? JsonPrimitive ?: return
        val code = codeElement.intOrNull ?: return
        if (code !in 200..299) {
            val message = (element["message"] as? JsonPrimitive)?.content
                ?: (element["msg"] as? JsonPrimitive)?.content
                ?: "NCM API error code: $code"
            throw NcmApiException(code = code, message = message)
        }
    }

    private fun logChunked(tag: String, message: String, debugEnabled: Boolean) {
        if (!debugEnabled) return
        if (message.length <= LOG_CHUNK_SIZE) {
            Log.d(tag, message)
            return
        }
        var idx = 0
        var part = 0
        while (idx < message.length) {
            val end = (idx + LOG_CHUNK_SIZE).coerceAtMost(message.length)
            val chunk = message.substring(idx, end)
            Log.d(tag, "part=$part $chunk")
            idx = end
            part++
        }
    }

    private fun decodeResponseBytes(bytes: ByteArray, contentEncoding: String?): String {
        val isGzipHeader = bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
        val shouldTryGunzip = isGzipHeader || contentEncoding?.contains("gzip", ignoreCase = true) == true
        if (shouldTryGunzip) {
            return gunzipToString(bytes)
        }
        return bytes.toString(Charsets.UTF_8)
    }

    internal fun maskIfSensitive(key: String, value: String): String {
        val k = key.lowercase()
        val sensitive = listOf(
            "password",
            "passwd",
            "token",
            "csrf",
            "music_u",
            "music_a",
            "cookie",
            "encseckey",
            "params",
            "eparams",
            "phone",
            "captcha",
            "ckid",
            "deviceid"
        )
        if (sensitive.any { k.contains(it) }) {
            return "<masked>"
        }
        return value
    }

    private fun decodeBodyForLog(raw: String, maxLen: Int): String {
        return if (raw.length > maxLen) raw.take(maxLen) + "...<truncated>" else raw
    }

    internal fun gunzipToString(bytes: ByteArray, charset: Charset = Charsets.UTF_8): String {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { gis ->
            val bos = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var totalBytes = 0L
            while (true) {
                val read = gis.read(buffer)
                if (read <= 0) break
                totalBytes += read
                if (totalBytes > MAX_DECOMPRESSED_SIZE) {
                    throw IllegalStateException("Decompressed response exceeded 10MB limit: $totalBytes bytes")
                }
                bos.write(buffer, 0, read)
            }
            return bos.toByteArray().toString(charset)
        }
    }
    
    enum class CryptoType {
        WEAPI, LINUXAPI, EAPI
    }

    private fun toJsonString(map: Map<String, Any>): String {
        val obj = map.mapValues { (_, v) -> v.toJsonElement() }
        return json.encodeToString(JsonElement.serializer(), kotlinx.serialization.json.JsonObject(obj))
    }

    private fun Any.toJsonElement(): JsonElement = when (this) {
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Iterable<*> -> JsonArray(this.map { (it ?: "null").toJsonElement() })
        is Map<*, *> -> JsonObject(this.entries.associate { it.key.toString() to (it.value ?: "null").toJsonElement() })
        else -> JsonPrimitive(this.toString())
    }
}
