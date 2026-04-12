package com.radiomii.data.player

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import com.radiomii.data.prefs.SettingsDataStore
import com.radiomii.domain.model.IcyMetadata
import com.radiomii.domain.model.Station
import com.radiomii.domain.error.GlobalError
import com.radiomii.domain.error.GlobalErrorManager
import com.radiomii.data.remote.NetworkMonitor
import com.radiomii.service.RadioMiiPlayerService
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val icyMetadataHolder: IcyMetadataHolder,
    private val networkMonitor: NetworkMonitor,
    private val globalErrorManager: GlobalErrorManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _activeStation = MutableStateFlow<Station?>(null)
    val activeStation: StateFlow<Station?> = _activeStation.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val metadata: StateFlow<IcyMetadata?> = icyMetadataHolder.metadata


    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private var retryCount = 0
    private val maxRetries = 5
    private var lastStation: Station? = null

    init {
        connect()
    }

    private fun connect() {
        val sessionToken = SessionToken(context, ComponentName(context, RadioMiiPlayerService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
        }, ContextCompat.getMainExecutor(context))
    }

    @Suppress("unused")
    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    fun playStation(station: Station) {
        if (!networkMonitor.isConnected()) {
            globalErrorManager.emitError(GlobalError.NO_INTERNET)
            return
        }

        lastStation = station
        retryCount = 0
        _activeStation.value = station
        icyMetadataHolder.clear()

        scope.launch {
            settingsDataStore.setActiveStation(station)
        }

        val mediaItem = MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist("")
                    .setArtworkUri(
                        if (station.favicon.isNotBlank()) station.favicon.toUri() else null
                    )
                    .build()
            )
            .build()

        controller?.let {
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }
    }

    // Restores the last active station without starting playback.
    fun restoreStation(station: Station) {
        _activeStation.value = station
    }

    // Updates the favicon of the active station in memory so the UI re-renders immediately.
    fun updateStationFavicon(faviconUrl: String) {
        _activeStation.value = _activeStation.value?.copy(favicon = faviconUrl)
    }

    fun togglePlayPause() {
        if (!networkMonitor.isConnected() && !_isPlaying.value) {
            globalErrorManager.emitError(GlobalError.NO_INTERNET)
            return
        }

        val ctrl = controller ?: return
        if (ctrl.isPlaying) {
            // Live streams can't be resumed — stop and clear the buffer.
            // Update state immediately; the async onIsPlayingChanged follows but is harmless.
            ctrl.stop()
            ctrl.clearMediaItems()
            icyMetadataHolder.clear()
            _isPlaying.value = false
            _isLoading.value = false
        } else if (ctrl.mediaItemCount == 0) {
            // No media item (cold restore or after stop) — reload and play
            _activeStation.value?.let { playStation(it) }
        } else {
            ctrl.play()
        }
    }

    fun stop() {
        controller?.stop()
        _isPlaying.value = false
        _isLoading.value = false
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isLoading.value = playbackState == Player.STATE_BUFFERING
        }

        override fun onPlayerError(error: PlaybackException) {
            _isLoading.value = false
            scope.launch {
                val isNetworkError = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT

                if (isNetworkError && !networkMonitor.isConnected()) {
                    globalErrorManager.emitError(GlobalError.NO_INTERNET)
                    _isPlaying.value = false
                    return@launch
                }

                if (retryCount < maxRetries) {
                    val delaySeconds = (1L shl retryCount) * 2 // 2, 4, 8, 16, 32s
                    retryCount++
                    delay(delaySeconds * 1000L)
                    
                    _activeStation.value?.let { station ->
                        val mediaItem = MediaItem.Builder()
                            .setUri(station.streamUrl)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(station.name)
                                    .setArtist("")
                                    .setArtworkUri(
                                        if (station.favicon.isNotBlank()) station.favicon.toUri() else null
                                    )
                                    .build()
                            )
                            .build()
                        controller?.setMediaItem(mediaItem)
                    }
                    
                    controller?.prepare()
                    controller?.play()
                } else {
                    retryCount = 0
                    _isPlaying.value = false
                    globalErrorManager.emitError(GlobalError.PLAYBACK_ERROR)
                }
            }
        }
    }
}
