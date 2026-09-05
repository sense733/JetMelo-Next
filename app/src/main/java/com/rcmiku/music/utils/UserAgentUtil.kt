package com.rcmiku.music.utils

import android.os.Build

class UserAgentUtil {

    companion object {

        private const val DEFAULT_NETEASE_VERSION = "3.7.01.250103035128(3007001)"
        private const val FALLBACK_VM_VERSION = "2.1.0"

        val DEFAULT_USER_AGENT: String = buildString {
            val validRelease = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Build.VERSION.RELEASE_OR_CODENAME.isNotBlank()
            } else {
                Build.VERSION.RELEASE.isNotBlank()
            }

            val cleanId = Build.ID?.trim().takeUnless { it.isNullOrBlank() }
            val cleanModel = Build.MODEL?.trim().takeUnless { it.isNullOrBlank() }
            val includeModel = "REL" == Build.VERSION.CODENAME && cleanModel != null
            val vmVersion = System.getProperty("java.vm.version")?.trim().takeUnless { it.isNullOrBlank() }
                ?: FALLBACK_VM_VERSION

            append("NeteaseMusic/").append(DEFAULT_NETEASE_VERSION)
            append(";Dalvik/").append(vmVersion)

            append(" (Linux; U; Android")
            if (validRelease) {
                val releaseVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Build.VERSION.RELEASE_OR_CODENAME.trim()
                } else {
                    Build.VERSION.RELEASE.trim()
                }
                append(" ").append(releaseVersion)
            }

            if (includeModel || cleanId != null) {
                append(";")
                if (includeModel) {
                    append(" ").append(cleanModel)
                }
                if (cleanId != null) {
                    append(" Build/").append(cleanId)
                }
            }

            append(")")
        }
    }
}