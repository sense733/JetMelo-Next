package com.rcmiku.music.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.model.CloudSong
import kotlinx.coroutines.CancellationException

class CloudPagingSource : PagingSource<Int, CloudSong>() {

    companion object {
        private const val MAX_LOAD_SIZE = 50
    }

    override fun getRefreshKey(state: PagingState<Int, CloudSong>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            // 假设严格单调线性 offset：prevKey 递进实际条数，nextKey 倒推并用 coerceAtLeast(0) 兜底越界
            (anchorPage?.prevKey?.plus(anchorPage.data.size)
                ?: anchorPage?.nextKey?.minus(anchorPage.data.size))?.coerceAtLeast(0)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CloudSong> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize.coerceAtMost(MAX_LOAD_SIZE).coerceAtLeast(1)
            val response = AccountApi.cloudSong(offset = offset, limit = limit)
            if (response.isSuccess) {
                val cloudSongResponse = response.getOrThrow()
                val data = cloudSongResponse.data
                // 严格单调 offset 假设：prevKey 递退 limit，nextKey 依据实际返回 data.size 递增
                val nextKey = if (cloudSongResponse.hasMore && data.isNotEmpty()) offset + data.size else null
                val prevKey = if (offset == 0) null else offset - limit
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