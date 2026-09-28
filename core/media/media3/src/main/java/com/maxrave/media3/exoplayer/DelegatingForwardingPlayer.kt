package com.maxrave.media3.exoplayer

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.maxrave.logger.Logger

private const val TAG = "DelegatingForwardingPlayer"

/**
 * A [ForwardingPlayer] that allows runtime swapping of its underlying delegate.
 *
 * Used for MediaSession integration - provides a stable [Player] reference while
 * [CrossfadeExoPlayerAdapter] cycles through different ExoPlayer instances.
 *
 * Pattern: Track all externally added listeners, then during swap:
 * 1. Remove them via [ForwardingPlayer.removeListener] (cleans up ForwardingListener wrappers)
 * 2. Swap the internal `player` field via reflection
 * 3. Re-add them via [ForwardingPlayer.addListener] (creates new ForwardingListener wrappers for new delegate)
 *
 * Additionally, since each underlying ExoPlayer only has a single MediaItem,
 * playlist navigation methods (hasNext/hasPrevious, seek, mediaItemCount, etc.)
 * are overridden to delegate to [PlaylistNavigationProvider] — which is backed by
 * [CrossfadeExoPlayerAdapter]'s internal playlist. This ensures MediaSession reports
 * correct available commands (SEEK_TO_NEXT, SEEK_TO_PREVIOUS) and shows
 * next/previous buttons in the system notification.
 */
