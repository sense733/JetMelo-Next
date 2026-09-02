package com.rcmiku.music.viewModel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.ncmapi.api.playlist.PlaylistApi
import com.rcmiku.ncmapi.model.PlaylistDetailResponse
import com.rcmiku.ncmapi.model.PlaylistInfoResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) :
    ViewModel() {
    private val playlistId = savedStateHandle.get<Long>("playlistId")
    private val limit = savedStateHandle.get<Int>("limit")
    private val noCache = savedStateHandle.get<Boolean>("noCache") ?: false
    private val _playlistDetail =
        MutableStateFlow<PlaylistDetailResponse?>(null)
    val playlistDetail: StateFlow<PlaylistDetailResponse?> =
        _playlistDetail.asStateFlow()

    private val _playlistInfo =
        MutableStateFlow<PlaylistInfoResponse?>(null)
    val playlistInfo: StateFlow<PlaylistInfoResponse?> =
        _playlistInfo.asStateFlow()

    init {
        viewModelScope.launch {
            if (noCache)
                fetchWithObserver()
            else
                playlistId?.let {
                    limit?.let {
                        _playlistDetail.value = PlaylistApi.playlistDetail(
                            id = playlistId,
                            limit = limit,
                        ).getOrNull()
                    }
                    fetchPlaylistInfo()
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun fetchWithObserver() {
        viewModelScope.launch {
            context.favoriteSongIdsDatastore.data
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest {
                    playlistId?.let { id ->
                        PlaylistApi.playlistV6Detail(id = id).onSuccess { detail ->
                            _playlistDetail.value = detail
                        }
                    }
                }
        }
    }

    private fun fetchPlaylistInfo() {
        viewModelScope.launch {
            playlistId?.let {
                _playlistInfo.value = PlaylistApi.playlistInfo(it).getOrNull()
            }
        }
    }

    fun playlistSub(isSub: Boolean) {
        viewModelScope.launch {
            playlistId?.let {
                PlaylistApi.playlistSub(id = it, targetState = !isSub).onSuccess {
                    fetchPlaylistInfo()
                }
            }
        }
    }

    fun deleteTrack(trackId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val pid = playlistId
            if (pid != null) {
                val result = PlaylistApi.playlistTracksManipulate(
                    op = "del",
                    pid = pid,
                    trackIds = listOf(trackId)
                )
                onResult(result.isSuccess)
            } else {
                onResult(false)
            }
        }
    }
}