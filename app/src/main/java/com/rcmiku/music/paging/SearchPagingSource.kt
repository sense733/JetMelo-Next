package com.rcmiku.music.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.search.SearchApi
import com.rcmiku.ncmapi.api.search.SearchType
import com.rcmiku.ncmapi.model.SearchResources

import kotlinx.coroutines.CancellationException

class SearchPagingSource(
    private val keyword: String,
    private val searchType: SearchType
) : PagingSource<Int, SearchResources>() {
    override fun getRefreshKey(state: PagingState<Int, SearchResources>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            (anchorPage?.prevKey?.plus(anchorPage.data.size)
                ?: anchorPage?.nextKey?.minus(anchorPage.data.size))?.coerceAtLeast(0)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResources> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize

            val response = SearchApi.search(offset, limit, keyword, searchType)
            if (response.isSuccess) {
                val searchResponse = response.getOrThrow()
                val data = searchResponse.data.resources
                val nextKey = if (searchResponse.data.more && data.isNotEmpty()) offset + data.size else null
                val prevKey = if (offset == 0) null else (offset - limit).coerceAtLeast(0)

                LoadResult.Page(
                    data = data,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Error(response.exceptionOrNull() ?: Exception("Load data failed"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