@UnstableApi
internal class DelegatingForwardingPlayer(
    initialDelegate: Player,
) : ForwardingPlayer(initialDelegate) {
    companion object {
        private val PLAYER_FIELD: java.lang.reflect.Field? =
            try {
                ForwardingPlayer::class.java.getDeclaredField("player").apply {
                    isAccessible = true
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to access ForwardingPlayer.player field", e)
                null
            }
    }

    // ========== Playlist Navigation Provider ==========

    /**
     * Provides playlist-level navigation information to the ForwardingPlayer.
     *
     * Each underlying ExoPlayer only has 1 MediaItem, so ExoPlayer's own
     * hasNextMediaItem()/hasPreviousMediaItem() always return false.
     * This provider bridges the gap, letting MediaSession see the full playlist state.
     */
    interface PlaylistNavigationProvider {
        fun hasNextMediaItem(): Boolean

        fun hasPreviousMediaItem(): Boolean

        fun seekToNext()

        fun seekToPrevious()

        /**
         * Always advances to the previous media item — bypasses the 3-second
         * "seek to start" rule used by [seekToPrevious]. Implementations may
         * default to [seekToPrevious] if the distinction is irrelevant.
         */
        fun seekToPreviousMediaItem() = seekToPrevious()
    }

    /**
     * Set by [CrossfadeExoPlayerAdapter] to provide playlist navigation.
     * When null, all navigation methods fall back to the underlying ExoPlayer (single-item behavior).
     */
    var playlistNavigationProvider: PlaylistNavigationProvider? = null

    /**
     * When non-null (Sonos / remote handoff active), MediaSession sees PLAYBACK_TYPE_REMOTE
     * and routes HW volume keys / system volume UI to this control instead of STREAM_MUSIC.
     * Cleared on disconnect so local volume behaviour is restored.
     */
    interface RemoteDeviceVolumeControl {
        fun deviceInfo(): DeviceInfo

        fun deviceVolume(): Int

        fun deviceMuted(): Boolean

        fun setDeviceVolume(volume: Int, flags: Int)

        fun increaseDeviceVolume(flags: Int)

        fun decreaseDeviceVolume(flags: Int)

        fun setDeviceMuted(muted: Boolean, flags: Int)
    }

    /**
     * When non-null, MediaSession / system transport commands on this session player
     * go to the remote (Sonos) — never to the wrapped local ExoPlayer. This is what
     * prevents dual-pipeline desync (phone + speaker playing different items).
     */
    interface RemoteTransportControl {
        fun play()

        fun pause()

        fun stop()

        fun seekTo(positionMs: Long)

        fun isPlaying(): Boolean

        fun getPlayWhenReady(): Boolean

        fun setPlayWhenReady(playWhenReady: Boolean)

        fun getCurrentPosition(): Long

        fun getDuration(): Long

        fun getPlaybackState(): Int
    }

    @Volatile
    var remoteDeviceVolumeControl: RemoteDeviceVolumeControl? = null

    @Volatile
    var remoteTransportControl: RemoteTransportControl? = null

    override fun getDeviceInfo(): DeviceInfo =
        remoteDeviceVolumeControl?.deviceInfo() ?: super.getDeviceInfo()

    override fun getDeviceVolume(): Int =
        remoteDeviceVolumeControl?.deviceVolume() ?: super.getDeviceVolume()

    override fun isDeviceMuted(): Boolean =
        remoteDeviceVolumeControl?.deviceMuted() ?: super.isDeviceMuted()

    override fun setDeviceVolume(volume: Int) {
        remoteDeviceVolumeControl?.setDeviceVolume(volume, /* flags= */ 0) ?: super.setDeviceVolume(volume)
    }

    override fun setDeviceVolume(volume: Int, flags: Int) {
        remoteDeviceVolumeControl?.setDeviceVolume(volume, flags) ?: super.setDeviceVolume(volume, flags)
    }

    override fun increaseDeviceVolume() {
        remoteDeviceVolumeControl?.increaseDeviceVolume(/* flags= */ 0) ?: super.increaseDeviceVolume()
    }

    override fun increaseDeviceVolume(flags: Int) {
        remoteDeviceVolumeControl?.increaseDeviceVolume(flags) ?: super.increaseDeviceVolume(flags)
    }

    override fun decreaseDeviceVolume() {
        remoteDeviceVolumeControl?.decreaseDeviceVolume(/* flags= */ 0) ?: super.decreaseDeviceVolume()
    }

    override fun decreaseDeviceVolume(flags: Int) {
        remoteDeviceVolumeControl?.decreaseDeviceVolume(flags) ?: super.decreaseDeviceVolume(flags)
    }

    override fun setDeviceMuted(muted: Boolean) {
        remoteDeviceVolumeControl?.setDeviceMuted(muted, /* flags= */ 0) ?: super.setDeviceMuted(muted)
    }

    override fun setDeviceMuted(muted: Boolean, flags: Int) {
        remoteDeviceVolumeControl?.setDeviceMuted(muted, flags) ?: super.setDeviceMuted(muted, flags)
    }

    // ========== Remote transport (Sonos sole owner) ==========

    override fun play() {
        remoteTransportControl?.play() ?: super.play()
    }

    override fun pause() {
        remoteTransportControl?.pause() ?: super.pause()
    }

    override fun stop() {
        remoteTransportControl?.stop() ?: super.stop()
    }

    override fun seekTo(positionMs: Long) {
        remoteTransportControl?.seekTo(positionMs) ?: super.seekTo(positionMs)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        // Playlist index seeks stay on PlaylistNavigationProvider / adapter; only
        // same-item scrub is remote-owned here. Index changes are serialized there.
        if (remoteTransportControl != null && mediaItemIndex == getCurrentMediaItemIndex()) {
            remoteTransportControl?.seekTo(positionMs)
        } else {
            super.seekTo(mediaItemIndex, positionMs)
        }
    }

    override fun isPlaying(): Boolean =
        remoteTransportControl?.isPlaying() ?: super.isPlaying()

    override fun getPlayWhenReady(): Boolean =
        remoteTransportControl?.getPlayWhenReady() ?: super.getPlayWhenReady()

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        remoteTransportControl?.setPlayWhenReady(playWhenReady) ?: super.setPlayWhenReady(playWhenReady)
    }

    override fun getCurrentPosition(): Long =
        remoteTransportControl?.getCurrentPosition() ?: super.getCurrentPosition()

    override fun getDuration(): Long =
        remoteTransportControl?.getDuration() ?: super.getDuration()

    override fun getBufferedPosition(): Long =
        remoteTransportControl?.getCurrentPosition() ?: super.getBufferedPosition()

    override fun getPlaybackState(): Int {
        remoteTransportControl?.let { return it.getPlaybackState() }
        val state = super.getPlaybackState()
        if (state == Player.STATE_ENDED && suppressPlaybackEnded) {
            return Player.STATE_BUFFERING
        }
        return state
    }

    /**
     * Tell MediaSession the session player is now remote (or local again) so it
     * rebuilds VolumeProviderCompat via setPlaybackToRemote / setPlaybackToLocal.
     * Without this, Samsung keeps showing STREAM_MUSIC instead of the cast/TV overlay.
     */
    fun notifyRemoteRoutingChanged() {
        val deviceInfo = getDeviceInfo()
        val volume = getDeviceVolume()
        val muted = isDeviceMuted()
        val commands = getAvailableCommands()
        Logger.i(
            TAG,
            "notifyRemoteRoutingChanged: playbackType=${deviceInfo.playbackType} " +
                "vol=$volume remoteTransport=${remoteTransportControl != null} " +
                "remoteVolume=${remoteDeviceVolumeControl != null}",
        )
        trackedListeners.forEach { listener ->
            try {
                listener.onDeviceInfoChanged(deviceInfo)
                listener.onDeviceVolumeChanged(volume, muted)
                listener.onAvailableCommandsChanged(commands)
            } catch (e: Exception) {
                Logger.w(TAG, "Error notifying remote routing change: ${e.message}")
            }
        }
    }

    // ========== Playback-Ended Suppression ==========

    /**
     * When true, [getPlaybackState] remaps [Player.STATE_ENDED] to [Player.STATE_BUFFERING].
     *
     * This prevents [androidx.media3.session.MediaLibraryService] from dropping the
     * foreground-service notification during the gap between one ExoPlayer finishing
     * and the next ExoPlayer being swapped in via [swapDelegate].
     *
     * Set by [CrossfadeExoPlayerAdapter] before a track-end transition starts,
     * cleared after the next player is swapped in and playing.
     */
    @Volatile
    var suppressPlaybackEnded = false


    // ========== Listener Tracking ==========

    // Track all externally registered listeners so we can re-register them after delegate swap
    private val trackedListeners = mutableListOf<Player.Listener>()

    override fun addListener(listener: Player.Listener) {
        trackedListeners.add(listener)
        super.addListener(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        trackedListeners.remove(listener)
        super.removeListener(listener)
    }

    // ========== Video Surface Tracking ==========
    // Track the current video output so it can be re-attached when the delegate is swapped.
    // Without this, video stops rendering after a delegate swap because the new ExoPlayer
    // instance never receives setVideoSurfaceView/setVideoSurface/etc.

    private sealed class VideoOutput {
        data class SurfaceViewOutput(val surfaceView: SurfaceView) : VideoOutput()

        data class TextureViewOutput(val textureView: TextureView) : VideoOutput()

        data class SurfaceOutput(val surface: Surface) : VideoOutput()

        data class SurfaceHolderOutput(val surfaceHolder: SurfaceHolder) : VideoOutput()
    }

    private var currentVideoOutput: VideoOutput? = null

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        currentVideoOutput = surfaceView?.let { VideoOutput.SurfaceViewOutput(it) }
        super.setVideoSurfaceView(surfaceView)
    }

    override fun setVideoTextureView(textureView: TextureView?) {
        currentVideoOutput = textureView?.let { VideoOutput.TextureViewOutput(it) }
        super.setVideoTextureView(textureView)
    }

    override fun setVideoSurface(surface: Surface?) {
        currentVideoOutput = surface?.let { VideoOutput.SurfaceOutput(it) }
        super.setVideoSurface(surface)
    }

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        currentVideoOutput = surfaceHolder?.let { VideoOutput.SurfaceHolderOutput(it) }
        super.setVideoSurfaceHolder(surfaceHolder)
    }

    override fun clearVideoSurface() {
        currentVideoOutput = null
        super.clearVideoSurface()
    }

    override fun clearVideoSurface(surface: Surface?) {
        if (currentVideoOutput is VideoOutput.SurfaceOutput &&
            (currentVideoOutput as VideoOutput.SurfaceOutput).surface === surface
        ) {
            currentVideoOutput = null
        }
        super.clearVideoSurface(surface)
    }

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
        if (currentVideoOutput is VideoOutput.SurfaceViewOutput &&
            (currentVideoOutput as VideoOutput.SurfaceViewOutput).surfaceView === surfaceView
        ) {
            currentVideoOutput = null
        }
        super.clearVideoSurfaceView(surfaceView)
    }

    override fun clearVideoTextureView(textureView: TextureView?) {
        if (currentVideoOutput is VideoOutput.TextureViewOutput &&
            (currentVideoOutput as VideoOutput.TextureViewOutput).textureView === textureView
        ) {
            currentVideoOutput = null
        }
        super.clearVideoTextureView(textureView)
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        if (currentVideoOutput is VideoOutput.SurfaceHolderOutput &&
            (currentVideoOutput as VideoOutput.SurfaceHolderOutput).surfaceHolder === surfaceHolder
        ) {
            currentVideoOutput = null
        }
        super.clearVideoSurfaceHolder(surfaceHolder)
    }

    /**
     * Clear video output from a specific player instance.
     * Must be called on the OLD delegate before swapping, so the native surface
     * is disconnected from the old MediaCodec before the new player tries to connect.
     */
    private fun clearVideoOutputFromPlayer(player: Player) {
        if (currentVideoOutput != null) {
            Logger.d(TAG, "Clearing video surface from old delegate before swap")
            try {
                player.clearVideoSurface()
            } catch (e: Exception) {
                Logger.w(TAG, "Error clearing video surface from old delegate: ${e.message}")
            }
        }
    }

    /**
     * Re-attach the tracked video output to the current delegate.
     * Called after [swapDelegate] to ensure video continues rendering on the new ExoPlayer.
     */
    private fun reAttachVideoOutput() {
        when (val output = currentVideoOutput) {
            is VideoOutput.SurfaceViewOutput -> {
                Logger.d(TAG, "Re-attaching SurfaceView to new delegate")
                wrappedPlayer.setVideoSurfaceView(output.surfaceView)
            }
            is VideoOutput.TextureViewOutput -> {
                Logger.d(TAG, "Re-attaching TextureView to new delegate")
                wrappedPlayer.setVideoTextureView(output.textureView)
            }
            is VideoOutput.SurfaceOutput -> {
                Logger.d(TAG, "Re-attaching Surface to new delegate")
                wrappedPlayer.setVideoSurface(output.surface)
            }
            is VideoOutput.SurfaceHolderOutput -> {
                Logger.d(TAG, "Re-attaching SurfaceHolder to new delegate")
                wrappedPlayer.setVideoSurfaceHolder(output.surfaceHolder)
            }
            null -> {
                // No video output to re-attach
            }
        }
    }

    // ========== Playlist Navigation Overrides ==========

    override fun getAvailableCommands(): Player.Commands {
        var baseCommands = super.getAvailableCommands()
        if (remoteDeviceVolumeControl != null) {
            baseCommands =
                baseCommands
                    .buildUpon()
                    .add(Player.COMMAND_GET_DEVICE_VOLUME)
                    .add(Player.COMMAND_SET_DEVICE_VOLUME)
                    .add(Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS)
                    .add(Player.COMMAND_ADJUST_DEVICE_VOLUME)
                    .add(Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS)
                    .build()
        }
        val nav = playlistNavigationProvider ?: return baseCommands

        val builder = baseCommands.buildUpon()

        // Always add seek-to-previous (allows seek to start of track even if no previous item)
        builder.add(Player.COMMAND_SEEK_TO_PREVIOUS)

        if (nav.hasNextMediaItem()) {
            builder.add(Player.COMMAND_SEEK_TO_NEXT)
            builder.add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        }
        if (nav.hasPreviousMediaItem()) {
            builder.add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        }

        return builder.build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        if (remoteDeviceVolumeControl != null) {
            when (command) {
                Player.COMMAND_GET_DEVICE_VOLUME,
                Player.COMMAND_SET_DEVICE_VOLUME,
                Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
                Player.COMMAND_ADJUST_DEVICE_VOLUME,
                Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS,
                -> return true
            }
        }
        val nav = playlistNavigationProvider
        if (nav != null) {
            when (command) {
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ->
                    return nav.hasNextMediaItem()
                Player.COMMAND_SEEK_TO_PREVIOUS ->
                    return true // Always allow seeking to start of current track
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ->
                    return nav.hasPreviousMediaItem()
            }
        }
        return super.isCommandAvailable(command)
    }

    override fun hasNextMediaItem(): Boolean =
        playlistNavigationProvider?.hasNextMediaItem() ?: super.hasNextMediaItem()

    override fun hasPreviousMediaItem(): Boolean =
        playlistNavigationProvider?.hasPreviousMediaItem() ?: super.hasPreviousMediaItem()

    override fun seekToNext() {
        val nav = playlistNavigationProvider
        if (nav != null) {
            nav.seekToNext()
        } else {
            super.seekToNext()
        }
    }

    override fun seekToPrevious() {
        val nav = playlistNavigationProvider
        if (nav != null) {
            nav.seekToPrevious()
        } else {
            super.seekToPrevious()
        }
    }

    override fun seekToNextMediaItem() {
        val nav = playlistNavigationProvider
        if (nav != null) {
            nav.seekToNext()
        } else {
            super.seekToNextMediaItem()
        }
    }

    override fun seekToPreviousMediaItem() {
        val nav = playlistNavigationProvider
        if (nav != null) {
            // Bypass the 3-second "seek to start" rule used by seekToPrevious().
            nav.seekToPreviousMediaItem()
        } else {
            super.seekToPreviousMediaItem()
        }
    }

    // NOTE: Do NOT override getMediaItemCount() or getCurrentMediaItemIndex() here.
    // These must remain consistent with the underlying ExoPlayer's Timeline (1 item, index 0).
    // Media3's PlayerWrapper.createPositionInfo() validates that currentMediaItemIndex < timeline.windowCount.
    // If we return the adapter's playlist index (e.g. 5) but Timeline only has 1 window, it crashes.

    // ========== Delegate Swap ==========

    /**
     * Swap the underlying delegate player.
     *
     * This properly migrates all registered listeners (e.g., MediaSession's listener)
     * from the old delegate to the new one.
     */
    fun swapDelegate(newDelegate: Player) {
        if (wrappedPlayer === newDelegate) return

        val field = PLAYER_FIELD
            ?: throw IllegalStateException("Cannot swap delegate - reflection on ForwardingPlayer.player field failed")

        // 1. Snapshot current listeners
        val listenersToReAdd = trackedListeners.toList()

        // 2. Clear video surface from OLD delegate BEFORE swapping.
        //    The native surface can only be connected to one MediaCodec at a time.
        //    If we don't clear it here, the new player's MediaCodec.setSurface() will fail
        //    with "already connected" → IllegalArgumentException crash.
        clearVideoOutputFromPlayer(wrappedPlayer)

        // 3. Remove all listeners from old delegate (ForwardingPlayer removes ForwardingListener wrappers)
        listenersToReAdd.forEach { listener ->
            try {
                super.removeListener(listener)
            } catch (e: Exception) {
                Logger.w(TAG, "Error removing listener during swap: ${e.message}")
            }
        }

        // 4. Swap the private final `player` field
        field.set(this, newDelegate)

        // 5. Re-add all listeners (ForwardingPlayer creates new ForwardingListener wrappers for new delegate)
        listenersToReAdd.forEach { listener ->
            super.addListener(listener)
        }

        // 6. Re-attach video surface to the new delegate
        //    Without this, video stops rendering because the new ExoPlayer
        //    never received setVideoSurfaceView/setVideoSurface/etc.
        reAttachVideoOutput()

        // 6. Verify
        if (wrappedPlayer !== newDelegate) {
            Logger.e(TAG, "Delegate swap verification FAILED - wrappedPlayer is not the new delegate!")
        } else {
            Logger.d(TAG, "Delegate swapped successfully")
        }
    }

    // ========== Manual Event Dispatch ==========

    /**
     * Manually notify all tracked listeners about a media item change.
     *
     * This is needed after [swapDelegate] because the new delegate may already have
     * a MediaItem set and be playing — meaning listeners missed the initial
     * [Player.Listener.onMediaItemTransition] and [Player.Listener.onMediaMetadataChanged] events.
     *
     * Also dispatches [Player.Listener.onAvailableCommandsChanged] so MediaSession
     * re-evaluates which notification buttons to show (next/previous).
     *
     * Primary use case: crossfade transitions, where the secondary player is prepared
     * (with MediaItem + prepare()) before the ForwardingPlayer is swapped to it.
     * MediaSession uses these events to update the system notification metadata.
     */
    fun notifyMediaItemChanged() {
        val player = wrappedPlayer
        val mediaItem = player.currentMediaItem ?: MediaItem.EMPTY
        val metadata = player.mediaMetadata
        val commands = getAvailableCommands()

        Logger.d(TAG, "Manually notifying ${trackedListeners.size} listeners about media item change: ${metadata.title}")

        trackedListeners.forEach { listener ->
            try {
                listener.onMediaItemTransition(mediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
                listener.onMediaMetadataChanged(metadata)
                listener.onAvailableCommandsChanged(commands)
            } catch (e: Exception) {
                Logger.w(TAG, "Error notifying listener about media item change: ${e.message}")
            }
        }
    }
}
