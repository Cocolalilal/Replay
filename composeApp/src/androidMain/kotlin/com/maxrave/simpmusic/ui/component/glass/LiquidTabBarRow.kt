package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.maxrave.simpmusic.ui.icon.Home
import com.maxrave.simpmusic.ui.icon.LibraryMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LiquidTabBarRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit,
    backdrop: LayerBackdrop,
    collapseProgress: Float,
    modifier: Modifier = Modifier,
) {
    val tabs = remember {
        listOf(
            TabItem("Home", SimpIcons.Home),
            TabItem("Library", SimpIcons.LibraryMusic)
        )
    }

    LiquidBottomTabs(
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        backdrop = backdrop,
        tabs = tabs,
        collapseProgress = collapseProgress,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun LiquidBottomTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: LayerBackdrop,
    tabs: List<TabItem>,
    collapseProgress: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val isDark = isSystemInDarkTheme()
    val containerColor = Color(if (isDark) 0xFF1E1E1E else 0xFFFAFAFA).copy(alpha = 0.18f)
    val activeColor = Color(0xFF82BEFF)
    val inactiveColor = Color.White.copy(alpha = 0.84f)

    val currentIndex = remember {
        mutableIntStateOf(selectedTabIndex.coerceIn(0, tabs.lastIndex))
    }
    val isDragging = remember { booleanArrayOf(false) }
    val contentBackdrop = rememberLayerBackdrop()
    val touchAnimatable = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier.graphicsLayer { clip = false }
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val totalHeightPx = constraints.maxHeight.toFloat()
        val tabCount = tabs.size.toFloat()
        val liveTabWidthPx = if (tabCount > 0) totalWidthPx / tabCount else 0f
        val tabWidthState = remember { mutableFloatStateOf(liveTabWidthPx) }

        LaunchedEffect(liveTabWidthPx) {
            tabWidthState.floatValue = liveTabWidthPx
        }

        val drag = remember(scope, tabCount) {
            DampedDragAnimation(
                animationScope = scope,
                initialValue = selectedTabIndex.toFloat(),
                valueRange = 0f..(tabs.size - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.15f,
                onDragStarted = { _, _ ->
                    isDragging[0] = false
                },
                onDragStopped = { damped ->
                    if (isDragging[0]) {
                        val target = damped.targetValue.roundToInt().coerceIn(0, tabs.lastIndex)
                        currentIndex.intValue = target
                        damped.animateToValue(target.toFloat())
                    }
                },
                onDrag = { damped, _, dragAmount ->
                    if (dragAmount.x != 0f) {
                        isDragging[0] = true
                    }
                    val delta = (dragAmount.x / tabWidthState.floatValue) * (if (isLtr) 1f else -1f)
                    val newTarget = (damped.targetValue + delta).coerceIn(0f, (tabs.size - 1).toFloat())
                    damped.updateValue(newTarget)
                }
            )
        }

        LaunchedEffect(selectedTabIndex, tabs) {
            currentIndex.intValue = selectedTabIndex
            drag.animateToValue(selectedTabIndex.toFloat())
        }

        LaunchedEffect(drag, onTabSelected) {
            // When settled or selected
        }

        val selectorX = if (isLtr) {
            (drag.value * liveTabWidthPx) + with(density) { 4.dp.toPx() }
        } else {
            totalWidthPx - ((drag.value + 1f) * liveTabWidthPx) - with(density) { 4.dp.toPx() }
        }
        val selectorWidthPx = liveTabWidthPx - with(density) { 8.dp.toPx() }
        val selectorHeightPx = totalHeightPx - with(density) { 8.dp.toPx() }
        val selectorOffsetYPx = (totalHeightPx - selectorHeightPx) / 2f

        val floatAmount = ((drag.pressProgress * (1f - collapseProgress)) + touchAnimatable.value).fastCoerceIn(0f, 1f)
        val textAlpha = (1f - (2.5f * collapseProgress)).fastCoerceIn(0f, 1f)
        val pillAlpha = (1f - (2f * collapseProgress)).fastCoerceIn(0f, 1f)

        // 1. Container Backdrop capsule background with contentBackdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val fadeVal = ((collapseProgress * 2f) - 0.8f) * 2f
                    alpha *= lerp(1f, fadeVal.fastCoerceIn(0f, 1f), collapseProgress)
                    scaleX *= 1f + 0.03f * floatAmount
                    scaleY *= 1f + 0.02f * floatAmount
                }
                .elasticGlassTouch(
                    enabled = collapseProgress > 0.4f,
                    dragEnabled = false,
                    onTap = {
                        if (collapseProgress > 0.4f) {
                            onTabSelected(currentIndex.intValue)
                        }
                    }
                )
                .layerBackdrop(contentBackdrop)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
            )

            // 2. Tab items (text and icon) rendered into contentBackdrop
            if (textAlpha > 0f) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(textAlpha),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val dist = abs(index.toFloat() - drag.value)
                        val progress = (1f - dist).fastCoerceIn(0f, 1f)
                        val tabColor = lerp(inactiveColor, activeColor, progress)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp),
                                    tint = tabColor
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    color = tabColor,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Floating Selection Bubble: Combines app backdrop and tab contentBackdrop to refract text & icons behind it
        if (pillAlpha > 0f) {
            val combinedBackdrop = rememberCombinedBackdrop(backdrop, contentBackdrop)
            Box(
                modifier = Modifier
                    .offset { IntOffset(selectorX.roundToInt(), selectorOffsetYPx.roundToInt()) }
                    .layout { measurable, _ ->
                        val velocity = (drag.velocity / 10f) * (1f - collapseProgress)
                        val clampedVelocity = (0.25f * velocity).fastCoerceIn(-0.12f, 0.12f)
                        val floatGrowX = 1f + 0.28f * floatAmount
                        val floatGrowY = 1f + 0.45f * floatAmount
                        val stretchX = floatGrowX * (1f + clampedVelocity)
                        val stretchY = floatGrowY * (1f - 0.5f * clampedVelocity)
                        val targetW = (selectorWidthPx * stretchX).roundToInt()
                        val targetH = (selectorHeightPx * stretchY).roundToInt()
                        val placeable = measurable.measure(Constraints.fixed(targetW, targetH))
                        layout(selectorWidthPx.roundToInt(), selectorHeightPx.roundToInt()) {
                            placeable.placeRelative(
                                (selectorWidthPx.roundToInt() - targetW) / 2,
                                (selectorHeightPx.roundToInt() - targetH) / 2
                            )
                        }
                    }
                    .graphicsLayer {
                        alpha = pillAlpha
                        clip = false
                    }
                    .drawBackdrop(
                        backdrop = combinedBackdrop,
                        shape = { Capsule() },
                        effects = {
                            if (floatAmount > 0.05f) {
                                val lensRadius = lerp(8f, 26f, floatAmount).dp.toPx()
                                val lensDepth = lerp(10f, 32f, floatAmount).dp.toPx()
                                lens(lensRadius, lensDepth, chromaticAberration = true)
                            }
                        },
                        highlight = { Highlight.Default.copy(alpha = lerp(0f, 0.9f, floatAmount)) },
                        shadow = { Shadow(alpha = lerp(0.25f, 0.45f, floatAmount)) },
                        innerShadow = { InnerShadow(radius = 8.dp, alpha = lerp(0.15f, 0.45f, floatAmount)) },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = lerp(0.12f, 0.10f, floatAmount)))
                            if (floatAmount > 0f) {
                                drawRect(Color.Black.copy(alpha = floatAmount * 0.03f))
                            }
                        }
                    )
            )
        }

        // 4. Interactive touch layer for tabs
        if (textAlpha > 0f) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(textAlpha)
                    .then(drag.modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(Capsule())
                            .semantics { role = Role.Tab }
                            .pointerInput(index) {
                                detectTapGestures(
                                    onPress = {
                                        scope.launch {
                                            touchAnimatable.animateTo(1f, spring(stiffness = 500f, dampingRatio = 0.8f))
                                        }
                                        tryAwaitRelease()
                                        scope.launch {
                                            touchAnimatable.animateTo(0f, spring(stiffness = 500f, dampingRatio = 0.8f))
                                        }
                                    },
                                    onTap = {
                                        currentIndex.intValue = index
                                        scope.launch {
                                            drag.animateToValue(index.toFloat())
                                        }
                                        onTabSelected(index)
                                    }
                                )
                            }
                    )
                }
            }
        }

        // 5. Collapsed state single icon
        if (collapseProgress > 0.4f) {
            val currentTab = tabs.getOrNull(currentIndex.intValue) ?: tabs[0]
            val singleIconAlpha = ((collapseProgress * 2f) - 0.8f).fastCoerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(singleIconAlpha)
                    .clickable(
                        enabled = collapseProgress > 0.4f,
                        onClick = { onTabSelected(currentIndex.intValue) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = currentTab.icon,
                    contentDescription = currentTab.title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
