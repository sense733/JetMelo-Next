package com.rcmiku.music.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.artist.ArtistApi
import com.rcmiku.ncmapi.model.Album
import kotlinx.coroutines.CancellationException

class ArtistAlbumPagingSource(private val id: Long) : PagingSource<Int, Album>() {
    companion object {
        private const val PAGE_SIZE = 30
    }

    override fun getRefreshKey(state: PagingState<Int, Album>): Int? {
        return state.anchorPosition?.let { pos ->
            state.closestPageToPosition(pos)?.let { page ->
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Album> {
        return try {
            val page = params.key ?: 0
            val offset = page * PAGE_SIZE
            val response = ArtistApi.artistAlbum(id = id, limit = PAGE_SIZE, offset = offset)
            if (response.isSuccess) {
                val artistAlbum = response.getOrThrow()
                val data = artistAlbum.hotAlbums
                val nextKey = if (artistAlbum.more && data.isNotEmpty()) page + 1 else null
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