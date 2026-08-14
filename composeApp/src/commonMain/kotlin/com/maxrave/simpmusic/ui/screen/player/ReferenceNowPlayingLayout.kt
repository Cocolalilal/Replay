@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.rememberPaletteState
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.expect.ui.PlatformCastButton
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.toAppleMusicTintColor
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.FavoriteBorder
import com.maxrave.simpmusic.ui.icon.Forward5
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.icon.Lyrics
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Replay5
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.ui.icon.Subtitles
import com.maxrave.simpmusic.ui.icon.SubtitlesOff
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.player.FullscreenDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.unavailable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

// Pure white: text, icons and active glyphs.
private val ReferenceText = Color.White
private val ReferenceMutedText = Color.White.copy(alpha = 0.65f)

// Fallback accents before the layout provides the artwork-derived ones.
private val ReferenceFallbackPrimary = Color(0xFFF4F3F0)

// The first colour: the artwork tint at 30% opacity. Used by button fills (heart, more,
// play/pause) and the played part of the progress bar.
private val LocalReferencePrimary = staticCompositionLocalOf { ReferenceFallbackPrimary.copy(alpha = 0.3f) }

// The second colour: the artwork tint at 20% opacity. Used by backing surfaces — the bottom pill
// container, the previous/next buttons and the unplayed track.
private val LocalReferenceSecondary = staticCompositionLocalOf { ReferenceFallbackPrimary.copy(alpha = 0.2f) }

// The SOLID artwork tint (no opacity). Used inside the progress bar's merged offscreen layer.
private val LocalReferenceTint = staticCompositionLocalOf { ReferenceFallbackPrimary }

// The bottom pill's three buttons. The container behind them already carries the tint at 20%,
// so the buttons use ~12.5% themselves — the two stacked opacities read optically as the same
// 30% as the play/pause button instead of a brighter ~44%.
private val LocalReferenceSegment = staticCompositionLocalOf { ReferenceFallbackPrimary.copy(alpha = 0.125f) }

// Shared-element plumbing for the cover art: the big player artwork morphs into the small header
// artwork when switching tabs. Provided by the tab AnimatedContent.
private val LocalSharedArtworkState = staticCompositionLocalOf<SharedTransitionScope.SharedContentState?> { null }
private val LocalSharedArtworkScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }
private val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

// Dark icon tint drawn ON TOP of a selected (white) surface.
private val ReferenceSegmentActiveTint = Color(0xFF23231F)

