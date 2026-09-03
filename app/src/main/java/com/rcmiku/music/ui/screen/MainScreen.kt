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
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.utils.CookieProvider
import com.rcmiku.ncmapi.utils.json

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
    val animatorScale = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        } catch (_: Exception) {
            1f
        }
    }

    val enterDuration = (520 * animatorScale).roundToInt().coerceAtLeast(0)
    val exitDuration = (420 * animatorScale).roundToInt().coerceAtLeast(0)

    val transitionProgress by animateFloatAsState(
        targetValue = if (showPlayer) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (showPlayer) enterDuration else exitDuration,
            easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        ),
        label = "player_transition_progress"
    )

    LaunchedEffect(ncmCookie) {
        if (ncmCookie.isNotEmpty()) {
            CookieProvider.init(json.decodeFromString(ncmCookie))
            AccountApi.account().getOrNull()?.profile?.userId?.let {
                userId = it
            }
        } else {
            userId = 0L
        }
    }

    val navBarInset =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBarBaseHeight = 64.dp

    val targetDockedBottomPadding = if (showNavigationBar) {
        navBarBaseHeight + navBarInset
    } else if (showMiniPlayer) {
        navBarInset
    } else {
        0.dp
    }

    val dockedBottomPadding by animateDpAsState(
        targetValue = targetDockedBottomPadding,
        animationSpec = tween(
            durationMillis = 280,
            easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        ),
        label = "docked_bottom_padding"
    )

    val fogBottomPadding = if (showNavigationBar && showMiniPlayer) {
        dockedBottomPadding + (MiniPlayerHeight / 2)
    } else {
        dockedBottomPadding
    }

    val bottomContentPadding = if (isSearchScreen) {
        navBarInset
    } else {
        dockedBottomPadding + if (showMiniPlayer) MiniPlayerHeight + 8.dp else 0.dp
    }

    CompositionLocalProvider(LocalArtworkColors provides artworkColors) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            visible = showNavigationBar,
                            enter = expandVertically(
                                animationSpec = tween(280, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f))
                            ),
                            exit = shrinkVertically(
                                animationSpec = tween(280, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f))
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
                    val p = transitionProgress
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .consumeWindowInsets(padding)
                            .graphicsLayer {
                                scaleX = 1f - 0.05f * p
                                scaleY = 1f - 0.05f * p
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                            .then(
                                if (p > 0f && Build.VERSION.SDK_INT >= 31) {
                                    Modifier.blur((p * 24).dp)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        NavGraph(
                            navController = navController,
                            bottomContentPadding = bottomContentPadding
                        )

                        if (showMiniPlayer && transitionProgress < 0.20f) {
                            BottomFogOverlay(
                                bottomPadding = fogBottomPadding,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .graphicsLayer {
                                        alpha = (1f - transitionProgress / 0.15f).coerceIn(0f, 1f)
                                    }
                            )
                        }
                    }
                }
            )

            val p = transitionProgress
            if (p > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = p * 0.35f))
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