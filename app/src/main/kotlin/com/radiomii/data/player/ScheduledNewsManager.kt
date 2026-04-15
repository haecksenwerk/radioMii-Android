package com.radiomii.data.player

import com.radiomii.data.prefs.SettingsDataStore
import com.radiomii.domain.model.NewsInterval
import com.radiomii.domain.model.ScheduledNews
import com.radiomii.domain.model.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// Interval math kept top-level so unit tests can call it with a fixed Calendar.
internal fun msUntilNextInterval(
    interval: NewsInterval,
    now: Calendar = Calendar.getInstance(),
): Long {
    val minutes = now.get(Calendar.MINUTE)
    val seconds = now.get(Calendar.SECOND)
    val millis = now.get(Calendar.MILLISECOND)
    val currentMs = (seconds * 1000 + millis).toLong()

    val intervalMinutes = when (interval) {
        NewsInterval.HOURLY      -> 60
        NewsInterval.HALF_HOURLY -> 30
    }
    val minutesUntil = intervalMinutes - (minutes % intervalMinutes)
    return minutesUntil * 60_000L - currentMs
}

// Switches to the configured news station at each interval boundary (hour or half-hour),
// then returns to the previous station after durationMinutes (if > 0).
// When showSkipButton is true, the play/stop button is replaced by a skip icon during news.
@Singleton
class ScheduledNewsManager @Inject constructor(
    private val playerController: PlayerController,
    private val settingsDataStore: SettingsDataStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isPlayingNews = MutableStateFlow(false)
    val isPlayingNews: StateFlow<Boolean> = _isPlayingNews.asStateFlow()

    private var previousStation: Station? = null

    private var scheduleJob: Job? = null
    private var returnJob: Job? = null

    // Call when scheduled news settings change.
    fun onSettingsChanged(news: ScheduledNews, newsStation: Station?) {
        if (news.enabled && newsStation != null) {
            reschedule(news)
        } else {
            cancel()
        }
    }

    // Skips the current news segment and returns to the previous station.
    fun skipCurrentNews() {
        if (_isPlayingNews.value) {
            returnJob?.cancel()
            returnJob = null
            returnToPrevious()
            scope.launch {
                reschedule(settingsDataStore.settingsFlow.first().scheduledNews)
            }
        }
    }

    // Cancels news mode if the user picks a different station.
    fun cancelNewsMode() {
        if (_isPlayingNews.value) {
            returnJob?.cancel()
            returnJob = null
            _isPlayingNews.value = false
            previousStation = null
            scope.launch {
                reschedule(settingsDataStore.settingsFlow.first().scheduledNews)
            }
        }
    }

    private fun reschedule(news: ScheduledNews) {
        scheduleJob?.cancel()
        scheduleJob = scope.launch {
            val delayMs = msUntilNextInterval(news.intervalEnum)
            delay(delayMs)
            triggerNewsSwitch(news)
        }
    }

    private fun cancel() {
        scheduleJob?.cancel()
        scheduleJob = null
        returnJob?.cancel()
        returnJob = null
        _isPlayingNews.value = false
    }

    private fun triggerNewsSwitch(news: ScheduledNews) {
        val current = playerController.activeStation.value ?: return
        val isPlaying = playerController.isPlaying.value

        if (current.stationuuid == news.stationId) {
            reschedule(news)
            return
        }

        // Never start playback from a stopped state, regardless of showSkipButton.
        if (!isPlaying) {
            reschedule(news)
            return
        }

        previousStation = current
        _isPlayingNews.value = true

        // Pending switch decouples this manager from FavoritesRepository.
        // AppViewModel observes it and does the actual playStation() call.
        _pendingSwitch.value = news.stationId

        if (news.durationMinutes > 0) {
            returnJob?.cancel()
            returnJob = scope.launch {
                delay(news.durationMinutes * 60_000L)
                returnToPrevious()
                reschedule(news)
            }
        } else {
            // duration = 0 means play until stopped; still reschedule next cycle
            reschedule(news)
        }
    }

    private fun returnToPrevious() {
        _isPlayingNews.value = false
        val prev = previousStation
        previousStation = null
        if (prev != null && playerController.isPlaying.value) {
            scope.launch(Dispatchers.Main) {
                playerController.playStation(prev)
            }
        }
    }

    // Used by AppViewModel to observe when a switch should be triggered
    private val _pendingSwitch = MutableStateFlow<String?>(null)
    val pendingSwitch: StateFlow<String?> = _pendingSwitch.asStateFlow()

    // Clears the pending station-switch signal after it has been handled.
    fun clearPendingSwitch() {
        _pendingSwitch.value = null
    }
}