// Solid white surface for SELECTED controls (bottom/queue segment, liked heart).
private val ReferenceSelectedSurface = Color.White

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun ReferenceNowPlayingLayout(
    sharedViewModel: SharedViewModel,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val queueDataState by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val shouldShowVideo by sharedViewModel.getVideo.collectAsStateWithLifecycle()
    val mediaPlayerHandler: MediaPlayerHandler = koinInject()
    val isInPipMode = com.maxrave.simpmusic.extension.rememberIsInPipMode()
    val currentVideoId = sharedViewModel.nowPlayingState.value?.track?.videoId

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showOverflow by rememberSaveable { mutableStateOf(false) }

    // Seek state, in fractions (0f..1f). The held target keeps the thumb where the user
    // dropped it until real playback catches up (same thresholds as PixelPlayer's bar).
    var isSliding by rememberSaveable { mutableStateOf(false) }
    var sliderValue by rememberSaveable { mutableFloatStateOf(0f) }
    var targetSeekFraction by rememberSaveable { mutableFloatStateOf(-1f) }
    var lastSeekFinishedAt by rememberSaveable { mutableStateOf(0L) }
    LaunchedEffect(timelineState, isSliding) {
        if (!isSliding) {
            val actualProgress =
                if (timelineState.total > 0L) {
                    timelineState.current.toFloat() / timelineState.total.toFloat()
                } else {
                    0f
                }
            if (
                targetSeekFraction >= 0f &&
                (kotlin.math.abs(actualProgress - targetSeekFraction) < 0.04f ||
                    System.currentTimeMillis() - lastSeekFinishedAt > 5000L)
            ) {
                targetSeekFraction = -1f
            }
            sliderValue = if (targetSeekFraction >= 0f) targetSeekFraction else actualProgress
        }
    }
    LaunchedEffect(screenDataState.thumbnailURL) {
        targetSeekFraction = -1f
        sliderValue = 0f
    }

    val paletteState = rememberPaletteState()
    LaunchedEffect(screenDataState.bitmap) {
        screenDataState.bitmap?.let { paletteState.generate(it) }
    }
    // The tint family comes from the cover art's strongest colour, with saturation quartered and
    // brightness maxed out (see UIExt.toAppleMusicTintColor): a soft pale tint, never a vivid fill.
    // The tint animates very slowly (1.5s) so a song change melts the whole control palette into
    // the next artwork's colours instead of snapping.
    val tintColor = paletteState.palette.toAppleMusicTintColor()
    val animatedTint by androidx.compose.animation.animateColorAsState(
        targetValue = tintColor,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "nowPlayingTint",
    )

    // The first colour is the tint at 30% opacity (button fills, played progress); the second is
    // the tint at 20% opacity (backing surfaces, unplayed progress). Nothing is solid: icons stay
    // white unless a button is SELECTED, in which case the button turns solid white and the icon
    // goes dark.
    val referencePrimary = animatedTint.copy(alpha = 0.30f)
    val referenceSecondary = animatedTint.copy(alpha = 0.20f)

    // The backdrop artwork animates only when the user hasn't disabled it AND the device is not
    // fatigued (low frame rate, battery saver or thermal throttling — see PerformanceMonitor.kt).
    val animatedBackgroundEnabled by
        sharedViewModel
            .getAnimatedNowPlayingBackground()
            .map { it == com.maxrave.domain.manager.DataStoreManager.TRUE }
            .collectAsStateWithLifecycle(initialValue = true)
    val isAnimationFatigued by rememberIsAnimationFatigued()
    val isBackgroundAnimationEnabled = animatedBackgroundEnabled && !isAnimationFatigued

    val animatedArtwork = screenDataState.animatedArtworkData
    val hasAnimatedArtwork = animatedArtwork != null && !isInPipMode
    val queue = queueDataState?.data?.listTracks.orEmpty()
    val topInset = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    val bottomInset = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }

    val navigateToArtist: () -> Unit = {
        val song = sharedViewModel.nowPlayingState.value?.songEntity
        val channelId =
            song?.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() }
                ?: screenDataState.songInfoData?.authorId
        channelId?.let {
            onDismiss()
            navController.navigate(ArtistDestination(channelId = it))
        }
    }

    if (showOverflow) {
        NowPlayingBottomSheet(
            onDismiss = { showOverflow = false },
            navController = navController,
            onNavigateToOtherScreen = onDismiss,
            song = null,
            setSleepTimerEnable = true,
            changeMainLyricsProviderEnable = true,
        )
    }

    CompositionLocalProvider(
        LocalReferencePrimary provides referencePrimary,
        LocalReferenceSecondary provides referenceSecondary,
        LocalReferenceTint provides animatedTint,
        LocalReferenceSegment provides animatedTint.copy(alpha = 0.125f),
    ) {
        // SharedTransitionLayout hosts the cover-art morph: the big player artwork and the small
        // header artwork are the same shared element, so switching tabs MOVES the art to its new
        // position instead of fading it out and back in.
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .background(Color.Black),
            ) {
                ReferenceBackdrop(
                    hasAnimatedArtwork = hasAnimatedArtwork,
                    animatedArtwork = animatedArtwork,
                    artworkUrl = screenDataState.thumbnailURL,
                    artworkBitmap = screenDataState.bitmap,
                    paletteColor = animatedTint,
                    mode =
                        when (selectedTab) {
                            1 -> BackgroundMode.LYRICS
                            2 -> BackgroundMode.QUEUE
                            else -> BackgroundMode.PLAYER
                        },
                    isAnimationEnabled = isBackgroundAnimationEnabled,
                )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = topInset, bottom = bottomInset),
            ) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                ) {
                    // Tab switch: the outgoing page fades out quickly and the incoming one only
                    // starts appearing as it goes — a short, tight crossfade instead of a long
                    // overlap, so text never lingers over the new page. The cover art still
                    // morphs via the shared element.
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn(tween(400, easing = FastOutSlowInEasing, delayMillis = 200)) togetherWith
                                fadeOut(tween(300, easing = FastOutSlowInEasing)))
                                .using(SizeTransform(clip = false))
                        },
                        label = "nowPlayingTab",
                    ) { tab ->
                        val sharedArtworkState = rememberSharedContentState("nowPlayingArtwork")
                        CompositionLocalProvider(
                            LocalSharedArtworkState provides sharedArtworkState,
                            LocalSharedArtworkScope provides this@AnimatedContent,
                        ) {
                            when (tab) {
                                1 -> {
                                    ReferenceLyricsPage(
                                        screenDataState = screenDataState,
                                        controllerState = controllerState,
                                        shouldShowVideo = shouldShowVideo,
                                        isInPipMode = isInPipMode,
                                        timelineValue = timelineState,
                                        onLike = { sharedViewModel.onUIEvent(UIEvent.ToggleLike) },
                                        onMore = { showOverflow = true },
                                        onArtistClick = navigateToArtist,
                                        onLineClick = { progress ->
                                            sharedViewModel.onUIEvent(UIEvent.UpdateProgress(progress))
                                        },
                                        onArtworkLoaded = { sharedViewModel.setBitmap(it) },
                                        timeLine = sharedViewModel.timeline,
                                    )
                                }

                                2 -> {
                                    ReferenceQueuePage(
                                        screenDataState = screenDataState,
                                        controllerState = controllerState,
                                        queue = queue,
                                        mediaPlayerHandler = mediaPlayerHandler,
                                        shouldShowVideo = shouldShowVideo,
                                        isInPipMode = isInPipMode,
                                        timelineValue = timelineState,
                                        onLike = { sharedViewModel.onUIEvent(UIEvent.ToggleLike) },
                                        onMore = { showOverflow = true },
                                        onArtistClick = navigateToArtist,
                                        onShuffle = { sharedViewModel.onUIEvent(UIEvent.Shuffle) },
                                        onRepeat = { sharedViewModel.onUIEvent(UIEvent.Repeat) },
                                        currentVideoId = currentVideoId,
                                        onArtworkLoaded = { sharedViewModel.setBitmap(it) },
                                    )
                                }

                                else -> {
                                    Crossfade(targetState = hasAnimatedArtwork, label = "playerArtworkMode") { animated ->
                                        ReferencePlayerPage(
                                            screenDataState = screenDataState,
                                            controllerState = controllerState,
                                            hasAnimatedArtwork = animated,
                                            shouldShowVideo = shouldShowVideo,
                                            isInPipMode = isInPipMode,
                                            timeLine = timelineState,
                                            onFullscreen = {
                                                onDismiss()
                                                navController.navigate(FullscreenDestination)
                                            },
                                            onBackward = { sharedViewModel.onUIEvent(UIEvent.Backward) },
                                            onForward = { sharedViewModel.onUIEvent(UIEvent.Forward) },
                                            onLike = { sharedViewModel.onUIEvent(UIEvent.ToggleLike) },
                                            onMore = { showOverflow = true },
                                            onArtistClick = navigateToArtist,
                                            onArtworkLoaded = { sharedViewModel.setBitmap(it) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            ReferenceProgressBar(
                value = sliderValue,
                enabled = true,
                isPlaying = controllerState.isPlaying,
                isSliding = isSliding,
                totalDurationMs = timelineState.total,
                onValueChange = {
                    isSliding = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    isSliding = false
                    targetSeekFraction = sliderValue
                    lastSeekFinishedAt = System.currentTimeMillis()
                    sharedViewModel.onUIEvent(UIEvent.UpdateProgress(sliderValue * 100f))
                },
            )

            Spacer(Modifier.height(4.dp))

            ReferenceTransportControls(
                controllerState = controllerState,
                onPrevious = { sharedViewModel.onUIEvent(UIEvent.Previous) },
                onPlayPause = { sharedViewModel.onUIEvent(UIEvent.PlayPause) },
                onNext = { sharedViewModel.onUIEvent(UIEvent.Next) },
            )

            Spacer(Modifier.height(6.dp))

            ReferenceBottomNavigation(
                selectedTab = selectedTab,
                onSelect = { tab ->
                    selectedTab = if (selectedTab == tab) 0 else tab
                },
            )
        }
        }
            }
    }
    }
}

/**
 * Apple Music-style backdrop. When animated artwork is present it plays fullscreen (the sketch
 * "with animated cover"), with a darkening gradient on top. Otherwise it is the twisted+blurred
 * artwork of [AppleMusicBackground]: slow, muted on the player tab, brighter on lyrics/queue,
 * animated only while [isAnimationEnabled].
 */
@Composable
private fun ReferenceBackdrop(
    hasAnimatedArtwork: Boolean,
    animatedArtwork: NowPlayingScreenData.AnimatedArtworkData?,
    artworkUrl: String?,
    artworkBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    paletteColor: Color,
    mode: BackgroundMode,
    isAnimationEnabled: Boolean,
) {
    if (hasAnimatedArtwork && animatedArtwork != null) {
        Crossfade(targetState = animatedArtwork.isVideo, label = "animatedArtwork") { isVideo ->
            if (isVideo) {
                MediaPlayerView(
                    url = animatedArtwork.url,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(animatedArtwork.url)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(animatedArtwork.url)
                            .crossfade(400)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops =
                                arrayOf(
                                    0f to Color.Black.copy(alpha = 0.08f),
                                    0.48f to Color.Black.copy(alpha = 0.04f),
                                    0.78f to Color.Black.copy(alpha = 0.58f),
                                    1f to Color.Black.copy(alpha = 0.94f),
                                ),
                        ),
                    ),
        )
        return
    }

    AppleMusicBackground(
        artworkUrl = artworkUrl,
        artworkBitmap = artworkBitmap,
        paletteColor = paletteColor,
        mode = mode,
        isAnimationEnabled = isAnimationEnabled,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ReferencePlayerPage(
    screenDataState: NowPlayingScreenData,
    controllerState: ControlState,
    hasAnimatedArtwork: Boolean,
    shouldShowVideo: Boolean,
    isInPipMode: Boolean,
    timeLine: TimeLine,
    onFullscreen: () -> Unit,
    onBackward: () -> Unit,
    onForward: () -> Unit,
    onLike: () -> Unit,
    onMore: () -> Unit,
    onArtistClick: () -> Unit,
    onArtworkLoaded: (androidx.compose.ui.graphics.ImageBitmap) -> Unit,
) {
    if (hasAnimatedArtwork) {
        // Fullscreen artwork is the backdrop; only the track header floats on top.
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = 14.dp),
        ) {
            ReferenceTrackHeader(
                screenDataState = screenDataState,
                controllerState = controllerState,
                showArtwork = false,
                onLike = onLike,
                onMore = onMore,
                onArtistClick = onArtistClick,
                onArtworkLoaded = onArtworkLoaded,
                modifier = Modifier.padding(horizontal = 26.dp),
            )
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // The content box ends right above the progress bar: the artwork and the title must
        // fit inside it, so only the header itself is reserved from the available height.
        val artworkSize =
            minOf(
                (maxWidth - 48.dp).coerceAtLeast(0.dp),
                (maxHeight - 140.dp).coerceAtLeast(0.dp),
            )
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(1f))
            // Cover art / video: gently shrinks when paused (like the miniplayer) and sits a
            // touch higher than the plain position.
            // The pause scale must live INSIDE the shared element's content (ReferenceStaticArtwork)
            // for the artwork: if the morph overlay renders unscaled and the in-place element is
            // scaled, the handoff at the end of the tab transition hard-cuts.
            val pauseScale by animateFloatAsState(
                targetValue = if (controllerState.isPlaying) 1f else 0.88f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "artworkPauseScale",
            )
            Box(
                modifier =
                    Modifier
                        .size(artworkSize)
                        .offset(y = (-10).dp)
                        .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                if (screenDataState.isVideo && shouldShowVideo) {
                    // The video track keeps the music video's 16:9 shape, playing through the
                    // main player with YouTube subtitles; tap the video for the overlay controls.
                    // It fills the SAME square footprint the cover art would occupy, centred both
                    // ways — so the video's middle lines up with where the artwork's middle would
                    // be.
                    ReferenceInlineVideo(
                        screenDataState = screenDataState,
                        isInPipMode = isInPipMode,
                        timeLine = timeLine,
                        isPlaying = controllerState.isPlaying,
                        onFullscreen = onFullscreen,
                        onBackward = onBackward,
                        onForward = onForward,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    ReferenceStaticArtwork(
                        url = screenDataState.thumbnailURL,
                        onSuccess = onArtworkLoaded,
                        modifier = Modifier.fillMaxSize(),
                        scale = pauseScale,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            ReferenceTrackHeader(
                screenDataState = screenDataState,
                controllerState = controllerState,
                showArtwork = false,
                onLike = onLike,
                onMore = onMore,
                onArtistClick = onArtistClick,
                onArtworkLoaded = onArtworkLoaded,
                modifier = Modifier.padding(horizontal = 26.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Inline music-video player: 16:9, playing the current media through the MAIN player with the
 * YouTube caption overlay. Tapping the video toggles a control overlay (fullscreen, rewind /
 * forward 5s, captions) that auto-hides — it only holds buttons that don't already exist in the
 * layout below (transport, progress, pills), so nothing is duplicated.
 */
@Composable
private fun ReferenceInlineVideo(
    screenDataState: NowPlayingScreenData,
    isInPipMode: Boolean,
    timeLine: TimeLine,
    isPlaying: Boolean,
    onFullscreen: () -> Unit,
    onBackward: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOverlay by rememberSaveable { mutableStateOf(true) }
    var showSubtitle by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(3_500)
            showOverlay = false
        }
    }
    // The big video is the shared-element source of the mini header video, so the pause scale
    // must live INSIDE the shared element (same rule as the artwork) or the morph handoff cuts.
    val sharedState = LocalSharedArtworkState.current
    val sharedScope = LocalSharedArtworkScope.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val pauseScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.88f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "videoPauseScale",
    )
    val baseModifier =
        if (sharedState != null && sharedScope != null && sharedTransitionScope != null) {
            with(sharedTransitionScope) { modifier.sharedElement(sharedState, sharedScope) }
        } else {
            modifier
        }
    Box(
        modifier =
            baseModifier
                .graphicsLayer {
                    scaleX = pauseScale
                    scaleY = pauseScale
                }
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black),
    ) {
        Box(Modifier.fillMaxSize()) {
            MediaPlayerViewWithSubtitle(
                playerName = Config.MAIN_PLAYER,
                modifier = Modifier.fillMaxSize(),
                shouldShowSubtitle = showSubtitle,
                shouldPip = false,
                shouldScaleDownSubtitle = true,
                isInPipMode = isInPipMode,
                timelineState = timeLine,
                lyricsData = screenDataState.lyricsData?.lyrics,
                translatedLyricsData = screenDataState.lyricsData?.translatedLyrics?.first,
                mainTextStyle = typo().bodyLarge,
                translatedTextStyle = typo().bodyMedium,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showOverlay = !showOverlay },
                    ),
        )
        if (showOverlay) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops =
                                    arrayOf(
                                        0f to Color.Black.copy(alpha = 0.55f),
                                        0.6f to Color.Black.copy(alpha = 0f),
                                        1f to Color.Black.copy(alpha = 0.35f),
                                    ),
                            ),
                        ),
            ) {
                ReferenceActionCircle(
                    icon = SimpIcons.Fullscreen,
                    tint = ReferenceText,
                    contentDescription = "Fullscreen",
                    onClick = onFullscreen,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                )
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ReferenceActionCircle(
                        icon = SimpIcons.Replay5,
                        tint = ReferenceText,
                        contentDescription = "Rewind 5 seconds",
                        onClick = onBackward,
                    )
                    ReferenceActionCircle(
                        icon = SimpIcons.Forward5,
                        tint = ReferenceText,
                        contentDescription = "Forward 5 seconds",
                        onClick = onForward,
                    )
                }
                if (screenDataState.lyricsData != null) {
                    ReferenceActionCircle(
                        icon = if (showSubtitle) SimpIcons.SubtitlesOff else SimpIcons.Subtitles,
                        tint = ReferenceText,
                        contentDescription = if (showSubtitle) "Hide captions" else "Show captions",
                        onClick = { showSubtitle = !showSubtitle },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceLyricsPage(
    screenDataState: NowPlayingScreenData,
    controllerState: ControlState,
    shouldShowVideo: Boolean = false,
    isInPipMode: Boolean = false,
    timelineValue: TimeLine? = null,
    onLike: () -> Unit,
    onMore: () -> Unit,
    onArtistClick: () -> Unit,
    onLineClick: (Float) -> Unit,
    onArtworkLoaded: (androidx.compose.ui.graphics.ImageBitmap) -> Unit,
    timeLine: StateFlow<TimeLine>,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp, end = 26.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReferenceTrackHeader(
                screenDataState = screenDataState,
                controllerState = controllerState,
                showArtwork = true,
                shouldShowVideo = shouldShowVideo,
                isInPipMode = isInPipMode,
                timeLine = timelineValue,
                onLike = onLike,
                onMore = onMore,
                onArtistClick = onArtistClick,
                onArtworkLoaded = onArtworkLoaded,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
        ) {
            val lyricsData = screenDataState.lyricsData
            if (lyricsData == null) {
                Text(
                    text = stringResource(Res.string.unavailable),
                    color = ReferenceMutedText,
                    style = typo().bodyLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LyricsView(
                    lyricsData = lyricsData,
                    timeLine = timeLine,
                    onLineClick = onLineClick,
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = Color.Transparent,
                )
            }
        }
    }
}

@Composable
private fun ReferenceQueuePage(
    screenDataState: NowPlayingScreenData,
    controllerState: ControlState,
    queue: List<Track>,
    mediaPlayerHandler: MediaPlayerHandler,
    shouldShowVideo: Boolean = false,
    isInPipMode: Boolean = false,
    timelineValue: TimeLine? = null,
    onLike: () -> Unit,
    onMore: () -> Unit,
    onArtistClick: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    currentVideoId: String?,
    onArtworkLoaded: (androidx.compose.ui.graphics.ImageBitmap) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ReferenceTrackHeader(
            screenDataState = screenDataState,
            controllerState = controllerState,
            showArtwork = true,
            shouldShowVideo = shouldShowVideo,
            isInPipMode = isInPipMode,
            timeLine = timelineValue,
            onLike = onLike,
            onMore = onMore,
            onArtistClick = onArtistClick,
            onArtworkLoaded = onArtworkLoaded,
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReferencePageSegment(
                selected = controllerState.isShuffle,
                outerSide = ReferenceSegmentOuterSide.Left,
                onClick = onShuffle,
            ) {
                Icon(
                    imageVector = SimpIcons.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (controllerState.isShuffle) ReferenceSegmentActiveTint else ReferenceText,
                    modifier = Modifier.size(22.dp),
                )
            }
            ReferencePageSegment(
                selected = controllerState.repeatState !is RepeatState.None,
                outerSide = ReferenceSegmentOuterSide.Right,
                onClick = onRepeat,
            ) {
                Icon(
                    imageVector =
                        if (controllerState.repeatState is RepeatState.One) {
                            SimpIcons.RepeatOne
                        } else {
                            SimpIcons.Repeat
                        },
                    contentDescription = "Repeat",
                    tint =
                        if (controllerState.repeatState !is RepeatState.None) {
                            ReferenceSegmentActiveTint
                        } else {
                            ReferenceText
                        },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentPadding = PaddingValues(start = 26.dp, end = 26.dp, bottom = 12.dp),
        ) {
            item {
                Column(Modifier.padding(top = 14.dp, bottom = 6.dp)) {
                    Text(
                        text = "Up Next",
                        color = ReferenceText,
                        style = typo().titleLarge.copy(fontSize = 20.sp),
                    )
                    Text(
                        text = "From ${queueDataPlaylistName(screenDataState)}",
                        color = ReferenceMutedText,
                        style = typo().bodyMedium.copy(fontSize = 14.sp),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            itemsIndexed(
                items = queue,
                key = { index, track -> "reference_queue_${track.videoId}_$index" },
            ) { index, track ->
                ReferenceQueueItem(
                    track = track,
                    isPlaying = track.videoId == currentVideoId,
                    onClick = { mediaPlayerHandler.playMediaItemInMediaSource(index) },
                )
            }
        }
    }
}

private fun queueDataPlaylistName(screenDataState: NowPlayingScreenData): String =
    screenDataState.playlistName.ifBlank { "Favourite Songs" }

/**
 * The small 16:9 video window in the lyrics/queue headers: the shared-element destination of the
 * big inline video (same key, same 8dp radius, same pause scale) so switching tabs morphs the
 * video instead of fading. Subtitles are off — they are unreadable at this size.
 */
@Composable
private fun ReferenceMiniVideo(
    screenDataState: NowPlayingScreenData,
    isInPipMode: Boolean,
    timeLine: TimeLine,
    isPlaying: Boolean,
    pauseScale: Float,
    modifier: Modifier = Modifier,
) {
    val sharedState = LocalSharedArtworkState.current
    val sharedScope = LocalSharedArtworkScope.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val baseModifier =
        if (sharedState != null && sharedScope != null && sharedTransitionScope != null) {
            with(sharedTransitionScope) { modifier.sharedElement(sharedState, sharedScope) }
        } else {
            modifier
        }
    Box(
        modifier =
            baseModifier
                .graphicsLayer {
                    scaleX = pauseScale
                    scaleY = pauseScale
                }
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black),
    ) {
        MediaPlayerViewWithSubtitle(
            playerName = Config.MAIN_PLAYER,
            modifier = Modifier.fillMaxSize(),
            shouldShowSubtitle = false,
            shouldPip = false,
            shouldScaleDownSubtitle = true,
            isInPipMode = isInPipMode,
            timelineState = timeLine,
            lyricsData = null,
            translatedLyricsData = null,
            mainTextStyle = typo().bodyLarge,
            translatedTextStyle = typo().bodyMedium,
        )
    }
}

@Composable
private fun ReferenceStaticArtwork(
    url: String?,
    onSuccess: (androidx.compose.ui.graphics.ImageBitmap) -> Unit,
    modifier: Modifier,
    cornerRadius: Dp = 16.dp,
    scale: Float = 1f,
) {
    val shape = RoundedCornerShape(cornerRadius)
    // The big player artwork and the small header artwork share one shared-element key, so
    // switching tabs morphs the art to its new position instead of fading it out and back in.
    val sharedState = LocalSharedArtworkState.current
    val sharedScope = LocalSharedArtworkScope.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val sharedModifier =
        if (sharedState != null && sharedScope != null && sharedTransitionScope != null) {
            with(sharedTransitionScope) { modifier.sharedElement(sharedState, sharedScope) }
        } else {
            modifier
        }
    AsyncImage(
        model =
            ImageRequest
                .Builder(LocalPlatformContext.current)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(url)
                .crossfade(450)
                .build(),
        placeholder = rememberHolderPainter(),
        error = rememberHolderPainter(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onSuccess = { onSuccess(it.result.image.toImageBitmap()) },
        modifier =
            sharedModifier
                // Scale INSIDE the shared element so the morph overlay renders the same scaled
                // state the element settles into — no size hard-cut at the end of the transition.
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(10.dp, shape, spotColor = Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
                .clip(shape),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReferenceTrackHeader(
    screenDataState: NowPlayingScreenData,
    controllerState: ControlState,
    showArtwork: Boolean,
    shouldShowVideo: Boolean = false,
    isInPipMode: Boolean = false,
    timeLine: TimeLine? = null,
    onLike: () -> Unit,
    onMore: () -> Unit,
    onArtistClick: () -> Unit,
    onArtworkLoaded: (androidx.compose.ui.graphics.ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showArtwork) {
            // Same pause scale as the big artwork/video so the shared-element morph handoff
            // never size-cuts (both ends of the morph scale together).
            val pauseScale by animateFloatAsState(
                targetValue = if (controllerState.isPlaying) 1f else 0.88f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "miniArtworkPauseScale",
            )
            if (shouldShowVideo && screenDataState.isVideo && timeLine != null) {
                // The video track keeps playing in the header — a small 16:9 window that is the
                // shared-element destination of the big video (same 8dp radius so the morph
                // never radius-cuts).
                ReferenceMiniVideo(
                    screenDataState = screenDataState,
                    isInPipMode = isInPipMode,
                    timeLine = timeLine,
                    isPlaying = controllerState.isPlaying,
                    pauseScale = pauseScale,
                    modifier = Modifier.height(44.dp),
                )
            } else {
                // Perfect square, and the shared-element destination of the big artwork — it MUST
                // use the same corner radius (16dp) or the radius hard-cuts at the start of the
                // morph.
                ReferenceStaticArtwork(
                    url = screenDataState.thumbnailURL,
                    onSuccess = onArtworkLoaded,
                    modifier = Modifier.size(44.dp),
                    scale = pauseScale,
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = screenDataState.nowPlayingTitle,
                color = ReferenceText,
                style = typo().titleLarge.copy(fontSize = if (showArtwork) 17.sp else 21.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE)
                        .focusable(),
            )
            Text(
                text = screenDataState.artistName,
                color = ReferenceMutedText,
                style = typo().bodyLarge.copy(fontSize = if (showArtwork) 13.sp else 15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .basicMarquee(iterations = Int.MAX_VALUE)
                        .focusable()
                        .clickable(onClick = onArtistClick),
            )
        }
        Spacer(Modifier.width(12.dp))
        ReferenceActionCircle(
            icon = if (controllerState.isLiked) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
            tint = ReferenceText,
            contentDescription = if (controllerState.isLiked) "Unlike" else "Like",
            selected = controllerState.isLiked,
            onClick = onLike,
        )
        Spacer(Modifier.width(6.dp))
        ReferenceActionCircle(
            icon = SimpIcons.MoreVert,
            tint = ReferenceText,
            contentDescription = "More options",
            onClick = onMore,
        )
    }
}

@Composable
private fun ReferenceActionCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    contentDescription: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (selected) {
                        ReferenceSelectedSurface
                    } else {
                        LocalReferencePrimary.current
                    },
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) ReferenceSegmentActiveTint else tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * PixelPlayer's `WavySliderExpressive` progress bar: a single 5dp wavy track with no gap between
 * the active and inactive segments, a round thumb that morphs into a pill while scrubbing, and
 * frame-clock interpolation so playback advances smoothly instead of in 200ms ticks.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReferenceProgressBar(
    value: Float,
    enabled: Boolean,
    isPlaying: Boolean,
    isSliding: Boolean,
    totalDurationMs: Long,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val strokeWidth = 5.dp
    val thumbRadius = 8.dp
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val thumbRadiusPx = with(density) { thumbRadius.toPx() }
    val thumbLineHeightPx = with(density) { 24.dp.toPx() }
    val stroke = remember(density) { Stroke(width = strokeWidthPx, cap = StrokeCap.Round) }

    val hapticFeedback = LocalHapticFeedback.current
    var lastTickSecond by remember { mutableLongStateOf(Long.MIN_VALUE) }
    val referencePrimary = LocalReferencePrimary.current
    // Solid tint used INSIDE the merged offscreen layer (see the thumb Canvas below): the layer
    // is drawn at 30% like the play/pause button, so the bar optically matches it.
    val referenceTint = LocalReferenceTint.current

    var isPointerSeeking by remember { mutableStateOf(false) }
    val isInteracting = isPointerSeeking || isSliding
    val thumbInteractionFraction by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "progressThumbInteraction",
    )
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (enabled && isPlaying && !isInteracting) 1f else 0f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progressAmplitude",
    )

    val timeAlpha by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0.6f,
        animationSpec = tween(150),
        label = "progressTimeAlpha",
    )

    val currentHalfWidth by remember {
        derivedStateOf {
            val fraction = thumbInteractionFraction
            thumbRadius * (1f - fraction) + strokeWidth * 0.6f * fraction
        }
    }
    val dynamicGapSize by remember {
        derivedStateOf {
            val fraction = thumbInteractionFraction
            val idleGap = 6.dp
            val draggingGap = currentHalfWidth + 1.2.dp
            idleGap + (draggingGap - idleGap) * fraction
        }
    }

    // Matches the gapSize handed to the indicator below; kept in px for the Canvas.
    val renderedGapPx =
        with(density) {
            (dynamicGapSize * 2f * (1f + 0.1573f * animatedAmplitude * animatedAmplitude)).toPx()
        }

    // The played portion and the ball are SOLID WHITE (the cleanest look, like Apple Music's
    // bar); the unplayed remainder stays a faint pre-blended tint.
    val playedSolid = Color.White
    val unplayedSolid =
        referenceTint.copy(
            red = referenceTint.red * 0.24f,
            green = referenceTint.green * 0.24f,
            blue = referenceTint.blue * 0.24f,
        )

    // Smooth interpolated progress, updated on the frame clock (never recomposes per frame).
    val renderedNormalizedProgress = remember { mutableFloatStateOf(value.coerceIn(0f, 1f)) }
    val latestValue by rememberUpdatedState(value.coerceIn(0f, 1f))
    var lastProgressUpdateNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isInteracting, enabled) {
        snapshotFlow { latestValue }.collect { target ->
            if (!enabled || isInteracting) {
                renderedNormalizedProgress.floatValue = target
                lastProgressUpdateNanos = 0L
                return@collect
            }
            val start = renderedNormalizedProgress.floatValue
            // Snap on discontinuities (song change, seek catch-up).
            if (abs(start - target) > 0.1f) {
                renderedNormalizedProgress.floatValue = target
                lastProgressUpdateNanos = 0L
                return@collect
            }
            if (abs(start - target) <= 0.0001f) {
                renderedNormalizedProgress.floatValue = target
                return@collect
            }
            val nowNanos = System.nanoTime()
            val intervalMs =
                if (lastProgressUpdateNanos == 0L) {
                    180L
                } else {
                    ((nowNanos - lastProgressUpdateNanos) / 1_000_000L).coerceIn(1L, 250L)
                }
            lastProgressUpdateNanos = nowNanos
            val durationNanos = (intervalMs * 900_000L).coerceAtLeast(1_000_000L)
            var startFrameNanos = 0L
            while (isActive) {
                val frameNanos = withFrameNanos { it }
                if (startFrameNanos == 0L) startFrameNanos = frameNanos
                val elapsedNanos = (frameNanos - startFrameNanos).coerceAtLeast(0L)
                val fraction =
                    (elapsedNanos.toDouble() / durationNanos.toDouble())
                        .toFloat()
                        .coerceIn(0f, 1f)
                renderedNormalizedProgress.floatValue = start + (target - start) * fraction
                if (fraction >= 1f) break
            }
            renderedNormalizedProgress.floatValue = target
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 18.dp)
                    .clearAndSetSemantics {
                        contentDescription = "Playback position"
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(
                            current = renderedNormalizedProgress.floatValue,
                            range = 0f..1f,
                            steps = 0,
                        )
                    if (enabled) {
                        setProgress { requested ->
                            val coerced = requested.coerceIn(0f, 1f)
                            onValueChange(coerced)
                            onValueChangeFinished()
                            true
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        // The original structure: the wavy indicator draws the played portion, our Canvas draws
        // the unplayed track and the round thumb. All colours are SOLID (pre-blended over black
        // so they look exactly like the translucent play/pause tints) — nothing is translucent
        // anymore, so nothing can show through anything, and the gap keeps the lines clear of
        // the thumb.
        LinearWavyProgressIndicator(
            progress = { renderedNormalizedProgress.floatValue },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = thumbRadius),
            color = playedSolid,
            // The track is drawn by our own Canvas below: material3 collapses its gap
            // while progress is near the start, making the thumb touch the track there.
            trackColor = Color.Transparent,
            stroke = stroke,
            trackStroke = stroke,
            gapSize = (dynamicGapSize.value * 2f * (1f + 0.1573f * animatedAmplitude * animatedAmplitude)).dp,
            stopSize = 0.dp,
            amplitude = { progress -> if (progress > 0f) animatedAmplitude else 0f },
            wavelength = 46.dp,
            waveSpeed = 15.dp,
        )

        // Round thumb, morphing into a vertical pill while scrubbing.
        Canvas(Modifier.fillMaxSize()) {
            val edgePaddingPx = thumbRadiusPx.coerceIn(0f, size.width / 2f)
            val trackStart = edgePaddingPx
            val trackEnd = size.width - edgePaddingPx
            val trackWidth = (trackEnd - trackStart).coerceAtLeast(0f)
            val thumbY = size.height / 2f
            val progress = renderedNormalizedProgress.floatValue

            // The material3 indicator collapses its gap while progress is near the very
            // start (barHead < stroke cap width), which makes the thumb touch the track;
            // drawing the track ourselves keeps the thumb-to-track distance constant.
            // The track runs to the bar's end and halts there: its round cap is the end
            // ball, and it never continues past it.
            val visibleTrackRight = size.width - edgePaddingPx - strokeWidthPx / 2f
            val trackLeft =
                (trackStart + trackWidth * progress + renderedGapPx + strokeWidthPx)
                    .coerceAtLeast(trackStart + strokeWidthPx / 2f)
            // Never past the bar's end: at the end of the song the track collapses into the
            // end ball instead of drawing a backwards line beyond it.
            val visibleTrackLeft = trackLeft.coerceAtMost(visibleTrackRight)
            drawLine(
                color = unplayedSolid,
                start = Offset(visibleTrackLeft, thumbY),
                end = Offset(visibleTrackRight, thumbY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )

            val currentWidth = lerp(thumbRadiusPx * 2f, strokeWidthPx * 1.2f, thumbInteractionFraction)
            val currentHeight = lerp(thumbRadiusPx * 2f, thumbLineHeightPx, thumbInteractionFraction)
            val rawThumbX = trackStart + trackWidth * progress
            val minThumbCenter = (currentWidth / 2f).coerceAtMost(size.width / 2f)
            val maxThumbCenter = (size.width - currentWidth / 2f).coerceAtLeast(minThumbCenter)
            val thumbX = rawThumbX.coerceIn(minThumbCenter, maxThumbCenter)

            drawRoundRect(
                color = playedSolid,
                topLeft = Offset(thumbX - currentWidth / 2f, thumbY - currentHeight / 2f),
                size = Size(currentWidth, currentHeight),
                cornerRadius = CornerRadius(currentWidth / 2f),
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput

                        fun valueForX(rawX: Float): Float {
                            val edgePadding = thumbRadiusPx.coerceIn(0f, size.width / 2f)
                            val trackStart = edgePadding
                            val trackEnd = size.width - edgePadding
                            val trackWidth = (trackEnd - trackStart).coerceAtLeast(1f)
                            return ((rawX - trackStart) / trackWidth).coerceIn(0f, 1f)
                        }

                        fun positionSecond(rawValue: Float): Long {
                            if (totalDurationMs <= 0L) return Long.MIN_VALUE
                            return (rawValue * totalDurationMs).toLong() / 1000L
                        }

                        fun tickHapticFor(rawValue: Float) {
                            if (totalDurationMs <= 0L) return
                            val second = positionSecond(rawValue)
                            if (second != lastTickSecond) {
                                lastTickSecond = second
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            }
                        }

                        awaitEachGesture {
                            try {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                isPointerSeeking = true
                                down.consume()
                                var latestGestureValue = valueForX(down.position.x)
                                lastTickSecond = positionSecond(latestGestureValue)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                onValueChange(latestGestureValue)

                                var pointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change =
                                        event.changes.firstOrNull { it.id == pointerId }
                                            ?: event.changes.firstOrNull { it.pressed }
                                            ?: break
                                    pointerId = change.id
                                    if (!change.pressed) {
                                        change.consume()
                                        break
                                    }
                                    if (change.position != change.previousPosition) {
                                        change.consume()
                                        latestGestureValue = valueForX(change.position.x)
                                        onValueChange(latestGestureValue)
                                        tickHapticFor(latestGestureValue)
                                    }
                                }
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                onValueChangeFinished()
                            } finally {
                                isPointerSeeking = false
                            }
                        }
                    },
                                )
        }
        Spacer(Modifier.height(2.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration((value * totalDurationMs).toLong().coerceAtLeast(0L)),
                color = ReferenceMutedText.copy(alpha = timeAlpha),
                style = typo().bodyMedium.copy(fontSize = 11.sp),
            )
            Text(
                text = formatDuration(totalDurationMs.coerceAtLeast(0L)),
                color = ReferenceMutedText.copy(alpha = timeAlpha),
                style = typo().bodyMedium.copy(fontSize = 11.sp),
            )
        }
    }
}

/**
 * Transport buttons: every button bounces the same way — it grows (1.1x) while pressed and stays
 * in that pose for as long as it is held, then settles back on release, with the siblings
 * shrinking (0.65x) while one is held. The play/pause pill additionally morphs its corners
 * between a circle (paused) and a rounded square (playing).
 */
@Composable
private fun ReferenceTransportControls(
    controllerState: ControlState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    // Spring instead of a tween: even a very fast tap gets a visible quick bounce, and
    // holding keeps the button in the bounced pose while the spring settles at 1.1x.
    val pressSpring = spring<Float>(dampingRatio = 0.5f, stiffness = 600f)
    val playPauseCorner by animateDpAsState(
        targetValue = if (controllerState.isPlaying) 26.dp else 60.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "playPauseCorner",
    )

    val previousInteraction = remember { MutableInteractionSource() }
    val playPauseInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val previousPressed by previousInteraction.collectIsPressedAsState()
    val playPausePressed by playPauseInteraction.collectIsPressedAsState()
    val nextPressed by nextInteraction.collectIsPressedAsState()

    // Click counters: the ONLY signal guaranteed to fire on every click. An ultra-fast tap can
    // coalesce the press state away entirely (true→false within one frame), so the bounce is
    // driven off the click itself.
    var previousClickCount by remember { mutableIntStateOf(0) }
    var playPauseClickCount by remember { mutableIntStateOf(0) }
    var nextClickCount by remember { mutableIntStateOf(0) }

    val previousWeight by
        transportWeight(
            isPressed = previousPressed,
            isSiblingPressed = playPausePressed || nextPressed,
            clickCount = previousClickCount,
            pressSpring = pressSpring,
        )
    val playPauseWeight by
        transportWeight(
            isPressed = playPausePressed,
            isSiblingPressed = previousPressed || nextPressed,
            clickCount = playPauseClickCount,
            pressSpring = pressSpring,
        )
    val nextWeight by
        transportWeight(
            isPressed = nextPressed,
            isSiblingPressed = previousPressed || playPausePressed,
            clickCount = nextClickCount,
            pressSpring = pressSpring,
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(88.dp)
                .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReferenceTransportButton(
            modifier = Modifier.weight(previousWeight).fillMaxHeight(),
            enabled = controllerState.isPreviousAvailable,
            shape = CircleShape,
            containerColor = LocalReferenceSecondary.current,
            interactionSource = previousInteraction,
            onClick = {
                onPrevious()
                previousClickCount++
            },
        ) {
            Icon(
                imageVector = SimpIcons.SkipPrevious,
                contentDescription = "Previous",
                tint = ReferenceText,
                modifier = Modifier.size(32.dp),
            )
        }
        ReferenceTransportButton(
            modifier = Modifier.weight(playPauseWeight).fillMaxHeight(),
            enabled = true,
            shape = RoundedCornerShape(playPauseCorner),
            containerColor = LocalReferencePrimary.current,
            interactionSource = playPauseInteraction,
            onClick = {
                onPlayPause()
                playPauseClickCount++
            },
        ) {
            Crossfade(
                targetState = controllerState.isPlaying,
                animationSpec = tween(150),
                label = "playPause",
            ) { isPlaying ->
                Icon(
                    imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = ReferenceText,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        ReferenceTransportButton(
            modifier = Modifier.weight(nextWeight).fillMaxHeight(),
            enabled = controllerState.isNextAvailable,
            shape = CircleShape,
            containerColor = LocalReferenceSecondary.current,
            interactionSource = nextInteraction,
            onClick = {
                onNext()
                nextClickCount++
            },
        ) {
            Icon(
                imageVector = SimpIcons.SkipNext,
                contentDescription = "Next",
                tint = ReferenceText,
                modifier = Modifier.size(32.dp),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
}

private fun referenceTransportWeight(
    isPressed: Boolean,
    isSiblingPressed: Boolean,
): Float =
    when {
        isPressed -> 1.1f
        isSiblingPressed -> 0.65f
        else -> 1f
    }

/**
 * Bouncy transport-button weight.
 *
 * The bounce is driven by [clickCount] — incremented by the button's onClick — because that is
 * the ONLY signal that is guaranteed to fire on every click. An ultra-fast tap can coalesce the
 * press state away entirely (true→false inside one frame, so `isPressed` is never observed), but
 * onClick always fires, so the horizontal bounce happens on every click no matter how fast.
 */
@Composable
private fun transportWeight(
    isPressed: Boolean,
    isSiblingPressed: Boolean,
    clickCount: Int,
    pressSpring: androidx.compose.animation.core.SpringSpec<Float>,
): State<Float> {
    val weight = remember { Animatable(1f) }
    // Press: spring up to the held pose. Release / sibling changes: settle at the rest pose.
    LaunchedEffect(isPressed, isSiblingPressed) {
        val target =
            referenceTransportWeight(
                isPressed = isPressed,
                isSiblingPressed = isSiblingPressed,
            )
        // Skip when the click-driven bounce is already running toward ~1.
        if (weight.value != target) {
            weight.animateTo(target, pressSpring)
        }
    }
    // Every click: pulse 1.15x then spring back — always visible, even for clicks so fast the
    // press state was never rendered. While held, skip so the hold pose wins.
    LaunchedEffect(clickCount) {
        if (clickCount > 0 && !isPressed && !isSiblingPressed) {
            weight.animateTo(1.15f, spring(dampingRatio = 0.5f, stiffness = 900f))
            weight.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
        }
    }
    return weight.asState()
}

@Composable
private fun ReferenceTransportButton(
    modifier: Modifier,
    enabled: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    containerColor: Color,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(containerColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.alpha(
                if (enabled) {
                    1f
                } else {
                    0.34f
                },
            ),
        ) {
            content()
        }
    }
}

@Composable
private fun ReferenceBottomNavigation(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Resting transport buttons each take (width - 48.dp - 2*8.dp) / 3; the pill spans
        // from the previous button's centre to the next button's centre.
        val restingTransportWidth = (maxWidth - 48.dp - 16.dp) / 3f
        val pillWidth = restingTransportWidth * 2f + 16.dp
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(pillWidth)
                        .height(64.dp)
                        .clip(RoundedCornerShape(60.dp))
                        .background(LocalReferenceSecondary.current),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ReferencePageSegment(
                        selected = selectedTab == 1,
                        outerSide = ReferenceSegmentOuterSide.Left,
                        onClick = { onSelect(1) },
                    ) {
                        Icon(
                            imageVector = SimpIcons.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (selectedTab == 1) ReferenceSegmentActiveTint else ReferenceText,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    ReferencePageSegment(
                        selected = false,
                        onClick = {},
                    ) {
                        PlatformCastButton(
                            modifier = Modifier.size(30.dp),
                            tint = ReferenceText,
                        )
                    }
                    ReferencePageSegment(
                        selected = selectedTab == 2,
                        outerSide = ReferenceSegmentOuterSide.Right,
                        onClick = { onSelect(2) },
                    ) {
                        ReferenceQueueListIcon(
                            tint = if (selectedTab == 2) ReferenceSegmentActiveTint else ReferenceText,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

private enum class ReferenceSegmentOuterSide {
    Left,
    Right,
}

@Composable
private fun RowScope.ReferencePageSegment(
    selected: Boolean,
    onClick: () -> Unit,
    outerSide: ReferenceSegmentOuterSide? = null,
    content: @Composable () -> Unit,
) {
    // Buttons wear the segment tint, chosen so the stack (button over the pill's second-colour
    // container) reads optically as the same 30% as the play/pause button; the selected one
    // turns solid white.
    val backgroundColor =
        if (selected) {
            ReferenceSelectedSurface
        } else {
            LocalReferenceSegment.current
        }
    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 60.dp else 10.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "pageSegmentCornerRadius",
    )
    // The outer side (facing away from the group) is always a full semicircle; the inner
    // corners match the neighbouring segment (10dp) and bloom into a full pill when selected.
    val shape =
        when (outerSide) {
            ReferenceSegmentOuterSide.Left ->
                RoundedCornerShape(
                    topStart = CornerSize(percent = 50),
                    bottomStart = CornerSize(percent = 50),
                    topEnd = CornerSize(cornerRadius),
                    bottomEnd = CornerSize(cornerRadius),
                )
            ReferenceSegmentOuterSide.Right ->
                RoundedCornerShape(
                    topStart = CornerSize(cornerRadius),
                    bottomStart = CornerSize(cornerRadius),
                    topEnd = CornerSize(percent = 50),
                    bottomEnd = CornerSize(percent = 50),
                )
            null -> RoundedCornerShape(cornerRadius)
        }
    Box(
        modifier =
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(shape)
                .background(backgroundColor)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ReferenceQueueItem(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val thumbnail = track.thumbnails?.maxByOrNull { it.width * it.height }?.url
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(thumbnail)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(thumbnail)
                    .crossfade(250)
                    .build(),
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
        ) {
            Text(
                text = track.title,
                color = if (isPlaying) ReferenceText else ReferenceText.copy(alpha = 0.95f),
                style = typo().titleMedium.copy(fontSize = 17.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artists?.joinToString(", ") { it.name }.orEmpty(),
                color = ReferenceMutedText,
                style = typo().bodyLarge.copy(fontSize = 14.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        ReferenceQueueListIcon(
            tint = if (isPlaying) ReferenceText.copy(alpha = 0.8f) else ReferenceMutedText.copy(alpha = 0.5f),
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun ReferenceQueueListIcon(
    tint: Color,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val dotRadius = size.minDimension * 0.075f
        val lineHeight = size.height * 0.12f
        val lineStart = size.width * 0.34f
        val lineWidth = size.width * 0.56f
        val rows = listOf(0.23f, 0.5f, 0.77f)
        rows.forEach { yFraction ->
            drawCircle(
                color = tint,
                radius = dotRadius,
                center = Offset(size.width * 0.15f, size.height * yFraction),
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(lineStart, size.height * yFraction - lineHeight / 2f),
                size = Size(lineWidth, lineHeight),
                cornerRadius = CornerRadius(lineHeight / 2f),
            )
        }
    }
}
