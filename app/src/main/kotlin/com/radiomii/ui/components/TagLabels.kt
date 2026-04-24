package com.radiomii.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdsClick
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radiomii.domain.model.SortOrder
import com.radiomii.domain.model.Station

@Composable
fun TagLabels(
    station: Station,
    modifier: Modifier = Modifier,
    maxTags: Int = 2,
    showVotesClicks: Boolean = true,
    activeSortOrder: SortOrder? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        // Vote count — only when showVotes = true
        if (showVotesClicks && station.votes > 0) {
            SmallChip(
                icon = { Icon(Icons.Outlined.ThumbUp, null, modifier = Modifier.size(12.dp)) },
                label = formatCount(station.votes),
                isHighlighted = activeSortOrder == SortOrder.VOTES,
            )
        }

        // Click count (clicks/24h) — shown as label with /24h suffix
        if (showVotesClicks && station.clickcount > 0) {
            SmallChip(
                icon = { Icon(Icons.Outlined.AdsClick, null, modifier = Modifier.size(12.dp)) },
                label = formatCount(station.clickcount),
                isHighlighted = activeSortOrder == SortOrder.CLICK_COUNT,
            )
        }

        // Codec + bitrate
        if (station.codec.isNotBlank() || station.bitrate > 0) {
            // radio-browser.org returns 128000 instead of 128 when bitrate is chosen as the sort criteria
            val formattedBitrate = if (station.bitrate >= 1000) station.bitrate / 1000 else station.bitrate
            val label = when {
                station.codec.isNotBlank() && station.bitrate > 0 -> "${station.codec} ${formattedBitrate}k"
                station.bitrate > 0 -> "${formattedBitrate}k"
                else -> station.codec
            }
            SmallChip(label = label, isHighlighted = activeSortOrder == SortOrder.BITRATE)
        }

        // Country code (e.g. "DE")
        if (station.countrycode.isNotBlank()) {
            SmallChip(
                label = station.countrycode.uppercase().take(2),
                isHighlighted = activeSortOrder == SortOrder.COUNTRY,
            )
        }

        // HTTPS lock
        if (station.url.startsWith("https")) {
            SmallChip(icon = { Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(15.dp)) })
        }

        // Tags
        station.tagList.take(maxTags).forEach { tag ->
            SmallChip(label = tag)
        }
    }
}

@Composable
private fun SmallChip(
    label: String = "",
    icon: (@Composable () -> Unit)? = null,
    isHighlighted: Boolean = false,
) {
    val chipColor = if (isHighlighted)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isHighlighted)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .background(chipColor, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        if (icon != null) icon()
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                maxLines = 1,
            )
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${count / 1_000}k"
    else -> count.toString()
}