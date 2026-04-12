package com.radiomii.ui.screens.appinfo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.radiomii.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalInfoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.legal_title)) },
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
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            LegalSection(
                title = stringResource(R.string.legal_about_title),
                body = stringResource(R.string.legal_about_text),
            )
            LegalSection(
                title = stringResource(R.string.legal_privacy_title),
                body = stringResource(R.string.legal_privacy_text),
            )
            LegalSection(
                title = stringResource(R.string.legal_third_party_title),
                body = stringResource(R.string.legal_third_party_text),
            )
            LegalSection(
                title = stringResource(R.string.legal_disclaimer_title),
                body = stringResource(R.string.legal_disclaimer_text),
            )
            LegalSection(
                title = stringResource(R.string.legal_no_warranty_title),
                body = stringResource(R.string.legal_no_warranty_text),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LegalSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
