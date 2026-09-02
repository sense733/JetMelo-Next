package com.rcmiku.ncmapi.api.account

enum class SongRecordType(val type: Int) {
    WEEK(1),
    ALL(0)
}

enum class UserPlaylistType(val type: String) {
    CREATE("create"),
    COLLECT("collect")
}
