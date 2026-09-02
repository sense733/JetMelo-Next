package com.rcmiku.music.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rcmiku.music.R
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.ui.components.Dialog
import com.rcmiku.music.ui.design.ProfileHeaderCard
import com.rcmiku.music.ui.design.QuickActionCard
import com.rcmiku.music.ui.design.QuickActionItem
import com.rcmiku.music.ui.design.SectionHeader
import com.rcmiku.music.ui.icons.Album
import com.rcmiku.music.ui.icons.Cloud
import com.rcmiku.music.ui.icons.FavoriteFill
import com.rcmiku.music.ui.icons.Leaderboard
import com.rcmiku.music.ui.icons.Logout
import com.rcmiku.music.ui.icons.Star
import com.rcmiku.music.ui.navigation.CloudSongNav
import com.rcmiku.music.ui.navigation.PlaylistNav
import com.rcmiku.music.ui.navigation.RecordNav
import com.rcmiku.music.ui.navigation.Screen
import com.rcmiku.music.ui.navigation.UserPlayListNav
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.music.viewModel.LibraryScreenViewModel
import com.rcmiku.ncmapi.api.account.UserPlaylistType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavHostController,
    libraryScreenViewModel: LibraryScreenViewModel = hiltViewModel(),
    bottomContentPadding: Dp = 0.dp
) {
    var ncmCookie by rememberPreference(ncmCookieKey, "")
    val userInfoBatchState by libraryScreenViewModel.userInfo.collectAsStateWithLifecycle()
    val favoriteSongState by libraryScreenViewModel.favoriteSong.collectAsStateWithLifecycle()
    var logout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(ncmCookie) {
        if (ncmCookie.isNotEmpty()) {
            libraryScreenViewModel.fetchUserInfo()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.library),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search)
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                    if (ncmCookie.isNotEmpty()) {
                        IconButton(onClick = { logout = true }) {
                            Icon(
                                imageVector = Logout,
                                contentDescription = stringResource(R.string.logout),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp + bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. User Profile Section
            item {
                val profile = userInfoBatchState?.account?.profile
                val level = userInfoBatchState?.level?.data?.level
                if (profile != null) {
                    ProfileHeaderCard(
                        avatarUrl = profile.avatarUrl,
                        nickname = profile.nickname,
                        isVip = profile.vipType != 0,
                        level = level,
                        onClick = { }
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(JetMeloShapes.large)
                            .clickable { navController.navigate(Screen.Settings.route) },
                        shape = JetMeloShapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.login_prompt),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. 2x2 Quick Action Grid (Semantic icon coloring)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Row 1: Favorite Songs & Cloud Music
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val favCount = favoriteSongState?.ids?.size
                        val errorColor = MaterialTheme.colorScheme.error
                        QuickActionCard(
                            item = QuickActionItem(
                                title = stringResource(R.string.favorite_music),
                                subtitle = if (favCount != null && favCount > 0)
                                    stringResource(R.string.song_size, favCount)
                                else
                                    stringResource(R.string.click_to_view),
                                icon = FavoriteFill,
                                iconTint = errorColor,
                                iconBackground = errorColor.copy(alpha = 0.12f),
                                onClick = {
                                    favoriteSongState?.data?.id?.let {
                                        navController.navigate(PlaylistNav(playlistId = it, noCache = true))
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            item = QuickActionItem(
                                title = stringResource(R.string.cloud_music),
                                subtitle = stringResource(R.string.cloud_music_subtitle),
                                icon = Cloud,
                                iconTint = MaterialTheme.colorScheme.primary,
                                iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                onClick = {
                                    userInfoBatchState?.account?.profile?.userId?.let {
                                        navController.navigate(CloudSongNav(uid = it))
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Listen Ranking & Favorite Albums
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            item = QuickActionItem(
                                title = stringResource(R.string.record),
                                subtitle = stringResource(R.string.record),
                                icon = Leaderboard,
                                iconTint = MaterialTheme.colorScheme.tertiary,
                                iconBackground = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                onClick = {
                                    userInfoBatchState?.account?.profile?.userId?.let {
                                        navController.navigate(RecordNav(uid = it))
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            item = QuickActionItem(
                                title = stringResource(R.string.favorite_album),
                                subtitle = stringResource(R.string.favorite_album),
                                icon = Album,
                                iconTint = MaterialTheme.colorScheme.secondary,
                                iconBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                onClick = {
                                    navController.navigate(Screen.AlbumSublist.route)
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. User Playlists Section
            item {
                SectionHeader(title = stringResource(R.string.my_playlists))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Created Playlists
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(JetMeloShapes.medium)
                            .clickable {
                                userInfoBatchState?.account?.profile?.userId?.let {
                                    navController.navigate(
                                        UserPlayListNav(
                                            userId = it,
                                            type = UserPlaylistType.CREATE.type
                                        )
                                    )
                                }
                            },
                        shape = JetMeloShapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.List,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = stringResource(R.string.create_playlist),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Collected Playlists
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(JetMeloShapes.medium)
                            .clickable {
                                userInfoBatchState?.account?.profile?.userId?.let {
                                    navController.navigate(
                                        UserPlayListNav(
                                            userId = it,
                                            type = UserPlaylistType.COLLECT.type
                                        )
                                    )
                                }
                            },
                        shape = JetMeloShapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = stringResource(R.string.collect_playlist),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (logout) {
        Dialog(
            onConfirmation = {
                ncmCookie = ""
                com.rcmiku.ncmapi.utils.CookieProvider.clear()
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                logout = false
            },
            onDismissRequest = {
                logout = false
            },
            dialogTitle = stringResource(R.string.logout),
        )
    }
}
