package com.maxrave.simpmusic.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.maxrave.domain.manager.DataStoreManager

val LocalFloatingSurfaceStyle = staticCompositionLocalOf { DataStoreManager.FLOATING_SURFACE_GLASSY }
val LocalPerformanceMode = staticCompositionLocalOf { false }

/**
 * Strong nearby colour (typically now-playing / album-art vibrant swatch) lightly
 * reflected onto glass surface borders. Null = no bleed (neutral outline only).
 */
val LocalGlassBleedColor = staticCompositionLocalOf<Color?> { null }

/**
 * Design tokens and colors mirroring the LastChat app surface styling.
 */
object LastChatSurfaceTokens {
    const val GlassAlphaDark = 0.34f
    const val GlassAlphaLight = 0.28f
    const val SoftEdgeAlpha = 0.6f
    /** How strongly a nearby vibrant colour tints the glass border (keep light). */
    const val BleedMix = 0.28f
    const val BleedAlpha = 0.45f
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

    fun softEdgeBorder(colorScheme: ColorScheme, bleedColor: Color? = null): BorderStroke {
        val base = outlineColor(colorScheme)
        val color =
            if (bleedColor != null) {
                lerp(base.copy(alpha = 1f), bleedColor.copy(alpha = 1f), BleedMix)
                    .copy(alpha = BleedAlpha)
            } else {
                base
            }
        return BorderStroke(SoftEdgeWidth, color)
    }
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
