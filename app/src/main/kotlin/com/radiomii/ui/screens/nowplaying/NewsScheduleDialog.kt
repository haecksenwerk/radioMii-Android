package com.radiomii.ui.screens.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radiomii.R
import com.radiomii.domain.model.NewsInterval
import com.radiomii.domain.model.ScheduledNews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScheduleDialog(
    scheduledNews: ScheduledNews,
    onSave: (ScheduledNews) -> Unit,
    onDismiss: () -> Unit,
) {
    // Local state — confirmed on Save
    var enabled by remember(scheduledNews.enabled) { mutableStateOf(scheduledNews.enabled) }
    var interval by remember(scheduledNews.interval) { mutableStateOf(scheduledNews.intervalEnum) }
    var durationMinutes by remember(scheduledNews.durationMinutes) { mutableIntStateOf(scheduledNews.durationMinutes) }
    var showSkipButton by remember(scheduledNews.showSkipButton) { mutableStateOf(scheduledNews.showSkipButton) }
    val stationId = scheduledNews.stationId
    val stationName = scheduledNews.stationName

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scheduled_news_title)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Enable toggle
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { enabled = !enabled }
                            .padding(vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.scheduled_news_enable),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }

                if (enabled) {
                    // Interval
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.scheduled_news_interval),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = interval == NewsInterval.HOURLY,
                                    onClick = { interval = NewsInterval.HOURLY },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    icon = {},
                                    label = { Text(stringResource(R.string.scheduled_news_hourly)) },
                                )
                                SegmentedButton(
                                    selected = interval == NewsInterval.HALF_HOURLY,
                                    onClick = { interval = NewsInterval.HALF_HOURLY },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    icon = {},
                                    label = { Text(stringResource(R.string.scheduled_news_half_hourly)) },
                                )
                            }
                        }
                    }

                    // Duration
                    item {
                        val durations = remember { listOf(0, 5, 10) }
                        Column {
                            Text(
                                text = stringResource(R.string.scheduled_news_duration),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                durations.forEachIndexed { index, dur ->
                                    SegmentedButton(
                                        selected = durationMinutes == dur,
                                        onClick = {
                                            durationMinutes = dur
                                            if (dur == 0) showSkipButton = false
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = durations.size),
                                        icon = {},
                                        label = {
                                            Text(
                                                if (dur == 0) stringResource(R.string.scheduled_news_off)
                                                else stringResource(R.string.scheduled_news_minutes, dur),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Skip when paused
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = durationMinutes > 0) {
                                                                        if (durationMinutes > 0) showSkipButton = !showSkipButton
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.scheduled_news_skip),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (durationMinutes > 0)
                                        MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outline,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.scheduled_news_skip_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = showSkipButton,
                                onCheckedChange = { if (durationMinutes > 0) showSkipButton = it },
                                enabled = durationMinutes > 0,
                            )
                        }
                    }

                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        ScheduledNews(
                            enabled = enabled,
                            stationId = stationId,
                            stationName = stationName,
                            interval = interval.name,
                            durationMinutes = durationMinutes,
                            showSkipButton = showSkipButton,
                        ),
                    )
                },
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )

}
