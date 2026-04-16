package com.radiomii.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radiomii.domain.model.IcyMetadata
import com.radiomii.domain.model.SleepTimerState
import com.radiomii.domain.model.Station

@Composable
fun NowPlayingBar(
    station: Station?,
    isPlaying: Boolean,
    isLoading: Boolean,
    metadata: IcyMetadata?,
    onTogglePlayPause: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    sleepTimerState: SleepTimerState? = null,
    showSkipNews: Boolean = false,
    onSkipNews: () -> Unit = {},
) {
    if (station == null) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onTap() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            StationIcon(
                url = station.favicon,
                contentDescription = station.displayName,
                modifier = Modifier
                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 12.dp)
                    .size(44.dp),
            )

            // ICY subtitle is only displayed (and animated) while actively playing
            val subtitle = if (isPlaying) metadata?.display?.takeIf { it.isNotEmpty() } else null
            if (subtitle != null) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = station.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                    )
                }
            } else {
                Text(
                    text = station.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                // Wrap play/stop button in a Box so the sleep-timer arc can be
                // drawn as a Canvas overlay without affecting the click target.
                Box(contentAlignment = Alignment.Center) {
                    val activeTimer = sleepTimerState?.takeIf { it.isActive }
                    if (activeTimer != null) {
                        val primary = MaterialTheme.colorScheme.primary
                        val track = MaterialTheme.colorScheme.surfaceVariant
                        Canvas(modifier = Modifier.size(48.dp)) {
                            val strokeWidth = 2.5.dp.toPx()
                            val inset = strokeWidth / 2
                            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                            val topLeft = Offset(inset, inset)
                            // Track ring
                            drawArc(
                                color = track,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                topLeft = topLeft,
                                size = arcSize,
                            )
                            // Progress arc — shrinks as timer counts down
                            drawArc(
                                color = primary,
                                startAngle = -90f,
                                sweepAngle = 360f * activeTimer.progress,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                topLeft = topLeft,
                                size = arcSize,
                            )
                        }
                    }
                    IconButton(onClick = { if (showSkipNews) onSkipNews() else onTogglePlayPause() }) {
                        Icon(
                            imageVector = when {
                                showSkipNews -> Icons.Default.SkipNext
                                isPlaying -> Icons.Default.Stop
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = when {
                                showSkipNews -> "Skip news"
                                isPlaying -> "Stop"
                                else -> "Play"
                            },
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
