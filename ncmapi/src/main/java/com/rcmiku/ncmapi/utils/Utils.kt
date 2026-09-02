package com.rcmiku.ncmapi.utils

import android.os.Build
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLEncoder
import java.util.UUID

val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    prettyPrint = false
}

// 标准 base64（带填充、不换行），与原 android.util.Base64.NO_WRAP 输出逐字节等价；
// 迁移到纯 JVM 实现以解除对 Android 运行时的依赖（单元测试可用）
fun base64Encode(data: ByteArray): String {
    return java.util.Base64.getEncoder().encodeToString(data)
}

object FileProvider {
    @Volatile
    private var cacheDir: File? = null

    fun init(dir: File) {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        cacheDir = dir
    }

    fun getCacheDir(): File {
        return cacheDir ?: throw IllegalStateException("FileProvider has not been initialized. Call FileProvider.init(cacheDir) first.")
    }
}

object UserAgentProvider {
    @Volatile
    private var userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    fun init(ua: String) {
        userAgent = ua
    }

    fun get(): String = userAgent
}

object CookieProvider {
    @Volatile
    private var cookie: Map<String, String> = emptyMap()

    fun init(cookies: Map<String, String>) {
        val mutable = cookies.toMutableMap()

        if (!mutable.containsKey("deviceId")) {
            val uuid = UUID.randomUUID().toString().replace("-", "").take(16)
            val raw = "null 02:00:00:00:00:00 $uuid unknown"
            mutable["deviceId"] = URLEncoder.encode(raw, Charsets.UTF_8.name())
        }
        mutable.putIfAbsent("osver", Build.VERSION.RELEASE)
        mutable.putIfAbsent("mobilename", Build.MODEL)

        synchronized(this) {
            cookie = mutable
        }
    }

    fun update(block: (MutableMap<String, String>) -> Unit) {
        synchronized(this) {
            val mutable = cookie.toMutableMap()
            block(mutable)
            cookie = mutable
        }
    }

    fun clear() {
        synchronized(this) {
            cookie = emptyMap()
        }
    }

    fun get(): Map<String, String> = cookie
}
