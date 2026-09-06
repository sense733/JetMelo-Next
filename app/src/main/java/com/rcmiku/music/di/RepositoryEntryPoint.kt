package com.rcmiku.music.di

import com.rcmiku.music.data.repository.AlbumRepository
import com.rcmiku.music.data.repository.PlaylistRepository
import com.rcmiku.music.data.repository.UserPlaylistRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    fun playlistRepository(): PlaylistRepository
    fun albumRepository(): AlbumRepository
    fun userPlaylistRepository(): UserPlaylistRepository
}
