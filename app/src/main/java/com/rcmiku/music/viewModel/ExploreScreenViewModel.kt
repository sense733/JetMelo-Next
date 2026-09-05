package com.rcmiku.music.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class ExploreScreenViewModel @Inject constructor() : ViewModel() {

    private val _topList =
        MutableStateFlow<Result<TopListResponse>?>(null)
    val topList: StateFlow<Result<TopListResponse>?> =
        _topList.asStateFlow()

    private val _newAlbum =
        MutableStateFlow<Result<NewAlbumResponse>?>(null)
    val newAlbum: StateFlow<Result<NewAlbumResponse>?> =
        _newAlbum.asStateFlow()

    fun fetchTopList() {
        viewModelScope.launch {
            _topList.value = PlaylistApi.topList()
        }
    }

    fun fetchNewAlbum() {
        viewModelScope.launch {
            _newAlbum.value = RecommendApi.newAlbum()
        }
    }

    fun refresh() {
        fetchTopList()
        fetchNewAlbum()
    }

    init {
        refresh()
    }
}