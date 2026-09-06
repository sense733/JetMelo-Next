package com.rcmiku.music.viewModel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.data.repository.PlaylistRepository
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.ncmapi.api.playlist.PlaylistApi
import com.rcmiku.ncmapi.model.PlaylistDetailResponse
import com.rcmiku.ncmapi.model.PlaylistInfoResponse
import com.rcmiku.ncmapi.model.Song
import androidx.navigation.toRoute
import com.rcmiku.music.ui.navigation.PlaylistNav
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
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    companion object {
        const val KEY_PLAYLIST_ID = "playlistId"
        const val KEY_LIMIT = "limit"
        const val KEY_NO_CACHE = "noCache"
        private const val DEFAULT_LIMIT = 1000
    }

    private val nav: PlaylistNav? = runCatching { savedStateHandle.toRoute<PlaylistNav>() }.getOrNull()
    private val playlistId: Long? = nav?.playlistId ?: savedStateHandle.get<Long>(KEY_PLAYLIST_ID)
    private val limit: Int? = nav?.limit ?: savedStateHandle.get<Int>(KEY_LIMIT)
    private val noCache: Boolean = nav?.noCache ?: savedStateHandle.get<Boolean>(KEY_NO_CACHE) ?: false

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
        playlistId?.let { id ->
            playlistRepository.getCachedPlaylist(id)?.let { cached ->
                applyDetail(cached.detail, isFromCache = true)
                cached.info?.let { _playlistInfo.value = it }
            }
        }
        load()
    }

    fun retry() = load(forceRefresh = true)

    fun load(forceRefresh: Boolean = false) {
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
            fetchPlaylistInfo(forceRefresh = forceRefresh)
            return
        }

        viewModelScope.launch {
            val cached = playlistRepository.getCachedPlaylist(id)
            val hasCache = cached != null
            if (!hasCache) {
                _isLoading.value = true
            }
            val effectiveLimit = limit?.takeIf { it > 0 } ?: DEFAULT_LIMIT
            val shouldForce = forceRefresh || cached?.isExpired() == true
            val result = playlistRepository.getPlaylistDetail(
                id = id,
                limit = effectiveLimit,
                forceRefresh = shouldForce
            )
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
            fetchPlaylistInfo(forceRefresh = shouldForce)
        }
    }

    private fun applyDetail(detail: PlaylistDetailResponse, isFromCache: Boolean = false) {
        _playlistDetail.value = detail
        val songList = detail.playlist.tracks
        _tracks.value = songList
        originalOrder = songList.mapIndexed { index, song -> song.id to index }.toMap()
        if (!isFromCache) {
            val id = playlistId ?: detail.playlist.id
            if (id != 0L) {
                playlistRepository.putCachedDetail(id, detail)
            }
        }
        if (noCache && songList.isNotEmpty()) {
            viewModelScope.launch {
                FavoriteSongIdsUtil.mergeSongIds(context, songList.map { it.id })
            }
        }
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
            if (_playlistDetail.value == null) {
                _isLoading.value = true
            }
            context.favoriteSongIdsDatastore.data
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest {
                    playlistRepository.getPlaylistV6DetailEapi(id = id, limit = effectiveLimit).fold(
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

    private fun fetchPlaylistInfo(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            playlistId?.let { id ->
                playlistRepository.getPlaylistInfo(id, forceRefresh = forceRefresh).onSuccess { info ->
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
            val targetState = !isSub
            PlaylistApi.playlistSub(id = id, targetState = targetState).fold(
                onSuccess = {
                    playlistRepository.updateSubscribed(id, targetState)
                    fetchPlaylistInfo(forceRefresh = true)
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

        val updatedTracks = currentTracks.filterNot { it.id == trackId }
        _tracks.value = updatedTracks

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
                playlistRepository.updateTracks(pid, updatedTracks)
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
        val currentTracks = _tracks.value
        val updatedTracks = insertByOriginal(currentTracks, song)
        viewModelScope.launch {
            val result = PlaylistApi.playlistTracksManipulate(
                op = "add",
                pid = pid,
                trackIds = listOf(song.id)
            )
            if (result.isSuccess) {
                _tracks.value = updatedTracks
                playlistRepository.updateTracks(pid, updatedTracks)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }
}