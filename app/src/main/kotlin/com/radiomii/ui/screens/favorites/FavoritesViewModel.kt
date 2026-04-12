package com.radiomii.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radiomii.data.repository.FavoritesRepository
import com.radiomii.domain.model.Station
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val favorites: StateFlow<List<Station>> = favoritesRepository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filterNames: StateFlow<List<String>> = favoritesRepository.filtersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stationFilterMap: StateFlow<Map<String, List<String>>> = favoritesRepository.filterMapFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun removeFavorite(stationuuid: String) {
        viewModelScope.launch { favoritesRepository.removeFavorite(stationuuid) }
    }

    fun reorderFavorites(reordered: List<Station>) {
        viewModelScope.launch { favoritesRepository.reorderFavorites(reordered) }
    }

    // Reorders a filtered subset in-place within the full list,
    // preserving positions of non-filtered stations.
    fun reorderFavoritesSubset(reordered: List<Station>, fullList: List<Station>) {
        viewModelScope.launch {
            val reorderedIds = reordered.map { it.stationuuid }.toSet()
            val slots = fullList.indices.filter { fullList[it].stationuuid in reorderedIds }
            val merged = fullList.toMutableList()
            slots.forEachIndexed { i, slot -> merged[slot] = reordered[i] }
            favoritesRepository.reorderFavorites(merged)
        }
    }

    fun toggleStationFilter(stationuuid: String, filterName: String) {
        viewModelScope.launch {
            favoritesRepository.toggleStationFilter(stationuuid, filterName)
        }
    }

    fun deleteFilter(filterName: String) {
        viewModelScope.launch { favoritesRepository.deleteFilter(filterName) }
    }

    fun reorderFilters(reordered: List<String>) {
        viewModelScope.launch { favoritesRepository.reorderFilters(reordered) }
    }

    // Returns stations for the given filter (-1 = all).
    fun filteredFavorites(all: List<Station>, filterNames: List<String>, activeIndex: Int): List<Station> =
        filterStationsByGroup(all, filterNames, activeIndex, stationFilterMap.value)
}

// Pure function extracted for testability. Maps activeIndex to a filter name,
// then returns only those stations whose UUID appears in the filterMap under that name.
internal fun filterStationsByGroup(
    all: List<Station>,
    filterNames: List<String>,
    activeIndex: Int,
    filterMap: Map<String, List<String>>,
): List<Station> {
    if (activeIndex < 0 || activeIndex >= filterNames.size) return all
    val name = filterNames[activeIndex]
    val uuids = filterMap.entries
        .filter { (_, filters) -> name in filters }
        .map { it.key }
        .toSet()
    return all.filter { it.stationuuid in uuids }
}
