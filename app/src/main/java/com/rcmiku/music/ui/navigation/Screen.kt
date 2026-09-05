package com.rcmiku.music.ui.navigation

import kotlinx.serialization.Serializable

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Explore : Screen("explore")
    data object Library : Screen("library")
    data object Settings : Screen("settings")
    data object TopList : Screen("topList")
    data object AlbumSublist : Screen("albumSublist")
    data object Login : Screen("login")
    data object Search : Screen("search")
}

const val DEFAULT_PLAYLIST_FETCH_LIMIT = 999

@Serializable
data class PlaylistNav(
    val playlistId: Long,
    val limit: Int = DEFAULT_PLAYLIST_FETCH_LIMIT,
    val noCache: Boolean = false
)

@Serializable
data class RecordNav(val uid: Long)

@Serializable
data class CloudSongNav(val uid: Long)

@Serializable
data class ArtistNav(val artistId: Long)

@Serializable
data class AlbumNav(val albumId: Long)

@Serializable
data class RadioNav(val radioId: Long)

@Serializable
data class UserPlayListNav(val userId: Long, val type: String) {
    companion object {
        const val TYPE_CREATE = "create"
        const val TYPE_COLLECT = "collect"
    }
}