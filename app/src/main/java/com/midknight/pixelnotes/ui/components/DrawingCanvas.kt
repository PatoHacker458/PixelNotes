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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import com.midknight.pixelnotes.domain.PointData
import com.midknight.pixelnotes.domain.StrokeData

@Composable
fun DrawingCanvas(
    strokes: List<StrokeData>,
    currentColor: Color,
    currentStrokeWidth: Float,
    isEraserMode: Boolean,
    onStrokeAdd: (StrokeData) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints by remember { mutableStateOf<MutableList<PointData>>(mutableListOf()) }
    var trigger by remember { mutableIntStateOf(0) }

    val updatedColor by rememberUpdatedState(currentColor)
    val updatedStrokeWidth by rememberUpdatedState(currentStrokeWidth)
    val updatedIsEraser by rememberUpdatedState(isEraserMode)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .pointerInput(Unit) {
                val virtualWidth = 1080f
                var stylusModeActive = false

                awaitEachGesture {
                    val down = awaitFirstDown()
                    val scaleRatio = size.width.toFloat() / virtualWidth

                    if (down.type == PointerType.Stylus || down.type == PointerType.Eraser) {
                        stylusModeActive = true
                    }

                    val isAllowedTouch = !stylusModeActive || (down.type == PointerType.Stylus || down.type == PointerType.Eraser)

                    val startX = down.position.x / scaleRatio
                    val startY = down.position.y / scaleRatio

                    val path = Path().apply { moveTo(startX, startY) }
                    val points = mutableListOf(PointData(startX, startY))
                    var prevX = startX
                    var prevY = startY

                    if (isAllowedTouch) {
                        currentPath = path
                        currentPoints = points
                    }

                    var isZooming = false

                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1) {
                            isZooming = true
                        }

                        if (!isZooming && isAllowedTouch) {
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change != null && change.pressed) {
                                change.consume()
                                val x = change.position.x / scaleRatio
                                val y = change.position.y / scaleRatio

                                val midX = (prevX + x) / 2f
                                val midY = (prevY + y) / 2f

                                path.quadraticBezierTo(prevX, prevY, midX, midY)
                                prevX = x
                                prevY = y

                                points.add(PointData(x, y))
                                trigger++
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (!isZooming && isAllowedTouch) {
                        path.lineTo(prevX, prevY)
                    }

                    if ((!isZooming || points.size > 3) && isAllowedTouch) {
                        onStrokeAdd(
                            StrokeData(
                                points = points.toList(),
                                colorArgb = updatedColor.toArgb(),
                                strokeWidth = updatedStrokeWidth,
                                isEraser = updatedIsEraser || down.type == PointerType.Eraser
                            )
                        )
                    }
                    currentPath = null
                    currentPoints = mutableListOf()
                }
            }
    ) {
        trigger
        val virtualWidth = 1080f
        val scaleRatio = size.width / virtualWidth

        withTransform({
            scale(scaleX = scaleRatio, scaleY = scaleRatio, pivot = Offset.Zero)
        }) {
            strokes.forEach { strokeData ->
                drawPath(
                    path = strokeData.toPath(),
                    color = if (strokeData.isEraser) Color.Transparent else Color(strokeData.colorArgb),
                    style = Stroke(width = strokeData.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = if (strokeData.isEraser) BlendMode.Clear else BlendMode.SrcOver
                )
            }

            currentPath?.let { path ->
                drawPath(
                    path = path,
                    color = if (updatedIsEraser) Color.Transparent else updatedColor,
                    style = Stroke(width = updatedStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = if (updatedIsEraser) BlendMode.Clear else BlendMode.SrcOver
                )
            }
        }
    }
}