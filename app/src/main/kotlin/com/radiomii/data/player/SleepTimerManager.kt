package com.radiomii.data.player

import com.radiomii.data.prefs.SettingsDataStore
import com.radiomii.domain.model.SleepTimerState
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepTimerManager @Inject constructor(
    private val playerController: PlayerController,
    private val settingsDataStore: SettingsDataStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    init {
        // Auto-cancel timer when playback stops (if cancelOnStop is enabled)
        scope.launch {
            playerController.isPlaying.collect { isPlaying ->
                if (!isPlaying && _state.value.isActive && _state.value.cancelOnStop) {
                    reset()
                }
            }
        }
    }

    fun start(minutes: Int, cancelOnStop: Boolean = false) {
        timerJob?.cancel()
        val endTime = System.currentTimeMillis() + minutes * 60_000L
        _state.value = SleepTimerState(
            isActive = true,
            endTimeMs = endTime,
            selectedMinutes = minutes,
            cancelOnStop = cancelOnStop,
            remainingMs = minutes * 60_000L,   // initialize so UI shows full bar immediately
        )
        scope.launch {
            settingsDataStore.setSleepTimerMinutes(minutes)
            settingsDataStore.setSleepTimerCancelOnStop(cancelOnStop)
        }

        timerJob = scope.launch {
            while (true) {
                delay(1_000L)
                // StateFlow needs an explicit field change to emit on each tick.
                val remaining = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
                _state.value = _state.value.copy(remainingMs = remaining)
                if (remaining <= 0L) {
                    playerController.stop()
                    reset()
                    break
                }
            }
        }
    }

    fun reset() {
        timerJob?.cancel()
        timerJob = null
        _state.value = SleepTimerState(
            isActive = false,
            selectedMinutes = _state.value.selectedMinutes,
            cancelOnStop = _state.value.cancelOnStop,
        )
    }

    suspend fun restoreSelectedMinutes() {
        val minutes = settingsDataStore.sleepTimerMinutesFlow.first()
        val cancelOnStop = settingsDataStore.sleepTimerCancelOnStopFlow.first()
        _state.value = _state.value.copy(selectedMinutes = minutes, cancelOnStop = cancelOnStop)
    }
}
