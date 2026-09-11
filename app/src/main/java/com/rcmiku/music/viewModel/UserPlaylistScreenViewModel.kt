package com.rcmiku.music.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.data.repository.PlaylistRepository
import com.rcmiku.music.data.repository.UserPlaylistRepository
import com.rcmiku.ncmapi.api.account.UserPlaylistType
import com.rcmiku.ncmapi.model.UserPlaylistResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserPlaylistScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userPlaylistRepository: UserPlaylistRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    private val userId = savedStateHandle.get<Long>("userId")
    private val type = savedStateHandle.get<String>("type")
    val userPlaylistType: UserPlaylistType? =
        type?.let { UserPlaylistType.entries.find { it.type == type } }

    private val _playlist =
        MutableStateFlow<UserPlaylistResponse?>(null)
    val playlist: StateFlow<UserPlaylistResponse?> =
        _playlist.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    init {
        val id = userId
        val playlistType = userPlaylistType
        if (id != null && playlistType != null) {
            userPlaylistRepository.getCachedUserPlaylist(id, playlistType.type)?.let { cached ->
                _playlist.value = cached
                playlistRepository.preloadPlaylists(cached.data.playlist.map { it.id })
            }
        }
        load()
    }

    fun retry() = load(forceRefresh = true)

    fun load(forceRefresh: Boolean = false) {
        _loadError.value = false
        val id = userId
        val playlistType = userPlaylistType
        if (id == null || playlistType == null) {
            _loadError.value = true
            return
        }

        viewModelScope.launch {
            val cached = userPlaylistRepository.getCachedUserPlaylist(id, playlistType.type)
            if (cached == null) {
                _isLoading.value = true
            }
            userPlaylistRepository.getUserPlaylist(
                userId = id,
                userPlaylistType = playlistType,
                forceRefresh = forceRefresh
            ).fold(
                onSuccess = { response ->
                    _playlist.value = response
                    playlistRepository.preloadPlaylists(response.data.playlist.map { it.id })
                },
                onFailure = {
                    if (_playlist.value == null) {
                        _loadError.value = true
                    }
                }
            )
            _isLoading.value = false
        }
    }
}