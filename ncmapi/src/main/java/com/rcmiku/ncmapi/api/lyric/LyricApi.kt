package com.rcmiku.ncmapi.api.lyric

import com.rcmiku.ncmapi.api.player.PlayerApi
import com.rcmiku.ncmapi.model.LyricResponse

object LyricApi {

    suspend fun songLyric(musicId: Long): Result<LyricResponse> = PlayerApi.songLyric(musicId)
}
