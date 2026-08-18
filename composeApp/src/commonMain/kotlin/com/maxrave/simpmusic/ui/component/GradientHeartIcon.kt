package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.icon.Favorite
import com.maxrave.simpmusic.ui.icon.SimpIcons

val HeartGradientTop = Color(0xFFFF5A75)
val HeartGradientBottom = Color(0xFF8B001F)

@Composable
fun GradientHeartIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = SimpIcons.Favorite,
            contentDescription = "Liked Songs",
            tint = Color.White,
            modifier = Modifier
                .size(size)
                .graphicsLayer(alpha = 0.99f)
                .drawWithCache {
                    val brush = Brush.verticalGradient(
                        colors = listOf(HeartGradientTop, HeartGradientBottom),
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush, blendMode = BlendMode.SrcAtop)
                    }
                },
        )
    }
}
