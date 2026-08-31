package com.rcmiku.music.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class JetMeloExtendedColors(
    val emberAccent: Color = EmberAccent,
    val emberAccentContainer: Color = EmberAccentContainer,
    val onEmberAccent: Color = OnEmberAccent,
    val onEmberAccentContainer: Color = OnEmberAccentContainer
)

val LocalJetMeloExtendedColors = staticCompositionLocalOf { JetMeloExtendedColors() }

private val DarkColorScheme = darkColorScheme(
    primary = IndigoDarkPrimary,
    onPrimary = IndigoDarkOnPrimary,
    primaryContainer = IndigoDarkPrimaryContainer,
    onPrimaryContainer = IndigoDarkOnPrimaryContainer,
    secondary = IndigoDarkSecondary,
    onSecondary = IndigoDarkOnSecondary,
    secondaryContainer = IndigoDarkSecondaryContainer,
    onSecondaryContainer = IndigoDarkOnSecondaryContainer,
    tertiary = IndigoDarkTertiary,
    onTertiary = IndigoDarkOnTertiary,
    tertiaryContainer = IndigoDarkTertiaryContainer,
    onTertiaryContainer = IndigoDarkOnTertiaryContainer,
    background = IndigoDarkBackground,
    onBackground = IndigoDarkOnBackground,
    surface = IndigoDarkSurface,
    onSurface = IndigoDarkOnSurface,
    surfaceVariant = IndigoDarkSurfaceVariant,
    onSurfaceVariant = IndigoDarkOnSurfaceVariant,
    surfaceContainerLowest = IndigoDarkSurfaceContainerLowest,
    surfaceContainerLow = IndigoDarkSurfaceContainerLow,
    surfaceContainer = IndigoDarkSurfaceContainer,
    surfaceContainerHigh = IndigoDarkSurfaceContainerHigh,
    surfaceContainerHighest = IndigoDarkSurfaceContainerHighest,
    outline = IndigoDarkOutline,
    outlineVariant = IndigoDarkOutlineVariant,
    error = IndigoDarkError,
    onError = IndigoDarkOnError,
    errorContainer = IndigoDarkErrorContainer,
    onErrorContainer = IndigoDarkOnErrorContainer,
    scrim = IndigoDarkScrim
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoLightPrimary,
    onPrimary = IndigoLightOnPrimary,
    primaryContainer = IndigoLightPrimaryContainer,
    onPrimaryContainer = IndigoLightOnPrimaryContainer,
    secondary = IndigoLightSecondary,
    onSecondary = IndigoLightOnSecondary,
    secondaryContainer = IndigoLightSecondaryContainer,
    onSecondaryContainer = IndigoLightOnSecondaryContainer,
    tertiary = IndigoLightTertiary,
    onTertiary = IndigoLightOnTertiary,
    tertiaryContainer = IndigoLightTertiaryContainer,
    onTertiaryContainer = IndigoLightOnTertiaryContainer,
    background = IndigoLightBackground,
    onBackground = IndigoLightOnBackground,
    surface = IndigoLightSurface,
    onSurface = IndigoLightOnSurface,
    surfaceVariant = IndigoLightSurfaceVariant,
    onSurfaceVariant = IndigoLightOnSurfaceVariant,
    surfaceContainerLowest = IndigoLightSurfaceContainerLowest,
    surfaceContainerLow = IndigoLightSurfaceContainerLow,
    surfaceContainer = IndigoLightSurfaceContainer,
    surfaceContainerHigh = IndigoLightSurfaceContainerHigh,
    surfaceContainerHighest = IndigoLightSurfaceContainerHighest,
    outline = IndigoLightOutline,
    outlineVariant = IndigoLightOutlineVariant,
    error = IndigoLightError,
    onError = IndigoLightOnError,
    errorContainer = IndigoLightErrorContainer,
    onErrorContainer = IndigoLightOnErrorContainer,
    scrim = IndigoLightScrim
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun JetMeloTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = JetMeloExtendedColors()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalJetMeloExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = JetMeloM3Shapes,
            content = content
        )
    }
}