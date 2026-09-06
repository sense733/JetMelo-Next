package com.rcmiku.music.ui.screen

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.constants.MiniPlayerHeight
import com.rcmiku.music.constants.currentPlayMediaIdKey
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.constants.userIdKey
import com.rcmiku.music.ui.components.tabs
import com.rcmiku.music.ui.design.BottomFogOverlay
import com.rcmiku.music.ui.design.LocalArtworkColors
import com.rcmiku.music.ui.design.rememberArtworkColors
import com.rcmiku.music.ui.navigation.NavGraph
import com.rcmiku.music.ui.navigation.Screen
import com.rcmiku.music.ui.theme.rememberDeviceCornerRadius
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.music.utils.rememberPreference
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.utils.CookieProvider
import com.rcmiku.ncmapi.utils.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val BASE_ENTER_DURATION = 520
private const val BASE_EXIT_DURATION = 420
private const val DOCKED_PADDING_ANIM_DURATION = 280
private val StandardDecelerateEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showNavigationBar =
        currentDestination?.hierarchy?.any { tabs.any { tab -> it.route == tab.route } } == true
    val isSearchScreen =
        currentDestination?.hierarchy?.any { it.route == Screen.Search.route } == true
    val ncmCookie by rememberPreference(ncmCookieKey, "")
    var userId by rememberPreference(userIdKey, 0L)
    val playerState = LocalPlayerState.current
    val showMiniPlayer =
        (playerState?.timeline?.windowCount ?: 0) != 0 && !isSearchScreen
    val currentMediaId = playerState?.currentMediaItem?.mediaId
    var currentPlayMediaId by rememberPreference(currentPlayMediaIdKey, 0)
    val isPlaying = playerState?.isPlaying == true

    val artworkUri = playerState?.mediaMetadata?.artworkUri
    val artworkColors = rememberArtworkColors(
        artworkUri = artworkUri,
        songId = currentMediaId
    )

    LaunchedEffect(currentMediaId) {
        currentMediaId?.toLongOrNull()?.let {
            currentPlayMediaId = it
        }
    }

    var showPlayer by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    var animatorScale by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(context) {
        try {
            animatorScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        } catch (_: Exception) {
            animatorScale = 1f
        }
    }

    val enterDuration = (BASE_ENTER_DURATION * animatorScale).roundToInt().coerceAtLeast(0)
    val exitDuration = (BASE_EXIT_DURATION * animatorScale).roundToInt().coerceAtLeast(0)

    val transitionProgress by animateFloatAsState(
        targetValue = if (showPlayer) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (showPlayer) enterDuration else exitDuration,
            easing = StandardDecelerateEasing
        ),
        label = "player_transition_progress"
    )

    LaunchedEffect(ncmCookie) {
        withContext(Dispatchers.IO) {
            try {
                if (ncmCookie.isNotEmpty()) {
                    CookieProvider.init(json.decodeFromString(ncmCookie))
                    AccountApi.account().getOrNull()?.profile?.userId?.let {
                        userId = it
                        AccountApi.getLikelist(it).getOrNull()?.ids?.let { songIds ->
                            if (songIds.isNotEmpty()) {
                                FavoriteSongIdsUtil.mergeSongIds(context, songIds)
                            }
                        }
                    }
                } else {
                    userId = 0L
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

    val dockedBottomPadding by animateDpAsState(
        targetValue = targetDockedBottomPadding,
        animationSpec = tween(
            durationMillis = DOCKED_PADDING_ANIM_DURATION,
            easing = StandardDecelerateEasing
        ),
        label = "docked_bottom_padding"
    )

    val fogBottomPadding by remember(showNavigationBar, showMiniPlayer, dockedBottomPadding) {
        derivedStateOf {
            if (showNavigationBar && showMiniPlayer) {
                dockedBottomPadding + (MiniPlayerHeight / 2)
            } else {
                dockedBottomPadding
            }
        }
    }

    val bottomContentPadding by remember(isSearchScreen, navBarInset, dockedBottomPadding, showMiniPlayer) {
        derivedStateOf {
            if (isSearchScreen) {
                navBarInset
            } else {
                dockedBottomPadding + if (showMiniPlayer) MiniPlayerHeight + 8.dp else 0.dp
            }
        }
    }

    CompositionLocalProvider(LocalArtworkColors provides artworkColors) {
        Box(modifier = Modifier.fillMaxSize()) {
            val p = transitionProgress
            val deviceCornerRadius = rememberDeviceCornerRadius()

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1f - 0.05f * p
                        scaleY = 1f - 0.05f * p
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                        if (p > 0f) {
                            shape = RoundedCornerShape(deviceCornerRadius * p)
                            clip = true
                        }
                    }
                    .then(
                        if (p > 0.05f && Build.VERSION.SDK_INT >= 31) {
                            Modifier.blur((p * 12).dp)
                        } else {
                            Modifier
                        }
                    ),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            visible = showNavigationBar,
                            enter = expandVertically(
                                animationSpec = tween(DOCKED_PADDING_ANIM_DURATION, easing = StandardDecelerateEasing)
                            ),
                            exit = shrinkVertically(
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
                                    modifier = Modifier.height(64.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
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
                },
                content = { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .consumeWindowInsets(padding)
                    ) {
                        NavGraph(
                            navController = navController,
                            bottomContentPadding = bottomContentPadding
                        )

                        if (showMiniPlayer) {
                            BottomFogOverlay(
                                bottomPadding = fogBottomPadding,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .graphicsLayer {
                                        alpha = (1f - p).coerceIn(0f, 1f)
                                    }
                            )
                        }
                    }
                }
            )

            if (p > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = p * 0.35f))
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

            if (showMiniPlayer || transitionProgress > 0f) {
                Box(
                    modifier = Modifier
                        .zIndex(1f)
                        .fillMaxSize()
                ) {
                    playerState?.mediaMetadata?.let {
                        PlayerTransform(
                            mediaMetadata = it,
                            onBackPressed = { showPlayer = false },
                            onClick = { showPlayer = true },
                            navController = navController,
                            isExpanded = showPlayer,
                            transitionProgress = transitionProgress,
                            dockedBottomPadding = dockedBottomPadding
                        )
                    }
                }
            }
        }
    }
}