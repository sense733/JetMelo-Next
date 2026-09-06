package com.rcmiku.ncmapi.api.lyric

import com.rcmiku.ncmapi.api.player.PlayerApi
import com.rcmiku.ncmapi.model.LyricResponse

/**
 * 歌词 API 委托层。
 *
 * 纯委托给 [PlayerApi.songLyric] 实现，保持向后兼容性与领域聚合。
 */
object LyricApi {

    /**
     * 获取歌曲歌词，委托给 [PlayerApi.songLyric]。
     *
     * @param musicId 歌曲 ID
     * @return [Result] 包含歌词响应
     */
    suspend fun songLyric(musicId: Long): Result<LyricResponse> = PlayerApi.songLyric(musicId)
}
