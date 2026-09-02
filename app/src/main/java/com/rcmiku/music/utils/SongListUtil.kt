package com.rcmiku.music.utils

import android.util.AtomicFile
import androidx.media3.common.MediaItem
import com.rcmiku.music.extensions.toMediaItem
import com.rcmiku.ncmapi.model.CloudSong
import com.rcmiku.ncmapi.model.Radio
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.utils.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
            // 云盘项缺 uid 时无法重建 "id_uid" 播放 URI，跳过而非伪造 "id_0"
            PlaylistItemType.CLOUD -> uid?.let { cloudSong?.toMediaItem(it) }
            PlaylistItemType.RADIO -> radio?.toMediaItem()
        }
    }
}

object SongListUtil {

    private const val SONG_LIST = "song_list.json"
    private var file: File? = null
    private var playlistItems: List<PlaylistItem> = emptyList()
    private val lock = Any()

    // v2 格式解析用严格实例：v1 旧文件（List<Song>）含未知键必然解析失败，
    // 从而正确触发 v1 降级。共享的 `json` 配置了 ignoreUnknownKeys=true，
    // 会把 v1 内容"成功"解码成 song=null 的空壳列表（验收 B1 缺陷）。
    private val strictJson = Json {
        ignoreUnknownKeys = false
        coerceInputValues = false
    }

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

    // 注：写盘在调用线程同步执行——与"读改写 + 落盘"共用一把锁的顺序性耦合。
    // 写路径调用方多为用户交互（低频），读路径已在 Dispatchers.IO（Player.init）。
    // 若改异步需先解决 save→load 的顺序一致性问题（02 分册验收 B3 的取舍）。
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
                // 严格模式解析 v2；v1 内容因未知键在此失败，走下方 v1 降级
                val items = strictJson.decodeFromString<List<PlaylistItem>>(content)
                // 防御空壳：全部负载为空说明内容并非合法 v2，强制走 v1 降级
                if (items.isNotEmpty() && items.all { it.song == null && it.cloudSong == null && it.radio == null }) {
                    error("empty-shell playlist items, fallback to v1")
                }
                items
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
