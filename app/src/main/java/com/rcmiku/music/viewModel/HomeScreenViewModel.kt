package com.rcmiku.music.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.recommend.RecommendApi
import com.rcmiku.ncmapi.model.DailySongsResponse
import com.rcmiku.ncmapi.model.PersonalizedPlaylistResponse
import com.rcmiku.ncmapi.model.RecommendPlaylistResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {

    private val _recommendSongs =
        MutableStateFlow<Result<DailySongsResponse>?>(null)
    val recommendSongs: StateFlow<Result<DailySongsResponse>?> =
        _recommendSongs.asStateFlow()
    private val _personalizedPlaylist =
        MutableStateFlow<Result<PersonalizedPlaylistResponse>?>(null)
    val personalizedPlaylist: StateFlow<Result<PersonalizedPlaylistResponse>?> =
        _personalizedPlaylist.asStateFlow()
    private val _recommendPlaylist =
        MutableStateFlow<Result<RecommendPlaylistResponse>?>(null)
    val recommendPlaylist: StateFlow<Result<RecommendPlaylistResponse>?> =
        _recommendPlaylist.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun fetchRecommendSongs() {
        viewModelScope.launch {
            _recommendSongs.value = RecommendApi.recommendSongs()
        }
    }

    fun fetchFavoriteSongIds() {
        viewModelScope.launch {
            runCatching {
                AccountApi.favoriteSongIds().getOrNull()?.ids?.let {
                    FavoriteSongIdsUtil.updateSongIds(context, it)
                }
            }
        }
    }

    fun fetchRecommendPlaylist() {
        viewModelScope.launch {
            _recommendPlaylist.value = RecommendApi.recommendPlaylist()
        }
    }

    fun fetchPersonalizedPlaylist() {
        viewModelScope.launch {
            _personalizedPlaylist.value = RecommendApi.personalizedPlaylist()
        }
    }

    private suspend fun loadAll() = supervisorScope {
        launch { _recommendSongs.value = RecommendApi.recommendSongs() }
        launch { _recommendPlaylist.value = RecommendApi.recommendPlaylist() }
        launch { _personalizedPlaylist.value = RecommendApi.personalizedPlaylist() }
        launch {
            runCatching {
                AccountApi.favoriteSongIds().getOrNull()?.ids?.let {
                    FavoriteSongIdsUtil.updateSongIds(context, it)
                }
            }
        }
    }

    init {
        viewModelScope.launch { loadAll() }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadAll()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

}
