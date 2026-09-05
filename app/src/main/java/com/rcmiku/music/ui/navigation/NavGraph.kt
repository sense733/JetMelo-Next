package com.rcmiku.music.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rcmiku.music.constants.DURATION_ENTER
import com.rcmiku.music.constants.DURATION_EXIT
import com.rcmiku.music.constants.EmphasizedAccelerateEasing
import com.rcmiku.music.constants.EmphasizedDecelerateEasing
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

private const val NAV_OFFSET_FACTOR = 0.15f
private const val NAV_TRANSITION_ALPHA = 0.8f

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    bottomContentPadding: Dp = 0.dp
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { (it * NAV_OFFSET_FACTOR).toInt() },
                    animationSpec = tween(DURATION_ENTER, easing = EmphasizedDecelerateEasing)
                ) + fadeIn(
                    animationSpec = tween(DURATION_ENTER, easing = EmphasizedDecelerateEasing),
                    initialAlpha = NAV_TRANSITION_ALPHA
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { (-it * NAV_OFFSET_FACTOR).toInt() },
                    animationSpec = tween(DURATION_EXIT, easing = EmphasizedAccelerateEasing)
                ) + fadeOut(
                    animationSpec = tween(DURATION_EXIT, easing = EmphasizedAccelerateEasing),
                    targetAlpha = NAV_TRANSITION_ALPHA
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { (-it * NAV_OFFSET_FACTOR).toInt() },
                    animationSpec = tween(DURATION_ENTER, easing = EmphasizedDecelerateEasing)
                ) + fadeIn(
                    animationSpec = tween(DURATION_ENTER, easing = EmphasizedDecelerateEasing),
                    initialAlpha = NAV_TRANSITION_ALPHA
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { (it * NAV_OFFSET_FACTOR).toInt() },
                    animationSpec = tween(DURATION_EXIT, easing = EmphasizedAccelerateEasing)
                ) + fadeOut(
                    animationSpec = tween(DURATION_EXIT, easing = EmphasizedAccelerateEasing),
                    targetAlpha = NAV_TRANSITION_ALPHA
                )
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable(Screen.Explore.route) {
                ExploreScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    navController = navController,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    navController = navController,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable(Screen.Login.route) { LoginScreen(navController = navController) }
            composable(Screen.Search.route) {
                SearchScreen(
                    navController = navController,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable<PlaylistNav> {
                PlaylistScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable<AlbumNav> {
                AlbumScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable(Screen.TopList.route) {
                ListScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable(Screen.AlbumSublist.route) {
                AlbumSublistScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable<UserPlayListNav> {
                UserPlaylistScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable<RecordNav> {
                RecordScreen(
                    navController = navController,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable<CloudSongNav> {
                CloudSongScreen(
                    navController = navController,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable<ArtistNav> {
                ArtistScreen(
                    navController = navController,
                    bottomContentPadding = bottomContentPadding
                )
            }
            composable<RadioNav> {
                ProgramRadioScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable,
                    bottomContentPadding = bottomContentPadding
                )
            }
        }
    }
}