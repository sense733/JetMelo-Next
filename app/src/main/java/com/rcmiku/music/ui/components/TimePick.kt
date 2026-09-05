package com.rcmiku.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rcmiku.music.R
import com.rcmiku.music.ui.icons.Remove
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.sizeIn
import kotlinx.coroutines.isActive

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSet: (Long) -> Unit
) {
    val maxMinutes = 12 * 60
    val minMinutes = 5
    val setTimesInMinutes = rememberSaveable { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.sleep_timer)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InteractiveButton(
                        icon = Remove,
                        contentDescription = "减少",
                        onClick = {
                            if (setTimesInMinutes.intValue - 5 >= minMinutes) {
                                setTimesInMinutes.intValue -= 5
                            }
                        },
                        onLongPress = {
                            if (setTimesInMinutes.intValue - 5 >= minMinutes) {
                                setTimesInMinutes.intValue -= 5
                            }
                        }
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(16.dp)
                            .width(100.dp)
                    ) {
                        val hours = setTimesInMinutes.intValue / 60
                        val minutes = setTimesInMinutes.intValue % 60
                        Text(
                            text = "%02dh:%02dm".format(hours, minutes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    InteractiveButton(
                        icon = Icons.Default.Add,
                        contentDescription = "增加",
                        onClick = {
                            if (setTimesInMinutes.intValue + 5 <= maxMinutes) {
                                setTimesInMinutes.intValue += 5
                            }
                        },
                        onLongPress = {
                            if (setTimesInMinutes.intValue + 5 <= maxMinutes) {
                                setTimesInMinutes.intValue += 5
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onTimeSet(setTimesInMinutes.intValue.minutes.inWholeSeconds)
            }) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun InteractiveButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    longPressDelay: Duration = 500.milliseconds,
    repeatInterval: Duration = 100.milliseconds,
    onLongPress: () -> Unit = {},
    size: Dp = 56.dp,
    padding: Dp = 8.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val buttonInteracted = isPressed || isHovered

    val animateColor by animateColorAsState(
        targetValue = if (buttonInteracted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
    )

    val animateIconColor by animateColorAsState(
        targetValue = if (buttonInteracted) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(300),
    )

    val animateIconSize by animateFloatAsState(
        targetValue = if (buttonInteracted) 1.2f else 1f,
        animationSpec = tween(300)
    )

    Surface(
        shape = CircleShape,
        color = animateColor,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .size(size)
            .scale(animateIconSize)
            .clip(CircleShape)
            .pointerInput(interactionSource, onClick, onLongPress) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pressInteraction = PressInteraction.Press(down.position)
                    scope.launch { interactionSource.emit(pressInteraction) }
                    var isLongPress = false
                    val pressJob = scope.launch {
                        delay(longPressDelay)
                        isLongPress = true
                        while (isActive) {
                            onLongPress()
                            delay(repeatInterval)
                        }
                    }
                    val up = waitForUpOrCancellation()
                    pressJob.cancel()
                    if (up != null) {
                        scope.launch { interactionSource.emit(PressInteraction.Release(pressInteraction)) }
                        if (!isLongPress) {
                            onClick()
                        }
                    } else {
                        scope.launch { interactionSource.emit(PressInteraction.Cancel(pressInteraction)) }
                    }
                }
            }
            .indication(
                interactionSource = interactionSource,
                indication = ripple()
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(padding)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = animateIconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
