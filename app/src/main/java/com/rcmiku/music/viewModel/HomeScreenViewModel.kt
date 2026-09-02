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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    // 4.5：下拉刷新状态由真实加载完成态驱动，替代 UI 层的 delay(1000) 假完成
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private suspend fun fetchRecommendSongs() {
        _recommendSongs.value = RecommendApi.recommendSongs()
    }

    private suspend fun fetchFavoriteSongIds() {
        AccountApi.favoriteSongIds().getOrNull()?.ids?.let {
            runCatching {
                FavoriteSongIdsUtil.updateSongIds(context, it)
            }
        }
    }

    private suspend fun fetchRecommendPlaylist() {
        _recommendPlaylist.value = RecommendApi.recommendPlaylist()
    }

    private suspend fun fetchPersonalizedPlaylist() {
        _personalizedPlaylist.value = RecommendApi.personalizedPlaylist()
    }

    // 四路请求并行，全部结束后 isRefreshing 才复位
    private suspend fun loadAll() = coroutineScope {
        launch { fetchRecommendSongs() }
        launch { fetchRecommendPlaylist() }
        launch { fetchPersonalizedPlaylist() }
        launch { fetchFavoriteSongIds() }
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
