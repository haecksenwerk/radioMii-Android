package com.radiomii.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.radiomii.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mii_settings")

private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val THEME_COLOR = stringPreferencesKey("theme_color")
    val CUSTOM_COLOR_INDEX = intPreferencesKey("custom_color_index")
    val TRUE_BLACK = booleanPreferencesKey("true_black")
    val LANGUAGE = stringPreferencesKey("language")
    val COMPACT_ROW = booleanPreferencesKey("compact_row")
    val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
    val RESUME_AFTER_CALL = booleanPreferencesKey("resume_after_call")
    val FIND_ON_PROVIDER = stringPreferencesKey("find_on_provider")
    val SHOW_FIND_ON_BUTTON = booleanPreferencesKey("show_find_on_button")
    val SHOW_FILTER_BAR = booleanPreferencesKey("show_filter_bar")
    val HIGHLIGHT_UNASSIGNED = booleanPreferencesKey("highlight_unassigned")
    val SEARCH_HINT_SHOWN = booleanPreferencesKey("search_hint_shown")
    val FAVORITES_HINT_SHOWN = booleanPreferencesKey("favorites_hint_shown")
    val SEARCH_OPTIONS_JSON = stringPreferencesKey("search_options_json")
    val ACTIVE_STATION_JSON = stringPreferencesKey("active_station_json")
    val SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
    val SLEEP_TIMER_CANCEL_ON_STOP = booleanPreferencesKey("sleep_timer_cancel_on_stop")
    val SCHEDULED_NEWS_JSON = stringPreferencesKey("scheduled_news_json")
    val LAST_EXPORT_URI = stringPreferencesKey("last_export_uri")
}

@kotlinx.serialization.Serializable
private data class SerializableSearchOptions(
    val searchMode: String = "TAG",
    val country: String = "",
    val sortOrder: String = "clickcount",
    val bitrateMin: Int = 96,
    val reverse: Boolean = true,
    val hidebroken: Boolean = true,
    val isHttpsOnly: Boolean = false,
    val showTagButtons: Boolean = true,
    val tagOrder: List<String> = emptyList(),
    val tagShortcutMode: String = "CARDS",
)


private fun SerializableSearchOptions.toDomain() = SearchOptions(
    searchMode = SearchMode.valueOf(searchMode),
    country = country,
    sortOrder = SortOrder.entries.firstOrNull { it.apiValue == sortOrder } ?: SortOrder.CLICK_COUNT,
    bitrateMin = bitrateMin,
    reverse = reverse,
    hidebroken = hidebroken,
    isHttpsOnly = isHttpsOnly,
    showTagButtons = showTagButtons,
    tagOrder = tagOrder,
    tagShortcutMode = runCatching { TagShortcutMode.valueOf(tagShortcutMode) }
        .getOrDefault(TagShortcutMode.CARDS),
)

private fun SearchOptions.toSerializable() = SerializableSearchOptions(
    searchMode = searchMode.name,
    country = country,
    sortOrder = sortOrder.apiValue,
    bitrateMin = bitrateMin,
    reverse = reverse,
    hidebroken = hidebroken,
    isHttpsOnly = isHttpsOnly,
    showTagButtons = showTagButtons,
    tagOrder = tagOrder,
    tagShortcutMode = tagShortcutMode.name,
)


@Singleton
class SettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) {
    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val searchOptsJson = prefs[Keys.SEARCH_OPTIONS_JSON]
        val searchOpts = if (searchOptsJson != null) {
            runCatching { json.decodeFromString<SerializableSearchOptions>(searchOptsJson) }
                .getOrDefault(SerializableSearchOptions())
        } else SerializableSearchOptions()

