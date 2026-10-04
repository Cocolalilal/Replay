package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.LastChatSurfaceTokens
import com.maxrave.simpmusic.ui.theme.LocalGlassBleedColor
import com.maxrave.simpmusic.ui.theme.isFloatingSurfaceBlurEnabled
import com.maxrave.simpmusic.ui.theme.isLastChatFloatingStyle
import com.maxrave.simpmusic.ui.theme.itemSubtitleFontFamily
import kotlin.math.roundToInt

@Composable
fun BottomNavigationOrchestrator(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    scrollCollapseProgress: Float,
    backdrop: LayerBackdrop,
    trackTitle: String,
    trackArtist: String,
    artworkUrl: String,
    isPlaying: Boolean,
    isShowMiniPlayer: Boolean,
    onPlayPauseToggle: () -> Unit,
    onPreviousTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onExpandFullPlayer: () -> Unit,
    onDismissMiniPlayer: () -> Unit,
    searchText: String = "",
    onSearchTextChange: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    onSearchFieldTapped: () -> Unit = {},
    onClearSearch: () -> Unit = {},
    onExpandRequested: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val searchProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.88f),
        label = "searchProgress"
    )

    val miniPlayerPresence by animateFloatAsState(
        targetValue = if (isShowMiniPlayer) 1f else 0f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.85f),
        label = "miniPlayerPresence"
    )

    val rawCollapse = if (isSearchActive) 0f else scrollCollapseProgress
    val effectiveCollapse = maxOf(searchProgress, rawCollapse).fastCoerceIn(0f, 1f)

    // Strong nearby colour from now-playing artwork — lightly reflected on glass borders.
    var glassBleedColor by remember { mutableStateOf<Color?>(null) }

    // Height follows the same springs as the collapse/presence animations so the
    // bar transitions smoothly between expanded and collapsed states.
    val orchestratorHeight = 56.dp + 89.dp * miniPlayerPresence * (1f - rawCollapse)

    CompositionLocalProvider(LocalGlassBleedColor provides glassBleedColor) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(orchestratorHeight)
            .graphicsLayer { clip = false }
    ) {
        val density = LocalDensity.current
        val totalWidthPx = constraints.maxWidth.toFloat()
        val currentContainerHeightDp = orchestratorHeight

        val bottomRowYPx = with(density) { (currentContainerHeightDp - 56.dp).toPx() }
        val topRowYPx = with(density) { (currentContainerHeightDp - 56.dp - 10.dp - 64.dp).toPx() }

        val searchButtonWidthPx = with(density) { 56.dp.toPx() }
        val spacingPx = with(density) { 10.dp.toPx() }

        val availableSpacePx = totalWidthPx - searchButtonWidthPx - spacingPx
        val maxAllowedTabBarWidthPx = with(density) { 240.dp.toPx() }
        val expandedTabBarWidthPx = minOf(availableSpacePx, maxAllowedTabBarWidthPx)
        
        val currentTabBarWidthPx = lerp(expandedTabBarWidthPx, searchButtonWidthPx, effectiveCollapse)

        Box(
            modifier = Modifier
                .offset { IntOffset(0, bottomRowYPx.roundToInt()) }
                .width(with(density) { currentTabBarWidthPx.toDp() })
                .height(56.dp)
                .graphicsLayer { clip = false }
        ) {
            LiquidTabBarRow(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { index ->
                    // Minimized pill: expand chrome only. Never reload / pop the
                    // nested screen the user is mid-way through.
                    if (effectiveCollapse > 0.4f) {
                        onExpandRequested?.invoke()
                    } else {
                        onTabSelected(index)
                    }
                },
                onSearchClick = { onSearchActiveChange(true) },
                backdrop = backdrop,
                collapseProgress = effectiveCollapse
            )
        }

        val searchAlpha = (1f - (2f * rawCollapse)).fastCoerceIn(0f, 1f)
        if (searchAlpha > 0.01f || searchProgress > 0.01f || isSearchActive) {
            val searchWidthPx = lerp(searchButtonWidthPx, (totalWidthPx - searchButtonWidthPx) - spacingPx, searchProgress)
            val searchXPx = lerp(totalWidthPx - searchButtonWidthPx, searchButtonWidthPx + spacingPx, searchProgress)

            Box(
                modifier = Modifier
                    .offset { IntOffset(searchXPx.roundToInt(), bottomRowYPx.roundToInt()) }
                    .width(with(density) { searchWidthPx.toDp() })
                    .height(56.dp)
                    .graphicsLayer {
                        alpha = if (isSearchActive) 1f else searchAlpha
                        scaleX = if (isSearchActive) 1f else (1f - (rawCollapse * 0.5f))
                        scaleY = if (isSearchActive) 1f else (1f - (0.5f * rawCollapse))
                    }
            ) {
                SearchFieldOrCircle(
                    isSearchActive = isSearchActive,
                    searchProgress = searchProgress,
                    backdrop = backdrop,
                    searchText = searchText,
                    onSearchTextChange = onSearchTextChange,
                    onSearchSubmit = onSearchSubmit,
                    onSearchFieldTapped = onSearchFieldTapped,
                    onCircleClick = { onSearchActiveChange(true) },
                    onClearClick = {
                        onSearchTextChange("")
                        onClearSearch()
                    }
                )
            }
        }

        if (miniPlayerPresence > 0.01f) {
            val playerWidthPx = lerp(totalWidthPx, totalWidthPx - searchButtonWidthPx - spacingPx, rawCollapse)
            val playerXPx = lerp(0f, searchButtonWidthPx + spacingPx, rawCollapse)
            val playerYPx = lerp(topRowYPx, bottomRowYPx, rawCollapse)
            val entryOffsetYPx = with(density) { (1f - miniPlayerPresence) * 40.dp.toPx() }

            Box(
                modifier = Modifier
                    .offset { IntOffset(playerXPx.roundToInt(), (playerYPx + entryOffsetYPx).roundToInt()) }
                    .width(with(density) { playerWidthPx.toDp() })
                    .height(lerp(64.dp.value, 56.dp.value, rawCollapse).dp)
                    .graphicsLayer {
                        alpha = miniPlayerPresence
                        scaleX = lerp(0.92f, 1f, miniPlayerPresence)
                        scaleY = lerp(0.92f, 1f, miniPlayerPresence)
                    }
            ) {
                LiquidMiniPlayer(
                    backdrop = backdrop,
                    title = trackTitle,
                    artist = trackArtist,
                    artworkUrl = artworkUrl,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = onPlayPauseToggle,
                    onPreviousTrack = onPreviousTrack,
                    onNextTrack = onNextTrack,
                    onExpandFullPlayer = onExpandFullPlayer,
                    onDismiss = onDismissMiniPlayer,
                    isInline = rawCollapse > 0.4f,
                    onBleedColor = { glassBleedColor = it },
                )
            }
        }
    }
    } // CompositionLocalProvider LocalGlassBleedColor
}

