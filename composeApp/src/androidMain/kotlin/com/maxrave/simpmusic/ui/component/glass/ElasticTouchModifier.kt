package com.maxrave.simpmusic.ui.component.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

fun Modifier.elasticGlassTouch(
    enabled: Boolean = true,
    dragEnabled: Boolean = true,
    onTap: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    val scope = rememberCoroutineScope()
    val translateX = remember { Animatable(0f) }
    val translateY = remember { Animatable(0f) }
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }
    var isDragging by remember { mutableStateOf(false) }

    val springSpec = spring<Float>(dampingRatio = 0.7f, stiffness = 450f)
    val releaseSpec = spring<Float>(dampingRatio = 0.6f, stiffness = 500f)

    this
        .graphicsLayer {
            translationX = translateX.value
            translationY = translateY.value
            this.scaleX = scaleX.value
            this.scaleY = scaleY.value
            clip = false
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    scope.launch {
                        scaleX.animateTo(0.95f, springSpec)
                    }
                    scope.launch {
                        scaleY.animateTo(0.95f, springSpec)
                    }
                    val released = tryAwaitRelease()
                    if (released) {
                        if (!isDragging) {
                            onTap?.invoke()
                        }
                    }
                    scope.launch {
                        scaleX.animateTo(1f, releaseSpec)
                    }
                    scope.launch {
                        scaleY.animateTo(1f, releaseSpec)
                    }
                }
            )
        }
        .pointerInput(dragEnabled) {
            if (dragEnabled) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        scope.launch {
                            scaleX.animateTo(0.96f, springSpec)
                            scaleY.animateTo(0.96f, springSpec)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        scope.launch {
                            translateX.animateTo(0f, releaseSpec)
                            translateY.animateTo(0f, releaseSpec)
                            scaleX.animateTo(1f, releaseSpec)
                            scaleY.animateTo(1f, releaseSpec)
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        scope.launch {
                            translateX.animateTo(0f, releaseSpec)
                            translateY.animateTo(0f, releaseSpec)
                            scaleX.animateTo(1f, releaseSpec)
                            scaleY.animateTo(1f, releaseSpec)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            translateX.snapTo(translateX.value + dragAmount.x * 0.4f)
                            translateY.snapTo(translateY.value + dragAmount.y * 0.4f)
                        }
                    }
                )
            }
        }
}
