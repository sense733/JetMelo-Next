package com.rcmiku.music.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.rcmiku.music.constants.EmphasizedDecelerateEasing
import com.rcmiku.music.constants.NAV_TRANSITION_DURATION
import com.rcmiku.music.ui.components.tabs
import com.rcmiku.music.ui.screen.AlbumScreen
import com.rcmiku.music.ui.screen.AlbumSublistScreen
import com.rcmiku.music.ui.screen.ArtistScreen
import com.rcmiku.music.ui.screen.CloudSongScreen
import com.rcmiku.music.ui.screen.ExploreScreen
import com.rcmiku.music.ui.screen.HomeScreen
import com.rcmiku.music.ui.screen.LibraryScreen
import com.rcmiku.music.ui.screen.ListScreen
import com.rcmiku.music.ui.screen.LoginScreen
import com.rcmiku.music.ui.screen.PlaylistScreen
import com.rcmiku.music.ui.screen.ProgramRadioScreen
import com.rcmiku.music.ui.screen.RecordScreen
import com.rcmiku.music.ui.screen.SearchScreen
import com.rcmiku.music.ui.screen.SettingsScreen
import com.rcmiku.music.ui.screen.UserPlaylistScreen

private const val NAV_DURATION_FORWARD = NAV_TRANSITION_DURATION
private const val NAV_DURATION_POP = NAV_TRANSITION_DURATION
private const val NAV_PARALLAX_FACTOR = 0.25f
private const val SCALE_BG = 0.93f
private const val SCALE_EXIT_POP = 0.97f

private val tabRouteIndices: Map<String, Int> =
    tabs.mapIndexed { index, tab -> tab.route to index }.toMap()

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabSlideDirection(): Int? {
    val fromIndex = tabRouteIndices[initialState.destination.route] ?: return null
    val toIndex = tabRouteIndices[targetState.destination.route] ?: return null
    if (fromIndex == toIndex) return null
    return if (toIndex > fromIndex) 1 else -1
}

private fun tabEnterTransition(direction: Int): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { direction * it },
        animationSpec = tween(NAV_DURATION_FORWARD, easing = EmphasizedDecelerateEasing)
    ) + fadeIn(
        animationSpec = tween(NAV_DURATION_FORWARD, easing = EmphasizedDecelerateEasing),
        initialAlpha = 0.8f
    )

private fun tabExitTransition(direction: Int): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { -direction * it },
        animationSpec = tween(NAV_DURATION_FORWARD, easing = EmphasizedDecelerateEasing)
    ) + fadeOut(
        animationSpec = tween(NAV_DURATION_FORWARD, easing = EmphasizedDecelerateEasing),
        targetAlpha = 0.8f
    )

private fun defaultEnterTransition(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(NAV_DURATION_FORWARD, easing = EmphasizedDecelerateEasing)
    ) + fadeIn(
        animationSpec = tween(NAV_DURATION_FORWARD, easing = EmphasizedDecelerateEasing),
        initialAlpha = 0.4f
    )

private fun defaultExitTransition(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { (-it * NAV_PARALLAX_FACTOR).toInt() },
        animationSpec = tween(NAV_DURATION_FORWARD, easing = EmphasizedDecelerateEasing)
    ) + scaleOut(
        targetScale = SCALE_BG,
        transformOrigin = TransformOrigin.Center,
        animationSpec = tween(NAV_DURATION_FORWARD, easing = EmphasizedDecelerateEasing)
    ) + fadeOut(
        animationSpec = tween(NAV_DURATION_FORWARD, easing = EmphasizedDecelerateEasing),
        targetAlpha = 0.6f
    )

private fun defaultPopEnterTransition(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { (-it * NAV_PARALLAX_FACTOR).toInt() },
        animationSpec = tween(NAV_DURATION_POP, easing = EmphasizedDecelerateEasing)
    ) + scaleIn(
        initialScale = SCALE_BG,
        transformOrigin = TransformOrigin.Center,
        animationSpec = tween(NAV_DURATION_POP, easing = EmphasizedDecelerateEasing)
    ) + fadeIn(
        animationSpec = tween(NAV_DURATION_POP, easing = EmphasizedDecelerateEasing),
        initialAlpha = 0.6f
    )

