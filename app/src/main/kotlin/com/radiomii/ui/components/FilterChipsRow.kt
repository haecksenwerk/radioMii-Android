package com.radiomii.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterChipsRow(
    filterNames: List<String>,
    activeFilterIndex: Int,
    onFilterSelected: (Int) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll so the active chip is centered — no instant jump
    LaunchedEffect(activeFilterIndex) {
        val target = if (activeFilterIndex < 0) 0 else (activeFilterIndex + 1).coerceAtMost(filterNames.size)
        // Wait until layout info is available
        snapshotFlow { listState.layoutInfo }
            .first { it.viewportSize.width > 0 && it.visibleItemsInfo.isNotEmpty() }
        val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == target }
        val viewportWidth = listState.layoutInfo.viewportSize.width
        if (itemInfo != null && viewportWidth > 0) {
            val scrollOffset = itemInfo.size / 2 - viewportWidth / 2
            listState.animateScrollToItem(target, scrollOffset)
        } else {
            // Item not yet visible: estimate centering from average visible item size
            val avgSize = listState.layoutInfo.visibleItemsInfo.map { it.size }.average().toInt()
            val estimatedOffset = if (avgSize > 0 && viewportWidth > 0) avgSize / 2 - viewportWidth / 2 else 0
            listState.animateScrollToItem(target, estimatedOffset)
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        // "All" chip at index -1
        item {
            FilterChip(
                selected = activeFilterIndex == -1,
                onClick = { onFilterSelected(-1) },
                label = { Text("All") },
                modifier = Modifier.combinedClickable(
                    onClick = { onFilterSelected(-1) },
                    onLongClick = onLongPress,
                ),
            )
        }

        itemsIndexed(filterNames) { index, name ->
            FilterChip(
                selected = activeFilterIndex == index,
                onClick = { onFilterSelected(index) },
                label = { Text(name) },
                modifier = Modifier.combinedClickable(
                    onClick = { onFilterSelected(index) },
                    onLongClick = onLongPress,
                ),
            )
        }
    }
}
