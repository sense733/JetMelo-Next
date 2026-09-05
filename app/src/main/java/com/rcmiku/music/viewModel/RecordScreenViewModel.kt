package com.rcmiku.music.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.account.SongRecordType
import com.rcmiku.ncmapi.model.RecordResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val uid = savedStateHandle.get<Long>("uid")

    val isMissingUid = uid == null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _songRecord = MutableStateFlow<RecordResponse?>(null)
    private val _songRecordType = MutableStateFlow(SongRecordType.WEEK)
    private val songRecordType: StateFlow<SongRecordType> = _songRecordType.asStateFlow()
    val songRecord: StateFlow<RecordResponse?> = _songRecord.asStateFlow()

    fun updateSongRecordType(songRecordType: SongRecordType) {
        _songRecordType.value = songRecordType
    }

    private suspend fun fetchSongRecord(type: SongRecordType) {
        val currentUid = uid ?: return
        _isLoading.value = true
        try {
            _songRecord.value = AccountApi.songRecord(currentUid, type).getOrNull()
        } finally {
            _isLoading.value = false
        }
    }

    init {
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            songRecordType
                .debounce(200)
                .distinctUntilChanged()
                .collectLatest { type ->
                    fetchSongRecord(type)
                }
        }
    }
}