private fun defaultPopExitTransition(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(NAV_DURATION_POP, easing = EmphasizedDecelerateEasing)
    ) + scaleOut(
        targetScale = SCALE_EXIT_POP,
        transformOrigin = TransformOrigin.Center,
        animationSpec = tween(NAV_DURATION_POP, easing = EmphasizedDecelerateEasing)
    ) + fadeOut(
        animationSpec = tween(NAV_DURATION_POP, easing = EmphasizedDecelerateEasing),
        targetAlpha = 0f
    )

private fun NavGraphBuilder.tabComposable(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            tabSlideDirection()?.let(::tabEnterTransition) ?: defaultEnterTransition()
        },
        exitTransition = {
            tabSlideDirection()?.let(::tabExitTransition) ?: defaultExitTransition()
        },
        popEnterTransition = {
            tabSlideDirection()?.let(::tabEnterTransition) ?: defaultPopEnterTransition()
        },
        popExitTransition = {
            tabSlideDirection()?.let(::tabExitTransition) ?: defaultPopExitTransition()
        },
        content = content
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    tabBottomContentPadding: Dp = 0.dp,
    subpageBottomContentPadding: Dp = 0.dp,
    bottomContentPadding: Dp = 0.dp
) {
    val tabPadding = if (tabBottomContentPadding != 0.dp) tabBottomContentPadding else bottomContentPadding
    val subpagePadding = if (subpageBottomContentPadding != 0.dp) subpageBottomContentPadding else bottomContentPadding

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { defaultEnterTransition() },
            exitTransition = { defaultExitTransition() },
            popEnterTransition = { defaultPopEnterTransition() },
            popExitTransition = { defaultPopExitTransition() }
        ) {
            tabComposable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this,
                    bottomContentPadding = tabPadding
                )
            }
            tabComposable(Screen.Explore.route) {
                ExploreScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this,
                    bottomContentPadding = tabPadding
                )
            }
            tabComposable(Screen.Library.route) {
                LibraryScreen(
                    navController = navController,
                    bottomContentPadding = tabPadding
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    navController = navController,
                    bottomContentPadding = subpagePadding
                )
            }
            composable(Screen.Login.route) { LoginScreen(navController = navController) }
            composable(Screen.Search.route) {
                SearchScreen(
                    navController = navController,
                    bottomContentPadding = subpagePadding
                )
            }
            composable<PlaylistNav> { backStackEntry ->
                val nav = backStackEntry.toRoute<PlaylistNav>()
                PlaylistScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = subpagePadding,
                    playlistId = nav.playlistId,
                    initialArtworkUri = nav.coverImgUrl,
                    initialTitle = nav.title,
                    enableSharedTransition = nav.enableSharedTransition
                )
            }
            composable<AlbumNav> { backStackEntry ->
                val nav = backStackEntry.toRoute<AlbumNav>()
                AlbumScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = subpagePadding,
                    albumId = nav.albumId,
                    initialArtworkUri = nav.coverImgUrl,
                    initialTitle = nav.title
                )
            }
            composable(Screen.TopList.route) {
                ListScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = subpagePadding
                )
            }
            composable(Screen.AlbumSublist.route) {
                AlbumSublistScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = subpagePadding
                )
            }
            composable<UserPlayListNav> {
                UserPlaylistScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = subpagePadding
                )
            }
            composable<RecordNav> {
                RecordScreen(
                    navController = navController,
                    bottomContentPadding = subpagePadding
                )
            }
            composable<CloudSongNav> {
                CloudSongScreen(
                    navController = navController,
                    bottomContentPadding = subpagePadding
                )
            }
            composable<ArtistNav> {
                ArtistScreen(
                    navController = navController,
                    bottomContentPadding = subpagePadding
                )
            }
            composable<RadioNav> {
                ProgramRadioScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = subpagePadding
                )
            }
        }
    }
}