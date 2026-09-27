package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.theme.LocalPerformanceMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.ui.theme.lyricsFontFamily
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.simpmusic.extension.KeepScreenOn
import com.maxrave.simpmusic.extension.ParsedRichSyncLine
import com.maxrave.simpmusic.extension.animateScrollAndCentralizeItem
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.hsvToColor
import com.maxrave.simpmusic.extension.parseRichSyncWords
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.unavailable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val TAG = "LyricsView"

// Minimum wipe animation duration. Words shorter than this still wipe over MIN_WIPE_MS so the
// motion stays perceivable; the snap-to-1f on isPast catches up at the actual word end.
private const val MIN_WIPE_MS = 150

// Repeated lyrics palette tokens hoisted to file scope: avoids re-allocating
// the same Color() objects on every recomposition of every line item.
// Apple Music-style: everything is a shade of white — sung lines are full white,
// pending lines are dimmed, translations are dimmer still.
private val DimOriginalColor = Color.White.copy(alpha = 0.42f)
private val DimTranslatedColor = Color.White.copy(alpha = 0.30f)
private val DimRichPendingColor = Color.White.copy(alpha = 0.55f)

// ── Apple Music lyric presentation ──────────────────────────────────────────
// Behaviour/values mirror the two reference renderers credited in the commit
// message (MochaRealm accompanist-lyrics-ui, matthewprince/lyra): lyra's engine
// documents the real player — dim base + brighten-fast/dim-slow line states,
// per-syllable gradient sweep with a soft edge, glow + lift envelope on the
// sung word, stepped depth blur by distance, breathing interlude dots, and the
// active line resting ~40% down the viewport with the scroll arriving ahead of
// the highlight. Everything in this section is gated behind `eyeCandy`
// (performance mode OFF); performance mode keeps the legacy flat rendering.
private const val TIMING_LEAD_MS = 110L // highlights land slightly before nominal time
private const val INTERLUDE_MIN_MS = 2600L // silent gap that earns breathing dots
private const val INTERLUDE_LEAD_MS = 4000L // intro longer than this earns dots
private const val HOLD_NOTE_MS = 800L // a word this long gets the sustained-glow treatment
private const val SWEEP_SOFT_EDGE = 0.18f // gradient edge width of the word sweep
private const val APPLE_INACTIVE_ALPHA = 0.34f // base dim for pending lines
private const val APPLE_STATIC_ALPHA = 0.85f // unsynced sheet
private const val APPLE_IDLE_WORD_SCALE = 0.96f
private const val APPLE_ACTIVE_LINE_SCALE = 1.03f
private const val APPLE_IDLE_LINE_SCALE = 0.93f

private fun smooth01(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Total line opacity by signed distance from the active line (lyra depth tiers). */
private fun appleLineAlpha(distance: Int): Float =
    when {
        distance == 0 -> 1f
        distance < 0 -> when (-distance) {
            // Sung history recedes hard.
            1 -> 0.50f
            2 -> 0.42f
            3 -> 0.36f
            else -> 0.30f
        }
        else -> when (distance) {
            // Read-ahead stays legible.
            1 -> 0.62f
            2 -> 0.54f
            3 -> 0.46f
            else -> 0.40f
        }
    }

/**
 * Stepped blur radius (dp) by signed distance from the active line. Blur steps
 * between tiers — it is never transitioned, because blur transitions run on
 * the main thread (lyra perf notes).
 */
private fun appleBlurRadiusDp(distance: Int): Float =
    when {
        distance == 0 -> 0f
        distance < 0 -> when (-distance) {
            1 -> 1f
            2 -> 1.8f
            3 -> 2.6f
            else -> 3.5f
        }
        else -> when (distance) {
            1 -> 0f
            2 -> 1f
            3 -> 1.6f
            else -> 2.2f
        }
    }

// ── Cascade ripple ──────────────────────────────────────────────────────────
// Lines below the active one follow with a per-line lag (lyra cascadeStep/
// cascadeMax) and a small pull-and-settle nudge, so a line change reads as a
// wave travelling down the list instead of every line flipping at once.
private const val CASCADE_STEP_MS = 45
private const val CASCADE_MAX_MS = 340
private const val CASCADE_PULL_DP = 10f

// Weighty, critically-damped line reveal (accompanist LyricsRevealSpring):
// smooth acceleration and deceleration, no snap, no overshoot on the line itself.
private val AppleRevealSpring = spring<Float>(dampingRatio = 1f, stiffness = 180f)

// ── Breathing interlude dots ────────────────────────────────────────────────
// Lifecycle ported from accompanist's PreparedBreathingDots (which itself ports
// the feature-text-engine draw.rs phases): smooth enter ramp, cosine breathing
// whose period adapts to the gap length, a pre-exit dip-and-rise, a still beat,
// then a shrink-to-zero exit. Pure functions of gap-relative time — no clocks,
// no per-frame allocation.
private const val DOTS_ENTER_MS = 450f
private const val DOTS_DIP_RISE_MS = 650f
private const val DOTS_STILL_MS = 250f
private const val DOTS_EXIT_MS = 400f
private const val DOTS_BREATH_HALF_CYCLE_MS = 1500f
private const val DOTS_VISIBILITY_RAMP_MS = 220f

private fun dotsPhaseFactor(durationMs: Float): Float =
    (durationMs / (DOTS_ENTER_MS + DOTS_DIP_RISE_MS + DOTS_STILL_MS + DOTS_EXIT_MS))
        .coerceAtMost(1f)

private fun dotsEnterEnd(durationMs: Float): Float = DOTS_ENTER_MS * dotsPhaseFactor(durationMs)

private fun dotsExitStart(durationMs: Float): Float =
    durationMs - DOTS_EXIT_MS * dotsPhaseFactor(durationMs)

private fun dotsScale(elapsedMs: Float, durationMs: Float): Float {
    if (durationMs <= 0f) return 0f
    val factor = dotsPhaseFactor(durationMs)
    val enterEnd = DOTS_ENTER_MS * factor
    val exitStart = durationMs - DOTS_EXIT_MS * factor
    val stillStart = exitStart - DOTS_STILL_MS * factor
    val dipStart = stillStart - DOTS_DIP_RISE_MS * factor
    val breathingDuration = (dipStart - enterEnd).coerceAtLeast(0f)
    val hasBreathing = breathingDuration > 16f
    // Odd half-cycle count so the breathe ends where it started.
    val halfCycles =
        (breathingDuration / DOTS_BREATH_HALF_CYCLE_MS).roundToInt().coerceAtLeast(1).let {
            if (it % 2 == 0) it + 1 else it
        }
    val period = 2f * breathingDuration / halfCycles
    return when {
        elapsedMs < enterEnd -> smooth01(elapsedMs / enterEnd) * if (hasBreathing) 0.8f else 1f
        hasBreathing && elapsedMs < dipStart ->
            0.9f - 0.1f * cos((elapsedMs - enterEnd) / period * 2f * PI.toFloat())
        elapsedMs < dipStart -> 1f
        elapsedMs < stillStart ->
            0.8f +
                0.2f *
                    cos(
                        (
                            (elapsedMs - dipStart) /
                                (stillStart - dipStart).coerceAtLeast(0.000001f)
                        ).coerceIn(0f, 1f) * 2f * PI.toFloat(),
                    )
        elapsedMs < exitStart -> 1f
        // Exit: shrink away (reads as growing-then-suddenly-smaller on the way out).
        else -> smooth01((durationMs - elapsedMs) / (durationMs - exitStart).coerceAtLeast(0.000001f))
    }
}

private fun dotsAlpha(elapsedMs: Float, durationMs: Float): Float {
    if (durationMs <= 0f) return 0f
    val enterEnd = dotsEnterEnd(durationMs)
    val exitStart = dotsExitStart(durationMs)
    return when {
        elapsedMs < enterEnd -> smooth01(elapsedMs / enterEnd)
        elapsedMs < exitStart -> 1f
        else -> smooth01((durationMs - elapsedMs) / (durationMs - exitStart).coerceAtLeast(0.000001f))
    }
}

private fun dotsDotAlpha(index: Int, elapsedMs: Float, durationMs: Float): Float {
    val dotSpan = (dotsExitStart(durationMs) - dotsEnterEnd(durationMs)).coerceAtLeast(1f) / 3f
    return 0.4f + 0.6f * ((elapsedMs - dotsEnterEnd(durationMs) - dotSpan * index) / dotSpan).coerceIn(0f, 1f)
}

private fun dotsVisibility(elapsedMs: Float, durationMs: Float): Float {
    if (durationMs <= 0f || elapsedMs < 0f || elapsedMs >= durationMs) return 0f
    return minOf(
        smooth01(elapsedMs / DOTS_VISIBILITY_RAMP_MS),
        smooth01((durationMs - elapsedMs) / DOTS_VISIBILITY_RAMP_MS),
    )
}

/**
 * Seamless viewport edge fade (accompanist LyricsEdgeFade): a DstIn gradient
 * mask inside an offscreen layer, so the *text itself* dissolves into whatever
 * is behind the list (the artwork backdrop) instead of painting darkness over it.
 * Brushes are built once in the cache block — nothing per frame.
 */
private fun Modifier.lyricsListEdgeFade(topLength: Dp, bottomLength: Dp): Modifier =
    drawWithCache {
        val topPx = topLength.toPx().coerceIn(0f, size.height)
        val bottomPx = bottomLength.toPx().coerceIn(0f, size.height)
        val topBrush =
            if (topPx > 0f) {
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = topPx,
                )
            } else {
                null
            }
        val bottomBrush =
            if (bottomPx > 0f) {
                Brush.verticalGradient(
                    listOf(Color.Black, Color.Transparent),
                    startY = size.height - bottomPx,
                    endY = size.height,
                )
            } else {
                null
            }
        onDrawWithContent {
            drawContent()
            topBrush?.let { drawRect(brush = it, blendMode = BlendMode.DstIn) }
            bottomBrush?.let { drawRect(brush = it, blendMode = BlendMode.DstIn) }
        }
    }

