package com.radiomii.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.radiomii.R
import com.radiomii.domain.model.Station
import com.radiomii.ui.components.TagLabels

@Composable
fun AssignFilterDialog(
    station: Station,
    filterNames: List<String>,
    assignedFilters: List<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorites_assign_filter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Station info header
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                TagLabels(station = station, maxTags = 3)
                HorizontalDivider()

                if (filterNames.isEmpty()) {
                    Text(
                        text = stringResource(R.string.favorites_no_filters),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filterNames) { name ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Checkbox(
                                    checked = name in assignedFilters,
                                    onCheckedChange = { onToggle(name) },
                                )
                                Text(text = name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
        },
    )
}
