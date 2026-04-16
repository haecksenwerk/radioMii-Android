package com.radiomii.ui.screens.nowplaying

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radiomii.R
import com.radiomii.domain.model.SleepTimerState

private val DURATION_OPTIONS = listOf(5, 15, 30, 60)

@Composable
fun SleepTimerDialog(
    timerState: SleepTimerState,
    onStart: (minutes: Int, cancelOnStop: Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedMinutes by remember(timerState.selectedMinutes) {
        mutableIntStateOf(timerState.selectedMinutes)
    }
    var cancelOnStop by remember(timerState.cancelOnStop) {
        mutableStateOf(timerState.cancelOnStop)
    }
    // Local active state so pressing "Stop" keeps dialog open showing "Start Timer"
    var localIsActive by remember(timerState.isActive) { mutableStateOf(timerState.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_timer_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.sleep_timer_duration),
                    style = MaterialTheme.typography.bodyMedium,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DURATION_OPTIONS.forEachIndexed { index, minutes ->
                        SegmentedButton(
                            selected = selectedMinutes == minutes,
                            onClick = {
                                selectedMinutes = minutes
                                if (localIsActive) {
                                    onStart(minutes, cancelOnStop)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = DURATION_OPTIONS.size),
                            icon = {},
                        ) {
                            Text("$minutes", maxLines = 1)
                        }
                    }
                }

                // Remaining time info when active
                if (localIsActive) {
                    Text(
                        text = stringResource(R.string.sleep_timer_remaining, timerState.remainingMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }

                HorizontalDivider()

                // Auto-cancel option
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = cancelOnStop,
                        onCheckedChange = {
                            cancelOnStop = it
                            if (localIsActive) onStart(selectedMinutes, it)
                        },
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.sleep_timer_cancel_on_stop),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.sleep_timer_cancel_on_stop_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (localIsActive) {
                        onReset()
                        localIsActive = false
                    } else {
                        onStart(selectedMinutes, cancelOnStop)
                        onDismiss()
                    }
                },
            ) {
                Text(
                    if (localIsActive)
                        stringResource(R.string.sleep_timer_stop)
                    else
                        stringResource(R.string.sleep_timer_start)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
