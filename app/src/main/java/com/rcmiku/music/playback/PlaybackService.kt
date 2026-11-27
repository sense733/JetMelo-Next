package com.rcmiku.music.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.emptyPreferences
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.session.CommandButton
import androidx.media3.session.CommandButton.ICON_UNDEFINED
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rcmiku.music.MainActivity
import com.rcmiku.music.R
import com.rcmiku.music.cache.LyricDiskCache
import com.rcmiku.music.cache.SongCacheLimit
import com.rcmiku.music.cache.SongCacheManager
import com.rcmiku.music.constants.MediaSessionConstants
import com.rcmiku.music.constants.allowMixWithOtherAppsKey
import com.rcmiku.music.constants.audioQualityKey
import com.rcmiku.music.constants.songCacheLimitKey
import com.rcmiku.music.constants.use40DpIconKey
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.extensions.updateMediaItemUri
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.music.utils.ImagePreloadUtil
import com.rcmiku.music.utils.dataStore
import com.rcmiku.music.utils.enumPreference
import com.rcmiku.music.utils.preference
import com.rcmiku.music.utils.toEnum
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.player.PlayerApi
import com.rcmiku.ncmapi.api.player.SongLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import android.widget.Toast
import androidx.media3.common.Player.STATE_ENDED

@UnstableApi
class PlaybackService : MediaSessionService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaSession: MediaSession? = null
    private var lastLayoutKey: Triple<Boolean, Boolean, Boolean>? = null
    private var favoriteSongIds: List<Long> by mutableStateOf(emptyList())
    private var use40DpIcon by mutableStateOf(false)
    private var audioQuality by mutableStateOf(SongLevel.STANDARD)
    private var audioManager: AudioManager? = null
    private var allowMixWithOtherApps = false
    private var wasRecordingActive = false
    private var isRecordingCallbackRegistered = false

    private val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            handleRecordingConfigsChanged(configs)
        }
    }

    private val favoriteButton: CommandButton
        get() = CommandButton.Builder(ICON_UNDEFINED)
            .setCustomIconResId(if (use40DpIcon) R.drawable.ic_favorite_40_dp else R.drawable.ic_favorite)
            .setDisplayName("like")
            .setSessionCommand(MediaSessionConstants.CommandToggleLike)
            .build()

    private val favoriteButtonOn: CommandButton
        get() = CommandButton.Builder(ICON_UNDEFINED)
            .setCustomIconResId(if (use40DpIcon) R.drawable.ic_favorite_fill_40_dp else R.drawable.ic_favorite_fill)
            .setDisplayName("like_on")
            .setSessionCommand(MediaSessionConstants.CommandToggleLike)
            .build()

    private val shuffleButton: CommandButton
        get() = CommandButton.Builder(ICON_UNDEFINED)
            .setCustomIconResId(if (use40DpIcon) R.drawable.ic_shuffle_40_dp else R.drawable.ic_shuffle)
            .setDisplayName("shuffle")
            .setSessionCommand(MediaSessionConstants.CommandToggleShuffle)
            .build()

    private val shuffleButtonOn: CommandButton
        get() = CommandButton.Builder(ICON_UNDEFINED)
            .setCustomIconResId(if (use40DpIcon) R.drawable.ic_shuffle_on_40_dp else R.drawable.ic_shuffle_on)
            .setDisplayName("shuffle_on")
            .setSessionCommand(MediaSessionConstants.CommandToggleShuffle)
            .build()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        scope.launch(Dispatchers.IO) {
            applicationContext.dataStore.data
                .map { it[audioQualityKey].toEnum(SongLevel.STANDARD) }
                .distinctUntilChanged()
                .collect { audioQuality = it }
        }
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { 2000 },
                DefaultMediaNotificationProvider.DEFAULT_CHANNEL_ID,
                DefaultMediaNotificationProvider.DEFAULT_CHANNEL_NAME_RESOURCE_ID
            ).apply {
                setSmallIcon(R.drawable.ic_music_note)
            }
        )
        val audioOnlyRenderersFactory =
            RenderersFactory {
                    handler: Handler,
                    _: VideoRendererEventListener,
                    audioListener: AudioRendererEventListener,
                    _: TextOutput,
                    _: MetadataOutput,
                ->
                arrayOf<Renderer>(
                    MediaCodecAudioRenderer(
                        this,
                        MediaCodecSelector.DEFAULT,
                        handler,
                        audioListener
                    )
                )
            }

        audioManager = getSystemService(AudioManager::class.java)
        val (initialAllowMix, initialCacheLimit) = runBlocking(Dispatchers.IO) {
            runCatching {
                withTimeout(1_000L) {
                    val prefs = applicationContext.dataStore.data
                        .catch { emit(emptyPreferences()) }
                        .first()
                    val allowMix = prefs[allowMixWithOtherAppsKey] ?: false
                    val cacheLimit = prefs[songCacheLimitKey].toEnum(SongCacheLimit.GB_1)
                    allowMix to cacheLimit
                }
            }.getOrDefault(false to SongCacheLimit.GB_1)
        }

        LyricDiskCache.init(applicationContext)
        val songCache = SongCacheManager.getCache(applicationContext, initialCacheLimit)

        val player = ExoPlayer.Builder(this, audioOnlyRenderersFactory)
            .apply {
                setAudioAttributes(
                    PlaybackFocusPolicy.musicAudioAttributes,
                    PlaybackFocusPolicy.shouldHandleAudioFocus(initialAllowMix)
                )
                val resolvingDataSourceFactory = ResolvingDataSource.Factory(
                    DefaultHttpDataSource.Factory()
                ) { dataSpec ->
                    runBlocking {
                        val songId = dataSpec.uri.path.orEmpty().substringAfterLast("/").substringBefore("_")
                        val resolvedUri = try {
                            withTimeout(15_000L) {
                                updateMediaItemUri(songId, audioQuality)
                            }
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            throw PlaybackException(
                                "Timeout resolving song uri for $songId",
                                e,
                                PlaybackException.ERROR_CODE_REMOTE_ERROR
                            )
                        } catch (e: Exception) {
                            throw PlaybackException(
                                "Failed to resolve song uri for $songId",
                                e,
                                PlaybackException.ERROR_CODE_REMOTE_ERROR
                            )
                        }
                        if (resolvedUri == null) {
                            throw PlaybackException(
                                "Resolved uri is null for $songId",
                                null,
                                PlaybackException.ERROR_CODE_REMOTE_ERROR
                            )
                        }
                        dataSpec.withUri(resolvedUri)
                    }
                }
                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(songCache)
                    .setUpstreamDataSourceFactory(resolvingDataSourceFactory)
                    .setCacheKeyFactory { dataSpec ->
                        dataSpec.key ?: SongCacheManager.buildCacheKey(dataSpec.uri.toString(), audioQuality)
                    }
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            }.build()

        player.repeatMode = REPEAT_MODE_ALL
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                prefetchOfflineAssets(mediaItem)
            }
        })
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            ).setCallback(MediaSessionCallback())
            .setCustomLayout(ImmutableList.of(favoriteButton, shuffleButton)).build()
        observeIconPreference()
        observeFavoriteSongIds()
        applyMixPlaybackPolicy(initialAllowMix)
        observeAllowMixPreference()
        observeSongCacheLimitPreference()
    }

    override fun onDestroy() {
        unregisterRecordingCallback()
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == STATE_ENDED) {
            pauseAllPlayersAndStopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    fun updateCustomLayout() {
        val currentMediaId = mediaSession?.player?.currentMediaItem?.mediaId?.toLongOrNull()
        val isFavorite = currentMediaId != null && favoriteSongIds.contains(currentMediaId)
        val isShuffle = mediaSession?.player?.shuffleModeEnabled != false
        val key = Triple(isFavorite, isShuffle, use40DpIcon)
        if (lastLayoutKey == key) return
        lastLayoutKey = key
        mediaSession?.setCustomLayout(
            ImmutableList.of(
                if (isFavorite) favoriteButtonOn else favoriteButton,
                if (isShuffle) shuffleButtonOn else shuffleButton
            )
        )
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        updateCustomLayout()
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    private fun toggleLike(like: Boolean, songId: Long) {
        scope.launch {
            if (like) {
                FavoriteSongIdsUtil.addSongId(applicationContext, songId)
            } else {
                FavoriteSongIdsUtil.removeSongId(applicationContext, songId)
            }
            updateCustomLayout()
            withContext(Dispatchers.IO) {
                AccountApi.songLike(like, songId)
            }.onSuccess {
                updateCustomLayout()
            }.onFailure {
                withContext(Dispatchers.Main) {
                    if (like) {
                        FavoriteSongIdsUtil.removeSongId(applicationContext, songId)
                    } else {
                        FavoriteSongIdsUtil.addSongId(applicationContext, songId)
                    }
                    updateCustomLayout()
                    Toast.makeText(
                        applicationContext,
                        applicationContext.getString(R.string.operation_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun observeIconPreference() {
        scope.launch {
            applicationContext.dataStore.data
                .map { it[use40DpIconKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    use40DpIcon = it
                    updateCustomLayout()
                }
        }
    }

    private fun observeFavoriteSongIds() {
        scope.launch {
            applicationContext.favoriteSongIdsDatastore.data
                .distinctUntilChanged()
                .collect {
                    favoriteSongIds = it.songIdsList
                    updateCustomLayout()
                }
        }
    }

    private fun observeAllowMixPreference() {
        scope.launch(Dispatchers.IO) {
            applicationContext.dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[allowMixWithOtherAppsKey] ?: false }
                .distinctUntilChanged()
                .collect { allowMix ->
                    withContext(Dispatchers.Main) {
                        applyMixPlaybackPolicy(allowMix)
                    }
                }
        }
    }

    private fun observeSongCacheLimitPreference() {
        scope.launch(Dispatchers.IO) {
            applicationContext.dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[songCacheLimitKey].toEnum(SongCacheLimit.GB_1) }
                .distinctUntilChanged()
                .collect { limit ->
                    SongCacheManager.updateLimit(applicationContext, limit)
                }
        }
    }

    private fun prefetchOfflineAssets(mediaItem: MediaItem?) {
        mediaItem ?: return
        ImagePreloadUtil.preloadMediaItems(applicationContext, listOf(mediaItem), limit = 1)
        val songId = mediaItem.mediaId.substringBefore("_").toLongOrNull() ?: return
        scope.launch(Dispatchers.IO) {
            if (LyricDiskCache.get(songId) != null) return@launch
            PlayerApi.songLyric(songId).getOrNull()?.let { response ->
                LyricDiskCache.put(songId, response)
            }
        }
    }

    private fun applyMixPlaybackPolicy(allowMix: Boolean) {
        allowMixWithOtherApps = allowMix
        mediaSession?.player?.setAudioAttributes(
            PlaybackFocusPolicy.musicAudioAttributes,
            PlaybackFocusPolicy.shouldHandleAudioFocus(allowMix)
        )
        if (allowMix) {
            unregisterRecordingCallback()
        } else {
            registerRecordingCallback()
        }
    }

    private fun registerRecordingCallback() {
        val manager = audioManager ?: return
        if (!isRecordingCallbackRegistered) {
            manager.registerAudioRecordingCallback(recordingCallback, Handler(Looper.getMainLooper()))
            isRecordingCallbackRegistered = true
        }
        handleRecordingConfigsChanged(manager.activeRecordingConfigurations)
    }

    private fun unregisterRecordingCallback() {
        if (isRecordingCallbackRegistered) {
            audioManager?.unregisterAudioRecordingCallback(recordingCallback)
            isRecordingCallbackRegistered = false
        }
        wasRecordingActive = false
    }

    private fun handleRecordingConfigsChanged(configs: List<AudioRecordingConfiguration>) {
        val isRecordingActive = PlaybackFocusPolicy.hasActiveExternalRecording(configs)
        val player = mediaSession?.player
        if (player != null && PlaybackFocusPolicy.shouldPauseOnRecordingChange(
                allowMixWithOtherApps = allowMixWithOtherApps,
                playWhenReady = player.playWhenReady,
                playbackSuppressionReason = player.playbackSuppressionReason,
                wasRecordingActive = wasRecordingActive,
                isRecordingActive = isRecordingActive
            )
        ) {
            player.pause()
        }
        wasRecordingActive = isRecordingActive
    }

    private inner class MediaSessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = if (controller.packageName == packageName) {
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(MediaSessionConstants.CommandToggleLike)
                    .add(MediaSessionConstants.CommandToggleShuffle)
                    .build()
            } else {
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
            }
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == MediaSessionConstants.ACTION_TOGGLE_SHUFFLE) {
                session.player.shuffleModeEnabled = !session.player.shuffleModeEnabled
                updateCustomLayout()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == MediaSessionConstants.ACTION_TOGGLE_LIKE) {
                session.player.currentMediaItem?.mediaId?.toLongOrNull()?.let {
                    toggleLike(it !in favoriteSongIds, it)
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }
}