private data class TimedLineIndex(
    val index: Int,
    val startTimeMs: Long,
)

/**
 * Returns the original line index of the last [TimedLineIndex] whose [TimedLineIndex.startTimeMs]
 * is `<= nowMs`. Assumes the receiver is sorted ascending by [TimedLineIndex.startTimeMs].
 *
 * Rules:
 *  - empty list -> -1
 *  - nowMs strictly before the first start time -> -1
 *  - nowMs after the last start time -> the last entry's original index (sticky last line)
 */
private fun List<TimedLineIndex>.activeIndexAt(nowMs: Long): Int {
    if (isEmpty()) return -1
    if (nowMs < first().startTimeMs) return -1
    // Binary search for the last item whose startTimeMs <= nowMs.
    var lo = 0
    var hi = size - 1
    var ans = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (this[mid].startTimeMs <= nowMs) {
            ans = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return if (ans >= 0) this[ans].index else -1
}

/**
 * Builds a [Map] from each ORIGINAL line index to its closest synced translated `words`
 * within [thresholdMs]. Two-pointer over both sorted lists; on ties an earlier translated
 * line (smaller startTimeMs in the sorted list) wins for determinism.
 *
 * Lines with invalid `startTimeMs` on either side are skipped.
 */
private fun buildSyncedTranslatedWordsByLineIndex(
    originalLines: List<com.maxrave.domain.data.model.metadata.Line>,
    translatedLines: List<com.maxrave.domain.data.model.metadata.Line>,
    thresholdMs: Long = 1000L,
): Map<Int, String> {
    if (originalLines.isEmpty() || translatedLines.isEmpty()) return emptyMap()

    // Sort translated entries by start time. We keep the original list order as a
    // tie-breaker via stable sort: the FIRST translated line in the SORTED list wins
    // when the time delta is equal.
    val sortedTranslated =
        translatedLines
            .mapNotNull { line ->
                val ts = line.startTimeMs.toLongOrNull() ?: return@mapNotNull null
                ts to line.words
            }.sortedBy { it.first }

    if (sortedTranslated.isEmpty()) return emptyMap()

    // Original lines paired with their parsed timestamp + original index, sorted by time.
    data class OriginalEntry(val index: Int, val ts: Long)

    val sortedOriginal =
        originalLines
            .mapIndexedNotNull { index, line ->
                val ts = line.startTimeMs.toLongOrNull() ?: return@mapIndexedNotNull null
                OriginalEntry(index, ts)
            }.sortedBy { it.ts }

    if (sortedOriginal.isEmpty()) return emptyMap()

    val result = HashMap<Int, String>(sortedOriginal.size)
    var j = 0
    for (orig in sortedOriginal) {
        // Advance j so that sortedTranslated[j] is the first translated entry with ts >= orig.ts,
        // or the last entry if everything is smaller.
        while (j + 1 < sortedTranslated.size && sortedTranslated[j + 1].first <= orig.ts) {
            j++
        }
        // Candidate window: j and j+1 (the next one), pick whichever is closer.
        val candA = sortedTranslated[j]
        val diffA = abs(candA.first - orig.ts)
        var bestTs = candA.first
        var bestWords = candA.second
        var bestDiff = diffA
        if (j + 1 < sortedTranslated.size) {
            val candB = sortedTranslated[j + 1]
            val diffB = abs(candB.first - orig.ts)
            // Tie-break: prefer earlier (smaller startTimeMs) translated line.
            if (diffB < bestDiff) {
                bestTs = candB.first
                bestWords = candB.second
                bestDiff = diffB
            }
        }
        if (bestDiff < thresholdMs) {
            result[orig.index] = bestWords
            // Suppress unused warning while keeping the chosen ts visible for future tweaks.
            @Suppress("UNUSED_VARIABLE")
            val _bt = bestTs
        }
    }
    return result
}

@Composable
fun LyricsView(
    lyricsData: NowPlayingScreenData.LyricsData,
    timeLine: StateFlow<TimeLine>,
    onLineClick: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showScrollShadows: Boolean = false,
    backgroundColor: Color = Color(0xFF242424),
) {
    val listState = rememberLazyListState()
    val current by timeLine.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    // Performance mode OFF = full Apple Music eye-candy (depth tiers, glow, word
    // lift, stepped blur, breathing dots). Performance mode keeps the legacy flat look.
    val eyeCandy = !LocalPerformanceMode.current

    val lines = lyricsData.lyrics.lines.orEmpty()
    val isSynced =
        lyricsData.lyrics.syncType == "LINE_SYNCED" ||
            lyricsData.lyrics.syncType == "RICH_SYNCED"
    val startMsList = remember(lines) { lines.map { it.startTimeMs.toLongOrNull() } }
    val endMsList = remember(lines) {
        lines.mapIndexed { index, line ->
            line.endTimeMs.toLongOrNull() ?: startMsList[index]
        }
    }

    val timedLineIndexes =
        remember(lines) {
            lines
                .mapIndexedNotNull { index, line ->
                    line.startTimeMs.toLongOrNull()?.let { TimedLineIndex(index, it) }
                }.sortedBy { it.startTimeMs }
        }

    val currentLineIndex by remember(timedLineIndexes) {
        derivedStateOf {
            val now = current.current
            // Perceptual lead: the highlight arrives just before the nominal time.
            if (now <= 0L) -1 else timedLineIndexes.activeIndexAt(now + TIMING_LEAD_MS)
        }
    }

    val syncedTranslatedWordsByLineIndex =
        remember(
            lyricsData.lyrics.lines,
            lyricsData.translatedLyrics?.first?.lines,
        ) {
            buildSyncedTranslatedWordsByLineIndex(
                originalLines = lyricsData.lyrics.lines.orEmpty(),
                translatedLines = lyricsData.translatedLyrics?.first?.lines.orEmpty(),
                thresholdMs = 1000L,
            )
        }
    LaunchedEffect(currentLineIndex, lyricsData.lyrics.syncType) {
        if (currentLineIndex > -1 &&
            (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED")
        ) {
            // Apple rests the active line ~40% down the viewport (not dead centre)
            // so there is always read-ahead below it.
            listState.animateScrollAndCentralizeItem(currentLineIndex, bias = 0.40f)
        }
    }

    // Scroll direction for the cascade pull: +1 advancing, -1 going back, 0 on load.
    val scrollDirection = remember { mutableIntStateOf(0) }
    val prevActiveIndex = remember { mutableIntStateOf(Int.MIN_VALUE) }
    LaunchedEffect(currentLineIndex) {
        val prev = prevActiveIndex.intValue
        scrollDirection.intValue =
            if (prev == Int.MIN_VALUE) 0 else (currentLineIndex - prev).coerceIn(-1, 1)
        prevActiveIndex.intValue = currentLineIndex
    }

    BoxWithConstraints(modifier = modifier) {
        // Asymmetric padding puts the resting active line at the 40% mark: the
        // first and last lines can still reach it instead of pinning to the edges.
        val topPadding = (maxHeight * 0.40f - 28.dp).coerceAtLeast(24.dp)
        val bottomPadding = (maxHeight * 0.60f - 28.dp).coerceAtLeast(24.dp)
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        // Seamless DstIn edge fade (eye-candy only; perf stays light).
                        if (eyeCandy && showScrollShadows) {
                            Modifier
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }.lyricsListEdgeFade(72.dp, 110.dp)
                        } else {
                            Modifier
                        },
                    ),
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
        ) {
            items(lines.size) { index ->
                val line = lines.getOrNull(index)
                val isCurrent = index == currentLineIndex
                // ── Cascade: lines below the active one retarget with a per-line
                // lag so a change ripples down the list instead of flipping at
                // once. Word timing below stays instant (locked to the audio) —
                // only the container visuals ripple.
                var cascadeActive by remember { mutableStateOf(isCurrent) }
                var cascadeDistance by remember {
                    mutableIntStateOf(if (currentLineIndex < 0) Int.MAX_VALUE else index - currentLineIndex)
                }
                LaunchedEffect(isCurrent, currentLineIndex, lines) {
                    val d = index - currentLineIndex
                    val stagger =
                        if (eyeCandy && isSynced && currentLineIndex >= 0 && d > 0) {
                            minOf(d * CASCADE_STEP_MS, CASCADE_MAX_MS).toLong()
                        } else {
                            0L
                        }
                    if (stagger > 0L) delay(stagger)
                    cascadeActive = isCurrent
                    cascadeDistance = if (currentLineIndex < 0) Int.MAX_VALUE else d
                }
                // ── Pull-and-settle: lagging lines take a small nudge along the
                // scroll direction, then spring home — the "top pulls the rest" feel.
                val settle = remember { Animatable(0f) }
                val pullPx = with(density) { CASCADE_PULL_DP.dp.toPx() }
                LaunchedEffect(currentLineIndex, lines) {
                    if (!eyeCandy || !isSynced) {
                        settle.snapTo(0f)
                    } else {
                        val d = index - currentLineIndex
                        if (currentLineIndex < 0 || d <= 0 || d > 8) {
                            settle.snapTo(0f)
                        } else {
                            delay(minOf(d * CASCADE_STEP_MS, CASCADE_MAX_MS).toLong())
                            val dir = scrollDirection.intValue
                            if (dir == 0) {
                                settle.snapTo(0f)
                            } else {
                                settle.snapTo(dir * pullPx)
                                settle.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.7f, stiffness = 300f),
                                )
                            }
                        }
                    }
                }
                val targetLineAlpha =
                    when {
                        !eyeCandy -> 1f
                        !isSynced -> APPLE_STATIC_ALPHA
                        currentLineIndex < 0 -> APPLE_INACTIVE_ALPHA
                        cascadeActive -> 1f
                        else -> appleLineAlpha(cascadeDistance)
                    }
                // Brighten fast, dim slow (lyra line transitions).
                // States are read inside graphicsLayer (draw phase), so the
                // running animations re-draw without recomposing the item.
                val lineAlphaState =
                    animateFloatAsState(
                        targetValue = targetLineAlpha,
                        animationSpec = tween(if (cascadeActive) 280 else 600, easing = FastOutSlowInEasing),
                        label = "appleLineAlpha",
                    )
                val lineScaleState =
                    animateFloatAsState(
                        targetValue =
                            if (!eyeCandy || !isSynced) {
                                1f
                            } else if (cascadeActive) {
                                APPLE_ACTIVE_LINE_SCALE
                            } else {
                                APPLE_IDLE_LINE_SCALE
                            },
                        animationSpec = AppleRevealSpring,
                        label = "appleLineScale",
                    )
                val blurRadiusDp =
                    if (!eyeCandy || !isSynced || currentLineIndex < 0 || cascadeActive) {
                        0f
                    } else {
                        appleBlurRadiusDp(cascadeDistance)
                    }
                val blurPx = with(density) { blurRadiusDp.dp.toPx() }
                val blurEffect = remember(blurPx) {
                    if (blurPx > 0.5f) BlurEffect(blurPx, blurPx) else null
                }
                // In eye-candy mode the line container carries the dimming so the
                // glyphs stay solid; the inner texts switch to solid whites below.
                val solidForTier = eyeCandy && !cascadeActive
                val lineDeco =
                    if (!eyeCandy) {
                        Modifier
                    } else {
                        Modifier.graphicsLayer {
                            alpha = lineAlphaState.value
                            scaleX = lineScaleState.value
                            scaleY = lineScaleState.value
                            translationY = settle.value
                            renderEffect = blurEffect
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                    }

                // Interlude dots, shown only for genuine silence: the provider
                // must have supplied a real end time (end > start). Missing or
                // zeroed ends mean "unknown timing", not a gap.
                val nowMs = current.current
                val thisStart = startMsList.getOrNull(index)
                val thisEnd = endMsList.getOrNull(index)
                // Lead-in above the first line.
                val leadGap =
                    if (index == 0 && isSynced && thisStart != null &&
                        thisStart >= INTERLUDE_LEAD_MS && nowMs in 0..thisStart
                    ) {
                        0L to thisStart
                    } else {
                        null
                    }
                val nextStart = startMsList.getOrNull(index + 1)
                // Silence below this line: real end, real next start, long
                // enough, and nothing is sung right now.
                val midGap =
                    if (isSynced && thisStart != null && thisEnd != null &&
                        thisEnd > thisStart && nextStart != null &&
                        nextStart - thisEnd >= INTERLUDE_MIN_MS &&
                        nowMs in thisEnd..nextStart
                    ) {
                        thisEnd to nextStart
                    } else {
                        null
                    }
                // Outro: dots through a long instrumental tail.
                val total = current.total
                val outroGap =
                    if (isSynced && index == lines.lastIndex && thisStart != null &&
                        thisEnd != null && thisEnd > thisStart &&
                        total > 0L && total - thisEnd >= INTERLUDE_MIN_MS + 1500L &&
                        nowMs in thisEnd..total
                    ) {
                        thisEnd to total
                    } else {
                        null
                    }
                val gap = midGap ?: outroGap

                Column(modifier = lineDeco) {
                    leadGap?.let { (gapStart, gapEnd) ->
                        InterludeDots(
                            elapsedMs = (nowMs - gapStart).toFloat(),
                            durationMs = (gapEnd - gapStart).toFloat(),
                        )
                    }
                    // Translated lyrics: synced -> precomputed map by line index, unsynced -> by index.
                    val translatedWords =
                        if (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED") {
                            syncedTranslatedWordsByLineIndex[index]
                        } else {
                            lyricsData.translatedLyrics
                                ?.first
                                ?.lines
                                ?.getOrNull(index)
                                ?.words
                        }

                    line?.words?.let { words ->
                        when {
                            // Rich sync: parse and use RichSyncLyricsLineItem
                            lyricsData.lyrics.syncType == "RICH_SYNCED" -> {
                                val parsedLine =
                                    remember(words, line.startTimeMs, line.endTimeMs) {
                                        val result = parseRichSyncWords(words, line.startTimeMs, line.endTimeMs)
                                        result
                                    }

                                if (parsedLine != null) {
                                    RichSyncLyricsLineItem(
                                        parsedLine = parsedLine,
                                        translatedWords = translatedWords,
                                        currentTimeMs = current.current,
                                        isCurrent = isCurrent,
                                        solidForTier = solidForTier,
                                        eyeCandy = eyeCandy,
                                        modifier =
                                            Modifier
                                                .clickable {
                                                    onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                                },
                                    )
                                } else {
                                    // Fallback to regular line item if parsing fails
                                    LyricsLineItem(
                                        originalWords = words,
                                        translatedWords = translatedWords,
                                        isBold = isCurrent,
                                        isCurrent = isCurrent,
                                        solidForTier = solidForTier,
                                        eyeCandy = eyeCandy,
                                        modifier =
                                            Modifier
                                                .clickable {
                                                    onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                                },
                                    )
                                }
                            }

                            // Line sync or unsynced: use existing LyricsLineItem
                            else -> {
                                LyricsLineItem(
                                    originalWords = words,
                                    translatedWords = translatedWords,
                                    isBold = isCurrent || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                    isCurrent = isCurrent || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                    solidForTier = solidForTier,
                                    eyeCandy = eyeCandy,
                                    modifier =
                                        Modifier
                                            .clickable(enabled = lyricsData.lyrics.syncType == "LINE_SYNCED") {
                                                onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                            },
                                )
                            }
                        }
                    }
                    gap?.let { (gapStart, gapEnd) ->
                        InterludeDots(
                            elapsedMs = (nowMs - gapStart).toFloat(),
                            durationMs = (gapEnd - gapStart).toFloat(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsLineItem(
    originalWords: String,
    translatedWords: String?,
    isBold: Boolean,
    isCurrent: Boolean = false,
    solidForTier: Boolean = false,
    eyeCandy: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Apple type: tight tracking, generous leading. When the container carries
    // the dimming (solidForTier) the glyphs stay near-solid white.
    val originalColor =
        if (solidForTier) {
            Color.White.copy(alpha = 0.92f)
        } else if (isCurrent) {
            Color.White
        } else {
            DimOriginalColor
        }
    val translatedColor =
        if (solidForTier) {
            Color.White.copy(alpha = 0.65f)
        } else if (isCurrent) {
            Color.White.copy(alpha = 0.55f)
        } else {
            DimTranslatedColor
        }
    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = originalWords,
            fontFamily = lyricsFontFamily(),
            fontSize = 27.sp,
            lineHeight = 31.sp,
            letterSpacing = (-0.25).sp,
            fontWeight = if (isBold || eyeCandy) FontWeight.Bold else FontWeight.Normal,
            color = originalColor,
        )
        if (translatedWords != null) {
            Text(
                text = translatedWords,
                fontFamily = lyricsFontFamily(),
                fontSize = 14.sp,
                color = translatedColor,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichSyncLyricsLineItem(
    parsedLine: ParsedRichSyncLine,
    translatedWords: String?,
    currentTimeMs: Long,
    isCurrent: Boolean,
    customFontSize: TextUnit? = null,
    customPadding: Dp = 12.dp,
    solidForTier: Boolean = false,
    eyeCandy: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val currentWordIndex by remember(currentTimeMs, parsedLine.words) {
        derivedStateOf {
            if (!isCurrent) return@derivedStateOf -1
            parsedLine.words.indexOfLast { it.startTimeMs <= currentTimeMs }
        }
    }
    val fontSize = customFontSize ?: 27.sp
    val wordStyle =
        TextStyle(
            fontFamily = lyricsFontFamily(),
            fontSize = fontSize,
            lineHeight = (fontSize.value * 1.16f).sp,
            letterSpacing = (-0.25).sp,
            fontWeight = FontWeight.Bold,
        )

    // Performance mode: snap-static words, zero animation objects — the light path.
    if (!eyeCandy) {
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(customPadding))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                parsedLine.words.forEachIndexed { index, wordTiming ->
                    Text(
                        text = wordTiming.text,
                        style = wordStyle,
                        color =
                            if (isCurrent && index <= currentWordIndex) {
                                Color.White
                            } else {
                                DimOriginalColor
                            },
                    )
                }
            }
            if (translatedWords != null) {
                Text(
                    text = translatedWords,
                    style = typo().bodyMedium.copy(fontSize = 14.sp),
                    color =
                        if (isCurrent) {
                            Color.White.copy(alpha = 0.5f)
                        } else {
                            DimTranslatedColor
                        },
                )
            }
            Spacer(modifier = Modifier.height(customPadding))
        }
        return
    }

    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(customPadding))

        // Original lyrics with rich sync highlighting - using FlowRow for word wrapping
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.Center,
        ) {            parsedLine.words.forEachIndexed { index, wordTiming ->
                // Calculate word end time (start time of next word or line end time)
                // If last word and lineEndTimeMs is invalid (Long.MAX_VALUE), estimate based on previous word duration
                val wordEndTimeMs =
                    if (index < parsedLine.words.size - 1) {
                        parsedLine.words[index + 1].startTimeMs
                    } else if (parsedLine.lineEndTimeMs == Long.MAX_VALUE || parsedLine.lineEndTimeMs <= wordTiming.startTimeMs) {
                        // Estimate: use previous word duration or default 500ms
                        if (index > 0 && parsedLine.words[index - 1].startTimeMs < wordTiming.startTimeMs) {
                            val prevWordDuration = wordTiming.startTimeMs - parsedLine.words[index - 1].startTimeMs
                            wordTiming.startTimeMs + prevWordDuration
                        } else {
                            wordTiming.startTimeMs + 500L // Default 500ms if no reference
                        }
                    } else {
                        parsedLine.lineEndTimeMs
                    }
                AnimatedWord(
                    word = wordTiming.text,
                    wordIndex = index,
                    wordStartTimeMs = wordTiming.startTimeMs,
                    wordEndTimeMs = wordEndTimeMs,
                    currentTimeMs = currentTimeMs,
                    isActive = isCurrent && index == currentWordIndex,
                    isPast = isCurrent && index < currentWordIndex,
                    isCurrent = isCurrent,
                    customFontSize = customFontSize,
                    solidForTier = solidForTier,
                    eyeCandy = eyeCandy,
                )
            }
        }

        // Translated lyrics (line-level, no word sync)
        if (translatedWords != null) {
            Text(
                text = translatedWords,
                style = typo().bodyMedium.copy(fontSize = 14.sp),
                color =
                    if (solidForTier) {
                        Color.White.copy(alpha = 0.65f)
                    } else if (isCurrent) {
                        Color.White.copy(alpha = 0.5f)
                    } else {
                        DimTranslatedColor
                    },
            )
        }

        Spacer(modifier = Modifier.height(customPadding))
    }
}

@Composable
private fun AnimatedWord(
    word: String,
    wordIndex: Int,
    wordStartTimeMs: Long,
    wordEndTimeMs: Long,
    currentTimeMs: Long,
    isActive: Boolean,
    isPast: Boolean,
    isCurrent: Boolean,
    customFontSize: TextUnit? = null,
    solidForTier: Boolean = false,
    eyeCandy: Boolean = false,
) {
    val fontSize = customFontSize ?: 27.sp
    val style =
        TextStyle(
            fontFamily = lyricsFontFamily(),
            fontSize = fontSize,
            lineHeight = (fontSize.value * 1.16f).sp,
            letterSpacing = (-0.25).sp,
            fontWeight = FontWeight.Bold,
        )

    if (!isCurrent) {
        Text(
            text = word,
            style = style,
            color = if (solidForTier) Color.White.copy(alpha = 0.92f) else DimOriginalColor,
        )
        return
    }

    // Wall-clock wipe driven by an Animatable.
    // - Future word (not active, not past): progress stays at 0.
    // - Active word: snap to current % then animateTo(1f) over the remaining duration of the
    //   word, in real wall-clock time. Independent of timeline emit rate, so wipe is smooth.
    // - Past word: snap to 1f.
    val wordDurationMs = (wordEndTimeMs - wordStartTimeMs).coerceAtLeast(100L)
    val anim =
        remember(wordStartTimeMs, wordEndTimeMs) {
            val initial =
                ((currentTimeMs - wordStartTimeMs).toFloat() / wordDurationMs.toFloat())
                    .coerceIn(0f, 1f)
            androidx.compose.animation.core.Animatable(initial)
        }

    LaunchedEffect(wordStartTimeMs, wordEndTimeMs, isActive, isPast) {
        when {
            isPast -> anim.snapTo(1f)
            isActive -> {
                val now = currentTimeMs
                val current =
                    ((now - wordStartTimeMs).toFloat() / wordDurationMs.toFloat())
                        .coerceIn(0f, 1f)
                anim.snapTo(current)
                // Ensure minimum visible wipe duration so very short words don't flash.
                // Tradeoff: visual may finish ~MIN_WIPE_MS after the actual word end, but the
                // next isPast=true transition will snap to 1f so it stays consistent.
                val remainingMs =
                    (wordEndTimeMs - now).coerceAtLeast(0L).toInt().coerceAtLeast(MIN_WIPE_MS)
                anim.animateTo(1f, tween(remainingMs, easing = LinearEasing))
            }
            // Future word (not active, not past): keep current value.
            // Don't snap to 0 — playback position can jitter backwards by a few ms,
            // briefly flipping isActive false. Snapping would jerk the wipe back.
        }
    }

    // The Animatable is owned here, but its value is read ONLY inside the tiny
    // overlay below — so the ticking word never recomposes its static neighbours
    // (accompanist isolates ticking rows in separate draw scopes for the same reason).
    Box {
        // Bottom layer: dimmed pending color, drawn once, never animated.
        Text(text = word, style = style, color = DimRichPendingColor)
        if (eyeCandy) {
            WordSweepOverlay(
                word = word,
                style = style,
                anim = anim,
                wordDurationMs = wordDurationMs,
            )
        } else {
            LegacyWipeOverlay(
                word = word,
                style = style,
                anim = anim,
            )
        }
    }
}

/**
 * Apple word sweep, engineered for zero per-frame allocation (the stutter fix):
 *
 * - The sweep band uses ONE remembered fixed gradient brush; only the clip
 *   window moves. (Accompanist: "a canvas translation reuses the fixed-width
 *   shader on both Android and Skia".)
 * - The glow is a static-shadow text whose *layer alpha* follows the lift
 *   envelope — the glyphs rasterize once and are cached; only opacity moves.
 * - Word motion (grow + lift) lives in graphicsLayer: transform-only, free.
 *
 * Per frame the engine redraws at most two small clipped text runs and moves
 * one rect/alpha — no object creation, no shader recompiles, no blur passes.
 */
@Composable
private fun WordSweepOverlay(
    word: String,
    style: TextStyle,
    anim: Animatable<Float, AnimationVector1D>,
    wordDurationMs: Long,
) {
    val p = anim.value.coerceIn(0f, 1f)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val bandBrush = remember { Brush.horizontalGradient(0f to Color.White, 1f to Color.Transparent) }
    val isHeld = wordDurationMs >= HOLD_NOTE_MS
    val bandStyle = remember(style, bandBrush) { style.copy(brush = bandBrush) }
    // Glow paints are built once (accompanist RowPaints: "playback selects a
    // shadow without allocating one"); the envelope below only picks opacity.
    val glowStyle = remember(style, wordDurationMs) {
        style.copy(
            shadow =
                Shadow(
                    color = Color.White.copy(alpha = if (isHeld) 0.65f else 0.5f),
                    blurRadius = if (isHeld) 14f else 10f,
                ),
        )
    }

    val grow = smooth01(p)
    // Lift amplitude from word duration (lyra pop): short words barely move.
    val pop = ((wordDurationMs - 120).toFloat() / 480f).coerceIn(0.25f, 1f)
    val elapsedMs = p * wordDurationMs
    val attackWindowMs = minOf(120f, wordDurationMs * 0.35f).coerceAtLeast(1f)
    val atk = smooth01(elapsedMs / attackWindowMs)
    // The arc: rise to a peak ~65% through the word, settle toward its end.
    val arc =
        atk * (
            if (p < 0.65f) {
                smooth01(p / 0.65f)
            } else {
                1f - 0.45f * smooth01((p - 0.65f) / 0.35f)
            }
        )
    val wordScale =
        APPLE_IDLE_WORD_SCALE + (1f - APPLE_IDLE_WORD_SCALE) * grow + 0.032f * pop * arc
    // Lift starts ~4px low and settles quadratically as the word is sung
    // (accompanist drawPreparedRow lift curve).
    val liftPx = 4f * (1f - grow) * (1f - grow)
    val glowAlpha = (0.9f * arc).coerceIn(0f, 1f)

    // The sweep overdrives past 100% so the soft edge fully clears the glyph
    // right as the word flips to sung (lyra --fill 118%).
    val fill = (p * (1f + SWEEP_SOFT_EDGE)).coerceIn(0f, 1f + SWEEP_SOFT_EDGE)
    val solidUpTo = (fill - SWEEP_SOFT_EDGE).coerceIn(0f, 1f)
    val bandLeft = if (!rtl) solidUpTo else (1f - fill).coerceIn(0f, 1f)
    val bandRight = if (!rtl) fill.coerceAtMost(1f) else (1f - solidUpTo).coerceIn(0f, 1f)

    Box(
        modifier =
            Modifier.graphicsLayer {
                scaleX = wordScale
                scaleY = wordScale
                translationY = liftPx
            },
    ) {
        if (solidUpTo > 0.001f) {
            Text(
                text = word,
                style = style,
                color = Color.White,
                modifier =
                    Modifier.drawWithContent {
                        clipRect(right = size.width * solidUpTo) {
                            this@drawWithContent.drawContent()
                        }
                    },
            )
        }
        if (fill > 0.001f && fill < 1f + SWEEP_SOFT_EDGE - 0.001f && bandRight > bandLeft + 0.001f) {
            Text(
                text = word,
                style = bandStyle,
                color = Color.Transparent,
                modifier =
                    Modifier.drawWithContent {
                        clipRect(
                            left = size.width * bandLeft,
                            right = size.width * bandRight,
                        ) {
                            this@drawWithContent.drawContent()
                        }
                    },
            )
        }
        if (glowAlpha > 0.01f) {
            Text(
                text = word,
                style = glowStyle,
                color = Color.White,
                modifier = Modifier.graphicsLayer { alpha = glowAlpha },
            )
        }
    }
}

/**
 * Legacy hard wipe (performance mode): a single clipped white run, allocation-
 * free and animation-cheap.
 */
@Composable
private fun LegacyWipeOverlay(
    word: String,
    style: TextStyle,
    anim: Animatable<Float, AnimationVector1D>,
) {
    val progress = anim.value
    Text(
        text = word,
        style = style,
        color = Color.White,
        modifier =
            Modifier.drawWithContent {
                clipRect(right = size.width * progress) {
                    this@drawWithContent.drawContent()
                }
            },
    )
}

/**
 * Interlude dots for long instrumental gaps, with the full lifecycle ported
 * from accompanist's PreparedBreathingDots: smooth enter ramp, cosine
 * breathing whose period adapts to the gap length, a pre-exit dip-and-rise, a
 * still beat, then a shrink-to-zero exit. Pure function of gap-relative time —
 * no clocks, no hooks, so it is equally at home in performance mode (where it
 * simply renders without the container blur around it).
 */
@Composable
private fun InterludeDots(
    elapsedMs: Float,
    durationMs: Float,
    modifier: Modifier = Modifier,
) {
    val scale = dotsScale(elapsedMs, durationMs)
    // The container already dims non-current lines; keep the dots' own alpha
    // relative so they never outshine the lyrics around them.
    val alpha = (dotsAlpha(elapsedMs, durationMs) * dotsVisibility(elapsedMs, durationMs))
        .coerceIn(0f, 1f)
    if (scale <= 0.01f || alpha <= 0.01f) return
    Row(
        modifier =
            modifier
                .padding(vertical = 10.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { dot ->
            Box(
                modifier =
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = dotsDotAlpha(dot, elapsedMs, durationMs))),
            )
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FullscreenLyricsSheet(
    sharedViewModel: SharedViewModel,
    navController: NavController,
    color: Color = Color(0xFF242424),
    onDismiss: () -> Unit,
) {
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val windowInsets = WindowInsets.systemBars

    var sliderValue by rememberSaveable {
        mutableFloatStateOf(0f)
    }

    // Auto-hide controls state - Only hide control buttons, not title/progress
    var showControlButtons by rememberSaveable {
        mutableStateOf(true)
    }

    var showNowPlayingSheet by rememberSaveable {
        mutableStateOf(false)
    }

    // Animated gradient colors - SMOOTH ANIMATION
    val startColor = remember { Animatable(color) }
    val midColor1 = remember { Animatable(color.copy(alpha = 0.95f)) }
    val midColor2 = remember { Animatable(color.copy(alpha = 0.85f)) }
    val endColor = remember { Animatable(Color.Black) }

    // Dynamic gradient animation - MULTIPLE DIRECTIONS
    // Replaces the previous `while(true) { delay(16) }` loop with a Compose
    // infinite transition.
    val gradientTransition = rememberInfiniteTransition(label = "lyricsGradient")
    val animatedAngle by gradientTransition.animateFloat(
        initialValue = -45f,
        targetValue = 45f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "lyricsGradientAngle",
    )
    val animatedOffsetX by gradientTransition.animateFloat(
        initialValue = -1500f,
        targetValue = 1500f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "lyricsGradientOffsetX",
    )
    val animatedOffsetY by gradientTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "lyricsGradientOffsetY",
    )
    val gradientAngle = animatedAngle
    val gradientOffsetX = animatedOffsetX
    val gradientOffsetY = animatedOffsetY

    // Smooth color animation based on lyrics color
    LaunchedEffect(color) {
        launch {
            startColor.animateTo(
                targetValue = color,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            )
        }
        launch {
            midColor1.animateTo(
                targetValue = color.copy(alpha = 0.95f),
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            )
        }
        launch {
            midColor2.animateTo(
                targetValue = color.copy(alpha = 0.85f),
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            )
        }
        launch {
            endColor.animateTo(
                targetValue = Color.Black,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            )
        }
    }

    // Reset auto-hide timer when controls are shown
    LaunchedEffect(key1 = showControlButtons) {
        if (showControlButtons) {
            delay(4000) // Hide after 4 seconds
            showControlButtons = false
        }
    }

    LaunchedEffect(key1 = timelineState) {
        sliderValue =
            if (timelineState.total > 0L) {
                timelineState.current.toFloat() * 100 / timelineState.total.toFloat()
            } else {
                0f
            }
    }

    if (screenDataState.lyricsData != null) {
        KeepScreenOn()
    }

    var showQueueBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showInfoBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        containerColor = Color.Black,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = .5f),
        sheetState = sheetState,
        modifier =
            Modifier
                .fillMaxHeight()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    // Show controls on tap
                    showControlButtons = true
                },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        // Crossfade: RGB rainbow color cycling when transitioning between tracks
        val infiniteTransition = rememberInfiniteTransition(label = "crossfadeRainbow")
        val rainbowHue by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "rainbowHue",
        )
        val rainbowColor = hsvToColor(rainbowHue, 1f, 1f)
        val sliderTrackColor by animateColorAsState(
            targetValue = if (timelineState.isCrossfading) rainbowColor else Color.White,
            animationSpec = tween(300),
            label = "sliderCrossfadeColor",
        )
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated gradient background
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        startColor.value,
                                        midColor1.value,
                                        midColor2.value,
                                        endColor.value.copy(alpha = 0.9f),
                                        endColor.value,
                                    ),
                                start =
                                    Offset(
                                        x = gradientOffsetX + (cos(gradientAngle * PI.toFloat() / 180f) * 800f),
                                        y = gradientOffsetY + (sin(gradientAngle * PI.toFloat() / 180f) * 800f),
                                    ),
                                end =
                                    Offset(
                                        x = gradientOffsetX + 2500f + (cos((gradientAngle + 180f) * PI.toFloat() / 180f) * 800f),
                                        y = gradientOffsetY + 2500f + (sin((gradientAngle + 180f) * PI.toFloat() / 180f) * 800f),
                                    ),
                            ),
                        ),
            )

            // ── Foreground content column ─────────────────────────────────────
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            bottom =
                                with(localDensity) {
                                    windowInsets.getBottom(localDensity).toDp()
                                },
                            top =
                                with(localDensity) {
                                    windowInsets.getTop(localDensity).toDp()
                                },
                        ),
            ) {
                // New Apple Music Style Header
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Song Poster (Small, Top Left)
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
                                .data(screenDataState.thumbnailURL)
                                .crossfade(300)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(screenDataState.thumbnailURL)
                                .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(45.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        sheetState.hide()
                                        onDismiss()
                                    }
                                },
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Song Info Column
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        // Song Name
                        Text(
                            text = screenDataState.nowPlayingTitle,
                            style = typo().labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Artist Name with Explicit Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.clickable {
                                    coroutineScope.launch {
                                        val song = sharedViewModel.nowPlayingState.value?.songEntity
                                        (
                                            song?.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() }
                                                ?: screenDataState.songInfoData?.authorId
                                        )?.let { channelId ->
                                            sheetState.hide()
                                            onDismiss()
                                            navController.navigate(
                                                ArtistDestination(
                                                    channelId = channelId,
                                                ),
                                            )
                                        }
                                    }
                                },
                        ) {
                            if (screenDataState.isExplicit) {
                                ExplicitBadge(
                                    modifier =
                                        Modifier
                                            .size(16.dp)
                                            .padding(end = 4.dp),
                                )
                            }
                            Text(
                                text = screenDataState.artistName,
                                style = typo().bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Like Button (Heart)
                    HeartCheckBox(
                        checked = controllerState.isLiked,
                        size = 28,
                    ) {
                        sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Three Dot Menu
                    IconButton(
                        onClick = { showNowPlayingSheet = true },
                    ) {
                        Icon(
                            imageVector = SimpIcons.MoreVert,
                            contentDescription = "",
                            tint = Color.White,
                        )
                    }
                }

                // Lyrics Content - Expands when controls are hidden
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 50.dp),
                ) {
                    Crossfade(
                        targetState = screenDataState.lyricsData != null,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (it) {
                            screenDataState.lyricsData?.let { lyrics ->
                                LyricsView(
                                    lyricsData = lyrics,
                                    timeLine = sharedViewModel.timeline,
                                    onLineClick = { f ->
                                        sharedViewModel.onUIEvent(UIEvent.UpdateProgress(f))
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    showScrollShadows = true,
                                    backgroundColor = startColor.value,
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.unavailable),
                                    style = typo().bodyMedium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                // Progress Bar and Time - Always visible
                Column {
                    // Real Slider
                    Box(
                        Modifier
                            .padding(
                                top = 15.dp,
                            ).padding(horizontal = 40.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Crossfade(timelineState.loading) {
                                if (it) {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                        LinearProgressIndicator(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .padding(
                                                        horizontal = 3.dp,
                                                    ).clip(
                                                        RoundedCornerShape(8.dp),
                                                    ),
                                            color = Color.Gray,
                                            trackColor = Color.DarkGray,
                                            strokeCap = StrokeCap.Round,
                                        )
                                    }
                                } else {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                        LinearProgressIndicator(
                                            progress = { timelineState.bufferedPercent.toFloat() / 100 },
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .padding(
                                                        horizontal = 3.dp,
                                                    ).clip(
                                                        RoundedCornerShape(8.dp),
                                                    ),
                                            color = Color.Gray,
                                            trackColor = Color.DarkGray,
                                            strokeCap = StrokeCap.Round,
                                            drawStopIndicator = {},
                                        )
                                    }
                                }
                            }
                        }
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            Slider(
                                // Fraction, not 0..100 — see the note in NowPlayingScreen:
                                // material3 alpha25 drops valueRange on its binary-compatibility
                                // overload.
                                value = sliderValue / 100f,
                                onValueChange = {
                                    sharedViewModel.onUIEvent(
                                        UIEvent.UpdateProgress(it * 100f),
                                    )
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 3.dp)
                                        .align(
                                            Alignment.TopCenter,
                                        ),
                                track = { sliderState ->
                                    SliderDefaults.Track(
                                        modifier =
                                            Modifier
                                                .height(5.dp),
                                        enabled = true,
                                        sliderState = sliderState,
                                        colors =
                                            SliderDefaults.colors().copy(
                                                thumbColor = sliderTrackColor,
                                                activeTrackColor = sliderTrackColor,
                                                inactiveTrackColor = Color.Transparent,
                                            ),
                                        thumbTrackGapSize = 0.dp,
                                        drawTick = { _, _ -> },
                                        drawStopIndicator = null,
                                    )
                                },
                                thumb = {
                                    SliderDefaults.Thumb(
                                        modifier =
                                            Modifier
                                                .height(18.dp)
                                                .width(8.dp)
                                                .padding(
                                                    vertical = 4.dp,
                                                ),
                                        thumbSize = DpSize(8.dp, 8.dp),
                                        interactionSource =
                                            remember {
                                                MutableInteractionSource()
                                            },
                                        colors =
                                            SliderDefaults.colors().copy(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color.White,
                                                inactiveTrackColor = Color.Transparent,
                                            ),
                                        enabled = true,
                                    )
                                },
                            )
                        }
                    }
                    LazyColumn {
                        item {
                            // Time Layout
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 40.dp),
                            ) {
                                Text(
                                    text = formatDuration(timelineState.current),
                                    style = typo().bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Left,
                                )
                                AnimatedVisibility(
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    visible = timelineState.isCrossfading,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.crossfading),
                                        style = typo().bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Text(
                                    text = formatDuration(timelineState.total),
                                    style = typo().bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Right,
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(5.dp),
                            )
                        }

                        item {
                            // Control Buttons - Animated visibility
                            AnimatedVisibility(
                                visible = showControlButtons,
                                enter =
                                    expandVertically(
                                        tween(300),
                                    ),
                                exit =
                                    shrinkVertically(
                                        tween(300),
                                    ),
                            ) {
                                PlayerControlLayout(controllerState) {
                                    sharedViewModel.onUIEvent(it)
                                }
                            }
                            AnimatedVisibility(
                                visible = showControlButtons,
                                enter =
                                    expandVertically(
                                        tween(300),
                                    ),
                                exit =
                                    shrinkVertically(
                                        tween(300),
                                    ),
                            ) {
                                // List Bottom Buttons
                                Box(
                                    modifier =
                                        Modifier
                                            .height(32.dp)
                                            .fillMaxWidth()
                                            .padding(horizontal = 40.dp),
                                ) {
                                    IconButton(
                                        modifier =
                                            Modifier
                                                .size(24.dp)
                                                .aspectRatio(1f)
                                                .align(Alignment.CenterStart)
                                                .clip(
                                                    CircleShape,
                                                ),
                                        onClick = {
                                            showInfoBottomSheet = true
                                            showControlButtons = true
                                        },
                                    ) {
                                        Icon(imageVector = SimpIcons.Info, tint = Color.White, contentDescription = "")
                                    }
                                    Row(
                                        Modifier.align(Alignment.CenterEnd),
                                    ) {
                                        Spacer(modifier = Modifier.size(8.dp))
                                        IconButton(
                                            modifier =
                                                Modifier
                                                    .size(24.dp)
                                                    .aspectRatio(1f)
                                                    .clip(
                                                        CircleShape,
                                                    ),
                                            onClick = {
                                                showQueueBottomSheet = true
                                                showControlButtons = true
                                            },
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.QueueMusic,
                                                tint = Color.White,
                                                contentDescription = "",
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }

                // When control buttons are hidden, add spacer to maintain proper spacing
                if (!showControlButtons) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
    if (showQueueBottomSheet) {
        QueueBottomSheet(
            onDismiss = {
                showQueueBottomSheet = false
            },
        )
    }
    if (showInfoBottomSheet) {
        InfoPlayerBottomSheet(
            onDismiss = {
                showInfoBottomSheet = false
            },
        )
    }
    if (showNowPlayingSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showNowPlayingSheet = false
            },
            navController = navController,
            onNavigateToOtherScreen = {
                onDismiss()
            },
            song = null,
            setSleepTimerEnable = true,
            changeMainLyricsProviderEnable = true,
        )
    }
}