        AppSettings(
            themeMode = ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name),
            themeColor = ThemeColor.valueOf(prefs[Keys.THEME_COLOR] ?: ThemeColor.MII.name),
            customSourceColorIndex = prefs[Keys.CUSTOM_COLOR_INDEX] ?: 4,
            trueBlack = prefs[Keys.TRUE_BLACK] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "system",
            compactRow = prefs[Keys.COMPACT_ROW] ?: false,
            backgroundPlayback = prefs[Keys.BACKGROUND_PLAYBACK] ?: false,
            resumeAfterCall = prefs[Keys.RESUME_AFTER_CALL] ?: false,
            findOnProvider = MusicProvider.valueOf(prefs[Keys.FIND_ON_PROVIDER] ?: MusicProvider.SPOTIFY.name),
            showFindOnButton = prefs[Keys.SHOW_FIND_ON_BUTTON] ?: false,
            showFilterBar = prefs[Keys.SHOW_FILTER_BAR] ?: true,
            highlightUnassigned = prefs[Keys.HIGHLIGHT_UNASSIGNED] ?: false,
            searchHintShown = prefs[Keys.SEARCH_HINT_SHOWN] ?: false,
            favoritesHintShown = prefs[Keys.FAVORITES_HINT_SHOWN] ?: false,
            searchOptions = searchOpts.toDomain(),
            scheduledNews = prefs[Keys.SCHEDULED_NEWS_JSON]?.let {
                runCatching { json.decodeFromString<ScheduledNews>(it) }.getOrDefault(ScheduledNews())
            } ?: ScheduledNews(),
        )
    }

    val activeStationFlow: Flow<Station?> = context.dataStore.data.map { prefs ->
        val stationJson = prefs[Keys.ACTIVE_STATION_JSON] ?: return@map null
        runCatching { json.decodeFromString<Station>(stationJson) }.getOrNull()
    }

    val sleepTimerMinutesFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.SLEEP_TIMER_MINUTES] ?: 30
    }

    val sleepTimerCancelOnStopFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SLEEP_TIMER_CANCEL_ON_STOP] ?: false
    }

    // Last exported favorites file URI, null if none.
    val lastExportUriFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_EXPORT_URI]
    }


    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setThemeColor(color: ThemeColor) = context.dataStore.edit { it[Keys.THEME_COLOR] = color.name }
    suspend fun setCustomColorIndex(index: Int) = context.dataStore.edit { it[Keys.CUSTOM_COLOR_INDEX] = index }
    suspend fun setTrueBlack(enabled: Boolean) = context.dataStore.edit { it[Keys.TRUE_BLACK] = enabled }
    suspend fun setLanguage(lang: String) = context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    suspend fun setCompactRow(compact: Boolean) = context.dataStore.edit { it[Keys.COMPACT_ROW] = compact }
    suspend fun setBackgroundPlayback(enabled: Boolean) = context.dataStore.edit { it[Keys.BACKGROUND_PLAYBACK] = enabled }
    suspend fun setResumeAfterCall(enabled: Boolean) = context.dataStore.edit { it[Keys.RESUME_AFTER_CALL] = enabled }
    suspend fun setFindOnProvider(provider: MusicProvider) = context.dataStore.edit { it[Keys.FIND_ON_PROVIDER] = provider.name }
    suspend fun setShowFindOnButton(show: Boolean) = context.dataStore.edit { it[Keys.SHOW_FIND_ON_BUTTON] = show }
    suspend fun setShowFilterBar(show: Boolean) = context.dataStore.edit { it[Keys.SHOW_FILTER_BAR] = show }
    suspend fun setHighlightUnassigned(enabled: Boolean) = context.dataStore.edit { it[Keys.HIGHLIGHT_UNASSIGNED] = enabled }
    suspend fun setSearchHintShown(shown: Boolean) = context.dataStore.edit { it[Keys.SEARCH_HINT_SHOWN] = shown }
    suspend fun setFavoritesHintShown(shown: Boolean) = context.dataStore.edit { it[Keys.FAVORITES_HINT_SHOWN] = shown }
    suspend fun setSleepTimerMinutes(minutes: Int) = context.dataStore.edit { it[Keys.SLEEP_TIMER_MINUTES] = minutes }
    suspend fun setSleepTimerCancelOnStop(enabled: Boolean) = context.dataStore.edit { it[Keys.SLEEP_TIMER_CANCEL_ON_STOP] = enabled }

    // Reads, updates, and writes SearchOptions atomically to avoid concurrent-write races.
    suspend fun updateSearchOptions(updater: SearchOptions.() -> SearchOptions) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SEARCH_OPTIONS_JSON]
                ?.let { runCatching { json.decodeFromString<SerializableSearchOptions>(it) }.getOrDefault(SerializableSearchOptions()) }
                ?: SerializableSearchOptions()
            prefs[Keys.SEARCH_OPTIONS_JSON] = json.encodeToString(
                SerializableSearchOptions.serializer(),
                current.toDomain().updater().toSerializable(),
            )
        }
    }

    suspend fun setSearchOptions(opts: SearchOptions) {
        context.dataStore.edit {
            it[Keys.SEARCH_OPTIONS_JSON] = json.encodeToString(
                SerializableSearchOptions.serializer(),
                opts.toSerializable(),
            )
        }
    }

    suspend fun setActiveStation(station: Station?) {
        context.dataStore.edit {
            if (station != null) it[Keys.ACTIVE_STATION_JSON] = json.encodeToString(Station.serializer(), station)
            else it.remove(Keys.ACTIVE_STATION_JSON)
        }
    }

    suspend fun setScheduledNews(news: ScheduledNews) {
        context.dataStore.edit { it[Keys.SCHEDULED_NEWS_JSON] = json.encodeToString(ScheduledNews.serializer(), news) }
    }

    suspend fun setLastExportUri(uriString: String?) {
        context.dataStore.edit {
            if (uriString != null) it[Keys.LAST_EXPORT_URI] = uriString
            else it.remove(Keys.LAST_EXPORT_URI)
        }
    }
}
