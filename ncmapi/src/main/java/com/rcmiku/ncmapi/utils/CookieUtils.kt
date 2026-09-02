package com.rcmiku.ncmapi.utils

object CookieKeys {
    const val DEVICE_ID = "deviceId"
    const val OS_VER = "osVer"
    const val MOBILE_NAME = "mobileName"
}

fun parseCookieString(cookie: String): Map<String, String> {
    if (cookie.isBlank()) return emptyMap()
    return cookie
        .split(';')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.contains('=') }
        .associate { part ->
            val idx = part.indexOf('=')
            val key = part.substring(0, idx).trim()
            val value = part.substring(idx + 1).trim()
            key to value
        }
}
