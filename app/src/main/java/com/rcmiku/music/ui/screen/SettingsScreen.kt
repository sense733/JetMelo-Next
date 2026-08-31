package com.rcmiku.music.ui.screen

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rcmiku.music.R
import com.rcmiku.music.constants.SettingItemCorner
import com.rcmiku.music.constants.SettingItemHeight
import com.rcmiku.music.constants.SettingItemSubCorner
import com.rcmiku.music.constants.audioQualityKey
import com.rcmiku.music.constants.autoSkipNextOnErrorKey
import com.rcmiku.music.constants.dynamicColorKey
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.constants.themeModeKey
import com.rcmiku.music.constants.use40DpIconKey
import com.rcmiku.music.ui.components.Dialog
import com.rcmiku.music.ui.components.SongQualityDialog
import com.rcmiku.music.ui.icons.DarkMode

import com.rcmiku.music.ui.icons.GraphicEq
import com.rcmiku.music.ui.icons.Login
import com.rcmiku.music.ui.icons.Logout
import com.rcmiku.music.ui.icons.Palette
import com.rcmiku.music.ui.icons.PlayPause
import com.rcmiku.music.ui.icons.SkipNext
import com.rcmiku.music.ui.icons.UserRound
import com.rcmiku.music.ui.navigation.Screen
import com.rcmiku.music.ui.theme.ThemeMode
import com.rcmiku.music.utils.getItemShape
import com.rcmiku.music.utils.rememberEnumPreference
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.ncmapi.api.player.SongLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val uriHandler = LocalUriHandler.current

    var themeMode by rememberEnumPreference(themeModeKey, defaultValue = ThemeMode.SYSTEM)
    var dynamicColor by rememberPreference(dynamicColorKey, true)
    var showThemeDialog by remember { mutableStateOf(false) }

    var use40DpIcon by rememberPreference(use40DpIconKey, false)
    var audioQuality by rememberEnumPreference(audioQualityKey, defaultValue = SongLevel.STANDARD)
    var showQualityDialog by remember { mutableStateOf(false) }
    var autoSkipNextOnError by rememberPreference(autoSkipNextOnErrorKey, false)
    var logout by rememberSaveable { mutableStateOf(false) }
    var ncmCookie by rememberPreference(ncmCookieKey, "")

    val appearanceSettingItems = buildList {
        add(
            SettingItemData(
                title = stringResource(R.string.theme_mode),
                subtitle = when (themeMode) {
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                },
                imageVector = DarkMode,
                onClick = { showThemeDialog = true }
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(
                SettingItemData(
                    title = stringResource(R.string.dynamic_color),
                    subtitle = stringResource(R.string.dynamic_color_subtitle),
                    imageVector = Palette,
                    trailingContent = {
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = { dynamicColor = it }
                        )
                        Spacer(Modifier.width(12.dp))
                    },
                    onClick = { dynamicColor = !dynamicColor }
                )
            )
        }
    }

    val baseSettingItems = listOf(
        SettingItemData(
            title = stringResource(if (ncmCookie.isNotEmpty()) R.string.logout else R.string.login),
            imageVector = if (ncmCookie.isNotEmpty()) Logout else Login,
            onClick = {
                if (ncmCookie.isNotEmpty())
                    logout = true
                else
                    navController.navigate(Screen.Login.route)
            }
        ),
        SettingItemData(
            title = stringResource(R.string.command_button),
            subtitle = stringResource(R.string.command_button_subtitle),
            imageVector = PlayPause,
            trailingContent = {
                Switch(
                    checked = use40DpIcon,
                    onCheckedChange = {
                        use40DpIcon = it
                    }
                )
                Spacer(Modifier.width(12.dp))
            },
            onClick = {
                use40DpIcon = !use40DpIcon
            }
        ),
        SettingItemData(
            title = stringResource(R.string.audio_quality),
            subtitle = when (audioQuality) {
                SongLevel.STANDARD -> stringResource(R.string.standard)
                SongLevel.HIGHER -> stringResource(R.string.higer)
                SongLevel.EXHIGH -> stringResource(R.string.exhigh)
                SongLevel.LOSSLESS -> stringResource(R.string.lossless)
                SongLevel.HIRES -> stringResource(R.string.hi_res)
            },
            imageVector = GraphicEq,
            onClick = {
                showQualityDialog = true
            }
        ),
        SettingItemData(
            title = stringResource(R.string.auto_skip),
            imageVector = SkipNext,
            trailingContent = {
                Switch(
                    checked = autoSkipNextOnError,
                    onCheckedChange = {
                        autoSkipNextOnError = it
                    }
                )
                Spacer(Modifier.width(12.dp))
            },
            onClick = {
                autoSkipNextOnError = !autoSkipNextOnError
            }
        )
    )

    val settingsItems = listOf(
        SettingItemData(
            title = stringResource(R.string.dev),
            subtitle = "rcmiku",
            imageVector = UserRound,
            onClick = { uriHandler.openUri("https://github.com/rcmiku") }
        ),
        SettingItemData(
            title = stringResource(R.string.source_code),
            imageVector = com.rcmiku.music.ui.icons.Code,
            onClick = { uriHandler.openUri("https://github.com/rcmiku/JetMelo") }
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = padding
        ) {
            // 1. Appearance & Theme Section
            item {
                Text(
                    stringResource(R.string.appearance_settings),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            itemsIndexed(appearanceSettingItems) { index, item ->
                val shape = getItemShape(
                    prevItem = appearanceSettingItems.getOrNull(index - 1),
                    nextItem = appearanceSettingItems.getOrNull(index + 1),
                    corner = SettingItemCorner,
                    subCorner = SettingItemSubCorner,
                )

                SettingCard(
                    title = item.title,
                    description = item.subtitle,
                    shape = shape,
                    leadingContent = {
                        Image(
                            imageVector = item.imageVector,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
                    },
                    trailingContent = item.trailingContent,
                    onClick = item.onClick,
                )
            }

            // 2. Basic Settings Section
            item {
                Text(
                    stringResource(R.string.basic_settings),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            itemsIndexed(baseSettingItems) { index, item ->
                val shape = getItemShape(
                    prevItem = baseSettingItems.getOrNull(index - 1),
                    nextItem = baseSettingItems.getOrNull(index + 1),
                    corner = SettingItemCorner,
                    subCorner = SettingItemSubCorner,
                )

                SettingCard(
                    title = item.title,
                    description = item.subtitle,
                    shape = shape,
                    leadingContent = {
                        Image(
                            imageVector = item.imageVector,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
                    },
                    trailingContent = item.trailingContent,
                    onClick = item.onClick,
                )
            }

            // 3. About Section
            item {
                Text(
                    stringResource(R.string.about),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            itemsIndexed(settingsItems) { index, item ->
                val shape = getItemShape(
                    prevItem = settingsItems.getOrNull(index - 1),
                    nextItem = settingsItems.getOrNull(index + 1),
                    corner = SettingItemCorner,
                    subCorner = SettingItemSubCorner,
                )

                SettingCard(
                    title = item.title,
                    description = item.subtitle,
                    shape = shape,
                    leadingContent = {
                        Image(
                            imageVector = item.imageVector,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
                    },
                    onClick = item.onClick,
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = themeMode,
            onModeSelect = {
                themeMode = it
                showThemeDialog = false
            },
            onDismissRequest = { showThemeDialog = false }
        )
    }

    if (showQualityDialog) {
        SongQualityDialog(
            currentLevel = audioQuality,
            onDismiss = { showQualityDialog = false },
            onQualitySelected = {
                audioQuality = it
                showQualityDialog = false
            }
        )
    }

    if (logout) {
        Dialog(
            onConfirmation = {
                ncmCookie = ""
                logout = false
            },
            onDismissRequest = {
                logout = false
            },
            dialogTitle = stringResource(R.string.logout),
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    onModeSelect: (ThemeMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    val modes = listOf(
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark)
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.theme_mode)) },
        text = {
            Column {
                modes.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onModeSelect(mode) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    shape: Shape = RoundedCornerShape(SettingItemCorner),
    leadingContent: @Composable () -> Unit = {},
    trailingContent: @Composable () -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SettingItemHeight),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SettingItemHeight)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                leadingContent()
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            trailingContent()
        }
    }
}

data class SettingItemData(
    val title: String,
    val subtitle: String? = null,
    val imageVector: ImageVector,
    val trailingContent: @Composable () -> Unit = {},
    val onClick: () -> Unit = {},
)
