package com.radiomii.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.radiomii.domain.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

// 500 entries stays well under 250 KB; was 200 before filter groups were added.
private const val MAX_FAVORITES = 500
private const val MAX_FILTERS = 31

// Single container for all favorites data, used for both persistence and backup.
@Serializable
data class FavoritesData(
    val stations: List<Station> = emptyList(),
    val filters: List<String> = emptyList(),
    // stationUuid → filter names
    val filterMap: Map<String, List<String>> = emptyMap(),
)

private val favoritesJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }

@OptIn(ExperimentalSerializationApi::class)
object FavoritesSerializer : Serializer<FavoritesData> {
    override val defaultValue: FavoritesData = FavoritesData()

    override suspend fun readFrom(input: InputStream): FavoritesData =
        runCatching {
            favoritesJson.decodeFromStream<FavoritesData>(input)
        }.getOrDefault(defaultValue)

    // DataStore dispatches on IO — the blocking write() is safe here. Suppression silences a lint false positive.
    override suspend fun writeTo(t: FavoritesData, output: OutputStream) {
        favoritesJson.encodeToStream(t, output)
    }
}

val Context.favoritesDataStore: DataStore<FavoritesData>
        by dataStore(fileName = "favorites.json", serializer = FavoritesSerializer)

// DataStore is injected to allow test-scoped replacement without an Android Context.
@Singleton
class FavoritesDataStore @Inject constructor(
    private val store: DataStore<FavoritesData>,
) {
    // full snapshot, used for backup
    val dataFlow: Flow<FavoritesData> = store.data

    val favoritesFlow: Flow<List<Station>> = dataFlow.map { it.stations }
    val filtersFlow: Flow<List<String>> = dataFlow.map { it.filters }
    val filterMapFlow: Flow<Map<String, List<String>>> = dataFlow.map { it.filterMap }

    suspend fun getSnapshot(): FavoritesData = dataFlow.first()

    // Returns false if the limit is reached; no-op (returns true) if station already present.
    suspend fun addFavorite(station: Station): Boolean {
        var added = false
        store.updateData { current ->
            when {
                current.stations.any { it.stationuuid == station.stationuuid } -> {
                    added = true
                    current
                }
                current.stations.size >= MAX_FAVORITES -> current
                else -> {
                    added = true
                    current.copy(stations = current.stations + station)
                }
            }
        }
        return added
    }

    suspend fun removeFavorite(uuid: String) {
        store.updateData { current ->
            current.copy(
                stations = current.stations.filter { it.stationuuid != uuid },
                filterMap = current.filterMap.toMutableMap().also { it.remove(uuid) },
            )
        }
    }

    suspend fun reorderFavorites(stations: List<Station>) {
        store.updateData { current ->
            current.copy(stations = stations)
        }
    }

    suspend fun updateFavicon(uuid: String, faviconUrl: String) {
        store.updateData { current ->
            current.copy(
                stations = current.stations.map {
                    if (it.stationuuid == uuid) it.copy(favicon = faviconUrl) else it
                }
            )
        }
    }

    // Returns false if the filter limit is reached or name already exists.
    suspend fun createFilter(name: String): Boolean {
        var created = false
        store.updateData { current ->
            if (current.filters.size >= MAX_FILTERS ||
                current.filters.any { it.equals(name, ignoreCase = true) }
            ) {
                current
            } else {
                created = true
                current.copy(filters = current.filters + name)
            }
        }
        return created
    }

    suspend fun deleteFilter(name: String) {
        store.updateData { current ->
            current.copy(
                filters = current.filters.filter { it != name },
                filterMap = current.filterMap
                    .mapValues { (_, filters) -> filters.filter { it != name } }
                    .filterValues { it.isNotEmpty() },
            )
        }
    }

    suspend fun reorderFilters(filters: List<String>) {
        store.updateData { current ->
            current.copy(filters = filters)
        }
    }

    suspend fun toggleStationFilter(stationUuid: String, filterName: String) {
        store.updateData { current ->
            val stationFilters = current.filterMap[stationUuid]?.toMutableList()
                ?: mutableListOf()
            if (filterName in stationFilters) stationFilters.remove(filterName)
            else stationFilters.add(filterName)
            val newMap = current.filterMap.toMutableMap()
            if (stationFilters.isEmpty()) newMap.remove(stationUuid)
            else newMap[stationUuid] = stationFilters
            current.copy(filterMap = newMap)
        }
    }

    suspend fun clearAll(alsoFilters: Boolean) {
        store.updateData { current ->
            if (alsoFilters) FavoritesData()
            else current.copy(stations = emptyList(), filterMap = emptyMap())
        }
    }

    // Replaces the entire dataset (backup restore).
    suspend fun replaceAll(data: FavoritesData) {
        store.updateData { data }
    }
}
