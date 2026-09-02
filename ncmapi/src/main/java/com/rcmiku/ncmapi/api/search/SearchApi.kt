package com.rcmiku.ncmapi.api.search

import com.rcmiku.ncmapi.model.PlaylistTerminalSearchResponse
import com.rcmiku.ncmapi.model.SearchData
import com.rcmiku.ncmapi.model.SearchResources
import com.rcmiku.ncmapi.model.SearchResponse
import com.rcmiku.ncmapi.model.SearchHotResponse
import com.rcmiku.ncmapi.model.SearchSuggestKeywordResponse
import com.rcmiku.ncmapi.model.WebSearchSuggestResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json

object SearchApi {
    suspend fun search(offset: Int = 0, limit: Int = 30, keyword: String, searchType: SearchType = SearchType.Song): Result<SearchResponse> {
        return runCatching {
            val (url, data) = when (searchType) {
                SearchType.Song -> "/api/search/resource/horizontal/song" to mapOf(
                    "offset" to offset.toString(),
                    "limit" to limit.toString(),
                    "keyword" to keyword,
                    "header" to "{}",
                    "e_r" to false
                )
                SearchType.Playlist -> "/api/search/multi/terminal/playlist/get" to mapOf(
                    "offset" to offset.toString(),
                    "limit" to limit.toString(),
                    "keyword" to keyword,
                    "header" to "{}",
                    "e_r" to false
                )
                else -> "/api/search/get" to mapOf(
                    "s" to keyword,
                    "type" to searchType.type,
                    "limit" to limit,
                    "offset" to offset
                )
            }
            val body = HttpManager.request(
                url = url,
                data = data,
                crypto = if (searchType == SearchType.Song || searchType == SearchType.Playlist) HttpManager.CryptoType.EAPI else HttpManager.CryptoType.WEAPI
            )
            when (searchType) {
                SearchType.Playlist -> {
                    val raw = json.decodeFromString(PlaylistTerminalSearchResponse.serializer(), body)
                    val resources = raw.data.resources.map { p ->
                        SearchResources(
                            resourceType = "playlist",
                            resourceId = p.id.toString(),
                            playlist = p
                        )
                    }
                    SearchResponse(
                        code = raw.code,
                        message = raw.message,
                        data = SearchData(
                            resources = resources,
                            more = raw.data.more,
                            totalCount = raw.data.totalCount,
                            moreText = raw.data.moreText
                        )
                    )
                }

                else -> json.decodeFromString(SearchResponse.serializer(), body)
            }
        }
    }
    
    suspend fun searchSuggestKeyword(keywords: String): Result<SearchSuggestKeywordResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/search/suggest/keyword",
                data = mapOf(
                    "s" to keywords
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(SearchSuggestKeywordResponse.serializer(), body)
        }
    }

    suspend fun searchSuggestWeb(keywords: String): Result<WebSearchSuggestResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/search/suggest/web",
                data = mapOf(
                    "s" to keywords
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(WebSearchSuggestResponse.serializer(), body)
        }
    }

    suspend fun searchHot(): Result<SearchHotResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/search/hot",
                data = mapOf("type" to 1111),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(SearchHotResponse.serializer(), body)
        }
    }
}
