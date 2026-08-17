package com.maxrave.simpmusic.ui.component.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (Size, Offset) -> Offset = { _, offset -> offset },
) {
    private val pressProgressAnimationSpec: SpringSpec<Float> = spring(0.5f, 300.0f, 0.001f)
    private val positionAnimationSpec: SpringSpec<Offset> = spring(0.5f, 300.0f, Offset(0.5f, 0.5f))

    private val pressProgressAnimation: Animatable<Float, AnimationVector1D> = Animatable(0.0f, 0.001f)
    private val positionAnimation: Animatable<Offset, AnimationVector2D> = Animatable(
        initialValue = Offset.Zero,
        typeConverter = Offset.VectorConverter,
        visibilityThreshold = Offset(0.5f, 0.5f)
    )

    private var startPosition: Offset = Offset.Zero

    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition

    private val shader: RuntimeShader? = if (Build.VERSION.SDK_INT >= 33) {
        RuntimeShader(
            """
            uniform float2 size;
            layout(color) uniform half4 color;
            uniform float radius;
            uniform float2 position;

            half4 main(float2 coord) {
                float dist = distance(coord, position);
                float intensity = smoothstep(radius, radius * 0.5, dist);
                return color * intensity;
            }
            """.trimIndent()
        )
    } else {
        null
    }

    val modifier: Modifier = Modifier.drawWithContent {
        val progress = pressProgressAnimation.value
        if (progress > 0.0f) {
            if (Build.VERSION.SDK_INT < 33 || shader == null) {
                drawRect(
                    color = Color.White.copy(alpha = progress * 0.25f),
                    blendMode = BlendMode.Plus
                )
            } else {
                drawRect(
                    color = Color.White.copy(alpha = progress * 0.08f),
                    blendMode = BlendMode.Plus
                )
                val pos = position(size, positionAnimation.value)
                shader.setFloatUniform("size", size.width, size.height)
                shader.setColorUniform("color", android.graphics.Color.argb((0.15f * progress * 255.0f).toInt(), 255, 255, 255))
                shader.setFloatUniform("radius", size.minDimension * 1.5f)
                shader.setFloatUniform("position", pos.x.fastCoerceIn(0f, size.width), pos.y.fastCoerceIn(0f, size.height))
                drawRect(
                    brush = ShaderBrush(shader),
                    blendMode = BlendMode.Plus
                )
            }
        }
        drawContent()
    }

    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        detectDragGestures(
            onDragStart = { down ->
                startPosition = down
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(1.0f, pressProgressAnimationSpec) }
                    launch { positionAnimation.snapTo(down) }
                }
            },
            onDragEnd = {
                animationScope.launch {
                    pressProgressAnimation.animateTo(0.0f, pressProgressAnimationSpec)
                }
            },
            onDragCancel = {
                animationScope.launch {
                    pressProgressAnimation.animateTo(0.0f, pressProgressAnimationSpec)
                }
            },
            onDrag = { change, _ ->
                change.consume()
                val pos = change.position
                animationScope.launch {
                    positionAnimation.animateTo(pos, positionAnimationSpec)
                }
            }
        )
    }
}
