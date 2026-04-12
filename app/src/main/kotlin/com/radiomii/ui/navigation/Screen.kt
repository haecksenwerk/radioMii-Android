package com.radiomii.ui.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable data object Favorites : Screen()
    @Serializable data object Search : Screen()
    @Serializable data object Settings : Screen()
    @Serializable data object AppInfo : Screen()
    @Serializable data object LegalInfo : Screen()
    @Serializable data object Licenses : Screen()
}