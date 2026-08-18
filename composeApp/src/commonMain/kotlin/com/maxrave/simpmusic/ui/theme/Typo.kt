package com.maxrave.simpmusic.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.google_sans_flex

/**
 * Replay Logo Font:
 * Slant 0, width 110, weight 500, grad 0, rond 0, optical size 144
 */
@Composable
fun replayLogoFontFamily(): FontFamily =
    FontFamily(
        Font(
            resource = Res.font.google_sans_flex,
            weight = FontWeight(500),
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.slant(0f),
                FontVariation.width(110f),
                FontVariation.weight(500),
                FontVariation.grade(0),
                FontVariation.Setting("ROND", 0f),
                FontVariation.Setting("opsz", 144f),
            ),
        ),
    )

/**
 * Section Titles (Quick Picks, Pinned, Your Library, Albums for you):
 * Slant 0, width 105, weight 400, grad 100, rond 100, optical size 144
 */
@Composable
fun sectionTitleFontFamily(): FontFamily =
    FontFamily(
        Font(
            resource = Res.font.google_sans_flex,
            weight = FontWeight(400),
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.slant(0f),
                FontVariation.width(105f),
                FontVariation.weight(400),
                FontVariation.grade(100),
                FontVariation.Setting("ROND", 100f),
                FontVariation.Setting("opsz", 144f),
            ),
        ),
    )

/**
 * Item Titles (song titles, card titles in quick picks, pinned, library):
 * Slant 0, width 100, weight 400, grad 100, rond 100, optical size 48
 */
@Composable
fun itemTitleFontFamily(): FontFamily =
    FontFamily(
        Font(
            resource = Res.font.google_sans_flex,
            weight = FontWeight(400),
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.slant(0f),
                FontVariation.width(100f),
                FontVariation.weight(400),
                FontVariation.grade(100),
                FontVariation.Setting("ROND", 100f),
                FontVariation.Setting("opsz", 48f),
            ),
        ),
    )

/**
 * Subtitles (artists, playlists, descriptions):
 * Slant 0, width 100, weight 400, grad 0, rond 100, optical size 36
 */
@Composable
fun itemSubtitleFontFamily(): FontFamily =
    FontFamily(
        Font(
            resource = Res.font.google_sans_flex,
            weight = FontWeight(400),
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.slant(0f),
                FontVariation.width(100f),
                FontVariation.weight(400),
                FontVariation.grade(0),
                FontVariation.Setting("ROND", 100f),
                FontVariation.Setting("opsz", 36f),
            ),
        ),
    )

/**
 * Now Playing Track Title (NOT wide, bold/semi-bold):
 * Slant 0, width 100, weight 600, grad 0, rond 0, optical size 48
 */
@Composable
fun nowPlayingTitleFontFamily(): FontFamily =
    FontFamily(
        Font(
            resource = Res.font.google_sans_flex,
            weight = FontWeight(600),
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.slant(0f),
                FontVariation.width(100f),
                FontVariation.weight(600),
                FontVariation.grade(0),
                FontVariation.Setting("ROND", 0f),
                FontVariation.Setting("opsz", 48f),
            ),
        ),
    )

/**
 * Lyrics Font (NOT wide, bold/medium, clean reading):
 * Slant 0, width 100, weight 600, grad 0, rond 0, optical size 36
 */
@Composable
fun lyricsFontFamily(): FontFamily =
    FontFamily(
        Font(
            resource = Res.font.google_sans_flex,
            weight = FontWeight(600),
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.slant(0f),
                FontVariation.width(100f),
                FontVariation.weight(600),
                FontVariation.grade(0),
                FontVariation.Setting("ROND", 0f),
                FontVariation.Setting("opsz", 36f),
            ),
        ),
    )

/**
 * Default fallback typography
 */
@Composable
fun fontFamily(): FontFamily = itemTitleFontFamily()

val LocalForceDarkText = staticCompositionLocalOf { false }

@Composable
fun typo(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    forceDark: Boolean = LocalForceDarkText.current,
): Typography {
    val sectionFont = sectionTitleFontFamily()
    val itemFont = itemTitleFontFamily()
    val subtitleFont = itemSubtitleFontFamily()
    val nowPlayingFont = nowPlayingTitleFontFamily()
    val lyricsFont = lyricsFontFamily()

    val titleColor = if (forceDark) Color.White else colorScheme.onBackground
    val bodyColor = if (forceDark) Color(0xFFA8A8A8) else colorScheme.onSurfaceVariant

    return Typography(
        titleSmall =
            TextStyle(
                fontSize = 13.sp,
                fontFamily = itemFont,
                color = titleColor,
            ),
        titleMedium =
            TextStyle(
                fontSize = 18.sp,
                fontFamily = nowPlayingFont,
                color = titleColor,
            ),
        titleLarge =
            TextStyle(
                fontSize = 22.sp,
                fontFamily = sectionFont,
                letterSpacing = 0.25.sp,
                color = titleColor,
            ),
        bodySmall =
            TextStyle(
                fontSize = 11.sp,
                fontFamily = subtitleFont,
                color = bodyColor,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 12.5.sp,
                fontFamily = subtitleFont,
                color = bodyColor,
            ),
        bodyLarge =
            TextStyle(
                fontSize = 15.sp,
                fontFamily = lyricsFont,
                color = bodyColor,
            ),
        displayLarge =
            TextStyle(
                fontSize = 26.sp,
                fontFamily = sectionFont,
                letterSpacing = 0.5.sp,
                color = bodyColor,
            ),
        displayMedium =
            TextStyle(
                fontSize = 22.sp,
                fontFamily = sectionFont,
                color = bodyColor,
            ),
        displaySmall =
            TextStyle(
                fontSize = 18.sp,
                fontFamily = sectionFont,
                color = bodyColor,
            ),
        headlineLarge =
            TextStyle(
                fontSize = 26.sp,
                fontFamily = lyricsFont,
                color = bodyColor,
            ),
        headlineMedium =
            TextStyle(
                fontSize = 22.sp,
                fontFamily = sectionFont,
                letterSpacing = 0.25.sp,
                color = bodyColor,
            ),
        headlineSmall =
            TextStyle(
                fontSize = 18.sp,
                fontFamily = sectionFont,
                color = bodyColor,
            ),
        labelLarge =
            TextStyle(
                fontSize = 16.sp,
                fontFamily = itemFont,
                color = bodyColor,
            ),
        labelMedium =
            TextStyle(
                fontSize = 14.5.sp,
                fontFamily = itemFont,
                color = bodyColor,
            ),
        labelSmall =
            TextStyle(
                fontSize = 12.sp,
                fontFamily = subtitleFont,
                color = bodyColor,
            ),
    )
}
