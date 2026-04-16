package com.radiomii.ui.screens.favorites

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
import com.radiomii.ui.components.TagLabels

private const val RECOMMENDED_NAME_LENGTH = 35

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
        title = { Text(stringResource(R.string.favorites_assign_filter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Station info header with editable name
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
                                textStyle = MaterialTheme.typography.titleSmall,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { commitName() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
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
                                )
                            }
                        }
                    } else {
                        Text(
                            text = displayedName,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
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
                        )
                    }
                }

                TagLabels(station = station, maxTags = 3)
                Spacer(modifier = Modifier.height(8.dp))
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

    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }
}
