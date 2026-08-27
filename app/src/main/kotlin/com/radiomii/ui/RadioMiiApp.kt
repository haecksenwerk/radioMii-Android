package com.radiomii.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiomii.R
import com.radiomii.domain.error.GlobalError
import com.radiomii.ui.navigation.AppNavHost
import com.radiomii.ui.theme.RadioMiiTheme

@Composable
fun RadioMiiApp() {
    val appViewModel: AppViewModel = hiltViewModel()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()

    var currentGlobalError by remember { mutableStateOf<GlobalError?>(null) }

    LaunchedEffect(Unit) {
        appViewModel.globalErrorEvent.collect { error ->
            currentGlobalError = error
        }
    }

    // Apply per-app locale whenever the language setting changes
    LaunchedEffect(settings.language) {
        val locales = if (settings.language == "system")
            LocaleListCompat.getEmptyLocaleList()
        else
            LocaleListCompat.forLanguageTags(settings.language)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    RadioMiiTheme(settings = settings) {
        AppNavHost(appViewModel = appViewModel)

        currentGlobalError?.let { error ->
            AlertDialog(
                onDismissRequest = { currentGlobalError = null },
                title = {
                    Text(
                        stringResource(
                            when (error) {
                                GlobalError.NO_INTERNET -> R.string.dialog_no_internet_title
                                GlobalError.SERVER_UNREACHABLE -> R.string.dialog_server_unreachable_title
                                GlobalError.PLAYBACK_ERROR -> R.string.dialog_playback_error_title
                            }
                        )
                    )
                },
                text = {
                    Text(
                        stringResource(
                            when (error) {
                                GlobalError.NO_INTERNET -> R.string.dialog_no_internet_message
                                GlobalError.SERVER_UNREACHABLE -> R.string.dialog_server_unreachable_message
                                GlobalError.PLAYBACK_ERROR -> R.string.dialog_playback_error_message
                            }
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = { currentGlobalError = null }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }
    }
}
