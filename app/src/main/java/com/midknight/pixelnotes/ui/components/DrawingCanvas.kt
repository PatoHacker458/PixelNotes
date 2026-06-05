package com.midknight.pixelnotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.input.pointer.pointerInput
import com.midknight.pixelnotes.domain.Stroke

@Composable
fun DrawingCanvas(modifier: Modifier = Modifier) {
    val strokes = remember { mutableStateListOf<Stroke>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var trigger by remember { mutableIntStateOf(0) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val path = Path().apply {
                        moveTo(down.position.x, down.position.y)
                    }
                    currentPath = path

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            path.lineTo(change.position.x, change.position.y)
                            trigger++
                        }
                    } while (event.changes.any { it.pressed })

                    strokes.add(Stroke(path, Color.Black, 8f))
                    currentPath = null
                }
            }
    ) {
        trigger

        strokes.forEach { stroke ->
            drawPath(
                path = stroke.path,
                color = stroke.color,
                style = DrawStroke(
                    width = stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        currentPath?.let { path ->
            drawPath(
                path = path,
                color = Color.Black,
                style = DrawStroke(
                    width = 8f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}