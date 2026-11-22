package com.rcmiku.music.update

import kotlinx.serialization.Serializable

/**
 * 服务端返回的远程应用版本信息。
 */
@Serializable
data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String = "",
    val forceUpdate: Boolean = false,
    val sha256: String = ""
)
