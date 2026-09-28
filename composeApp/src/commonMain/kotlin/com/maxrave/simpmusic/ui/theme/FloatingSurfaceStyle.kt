package com.maxrave.simpmusic.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxrave.domain.manager.DataStoreManager

val LocalFloatingSurfaceStyle = staticCompositionLocalOf { DataStoreManager.FLOATING_SURFACE_GLASSY }
val LocalPerformanceMode = staticCompositionLocalOf { false }

/**
 * Design tokens and colors mirroring the LastChat app surface styling.
 */
object LastChatSurfaceTokens {
    const val GlassAlphaDark = 0.34f
    const val GlassAlphaLight = 0.28f
    const val SoftEdgeAlpha = 0.6f
    val SoftEdgeWidth = 1.dp
    val BlurRadius = 24.dp

    fun surfaceColor(colorScheme: ColorScheme, isDark: Boolean, isBlur: Boolean): Color {
        val opaque = colorScheme.surfaceContainer.copy(alpha = 1f)
        if (!isBlur) return opaque
        val alpha = if (isDark) GlassAlphaDark else GlassAlphaLight
        return opaque.copy(alpha = alpha)
    }

    fun outlineColor(colorScheme: ColorScheme): Color =
        colorScheme.outlineVariant.copy(alpha = SoftEdgeAlpha)

    fun softEdgeBorder(colorScheme: ColorScheme): BorderStroke =
        BorderStroke(SoftEdgeWidth, outlineColor(colorScheme))
}

@Composable
fun isLastChatFloatingStyle(): Boolean {
    val style = LocalFloatingSurfaceStyle.current
    val performance = LocalPerformanceMode.current
    return performance || style == DataStoreManager.FLOATING_SURFACE_LASTCHAT || style == DataStoreManager.FLOATING_SURFACE_LASTCHAT_BLUR
}

@Composable
fun isFloatingSurfaceBlurEnabled(): Boolean {
    val style = LocalFloatingSurfaceStyle.current
    val performance = LocalPerformanceMode.current
    if (performance) return false
    return style == DataStoreManager.FLOATING_SURFACE_GLASSY || style == DataStoreManager.FLOATING_SURFACE_LASTCHAT_BLUR
}
