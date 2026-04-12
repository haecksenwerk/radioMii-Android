package com.radiomii.ui.screens.appinfo

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.radiomii.R
import org.json.JSONArray

private data class LicenseEntry(val name: String, val license: String)

private fun loadLicenses(context: Context): List<LicenseEntry> {
    return runCatching {
        val json = context.assets.open("licenses.json").bufferedReader().readText()
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            LicenseEntry(name = obj.getString("name"), license = obj.getString("license"))
        }
    }.getOrDefault(emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val licenses = remember { loadLicenses(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_info_licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
        modifier = modifier,
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            items(licenses) { entry ->
                ListItem(
                    headlineContent = { Text(entry.name) },
                    trailingContent = {
                        Text(
                            text = entry.license,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
