package com.rcmiku.music.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.constants.ListItemHeight
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.extensions.addSong
import com.rcmiku.music.ui.components.AlbumListItem
import com.rcmiku.music.ui.components.ArtistListItem
import com.rcmiku.music.ui.components.PlaylistListItem
import com.rcmiku.music.ui.components.SongListItem
import com.rcmiku.music.ui.components.SongMenuBottomSheet
import com.rcmiku.music.ui.components.VoiceListItem
import com.rcmiku.music.ui.design.rememberShimmerBrush
import com.rcmiku.music.ui.icons.Album
import com.rcmiku.music.ui.icons.ArrowInsert
import com.rcmiku.music.ui.icons.AudioLines
import com.rcmiku.music.ui.icons.History
import com.rcmiku.music.ui.icons.LocalFireDepartment
import com.rcmiku.music.ui.icons.Search
import com.rcmiku.music.ui.icons.TrendingUp
import com.rcmiku.music.ui.icons.UserRound
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.ui.navigation.ArtistNav
import com.rcmiku.music.ui.navigation.PlaylistNav
import com.rcmiku.music.ui.navigation.RadioNav
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.viewModel.SearchSuggestion
import com.rcmiku.music.viewModel.SearchViewModel
import com.rcmiku.ncmapi.api.search.SearchType
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.toAlbumList
import com.rcmiku.ncmapi.model.toPlaylist
import com.rcmiku.ncmapi.model.toSearchArtist
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    searchViewModel: SearchViewModel = hiltViewModel(),
    bottomContentPadding: Dp = 0.dp
) {
    var searchValue by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(true) }
    val currentSubmittedKeyword by searchViewModel.searchValue.collectAsStateWithLifecycle()
    val searchType by searchViewModel.searchType.collectAsStateWithLifecycle()
    val searchResults = searchViewModel.searchResults.collectAsLazyPagingItems()
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()
    val searchHistoryState by searchViewModel.searchHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val suggestions by searchViewModel.suggestions.collectAsStateWithLifecycle()
    val hotSearches by searchViewModel.hotSearches.collectAsStateWithLifecycle()
    val isHotLoading by searchViewModel.isHotLoading.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val songIds by remember(context) {
        context.favoriteSongIdsDatastore.data.map { it.songIdsList.toSet() }
    }.collectAsStateWithLifecycle(emptySet())
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val tabList = listOf(
        SearchType.Song to stringResource(R.string.song),
        SearchType.Playlist to stringResource(R.string.playlists),
        SearchType.VoiceList to stringResource(R.string.voice_list),
        SearchType.Artist to stringResource(R.string.artist),
        SearchType.Album to stringResource(R.string.album)
    )

    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectSong by remember { mutableStateOf<Song?>(null) }

    val handleExpandedChange: (Boolean) -> Unit = { isExpanded ->
        if (!isExpanded) {
            if (currentSubmittedKeyword.isNotEmpty()) {
                searchValue = currentSubmittedKeyword
                expanded = false
                keyboardController?.hide()
                focusManager.clearFocus()
            } else {
                keyboardController?.hide()
                focusManager.clearFocus()
                navController.navigateUp()
            }
        } else {
            expanded = true
        }
    }

    BackHandler(enabled = !expanded) {
        searchViewModel.updateSearchValue("")
        searchValue = ""
        expanded = true
    }

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
                        searchViewModel.onInputQueryChange(it)
                    },
                    onSearch = {
                        if (it.isNotBlank()) {
                            expanded = false
                            searchViewModel.updateSearchValue(it)
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    },
                    expanded = expanded,
                    onExpandedChange = handleExpandedChange,
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = {
                        IconButton(onClick = {
                            if (expanded) {
                                handleExpandedChange(false)
                            } else {
                                searchViewModel.updateSearchValue("")
                                searchValue = ""
                                expanded = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    trailingIcon = {
                        if (searchValue.isNotEmpty()) {
                            IconButton(onClick = {
                                searchValue = ""
                                searchViewModel.onInputQueryChange("")
                                expanded = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear)
                                )
                            }
                        }
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = handleExpandedChange,
            content = {
                if (searchValue.isNotEmpty()) {
                    val songs = remember(suggestions) { suggestions.filterIsInstance<SearchSuggestion.Song>() }
                    val albums = remember(suggestions) { suggestions.filterIsInstance<SearchSuggestion.Album>() }
                    val artists = remember(suggestions) { suggestions.filterIsInstance<SearchSuggestion.Artist>() }
                    val keywords = remember(suggestions) { suggestions.filterIsInstance<SearchSuggestion.Keyword>() }

                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 16.dp + bottomContentPadding
                        )
                    ) {
                        if (suggestions.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    shape = JetMeloShapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (songs.isNotEmpty()) {
                                            SuggestGroupHeader(title = stringResource(R.string.song))
                                            songs.forEach { song ->
                                                SongSuggestItem(
                                                    song = song,
                                                    query = searchValue,
                                                    onInsert = {
                                                        searchValue = song.name
                                                        searchViewModel.onInputQueryChange(song.name)
                                                    },
                                                    onClick = {
                                                        searchValue = song.name
                                                        expanded = false
                                                        searchViewModel.updateSearchValue(song.name)
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                    }
                                                )
                                            }
                                        }

                                        if (albums.isNotEmpty()) {
                                            SuggestGroupHeader(title = stringResource(R.string.album))
                                            albums.forEach { album ->
                                                AlbumSuggestItem(
                                                    album = album,
                                                    query = searchValue,
                                                    onInsert = {
                                                        searchValue = album.name
                                                        searchViewModel.onInputQueryChange(album.name)
                                                    },
                                                    onClick = {
                                                        searchValue = album.name
                                                        expanded = false
                                                        searchViewModel.updateSearchValue(album.name)
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                    }
                                                )
                                            }
                                        }

                                        if (artists.isNotEmpty()) {
                                            SuggestGroupHeader(title = stringResource(R.string.artist))
                                            artists.forEach { artist ->
                                                ArtistSuggestItem(
                                                    artist = artist,
                                                    query = searchValue,
                                                    onInsert = {
                                                        searchValue = artist.name
                                                        searchViewModel.onInputQueryChange(artist.name)
                                                    },
                                                    onClick = {
                                                        searchValue = artist.name
                                                        expanded = false
                                                        searchViewModel.updateSearchValue(artist.name)
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                    }
                                                )
                                            }
                                        }

                                        if (keywords.isNotEmpty()) {
                                            SuggestGroupHeader(title = stringResource(R.string.suggest_keywords))
                                            keywords.forEach { kw ->
                                                KeywordSuggestItem(
                                                    keyword = kw.keyword,
                                                    query = searchValue,
                                                    onInsert = {
                                                        searchValue = kw.keyword
                                                        searchViewModel.onInputQueryChange(kw.keyword)
                                                    },
                                                    onClick = {
                                                        searchValue = kw.keyword
                                                        expanded = false
                                                        searchViewModel.updateSearchValue(kw.keyword)
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 16.dp + bottomContentPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (searchHistoryState.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    shape = JetMeloShapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.recent_searches),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            TextButton(
                                                onClick = { searchViewModel.clearSearchHistory() },
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.clear_all),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        searchHistoryState.forEach { historyItem ->
                                            RecentHistoryItem(
                                                query = historyItem,
                                                onClick = {
                                                    searchValue = historyItem
                                                    expanded = false
                                                    searchViewModel.updateSearchValue(historyItem)
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                },
                                                onDelete = {
                                                    searchViewModel.deleteSearchQuery(historyItem)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = JetMeloShapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = LocalFireDepartment,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.hot_searches),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    if (isHotLoading && hotSearches.isEmpty()) {
                                        val brush = rememberShimmerBrush()
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val pillWidths = listOf(80.dp, 110.dp, 90.dp, 120.dp, 85.dp, 100.dp)
                                            pillWidths.forEach { pillWidth ->
                                                Box(
                                                    modifier = Modifier
                                                        .width(pillWidth)
                                                        .height(36.dp)
                                                        .clip(JetMeloShapes.full)
                                                        .background(brush)
                                                )
                                            }
                                        }
                                    } else {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            hotSearches.forEachIndexed { index, tag ->
                                                val showTrending = index < 2
                                                Surface(
                                                    shape = JetMeloShapes.full,
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    border = BorderStroke(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                    ),
                                                    modifier = Modifier
                                                        .clip(JetMeloShapes.full)
                                                        .clickable {
                                                            searchValue = tag
                                                            expanded = false
                                                            searchViewModel.updateSearchValue(tag)
                                                            focusManager.clearFocus()
                                                            keyboardController?.hide()
                                                        }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                                    ) {
                                                        if (showTrending) {
                                                            Icon(
                                                                imageVector = TrendingUp,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.tertiary,
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .padding(end = 4.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = tag,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )

        if (!expanded && currentSubmittedKeyword.isNotEmpty()) {
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
                .semantics { traversalIndex = 1f },
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + bottomContentPadding)
        ) {
            when (searchType) {
                SearchType.Song -> {
                    items(
                        count = searchResults.itemCount,
                        key = { index -> searchResults.peek(index)?.song?.id ?: index }
                    ) { index ->
                        searchResults[index]?.let { resource ->
                            resource.song?.let { song ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    shape = JetMeloShapes.small,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    SongListItem(
                                        isPlaying = isPlaying,
                                        isActive = currentMediaId == song.id,
                                        showLikedIcon = song.id in songIds,
                                        song = song,
                                        songIndex = index + 1,
                                        modifier = Modifier.clickable {
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
                }

                SearchType.Playlist -> {
                    items(
                        count = searchResults.itemCount,
                        key = { index -> searchResults.peek(index)?.toPlaylist()?.id ?: index }
                    ) { index ->
                        searchResults[index]?.let { resource ->
                            resource.toPlaylist()?.let { playlist ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    shape = JetMeloShapes.small,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    PlaylistListItem(
                                        playlist = playlist,
                                        modifier = Modifier.clickable {
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
                }

                SearchType.VoiceList -> {
                    items(
                        count = searchResults.itemCount,
                        key = { index -> searchResults.peek(index)?.baseInfo?.id ?: index }
                    ) { index ->
                        searchResults[index]?.let { resource ->
                            resource.baseInfo?.let { voice ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    shape = JetMeloShapes.small,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    VoiceListItem(
                                        voice = voice,
                                        modifier = Modifier.clickable {
                                            navController.navigate(RadioNav(radioId = voice.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                SearchType.Artist -> {
                    items(
                        count = searchResults.itemCount,
                        key = { index -> searchResults.peek(index)?.toSearchArtist()?.id ?: index }
                    ) { index ->
                        searchResults[index]?.let { resource ->
                            resource.toSearchArtist()?.let { artist ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    shape = JetMeloShapes.small,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    ArtistListItem(
                                        artist = artist,
                                        modifier = Modifier.clickable {
                                            navController.navigate(ArtistNav(artistId = artist.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                SearchType.Album -> {
                    items(
                        count = searchResults.itemCount,
                        key = { index -> searchResults.peek(index)?.toAlbumList()?.id ?: index }
                    ) { index ->
                        searchResults[index]?.let { resource ->
                            resource.toAlbumList()?.let { album ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    shape = JetMeloShapes.small,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    AlbumListItem(
                                        album = album,
                                        modifier = Modifier.clickable {
                                            navController.navigate(AlbumNav(albumId = album.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    SongMenuBottomSheet(
        navController = navController,
        song = selectSong,
        onDismiss = {
            openBottomSheet = false
            selectSong = null
        },
        openBottomSheet = openBottomSheet && selectSong != null
    )
}

@Composable
fun SuggestGroupHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
fun SuggestBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = JetMeloShapes.extraSmall,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
fun SongSuggestItem(
    song: SearchSuggestion.Song,
    query: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onInsert: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(ListItemHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        if (!song.coverUrl.isNullOrEmpty()) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = song.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(JetMeloShapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(JetMeloShapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AudioLines,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = highlightSearchKeyword(
                        text = song.name,
                        query = query,
                        highlightColor = MaterialTheme.colorScheme.primary
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                SuggestBadge(
                    text = stringResource(R.string.song),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            if (song.artistName.isNotBlank()) {
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onInsert) {
            Icon(
                imageVector = ArrowInsert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AlbumSuggestItem(
    album: SearchSuggestion.Album,
    query: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onInsert: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(ListItemHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(JetMeloShapes.small)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Album,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = highlightSearchKeyword(
                        text = album.name,
                        query = query,
                        highlightColor = MaterialTheme.colorScheme.primary
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                SuggestBadge(
                    text = stringResource(R.string.album),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            if (album.artistName.isNotBlank()) {
                Text(
                    text = album.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onInsert) {
            Icon(
                imageVector = ArrowInsert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ArtistSuggestItem(
    artist: SearchSuggestion.Artist,
    query: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onInsert: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(ListItemHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        if (!artist.avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = artist.avatarUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = UserRound,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = highlightSearchKeyword(
                        text = artist.name,
                        query = query,
                        highlightColor = MaterialTheme.colorScheme.primary
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                SuggestBadge(
                    text = stringResource(R.string.artist),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Text(
                text = stringResource(R.string.artist),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onInsert) {
            Icon(
                imageVector = ArrowInsert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun KeywordSuggestItem(
    keyword: String,
    query: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onInsert: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(ListItemHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(JetMeloShapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Text(
            text = highlightSearchKeyword(
                text = keyword,
                query = query,
                highlightColor = MaterialTheme.colorScheme.primary
            ),
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

@Composable
fun RecentHistoryItem(
    query: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

fun highlightSearchKeyword(
    text: String,
    query: String,
    highlightColor: Color
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val index = text.indexOf(query, ignoreCase = true)
    if (index < 0) return AnnotatedString(text)

    return buildAnnotatedString {
        if (index > 0) {
            append(text.substring(0, index))
        }
        withStyle(
            SpanStyle(
                color = highlightColor,
                fontWeight = FontWeight.Bold
            )
        ) {
            append(text.substring(index, index + query.length))
        }
        if (index + query.length < text.length) {
            append(text.substring(index + query.length))
        }
    }
}