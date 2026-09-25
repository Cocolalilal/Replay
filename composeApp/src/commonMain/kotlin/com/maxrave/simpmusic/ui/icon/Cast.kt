package com.maxrave.simpmusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val SimpIcons.Cast: ImageVector
  get() {
    if (_Cast != null) {
      return _Cast!!
    }
    _Cast =
      ImageVector.Builder(
          name = "Cast",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(21f, 3f)
            horizontalLineTo(3f)
            quadToRelative(-0.82f, 0f, -1.41f, 0.59f)
            reflectiveQuadTo(1f, 5f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            reflectiveQuadTo(2f, 9f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            reflectiveQuadTo(3f, 8f)
            verticalLineTo(5f)
            horizontalLineToRelative(18f)
            verticalLineToRelative(14f)
            horizontalLineToRelative(-5f)
            quadToRelative(-0.42f, 0f, -0.71f, 0.29f)
            reflectiveQuadTo(15f, 20f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(16f, 21f)
            horizontalLineToRelative(5f)
            quadToRelative(0.82f, 0f, 1.41f, -0.59f)
            reflectiveQuadTo(23f, 19f)
            verticalLineTo(5f)
            quadToRelative(0f, -0.82f, -0.59f, -1.41f)
            reflectiveQuadTo(21f, 3f)
            close()
            moveTo(1f, 12.5f)
            verticalLineToRelative(1.5f)
            quadToRelative(3.32f, 0f, 5.66f, 2.34f)
            reflectiveQuadTo(9f, 22f)
            horizontalLineToRelative(1.5f)
            quadToRelative(0f, -3.95f, -2.78f, -6.72f)
            reflectiveQuadTo(1f, 12.5f)
            close()
            moveTo(1f, 16.5f)
            verticalLineToRelative(1.5f)
            quadToRelative(1.66f, 0f, 2.83f, 1.17f)
            reflectiveQuadTo(5f, 22f)
            horizontalLineToRelative(1.5f)
            quadToRelative(0f, -2.28f, -1.61f, -3.89f)
            reflectiveQuadTo(1f, 16.5f)
            close()
            moveTo(1f, 20.5f)
            verticalLineTo(22f)
            horizontalLineToRelative(1.5f)
            quadToRelative(0f, -0.62f, -0.44f, -1.06f)
            reflectiveQuadTo(1f, 20.5f)
            close()
          }
        }
        .build()
    return _Cast!!
  }

private var _Cast: ImageVector? = null
