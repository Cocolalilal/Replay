package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.maxrave.simpmusic.ui.component.DampedDragAnimation
import com.maxrave.simpmusic.ui.icon.Home
import com.maxrave.simpmusic.ui.icon.LibraryMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
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
    val containerColor = (if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)).copy(alpha = 0.18f)
    val activeColor = Color(0xFFFA2D48)
    val inactiveColor = Color.White.copy(alpha = 0.84f)

    var currentIndex by remember {
        mutableIntStateOf(selectedTabIndex.coerceIn(0, tabs.lastIndex))
    }
    val draggedFlag = remember { booleanArrayOf(false) }
    val tabsBackdrop = rememberLayerBackdrop()
    val tapPulse = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier.graphicsLayer { clip = false }
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val totalHeightPx = constraints.maxHeight.toFloat()
        val tabCount = tabs.size
        val tabWidthPx = if (tabCount > 0) totalWidthPx / tabCount else 0f
        val liveTabWidthPx = remember { mutableFloatStateOf(tabWidthPx) }
        liveTabWidthPx.floatValue = tabWidthPx

        val drag = remember(scope, tabCount) {
            DampedDragAnimation(
                animationScope = scope,
                initialValue = currentIndex.toFloat(),
                valueRange = 0f..(tabCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.15f,
                onDragStarted = {
                    draggedFlag[0] = false
                },
                onDragStopped = {
                    if (draggedFlag[0]) {
                        val target = targetValue.roundToInt().coerceIn(0, tabs.lastIndex)
                        currentIndex = target
                        animateToValue(target.toFloat())
                    }
                },
                onDrag = { _, dragAmount ->
                    if (dragAmount.x != 0f) {
                        draggedFlag[0] = true
                    }
                    val currentTarget = targetValue
                    val newTarget = (currentTarget + (dragAmount.x / liveTabWidthPx.floatValue) * if (isLtr) 1f else -1f)
                        .fastCoerceIn(0f, (tabs.size - 1).toFloat())
                    updateValue(newTarget)
                }
            )
        }

        LaunchedEffect(selectedTabIndex) {
            if (selectedTabIndex in tabs.indices && selectedTabIndex != currentIndex) {
                currentIndex = selectedTabIndex
                drag.animateToValue(selectedTabIndex.toFloat())
            }
        }

        val floatAmount = drag.pressProgress
        val pillWidthPx = tabWidthPx - with(density) { 8.dp.toPx() }
        val pillHeightPx = totalHeightPx - with(density) { 8.dp.toPx() }
        val selectorX = if (isLtr) drag.value * tabWidthPx + with(density) { 4.dp.toPx() } else (tabCount - 1 - drag.value) * tabWidthPx + with(density) { 4.dp.toPx() }
        val selectorOffsetYPx = with(density) { 4.dp.toPx() }
        val selectionAlpha = (1f - collapseProgress * 2.5f).fastCoerceIn(0f, 1f)

        // Backdrop capsule background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val fadeProgress = ((collapseProgress * 2f) - 0.8f) * 2f
                    alpha = (alpha * lerp(1f, fadeProgress.fastCoerceIn(0f, 1f), collapseProgress))
                    scaleX *= (1f + 0.03f * floatAmount)
                    scaleY *= (1f + 0.02f * floatAmount)
                }
                .elasticGlassTouch(
                    enabled = true,
                    dragEnabled = false,
                    onTap = {
                        if (collapseProgress > 0.4f) {
                            onTabSelected(currentIndex)
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
        )

        // Sliding indicator pill
        if (selectionAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(selectorX.roundToInt(), selectorOffsetYPx.roundToInt()) }
                    .layout { measurable, _ ->
                        val velocity = (drag.velocity / 10f) * (1f - collapseProgress)
                        val stretch = (0.25f * velocity).fastCoerceIn(-0.12f, 0.12f)
                        val floatGrowX = 1f + 0.28f * floatAmount
                        val floatGrowY = 1f + 0.45f * floatAmount
                        val sx = floatGrowX * (1f + stretch)
                        val sy = floatGrowY * (1f - 0.5f * stretch)
                        val w = (pillWidthPx * sx).roundToInt()
                        val h = (pillHeightPx * sy).roundToInt()
                        val placeable = measurable.measure(Constraints.fixed(w, h))
                        layout(pillWidthPx.roundToInt(), pillHeightPx.roundToInt()) {
                            val offsetX = (pillWidthPx.roundToInt() - w) / 2
                            val offsetY = (pillHeightPx.roundToInt() - h) / 2
                            placeable.placeRelative(offsetX, offsetY)
                        }
                    }
                    .graphicsLayer {
                        alpha = selectionAlpha
                        clip = false
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
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

        // Tabs items Row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(drag.modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = currentIndex == index
                val itemAlpha = (1f - collapseProgress * 2.5f).fastCoerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .semantics { role = Role.Tab }
                        .elasticGlassTouch(
                            enabled = true,
                            dragEnabled = false,
                            onTap = {
                                currentIndex = index
                                drag.animateToValue(index.toFloat())
                                onTabSelected(index)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (collapseProgress < 0.4f) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) activeColor else inactiveColor,
                            modifier = Modifier
                                .graphicsLayer { alpha = itemAlpha }
                                .size(24.dp)
                        )
                    } else if (index == currentIndex) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = activeColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
