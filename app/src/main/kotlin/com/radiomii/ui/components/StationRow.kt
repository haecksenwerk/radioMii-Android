package com.radiomii.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radiomii.domain.model.SortOrder
import com.radiomii.domain.model.Station

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchRow(
    station: Station,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showVotesClicks: Boolean = true,
    activeSortOrder: SortOrder? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onClick,
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
            ) {
                StationIcon(
                    url = station.favicon,
                    contentDescription = station.name,
                    size = 50.dp,
                    modifier = Modifier.padding(end = 12.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        TagLabels(
                            station = station,
                            showVotesClicks = showVotesClicks,
                            activeSortOrder = activeSortOrder,
                            maxTags = 0,
                        )
                    }
                }

                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteRow(
    station: Station,
    isCompact: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    isNewsStation: Boolean = false,
) {
    val rowHeight by animateDpAsState(
        targetValue = if (isCompact) 48.dp else 64.dp,
        label = "fav_row_height",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onClick,
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
            ) {
                if (isEditMode) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = dragHandleModifier.padding(end = 4.dp),
                    )
                }

                StationIcon(
                    url = station.favicon,
                    contentDescription = station.name,
                    size = if (isCompact) 44.dp else 50.dp,
                    modifier = Modifier.padding(end = 12.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    AnimatedVisibility(visible = !isCompact) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            TagLabels(station = station, showVotesClicks = false)
                        }
                    }
                }

                if (isNewsStation) {
                    Icon(
                        imageVector = Icons.Outlined.Newspaper,
                        contentDescription = "News station",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(20.dp),
                    )
                }
            }
        }
    }
}
