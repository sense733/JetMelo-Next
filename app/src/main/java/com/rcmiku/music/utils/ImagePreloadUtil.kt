package com.rcmiku.music.utils

import android.content.Context
import androidx.media3.common.MediaItem
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.rcmiku.ncmapi.model.Album
import com.rcmiku.ncmapi.model.Playlist
import com.rcmiku.ncmapi.model.Song

/**
 * 图像预加载工具类
 * 负责在数据模型获取后提前触发 Coil 内存/磁盘缓存预热，规避滑动到屏幕时产生的网络与解码延迟。
 */
object ImagePreloadUtil {
    val ThumbnailRequestSizePx = Size(176, 176)

    fun preloadSongThumbnail(context: Context, song: Song) {
        val artworkUrl = song.al.picUrl
        if (artworkUrl.isEmpty()) return
        val imageKey = artworkUrl.ifEmpty { song.id.toString() }
        val request = ImageRequest.Builder(context)
            .data(artworkUrl)
            .size(ThumbnailRequestSizePx)
            .memoryCacheKey(imageKey)
            .diskCacheKey(imageKey)
            .crossfade(false)
            .build()
        SingletonImageLoader.get(context).enqueue(request)
    }

    fun preloadSongThumbnails(context: Context, songs: List<Song>, limit: Int = 80) {
        if (songs.isEmpty()) return
        val targetSongs = if (limit > 0) songs.take(limit) else songs
        val loader = SingletonImageLoader.get(context)
        for (song in targetSongs) {
            val artworkUrl = song.al.picUrl
            if (artworkUrl.isNotEmpty()) {
                val imageKey = artworkUrl.ifEmpty { song.id.toString() }
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .size(ThumbnailRequestSizePx)
                    .memoryCacheKey(imageKey)
                    .diskCacheKey(imageKey)
                    .crossfade(false)
                    .build()
                loader.enqueue(request)
            }
        }
    }

    fun preloadMediaItems(context: Context, items: List<MediaItem>, limit: Int = 80) {
        if (items.isEmpty()) return
        val targetItems = if (limit > 0) items.take(limit) else items
        val loader = SingletonImageLoader.get(context)
        for (item in targetItems) {
            val artworkUri = item.mediaMetadata.artworkUri ?: continue
            val key = artworkUri.toString()
            if (key.isNotEmpty()) {
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(ThumbnailRequestSizePx)
                    .memoryCacheKey(key)
                    .diskCacheKey(key)
                    .crossfade(false)
                    .build()
                loader.enqueue(request)
            }
        }
    }

    fun preloadCover(context: Context, id: Long?, url: String?) {
        if (url.isNullOrEmpty()) return
        val coverKey = id?.let { "cover_$it" }
        val request = ImageRequest.Builder(context)
            .data(url)
            .apply {
                if (coverKey != null) {
                    memoryCacheKey(coverKey)
                    placeholderMemoryCacheKey(coverKey)
                }
            }
            .build()
        SingletonImageLoader.get(context).enqueue(request)
    }

    fun preloadPlaylistCovers(context: Context, playlists: List<Playlist>, limit: Int = 30) {
        if (playlists.isEmpty()) return
        val targetPlaylists = if (limit > 0) playlists.take(limit) else playlists
        val loader = SingletonImageLoader.get(context)
        for (playlist in targetPlaylists) {
            val coverUrl = playlist.cover
            if (coverUrl.isNotEmpty()) {
                val coverKey = "cover_${playlist.id}"
                val request = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .memoryCacheKey(coverKey)
                    .placeholderMemoryCacheKey(coverKey)
                    .build()
                loader.enqueue(request)
            }
        }
    }

    fun preloadAlbumCovers(context: Context, albums: List<Album>, limit: Int = 30) {
        if (albums.isEmpty()) return
        val targetAlbums = if (limit > 0) albums.take(limit) else albums
        val loader = SingletonImageLoader.get(context)
        for (album in targetAlbums) {
            val picUrl = album.picUrl
            if (picUrl.isNotEmpty()) {
                val coverKey = "cover_${album.id}"
                val request = ImageRequest.Builder(context)
                    .data(picUrl)
                    .memoryCacheKey(coverKey)
                    .placeholderMemoryCacheKey(coverKey)
                    .build()
                loader.enqueue(request)
            }
        }
    }
}
