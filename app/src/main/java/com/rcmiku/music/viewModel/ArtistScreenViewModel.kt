package com.rcmiku.music.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.rcmiku.music.paging.ArtistAlbumPagingSource
import com.rcmiku.ncmapi.api.artist.ArtistApi
import com.rcmiku.ncmapi.model.ArtistHeadInfoResponse
import com.rcmiku.ncmapi.model.ArtistTopSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val artistId = savedStateHandle.get<Long>("artistId")

    private val _artistHeadInfo =
        MutableStateFlow<ArtistHeadInfoResponse?>(null)
    val artistHeadInfo: StateFlow<ArtistHeadInfoResponse?> =
        _artistHeadInfo.asStateFlow()

    private val _artistTopSong =
        MutableStateFlow<ArtistTopSong?>(null)
    val artistTopSong: StateFlow<ArtistTopSong?> =
        _artistTopSong.asStateFlow()

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
        val id = artistId
        if (id == null) {
            _loadError.value = true
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val headInfoDeferred = async { ArtistApi.artistHeadInfo(id) }
            val topSongDeferred = async { ArtistApi.artistTopSong(id) }

            val headInfoResult = headInfoDeferred.await()
            val topSongResult = topSongDeferred.await()

            var failed = false
            headInfoResult.onSuccess {
                _artistHeadInfo.value = it
            }.onFailure {
                failed = true
            }

            topSongResult.onSuccess {
                _artistTopSong.value = it
            }.onFailure {
                failed = true
            }

            if (failed) {
                if (_artistHeadInfo.value == null && _artistTopSong.value == null) {
                    _loadError.value = true
                } else {
                    _actionError.tryEmit(Unit)
                }
            }

            _isLoading.value = false
        }
    }

    val artistAlbumList = artistId?.let { id ->
        Pager(
            config = PagingConfig(
                pageSize = 30,
                prefetchDistance = 15,
                enablePlaceholders = false,
                initialLoadSize = 60
            ),
            pagingSourceFactory = { ArtistAlbumPagingSource(id) }
        ).flow.cachedIn(viewModelScope)
    } ?: flowOf()
}