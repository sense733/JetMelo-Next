package com.rcmiku.music.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.extensions.addSong
import com.rcmiku.music.ui.components.AlbumListItem
import com.rcmiku.music.ui.components.ArtistListItem
import com.rcmiku.music.ui.components.PlaylistListItem
import com.rcmiku.music.ui.components.SongListItem
import com.rcmiku.music.ui.components.SongMenuBottomSheet
import com.rcmiku.music.ui.components.VoiceListItem
import com.rcmiku.music.ui.icons.ArrowInsert
import com.rcmiku.music.ui.icons.History
import com.rcmiku.music.ui.icons.Search
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.ui.navigation.ArtistNav
import com.rcmiku.music.ui.navigation.PlaylistNav
import com.rcmiku.music.ui.navigation.RadioNav
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.viewModel.SearchViewModel
import com.rcmiku.ncmapi.api.search.SearchType
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.toAlbumList
import com.rcmiku.ncmapi.model.toPlaylist
import com.rcmiku.ncmapi.model.toSearchArtist
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    var searchValue by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val searchType by searchViewModel.searchType.collectAsState()
    val searchResults = searchViewModel.searchResults.collectAsLazyPagingItems()
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()
    val searchHistoryState by searchViewModel.searchHistory.collectAsState(initial = emptyList())
    val suggestKeywordResponseState by searchViewModel.suggestKeywordResponse.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val songIds by context.favoriteSongIdsDatastore.data.map { it.songIdsList }
        .collectAsState(emptyList())
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val tabList = listOf(
        SearchType.Song to stringResource(R.string.song),
        SearchType.Playlist to stringResource(R.string.playlists),
        SearchType.VoiceList to "播客",
        SearchType.Artist to stringResource(R.string.artist),
        SearchType.Album to stringResource(R.string.album)
    )

    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectSong by remember { mutableStateOf<Song?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (expanded) 0.dp else 16.dp)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchValue,
                    onQueryChange = {
                        searchValue = it
                        if (it.isNotEmpty()) {
                            searchViewModel.fetchSearchKeyword(it)
                        }
                    },
                    onSearch = {
                        expanded = false
                        searchViewModel.updateSearchValue(it)
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = it
                    },
                    placeholder = { Text(stringResource(R.string.search)) },
                    leadingIcon = {
                        IconButton(onClick = {
                            if (!expanded) {
                                navController.navigateUp()
                            } else {
                                expanded = false
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = {
                expanded = it
            },
            content = {
                LazyColumn {
                    suggestKeywordResponseState?.data?.suggests?.let {
                        items(it) { item ->
                            SuggestItem(
                                history = false,
                                query = item.keyword,
                                onInsert = {
                                    searchValue = item.keyword
                                    focusManager.clearFocus()
                                },
                                onClick = {
                                    searchValue = item.keyword
                                    expanded = false
                                    searchViewModel.updateSearchValue(item.keyword)
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                },
                                onDelete = { }
                            )
                        }
                    }

                    items(searchHistoryState) { historyItem ->
                        SuggestItem(
                            history = true,
                            query = historyItem,
                            onInsert = {
                                searchValue = historyItem
                                focusManager.clearFocus()
                            },
                            onClick = {
                                searchValue = historyItem
                                expanded = false
                                searchViewModel.updateSearchValue(historyItem)
                                focusManager.moveFocus(FocusDirection.Right)
                                keyboardController?.hide()
                            },
                            onDelete = {
                                searchViewModel.deleteSearchQuery(historyItem)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }
            }
        )

        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabList.forEachIndexed { index, item ->
                Tab(
                    modifier = Modifier.clip(JetMeloShapes.small),
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        searchViewModel.updateSearchType(item.first)
                    },
                    text = {
                        Text(
                            text = item.second,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .semantics { traversalIndex = 1f }
        ) {
            when (searchType) {
                SearchType.Song -> {
                    items(searchResults.itemCount) { index ->
                        searchResults[index]?.let { resource ->
                            resource.song?.let { song ->
                                SongListItem(
                                    isPlaying = isPlaying,
                                    isActive = currentMediaId == song.id,
                                    showLikedIcon = song.id in songIds,
                                    song = song,
                                    songIndex = index + 1,
                                    modifier = Modifier
                                        .clip(JetMeloShapes.small)
                                        .clickable {
                                            mediaController?.addSong(song)
                                        },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            selectSong = song
                                            openBottomSheet = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = stringResource(R.string.more)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                SearchType.Playlist -> {
                    items(searchResults.itemCount) { index ->
                        searchResults[index]?.let { resource ->
                            resource.toPlaylist()?.let { playlist ->
                                PlaylistListItem(
                                    playlist = playlist,
                                    modifier = Modifier
                                        .clip(JetMeloShapes.small)
                                        .clickable {
                                            navController.navigate(
                                                PlaylistNav(
                                                    playlistId = playlist.id,
                                                    limit = playlist.trackCount
                                                )
                                            )
                                        }
                                )
                            }
                        }
                    }
                }

                SearchType.VoiceList -> {
                    items(searchResults.itemCount) { index ->
                        searchResults[index]?.let { resource ->
                            resource.baseInfo?.let { voice ->
                                VoiceListItem(
                                    voice = voice,
                                    modifier = Modifier
                                        .clip(JetMeloShapes.small)
                                        .clickable {
                                            navController.navigate(RadioNav(radioId = voice.id))
                                        }
                                )
                            }
                        }
                    }
                }

                SearchType.Artist -> {
                    items(searchResults.itemCount) { index ->
                        searchResults[index]?.let { resource ->
                            resource.toSearchArtist()?.let { artist ->
                                ArtistListItem(
                                    artist = artist,
                                    modifier = Modifier
                                        .clip(JetMeloShapes.small)
                                        .clickable {
                                            navController.navigate(ArtistNav(artistId = artist.id))
                                        }
                                )
                            }
                        }
                    }
                }

                SearchType.Album -> {
                    items(searchResults.itemCount) { index ->
                        searchResults[index]?.let { resource ->
                            resource.toAlbumList()?.let { album ->
                                AlbumListItem(
                                    album = album,
                                    modifier = Modifier
                                        .clip(JetMeloShapes.small)
                                        .clickable {
                                            navController.navigate(AlbumNav(albumId = album.id))
                                        }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    SongMenuBottomSheet(
        navController = navController,
        song = selectSong,
        onDismiss = { openBottomSheet = false },
        openBottomSheet = openBottomSheet
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuggestItem(
    history: Boolean,
    modifier: Modifier = Modifier,
    query: String,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onInsert: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onDelete() }
            )
            .padding(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = if (history) History else Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onInsert) {
            Icon(
                imageVector = ArrowInsert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}