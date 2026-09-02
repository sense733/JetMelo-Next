package com.rcmiku.music.extensions

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.rcmiku.music.constants.currentPlayMediaIdKey
import com.rcmiku.music.utils.PlaylistItem
import com.rcmiku.music.utils.PlaylistItemType
import com.rcmiku.music.utils.SongListUtil
import com.rcmiku.music.utils.dataStore
import com.rcmiku.ncmapi.model.CloudSong
import com.rcmiku.ncmapi.model.Radio
import com.rcmiku.ncmapi.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal val Player.currentMediaItems: List<MediaItem>
    get() {
        return List(mediaItemCount, ::getMediaItemAt)
    }

internal val cacheSongs: MutableStateFlow<List<Song>?> = MutableStateFlow(null)

suspend fun Player.init(context: Context) {
    val (playlist, index) = withContext(Dispatchers.IO) {
        val currentPlayMediaId = context.dataStore.data
            .map { it[currentPlayMediaIdKey] }
            .first()
        val playlist = SongListUtil.loadPlaylist()?.mapNotNull { it.toMediaItem() }?.takeIf { it.isNotEmpty() }
        val index = if (playlist != null && currentPlayMediaId != null) {
            playlist.indexOfFirst { it.mediaId == currentPlayMediaId.toString() }
        } else -1
        playlist to index
    }
    if (playlist != null && currentMediaItems.isEmpty()) {
        setMediaItems(playlist)
        if (index != -1) {
            seekToDefaultPosition(index)
        }
        // 恢复媒体通道状态，保持暂停等待用户显式触发播放，避免冷启动出声
        prepare()
    }
}

fun Player.setPlaylist(songs: List<Song>) {
    if (cacheSongs.value != songs) {
        cacheSongs.value = songs
        setMediaItems(songs.toMediaItemList())
        SongListUtil.saveSongList(songs)
    }
}

fun Player.setCloudSongPlaylist(uid: Long, cloudSongs: List<CloudSong>) {
    cacheSongs.value = null
    setMediaItems(cloudSongs.toCloudSongMediaItemList(uid = uid))
    SongListUtil.savePlaylist(cloudSongs.map { PlaylistItem(type = PlaylistItemType.CLOUD, cloudSong = it, uid = uid) })
}

fun Player.setRadioPlaylist(radio: List<Radio>) {
    cacheSongs.value = null
    setMediaItems(radio.toRadioMediaItemList())
    SongListUtil.savePlaylist(radio.map { PlaylistItem(type = PlaylistItemType.RADIO, radio = it) })
}

fun Player.addSong(song: Song) {
    if (nextMediaItemIndex != C.INDEX_UNSET) {
        val songIndex = currentMediaItems.indexOfFirst {
            it.mediaId == song.id.toString() && it.localConfiguration?.uri?.toString() == song.id.toString()
        }
        if (songIndex != -1) {
            playMediaAt(songIndex)
        } else {
            addMediaItem(nextMediaItemIndex, song.toMediaItem())
            playMediaAt(nextMediaItemIndex)
            SongListUtil.saveSong(song, nextMediaItemIndex)
        }
    } else {
        setMediaItem(song.toMediaItem())
        playMediaAt()
        SongListUtil.saveSong(song)
    }
}

fun Player.addToPlaylist(song: Song) {
    if (currentMediaItems.isNotEmpty()) {
        val songIndex = currentMediaItems.indexOfFirst {
            it.mediaId == song.id.toString() && it.localConfiguration?.uri?.toString() == song.id.toString()
        }
        if (songIndex == -1) {
            addMediaItem(song.toMediaItem())
            SongListUtil.saveSong(song)
        }
    } else {
        setMediaItem(song.toMediaItem())
        SongListUtil.saveSong(song)
    }
}

fun Player.insertToPlaylist(song: Song) {
    if (nextMediaItemIndex != C.INDEX_UNSET) {
        val songIndex = currentMediaItems.indexOfFirst {
            it.mediaId == song.id.toString() && it.localConfiguration?.uri?.toString() == song.id.toString()
        }
        if (songIndex != -1) {
            moveMediaItem(songIndex, nextMediaItemIndex)
        } else {
            addMediaItem(nextMediaItemIndex, song.toMediaItem())
            SongListUtil.saveSong(song, nextMediaItemIndex)
        }
    } else {
        setMediaItem(song.toMediaItem())
        SongListUtil.saveSong(song)
    }
}

fun Player.removeSong(mediaId: String) {
    if (currentMediaItems.isNotEmpty()) {
        val songIndex = currentMediaItems.indexOfFirst { it.mediaId == mediaId }
        if (songIndex != -1) {
            removeMediaItem(songIndex)
            SongListUtil.removePlaylistItem(mediaId)
        }
    }
}

fun Player.playMediaAt(index: Int? = null) {
    if (index != -1)
        index?.let { seekToDefaultPosition(it) }
    prepare()
    play()
}

fun Player.playMediaAtId(id: Long? = null) {
    val index = currentMediaItems.indexOfFirst { it.mediaId == id.toString() }
    if (index != -1)
        seekToDefaultPosition(index)
    prepare()
    play()
}

fun Player.playMediaAtMediaId(mediaId: String) {
    val index = currentMediaItems.indexOfFirst { it.mediaId == mediaId }
    if (index != -1) {
        seekToDefaultPosition(index)
        prepare()
        play()
    }
}