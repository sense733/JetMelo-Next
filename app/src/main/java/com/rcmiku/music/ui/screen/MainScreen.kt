package com.rcmiku.music.ui.screen

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import com.rcmiku.music.ui.design.LocalArtworkColors
import com.rcmiku.music.ui.design.rememberArtworkColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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

    var showPlayer by remember { mutableStateOf(false) }

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

    val playerBottomPadding = if (!showPlayer) {
        if (showNavigationBar) {
            navBarBaseHeight + navBarInset
        } else if (showMiniPlayer) {
            navBarInset
        } else {
            0.dp
        }
    } else {
        0.dp
    }

    val fogBottomPadding = if (showNavigationBar && showMiniPlayer) {
        playerBottomPadding + (MiniPlayerHeight / 2)
    } else {
        playerBottomPadding
    }

    val bottomContentPadding = if (isSearchScreen) {
        navBarInset
    } else {
        playerBottomPadding + if (showMiniPlayer) MiniPlayerHeight + 8.dp else 0.dp
    }

    CompositionLocalProvider(LocalArtworkColors provides artworkColors) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            showNavigationBar && !showPlayer,
                            enter = expandVertically(),
                            exit = shrinkVertically()
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
                        modifier = Modifier.fillMaxSize()
                    ) {
                        NavGraph(
                            navController = navController,
                            bottomContentPadding = bottomContentPadding
                        )

                        AnimatedVisibility(
                            visible = showMiniPlayer && !showPlayer,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            BottomFogOverlay(
                                bottomPadding = fogBottomPadding
                            )
                        }
                    }
                }
            )

            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxSize()
            ) {
                AnimatedVisibility(
                    visible = showMiniPlayer || showPlayer,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = MiniPlayerHeight)
                            .windowInsetsPadding(WindowInsets(bottom = playerBottomPadding)),
                    ) {
                        playerState?.mediaMetadata?.let {
                            PlayerTransform(
                                mediaMetadata = it,
                                onBackPressed = { showPlayer = false },
                                onClick = { showPlayer = true },
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}