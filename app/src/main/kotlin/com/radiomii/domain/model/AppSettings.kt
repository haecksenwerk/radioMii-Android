package com.radiomii.domain.model

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class ThemeColor { MII, DYNAMIC, CUSTOM }
enum class MusicProvider { SPOTIFY, YOUTUBE }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeColor: ThemeColor = ThemeColor.MII,
    val customSourceColorIndex: Int = 4,   // default: Teal
    val trueBlack: Boolean = false,
    val language: String = "system",
    val compactRow: Boolean = false,
    val backgroundPlayback: Boolean = false,
    val resumeAfterCall: Boolean = false,
    val findOnProvider: MusicProvider = MusicProvider.SPOTIFY,
    val showFindOnButton: Boolean = true,
    val useMusicDetection: Boolean = true,
    val showFilterBar: Boolean = true,
    val highlightUnassigned: Boolean = false,
    val searchOptions: SearchOptions = SearchOptions(),
    val scheduledNews: ScheduledNews = ScheduledNews(),
)
