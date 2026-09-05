package com.rcmiku.music.viewModel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.ncmapi.api.playlist.PlaylistApi
import com.rcmiku.ncmapi.model.PlaylistDetailResponse
import com.rcmiku.ncmapi.model.PlaylistInfoResponse
import com.rcmiku.ncmapi.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        const val KEY_PLAYLIST_ID = "playlistId"
        const val KEY_LIMIT = "limit"
        const val KEY_NO_CACHE = "noCache"
        private const val DEFAULT_LIMIT = 1000
    }

    private val playlistId = savedStateHandle.get<Long>(KEY_PLAYLIST_ID)
    private val limit = savedStateHandle.get<Int>(KEY_LIMIT)
    private val noCache = savedStateHandle.get<Boolean>(KEY_NO_CACHE) ?: false

    private val _playlistDetail = MutableStateFlow<PlaylistDetailResponse?>(null)
    val playlistDetail: StateFlow<PlaylistDetailResponse?> = _playlistDetail.asStateFlow()

    private val _playlistInfo = MutableStateFlow<PlaylistInfoResponse?>(null)
    val playlistInfo: StateFlow<PlaylistInfoResponse?> = _playlistInfo.asStateFlow()

    private val _tracks = MutableStateFlow<List<Song>>(emptyList())
    val tracks: StateFlow<List<Song>> = _tracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    private val _actionError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val actionError: SharedFlow<Unit> = _actionError.asSharedFlow()

    private var originalOrder: Map<Long, Int> = emptyMap()
    private var observerStarted = false

    init {
        load()
    }

    fun retry() = load()

    fun load() {
        _loadError.value = false
        val id = playlistId
        if (id == null) {
            _loadError.value = true
            return
        }

        if (noCache) {
            if (!observerStarted) {
                observerStarted = true
                fetchWithObserver()
            }
            fetchPlaylistInfo()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val effectiveLimit = limit?.takeIf { it > 0 } ?: DEFAULT_LIMIT
            val result = PlaylistApi.playlistDetail(id = id, limit = effectiveLimit)
            result.onSuccess { detail ->
                applyDetail(detail)
            }.onFailure {
                if (_playlistDetail.value == null) {
                    _loadError.value = true
                } else {
                    _actionError.tryEmit(Unit)
                }
            }
            _isLoading.value = false
            fetchPlaylistInfo()
        }
    }

    private fun applyDetail(detail: PlaylistDetailResponse) {
        _playlistDetail.value = detail
        val songList = detail.playlist.tracks
        _tracks.value = songList
        originalOrder = songList.mapIndexed { index, song -> song.id to index }.toMap()
    }

    private fun insertByOriginal(list: List<Song>, song: Song): List<Song> {
        val targetOrder = originalOrder[song.id] ?: Int.MAX_VALUE
        val mutable = list.toMutableList()
        val index = mutable.indexOfFirst { (originalOrder[it.id] ?: Int.MAX_VALUE) > targetOrder }
        if (index >= 0) {
            mutable.add(index, song)
        } else {
            mutable.add(song)
        }
        return mutable
    }

    @OptIn(FlowPreview::class)
    private fun fetchWithObserver() {
        val id = playlistId ?: return
        val effectiveLimit = limit?.takeIf { it > 0 } ?: DEFAULT_LIMIT
        viewModelScope.launch {
            _isLoading.value = true
            context.favoriteSongIdsDatastore.data
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest {
                    PlaylistApi.playlistV6DetailEapi(id = id, n = effectiveLimit).fold(
                        onSuccess = { detail ->
                            applyDetail(detail)
                            _isLoading.value = false
                        },
                        onFailure = {
                            if (_playlistDetail.value == null) {
                                _loadError.value = true
                            }
                            _isLoading.value = false
                        }
                    )
                }
        }
    }

    private fun fetchPlaylistInfo() {
        viewModelScope.launch {
            playlistId?.let { id ->
                PlaylistApi.playlistInfo(id).onSuccess { info ->
                    _playlistInfo.value = info
                }
            }
        }
    }

    fun playlistSub(isSub: Boolean) {
        val id = playlistId
        if (id == null) {
            _actionError.tryEmit(Unit)
            return
        }
        viewModelScope.launch {
            PlaylistApi.playlistSub(id = id, targetState = !isSub).fold(
                onSuccess = {
                    fetchPlaylistInfo()
                },
                onFailure = {
                    _actionError.tryEmit(Unit)
                }
            )
        }
    }

    fun deleteTrack(trackId: Long, onResult: (Boolean) -> Unit) {
        val currentTracks = _tracks.value
        val songToDelete = currentTracks.find { it.id == trackId }
        if (songToDelete == null) {
            onResult(false)
            return
        }

        _tracks.value = currentTracks.filterNot { it.id == trackId }

        val pid = playlistId
        if (pid == null) {
            _tracks.value = insertByOriginal(_tracks.value, songToDelete)
            onResult(false)
            return
        }

        viewModelScope.launch {
            val result = PlaylistApi.playlistTracksManipulate(
                op = "del",
                pid = pid,
                trackIds = listOf(trackId)
            )
            if (result.isSuccess) {
                onResult(true)
            } else {
                _tracks.value = insertByOriginal(_tracks.value, songToDelete)
                onResult(false)
            }
        }
    }

    fun addTrack(song: Song, onResult: (Boolean) -> Unit = {}) {
        val pid = playlistId
        if (pid == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val result = PlaylistApi.playlistTracksManipulate(
                op = "add",
                pid = pid,
                trackIds = listOf(song.id)
            )
            if (result.isSuccess) {
                _tracks.value = insertByOriginal(_tracks.value, song)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }
}