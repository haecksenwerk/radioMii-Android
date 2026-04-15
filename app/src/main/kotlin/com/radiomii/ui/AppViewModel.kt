package com.radiomii.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radiomii.data.player.SleepTimerManager
import com.radiomii.data.player.ScheduledNewsManager
import com.radiomii.data.player.PlayerController
import com.radiomii.data.prefs.SettingsDataStore
import com.radiomii.data.repository.FavoritesRepository
import com.radiomii.domain.model.AppSettings
import com.radiomii.domain.model.IcyMetadata
import com.radiomii.domain.model.ScheduledNews
import com.radiomii.domain.model.SearchOptions
import com.radiomii.domain.model.SleepTimerState
import com.radiomii.domain.model.StartupState
import com.radiomii.domain.model.Station
import com.radiomii.domain.error.GlobalErrorManager
import com.radiomii.domain.error.GlobalError
import com.radiomii.data.remote.NetworkMonitor
import com.radiomii.data.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// App-level ViewModel — shared playback state across all screens.
@HiltViewModel
class AppViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val sleepTimerManager: SleepTimerManager,
    private val settingsDataStore: SettingsDataStore,
    private val scheduledNewsManager: ScheduledNewsManager,
    private val favoritesRepository: FavoritesRepository,
    private val stationRepository: StationRepository,
    private val networkMonitor: NetworkMonitor,
    private val globalErrorManager: GlobalErrorManager,
) : ViewModel() {

    private val _startupState = MutableStateFlow(StartupState.INITIALIZING)
    val startupState: StateFlow<StartupState> = _startupState

    val globalErrorEvent = globalErrorManager.errorEvent

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    val activeStation: StateFlow<Station?> = playerController.activeStation.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val isPlaying: StateFlow<Boolean> = playerController.isPlaying.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    val isLoading: StateFlow<Boolean> = playerController.isLoading.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    val metadata: StateFlow<IcyMetadata?> = playerController.metadata.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )


    val sleepTimer: StateFlow<SleepTimerState> = sleepTimerManager.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SleepTimerState(),
    )

    val isPlayingNews: StateFlow<Boolean> = scheduledNewsManager.isPlayingNews.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    // Eagerly started so the result is ready before the first UI subscriber arrives.
    val hasFavorites: StateFlow<Boolean?> = favoritesRepository.favoritesFlow.map { it.isNotEmpty() }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    init {
        viewModelScope.launch {
            _startupState.value = StartupState.LOADING
            if (!networkMonitor.isConnected()) {
                _startupState.value = StartupState.ERROR_NO_INTERNET
                globalErrorManager.emitError(GlobalError.NO_INTERNET)
            } else {
                try {
                    val searchOptions = settingsDataStore.settingsFlow.first().searchOptions
                    stationRepository.searchStations(0, "", searchOptions)
                    _startupState.value = StartupState.SUCCESS
                } catch (_: Exception) {
                    _startupState.value = StartupState.ERROR_SERVER_UNREACHABLE
                    globalErrorManager.emitError(GlobalError.SERVER_UNREACHABLE)
                }
            }
        }

        viewModelScope.launch {
            sleepTimerManager.restoreSelectedMinutes()
            // Restore last active station without auto-playing
            val savedStation = settingsDataStore.activeStationFlow.first()
            if (playerController.activeStation.value == null && savedStation != null) {
                playerController.restoreStation(savedStation)
            }
        }

        // Wire ScheduledNewsManager: react to settings + favorites changes
        viewModelScope.launch {
            combine(
                settingsDataStore.settingsFlow,
                favoritesRepository.favoritesFlow,
            ) { settings, favorites ->
                val news = settings.scheduledNews
                val newsStation = favorites.firstOrNull { it.stationuuid == news.stationId }
                scheduledNewsManager.onSettingsChanged(news, newsStation)
            }.collect {}
        }

        // Handle pending station switches triggered by the scheduler
        viewModelScope.launch {
            scheduledNewsManager.pendingSwitch.collect { stationId ->
                if (stationId != null) {
                    val station = favoritesRepository.favoritesFlow.first()
                        .firstOrNull { it.stationuuid == stationId }
                    if (station != null) {
                        playerController.playStation(station)
                    }
                    scheduledNewsManager.clearPendingSwitch()
                }
            }
        }
    }

    fun play(station: Station) {
        scheduledNewsManager.cancelNewsMode()
        playerController.playStation(station)
    }
    fun togglePlayPause() {
        // Cancel news mode first so the returnJob is stopped before the player halts.
        scheduledNewsManager.cancelNewsMode()
        playerController.togglePlayPause()
    }
    fun skipCurrentNews() = scheduledNewsManager.skipCurrentNews()
    fun startSleepTimer(minutes: Int, cancelOnStop: Boolean = false) = sleepTimerManager.start(minutes, cancelOnStop)
    fun resetSleepTimer() = sleepTimerManager.reset()
    fun setScheduledNews(news: ScheduledNews) = viewModelScope.launch { settingsDataStore.setScheduledNews(news) }
    fun setCompactRow(compact: Boolean) = viewModelScope.launch { settingsDataStore.setCompactRow(compact) }
    fun setShowFilterBar(show: Boolean) = viewModelScope.launch { settingsDataStore.setShowFilterBar(show) }
    fun setSearchOptions(opts: SearchOptions) = viewModelScope.launch { settingsDataStore.setSearchOptions(opts) }
}
