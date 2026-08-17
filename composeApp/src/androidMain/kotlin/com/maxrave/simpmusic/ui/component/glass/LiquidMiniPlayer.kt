package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LiquidMiniPlayer(
    backdrop: LayerBackdrop,
    title: String,
    artist: String,
    artworkUrl: String,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onPreviousTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onExpandFullPlayer: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    isInline: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val containerColor = (if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)).copy(alpha = 0.18f)
    val textColor = if (isDark) Color.White else Color.Black
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.65f)

    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.7f),
        label = "miniPlayerScale"
    )
    val inlineProgress by animateFloatAsState(
        targetValue = if (isInline) 1f else 0f,
        animationSpec = spring(stiffness = 350f, dampingRatio = 0.7f),
        label = "miniPlayerInlineProgress"
    )
    val pausedArtworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.9f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.7f),
        label = "miniPlayerPausedArtworkScale"
    )

    val dismissOffsetY = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, dismissOffsetY.value.roundToInt()) }
            .graphicsLayer {
                alpha = 1f - (dismissOffsetY.value / 300f).coerceIn(0f, 1f)
            }
            .scale(pressScale)
            .elasticGlassTouch(
                enabled = false,
                dragEnabled = false,
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            onExpandFullPlayer()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { velocityTracker.resetTracking() },
                    onDragEnd = {
                        if (dismissOffsetY.value > 120f) {
                            scope.launch {
                                dismissOffsetY.animateTo(400f, spring(stiffness = 400f, dampingRatio = 0.8f))
                                onDismiss()
                            }
                        } else {
                            scope.launch {
                                dismissOffsetY.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.7f))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dismissOffsetY.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.7f))
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (dragAmount.y > 0 || dismissOffsetY.value > 0) {
                            change.consume()
                            scope.launch {
                                dismissOffsetY.snapTo((dismissOffsetY.value + dragAmount.y).coerceAtLeast(0f))
                            }
                        }
                    }
                )
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(7.dp.toPx())
                    lens(18.dp.toPx(), 22.dp.toPx(), chromaticAberration = true)
                },
                highlight = { Highlight.Default.copy(alpha = 0.80f) },
                shadow = { Shadow(alpha = 0.35f) },
                innerShadow = { InnerShadow(radius = 6.dp, alpha = 0.25f) },
                onDrawSurface = { drawRect(containerColor) }
            )
            .height(lerp(68.dp, 56.dp, inlineProgress))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = "Album Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(lerp(48.dp, 40.dp, inlineProgress))
                    .offset(x = (-2).dp)
                    .scale(pausedArtworkScale)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    color = subtitleColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            val btnWidth = lerp(38.dp, 0.dp, inlineProgress)
            val btnSpacing = lerp(8.dp, 0.dp, inlineProgress)
            val clusterOffset = lerp(0.dp, 4.dp, inlineProgress)

            Row(
                modifier = Modifier.offset(x = clusterOffset),
                horizontalArrangement = Arrangement.spacedBy(btnSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                Box(
                    modifier = Modifier
                        .size(width = btnWidth, height = 38.dp)
                        .graphicsLayer {
                            alpha = 1f - inlineProgress
                            clip = false
                        }
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
                        .elasticGlassTouch(
                            enabled = true,
                            dragEnabled = false,
                            onTap = onPreviousTrack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SimpIcons.SkipPrevious,
                        contentDescription = "Previous",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.15f))
                        .elasticGlassTouch(
                            enabled = true,
                            dragEnabled = false,
                            onTap = onPlayPauseToggle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Next button
                Box(
                    modifier = Modifier
                        .size(width = btnWidth, height = 38.dp)
                        .graphicsLayer {
                            alpha = 1f - inlineProgress
                            clip = false
                        }
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
                        .elasticGlassTouch(
                            enabled = true,
                            dragEnabled = false,
                            onTap = onNextTrack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SimpIcons.SkipNext,
                        contentDescription = "Next",
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
