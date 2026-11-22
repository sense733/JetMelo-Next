package com.rcmiku.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rcmiku.music.R
import com.rcmiku.music.update.AppUpdateInfo
import com.rcmiku.music.update.UpdateState
import kotlin.math.roundToInt

/**
 * 展示应用内自更新状态、更新日志与下载进度的全局弹窗。
 */
@Composable
fun UpdateDialog(
    state: UpdateState,
    onStartDownload: (AppUpdateInfo) -> Unit,
    onInstallNow: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state) {
        is UpdateState.Available -> {
            if (!state.showDialog) return
            val info = state.info
            AlertDialog(
                onDismissRequest = { if (!info.forceUpdate) onDismiss() },
                title = { Text(stringResource(R.string.new_version_found, info.versionName)) },
                text = {
                    if (info.changelog.isNotBlank()) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = info.changelog,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onStartDownload(info) }) {
                        Text(stringResource(R.string.update_now))
                    }
                },
                dismissButton = if (!info.forceUpdate) {
                    {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.update_later))
                        }
                    }
                } else null
            )
        }

        is UpdateState.Downloading -> {
            if (!state.showDialog) return
            val progress = state.progress
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.downloading_update)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = state.info.versionName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (progress != null) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "${(progress * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.download_in_background))
                    }
                }
            )
        }

        is UpdateState.ReadyToInstall -> {
            if (!state.showDialog) return
            AlertDialog(
                onDismissRequest = { if (!state.info.forceUpdate) onDismiss() },
                title = { Text(stringResource(R.string.new_version_found, state.info.versionName)) },
                text = {
                    Text(
                        text = if (state.needsInstallPermission) {
                            stringResource(R.string.install_permission_required)
                        } else {
                            state.info.changelog.ifBlank { stringResource(R.string.install_now) }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = onInstallNow) {
                        Text(stringResource(R.string.install_now))
                    }
                },
                dismissButton = if (!state.info.forceUpdate) {
                    {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                } else null
            )
        }

        is UpdateState.Error -> {
            if (!state.showDialog) return
            val info = state.info
            val message = if (state.isDownloadError) {
                stringResource(R.string.update_download_failed, state.message)
            } else {
                stringResource(R.string.update_check_failed, state.message)
            }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.check_update)) },
                text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    if (info != null && state.isDownloadError) {
                        TextButton(onClick = { onStartDownload(info) }) {
                            Text(stringResource(R.string.retry))
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                },
                dismissButton = if (info != null && state.isDownloadError) {
                    {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                } else null
            )
        }

        else -> Unit
    }
}
