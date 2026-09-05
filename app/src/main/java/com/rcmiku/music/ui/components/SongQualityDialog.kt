package com.rcmiku.music.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rcmiku.music.R
import com.rcmiku.ncmapi.api.player.SongLevel

@Composable
fun SongQualityDialog(
    currentLevel: SongLevel,
    onDismiss: () -> Unit,
    onQualitySelected: (SongLevel) -> Unit,
    availableLevels: Set<SongLevel>? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .padding(vertical = 24.dp)
            ) {
                SongLevel.entries.forEach { level ->
                    val isAvailable = availableLevels?.contains(level) ?: true
                    val isSelected = level == currentLevel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .selectable(
                                selected = isSelected,
                                enabled = isAvailable,
                                onClick = {
                                    if (isAvailable) {
                                        onQualitySelected(level)
                                        onDismiss()
                                    }
                                },
                                role = Role.RadioButton,
                            )
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            enabled = isAvailable,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val title = when (level) {
                                SongLevel.STANDARD -> stringResource(R.string.standard)
                                SongLevel.HIGHER -> stringResource(R.string.higer)
                                SongLevel.EXHIGH -> stringResource(R.string.exhigh)
                                SongLevel.LOSSLESS -> stringResource(R.string.lossless)
                                SongLevel.HIRES -> stringResource(R.string.hi_res)
                                SongLevel.SKY -> stringResource(R.string.sky)
                            }
                            val subtitle = if (!isAvailable) {
                                "当前音源或账号暂不可用"
                            } else {
                                when (level) {
                                    SongLevel.STANDARD -> "128kbps"
                                    SongLevel.HIGHER -> "192kbps"
                                    SongLevel.EXHIGH -> "320kbps"
                                    SongLevel.LOSSLESS -> "FLAC 16bit / 44.1kHz"
                                    SongLevel.HIRES -> "高解析 24bit / 96kHz+"
                                    SongLevel.SKY -> "沉浸环绕声"
                                }
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
        }
    }
}
