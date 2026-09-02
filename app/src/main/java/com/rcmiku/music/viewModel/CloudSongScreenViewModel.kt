package com.rcmiku.music.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.rcmiku.music.paging.CloudPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CloudSongScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) :
    ViewModel() {

    // 2.32：/api/v1/cloud/get 仅凭登录 Cookie 鉴权（无按 uid 查询他人云盘的协议），
    // uid 不参与数据源查询，仅供 CloudSongScreen 构建播放队列的 "id_uid" URI 使用
    val uid = savedStateHandle.get<Long>("uid")

    val cloudSong = Pager(
        config = PagingConfig(
            pageSize = 500,
            prefetchDistance = 50,
            enablePlaceholders = false,
            initialLoadSize = 500
        ),
        pagingSourceFactory = { CloudPagingSource() }
    ).flow.cachedIn(viewModelScope)
}