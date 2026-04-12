package com.radiomii.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.radiomii.R

@Composable
fun CreateFilterDialog(
    existingFilters: List<String>,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val isDuplicate = existingFilters.any { it.equals(name.trim(), ignoreCase = true) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorites_create_filter_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.favorites_create_filter_placeholder)) },
                    singleLine = true,
                    isError = isDuplicate && name.isNotEmpty(),
                    supportingText = {
                        if (isDuplicate && name.isNotEmpty()) {
                            Text(stringResource(R.string.favorites_duplicate_filter))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                Text(
                    text = stringResource(R.string.favorites_create_filter_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty() && !isDuplicate) {
                        onCreate(trimmed)
                        onDismiss()
                    }
                },
                enabled = name.trim().isNotEmpty() && !isDuplicate,
            ) {
                Text(stringResource(R.string.favorites_create_filter))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
