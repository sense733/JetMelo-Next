package com.rcmiku.music.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.model.FavoriteSongResponse
import com.rcmiku.ncmapi.model.UserInfoBatch
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _userInfo = MutableStateFlow<UserInfoBatch?>(null)
    val userInfo: StateFlow<UserInfoBatch?> = _userInfo.asStateFlow()

    private val _favoriteSong = MutableStateFlow<FavoriteSongResponse?>(null)
    val favoriteSong: StateFlow<FavoriteSongResponse?> = _favoriteSong.asStateFlow()

    init {
        observeUserIdChanges()
    }

    fun fetchUserInfo() {
        viewModelScope.launch {
            _userInfo.value = AccountApi.accountInfo().getOrNull()
        }
    }

    private suspend fun fetchFavoriteSong(userId: Long) {
        val response = AccountApi.favoriteSong(userId).getOrNull()
        _favoriteSong.value = response
        response?.ids?.let { ids ->
            if (ids.isNotEmpty()) {
                FavoriteSongIdsUtil.mergeSongIds(context, ids)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeUserIdChanges() {
        viewModelScope.launch {
            userInfo
                .mapLatest { it?.account?.profile?.userId }
                .distinctUntilChanged()
                .collectLatest { userId ->
                    if (userId != null) {
                        fetchFavoriteSong(userId)
                    } else {
                        _favoriteSong.value = null
                    }
                }
        }
    }
}
