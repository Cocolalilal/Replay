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
 * Apple-Music-style background: 3 floating, rotating, blurred layers derived directly
 * from the playing song's artwork bitmap.
 *
 * Automatically and organically adapts all colors and shades to the currently playing song,
 * smoothly melting between tracks on song changes.
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

    val twistClock = rememberTwistClock(isAnimationEnabled)

    val scrimTop by animateColorAsState(
        targetValue = if (mode == BackgroundMode.PLAYER) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.15f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "appleMusicScrimTop"
    )

    val scrimBottom by animateColorAsState(
        targetValue = if (mode == BackgroundMode.PLAYER) Color.Black.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.55f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "appleMusicScrimBottom"
    )

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        AnimatedContent(
            targetState = loadedBitmap,
            transitionSpec = {
                (fadeIn(animationSpec = tween(900, easing = FastOutSlowInEasing)) togetherWith
                 fadeOut(animationSpec = tween(900, delayMillis = 450, easing = FastOutSlowInEasing)))
            },
            label = "appleMusicBackground"
        ) { bitmap ->
            if (bitmap != null) {
                TwistedArtworkLayers(artwork = bitmap, clock = twistClock)
            } else if (artworkUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(artworkUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(artworkUrl)
                        .crossfade(300)
                        .build(),
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { success ->
                        loadedBitmap = success.result.image.toImageBitmap()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(paletteColor)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(smoothScrimBrush(scrimTop, scrimBottom))
        )
    }
}

enum class BackgroundMode {
    PLAYER,
    LYRICS,
    QUEUE,
}

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

@Composable
private fun TwistedArtworkLayers(
    artwork: ImageBitmap,
    clock: State<Float>,
) {
    val density = LocalDensity.current
    val blurRadius = with(density) { 56.dp.toPx() }
    val blurEffect = remember(blurRadius) { BlurEffect(blurRadius, blurRadius) }
    val driftAmplitude = with(density) { 64.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        TwistedArtworkLayer(
            artwork = artwork,
            baseRotation = -24.0f,
            swing = 12.0f,
            driftAmplitudePx = driftAmplitude,
            blurEffect = blurEffect,
            clock = clock,
            periodSeconds = 18.0f
        )
        TwistedArtworkLayer(
            artwork = artwork,
            baseRotation = 48.0f,
            swing = 10.0f,
            driftAmplitudePx = driftAmplitude * 0.8f,
            blurEffect = blurEffect,
            clock = clock,
            periodSeconds = 15.0f
        )
        TwistedArtworkLayer(
            artwork = artwork,
            baseRotation = 132.0f,
            swing = 14.0f,
            driftAmplitudePx = driftAmplitude * 1.1f,
            blurEffect = blurEffect,
            clock = clock,
            periodSeconds = 21.0f
        )
    }
}

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
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val phase = (clock.value / periodSeconds) % 1.0f
                val angle = phase * 2.0 * PI
                rotationZ = (sin(angle).toFloat() * swing) + baseRotation
                translationX = cos(1.7 * angle).toFloat() * driftAmplitudePx
                translationY = sin(angle).toFloat() * driftAmplitudePx
                scaleX = 3.0f
                scaleY = 3.0f
                renderEffect = blurEffect
            }
    ) {
        Image(
            bitmap = artwork,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
