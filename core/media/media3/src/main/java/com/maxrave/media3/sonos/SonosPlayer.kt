package com.maxrave.media3.sonos

import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.maxrave.logger.Logger

/**
 * Media3 [Player] implementation for an active Sonos playback session.
 * Extends [ForwardingPlayer] to inherit default player functionality while routing
 * transport controls, position, duration, and volume to the remote Sonos speaker.
 */
@UnstableApi
internal class SonosPlayer(
    private val basePlayer: Player,
    private val onPlayAction: () -> Unit,
    private val onPauseAction: () -> Unit,
    private val onStopAction: () -> Unit,
    private val onSeekAction: (Long) -> Unit,
    private val onVolumeAction: (Float) -> Unit,
    private val onSeekToNextAction: () -> Unit,
    private val onSeekToPreviousAction: () -> Unit,
) : ForwardingPlayer(basePlayer) {

    @Volatile
    var remoteIsPlaying: Boolean = false

    @Volatile
    var remotePositionMs: Long = 0L

    @Volatile
    var remoteDurationMs: Long = 0L

    @Volatile
    var remoteVolume: Float = 0.5f

    override fun play() {
        Logger.d(TAG, "play() called on SonosPlayer")
        remoteIsPlaying = true
        onPlayAction()
    }

    override fun pause() {
        Logger.d(TAG, "pause() called on SonosPlayer")
        remoteIsPlaying = false
        onPauseAction()
    }

    override fun stop() {
        Logger.d(TAG, "stop() called on SonosPlayer")
        remoteIsPlaying = false
        onStopAction()
    }

    override fun seekTo(positionMs: Long) {
        Logger.d(TAG, "seekTo($positionMs) called on SonosPlayer")
        remotePositionMs = positionMs
        onSeekAction(positionMs)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        Logger.d(TAG, "seekTo($mediaItemIndex, $positionMs) called on SonosPlayer")
        seekTo(positionMs)
    }

    override fun seekToNext() {
        Logger.d(TAG, "seekToNext() called on SonosPlayer")
        onSeekToNextAction()
    }

    override fun seekToPrevious() {
        Logger.d(TAG, "seekToPrevious() called on SonosPlayer")
        onSeekToPreviousAction()
    }

    override fun isPlaying(): Boolean = remoteIsPlaying

    override fun getCurrentPosition(): Long = remotePositionMs

    override fun getDuration(): Long = remoteDurationMs

    override fun getBufferedPosition(): Long = remotePositionMs

    override fun getVolume(): Float = remoteVolume

    /**
     * Soft/player volume API. Must NOT write to the Sonos speaker.
     * ExoPlayer soft volume (0..1 gain) is a different concept from Sonos absolute
     * device volume; conflating them caused unprompted MAX blasts on skip/scrub.
     * Speaker volume changes go through [setDeviceVolume] / [increaseDeviceVolume]
     * (MediaSession HW keys) or SonosHandoffManager.setVolume (in-app slider).
     */
    override fun setVolume(volume: Float) {
        Logger.w(TAG, "Ignoring soft setVolume($volume) on SonosPlayer — speaker volume is device-volume only")
    }

    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_GET_DEVICE_VOLUME)
            .add(Player.COMMAND_SET_DEVICE_VOLUME)
            .add(Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS)
            .add(Player.COMMAND_ADJUST_DEVICE_VOLUME)
            .add(Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS)
            .build()

    override fun isCommandAvailable(command: Int): Boolean =
        when (command) {
            Player.COMMAND_GET_DEVICE_VOLUME,
            Player.COMMAND_SET_DEVICE_VOLUME,
            Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
            Player.COMMAND_ADJUST_DEVICE_VOLUME,
            Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS -> true
            else -> super.isCommandAvailable(command)
        }

    override fun getDeviceInfo(): DeviceInfo =
        DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_REMOTE)
            .setMinVolume(0)
            .setMaxVolume(100)
            // Helps MediaSession / MediaRouter associate this with an external sink
            // so OEMs (Samsung) show the cast/TV external volume overlay.
            .setRoutingControllerId("sonos")
            .build()

    override fun getDeviceVolume(): Int = (remoteVolume * 100).toInt().coerceIn(0, 100)

    override fun isDeviceMuted(): Boolean = remoteVolume <= 0f

    override fun setDeviceVolume(volume: Int) {
        applyDeviceVolume(volume, fromUserHardwareOrSession = true)
    }

    override fun setDeviceVolume(volume: Int, flags: Int) {
        applyDeviceVolume(volume, fromUserHardwareOrSession = true)
    }

    /**
     * Apply absolute Sonos device volume (0..100).
     * Called from MediaSession / HW volume keys ([fromUserHardwareOrSession]=true) —
     * that path is intentional user input, so MAX is allowed.
     * Programmatic callers must not use this to "reset" volume on transport events.
     */
    fun applyDeviceVolume(volume: Int, fromUserHardwareOrSession: Boolean) {
        val clampedInt = volume.coerceIn(0, 100)
        val floatVol = clampedInt / 100f
        // Match SOAP hard gate: unprompted writes at/above 85% are refused here too.
        if (!fromUserHardwareOrSession && clampedInt >= SonosSoapClient.HIGH_VOLUME_THRESHOLD) {
            Logger.e(TAG, "REFUSING unprompted Sonos device volume write to $clampedInt%")
            return
        }
        remoteVolume = floatVol
        onVolumeAction(floatVol)
    }

    override fun increaseDeviceVolume() {
        // Finer steps (±2) so Samsung/Android remote volume bar feels Spotify-like.
        val next = (getDeviceVolume() + VOLUME_STEP).coerceAtMost(100)
        setDeviceVolume(next)
    }

    override fun increaseDeviceVolume(flags: Int) {
        // flags from MediaSession VolumeProvider (includes SHOW_UI on the system side).
        increaseDeviceVolume()
    }

    override fun decreaseDeviceVolume() {
        val prev = (getDeviceVolume() - VOLUME_STEP).coerceAtLeast(0)
        setDeviceVolume(prev)
    }

    override fun decreaseDeviceVolume(flags: Int) {
        decreaseDeviceVolume()
    }

    override fun setDeviceMuted(muted: Boolean) {
        if (muted) setDeviceVolume(0)
    }

    override fun setDeviceMuted(muted: Boolean, flags: Int) {
        setDeviceMuted(muted)
    }

    companion object {
        private const val TAG = "SonosPlayer"
        private const val VOLUME_STEP = 2
    }
}
