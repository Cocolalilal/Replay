package com.maxrave.media3.sonos

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.maxrave.domain.data.player.RemoteDeviceType
import com.maxrave.domain.data.player.SonosDevice
import com.maxrave.domain.mediaservice.sonos.SonosController
import com.maxrave.logger.Logger
import com.maxrave.media3.exoplayer.CrossfadeExoPlayerAdapter
import com.maxrave.media3.exoplayer.DelegatingForwardingPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

/**
 * Sonos is the sole audio + transport owner while connected.
 *
 * Architecture:
 * - Local ExoPlayer is silenced and never advances a parallel queue.
 * - MediaSession talks to [DelegatingForwardingPlayer] remote transport/volume
 *   controls so HW keys show the system external-device volume UI and play/pause
 *   never hit ExoPlayer.
 * - All track loads go through a single cancellable [playTrackJob] (generation-
 *   guarded) so rapid skip/mode cannot leave two pipelines playing.
 * - End-of-track auto-advance uses last-known playing position (Sonos often
 *   resets RelTime to 0 on STOPPED — that must not be misread as "paused at start").
 */
@UnstableApi
internal class SonosHandoffManager(
    private val context: Context,
    private val adapter: CrossfadeExoPlayerAdapter,
    private val streamResolver: SonosStreamResolver,
    private val coroutineScope: CoroutineScope,
) : SonosController {
    private val soapClient = SonosSoapClient()
    private val discovery = SonosDiscovery(context, coroutineScope, soapClient)
    private val audioServer = SonosAudioServer(context, streamResolver, coroutineScope)

    override val devices: StateFlow<List<SonosDevice>> = discovery.devices
    override val isScanning: StateFlow<Boolean> = discovery.isScanning

    private val _connectedDevice = MutableStateFlow<SonosDevice?>(null)
    override val connectedDevice: StateFlow<SonosDevice?> = _connectedDevice.asStateFlow()

    private val _volume = MutableStateFlow(0.5f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private var pollJob: Job? = null
    private var volumeJob: Job? = null
    private var playTrackJob: Job? = null
    private var connectJob: Job? = null
    private var advanceJob: Job? = null

    /** CastHandoffManager baseline router — restored on Sonos disconnect. */
    private var previousCastPlaybackRouter: ((Int, Long, Boolean) -> Unit)? = null

    /** Bumped on every user-facing handoff so in-flight SOAP/prepare work aborts. */
    private val handoffGeneration = AtomicLong(0L)

    /** Last volume the user explicitly set (0..100). Used to CAP on connect — never raise. */
    @Volatile
    private var lastUserVolumePercent: Int? = null

    @Volatile
    private var suppressPollingUntilMs: Long = 0L

    @Volatile
    private var currentLoadedVideoId: String? = null

    /** Last RelTime observed while transport was PLAYING (survives STOPPED RelTime=0 reset). */
    @Volatile
    private var lastPositionWhilePlayingMs: Long = 0L

    @Volatile
    private var sawPlayingForCurrentTrack: Boolean = false

    val isConnected: Boolean
        get() = _connectedDevice.value != null

    private val sonosPlayer: SonosPlayer =
        SonosPlayer(
            basePlayer = adapter.forwardingPlayer,
            onPlayAction = { requestPlay() },
            onPauseAction = { requestPause() },
            onStopAction = { requestStop() },
            onSeekAction = { positionMs -> seekSonos(positionMs) },
            onVolumeAction = { vol -> setVolume(vol, fromUser = true) },
            onSeekToNextAction = { adapter.seekToNext() },
            onSeekToPreviousAction = { adapter.seekToPrevious() },
        )

    fun start() {
        Logger.d(TAG, "SonosHandoffManager initialized")
    }

    override fun startDiscovery() {
        discovery.startDiscovery()
    }

    override fun stopDiscovery() {
        discovery.stopDiscovery()
    }

    override fun refresh() {
        discovery.refresh()
    }

    override fun connect(device: SonosDevice) {
        if (_connectedDevice.value?.id == device.id && isConnected) {
            Logger.d(TAG, "Already connected to ${device.name}")
            return
        }

        // Cancel any prior connect / track handoff — new connect wins.
        connectJob?.cancel()
        playTrackJob?.cancel()
        advanceJob?.cancel()
        val generation = handoffGeneration.incrementAndGet()

        connectJob =
            coroutineScope.launch {
                try {
                    Logger.i(TAG, "Connecting to Sonos device: ${device.name} (${device.ip})")

                    // Snapshot BEFORE silencing / routing — works from playing OR paused.
                    val startIndex = adapter.currentMediaItemIndex
                    val startPositionMs = adapter.currentPosition.coerceAtLeast(0L)
                    val playWhenReady = adapter.isPlaying || adapter.playWhenReady
                    val initialDurationMs = adapter.duration.takeIf { it > 0 } ?: 0L

                    Logger.i(
                        TAG,
                        "Connect snapshot: index=$startIndex pos=${startPositionMs}ms " +
                            "playWhenReady=$playWhenReady (no pause-first ritual required)",
                    )

                    _connectedDevice.value = device
                    audioServer.start()

                    sonosPlayer.remotePositionMs = startPositionMs
                    sonosPlayer.remoteDurationMs = initialDurationMs
                    sonosPlayer.remoteIsPlaying = playWhenReady
                    lastPositionWhilePlayingMs = startPositionMs
                    sawPlayingForCurrentTrack = false

                    // Sole owner: silence local ExoPlayer sync, route adapter + MediaSession.
                    adapter.setCastActive(sonosPlayer, "${device.name} (Sonos)", RemoteDeviceType.SONOS)
                    wireSessionRemoteControls()
                    if (previousCastPlaybackRouter == null) {
                        previousCastPlaybackRouter = adapter.castPlaybackRouter
                    }
                    adapter.castPlaybackRouter = { index, positionMs, pwr ->
                        playTrack(index, positionMs, pwr)
                    }

                    if (generation != handoffGeneration.get()) return@launch

                    // CAP only (never raise) on connect.
                    syncVolumeOnConnect(device)

                    startPolling(device)

                    if (startIndex >= 0 && startIndex < adapter.mediaItemCount) {
                        playTrack(startIndex, startPositionMs, playWhenReady)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Logger.e(TAG, "Failed to connect to Sonos speaker: ${e.message}", e)
                    disconnect()
                }
            }
    }

    private fun wireSessionRemoteControls() {
        val fp = adapter.forwardingPlayer
        fp.remoteDeviceVolumeControl =
            object : DelegatingForwardingPlayer.RemoteDeviceVolumeControl {
                override fun deviceInfo() = sonosPlayer.deviceInfo

                override fun deviceVolume() = sonosPlayer.deviceVolume

                override fun deviceMuted() = sonosPlayer.isDeviceMuted

                override fun setDeviceVolume(volume: Int, flags: Int) = sonosPlayer.setDeviceVolume(volume, flags)

                override fun increaseDeviceVolume(flags: Int) = sonosPlayer.increaseDeviceVolume(flags)

                override fun decreaseDeviceVolume(flags: Int) = sonosPlayer.decreaseDeviceVolume(flags)

                override fun setDeviceMuted(muted: Boolean, flags: Int) = sonosPlayer.setDeviceMuted(muted, flags)
            }
        fp.remoteTransportControl =
            object : DelegatingForwardingPlayer.RemoteTransportControl {
                override fun play() = sonosPlayer.play()

                override fun pause() = sonosPlayer.pause()

                override fun stop() = sonosPlayer.stop()

                override fun seekTo(positionMs: Long) = sonosPlayer.seekTo(positionMs)

                override fun isPlaying() = sonosPlayer.isPlaying

                override fun getPlayWhenReady() = sonosPlayer.remoteIsPlaying

                override fun setPlayWhenReady(playWhenReady: Boolean) {
                    if (playWhenReady) sonosPlayer.play() else sonosPlayer.pause()
                }

                override fun getCurrentPosition() = sonosPlayer.currentPosition

                override fun getDuration() = sonosPlayer.duration

                override fun getPlaybackState(): Int =
                    when {
                        currentLoadedVideoId == null -> Player.STATE_BUFFERING
                        sonosPlayer.remoteIsPlaying -> Player.STATE_READY
                        else -> Player.STATE_READY
                    }
            }
        // Critical: rebuild MediaSession VolumeProvider (setPlaybackToRemote) for Samsung overlay.
        fp.notifyRemoteRoutingChanged()
    }

    private suspend fun syncVolumeOnConnect(device: SonosDevice) {
        val remoteVol = withContext(Dispatchers.IO) { soapClient.getVolume(device.baseUrl) }
        if (remoteVol != null) {
            val lastUser = lastUserVolumePercent
            val capped =
                if (lastUser != null) {
                    minOf(remoteVol, lastUser).coerceIn(0, 100)
                } else {
                    remoteVol.coerceIn(0, 100)
                }
            if (capped < remoteVol) {
                Logger.w(
                    TAG,
                    "Connect CAP: device was ${remoteVol}%, applying ${capped}% " +
                        "(min of device, lastUser=$lastUser) — never raising on connect",
                )
                withContext(Dispatchers.IO) {
                    soapClient.setVolume(
                        device.baseUrl,
                        capped,
                        fromUser = false,
                        onlyIfLoweringFrom = remoteVol,
                    )
                }
            } else {
                Logger.i(TAG, "Connect: syncing UI to device volume ${remoteVol}% (no SetVolume raise)")
            }
            val volFloat = capped / 100f
            _volume.value = volFloat
            sonosPlayer.remoteVolume = volFloat
            adapter.notifyRemoteDeviceVolumeChanged(capped)
            adapter.forwardingPlayer.notifyRemoteRoutingChanged()
        } else {
            Logger.w(TAG, "Connect: unknown device volume — keeping safe mid 0.5 (never 1.0)")
            _volume.value = 0.5f
            sonosPlayer.remoteVolume = 0.5f
            adapter.notifyRemoteDeviceVolumeChanged(50)
            adapter.forwardingPlayer.notifyRemoteRoutingChanged()
        }
    }

    override fun disconnect() {
        val current = _connectedDevice.value ?: return
        Logger.i(TAG, "Disconnecting from Sonos: ${current.name}")
        handoffGeneration.incrementAndGet()
        _connectedDevice.value = null
        currentLoadedVideoId = null
        sawPlayingForCurrentTrack = false
        lastPositionWhilePlayingMs = 0L

        stopPolling()
        playTrackJob?.cancel()
        playTrackJob = null
        connectJob?.cancel()
        connectJob = null
        advanceJob?.cancel()
        advanceJob = null

        val resumeIndex = adapter.currentMediaItemIndex
        val resumePositionMs = sonosPlayer.remotePositionMs
        val resumePlay = sonosPlayer.remoteIsPlaying

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { soapClient.pause(current.baseUrl) }
            }
        }

        audioServer.stop()

        adapter.forwardingPlayer.remoteDeviceVolumeControl = null
        adapter.forwardingPlayer.remoteTransportControl = null
        adapter.forwardingPlayer.notifyRemoteRoutingChanged()
        adapter.castPlaybackRouter = previousCastPlaybackRouter
        previousCastPlaybackRouter = null
        adapter.setCastActive(null, null)

        if (resumeIndex >= 0 && resumeIndex < adapter.mediaItemCount) {
            Logger.d(TAG, "Resuming local playback at index $resumeIndex, ${resumePositionMs}ms play=$resumePlay")
            adapter.playWhenReady = resumePlay
            adapter.seekTo(resumeIndex, resumePositionMs)
        }
    }

    /**
     * Write absolute Sonos speaker volume.
     * @param fromUser true when the in-app volume slider / buttons initiated the change,
     *   or when MediaSession HW volume keys routed through [SonosPlayer.setDeviceVolume].
     *   Unprompted writes at/near MAX are refused (safety: never deafen on skip/scrub).
     */
    override fun setVolume(volume: Float) {
        setVolume(volume, fromUser = true)
    }

    fun setVolume(
        volume: Float,
        fromUser: Boolean,
    ) {
        val clamped = volume.coerceIn(0f, 1f)
        val volumePercent = (clamped * 100).toInt()
        if (!fromUser && volumePercent >= SonosSoapClient.HIGH_VOLUME_THRESHOLD) {
            Logger.e(TAG, "REFUSING unprompted Sonos volume write to $volumePercent%")
            return
        }
        if (fromUser) {
            soapClient.markUserVolumeGesture()
            lastUserVolumePercent = volumePercent
        }
        _volume.value = clamped
        sonosPlayer.remoteVolume = clamped
        adapter.notifyRemoteDeviceVolumeChanged(volumePercent)

        val device = _connectedDevice.value ?: return
        volumeJob?.cancel()
        volumeJob =
            coroutineScope.launch {
                delay(50)
                if (fromUser) {
                    soapClient.markUserVolumeGesture()
                }
                withContext(Dispatchers.IO) {
                    soapClient.setVolume(device.baseUrl, volumePercent, fromUser = fromUser)
                }
            }
    }

    private fun requestPlay() {
        val dev = _connectedDevice.value ?: return
        coroutineScope.launch {
            suppressPollingUntilMs = System.currentTimeMillis() + 3500L
            sonosPlayer.remoteIsPlaying = true
            adapter.notifyRemoteIsPlaying(true)
            adapter.silenceLocalPlayersForRemote()

            val transport = withContext(Dispatchers.IO) { soapClient.getTransportInfo(dev.baseUrl) }
            val currentMedia = adapter.currentMediaItem
            val isTrackLoaded =
                currentLoadedVideoId != null &&
                    currentLoadedVideoId == currentMedia?.mediaId &&
                    !transport.equals("STOPPED", ignoreCase = true) &&
                    !transport.equals("NO_MEDIA_PRESENT", ignoreCase = true)

            if (!isTrackLoaded) {
                Logger.d(TAG, "onPlayAction: track not loaded on Sonos (state=$transport), re-triggering playTrack()")
                playTrack(adapter.currentMediaItemIndex, sonosPlayer.remotePositionMs, true)
            } else {
                val playOk = withContext(Dispatchers.IO) { soapClient.play(dev.baseUrl) }
                if (!playOk) {
                    Logger.w(TAG, "soapClient.play() returned false, re-triggering playTrack()")
                    playTrack(adapter.currentMediaItemIndex, sonosPlayer.remotePositionMs, true)
                }
            }
        }
    }

    private fun requestPause() {
        val dev = _connectedDevice.value ?: return
        coroutineScope.launch {
            suppressPollingUntilMs = System.currentTimeMillis() + 3500L
            sonosPlayer.remoteIsPlaying = false
            adapter.notifyRemoteIsPlaying(false)
            adapter.silenceLocalPlayersForRemote()
            withContext(Dispatchers.IO) { soapClient.pause(dev.baseUrl) }
        }
    }

    private fun requestStop() {
        val dev = _connectedDevice.value ?: return
        coroutineScope.launch {
            suppressPollingUntilMs = System.currentTimeMillis() + 3500L
            sonosPlayer.remoteIsPlaying = false
            adapter.notifyRemoteIsPlaying(false)
            adapter.silenceLocalPlayersForRemote()
            withContext(Dispatchers.IO) { soapClient.stop(dev.baseUrl) }
        }
    }

    private fun seekSonos(positionMs: Long) {
        val device = _connectedDevice.value ?: return
        suppressPollingUntilMs = System.currentTimeMillis() + 3000L
        sonosPlayer.remotePositionMs = positionMs
        if (sonosPlayer.remoteIsPlaying) {
            lastPositionWhilePlayingMs = positionMs
        }
        soapClient.beginTransportVolumeAssert()
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    soapClient.seek(device.baseUrl, positionMs)
                }
            } finally {
                soapClient.endTransportVolumeAssert()
            }
        }
    }

    private fun playTrack(
        index: Int,
        startPositionMs: Long,
        playWhenReady: Boolean,
    ) {
        val device = _connectedDevice.value ?: return
        if (index < 0 || index >= adapter.mediaItemCount) return

        // New user/action handoff cancels any in-flight prepare/SOAP.
        playTrackJob?.cancel()
        advanceJob?.cancel()
        val generation = handoffGeneration.incrementAndGet()
        adapter.silenceLocalPlayersForRemote()

        playTrackJob =
            coroutineScope.launch {
                soapClient.beginTransportVolumeAssert()
                try {
                    suppressPollingUntilMs = System.currentTimeMillis() + 5000L
                    val mediaItem = adapter.getMediaItemAt(index) ?: return@launch
                    val videoId = mediaItem.mediaId
                    Logger.d(TAG, "playTrack: preparing $videoId (index=$index, pos=${startPositionMs}ms, pwr=$playWhenReady)")

                    sawPlayingForCurrentTrack = false
                    lastPositionWhilePlayingMs = startPositionMs.coerceAtLeast(0L)
                    currentLoadedVideoId = null

                    val initialDurationMs: Long =
                        adapter.duration.takeIf { it > 0 }
                            ?: sonosPlayer.remoteDurationMs.takeIf { it > 0 }
                            ?: 0L

                    sonosPlayer.remotePositionMs = startPositionMs
                    if (initialDurationMs > 0L) {
                        sonosPlayer.remoteDurationMs = initialDurationMs
                    }
                    sonosPlayer.remoteIsPlaying = playWhenReady
                    adapter.notifyRemoteIsPlaying(playWhenReady)
                    adapter.notifyRemoteTransition(index)
                    adapter.notifyRemotePlaybackState(Player.STATE_BUFFERING)

                    val prepared = audioServer.prepareTrack(videoId)
                    if (generation != handoffGeneration.get() || _connectedDevice.value?.id != device.id) {
                        Logger.d(TAG, "playTrack aborted after prepare (superseded)")
                        return@launch
                    }

                    val extension = prepared?.extension ?: "m4a"
                    val streamMime = prepared?.mimeType ?: "audio/mp4"
                    val streamSize = prepared?.file?.length() ?: 0L
                    val streamUrl = audioServer.getStreamUrl(videoId, extension, "")
                    val artworkUrl = audioServer.getArtworkUrl(videoId, mediaItem.metadata.artworkUri)

                    val title = mediaItem.metadata.title ?: "Unknown Title"
                    val artist = mediaItem.metadata.artist ?: "Unknown Artist"
                    val album = mediaItem.metadata.albumTitle ?: ""
                    val durationSec =
                        prepared?.durationSeconds?.takeIf { it > 0 }
                            ?: ((sonosPlayer.remoteDurationMs / 1000).toInt().takeIf { it > 0 })
                            ?: 0

                    if (durationSec > 0 && sonosPlayer.remoteDurationMs <= 0L) {
                        sonosPlayer.remoteDurationMs = durationSec * 1000L
                    }

                    Logger.i(TAG, "Sending track to Sonos: $title by $artist ($streamUrl)")
                    val setUriSuccess =
                        withContext(Dispatchers.IO) {
                            soapClient.setAVTransportURI(
                                baseUrl = device.baseUrl,
                                streamUrl = streamUrl,
                                title = title,
                                artist = artist,
                                album = album,
                                artworkUrl = artworkUrl,
                                durationSeconds = durationSec,
                                mimeType = streamMime,
                                sizeBytes = streamSize,
                            )
                        }

                    if (generation != handoffGeneration.get()) return@launch

                    if (!setUriSuccess) {
                        Logger.e(TAG, "Failed to set AVTransport URI on Sonos for $videoId")
                        return@launch
                    }

                    currentLoadedVideoId = videoId

                    if (startPositionMs > 1000L) {
                        withContext(Dispatchers.IO) {
                            soapClient.seek(device.baseUrl, startPositionMs)
                        }
                    }

                    if (generation != handoffGeneration.get()) return@launch

                    if (playWhenReady) {
                        val playOk =
                            withContext(Dispatchers.IO) {
                                soapClient.play(device.baseUrl)
                            }
                        if (!playOk) {
                            Logger.w(TAG, "Initial soapClient.play() returned false, retrying after 300ms...")
                            delay(300)
                            if (generation != handoffGeneration.get()) return@launch
                            withContext(Dispatchers.IO) { soapClient.play(device.baseUrl) }
                        }
                        sonosPlayer.remoteIsPlaying = true
                        adapter.notifyRemoteIsPlaying(true)
                    }

                    adapter.silenceLocalPlayersForRemote()
                    adapter.notifyRemotePlaybackState(Player.STATE_READY)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Logger.e(TAG, "Error starting playback on Sonos: ${e.message}", e)
                } finally {
                    soapClient.endTransportVolumeAssert()
                }
            }
    }

    private fun advanceAfterTrackEnd() {
        if (advanceJob?.isActive == true) return
        if (playTrackJob?.isActive == true) return
        val device = _connectedDevice.value ?: return

        advanceJob =
            coroutineScope.launch {
                Logger.i(
                    TAG,
                    "Track ended on Sonos (lastPos=${lastPositionWhilePlayingMs}ms " +
                        "dur=${sonosPlayer.remoteDurationMs}ms) — advancing queue",
                )
                sawPlayingForCurrentTrack = false
                lastPositionWhilePlayingMs = 0L
                suppressPollingUntilMs = System.currentTimeMillis() + 4000L

                // hasNextMediaItem is true for REPEAT_ONE (same index) and REPEAT_ALL (wrap).
                if (adapter.hasNextMediaItem()) {
                    sonosPlayer.remoteIsPlaying = true
                    adapter.notifyRemoteIsPlaying(true)
                    adapter.seekToNext()
                } else {
                    sonosPlayer.remoteIsPlaying = false
                    adapter.notifyRemoteIsPlaying(false)
                    adapter.notifyRemotePlaybackState(Player.STATE_ENDED)
                    withContext(Dispatchers.IO) {
                        runCatching { soapClient.pause(device.baseUrl) }
                    }
                }
            }
    }

    private fun startPolling(device: SonosDevice) {
        pollJob?.cancel()
        pollJob =
            coroutineScope.launch {
                var volumePollCycle = 0

                while (isActive && _connectedDevice.value?.id == device.id) {
                    delay(POLL_INTERVAL_MS)

                    volumePollCycle++
                    if (volumePollCycle % 3 == 0 && volumeJob?.isActive != true) {
                        try {
                            val speakerVol = withContext(Dispatchers.IO) { soapClient.getVolume(device.baseUrl) }
                            if (speakerVol != null && volumeJob?.isActive != true) {
                                val volFloat = speakerVol / 100f
                                if (kotlin.math.abs(volFloat - _volume.value) >= 0.02f) {
                                    _volume.value = volFloat
                                    sonosPlayer.remoteVolume = volFloat
                                    adapter.notifyRemoteDeviceVolumeChanged(speakerVol)
                                }
                            }
                        } catch (e: Exception) {
                            Logger.w(TAG, "Error polling Sonos volume: ${e.message}")
                        }
                    }

                    if (System.currentTimeMillis() < suppressPollingUntilMs) {
                        continue
                    }
                    // Don't fight an in-flight handoff.
                    if (playTrackJob?.isActive == true || advanceJob?.isActive == true) {
                        continue
                    }

                    try {
                        val posInfo =
                            withContext(Dispatchers.IO) {
                                soapClient.getPositionInfo(device.baseUrl)
                            }
                        if (posInfo != null) {
                            // Prefer non-zero RelTime; Sonos often reports 0 briefly on STOPPED.
                            if (posInfo.relTimeMs > 0L || !sonosPlayer.remoteIsPlaying) {
                                sonosPlayer.remotePositionMs = posInfo.relTimeMs
                            }
                            if (posInfo.durationMs > 0L) {
                                sonosPlayer.remoteDurationMs = posInfo.durationMs
                            }
                        }

                        val transportState =
                            withContext(Dispatchers.IO) {
                                soapClient.getTransportInfo(device.baseUrl)
                            } ?: continue

                        when {
                            transportState.equals("PLAYING", ignoreCase = true) -> {
                                sawPlayingForCurrentTrack = true
                                if (sonosPlayer.remotePositionMs > 0L) {
                                    lastPositionWhilePlayingMs = sonosPlayer.remotePositionMs
                                }
                                if (!sonosPlayer.remoteIsPlaying) {
                                    sonosPlayer.remoteIsPlaying = true
                                    adapter.notifyRemoteIsPlaying(true)
                                }
                            }
                            transportState.equals("PAUSED_PLAYBACK", ignoreCase = true) -> {
                                if (sonosPlayer.remoteIsPlaying) {
                                    sonosPlayer.remoteIsPlaying = false
                                    adapter.notifyRemoteIsPlaying(false)
                                }
                            }
                            transportState.equals("TRANSITIONING", ignoreCase = true) -> {
                                // Keep state; optionally treat near-end transitioning as end.
                                val dur = sonosPlayer.remoteDurationMs
                                val pos = lastPositionWhilePlayingMs.coerceAtLeast(sonosPlayer.remotePositionMs)
                                if (sawPlayingForCurrentTrack &&
                                    sonosPlayer.remoteIsPlaying &&
                                    dur > 0L &&
                                    pos >= dur - 2000L
                                ) {
                                    advanceAfterTrackEnd()
                                }
                            }
                            transportState.equals("STOPPED", ignoreCase = true) ||
                                transportState.equals("NO_MEDIA_PRESENT", ignoreCase = true) -> {
                                handleStoppedWhileConnected(transportState)
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Logger.d(TAG, "Polling update error: ${e.message}")
                    }
                }
            }
    }

    /**
     * Sonos reports STOPPED at natural end AND often resets RelTime to 0.
     * Never treat that as "paused near start" — use [lastPositionWhilePlayingMs].
     */
    private fun handleStoppedWhileConnected(transportState: String) {
        if (!sonosPlayer.remoteIsPlaying) {
            // User paused / stopped intentionally (or we already advanced).
            return
        }
        if (!sawPlayingForCurrentTrack) {
            // URI set but play not yet observed — don't advance.
            Logger.d(TAG, "STOPPED before PLAYING observed — waiting (state=$transportState)")
            return
        }

        val dur = sonosPlayer.remoteDurationMs
        val lastPos = lastPositionWhilePlayingMs
        val nearEnd = dur > 0L && lastPos >= (dur - END_NEAR_MS).coerceAtLeast(0L)
        val hadMeaningfulProgress = lastPos >= MIN_PROGRESS_FOR_END_MS

        if (nearEnd || hadMeaningfulProgress) {
            Logger.i(
                TAG,
                "STOPPED after playing (lastPos=${lastPos}ms dur=${dur}ms nearEnd=$nearEnd) — end of track",
            )
            advanceAfterTrackEnd()
        } else {
            Logger.d(
                TAG,
                "STOPPED while intending play but little progress (lastPos=${lastPos}ms) — re-play",
            )
            // Likely a failed start; retry current item once.
            playTrack(adapter.currentMediaItemIndex, lastPos, true)
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    companion object {
        private const val TAG = "SonosHandoffManager"
        private const val POLL_INTERVAL_MS = 1000L
        private const val END_NEAR_MS = 5000L
        private const val MIN_PROGRESS_FOR_END_MS = 8000L
    }
}
