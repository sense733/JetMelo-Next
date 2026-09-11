package com.rcmiku.music.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.constants.userIdKey
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.music.utils.dataStore
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.recommend.RecommendApi
import com.rcmiku.ncmapi.model.DailySongsResponse
import com.rcmiku.ncmapi.model.PersonalizedPlaylistResponse
import com.rcmiku.ncmapi.model.RecommendPlaylistResponse
import com.rcmiku.music.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

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
                val storedUid = context.dataStore.data.map { it[userIdKey] ?: 0L }.first()
                val ids = AccountApi.favoriteSongIds(storedUid).getOrNull()?.ids
                if (!ids.isNullOrEmpty()) {
                    FavoriteSongIdsUtil.updateSongIds(context, ids)
                }
            }
        }
    }

    fun fetchRecommendPlaylist() {
        viewModelScope.launch {
            val res = RecommendApi.recommendPlaylist()
            _recommendPlaylist.value = res
            res.getOrNull()?.recommend?.map { it.id }?.let { playlistRepository.preloadPlaylists(it) }
        }
    }

    fun fetchPersonalizedPlaylist() {
        viewModelScope.launch {
            val res = RecommendApi.personalizedPlaylist()
            _personalizedPlaylist.value = res
            res.getOrNull()?.result?.map { it.id }?.let { playlistRepository.preloadPlaylists(it) }
        }
    }

    private suspend fun loadAll() = supervisorScope {
        launch { _recommendSongs.value = RecommendApi.recommendSongs() }
        launch {
            val res = RecommendApi.recommendPlaylist()
            _recommendPlaylist.value = res
            res.getOrNull()?.recommend?.map { it.id }?.let { playlistRepository.preloadPlaylists(it) }
        }
        launch {
            val res = RecommendApi.personalizedPlaylist()
            _personalizedPlaylist.value = res
            res.getOrNull()?.result?.map { it.id }?.let { playlistRepository.preloadPlaylists(it) }
        }
        launch {
            runCatching {
                val storedUid = context.dataStore.data.map { it[userIdKey] ?: 0L }.first()
                val ids = AccountApi.favoriteSongIds(storedUid).getOrNull()?.ids
                if (!ids.isNullOrEmpty()) {
                    FavoriteSongIdsUtil.updateSongIds(context, ids)
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
