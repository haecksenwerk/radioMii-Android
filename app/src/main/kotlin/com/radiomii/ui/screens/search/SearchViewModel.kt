package com.radiomii.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radiomii.data.repository.FavoritesRepository
import com.radiomii.data.repository.StationRepository
import com.radiomii.domain.model.SearchOptions
import com.radiomii.domain.model.Station
import com.radiomii.domain.error.GlobalError
import com.radiomii.domain.error.GlobalErrorManager
import com.radiomii.data.remote.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val favoritesRepository: FavoritesRepository,
    private val networkMonitor: NetworkMonitor,
    private val globalErrorManager: GlobalErrorManager,
) : ViewModel() {

    private val _results = MutableStateFlow<List<Station>>(emptyList())
    val results: StateFlow<List<Station>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _moreAvailable = MutableStateFlow(false)
    val moreAvailable: StateFlow<Boolean> = _moreAvailable.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Bumped on each fresh search so the UI can scroll back to the top.
    private val _searchVersion = MutableStateFlow(0)
    val searchVersion: StateFlow<Int> = _searchVersion.asStateFlow()

    val favorites: StateFlow<List<Station>> = favoritesRepository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var currentQuery = ""
    private var currentOffset = 0
    private var currentOptions = SearchOptions()
    private var searchJob: Job? = null
    private companion object {
        const val PAGE_SIZE = 50
    }

    fun search(query: String, options: SearchOptions) {
        if (!networkMonitor.isConnected()) {
            globalErrorManager.emitError(GlobalError.NO_INTERNET)
            return
        }

        if (query == currentQuery && options == currentOptions && currentOffset == 0 && _results.value.isNotEmpty()) return
        currentQuery = query
        currentOptions = options
        currentOffset = 0
        _results.value = emptyList()
        _searchVersion.value++
        loadPage()
    }

    fun loadMore() {
        if (!networkMonitor.isConnected()) {
            globalErrorManager.emitError(GlobalError.NO_INTERNET)
            return
        }

        if (_isLoading.value || !_moreAvailable.value) return
        currentOffset += PAGE_SIZE
        loadPage()
    }

    private fun loadPage() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                stationRepository.searchStations(
                    offset = currentOffset,
                    searchTerm = currentQuery,
                    options = currentOptions,
                )
            }.fold(
                onSuccess = { stations ->
                    _results.value = if (currentOffset == 0) stations else _results.value + stations
                    _moreAvailable.value = stations.size == PAGE_SIZE
                },
                onFailure = { e ->
                    _error.value = e.message
                    _moreAvailable.value = false
                },
            )
            _isLoading.value = false
        }
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            val isFav = favorites.value.any { it.stationuuid == station.stationuuid }
            if (isFav) {
                favoritesRepository.removeFavorite(station.stationuuid)
            } else {
                favoritesRepository.addFavorite(station)
            }
        }
    }
}
