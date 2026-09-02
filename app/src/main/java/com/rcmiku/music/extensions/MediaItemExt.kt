package com.rcmiku.music.extensions

import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.rcmiku.ncmapi.api.player.PlayerApi
import com.rcmiku.ncmapi.api.player.SongLevel
import com.rcmiku.ncmapi.model.CloudSong
import com.rcmiku.ncmapi.model.Radio
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.utils.json

fun Song.toMediaItem() =
    MediaItem.Builder()
        .setUri(this.id.toString())
        .setMediaId(this.id.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setArtist(this.ar.joinToString("/") { it.name })
                .setTitle(this.name)
                .setArtworkUri(this.al.picUrl.toUri())
                .setExtras(Bundle().apply {
                    putString(
                        "song",
                        json.encodeToString(this@toMediaItem)
                    )
                })
                .build()
        )
        .build()


fun List<Song>.toMediaItemList(): List<MediaItem> =
    this.map { it.toMediaItem() }

fun CloudSong.toMediaItem(uid: Long): MediaItem =
    MediaItem.Builder()
        .setUri("${this.simpleSong.id}_$uid")
        .setMediaId(this.simpleSong.id.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setArtist(this.artist)
                .setTitle(this.simpleSong.name)
                .setArtworkUri(this.simpleSong.al?.picUrl?.toUri())
                .build()
        )
        .build()

fun List<CloudSong>.toCloudSongMediaItemList(uid: Long): List<MediaItem> =
    this.map { it.toMediaItem(uid) }

fun Radio.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setUri(this.mainSong.id.toString())
        .setMediaId(this.mainSong.id.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setArtist(this.mainSong.artists.joinToString { it.name })
                .setTitle(this.mainSong.name)
                .setArtworkUri(this.coverUrl.toUri())
                .build()
        )
        .build()

fun List<Radio>.toRadioMediaItemList(): List<MediaItem> =
    this.map { it.toMediaItem() }

suspend fun updateMediaItemUri(songId: String, songLevel: SongLevel): Uri? {
    return PlayerApi.songPlayUrlV1(songId, songLevel = songLevel)
        .getOrNull()?.data?.firstOrNull()?.url?.toUri()
}