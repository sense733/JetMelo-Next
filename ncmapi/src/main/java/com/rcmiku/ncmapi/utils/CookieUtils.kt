package com.rcmiku.ncmapi.utils

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object CookieKeys {
    const val DEVICE_ID = "deviceId"
    const val OS_VER = "osVer"
    const val OS_VER_LOWER = "osver"
    const val MOBILE_NAME = "mobileName"
    const val MOBILE_NAME_LOWER = "mobilename"
}

private val IGNORED_COOKIE_ATTRIBUTES = setOf(
    "secure", "httponly", "domain", "path", "expires", "max-age", "samesite"
)

fun parseCookieString(cookie: String): Map<String, String> {
    if (cookie.isBlank()) return emptyMap()
    val result = linkedMapOf<String, String>()
    val parts = cookie.split(';')
    for (part in parts) {
        val trimmed = part.trim()
        if (trimmed.isEmpty() || !trimmed.contains('=')) continue
        val idx = trimmed.indexOf('=')
        val key = trimmed.substring(0, idx).trim()
        val value = trimmed.substring(idx + 1).trim()
        if (key.lowercase() in IGNORED_COOKIE_ATTRIBUTES) continue
        val decodedValue = try {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            value
        }
        result.putIfAbsent(key, decodedValue)
    }
    return result
}
