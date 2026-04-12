package com.radiomii.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.radiomii.ui.components.AppBottomBar
import com.radiomii.ui.components.NowPlayingBar
import com.radiomii.ui.screens.appinfo.AppInfoScreen
import com.radiomii.ui.screens.appinfo.LegalInfoScreen
import com.radiomii.ui.screens.appinfo.LicensesScreen
import com.radiomii.ui.screens.favorites.FavoritesScreen
import com.radiomii.ui.screens.nowplaying.NowPlayingSheet
import com.radiomii.ui.screens.search.SearchScreen
import com.radiomii.ui.screens.settings.SettingsScreen
import com.radiomii.ui.AppViewModel

@Composable
fun AppNavHost(appViewModel: AppViewModel) {
    val hasFavorites by appViewModel.hasFavorites.collectAsStateWithLifecycle()

    if (hasFavorites == null) {
        // Show a centred spinner while DataStore resolves on first launch instead of
        // a blank screen.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentEntry?.destination

    val activeStation by appViewModel.activeStation.collectAsStateWithLifecycle()
    val isPlaying by appViewModel.isPlaying.collectAsStateWithLifecycle()
    val isLoading by appViewModel.isLoading.collectAsStateWithLifecycle()
    val metadata by appViewModel.metadata.collectAsStateWithLifecycle()
    val sleepTimer by appViewModel.sleepTimer.collectAsStateWithLifecycle()
    val isPlayingNews by appViewModel.isPlayingNews.collectAsStateWithLifecycle()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()

    var showNowPlaying by remember { mutableStateOf(false) }

    val showNowPlayingBar = activeStation != null &&
        currentDestination?.hasRoute(Screen.Settings::class) != true &&
        currentDestination?.hasRoute(Screen.AppInfo::class) != true &&
        currentDestination?.hasRoute(Screen.LegalInfo::class) != true &&
        currentDestination?.hasRoute(Screen.Licenses::class) != true

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = showNowPlayingBar,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    NowPlayingBar(
                        station = activeStation,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        metadata = metadata,
                        sleepTimerState = sleepTimer,
                        showSkipNews = isPlayingNews && settings.scheduledNews.skipWhenPaused,
                        onSkipNews = appViewModel::skipCurrentNews,
                        onTogglePlayPause = appViewModel::togglePlayPause,
                        onTap = { showNowPlaying = true },
                    )
                }
                AppBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { route ->
                        // If navigating away while a Settings sub-screen is active,
                        // pop back to Settings first so the sub-stack is not saved/restored.
                        val isOnSettingsSubScreen =
                            currentDestination?.hasRoute(Screen.AppInfo::class) == true ||
                            currentDestination?.hasRoute(Screen.LegalInfo::class) == true ||
                            currentDestination?.hasRoute(Screen.Licenses::class) == true
                        if (isOnSettingsSubScreen) {
                            navController.popBackStack(Screen.Settings, inclusive = false)
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = if (hasFavorites == true) Screen.Favorites else Screen.Search,
            ) {
                composable<Screen.Search> {
                    SearchScreen(
                        appViewModel = appViewModel,
                        onStationClick = {},
                    )
                }
                composable<Screen.Favorites> {
                    FavoritesScreen(
                        appViewModel = appViewModel,
                    )
                }
                composable<Screen.Settings> {
                    SettingsScreen(
                        onNavigateToAppInfo = { navController.navigate(Screen.AppInfo) },
                    )
                }
                composable<Screen.AppInfo>(
                    enterTransition = {
                        slideIntoContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right
                        )
                    },
                ) {
                    AppInfoScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToLegal = { navController.navigate(Screen.LegalInfo) },
                        onNavigateToLicenses = { navController.navigate(Screen.Licenses) },
                    )
                }
                composable<Screen.LegalInfo>(
                    enterTransition = {
                        slideIntoContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right
                        )
                    },
                ) {
                    LegalInfoScreen(onBack = { navController.popBackStack() })
                }
                composable<Screen.Licenses>(
                    enterTransition = {
                        slideIntoContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right
                        )
                    },
                ) {
                    LicensesScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }

        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            NowPlayingSheet(
                appViewModel = appViewModel,
                onDismiss = { showNowPlaying = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
