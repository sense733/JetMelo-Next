package com.rcmiku.music.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.model.SubAlbum
import kotlinx.coroutines.CancellationException

class AlbumPagingSource : PagingSource<Int, SubAlbum>() {
    companion object {
        private const val PAGE_SIZE = 30
    }

    override fun getRefreshKey(state: PagingState<Int, SubAlbum>): Int? {
        return state.anchorPosition?.let { pos ->
            state.closestPageToPosition(pos)?.let { page ->
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SubAlbum> {
        return try {
            val page = params.key ?: 0
            val offset = page * PAGE_SIZE
            val response = AccountApi.albumSublist(offset, PAGE_SIZE)
            if (response.isSuccess) {
                val albumSublist = response.getOrThrow()
                val data = albumSublist.data
                val nextKey = if (albumSublist.hasMore && data.isNotEmpty()) page + 1 else null
                val prevKey = if (page == 0) null else page - 1
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