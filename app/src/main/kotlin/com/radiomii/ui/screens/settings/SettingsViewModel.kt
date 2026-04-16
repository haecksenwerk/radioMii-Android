package com.radiomii.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radiomii.data.prefs.FavoritesData
import com.radiomii.data.prefs.SettingsDataStore
import com.radiomii.data.repository.FavoritesRepository
import com.radiomii.data.repository.StationRepository
import com.radiomii.domain.model.AppSettings
import com.radiomii.domain.model.Country
import com.radiomii.domain.model.TagShortcutMode
import com.radiomii.domain.model.MusicProvider
import com.radiomii.domain.model.SearchOptions
import com.radiomii.domain.model.SortOrder
import com.radiomii.domain.model.Station
import com.radiomii.domain.model.ThemeColor
import com.radiomii.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

sealed class ImportExportEvent {
    data object Saved : ImportExportEvent()
    data object Loaded : ImportExportEvent()
    data object SaveError : ImportExportEvent()
    data object LoadError : ImportExportEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: SettingsDataStore,
    private val stationRepository: StationRepository,
    private val favoritesRepository: FavoritesRepository,
    private val json: Json,
) : ViewModel() {

    val settings = store.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries = _countries.asStateFlow()

    private val _countriesLoading = MutableStateFlow(false)
    val countriesLoading = _countriesLoading.asStateFlow()

    private val _importExportMessage = MutableStateFlow<ImportExportEvent?>(null)
    val importExportMessage = _importExportMessage.asStateFlow()

    fun loadCountries() {
        if (_countries.value.isNotEmpty() || _countriesLoading.value) return
        viewModelScope.launch {
            _countriesLoading.value = true
            runCatching { stationRepository.getCountries() }
                .onSuccess { _countries.value = it }
            _countriesLoading.value = false
        }
    }

    // Appearance
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }
    fun setThemeColor(color: ThemeColor) = viewModelScope.launch { store.setThemeColor(color) }
    fun setCustomColorIndex(index: Int) = viewModelScope.launch { store.setCustomColorIndex(index) }
    fun setTrueBlack(enabled: Boolean) = viewModelScope.launch { store.setTrueBlack(enabled) }
    fun setLanguage(lang: String) = viewModelScope.launch { store.setLanguage(lang) }

    // Playback
    fun setBackgroundPlayback(enabled: Boolean) = viewModelScope.launch { store.setBackgroundPlayback(enabled) }
    fun setResumeAfterCall(enabled: Boolean) = viewModelScope.launch { store.setResumeAfterCall(enabled) }
    fun setShowFindOnButton(enabled: Boolean) = viewModelScope.launch { store.setShowFindOnButton(enabled) }
    fun setFindOnProvider(provider: MusicProvider) = viewModelScope.launch { store.setFindOnProvider(provider) }

    // Favorites
    fun setHighlightUnassigned(enabled: Boolean) = viewModelScope.launch { store.setHighlightUnassigned(enabled) }
    fun clearAllFavorites() = viewModelScope.launch { favoritesRepository.clearAll(alsoFilters = true) }

    val filterNames: kotlinx.coroutines.flow.StateFlow<List<String>> = favoritesRepository.filtersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createFilter(name: String) {
        viewModelScope.launch { favoritesRepository.createFilter(name) }
    }

    fun reorderFilters(reordered: List<String>) {
        viewModelScope.launch { favoritesRepository.reorderFilters(reordered) }
    }

    fun deleteFilter(filterName: String) {
        viewModelScope.launch { favoritesRepository.deleteFilter(filterName) }
    }

    fun addFavorite(station: Station) {
        viewModelScope.launch { favoritesRepository.addFavorite(station) }
    }

    // Last backup URI, null if none.
    val lastExportUri: kotlinx.coroutines.flow.StateFlow<String?> = store.lastExportUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Delegates to SettingsDataStore.updateSearchOptions() to prevent concurrent-write races.
    fun updateSearchOptions(updater: SearchOptions.() -> SearchOptions) {
        viewModelScope.launch { store.updateSearchOptions(updater) }
    }

    fun setSearchHideBroken(enabled: Boolean) = updateSearchOptions { copy(hidebroken = enabled) }
    fun setSearchHttpsOnly(enabled: Boolean) = updateSearchOptions { copy(isHttpsOnly = enabled) }
    fun setSearchShowTagButtons(enabled: Boolean) = updateSearchOptions { copy(showTagButtons = enabled) }
    fun setSearchTagOrder(order: List<String>) = updateSearchOptions { copy(tagOrder = order) }
    fun setSearchGenreSuggestionMode(mode: TagShortcutMode) = updateSearchOptions { copy(tagShortcutMode = mode) }
    fun setSearchCountry(country: String) = updateSearchOptions { copy(country = country) }
    fun setSearchBitrateMin(bitrate: Int) = updateSearchOptions { copy(bitrateMin = bitrate) }
    fun setSearchSortOrder(order: SortOrder) = updateSearchOptions {
        val defaultReverse = order != SortOrder.COUNTRY && order != SortOrder.NAME
        copy(sortOrder = order, reverse = defaultReverse)
    }
    fun setSearchReverse(reverse: Boolean) = updateSearchOptions { copy(reverse = reverse) }

    // Writes favorites JSON to the given SAF URI.
    fun saveFavoritesToUri(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val data = favoritesRepository.getSnapshot()
                    val jsonStr = json.encodeToString(FavoritesData.serializer(), data)
                    context.contentResolver.openOutputStream(uri, "wt")
                        ?.use { it.write(jsonStr.toByteArray()) }
                        ?: error("Cannot open output stream for URI: $uri")
                    runCatching {
                        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        context.contentResolver.takePersistableUriPermission(uri, flags)
                    }
                }
            }.onSuccess {
                store.setLastExportUri(uri.toString())
                _importExportMessage.value = ImportExportEvent.Saved
            }.onFailure {
                _importExportMessage.value = ImportExportEvent.SaveError
            }
        }
    }

    // Re-saves to the last backup URI; clears it on failure (e.g. revoked permission).
    fun overwriteLastBackup() {
        viewModelScope.launch {
            val uriString = store.lastExportUriFlow.first() ?: run {
                _importExportMessage.value = ImportExportEvent.SaveError
                return@launch
            }
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = uriString.toUri()
                    val data = favoritesRepository.getSnapshot()
                    val jsonStr = json.encodeToString(FavoritesData.serializer(), data)
                    context.contentResolver.openOutputStream(uri, "wt")
                        ?.use { it.write(jsonStr.toByteArray()) }
                        ?: error("Cannot open output stream for URI: $uri")
                }
            }.onSuccess {
                _importExportMessage.value = ImportExportEvent.Saved
            }.onFailure {
                store.setLastExportUri(null)
                _importExportMessage.value = ImportExportEvent.SaveError
            }
        }
    }

    // Reads and imports favorites from the given SAF URI.
    fun loadFavoritesFromUri(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val jsonStr = context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes().decodeToString() }
                        ?: error("Cannot read file")
                    runCatching { json.decodeFromString<FavoritesData>(jsonStr) }
                        .getOrNull()
                        ?: FavoritesData(stations = json.decodeFromString<List<Station>>(jsonStr))
                }
            }.onSuccess { data ->
                favoritesRepository.replaceAll(data)
                _importExportMessage.value = ImportExportEvent.Loaded
            }.onFailure {
                _importExportMessage.value = ImportExportEvent.LoadError
            }
        }
    }

    fun clearImportExportMessage() {
        _importExportMessage.value = null
    }
}
