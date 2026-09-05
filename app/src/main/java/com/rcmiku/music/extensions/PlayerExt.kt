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
    withContext(Dispatchers.Main.immediate) {
        if (playlist != null && currentMediaItems.isEmpty()) {
            setMediaItems(playlist)
            if (index != -1) {
                seekToDefaultPosition(index)
            }
            prepare()
        }
    }
}

fun Player.setPlaylist(songs: List<Song>) {
    val cached = cacheSongs.value
    if (cached != null && cached.size == songs.size && cached.map { it.id } == songs.map { it.id }) return
    cacheSongs.value = songs
    setMediaItems(songs.toMediaItemList())
    SongListUtil.saveSongList(songs)
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
    val songMediaId = song.id.toString()
    if (nextMediaItemIndex != C.INDEX_UNSET) {
        val songIndex = (0 until mediaItemCount).firstOrNull { getMediaItemAt(it).mediaId == songMediaId } ?: -1
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
    val songMediaId = song.id.toString()
    if (mediaItemCount > 0) {
        val songIndex = (0 until mediaItemCount).firstOrNull { getMediaItemAt(it).mediaId == songMediaId } ?: -1
        if (songIndex != -1) {
            moveMediaItem(songIndex, mediaItemCount)
        } else {
            addMediaItem(song.toMediaItem())
            SongListUtil.saveSong(song)
        }
    } else {
        setMediaItem(song.toMediaItem())
        SongListUtil.saveSong(song)
    }
}

fun Player.insertToPlaylist(song: Song) {
    val songMediaId = song.id.toString()
    if (nextMediaItemIndex != C.INDEX_UNSET) {
        val songIndex = (0 until mediaItemCount).firstOrNull { getMediaItemAt(it).mediaId == songMediaId } ?: -1
        val targetIndex = nextMediaItemIndex.coerceIn(0, mediaItemCount)
        if (songIndex != -1) {
            moveMediaItem(songIndex, targetIndex)
        } else {
            addMediaItem(targetIndex, song.toMediaItem())
            SongListUtil.saveSong(song, targetIndex)
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
    if (index != null) {
        if (index !in 0 until mediaItemCount) return
        seekToDefaultPosition(index)
    }
    prepare()
    play()
}

fun Player.playMediaAtId(id: Long? = null) {
    if (id == null) return
    val targetId = id.toString()
    val index = (0 until mediaItemCount).firstOrNull { getMediaItemAt(it).mediaId == targetId } ?: return
    if (index !in 0 until mediaItemCount) return
    seekToDefaultPosition(index)
    prepare()
    play()
}

fun Player.playMediaAtMediaId(mediaId: String) {
    val index = (0 until mediaItemCount).firstOrNull { getMediaItemAt(it).mediaId == mediaId } ?: return
    if (index !in 0 until mediaItemCount) return
    seekToDefaultPosition(index)
    prepare()
    play()
}