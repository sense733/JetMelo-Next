package com.rcmiku.music.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rcmiku.music.R
import com.rcmiku.music.constants.userIdKey
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.playlist.PlaylistApi
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.UserPlaylistV1Response
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListBottomSheet(
    song: Song?,
    onDismiss: () -> Unit,
    openBottomSheet: Boolean,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var playlistResponse by remember { mutableStateOf<UserPlaylistV1Response?>(null) }
    val userId by rememberPreference(userIdKey, 0L)
    val selectedPlaylistId = remember { mutableStateOf<Long?>(null) }
    val selectedRemovePlaylistId = remember { mutableStateOf<Long?>(null) }
    var removeSong by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(song) {
        if (song != null) {
            AccountApi.userPlaylistV1(userId = userId, trackIds = listOf(song.id))
                .onSuccess {
                    playlistResponse = it
                }
        }
    }

    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.add_to_songList),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )

                playlistResponse?.playlist?.let { playlist ->
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlist) { item ->
                            val isSelected = selectedPlaylistId.value == item.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (item.containsTracks) {
                                            selectedRemovePlaylistId.value = item.id
                                            removeSong = true
                                        } else {
                                            selectedPlaylistId.value = if (isSelected) null else item.id
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (item.containsTracks) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        ) {
                                            Text(stringResource(R.string.collected))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        selectedPlaylistId.value = null
                                        onDismiss()
                                    },
                                ) {
                                    Text(text = stringResource(R.string.cancel))
                                }

                                Spacer(Modifier.width(12.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val selectedId = selectedPlaylistId.value
                                            val songId = song?.id
                                            if (selectedId != null && songId != null) {
                                                PlaylistApi.playlistTracksManipulate(
                                                    op = "add",
                                                    pid = selectedId,
                                                    trackIds = listOf(songId)
                                                ).onSuccess {
                                                    Toast.makeText(
                                                        context,
                                                        context.getText(R.string.add_success),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }.onFailure {
                                                    Toast.makeText(
                                                        context,
                                                        context.getText(R.string.add_fail),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                onDismiss()
                                            }
                                        }
                                    },
                                    enabled = selectedPlaylistId.value != null,
                                ) {
                                    Text(text = stringResource(R.string.confirm))
                                }
                            }

                            if (removeSong) {
                                Dialog(
                                    onConfirmation = {
                                        removeSong = false
                                        scope.launch {
                                            val selectedId = selectedRemovePlaylistId.value
                                            val songId = song?.id
                                            if (selectedId != null && songId != null) {
                                                PlaylistApi.playlistTracksManipulate(
                                                    op = "del",
                                                    pid = selectedId,
                                                    trackIds = listOf(songId)
                                                )
                                                onDismiss()
                                            }
                                        }
                                    },
                                    onDismissRequest = {
                                        removeSong = false
                                    },
                                    dialogTitle = stringResource(R.string.remove_from_songList),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}