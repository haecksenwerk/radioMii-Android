package com.radiomii.ui.screens.favorites

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.radiomii.R
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ReorderFiltersDialog(
    filterNames: List<String>,
    onReorder: (List<String>) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reorderList by remember(filterNames) { mutableStateOf(filterNames) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        reorderList = reorderList.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    AlertDialog(
        onDismissRequest = {
            onReorder(reorderList)
            onDismiss()
        },
        title = { Text(stringResource(R.string.favorites_reorder_filters)) },
        text = {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(reorderList, key = { it }) { name ->
                    ReorderableItem(reorderState, key = name) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp, label = "drag_elevation")
                        Surface(
                            shadowElevation = elevation,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                            ) {
                                IconButton(
                                    onClick = {
                                        onDelete(name)
                                        reorderList = reorderList.toMutableList().apply { remove(name) }
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete filter",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(),
                                ) {
                                    Icon(Icons.Default.DragHandle, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onReorder(reorderList)
                onDismiss()
            }) {
                Text(stringResource(R.string.common_done))
            }
        },
    )
}
