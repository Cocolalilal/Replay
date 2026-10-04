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

    /**
     * Src-over stroke that lightens the blur under the edge. On a black backdrop it
     * matches [outlineColor] composited on glass-over-black.
     */
    fun softEdgeLightenStroke(
        outline: Color,
        surface: Color,
        glassAlpha: Float,
        edgeAlpha: Float = SoftEdgeAlpha,
    ): Color {
        fun glass(channel: Float) = channel * glassAlpha
        fun current(outlineChannel: Float, glassChannel: Float) =
            outlineChannel * edgeAlpha + glassChannel * (1f - edgeAlpha)

        val glassRed = glass(surface.red)
        val glassGreen = glass(surface.green)
        val glassBlue = glass(surface.blue)
        val currentRed = current(outline.red, glassRed)
        val currentGreen = current(outline.green, glassGreen)
        val currentBlue = current(outline.blue, glassBlue)

        fun ratio(currentChannel: Float, glassChannel: Float): Float {
            val denom = 1f - glassChannel
            if (denom <= 0.0001f) return 0f
            return (currentChannel - glassChannel) / denom
        }

        val alpha = maxOf(
            ratio(currentRed, glassRed),
            ratio(currentGreen, glassGreen),
            ratio(currentBlue, glassBlue),
        ).coerceIn(0f, 1f)
        if (alpha <= 0.0001f) return outline.copy(alpha = edgeAlpha)

        fun source(currentChannel: Float, glassChannel: Float): Float =
            (glassChannel + (currentChannel - glassChannel) / alpha).coerceIn(0f, 1f)

        return Color(
            red = source(currentRed, glassRed),
            green = source(currentGreen, glassGreen),
            blue = source(currentBlue, glassBlue),
            alpha = alpha,
        )
    }

    fun softEdgeBorder(
        colorScheme: ColorScheme,
        bleedColor: Color? = null,
        lightenBackdrop: Boolean = false,
        isDark: Boolean = true,
    ): BorderStroke {
        if (!lightenBackdrop) {
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
        val glassAlpha = if (isDark) GlassAlphaDark else GlassAlphaLight
        val stroke = softEdgeLightenStroke(
            outline = colorScheme.outlineVariant,
            surface = colorScheme.surfaceContainer,
            glassAlpha = glassAlpha,
        )
        val color =
            if (bleedColor != null) {
                lerp(Color(stroke.red, stroke.green, stroke.blue), bleedColor.copy(alpha = 1f), BleedMix)
                    .copy(alpha = stroke.alpha)
            } else {
                stroke
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
