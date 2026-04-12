package com.radiomii.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.radiomii.MainActivity
import com.radiomii.data.player.IcyMetadataHolder
import com.radiomii.data.prefs.SettingsDataStore
import com.radiomii.domain.model.IcyMetadata
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class RadioMiiPlayerService : MediaSessionService() {

    @Inject lateinit var metadataHolder: IcyMetadataHolder
    @Inject lateinit var settingsDataStore: SettingsDataStore

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setLoadControl(
                androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        /* minBufferMs = */ 10_000,
                        /* maxBufferMs = */ 30_000,
                        /* bufferForPlaybackMs = */ 1_000,
                        /* bufferForPlaybackAfterRebufferMs = */ 1_000,
                    )
                    .build()
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // ICY metadata goes to the shared holder (MediaController doesn't reliably forward it).
        // Also update MediaSession metadata so the OS notification shows track info.
        player.addListener(object : Player.Listener {
            override fun onMetadata(metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    val entry = metadata[i]
                    if (entry is IcyInfo) {
                        val raw = entry.title ?: continue
                        val parts = raw.split(" - ", limit = 2)
                        val icyMeta = if (parts.size == 2)
                            IcyMetadata(artist = parts[0].trim(), title = parts[1].trim(), rawTitle = raw)
                        else IcyMetadata(rawTitle = raw)

                        metadataHolder.update(icyMeta)

                        // replaceMediaItem() swaps only the metadata, playback is not interrupted.
                        if (player.mediaItemCount > 0) {
                            val current = player.getMediaItemAt(0)
                            val updatedMeta = current.mediaMetadata.buildUpon()
                                .setArtist(if (parts.size == 2) parts[0].trim() else raw)
                                .setSubtitle(if (parts.size == 2) parts[1].trim() else null)
                                .build()
                            player.replaceMediaItem(
                                0,
                                current.buildUpon().setMediaMetadata(updatedMeta).build()
                            )
                        }
                        return
                    }
                }
            }
        })

        val activityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(activityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
            return
        }
        // When background playback is disabled, stop the player when the task is removed
        serviceScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            if (!settings.backgroundPlayback) {
                player.stop()
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
