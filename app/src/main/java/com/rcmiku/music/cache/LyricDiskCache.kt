package com.rcmiku.music.cache

import android.content.Context
import com.rcmiku.ncmapi.model.LyricResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 歌词本地磁盘缓存，用于确保已缓存歌曲在离线状态下也能读取歌词。
 */
class LyricFileStore(
    private val dir: File,
    private val maxFiles: Int = DEFAULT_MAX_FILES
) {
    private val lock = Any()
    private val json = Json { ignoreUnknownKeys = true }

    fun get(musicId: Long): LyricResponse? = synchronized(lock) {
        val file = File(dir, "$musicId.json")
        if (!file.isFile) return@synchronized null
        return@synchronized runCatching {
            val content = file.readText()
            file.setLastModified(System.currentTimeMillis())
            json.decodeFromString<LyricResponse>(content)
        }.onFailure {
            file.delete()
        }.getOrNull()
    }

    fun put(musicId: Long, response: LyricResponse) = synchronized(lock) {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val target = File(dir, "$musicId.json")
        val temp = File(dir, "$musicId.json.tmp")
        runCatching {
            temp.writeText(json.encodeToString(response))
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            trimIfNeeded()
        }.onFailure {
            temp.delete()
        }
    }

    fun clear() = synchronized(lock) {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun trimIfNeeded() {
        val files = dir.listFiles { file -> file.isFile && file.name.endsWith(".json") } ?: return
        if (files.size <= maxFiles) return
        val excess = files.size - maxFiles
        files.sortedBy { it.lastModified() }
            .take(excess)
            .forEach { it.delete() }
    }

    companion object {
        private const val DEFAULT_MAX_FILES = 2000
    }
}

/**
 * 全局歌词磁盘缓存入口。
 */
object LyricDiskCache {

    private const val LYRIC_CACHE_DIR = "lyrics"

    @Volatile
    private var store: LyricFileStore? = null

    fun init(context: Context) {
        if (store == null) {
            synchronized(this) {
                if (store == null) {
                    val dir = File(context.applicationContext.cacheDir, LYRIC_CACHE_DIR).apply { mkdirs() }
                    store = LyricFileStore(dir)
                }
            }
        }
    }

    fun get(musicId: Long): LyricResponse? = store?.get(musicId)

    fun put(musicId: Long, response: LyricResponse) {
        store?.put(musicId, response)
    }

    fun clear() {
        store?.clear()
    }
}
