package com.rcmiku.music.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.ncmapi.api.player.PlayerApi
import com.rcmiku.ncmapi.model.LyricResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LyricViewModel @Inject constructor() : ViewModel() {

    private val currentMusicId = MutableSharedFlow<Long?>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _lyricError = MutableStateFlow<Throwable?>(null)
    val lyricError: StateFlow<Throwable?> = _lyricError.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val lyric: StateFlow<LyricResponse?> = currentMusicId
        .flatMapLatest { musicId ->
            flow {
                emit(null)
                _lyricError.value = null
                if (musicId != null) {
                    val result = PlayerApi.songLyric(musicId)
                    result.onFailure {
                        _lyricError.value = it
                    }
                    emit(result.getOrNull())
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0),
            initialValue = null
        )

    /**
     * 拉取指定歌曲的歌词。
     *
     * @param musicId 歌曲 ID
     */
    fun fetchLyric(musicId: Long) {
        currentMusicId.tryEmit(musicId)
    }

    /**
     * 清理当前歌词状态与错误信息，供切至非数字 ID（如本地歌曲或空媒体项）时清理残留旧词。
     */
    fun clearLyric() {
        _lyricError.value = null
        currentMusicId.tryEmit(null)
    }
}