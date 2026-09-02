package com.rcmiku.music

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.media3.common.util.UnstableApi
import com.rcmiku.music.constants.dynamicColorKey
import com.rcmiku.music.constants.themeModeKey
import com.rcmiku.music.extensions.init
import com.rcmiku.music.playback.PlayerController
import com.rcmiku.music.playback.PlayerState
import com.rcmiku.music.playback.state
import com.rcmiku.music.ui.screen.MainScreen
import com.rcmiku.music.ui.theme.JetMeloTheme
import com.rcmiku.music.ui.theme.ThemeMode
import com.rcmiku.music.utils.rememberEnumPreference
import com.rcmiku.music.utils.rememberPreference
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var playerController: PlayerController
    private var playerState by mutableStateOf<PlayerState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        playerController = PlayerController
        setContent {
            val themeMode by rememberEnumPreference(themeModeKey, ThemeMode.SYSTEM)
            val dynamicColor by rememberPreference(dynamicColorKey, true)

            val controller = playerController.controller

            DisposableEffect(controller) {
                if (controller != null) {
                    playerState = controller.state(applicationContext)
                }
                onDispose {
                    playerState?.dispose()
                    playerState = null
                }
            }

            LaunchedEffect(controller) {
                controller?.init(applicationContext)
            }
            JetMeloTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                CompositionLocalProvider(
                    LocalPlayerController provides playerController,
                    LocalPlayerState provides playerState
                ) {
                    MainScreen()
                }
            }
        }
    }

}

val LocalPlayerController = staticCompositionLocalOf<PlayerController> {
    error("No PlayerController provided")
}

val LocalPlayerState = staticCompositionLocalOf<PlayerState?> {
    error("No PlayerState provided")
}