package com.rcmiku.music.viewModel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.cache.LyricDiskCache
import com.rcmiku.music.data.repository.SimpleLruCache
import com.rcmiku.music.utils.LrcLine
import com.rcmiku.music.utils.parseLrc
import com.rcmiku.music.utils.parseYrc
import com.rcmiku.ncmapi.api.player.PlayerApi
import com.rcmiku.ncmapi.model.LyricResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

@Immutable
data class ParsedLyric(
    val musicId: Long,
    val lines: List<LrcLine>,
    val translations: List<String?>
)

internal fun matchLyricTranslations(
    lines: List<LrcLine>,
    tLrcLines: List<LrcLine>?
): List<String?> {
    if (lines.isEmpty() || tLrcLines.isNullOrEmpty()) {
        return List(lines.size) { null }
    }
    val exactMap = HashMap<Long, String>(tLrcLines.size)
    for (entry in tLrcLines) {
        exactMap[entry.time] = entry.text
    }
    val sortedT = tLrcLines.sortedBy { it.time }

    return lines.map { line ->
        if (line.text.isEmpty()) return@map null
        exactMap[line.time]?.let { return@map it }

        val targetMin = line.time - 100L
        val targetMax = line.time + 100L
        var low = 0
        var high = sortedT.lastIndex
        var startIdx = sortedT.size
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (sortedT[mid].time >= targetMin) {
                startIdx = mid
                high = mid - 1
            } else {
                low = mid + 1
            }
        }

        var bestText: String? = null
        var bestDiff = Long.MAX_VALUE
        var idx = startIdx
        while (idx < sortedT.size) {
            val candidate = sortedT[idx]
            if (candidate.time > targetMax) break
            val diff = abs(candidate.time - line.time)
            if (diff < bestDiff) {
                bestDiff = diff
                bestText = candidate.text
            }
            idx++
        }
        bestText
    }
}

internal fun parseLyricResponse(musicId: Long, response: LyricResponse): ParsedLyric {
    val parsedYrc = response.yrc?.lyric
        ?.takeIf { it.isNotBlank() }
        ?.parseYrc()
        ?.takeIf { it.isNotEmpty() }
    val lines = parsedYrc ?: response.lrc?.lyric?.parseLrc().orEmpty()
    val rawTranslation = if (parsedYrc != null && !response.ytlrc?.lyric.isNullOrBlank()) {
        response.ytlrc?.lyric
    } else {
        response.tlyric?.lyric
    }
    val tLrcLines = rawTranslation?.parseLrc()?.filter { it.text.isNotBlank() }
    val translations = matchLyricTranslations(lines, tLrcLines)
    return ParsedLyric(
        musicId = musicId,
        lines = lines,
        translations = translations
    )
}

@HiltViewModel
class LyricViewModel internal constructor(
    private val lyricFetcher: suspend (Long) -> Result<LyricResponse>,
    scopeOverride: CoroutineScope? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val diskCacheReader: ((Long) -> LyricResponse?)? = { LyricDiskCache.get(it) },
    private val diskCacheWriter: ((Long, LyricResponse) -> Unit)? = { id, resp -> LyricDiskCache.put(id, resp) }
) : ViewModel() {

    companion object {
        private const val MAX_CACHE_SIZE = 50
    }

    @Inject
    constructor() : this(PlayerApi::songLyric, null)

    private val activeScope: CoroutineScope = scopeOverride ?: viewModelScope
    private val lyricCache = SimpleLruCache<Long, Pair<LyricResponse, ParsedLyric>>(MAX_CACHE_SIZE)

    private data class LyricRequest(val musicId: Long? = null, val nonce: Int = 0)

    private val currentRequest = MutableStateFlow(LyricRequest())

    private val _lyricError = MutableStateFlow<Throwable?>(null)
    val lyricError: StateFlow<Throwable?> = _lyricError.asStateFlow()

    private val _errorMusicId = MutableStateFlow<Long?>(null)
    val errorMusicId: StateFlow<Long?> = _errorMusicId.asStateFlow()

    private val _loadedMusicId = MutableStateFlow<Long?>(null)
    val loadedMusicId: StateFlow<Long?> = _loadedMusicId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val lyricState: StateFlow<Pair<LyricResponse?, ParsedLyric?>> = currentRequest
        .flatMapLatest { request ->
            flow {
                val musicId = request.musicId
                if (musicId == null) {
                    _loadedMusicId.value = null
                    _errorMusicId.value = null
                    _lyricError.value = null
                    emit(null to null)
                    return@flow
                }

                val cached = lyricCache.get(musicId)
                if (cached != null) {
                    _errorMusicId.value = null
                    _lyricError.value = null
                    _loadedMusicId.value = musicId
                    emit(cached)
                    return@flow
                }

                _loadedMusicId.value = null
                _errorMusicId.value = null
                _lyricError.value = null
                emit(null to null)

                val diskResponse = withContext(ioDispatcher) {
                    diskCacheReader?.invoke(musicId)
                }
                currentCoroutineContext().ensureActive()
                if (diskResponse != null) {
                    val diskParsed = withContext(defaultDispatcher) {
                        parseLyricResponse(musicId, diskResponse)
                    }
                    currentCoroutineContext().ensureActive()
                    lyricCache.put(musicId, diskResponse to diskParsed)
                    _loadedMusicId.value = musicId
                    emit(diskResponse to diskParsed)
                    return@flow
                }

                val result = withContext(ioDispatcher) {
                    lyricFetcher(musicId)
                }
                currentCoroutineContext().ensureActive()
                val failure = result.exceptionOrNull()
                if (failure is CancellationException) {
                    throw failure
                }
                if (failure != null) {
                    _errorMusicId.value = musicId
                    _lyricError.value = failure
                }
                val response = result.getOrNull()
                val parsed = if (response != null) {
                    withContext(defaultDispatcher) {
                        parseLyricResponse(musicId, response)
                    }
                } else {
                    null
                }
                currentCoroutineContext().ensureActive()
                if (response != null && parsed != null) {
                    withContext(ioDispatcher) {
                        diskCacheWriter?.invoke(musicId, response)
                    }
                    lyricCache.put(musicId, response to parsed)
                    _loadedMusicId.value = musicId
                }
                emit(response to parsed)
            }
        }
        .stateIn(
            scope = activeScope,
            started = SharingStarted.Eagerly,
            initialValue = null to null
        )

    val lyric: StateFlow<LyricResponse?> = lyricState
        .map { it.first }
        .stateIn(
            scope = activeScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val parsedLyric: StateFlow<ParsedLyric?> = lyricState
        .map { it.second }
        .stateIn(
            scope = activeScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    internal fun peekCachedParsedLyric(musicId: Long): ParsedLyric? =
        lyricCache.get(musicId)?.second

    fun fetchLyric(musicId: Long) {
        val existing = currentRequest.value
        if (existing.musicId == musicId && _lyricError.value == null) {
            return
        }
        _errorMusicId.value = null
        _lyricError.value = null
        currentRequest.value = LyricRequest(musicId = musicId, nonce = existing.nonce + 1)
    }

    fun clearLyric() {
        _errorMusicId.value = null
        _lyricError.value = null
        val existing = currentRequest.value
        existing.musicId?.let { lyricCache.remove(it) }
        if (existing.musicId != null || lyricState.value.first != null) {
            currentRequest.value = LyricRequest(musicId = null, nonce = existing.nonce + 1)
        }
    }
}