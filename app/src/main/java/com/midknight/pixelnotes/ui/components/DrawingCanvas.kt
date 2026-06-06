package com.midknight.pixelnotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.midknight.pixelnotes.domain.PointData
import com.midknight.pixelnotes.domain.StrokeData

@Composable
fun DrawingCanvas(
    strokes: MutableList<StrokeData>,
    currentColor: Color,
    currentStrokeWidth: Float,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints by remember { mutableStateOf<MutableList<PointData>>(mutableListOf()) }
    var trigger by remember { mutableIntStateOf(0) }

    val updatedColor by rememberUpdatedState(currentColor)
    val updatedStrokeWidth by rememberUpdatedState(currentStrokeWidth)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val path = Path().apply {
                        moveTo(down.position.x, down.position.y)
                    }
                    val points = mutableListOf(PointData(down.position.x, down.position.y))

                    currentPath = path
                    currentPoints = points

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            path.lineTo(change.position.x, change.position.y)
                            points.add(PointData(change.position.x, change.position.y))
                            trigger++
                        }
                    } while (event.changes.any { it.pressed })

                    strokes.add(
                        StrokeData(
                            points = points.toList(),
                            colorArgb = updatedColor.toArgb(),
                            strokeWidth = updatedStrokeWidth
                        )
                    )
                    currentPath = null
                    currentPoints = mutableListOf()
                }
            }
    ) {
        trigger

        strokes.forEach { strokeData ->
            drawPath(
                path = strokeData.toPath(),
                color = Color(strokeData.colorArgb),
                style = Stroke(
                    width = strokeData.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        currentPath?.let { path ->
            drawPath(
                path = path,
                color = updatedColor,
                style = Stroke(
                    width = updatedStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}