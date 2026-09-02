package com.rcmiku.music

import android.app.Application
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.playback.PlayerController
import com.rcmiku.music.utils.SongListUtil
import com.rcmiku.music.utils.UserAgentUtil
import com.rcmiku.music.utils.dataStore
import com.rcmiku.ncmapi.utils.CookieProvider
import com.rcmiku.ncmapi.utils.FileProvider
import com.rcmiku.ncmapi.utils.UserAgentProvider
import com.rcmiku.ncmapi.utils.json
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltAndroidApp
class JetMeloApp : Application(), SingletonImageLoader.Factory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        PlayerController.init(this)
        FileProvider.init(cacheDir.resolve("ncm"))
        SongListUtil.init(filesDir.resolve("playlist"))
        UserAgentProvider.init(UserAgentUtil.DEFAULT_USER_AGENT)
        applicationScope.launch {
            var hadCookie = false
            dataStore.data
                .map { it[ncmCookieKey] }
                .distinctUntilChanged()
                .collect { ncmCookie ->
                    if (!ncmCookie.isNullOrEmpty()) {
                        runCatching {
                            CookieProvider.init(json.decodeFromString(ncmCookie))
                            hadCookie = true
                        }
                    } else if (hadCookie) {
                        CookieProvider.clear()
                        hadCookie = false
                    }
                }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .maxSizeBytes(256 * 1024 * 1024L)
                    .directory(cacheDir.resolve("coil"))
                    .build()
            }
            .build()
    }
}
