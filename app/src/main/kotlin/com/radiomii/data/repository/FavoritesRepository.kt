package com.radiomii.data.repository

import com.radiomii.data.prefs.FavoritesData
import com.radiomii.data.prefs.FavoritesDataStore
import com.radiomii.domain.model.Station
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val store: FavoritesDataStore,
) {
    val favoritesFlow: Flow<List<Station>> = store.favoritesFlow
    val filtersFlow: Flow<List<String>> = store.filtersFlow
    val filterMapFlow: Flow<Map<String, List<String>>> = store.filterMapFlow

    /** Returns a single snapshot of all favorites data (for backup export). */
    suspend fun getSnapshot(): FavoritesData = store.getSnapshot()

    // Returns false if the favorites limit is reached.
    suspend fun addFavorite(station: Station): Boolean = store.addFavorite(station)

    suspend fun removeFavorite(uuid: String) = store.removeFavorite(uuid)

    suspend fun reorderFavorites(stations: List<Station>) = store.reorderFavorites(stations)

    suspend fun updateFavicon(uuid: String, faviconUrl: String) =
        store.updateFavicon(uuid, faviconUrl)

    /** Persists a user-defined display name. Null or blank resets to the original name. */
    suspend fun updateStationName(uuid: String, customName: String?) =
        store.updateStationName(uuid, customName)

    // Returns false if the filter limit is reached or name already exists.
    suspend fun createFilter(name: String): Boolean = store.createFilter(name)

    suspend fun deleteFilter(name: String) = store.deleteFilter(name)

    suspend fun reorderFilters(filters: List<String>) = store.reorderFilters(filters)

    suspend fun toggleStationFilter(stationUuid: String, filterName: String) =
        store.toggleStationFilter(stationUuid, filterName)

    suspend fun clearAll(alsoFilters: Boolean) = store.clearAll(alsoFilters)

    // Replaces the entire dataset (backup restore).
    suspend fun replaceAll(data: FavoritesData) = store.replaceAll(data)
}
