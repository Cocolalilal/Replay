package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.SimpIcons

val HeartGradientTop = Color(0xFFFF5A75)
val HeartGradientBottom = Color(0xFF8B001F)

val LikedSongsGradientColors = listOf(
    Color(0xFF8B001F), // Dark crimson
    Color(0xFFFF5A75), // Vibrant coral
)

val LikedSongsGradientBrush = Brush.linearGradient(
    colors = LikedSongsGradientColors,
)

/**
 * Canonical cover for the Liked Songs playlist: red gradient background with a solid white heart.
 */
@Composable
fun LikedSongsCover(
    modifier: Modifier = Modifier,
    iconSize: Dp? = null,
    shape: Shape? = null,
) {
    val boxModifier = if (shape != null) {
        modifier.clip(shape).background(LikedSongsGradientBrush)
    } else {
        modifier.background(LikedSongsGradientBrush)
    }
    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (iconSize != null) {
            Icon(
                imageVector = SimpIcons.Favorite,
                contentDescription = "Liked Songs",
                tint = Color.White,
                modifier = Modifier.size(iconSize),
            )
        } else {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val heartSize = min(maxWidth, maxHeight) * 0.45f
                Icon(
                    imageVector = SimpIcons.Favorite,
                    contentDescription = "Liked Songs",
                    tint = Color.White,
                    modifier = Modifier.size(heartSize),
                )
            }
        }
    }
}

@Composable
fun GradientHeartIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp? = null,
) {
    LikedSongsCover(
        modifier = modifier.size(size),
        iconSize = iconSize ?: (size * 0.5f),
    )
}
