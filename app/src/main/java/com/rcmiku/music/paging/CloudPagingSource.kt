package com.rcmiku.music.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.model.CloudSong

import kotlinx.coroutines.CancellationException

class CloudPagingSource : PagingSource<Int, CloudSong>() {
    override fun getRefreshKey(state: PagingState<Int, CloudSong>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            (anchorPage?.prevKey?.plus(anchorPage.data.size)
                ?: anchorPage?.nextKey?.minus(anchorPage.data.size))?.coerceAtLeast(0)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CloudSong> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize
            val response = AccountApi.cloudSong(offset = offset, limit = limit)
            if (response.isSuccess) {
                val cloudSongResponse = response.getOrThrow()
                val data = cloudSongResponse.data
                val nextKey = if (cloudSongResponse.hasMore && data.isNotEmpty()) offset + data.size else null
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