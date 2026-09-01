package com.rcmiku.music.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.rcmiku.music.data.searchHistoryDataStore
import com.rcmiku.music.paging.SearchPagingSource
import com.rcmiku.ncmapi.api.search.SearchApi
import com.rcmiku.ncmapi.api.search.SearchType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchSuggestion {
    data class Keyword(val keyword: String) : SearchSuggestion
    data class Song(
        val id: Long,
        val name: String,
        val artistName: String,
        val coverUrl: String? = null
    ) : SearchSuggestion
    data class Artist(val id: Long, val name: String, val avatarUrl: String?) : SearchSuggestion
    data class Album(val id: Long, val name: String, val artistName: String) : SearchSuggestion
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        val DEFAULT_HOT_SEARCHES = listOf(
            "周杰伦", "林俊杰", "Taylor Swift", "陈奕迅", "K-Pop Hits", "Billboard Hot 100", "陶喆", "告五人"
        )
    }

    private val _searchType = MutableStateFlow<SearchType>(SearchType.Song)
    val searchType: StateFlow<SearchType> = _searchType.asStateFlow()

    private val _searchValue = MutableStateFlow("")
    val searchValue: StateFlow<String> = _searchValue.asStateFlow()

    private val _inputQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val suggestions: StateFlow<List<SearchSuggestion>> = _inputQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                flow {
                    emit(fetchSuggestionsInternal(query))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _hotSearches = MutableStateFlow<List<String>>(emptyList())
    val hotSearches: StateFlow<List<String>> = _hotSearches.asStateFlow()

    private val _isHotLoading = MutableStateFlow(true)
    val isHotLoading: StateFlow<Boolean> = _isHotLoading.asStateFlow()

    val searchHistory: Flow<List<String>> = getSearchHistory(context).stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults = _searchValue.filter { it.isNotEmpty() }
        .combine(_searchType) { keyword, searchType ->
            Pager(
                config = PagingConfig(
                    pageSize = 100,
                    prefetchDistance = 50,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = { SearchPagingSource(keyword, searchType) }
            ).flow
        }
        .flatMapLatest { it }
        .cachedIn(viewModelScope)

    init {
        fetchHotSearches()
    }

    fun onInputQueryChange(query: String) {
        _inputQuery.value = query
    }

    fun updateSearchType(searchType: SearchType) {
        _searchType.value = searchType
    }

    fun updateSearchValue(searchValue: String) {
        _searchValue.value = searchValue
        saveSearch(searchValue)
    }

    private suspend fun fetchSuggestionsInternal(query: String): List<SearchSuggestion> = coroutineScope {
        val webDeferred = async { SearchApi.searchSuggestWeb(query).getOrNull()?.result }
        val keywordDeferred = async { SearchApi.searchSuggestKeyword(query).getOrNull()?.result }

        val webResult = webDeferred.await()
        val keywordResult = keywordDeferred.await()

        val resultList = mutableListOf<SearchSuggestion>()
        val addedKeywords = mutableSetOf<String>()

        webResult?.artists?.firstOrNull()?.let { artist ->
            if (artist.name.isNotBlank()) {
                resultList.add(
                    SearchSuggestion.Artist(
                        id = artist.id,
                        name = artist.name,
                        avatarUrl = artist.picUrl ?: artist.img1v1Url
                    )
                )
                addedKeywords.add(artist.name.lowercase())
            }
        }

        webResult?.songs?.take(3)?.forEach { song ->
            if (song.name.isNotBlank()) {
                resultList.add(
                    SearchSuggestion.Song(
                        id = song.id,
                        name = song.name,
                        artistName = song.artistName,
                        coverUrl = song.album?.picUrl
                    )
                )
            }
        }

        webResult?.albums?.firstOrNull()?.let { album ->
            if (album.name.isNotBlank()) {
                resultList.add(
                    SearchSuggestion.Album(
                        id = album.id,
                        name = album.name,
                        artistName = album.artistName
                    )
                )
            }
        }

        val rawKeywords = keywordResult?.allMatch?.map { it.keyword }
            ?: webResult?.allMatch?.map { it.keyword }
            ?: emptyList()

        rawKeywords.forEach { kw ->
            val trimmed = kw.trim()
            if (trimmed.isNotBlank() && addedKeywords.add(trimmed.lowercase())) {
                resultList.add(SearchSuggestion.Keyword(trimmed))
            }
        }

        resultList
    }

    private fun fetchHotSearches() {
        viewModelScope.launch {
            _isHotLoading.value = true
            val hots = SearchApi.searchHot().getOrNull()?.result?.hots
                ?.map { it.first }
                ?.filter { it.isNotBlank() }

            if (!hots.isNullOrEmpty()) {
                _hotSearches.value = hots
            } else {
                _hotSearches.value = DEFAULT_HOT_SEARCHES
            }
            _isHotLoading.value = false
        }
    }

    private fun saveSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            saveSearchQuery(context, query)
        }
    }

    private suspend fun saveSearchQuery(context: Context, query: String) {
        context.searchHistoryDataStore.updateData { currentHistory ->
            val historyList = currentHistory.historyList.toMutableList()
            historyList.remove(query)
            historyList.add(0, query)
            if (historyList.size > 10) historyList.removeAt(historyList.lastIndex)
            currentHistory.toBuilder().clearHistory().addAllHistory(historyList).build()
        }
    }

    private fun getSearchHistory(context: Context): Flow<List<String>> {
        return context.searchHistoryDataStore.data.map { it.historyList }
    }

    fun deleteSearchQuery(query: String) {
        viewModelScope.launch {
            removeSearchQuery(context, query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            context.searchHistoryDataStore.updateData { currentHistory ->
                currentHistory.toBuilder().clearHistory().build()
            }
        }
    }

    private suspend fun removeSearchQuery(context: Context, query: String) {
        context.searchHistoryDataStore.updateData { currentHistory ->
            val historyList = currentHistory.historyList.toMutableList()
            historyList.remove(query)
            currentHistory.toBuilder().clearHistory().addAllHistory(historyList).build()
        }
    }
}