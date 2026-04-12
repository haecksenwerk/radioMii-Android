package com.radiomii.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radiomii.data.remote.FaviconFetcherService
import com.radiomii.data.player.ScheduledNewsManager
import com.radiomii.data.player.PlayerController
import com.radiomii.data.repository.FavoritesRepository
import com.radiomii.data.repository.StationRepository
import com.radiomii.data.player.SleepTimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Vote outcome ──────────────────────────────────────────────────────────────

sealed class VoteOutcome {
    object Success : VoteOutcome()
    object AlreadyVoted : VoteOutcome()
    object Error : VoteOutcome()
}

// ── Icon search state ─────────────────────────────────────────────────────────

sealed class IconSearchState {
    object Idle : IconSearchState()
    object Loading : IconSearchState()
    data class Results(val urls: List<String>) : IconSearchState()
    object NoResults : IconSearchState()
    data class Error(val message: String) : IconSearchState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val sleepTimerManager: SleepTimerManager,
    private val favoritesRepository: FavoritesRepository,
    private val stationRepository: StationRepository,
    private val faviconFetcherService: FaviconFetcherService,
    private val scheduledNewsManager: ScheduledNewsManager,
) : ViewModel() {

    val activeStation = playerController.activeStation
    val isPlaying = playerController.isPlaying
    val isLoading = playerController.isLoading
    val metadata = playerController.metadata
    val sleepTimer = sleepTimerManager.state

    val isPlayingNews = scheduledNewsManager.isPlayingNews

    val favorites = favoritesRepository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _voteEvent = MutableSharedFlow<VoteOutcome>()
    val voteEvent: SharedFlow<VoteOutcome> = _voteEvent.asSharedFlow()

    private val _iconSearchState = MutableStateFlow<IconSearchState>(IconSearchState.Idle)
    val iconSearchState: StateFlow<IconSearchState> = _iconSearchState.asStateFlow()

    fun skipCurrentNews() = scheduledNewsManager.skipCurrentNews()

    fun toggleFavorite() {
        val station = activeStation.value ?: return
        viewModelScope.launch {
            val isFav = favorites.value.any { it.stationuuid == station.stationuuid }
            if (isFav) {
                favoritesRepository.removeFavorite(station.stationuuid)
            } else {
                favoritesRepository.addFavorite(station)
            }
        }
    }

    fun vote() {
        val station = activeStation.value ?: return
        viewModelScope.launch {
            val result = stationRepository.vote(station.stationuuid)
            val outcome = when {
                result.isFailure -> VoteOutcome.Error
                result.getOrNull()?.ok == true -> VoteOutcome.Success
                else -> VoteOutcome.AlreadyVoted
            }
            _voteEvent.emit(outcome)
        }
    }

    fun resetSleepTimer() {
        sleepTimerManager.reset()
    }

    /** Searches for icon candidates for the given homepage URL. */
    fun searchFavicons(homepageUrl: String) {
        viewModelScope.launch {
            _iconSearchState.value = IconSearchState.Loading
            try {
                val results = faviconFetcherService.fetchTopCandidates(homepageUrl, maxResults = 3)
                _iconSearchState.value = if (results.isEmpty()) IconSearchState.NoResults
                else IconSearchState.Results(results)
            } catch (e: Exception) {
                _iconSearchState.value = IconSearchState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Persists the chosen favicon URL and updates the in-memory active station immediately. */
    fun applyFaviconChoice(faviconUrl: String) {
        val station = activeStation.value ?: return
        viewModelScope.launch {
            favoritesRepository.updateFavicon(station.stationuuid, faviconUrl)
            playerController.updateStationFavicon(faviconUrl)
        }
        _iconSearchState.value = IconSearchState.Idle
    }

    /** Resets the icon search state (e.g. when dialog is dismissed). */
    fun resetIconSearch() {
        _iconSearchState.value = IconSearchState.Idle
    }
}
