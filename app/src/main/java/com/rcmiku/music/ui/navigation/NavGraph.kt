package com.rcmiku.music.ui.navigation

import android.provider.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

private const val NAV_DURATION_FORWARD = 350
private const val NAV_DURATION_POP = 350
private const val NAV_PARALLAX_FACTOR = 0.25f
private const val SCALE_BG = 0.93f
private const val SCALE_EXIT_POP = 0.97f

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    bottomContentPadding: Dp = 0.dp
) {
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

    val forwardDuration = (NAV_DURATION_FORWARD * animatorScale).roundToInt().coerceAtLeast(0)
    val popDuration = (NAV_DURATION_POP * animatorScale).roundToInt().coerceAtLeast(0)

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(forwardDuration, easing = EmphasizedDecelerateEasing)
                ) + fadeIn(
                    animationSpec = tween(forwardDuration, easing = EmphasizedDecelerateEasing),
                    initialAlpha = 0.4f
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { (-it * NAV_PARALLAX_FACTOR).toInt() },
                    animationSpec = tween(forwardDuration, easing = EmphasizedDecelerateEasing)
                ) + scaleOut(
                    targetScale = SCALE_BG,
                    transformOrigin = TransformOrigin.Center,
                    animationSpec = tween(forwardDuration, easing = EmphasizedDecelerateEasing)
                ) + fadeOut(
                    animationSpec = tween(forwardDuration, easing = EmphasizedDecelerateEasing),
                    targetAlpha = 0.6f
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { (-it * NAV_PARALLAX_FACTOR).toInt() },
                    animationSpec = tween(popDuration, easing = EmphasizedDecelerateEasing)
                ) + scaleIn(
                    initialScale = SCALE_BG,
                    transformOrigin = TransformOrigin.Center,
                    animationSpec = tween(popDuration, easing = EmphasizedDecelerateEasing)
                ) + fadeIn(
                    animationSpec = tween(popDuration, easing = EmphasizedDecelerateEasing),
                    initialAlpha = 0.6f
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(popDuration, easing = EmphasizedDecelerateEasing)
                ) + scaleOut(
                    targetScale = SCALE_EXIT_POP,
                    transformOrigin = TransformOrigin.Center,
                    animationSpec = tween(popDuration, easing = EmphasizedDecelerateEasing)
                ) + fadeOut(
                    animationSpec = tween(popDuration, easing = EmphasizedDecelerateEasing),
                    targetAlpha = 0f
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