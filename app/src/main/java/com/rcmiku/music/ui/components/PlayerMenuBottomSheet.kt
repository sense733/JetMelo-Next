package com.rcmiku.music.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.ui.icons.SongListAdd
import com.rcmiku.music.ui.icons.Timelapse
import com.rcmiku.music.ui.icons.Timer
import com.rcmiku.ncmapi.model.Song
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMenuBottomSheet(
    currentSong: Song? = null,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playerState = LocalPlayerState.current
    val isSleepTimerSet = playerState?.isSleepTimerSet == true
    val context = LocalContext.current
    var activeDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var openSongListBottomSheet by rememberSaveable { mutableStateOf(false) }

    val remainingTimeText by remember(playerState) {
        derivedStateOf {
            val seconds = playerState?.remainingTime ?: return@derivedStateOf null
            seconds.toDuration(DurationUnit.SECONDS).toComponents { hours, minutes, secs, _ ->
                if (hours > 0) {
                    "%02d:%02d:%02d".format(hours, minutes, secs)
                } else {
                    "%02d:%02d".format(minutes, secs)
                }
            }
        }
    }

    LaunchedEffect(openBottomSheet) {
        runCatching {
            if (openBottomSheet) {
                if (!bottomSheetState.isVisible) {
                    bottomSheetState.show()
                }
            } else {
                if (bottomSheetState.isVisible) {
                    bottomSheetState.hide()
                }
            }
        }
    }

    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
        ) {
            LazyColumn(
                Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 8.dp,
                            bottomEnd = 8.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        activeDialog = if (isSleepTimerSet) "CANCEL" else "TIME_PICKER"
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSleepTimerSet) Timelapse else Timer,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(if (isSleepTimerSet) R.string.remaining_time else R.string.sleep_timer),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (isSleepTimerSet) {
                                remainingTimeText?.let { timeStr ->
                                    Text(
                                        text = " $timeStr",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        openSongListBottomSheet = true
                                        onDismiss()
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = SongListAdd,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.add_to_songList),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 8.dp,
                            topEnd = 8.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        currentSong?.id?.let {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "https://music.163.com/#/song?id=${it}"
                                                )
                                            }
                                            context.startActivity(
                                                Intent.createChooser(
                                                    shareIntent,
                                                    context.getString(R.string.share_link)
                                                )
                                            )
                                        }
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.share),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(12.dp))
                }
            }

            when (activeDialog) {
                "TIME_PICKER" -> {
                    TimePickerDialog(
                        onDismiss = {
                            activeDialog = null
                        },
                        onTimeSet = {
                            playerState?.startTimer(it)
                            activeDialog = null
                        }
                    )
                }
                "CANCEL" -> {
                    Dialog(
                        onConfirmation = {
                            playerState?.cancelTimer()
                            activeDialog = null
                        },
                        onDismissRequest = {
                            activeDialog = null
                        },
                        dialogTitle = stringResource(R.string.sleep_timer_cancel),
                    )
                }
            }
        }
    }

    SongListBottomSheet(
        song = currentSong,
        onDismiss = {
            openSongListBottomSheet = false
        },
        openBottomSheet = openSongListBottomSheet
    )
}