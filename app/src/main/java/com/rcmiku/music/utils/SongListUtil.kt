package com.rcmiku.music.utils

import android.util.AtomicFile
import androidx.media3.common.MediaItem
import com.rcmiku.music.extensions.toMediaItem
import com.rcmiku.ncmapi.model.CloudSong
import com.rcmiku.ncmapi.model.Radio
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.utils.json
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileOutputStream

@Serializable
enum class PlaylistItemType {
    SONG,
    CLOUD,
    RADIO
}

@Serializable
data class PlaylistItem(
    val type: PlaylistItemType = PlaylistItemType.SONG,
    val song: Song? = null,
    val cloudSong: CloudSong? = null,
    val uid: Long? = null,
    val radio: Radio? = null
) {
    val mediaId: String?
        get() = when (type) {
            PlaylistItemType.SONG -> song?.id?.toString()
            PlaylistItemType.CLOUD -> cloudSong?.simpleSong?.id?.toString()
            PlaylistItemType.RADIO -> radio?.mainSong?.id?.toString()
        }

    fun toMediaItem(): MediaItem? {
        return when (type) {
            PlaylistItemType.SONG -> song?.toMediaItem()
            PlaylistItemType.CLOUD -> cloudSong?.toMediaItem(uid ?: 0L)
            PlaylistItemType.RADIO -> radio?.toMediaItem()
        }
    }
}

object SongListUtil {

    private const val SONG_LIST = "song_list.json"
    private var file: File? = null
    private var playlistItems: List<PlaylistItem> = emptyList()
    private val lock = Any()

    fun init(file: File) {
        if (!file.exists()) file.mkdir()
        this.file = file
    }

    fun saveSong(song: Song) = synchronized(lock) {
        val updated = playlistItems.toMutableList()
        updated.add(PlaylistItem(type = PlaylistItemType.SONG, song = song))
        savePlaylist(updated)
    }

    fun saveSong(song: Song, index: Int) = synchronized(lock) {
        val updated = playlistItems.toMutableList()
        val targetIndex = index.coerceIn(0, updated.size)
        updated.add(targetIndex, PlaylistItem(type = PlaylistItemType.SONG, song = song))
        savePlaylist(updated)
    }

    fun removeSong(songId: Long) = synchronized(lock) {
        val updated = playlistItems.toMutableList()
        updated.removeIf { item ->
            item.song?.id == songId ||
                item.cloudSong?.simpleSong?.id == songId ||
                item.radio?.mainSong?.id == songId
        }
        savePlaylist(updated)
    }

    fun removePlaylistItem(mediaId: String) = synchronized(lock) {
        val updated = playlistItems.toMutableList()
        updated.removeIf { it.mediaId == mediaId }
        savePlaylist(updated)
    }

    fun saveSongList(songList: List<Song>) {
        savePlaylist(songList.map { PlaylistItem(type = PlaylistItemType.SONG, song = it) })
    }

    fun savePlaylist(items: List<PlaylistItem>) = synchronized(lock) {
        val dir = file ?: return
        this.playlistItems = items
        val jsonStr = json.encodeToString<List<PlaylistItem>>(items)
        val atomicFile = AtomicFile(File(dir, SONG_LIST))
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(jsonStr.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            if (stream != null) {
                atomicFile.failWrite(stream)
            }
        }
    }

    fun loadSongList(): List<Song>? = synchronized(lock) {
        val items = loadPlaylist() ?: return null
        return items.mapNotNull { it.song }.ifEmpty { null }
    }

    fun loadPlaylist(): List<PlaylistItem>? = synchronized(lock) {
        val dir = file ?: return null
        val targetFile = File(dir, SONG_LIST)
        if (!targetFile.exists()) return null
        val atomicFile = AtomicFile(targetFile)
        val parsed = runCatching {
            val content = atomicFile.readFully().toString(Charsets.UTF_8)
            runCatching {
                json.decodeFromString<List<PlaylistItem>>(content)
            }.getOrElse {
                json.decodeFromString<List<Song>>(content).map {
                    PlaylistItem(type = PlaylistItemType.SONG, song = it)
                }
            }
        }.getOrNull()
        if (parsed != null) {
            playlistItems = parsed
        }
        return parsed
    }
}
