package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.smoothScrimBrush
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Apple-Music-style backdrop: the artwork itself, drawn as multiple oversized copies that are
 * rotated around slowly drifting centres and heavily blurred — approximating the Metal twist
 * shader of the real app — with a mode-dependent darkening scrim on top.
 *
 * - [mode] picks the scrim: [BackgroundMode.PLAYER] is darker and more muted, LYRICS/QUEUE are
 *   brighter and more vibrant. The scrim ramps slowly so tab switches melt instead of snapping.
 * - When [isAnimationEnabled] is false the twist CLOCK freezes but the layers stay exactly where
 *   they are — pausing or resuming the animation (fatigue protection, settings) never causes a
 *   visual cut. The artwork crossfades on song changes so the background always flows.
 * - The [artworkBitmap] from the palette pipeline is preferred; until it arrives (or if the URL
 *   is the only source) the URL is loaded directly. Without any artwork, [paletteColor] is used
 *   as a flat fallback.
 */
@Composable
fun AppleMusicBackground(
    artworkUrl: String?,
    artworkBitmap: ImageBitmap?,
    paletteColor: Color,
    mode: BackgroundMode,
    isAnimationEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var loadedBitmap by remember { mutableStateOf(artworkBitmap) }
    LaunchedEffect(artworkBitmap) {
        artworkBitmap?.let { loadedBitmap = it }
    }

    // One shared twist clock for the whole backdrop, hoisted ABOVE the artwork crossfade so the
    // drifting phases never restart when the cover changes; freezing it (fatigue/setting) keeps
    // the layers in place instead of swapping to a different static layout.
    val twistClock = rememberTwistClock(running = isAnimationEnabled)

    // The scrim ramps slowly so switching between player / lyrics / queue melts instead of
    // snapping — it runs in sync with the page crossfade in the layout.
    val scrimTop by animateColorAsState(
        targetValue =
            if (mode == BackgroundMode.PLAYER) {
                Color.Black.copy(alpha = 0.35f)
            } else {
                Color.Black.copy(alpha = 0.15f)
            },
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "appleMusicScrimTop",
    )
    val scrimBottom by animateColorAsState(
        targetValue =
            if (mode == BackgroundMode.PLAYER) {
                Color.Black.copy(alpha = 0.75f)
            } else {
                Color.Black.copy(alpha = 0.55f)
            },
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "appleMusicScrimBottom",
    )

    Box(modifier.fillMaxSize().clipToBounds()) {
        // Fade-THROUGH (new image fades in while the old stays fully visible, then the old fades
        // out over the new) instead of a plain crossfade: with two dark images a crossfade dips
        // through black at the midpoint, which read as "fade to black, then to the next colour".
        AnimatedContent(
            targetState = loadedBitmap,
            transitionSpec = {
                fadeIn(tween(900, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(tween(900, easing = FastOutSlowInEasing, delayMillis = 450))
            },
            label = "appleMusicBackground",
        ) { bitmap ->
            when {
                bitmap != null -> {
                    TwistedArtworkLayers(
                        artwork = bitmap,
                        clock = twistClock,
                    )
                }

                artworkUrl != null -> {
                    // Artwork not decoded yet: show it plain until it lands, then the twisted
                    // layers take over through the bitmap branch above.
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
                                .data(artworkUrl)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(artworkUrl)
                                .crossfade(300)
                                .build(),
                        placeholder = rememberHolderPainter(),
                        error = rememberHolderPainter(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onSuccess = { loadedBitmap = it.result.image.toImageBitmap() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    Box(Modifier.fillMaxSize().background(paletteColor))
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        smoothScrimBrush(
                            from = scrimTop,
                            to = scrimBottom,
                        ),
                    ),
        )
    }
}

/** Which player screen the background belongs to — controls how dark the scrim is. */
enum class BackgroundMode {
    PLAYER,
    LYRICS,
    QUEUE,
}

/**
 * A wall-clock in seconds for the twist animation. While [running] it advances with the frame
 * clock; when paused it FREEZES in place and resumes from the frozen value later — so the
 * layers never jump when the animation is toggled off and on.
 */
@Composable
private fun rememberTwistClock(running: Boolean): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val base = seconds.floatValue
        val startNanos = System.nanoTime()
        while (isActive) {
            withFrameNanos { frameNanos ->
                seconds.floatValue = base + (frameNanos - startNanos) / 1_000_000_000f
            }
        }
    }
    return seconds
}

/**
 * One artwork copy: scaled ~3x so no rotation ever reveals an edge, blurred heavily and slowly
 * twisting around its centre. Its phase derives from the shared [clock] at its own [periodSeconds]
 * so the motion stays continuous across artwork changes and animation pauses.
 */
@Composable
private fun TwistedArtworkLayer(
    artwork: ImageBitmap,
    baseRotation: Float,
    swing: Float,
    driftAmplitudePx: Float,
    blurEffect: BlurEffect,
    clock: State<Float>,
    periodSeconds: Float,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val phase = (clock.value / periodSeconds) % 1f
                    val angle = phase.toDouble() * 2.0 * PI
                    rotationZ = baseRotation + swing * sin(angle).toFloat()
                    translationX = driftAmplitudePx * cos(angle * 1.7).toFloat()
                    translationY = driftAmplitudePx * sin(angle).toFloat()
                    scaleX = 3f
                    scaleY = 3f
                    renderEffect = blurEffect
                },
    ) {
        Image(
            bitmap = artwork,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TwistedArtworkLayers(
    artwork: ImageBitmap,
    clock: State<Float>,
) {
    val blurRadius = with(LocalDensity.current) { 56.dp.toPx() }
    val blurEffect = remember(blurRadius) { BlurEffect(blurRadius, blurRadius) }
    val driftAmplitude = with(LocalDensity.current) { 64.dp.toPx() }

    // Three copies, each twisting on its own very slow clock (~13-21s cycles) so the motion
    // never repeats visually — the Apple Music "breathing" feel. The layers are ALWAYS the same
    // geometry: when the clock freezes they simply hold still.
    Box(Modifier.fillMaxSize()) {
        TwistedArtworkLayer(
            artwork = artwork,
            baseRotation = -24f,
            swing = 12f,
            driftAmplitudePx = driftAmplitude,
            blurEffect = blurEffect,
            clock = clock,
            periodSeconds = 18f,
        )
        TwistedArtworkLayer(
            artwork = artwork,
            baseRotation = 48f,
            swing = 10f,
            driftAmplitudePx = driftAmplitude * 0.8f,
            blurEffect = blurEffect,
            clock = clock,
            periodSeconds = 15f,
        )
        TwistedArtworkLayer(
            artwork = artwork,
            baseRotation = 132f,
            swing = 14f,
            driftAmplitudePx = driftAmplitude * 1.1f,
            blurEffect = blurEffect,
            clock = clock,
            periodSeconds = 21f,
        )
    }
}
