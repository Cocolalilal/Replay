package com.maxrave.simpmusic.extension

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default scroll threshold from the top before considering the list scrolled away.
 * Prevents instant minimization of the bottom tab bar on minor scroll movements.
 */
val DEFAULT_SCROLL_TOP_THRESHOLD_DP: Dp = 90.dp

@Composable
fun LazyListState.TrackScrolling(
    topThreshold: Dp = DEFAULT_SCROLL_TOP_THRESHOLD_DP,
    onScrolling: (isAtTop: Boolean, direction: Int) -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { topThreshold.roundToPx() }
    val prevScrollPosition = rememberSaveable(this) {
        mutableFloatStateOf(firstVisibleItemIndex + firstVisibleItemScrollOffset / 10000.0f)
    }

    LaunchedEffect(this, thresholdPx) {
        snapshotFlow {
            val idx = firstVisibleItemIndex
            val off = firstVisibleItemScrollOffset
            Triple(idx == 0 && off < thresholdPx, idx, off)
        }.collect { (isAtTop, idx, off) ->
            val position = idx + (off / 10000.0f)
            val direction = if (position > prevScrollPosition.floatValue) {
                -1
            } else if (position < prevScrollPosition.floatValue) {
                1
            } else {
                0
            }
            prevScrollPosition.floatValue = position
            onScrolling(isAtTop, direction)
        }
    }
}

@Composable
fun LazyGridState.TrackScrolling(
    topThreshold: Dp = DEFAULT_SCROLL_TOP_THRESHOLD_DP,
    onScrolling: (isAtTop: Boolean, direction: Int) -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { topThreshold.roundToPx() }
    val prevScrollPosition = rememberSaveable(this) {
        mutableFloatStateOf(firstVisibleItemIndex + firstVisibleItemScrollOffset / 10000.0f)
    }

    LaunchedEffect(this, thresholdPx) {
        snapshotFlow {
            val idx = firstVisibleItemIndex
            val off = firstVisibleItemScrollOffset
            Triple(idx == 0 && off < thresholdPx, idx, off)
        }.collect { (isAtTop, idx, off) ->
            val position = idx + (off / 10000.0f)
            val direction = if (position > prevScrollPosition.floatValue) {
                -1
            } else if (position < prevScrollPosition.floatValue) {
                1
            } else {
                0
            }
            prevScrollPosition.floatValue = position
            onScrolling(isAtTop, direction)
        }
    }
}
