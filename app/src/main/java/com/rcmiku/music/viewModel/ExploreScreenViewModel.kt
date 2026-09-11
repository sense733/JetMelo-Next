package com.rcmiku.music.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.data.repository.PlaylistRepository
import com.rcmiku.ncmapi.api.explore.ExploreApi
import com.rcmiku.ncmapi.api.playlist.PlaylistApi
import com.rcmiku.ncmapi.api.recommend.RecommendApi
import com.rcmiku.ncmapi.model.NewAlbumResponse
import com.rcmiku.ncmapi.model.TopListResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreScreenViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _topList =
        MutableStateFlow<Result<TopListResponse>?>(null)
    val topList: StateFlow<Result<TopListResponse>?> =
        _topList.asStateFlow()

    private val _newAlbum =
        MutableStateFlow<Result<NewAlbumResponse>?>(null)
    val newAlbum: StateFlow<Result<NewAlbumResponse>?> =
        _newAlbum.asStateFlow()

    private val _allNewAlbum =
        MutableStateFlow<Result<NewAlbumResponse>?>(null)
    val allNewAlbum: StateFlow<Result<NewAlbumResponse>?> =
        _allNewAlbum.asStateFlow()

    fun fetchTopList() {
        viewModelScope.launch {
            val res = PlaylistApi.topList()
            _topList.value = res
            res.getOrNull()?.list?.take(10)?.map { it.id }?.let { playlistRepository.preloadPlaylists(it) }
        }
    }

    fun fetchNewAlbum() {
        viewModelScope.launch {
            _newAlbum.value = RecommendApi.newAlbum()
        }
    }

    fun fetchAllNewAlbum() {
        viewModelScope.launch {
            _allNewAlbum.value = ExploreApi.newAlbum(limit = 30)
        }
    }

    fun refresh() {
        fetchTopList()
        fetchNewAlbum()
        fetchAllNewAlbum()
    }

    init {
        refresh()
    }
}