package com.midknight.pixelnotes.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun MorphingLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "m3_expressive")

    val shapes = remember {
        listOf(
            RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.7f, rounding = CornerRounding(0.15f)),
            RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.8f, rounding = CornerRounding(0.2f)),
            RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.75f, rounding = CornerRounding(0.4f)),
            RoundedPolygon(numVertices = 6, rounding = CornerRounding(0.4f)),
            RoundedPolygon.star(numVerticesPerRadius = 7, innerRadius = 0.85f, rounding = CornerRounding(0.3f)),
            RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.85f, rounding = CornerRounding(0.3f)),
            RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = 0.9f, rounding = CornerRounding(0.25f))
        )
    }

    val morphs = remember {
        shapes.mapIndexed { index, shape ->
            Morph(shape, shapes[(index + 1) % shapes.size])
        }
    }

    val globalProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = shapes.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = shapes.size * 800, easing = LinearEasing)
        ),
        label = "morph_progress"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val currentIndex = (globalProgress.toInt()) % shapes.size
    val rawFraction = globalProgress - globalProgress.toInt()

    val morphEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val easedFraction = morphEasing.transform(rawFraction)

    val scaleMultiplier = 1f + (0.08f * sin(rawFraction * PI).toFloat())

    val activeMorph = morphs[currentIndex]

    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val shapeColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrimColor)
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(containerColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(38.dp)) {
                val baseScale = size.width / 2f
                val path = activeMorph.toPath(easedFraction).asComposePath()

                withTransform({
                    translate(left = center.x, top = center.y)

                    scale(scaleX = baseScale * scaleMultiplier, scaleY = baseScale * scaleMultiplier, pivot = Offset.Zero)
                    rotate(degrees = rotation, pivot = Offset.Zero)
                }) {
                    drawPath(
                        path = path,
                        color = shapeColor
                    )
                }
            }
        }
    }
}