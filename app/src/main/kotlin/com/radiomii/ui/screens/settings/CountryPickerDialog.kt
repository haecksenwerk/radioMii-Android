package com.radiomii.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.radiomii.R
import com.radiomii.domain.model.Country

/** Converts an ISO 3166-1 alpha-2 country code (e.g. "DE") to its emoji flag. */
private fun String.toFlagEmoji(): String {
    if (length != 2) return "🌍"
    val first  = this[0].uppercaseChar() - 'A' + 0x1F1E6
    val second = this[1].uppercaseChar() - 'A' + 0x1F1E6
    return String(intArrayOf(first, second), 0, 2)
}

@Composable
fun CountryPickerDialog(
    countries: List<Country>,
    selectedCountry: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var sortAlphabetical by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    val filteredCountries = remember(countries, searchQuery) {
        if (searchQuery.isBlank()) countries else countries.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val sortedCountries = remember(filteredCountries, sortAlphabetical) {
        if (sortAlphabetical) {
            filteredCountries.sortedBy { it.name }
        } else {
            filteredCountries.sortedByDescending { it.stationcount }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.9f),
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.search_country),
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        IconButton(onClick = {
                            searchActive = !searchActive
                            if (!searchActive) searchQuery = ""
                        }) {
                            Icon(
                                imageVector = if (searchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = { sortAlphabetical = !sortAlphabetical }) {
                            if (sortAlphabetical) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort numeric")
                            } else {
                                Icon(imageVector = Icons.Default.SortByAlpha, contentDescription = "Sort alphabetic")
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = searchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(16.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(0.8f)
            ) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.search_country_any)) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { onSelect("") },
                        trailingContent = if (selectedCountry.isBlank()) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else null
                    )
                    HorizontalDivider()
                }
                items(sortedCountries) { country ->
                    ListItem(
                        headlineContent = { Text(country.name) },
                        supportingContent = { Text("${country.stationcount} Stations") },
                        leadingContent = {
                            Text(
                                text = country.iso31661.toFlagEmoji(),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { onSelect(country.iso31661) },
                        trailingContent = if (selectedCountry.equals(country.iso31661, ignoreCase = true)) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else null
                    )
                }
            }
        }
    )
}


