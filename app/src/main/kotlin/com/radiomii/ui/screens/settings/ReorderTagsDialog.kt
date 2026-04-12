package com.radiomii.ui.screens.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
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
fun ReorderTagsDialog(
    tagOrder: List<String>,
    onReorder: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var reorderList by remember(tagOrder) { mutableStateOf(tagOrder) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        reorderList = reorderList.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    AlertDialog(
        // Tapping outside / back behaves like Cancel: discard changes.
        // Only "Done" explicitly saves the new order.
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reorder_tags)) },
        text = {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(reorderList, key = { it }) { tag ->
                    ReorderableItem(reorderState, key = tag) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp, label = "drag_elevation")
                        Surface(
                            shadowElevation = elevation,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = tag,
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
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
            }) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}