@Composable
private fun SearchFieldOrCircle(
    isSearchActive: Boolean,
    searchProgress: Float,
    backdrop: LayerBackdrop,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onSearchFieldTapped: () -> Unit,
    onCircleClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val isLastChat = isLastChatFloatingStyle()
    val isBlur = isFloatingSurfaceBlurEnabled()
    val containerColor = (if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)).copy(alpha = if (isDark) 0.38f else 0.45f)
    val surfaceColor = if (isLastChat) LastChatSurfaceTokens.surfaceColor(colorScheme = MaterialTheme.colorScheme, isDark = isDark, isBlur = isBlur) else containerColor
    val glassBleed = LocalGlassBleedColor.current
    val outlineBorder = LastChatSurfaceTokens.softEdgeBorder(
        colorScheme = MaterialTheme.colorScheme,
        bleedColor = glassBleed,
        lightenBackdrop = isLastChat && isBlur,
        isDark = isDark,
    )
    val textColor = if (isDark) Color.White else Color.Black
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isLastChat) {
                    Modifier
                        .elasticGlassTouch(
                            enabled = true,
                            dragEnabled = false,
                            onTap = {
                                if (!isSearchActive) {
                                    onCircleClick()
                                }
                            }
                        )
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
                } else if (isBlur) {
                    Modifier
                        .clip(Capsule())
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                blur(LastChatSurfaceTokens.BlurRadius.toPx())
                            },
                            highlight = null,
                            shadow = null,
                            innerShadow = null,
                            onDrawSurface = { drawRect(surfaceColor) }
                        )
                        .clickable(
                            enabled = !isSearchActive,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onCircleClick
                        )
                        .border(outlineBorder, Capsule())
                } else {
                    Modifier
                        .clip(Capsule())
                        .background(surfaceColor)
                        .clickable(
                            enabled = !isSearchActive,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onCircleClick
                        )
                        .border(outlineBorder, Capsule())
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (searchProgress < 0.5f) {
            Icon(
                imageVector = SimpIcons.Search,
                contentDescription = "Search",
                tint = textColor,
                modifier = Modifier.size(26.dp)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = SimpIcons.Search,
                    contentDescription = "Search",
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchText.isEmpty()) {
                        Text(
                            text = "Search songs, artists...",
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontFamily = itemSubtitleFontFamily(),
                            fontWeight = FontWeight.Normal
                        )
                    }
                    val textStyle = TextStyle(
                        color = textColor,
                        fontSize = 14.sp,
                        fontFamily = itemSubtitleFontFamily(),
                        fontWeight = FontWeight.Normal
                    )
                    BasicTextField(
                        value = searchText,
                        onValueChange = onSearchTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onSearchFieldTapped()
                                }
                            },
                        textStyle = textStyle,
                        cursorBrush = SolidColor(if (isDark) Color.White else Color.Black),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                            if (searchText.isNotBlank()) {
                                onSearchSubmit(searchText.trim())
                            }
                        })
                    )
                }
                if (searchText.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onClearClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SimpIcons.Close,
                            contentDescription = "Clear",
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
