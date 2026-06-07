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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import com.midknight.pixelnotes.ui.viewmodels.DrawingTool
import kotlin.math.abs

fun getSelectionBounds(strokes: List<StrokeData>): Rect {
    if (strokes.isEmpty()) return Rect.Zero
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    strokes.forEach { stroke ->
        val b = stroke.getBounds()
        if (b.left < minX) minX = b.left
        if (b.top < minY) minY = b.top
        if (b.right > maxX) maxX = b.right
        if (b.bottom > maxY) maxY = b.bottom
    }
    return Rect(minX, minY, maxX, maxY)
}

@Composable
fun DrawingCanvas(
    pageIndex: Int,
    strokes: List<StrokeData>,
    selectedStrokes: List<StrokeData>,
    isSelectionActiveOnPage: Boolean,
    selectionMode: Int,
    currentColor: Color,
    currentStrokeWidth: Float,
    currentTool: DrawingTool,
    eraserType: Int,
    fingerDrawingEnabled: Boolean,
    onStrokeAdd: (StrokeData) -> Unit,
    onStrokeRemove: (StrokeData) -> Unit,
    onProcessSelection: (List<PointData>) -> Unit,
    onMoveSelection: (Float, Float) -> Unit,
    onCommitSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints by remember { mutableStateOf<MutableList<PointData>>(mutableListOf()) }
    var trigger by remember { mutableIntStateOf(0) }
    var currentIsEraser by remember { mutableStateOf(false) }

    val updatedColor by rememberUpdatedState(currentColor)
    val updatedStrokeWidth by rememberUpdatedState(currentStrokeWidth)
    val updatedTool by rememberUpdatedState(currentTool)
    val updatedEraserType by rememberUpdatedState(eraserType)
    val updatedFingerDrawingEnabled by rememberUpdatedState(fingerDrawingEnabled)
    val updatedStrokes by rememberUpdatedState(strokes)
    val updatedSelectedStrokes by rememberUpdatedState(selectedStrokes)
    val updatedIsSelectionActive by rememberUpdatedState(isSelectionActiveOnPage)
    val updatedSelectionMode by rememberUpdatedState(selectionMode)

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

                    val isStylusOrEraser = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                    if (isStylusOrEraser) stylusModeActive = true

                    val isAllowedTouch = if (updatedFingerDrawingEnabled) {
                        !stylusModeActive || isStylusOrEraser
                    } else {
                        isStylusOrEraser
                    }

                    // --- SELECTION TOOL LOGIC ---
                    if (updatedTool == DrawingTool.SELECTION && isAllowedTouch) {
                        val touchX = down.position.x / scaleRatio
                        val touchY = down.position.y / scaleRatio
                        val bounds = getSelectionBounds(updatedSelectedStrokes)

                        // Detect if touch is inside existing bounding box (with padding)
                        val isTouchingSelection = updatedIsSelectionActive &&
                                touchX >= bounds.left - 40f && touchX <= bounds.right + 40f &&
                                touchY >= bounds.top - 40f && touchY <= bounds.bottom + 40f

                        if (isTouchingSelection) {
                            var prevTx = touchX
                            var prevTy = touchY
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change != null && change.pressed) {
                                    change.consume()
                                    val currTx = change.position.x / scaleRatio
                                    val currTy = change.position.y / scaleRatio
                                    onMoveSelection(currTx - prevTx, currTy - prevTy)
                                    prevTx = currTx
                                    prevTy = currTy
                                }
                            } while (event.changes.any { it.pressed })
                            return@awaitEachGesture
                        } else {
                            onCommitSelection() // Deselect previous
                            val path = Path().apply { moveTo(touchX, touchY) }
                            val points = mutableListOf(PointData(touchX, touchY))
                            currentPath = path
                            currentPoints = points
                            var prevX = touchX
                            var prevY = touchY

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change != null && change.pressed) {
                                    change.consume()
                                    val x = change.position.x / scaleRatio
                                    val y = change.position.y / scaleRatio
                                    path.lineTo(x, y)
                                    points.add(PointData(x, y))
                                    prevX = x
                                    prevY = y
                                    trigger++
                                }
                            } while (event.changes.any { it.pressed })

                            if (updatedSelectionMode == 0 && points.size > 2) path.lineTo(points.first().x, points.first().y)
                            onProcessSelection(points.toList())
                            currentPath = null
                            currentPoints = mutableListOf()
                            return@awaitEachGesture
                        }
                    }

                    // --- DRAWING & ERASER LOGIC ---
                    val isHardwareEraser = down.type == PointerType.Eraser
                    val activeEraser = updatedTool == DrawingTool.ERASER || isHardwareEraser
                    currentIsEraser = activeEraser

                    if (activeEraser && updatedEraserType == 1 && isAllowedTouch) {
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change != null && change.pressed) {
                                change.consume()
                                val x = change.position.x / scaleRatio
                                val y = change.position.y / scaleRatio
                                val hitRadiusSq = 2500f
                                updatedStrokes.toList().forEach { stroke ->
                                    if (stroke.points.any { p ->
                                            val dx = p.x - x
                                            val dy = p.y - y
                                            (dx * dx + dy * dy) < hitRadiusSq
                                        }) { onStrokeRemove(stroke) }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        return@awaitEachGesture
                    }

                    val startX = down.position.x / scaleRatio
                    val startY = down.position.y / scaleRatio

                    val path = Path().apply { moveTo(startX, startY) }
                    val points = mutableListOf(PointData(startX, startY))
                    var prevX = startX
                    var prevY = startY

                    if (isAllowedTouch && updatedTool != DrawingTool.TEXT) {
                        currentPath = path
                        currentPoints = points
                    }

                    var isZooming = false
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1) isZooming = true

                        if (!isZooming && isAllowedTouch && updatedTool != DrawingTool.TEXT) {
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

                    if (!isZooming && isAllowedTouch && updatedTool != DrawingTool.TEXT) {
                        path.lineTo(prevX, prevY)
                    }

                    if ((!isZooming || points.size > 3) && isAllowedTouch && updatedTool != DrawingTool.TEXT) {
                        onStrokeAdd(StrokeData(points = points.toList(), colorArgb = updatedColor.toArgb(), strokeWidth = updatedStrokeWidth, isEraser = currentIsEraser, isHighlighter = updatedTool == DrawingTool.HIGHLIGHTER && !isHardwareEraser))
                    }
                    currentPath = null
                    currentPoints = mutableListOf()
                }
            }
    ) {
        trigger
        val virtualWidth = 1080f
        val scaleRatio = size.width / virtualWidth

        withTransform({ scale(scaleX = scaleRatio, scaleY = scaleRatio, pivot = Offset.Zero) }) {
            // Dibujar Trazos Normales
            strokes.forEach { strokeData ->
                val color = if (strokeData.isEraser) Color.Transparent else Color(strokeData.colorArgb).copy(alpha = if (strokeData.isHighlighter) 0.4f else 1f)
                val blend = if (strokeData.isEraser) BlendMode.Clear else if (strokeData.isHighlighter) BlendMode.Multiply else BlendMode.SrcOver
                drawPath(path = strokeData.toPath(), color = color, style = Stroke(width = strokeData.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
            }

            // Dibujar Trazos Seleccionados (Lasso)
            if (updatedIsSelectionActive) {
                updatedSelectedStrokes.forEach { strokeData ->
                    val color = Color(strokeData.colorArgb).copy(alpha = if (strokeData.isHighlighter) 0.4f else 1f)
                    drawPath(path = strokeData.toPath(), color = color, style = Stroke(width = strokeData.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = if (strokeData.isHighlighter) BlendMode.Multiply else BlendMode.SrcOver)
                }
                val bounds = getSelectionBounds(updatedSelectedStrokes)
                if (bounds != Rect.Zero) {
                    drawRect(
                        color = Color.Blue,
                        topLeft = Offset(bounds.left - 10f, bounds.top - 10f),
                        size = Size(bounds.width + 20f, bounds.height + 20f),
                        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f))),
                        alpha = 0.5f
                    )
                }
            }

            // Dibujar en Vivo
            currentPath?.let { path ->
                if (updatedTool == DrawingTool.SELECTION) {
                    if (updatedSelectionMode == 0) {
                        drawPath(path, color = Color.Gray, style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f))))
                    } else if (currentPoints.isNotEmpty()) {
                        val first = currentPoints.first()
                        val last = currentPoints.last()
                        drawRect(color = Color.Gray, topLeft = Offset(minOf(first.x, last.x), minOf(first.y, last.y)), size = Size(abs(last.x - first.x), abs(last.y - first.y)), style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f))))
                    }
                } else {
                    val color = if (currentIsEraser) Color.Transparent else updatedColor.copy(alpha = if (updatedTool == DrawingTool.HIGHLIGHTER) 0.4f else 1f)
                    val blend = if (currentIsEraser) BlendMode.Clear else if (updatedTool == DrawingTool.HIGHLIGHTER) BlendMode.Multiply else BlendMode.SrcOver
                    drawPath(path = path, color = color, style = Stroke(width = updatedStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
                }
            }
        }
    }
}