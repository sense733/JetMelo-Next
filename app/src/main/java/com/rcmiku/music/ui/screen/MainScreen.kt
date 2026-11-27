package com.rcmiku.music.ui.screen

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.constants.EmphasizedAccelerateEasing
import com.rcmiku.music.constants.EmphasizedDecelerateEasing
import com.rcmiku.music.constants.MiniPlayerHeight
import com.rcmiku.music.constants.accountVipTierKey
import com.rcmiku.music.constants.currentPlayMediaIdKey
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.constants.userIdKey
import com.rcmiku.music.ui.components.tabs
import com.rcmiku.music.ui.design.BottomFogOverlay
import com.rcmiku.music.ui.design.LocalArtworkColors
import com.rcmiku.music.ui.design.rememberArtworkColors
import com.rcmiku.music.ui.navigation.NavGraph
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.music.utils.setPreference
import com.rcmiku.ncmapi.model.AccountVipTier
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.utils.CookieProvider
import com.rcmiku.ncmapi.utils.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val BASE_ENTER_DURATION = 520
private const val BASE_EXIT_DURATION = 420
private const val DOCKED_PADDING_ANIM_DURATION = 280
private val StandardDecelerateEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
internal const val QUANTIZED_BLUR_LEVELS = 8

internal fun quantizedBlurLevelIndex(progress: Float): Int {
    if (progress <= 0.05f || progress >= 1f) return -1
    val normalized = ((progress - 0.05f) / 0.95f).coerceIn(0f, 1f)
    return (normalized * (QUANTIZED_BLUR_LEVELS - 1)).roundToInt().coerceIn(0, QUANTIZED_BLUR_LEVELS - 1)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showNavigationBar =
        currentDestination?.hierarchy?.any { tabs.any { tab -> it.route == tab.route } } == true

    val ncmCookie by rememberPreference(ncmCookieKey, "")
    var userId by rememberPreference(userIdKey, 0L)
    val playerState = LocalPlayerState.current
    val showMiniPlayer = (playerState?.timeline?.windowCount ?: 0) != 0
    val currentMediaItem = playerState?.currentMediaItem
    val currentMediaId = currentMediaItem?.mediaId
    var currentPlayMediaId by rememberPreference(currentPlayMediaIdKey, 0)
    val isPlaying = playerState?.isPlaying == true

    val artworkUri = currentMediaItem?.mediaMetadata?.artworkUri ?: playerState?.mediaMetadata?.artworkUri
    val fallbackDominant = MaterialTheme.colorScheme.surfaceContainer
    val fallbackAccent = MaterialTheme.colorScheme.primary

    val artworkColors = rememberArtworkColors(
        artworkUri = artworkUri,
        songId = currentMediaId,
        fallbackDominant = fallbackDominant,
        fallbackAccent = fallbackAccent
    )

    LaunchedEffect(currentMediaId) {
        currentMediaId?.toLongOrNull()?.let {
            currentPlayMediaId = it
        }
    }

    val context = LocalContext.current

    LaunchedEffect(currentMediaId, artworkUri, playerState?.timeline) {
        if (artworkUri != null) {
            com.rcmiku.music.ui.design.PaletteExtractor.getOrCreateBlurredWash(
                context = context,
                artworkUri = artworkUri,
                songId = currentMediaId
            )
        }
        val timeline = playerState?.timeline ?: return@LaunchedEffect
        val currentIndex = playerState.mediaItemIndex
        if (timeline.isEmpty || currentIndex < 0) return@LaunchedEffect

        val window = androidx.media3.common.Timeline.Window()
        val adjacentIndices = listOf(currentIndex + 1, currentIndex - 1)
        for (index in adjacentIndices) {
            if (index in 0 until timeline.windowCount) {
                timeline.getWindow(index, window)
                val item = window.mediaItem
                val itemArtwork = item.mediaMetadata.artworkUri
                val itemId = item.mediaId
                if (itemArtwork != null) {
                    com.rcmiku.music.ui.design.PaletteExtractor.preload(
                        context = context,
                        artworkUri = itemArtwork,
                        songId = itemId,
                        fallbackDominant = fallbackDominant,
                        fallbackAccent = fallbackAccent
                    )
                    com.rcmiku.music.ui.design.PaletteExtractor.getOrCreateBlurredWash(
                        context = context,
                        artworkUri = itemArtwork,
                        songId = itemId
                    )
                }
            }
        }
    }

    var showPlayer by rememberSaveable { mutableStateOf(false) }

    val transitionProgressState = animateFloatAsState(
        targetValue = if (showPlayer) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (showPlayer) BASE_ENTER_DURATION else BASE_EXIT_DURATION,
            easing = StandardDecelerateEasing
        ),
        label = "player_transition_progress"
    )

    LaunchedEffect(ncmCookie) {
        withContext(Dispatchers.IO) {
            try {
                if (ncmCookie.isNotEmpty()) {
                    CookieProvider.init(json.decodeFromString(ncmCookie))
                    val accountInfo = AccountApi.account().getOrNull()
                    accountInfo?.profile?.userId?.let {
                        userId = it
                        AccountApi.getLikelist(it).getOrNull()?.ids?.let { songIds ->
                            FavoriteSongIdsUtil.mergeSongIds(context, songIds)
                        }
                    }
                    val tier = accountInfo?.account?.effectiveVipTier ?: AccountVipTier.NONE
                    context.setPreference(accountVipTierKey, tier.name)
                } else {
                    userId = 0L
                    context.setPreference(accountVipTierKey, AccountVipTier.NONE.name)
                }
            } catch (e: Exception) {
                Log.e("MainScreen", "Failed to restore ncmCookie", e)
            }
        }
    }

    val navBarInset =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBarBaseHeight = 64.dp

    val targetDockedBottomPadding by remember(showNavigationBar, showMiniPlayer, navBarInset) {
        derivedStateOf {
            if (showNavigationBar) {
                navBarBaseHeight + navBarInset
            } else if (showMiniPlayer) {
                navBarInset
            } else {
                0.dp
            }
        }
    }

    val dockedBottomPaddingState = animateDpAsState(
        targetValue = targetDockedBottomPadding,
        animationSpec = tween(
            durationMillis = DOCKED_PADDING_ANIM_DURATION,
            easing = StandardDecelerateEasing
        ),
        label = "docked_bottom_padding"
    )

    val isScrimVisible by remember {
        derivedStateOf { transitionProgressState.value > 0f }
    }

    val showPlayerOverlay by remember(showMiniPlayer) {
        derivedStateOf { showMiniPlayer || transitionProgressState.value > 0f }
    }

    val tabBottomContentPadding = remember(navBarInset, showMiniPlayer) {
        navBarBaseHeight + navBarInset + (if (showMiniPlayer) MiniPlayerHeight + 8.dp else 0.dp)
    }

    val subpageBottomContentPadding = remember(navBarInset, showMiniPlayer) {
        navBarInset + (if (showMiniPlayer) MiniPlayerHeight + 8.dp else 0.dp)
    }

    val density = LocalDensity.current
    val quantizedBlurEffects = remember(density) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Array(QUANTIZED_BLUR_LEVELS) { index ->
                val radiusPx = with(density) { ((index + 1) * 1.5f).dp.toPx() }
                BlurEffect(
                    radiusX = radiusPx,
                    radiusY = radiusPx,
                    edgeTreatment = TileMode.Decal
                )
            }
        } else {
            null
        }
    }

    val transitionProgressProvider = remember { { transitionProgressState.value } }
    val dockedBottomPaddingProvider = remember { { dockedBottomPaddingState.value } }
    val fogBottomPaddingProvider = remember(showNavigationBar, showMiniPlayer) {
        {
            if (showNavigationBar && showMiniPlayer) {
                dockedBottomPaddingState.value + (MiniPlayerHeight / 2)
            } else {
                dockedBottomPaddingState.value
            }
        }
    }

    CompositionLocalProvider(LocalArtworkColors provides artworkColors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                content = { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .consumeWindowInsets(padding)
                            .graphicsLayer {
                                val p = transitionProgressState.value
                                val scale = 1f - 0.05f * p
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                                if (quantizedBlurEffects != null) {
                                    val blurIndex = quantizedBlurLevelIndex(p)
                                    renderEffect = if (blurIndex >= 0) quantizedBlurEffects[blurIndex] else null
                                }
                            }
                    ) {
                        NavGraph(
                            navController = navController,
                            tabBottomContentPadding = tabBottomContentPadding,
                            subpageBottomContentPadding = subpageBottomContentPadding
                        )

                        AnimatedVisibility(
                            visible = showNavigationBar && showMiniPlayer,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            enter = fadeIn(
                                animationSpec = tween(400, easing = EmphasizedDecelerateEasing)
                            ),
                            exit = fadeOut(
                                animationSpec = tween(300, easing = EmphasizedAccelerateEasing)
                            )
                        ) {
                            BottomFogOverlay(
                                bottomPaddingProvider = fogBottomPaddingProvider,
                                modifier = Modifier.graphicsLayer {
                                    alpha = (1f - transitionProgressState.value).coerceIn(0f, 1f)
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = showNavigationBar,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(DOCKED_PADDING_ANIM_DURATION, easing = StandardDecelerateEasing)
                            ) + fadeIn(
                                animationSpec = tween(DOCKED_PADDING_ANIM_DURATION, easing = StandardDecelerateEasing)
                            ),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(DOCKED_PADDING_ANIM_DURATION, easing = StandardDecelerateEasing)
                            ) + fadeOut(
                                animationSpec = tween(DOCKED_PADDING_ANIM_DURATION, easing = StandardDecelerateEasing)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                if (showMiniPlayer) {
                                    Spacer(modifier = Modifier.height(MiniPlayerHeight / 2))
                                }
                                NavigationBar(
                                    modifier = Modifier.height(navBarBaseHeight + navBarInset),
                                    containerColor = MaterialTheme.colorScheme.background
                                ) {
                                    tabs.forEach { item ->
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = stringResource(id = item.titleRes)
                                                )
                                            },
                                            label = { Text(stringResource(id = item.titleRes)) },
                                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                            onClick = {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )

            if (isScrimVisible) {
                val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val scrimColor = MaterialTheme.colorScheme.scrim
                val maxScrimAlpha = if (isDarkTheme) 0.35f else 0.12f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val alpha = (transitionProgressState.value * maxScrimAlpha).coerceIn(0f, 1f)
                            if (alpha > 0f) {
                                drawRect(color = scrimColor.copy(alpha = alpha))
                            }
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                )
            }

            val miniPlayerSlideOffsetPx = remember(density, navBarInset) {
                with(density) { (navBarBaseHeight + navBarInset + MiniPlayerHeight + 32.dp).roundToPx() }
            }

            AnimatedVisibility(
                visible = showPlayerOverlay,
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxSize(),
                enter = slideInVertically(
                    initialOffsetY = { miniPlayerSlideOffsetPx },
                    animationSpec = tween(400, easing = EmphasizedDecelerateEasing)
                ) + fadeIn(
                    animationSpec = tween(400, easing = EmphasizedDecelerateEasing)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { miniPlayerSlideOffsetPx },
                    animationSpec = tween(300, easing = EmphasizedAccelerateEasing)
                ) + fadeOut(
                    animationSpec = tween(300, easing = EmphasizedAccelerateEasing)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    playerState?.mediaMetadata?.let {
                        PlayerTransform(
                            mediaMetadata = it,
                            onBackPressed = { showPlayer = false },
                            onClick = { showPlayer = true },
                            navController = navController,
                            isExpanded = showPlayer,
                            transitionProgressProvider = transitionProgressProvider,
                            dockedBottomPaddingProvider = dockedBottomPaddingProvider
                        )
                    }
                }
            }
        }
    }
}