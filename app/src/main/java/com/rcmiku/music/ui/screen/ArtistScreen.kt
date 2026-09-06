package com.rcmiku.music.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.data.favoriteSongIdsDatastore
import kotlinx.coroutines.flow.map
import com.rcmiku.music.R
import com.rcmiku.music.extensions.playMediaAtId
import com.rcmiku.music.extensions.setPlaylist
import com.rcmiku.music.ui.components.AlbumListItem
import com.rcmiku.music.ui.components.NavigationTitle
import com.rcmiku.music.ui.components.SongListItem
import com.rcmiku.music.ui.components.SongMenuBottomSheet
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.ui.theme.JetMeloShapes
import com.rcmiku.music.viewModel.ArtistScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavHostController,
    artistScreenViewModel: ArtistScreenViewModel = hiltViewModel(),
    bottomContentPadding: Dp = 0.dp
) {
    val artistHeadInfoState by artistScreenViewModel.artistHeadInfo.collectAsStateWithLifecycle()
    val artistTopSongState by artistScreenViewModel.artistTopSong.collectAsStateWithLifecycle()
    val artistAlbumList = artistScreenViewModel.artistAlbumList.collectAsLazyPagingItems()
    val isLoading by artistScreenViewModel.isLoading.collectAsStateWithLifecycle()
    val loadError by artistScreenViewModel.loadError.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val showArtistTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    var state by rememberSaveable { mutableIntStateOf(1) }
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSongId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectSong = artistTopSongState?.songs?.firstOrNull { it.id == selectedSongId }
    val context = LocalContext.current
    val songIds by remember(context) {
        context.favoriteSongIdsDatastore.data.map { it.songIdsList.toSet() }
    }.collectAsStateWithLifecycle(emptySet())

    LaunchedEffect(Unit) {
        artistScreenViewModel.actionError.collect {
            Toast.makeText(context, context.getString(R.string.operation_failed), Toast.LENGTH_SHORT).show()
        }
    }

    val titles = listOf(
        stringResource(R.string.home),
        stringResource(R.string.song),
        stringResource(R.string.album)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showArtistTitle) artistHeadInfoState?.data?.artist?.name ?: "" else "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigateUp()
                        },
                        colors = if (showArtistTitle) IconButtonDefaults.iconButtonColors() else IconButtonDefaults.filledIconButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (showArtistTitle)
                        MaterialTheme.colorScheme.surfaceContainer
                    else
                        Color.Transparent
                )
            )
        }
    ) { padding ->
        val headInfo = artistHeadInfoState
        val topSong = artistTopSongState

        when {
            headInfo == null && topSong == null && isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding()),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            headInfo == null && topSong == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                        .clickable(role = Role.Button) {
                            artistScreenViewModel.retry()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.operation_failed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = bottomContentPadding
                    )
                ) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            AsyncImage(
                                model = artistHeadInfoState?.data?.artist?.cover,
                                contentDescription = artistHeadInfoState?.data?.artist?.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.aspectRatio(4f / 3f)
                            )
                            artistHeadInfoState?.data?.artist?.name?.let {
                                Box(
                                    Modifier
                                        .padding(6.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(color = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        text = it,
                                        Modifier.padding(4.dp),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Column {
                            SecondaryTabRow(selectedTabIndex = state) {
                                titles.forEachIndexed { index, title ->
                                    Tab(
                                        selected = state == index,
                                        onClick = { state = index },
                                        text = {
                                            Text(
                                                text = title,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    when (state) {
                        0 -> {
                            item {
                                NavigationTitle(
                                    title = stringResource(R.string.artist_info),
                                    modifier = Modifier.animateItem()
                                )
                                artistHeadInfoState?.data?.artist?.briefDesc?.trimIndent()?.let {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp)
                                            .padding(bottom = 12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                    ) {
                                        if (it.isNotBlank())
                                            Text(
                                                text = it,
                                                modifier = Modifier.padding(12.dp),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        else
                                            Text(
                                                text = stringResource(R.string.no_brief),
                                                modifier = Modifier.padding(12.dp),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                    }
                                }
                            }
                        }

                        1 -> {
                            val songs = artistTopSongState?.songs
                            when {
                                songs == null -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                                songs.isEmpty() -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.no_brief),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                                        SongListItem(
                                            song = song,
                                            isPlaying = isPlaying,
                                            showLikedIcon = song.id in songIds,
                                            isActive = currentMediaId == song.id,
                                            songIndex = index + 1,
                                            modifier = Modifier
                                                .clip(JetMeloShapes.small)
                                                .clickable {
                                                    mediaController?.setPlaylist(songs)
                                                    mediaController?.playMediaAtId(song.id)
                                                },
                                            trailingContent = {
                                                IconButton(onClick = {
                                                    selectedSongId = song.id
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

                        2 -> {
                            val loadState = artistAlbumList.loadState
                            when {
                                loadState.refresh is LoadState.Loading && artistAlbumList.itemCount == 0 -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                                loadState.refresh is LoadState.Error && artistAlbumList.itemCount == 0 -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Button(onClick = { artistAlbumList.retry() }) {
                                                Text(text = stringResource(R.string.operation_failed))
                                            }
                                        }
                                    }
                                }
                                loadState.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached && artistAlbumList.itemCount == 0 -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.no_brief),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    items(
                                        count = artistAlbumList.itemCount,
                                        key = { index -> index }
                                    ) { index ->
                                        artistAlbumList[index]?.let {
                                            AlbumListItem(
                                                album = it,
                                                modifier = Modifier
                                                    .clip(JetMeloShapes.small)
                                                    .clickable {
                                                        navController.navigate(
                                                            AlbumNav(
                                                                albumId = it.id,
                                                                coverImgUrl = it.picUrl,
                                                                title = it.name
                                                            )
                                                        )
                                                    }
                                            )
                                        }
                                    }
                                    if (loadState.append is LoadState.Loading) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
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

    LaunchedEffect(openBottomSheet, selectSong) {
        if (openBottomSheet && selectSong == null) openBottomSheet = false
    }

    SongMenuBottomSheet(
        navController = navController,
        song = selectSong,
        onDismiss = { openBottomSheet = false },
        openBottomSheet = openBottomSheet
    )
}
