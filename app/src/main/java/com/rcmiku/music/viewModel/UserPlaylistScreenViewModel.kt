package com.rcmiku.music.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.account.UserPlaylistType
import com.rcmiku.ncmapi.model.UserPlaylistResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserPlaylistScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) :
    ViewModel() {
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
        load()
    }

    fun retry() = load()

    fun load() {
        _loadError.value = false
        val id = userId
        val playlistType = userPlaylistType
        if (id == null || playlistType == null) {
            _loadError.value = true
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            AccountApi.userPlaylist(
                userId = id,
                userPlaylistType = playlistType
            ).fold(
                onSuccess = { response ->
                    _playlist.value = response
                },
                onFailure = {
                    _loadError.value = true
                }
            )
            _isLoading.value = false
        }
    }
}