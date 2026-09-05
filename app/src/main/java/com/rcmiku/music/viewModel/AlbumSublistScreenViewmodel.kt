package com.rcmiku.music.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.rcmiku.music.paging.AlbumPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AlbumSublistScreenViewModel @Inject constructor() : ViewModel() {

    val albumSublist = Pager(
        config = PagingConfig(
            pageSize = 30,
            prefetchDistance = 15,
            enablePlaceholders = false,
            initialLoadSize = 60
        ),
        pagingSourceFactory = { AlbumPagingSource() }
    ).flow.cachedIn(viewModelScope)
}