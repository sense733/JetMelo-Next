package com.rcmiku.ncmapi.api.search

enum class SearchType(val type: Int) {
    Song(1),
    Album(10),
    Artist(100),
    Playlist(1000),
    VoiceList(2000);

    companion object {
        fun fromType(type: Int): SearchType {
            return when (type) {
                1 -> Song
                10 -> Album
                100 -> Artist
                1000 -> Playlist
                2000 -> VoiceList
                else -> Song
            }
        }

        fun fromRawName(name: String): SearchType {
            return when (name.uppercase()) {
                "SONG" -> Song
                "ALBUM" -> Album
                "ARTIST" -> Artist
                "PLAYLIST" -> Playlist
                "SOUND", "RADIO" -> VoiceList
                else -> Song
            }
        }
    }
}
