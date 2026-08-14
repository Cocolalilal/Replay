package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive

/**
 * Performance-fatigue guard for the animated Apple Music background.
 *
 * Returns a state that is `true` while the animation should be paused:
 * - the sliding window of frame times (last ~3s) averages above ~22ms (under ~45fps), or
 * - the platform signal fires (Android: power-save mode or moderate+ thermal; Desktop: never).
 *
 * The monitoring itself is free — it only compares timestamps of frames that are already being
 * rendered — and the animation resumes by itself once the device cools down / exits power-save
 * mode / frames speed back up.
 */
@Composable
fun rememberIsAnimationFatigued(): State<Boolean> {
    val platformFatigued = rememberPlatformFatigueSignal()
    val frameFatigued = rememberFrameTimingFatigue()
    return remember(platformFatigued, frameFatigued) {
        derivedStateOf { platformFatigued.value || frameFatigued.value }
    }
}

/**
 * Platform-only fatigue signals (battery saver / thermal on Android). Desktop has no such
 * signals and reports false.
 */
@Composable
expect fun rememberPlatformFatigueSignal(): State<Boolean>

/**
 * Sliding ~3s window of frame deltas. Fatigued when the average delta exceeds
 * [FATIGUE_THRESHOLD_NANOS] (~22ms, i.e. under ~45fps); recovers once it drops under
 * [RECOVER_THRESHOLD_NANOS] (~17ms), so the flag never flickers around the boundary.
 */
private const val FRAME_WINDOW_NANOS = 3_000_000_000L
private const val MIN_WINDOW_FRAMES = 30L
private const val FATIGUE_THRESHOLD_NANOS = 22_000_000L
private const val RECOVER_THRESHOLD_NANOS = 17_000_000L

@Composable
private fun rememberFrameTimingFatigue(): State<Boolean> {
    val fatigue = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Each entry is the frame time at which a delta ENDED, paired with the delta itself.
        val deltas = ArrayDeque<Pair<Long, Long>>()
        var lastFrameNanos = 0L
        var sumNanos = 0L
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            if (lastFrameNanos != 0L) {
                deltas.addLast(frameNanos to (frameNanos - lastFrameNanos))
                sumNanos += frameNanos - lastFrameNanos
                while (deltas.isNotEmpty() && frameNanos - deltas.first().first > FRAME_WINDOW_NANOS) {
                    sumNanos -= deltas.removeFirst().second
                }
                if (deltas.size >= MIN_WINDOW_FRAMES) {
                    val averageNanos = sumNanos / deltas.size
                    if (fatigue.value) {
                        if (averageNanos < RECOVER_THRESHOLD_NANOS) {
                            fatigue.value = false
                        }
                    } else if (averageNanos > FATIGUE_THRESHOLD_NANOS) {
                        fatigue.value = true
                    }
                }
            }
            lastFrameNanos = frameNanos
        }
    }
    return fatigue
}
