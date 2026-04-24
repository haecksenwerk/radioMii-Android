package com.radiomii.ui.screens.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.radiomii.R
import com.radiomii.domain.model.Station

private const val RECOMMENDED_NAME_LENGTH = 30

@Composable
fun AssignFilterDialog(
    station: Station,
    filterNames: List<String>,
    assignedFilters: List<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onRenameStation: (String?) -> Unit = {},
) {
    var isEditing by remember { mutableStateOf(false) }
    // Keeps the committed display name in sync within the dialog lifetime,
    // independent of whether the parent has recomposed with the updated Station yet.
    var displayedName by remember { mutableStateOf(station.displayName) }
    var nameInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = station.displayName,
                selection = TextRange(station.displayName.length),
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    fun commitName() {
        val trimmed = nameInput.text.trim()
        val resolved = trimmed.ifBlank { station.name }  // blank = reset to original
        displayedName = resolved
        onRenameStation(trimmed.ifBlank { null })
        isEditing = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.favorites_edit_favorite),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (station.tagList.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        station.tagList.take(10).forEach { tag ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val currentName = if (isEditing) nameInput.text else displayedName
                    val isCustom = currentName.trim().run { isNotEmpty() && this != station.name }
                    val nameLabelRes = if (isCustom) {
                        R.string.favorites_custom_station_name
                    } else {
                        R.string.favorites_station_name
                    }
                    Text(
                        text = stringResource(nameLabelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isEditing) {
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { commitName() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    shape = MaterialTheme.shapes.medium
                                )
                                if (nameInput.text.length > RECOMMENDED_NAME_LENGTH) {
                                    Text(
                                        text = stringResource(
                                            R.string.favorites_station_name_length_hint,
                                            nameInput.text.length,
                                            RECOMMENDED_NAME_LENGTH,
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = displayedName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isEditing) {
                                    commitName()
                                } else {
                                    nameInput = TextFieldValue(
                                        text = displayedName,
                                        selection = TextRange(displayedName.length),
                                    )
                                    isEditing = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.favorites_assign_filter),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    if (filterNames.isEmpty()) {
                        Text(
                            text = stringResource(R.string.favorites_no_filters),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filterNames) { name ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggle(name) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                ) {
                                    Checkbox(
                                        checked = name in assignedFilters,
                                        onCheckedChange = null,
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
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

    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }
}
