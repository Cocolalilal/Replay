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
 * One static instance of the Google Sans Flex variable font.
 *
 * Each role family below declares all four weights the app uses (Normal/Medium/SemiBold/Bold)
 * with that role's width/grade/rond/opsz axes held constant. Without the full set, any
 * `copy(fontWeight = ...)` or `fontWeight = ...` override resolves to a weight the family
 * doesn't contain and Compose falls back to the system font — which is how mixed fonts
 * leak into the UI. The variable file carries the whole weight axis, so each entry just
 * pins the axis position matching its [FontWeight].
 */
@Composable
private fun flexFont(
    weight: FontWeight,
    weightAxis: Int,
    width: Float,
    grade: Int,
    rond: Float,
    opsz: Float,
) = Font(
    resource = Res.font.google_sans_flex,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(
        FontVariation.slant(0f),
        FontVariation.width(width),
        FontVariation.weight(weightAxis),
        FontVariation.grade(grade),
        FontVariation.Setting("ROND", rond),
        FontVariation.Setting("opsz", opsz),
    ),
)

/**
 * Section Titles (Quick Picks, Pinned, Your Library, Albums for you):
 * Slant 0, width 105, grad 100, rond 100, optical size 144
 */
@Composable
fun sectionTitleFontFamily(): FontFamily =
    FontFamily(
        flexFont(FontWeight.Normal, 400, 105f, 100, 100f, 144f),
        flexFont(FontWeight.Medium, 500, 105f, 100, 100f, 144f),
        flexFont(FontWeight.SemiBold, 600, 105f, 100, 100f, 144f),
        flexFont(FontWeight.Bold, 700, 105f, 100, 100f, 144f),
    )

/**
 * Item Titles (song titles, card titles in quick picks, pinned, library):
 * Slant 0, width 100, grad 100, rond 100, optical size 48
 */
@Composable
fun itemTitleFontFamily(): FontFamily =
    FontFamily(
        flexFont(FontWeight.Normal, 400, 100f, 100, 100f, 48f),
        flexFont(FontWeight.Medium, 500, 100f, 100, 100f, 48f),
        flexFont(FontWeight.SemiBold, 600, 100f, 100, 100f, 48f),
        flexFont(FontWeight.Bold, 700, 100f, 100, 100f, 48f),
    )

/**
 * Subtitles (artists, playlists, descriptions):
 * Slant 0, width 100, grad 0, rond 100, optical size 36
 */
@Composable
fun itemSubtitleFontFamily(): FontFamily =
    FontFamily(
        flexFont(FontWeight.Normal, 400, 100f, 0, 100f, 36f),
        flexFont(FontWeight.Medium, 500, 100f, 0, 100f, 36f),
        flexFont(FontWeight.SemiBold, 600, 100f, 0, 100f, 36f),
        flexFont(FontWeight.Bold, 700, 100f, 0, 100f, 36f),
    )

/**
 * Now Playing Track Title (NOT wide, no rounding):
 * Slant 0, width 100, grad 0, rond 0, optical size 48
 */
@Composable
fun nowPlayingTitleFontFamily(): FontFamily =
    FontFamily(
        flexFont(FontWeight.Normal, 400, 100f, 0, 0f, 48f),
        flexFont(FontWeight.Medium, 500, 100f, 0, 0f, 48f),
        flexFont(FontWeight.SemiBold, 600, 100f, 0, 0f, 48f),
        flexFont(FontWeight.Bold, 700, 100f, 0, 0f, 48f),
    )

/**
 * Lyrics Font (NOT wide, no rounding, clean reading):
 * Slant 0, width 100, grad 0, rond 0, optical size 36
 */
@Composable
fun lyricsFontFamily(): FontFamily =
    FontFamily(
        flexFont(FontWeight.Normal, 400, 100f, 0, 0f, 36f),
        flexFont(FontWeight.Medium, 500, 100f, 0, 0f, 36f),
        flexFont(FontWeight.SemiBold, 600, 100f, 0, 0f, 36f),
        flexFont(FontWeight.Bold, 700, 100f, 0, 0f, 36f),
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
                fontWeight = FontWeight.SemiBold,
                fontFamily = itemFont,
                color = titleColor,
            ),
        titleMedium =
            TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = nowPlayingFont,
                color = titleColor,
            ),
        titleLarge =
            TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = sectionFont,
                letterSpacing = 0.25.sp,
                color = titleColor,
            ),
        bodySmall =
            TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = subtitleFont,
                color = bodyColor,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = subtitleFont,
                color = bodyColor,
            ),
        bodyLarge =
            TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = lyricsFont,
                color = bodyColor,
            ),
        displayLarge =
            TextStyle(
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = sectionFont,
                letterSpacing = 0.5.sp,
                color = bodyColor,
            ),
        displayMedium =
            TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = sectionFont,
                color = bodyColor,
            ),
        displaySmall =
            TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = sectionFont,
                color = bodyColor,
            ),
        headlineLarge =
            TextStyle(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = lyricsFont,
                color = bodyColor,
            ),
        headlineMedium =
            TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = sectionFont,
                letterSpacing = 0.25.sp,
                color = bodyColor,
            ),
        headlineSmall =
            TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = sectionFont,
                color = bodyColor,
            ),
        labelLarge =
            TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = itemFont,
                color = bodyColor,
            ),
        labelMedium =
            TextStyle(
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = itemFont,
                color = bodyColor,
            ),
        labelSmall =
            TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = subtitleFont,
                color = bodyColor,
            ),
    )
}
