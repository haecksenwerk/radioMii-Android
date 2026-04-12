package com.radiomii.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radiomii.domain.model.SleepTimerState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SleepTimerButton(
    state: SleepTimerState,
    onShortPress: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onShortPress,
                onLongClick = onLongPress,
            ),
    ) {
        if (state.isActive) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = 3.dp.toPx()
                val inset = strokeWidth / 2
                // Track ring
                drawArc(
                    color = surfaceVariant,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = this.size.copy(
                        width = this.size.width - strokeWidth,
                        height = this.size.height - strokeWidth,
                    ),
                )
                // Progress arc (shrinks as timer counts down)
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = 360f * state.progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = this.size.copy(
                        width = this.size.width - strokeWidth,
                        height = this.size.height - strokeWidth,
                    ),
                )
            }

            val remaining = state.remainingMinutes
            Text(
                text = if (remaining >= 1) "$remaining" else "<1",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = primary,
            )
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bedtime,
                    contentDescription = "Sleep timer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size * 0.5f),
                )
            }
        }
    }
}
