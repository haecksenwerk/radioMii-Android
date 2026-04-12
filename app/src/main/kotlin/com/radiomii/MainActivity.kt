package com.radiomii

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.radiomii.domain.model.StartupState
import com.radiomii.ui.RadioMiiApp
import com.radiomii.ui.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var isAppReady = false
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appViewModel.startupState.collect { state ->
                    val ready = state != StartupState.INITIALIZING && state != StartupState.LOADING
                    if (ready && !isAppReady) {
                        isAppReady = true
                        reportFullyDrawn()
                    }
                }
            }
        }

        splashScreen.setKeepOnScreenCondition { !isAppReady }

        enableEdgeToEdge()
        setContent {
            RadioMiiApp()
        }
    }
}
