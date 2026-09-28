package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float = 0.001f,
    val initialScale: Float = 1.0f,
    val pressedScale: Float = 1.39f,
    val onDragStarted: (DampedDragAnimation, Offset) -> Unit = { _, _ -> },
    val onDragStopped: (DampedDragAnimation) -> Unit = { _ -> },
    val onDrag: (DampedDragAnimation, IntSize, Offset) -> Unit = { _, _, _ -> },
) {
    private val valueAnimationSpec: SpringSpec<Float> = spring(1.0f, 1000.0f, visibilityThreshold)
    private val velocityAnimationSpec: SpringSpec<Float> = spring(0.5f, 300.0f, visibilityThreshold * 10.0f)
    private val pressProgressAnimationSpec: SpringSpec<Float> = spring(1.0f, 1000.0f, 0.001f)
    private val scaleXAnimationSpec: SpringSpec<Float> = spring(0.6f, 250.0f, 0.001f)
    private val scaleYAnimationSpec: SpringSpec<Float> = spring(0.7f, 250.0f, 0.001f)

    private val valueAnimation: Animatable<Float, AnimationVector1D> = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation: Animatable<Float, AnimationVector1D> = Animatable(0.0f, 5.0f)
    private val pressProgressAnimation: Animatable<Float, AnimationVector1D> = Animatable(0.0f, 0.001f)
    private val scaleXAnimation: Animatable<Float, AnimationVector1D> = Animatable(initialScale, 0.001f)
    private val scaleYAnimation: Animatable<Float, AnimationVector1D> = Animatable(initialScale, 0.001f)

    private var accumulatedTarget: Float = initialValue
    private val mutatorMutex: MutatorMutex = MutatorMutex()
    private val velocityTracker: VelocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val progress: Float get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val targetValue: Float get() = accumulatedTarget
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { down ->
                onDragStarted(this@DampedDragAnimation, down)
                press()
            },
            onDragEnd = {
                onDragStopped(this@DampedDragAnimation)
                release()
            },
            onDragCancel = {
                onDragStopped(this@DampedDragAnimation)
                release()
            },
            onDrag = { change, dragAmount ->
                change.consume()
                onDrag(this@DampedDragAnimation, size, dragAmount)
            }
        )
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1.0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            try {
                awaitFrame()
                if (value != accumulatedTarget) {
                    val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                    snapshotFlow { valueAnimation.value }
                        .filter { abs(it - accumulatedTarget) <= threshold }
                        .first()
                }
                launch { pressProgressAnimation.animateTo(0.0f, pressProgressAnimationSpec) }
                launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
                launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
            } catch (_: Exception) {}
        }
    }

    fun updateValue(value: Float) {
        accumulatedTarget = value.coerceIn(valueRange.start, valueRange.endInclusive)
        val target = accumulatedTarget
        animationScope.launch {
            launch {
                valueAnimation.animateTo(target, valueAnimationSpec) {
                    updateVelocity()
                }
            }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                accumulatedTarget = value.coerceIn(valueRange.start, valueRange.endInclusive)
                val target = accumulatedTarget
                launch {
                    valueAnimation.animateTo(target, valueAnimationSpec)
                }
                if (velocity != 0.0f) {
                    launch {
                        velocityAnimation.animateTo(0.0f, velocityAnimationSpec)
                    }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        val currentTimeMillis = System.currentTimeMillis()
        velocityTracker.addPosition(currentTimeMillis, Offset(value, 0.0f))
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch {
            velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec)
        }
    }
}
