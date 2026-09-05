package com.rcmiku.music.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.ncmapi.api.album.AlbumApi
import com.rcmiku.ncmapi.model.AlbumDetailResponse
import com.rcmiku.ncmapi.model.AlbumInfoResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) :
    ViewModel() {
    private val albumId = savedStateHandle.get<Long>("albumId")

    private val _albumDetail =
        MutableStateFlow<Result<AlbumDetailResponse>?>(null)
    val albumDetail: StateFlow<Result<AlbumDetailResponse>?> =
        _albumDetail.asStateFlow()

    private val _albumInfo =
        MutableStateFlow<AlbumInfoResponse?>(null)
    val albumInfo: StateFlow<AlbumInfoResponse?> =
        _albumInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    private val _actionError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val actionError: SharedFlow<Unit> = _actionError.asSharedFlow()

    init {
        load()
    }

    fun retry() = load()

    fun load() {
        _loadError.value = false
        val id = albumId
        if (id == null) {
            _loadError.value = true
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val detailDeferred = async { AlbumApi.albumDetail(id) }
            val infoDeferred = async { AlbumApi.albumInfo(id) }

            val detailResult = detailDeferred.await()
            val infoResult = infoDeferred.await()

            detailResult.onSuccess { detail ->
                _albumDetail.value = Result.success(detail)
            }.onFailure {
                if (_albumDetail.value == null) {
                    _loadError.value = true
                } else {
                    _actionError.tryEmit(Unit)
                }
            }

            infoResult.onSuccess { info ->
                _albumInfo.value = info
            }.onFailure {
                _actionError.tryEmit(Unit)
            }

            _isLoading.value = false
        }
    }

    private fun fetchAlbumInfo() {
        val id = albumId ?: return
        viewModelScope.launch {
            AlbumApi.albumInfo(id).fold(
                onSuccess = { _albumInfo.value = it },
                onFailure = { _actionError.tryEmit(Unit) }
            )
        }
    }

    fun albumSub(isSub: Boolean) {
        val id = albumId
        if (id == null) {
            _actionError.tryEmit(Unit)
            return
        }
        viewModelScope.launch {
            AlbumApi.albumSub(id = id, targetState = !isSub).fold(
                onSuccess = {
                    fetchAlbumInfo()
                },
                onFailure = {
                    _actionError.tryEmit(Unit)
                }
            )
        }
    }
}