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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.midknight.pixelnotes.data.CustomFont
import com.midknight.pixelnotes.domain.ImageData
import com.midknight.pixelnotes.domain.PointData
import com.midknight.pixelnotes.domain.StrokeData
import com.midknight.pixelnotes.domain.TextData
import com.midknight.pixelnotes.domain.TypefaceManager
import com.midknight.pixelnotes.domain.detectAndSnapShape
import com.midknight.pixelnotes.ui.viewmodels.DrawingTool

fun getSelectionBounds(strokes: List<StrokeData>, texts: List<TextData>, images: List<ImageData>): Rect {
    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE

    strokes.forEach { stroke -> val b = stroke.getBounds(); if (b.left < minX) minX = b.left; if (b.top < minY) minY = b.top; if (b.right > maxX) maxX = b.right; if (b.bottom > maxY) maxY = b.bottom }
    val paint = android.graphics.Paint()
    texts.forEach { textData -> paint.textSize = textData.fontSize; val width = paint.measureText(textData.text); if (textData.x < minX) minX = textData.x; if (textData.y - textData.fontSize < minY) minY = textData.y - textData.fontSize; if (textData.x + width > maxX) maxX = textData.x + width; if (textData.y > maxY) maxY = textData.y }
    images.forEach { img -> if (img.x < minX) minX = img.x; if (img.y < minY) minY = img.y; if (img.x + img.width > maxX) maxX = img.x + img.width; if (img.y + img.height > maxY) maxY = img.y + img.height }

    return if (minX == Float.MAX_VALUE) Rect.Zero else Rect(minX, minY, maxX, maxY)
}

@Composable
fun DrawingCanvas(
    pageIndex: Int, strokes: List<StrokeData>, selectedStrokes: List<StrokeData>, texts: List<TextData>, selectedTexts: List<TextData>, images: List<ImageData>, selectedImages: List<ImageData>, customFonts: List<CustomFont>, isSelectionActiveOnPage: Boolean, selectionMode: Int, currentColor: Color, currentStrokeWidth: Float, currentTool: DrawingTool, eraserType: Int, fingerDrawingEnabled: Boolean,
    onStrokeAdd: (StrokeData) -> Unit, onStrokeRemove: (StrokeData) -> Unit, onTextToolTap: (Float, Float) -> Unit, onProcessSelection: (List<PointData>) -> Unit, onMoveSelection: (Float, Float) -> Unit, onScaleSelection: (Float, Float, Float) -> Unit, onCommitSelection: () -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints by remember { mutableStateOf<MutableList<PointData>>(mutableListOf()) }
    var trigger by remember { mutableIntStateOf(0) }
    var currentIsEraser by remember { mutableStateOf(false) }

    val updatedColor by rememberUpdatedState(currentColor); val updatedStrokeWidth by rememberUpdatedState(currentStrokeWidth); val updatedTool by rememberUpdatedState(currentTool); val updatedEraserType by rememberUpdatedState(eraserType); val updatedFingerDrawingEnabled by rememberUpdatedState(fingerDrawingEnabled); val updatedStrokes by rememberUpdatedState(strokes); val updatedSelectedStrokes by rememberUpdatedState(selectedStrokes); val updatedSelectedTexts by rememberUpdatedState(selectedTexts); val updatedSelectedImages by rememberUpdatedState(selectedImages); val updatedIsSelectionActive by rememberUpdatedState(isSelectionActiveOnPage); val updatedSelectionMode by rememberUpdatedState(selectionMode)

    val baseImagePainters = images.map { it to rememberAsyncImagePainter(it.uri) }
    val selectedImagePainters = selectedImages.map { it to rememberAsyncImagePainter(it.uri) }

    Canvas(
        modifier = modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.pointerInput(Unit) {
            val virtualWidth = 1080f; var stylusModeActive = false
            awaitEachGesture {
                val down = awaitFirstDown()
                val scaleRatio = size.width.toFloat() / virtualWidth
                val isStylusOrEraser = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                if (isStylusOrEraser) stylusModeActive = true
                val isAllowedTouch = if (updatedFingerDrawingEnabled) { !stylusModeActive || isStylusOrEraser } else { isStylusOrEraser }

                if (updatedTool == DrawingTool.TEXT && isAllowedTouch) { val touchX = down.position.x / scaleRatio; val touchY = down.position.y / scaleRatio; onTextToolTap(touchX, touchY); return@awaitEachGesture }

                if (updatedTool == DrawingTool.SELECTION && isAllowedTouch) {
                    val touchX = down.position.x / scaleRatio; val touchY = down.position.y / scaleRatio
                    val bounds = getSelectionBounds(updatedSelectedStrokes, updatedSelectedTexts, updatedSelectedImages)

                    if (updatedIsSelectionActive && bounds != Rect.Zero) {
                        val pad = 20f; val left = bounds.left - pad; val top = bounds.top - pad; val right = bounds.right + pad; val bottom = bounds.bottom + pad
                        val hitRadius = 80f
                        fun dist(x1: Float, y1: Float, x2: Float, y2: Float) = kotlin.math.hypot(x2 - x1, y2 - y1)
                        val isTL = dist(touchX, touchY, left, top) < hitRadius; val isTR = dist(touchX, touchY, right, top) < hitRadius; val isBL = dist(touchX, touchY, left, bottom) < hitRadius; val isBR = dist(touchX, touchY, right, bottom) < hitRadius

                        if (isTL || isTR || isBL || isBR) {
                            val pivotX = if (isTL || isBL) right else left; val pivotY = if (isTL || isTR) bottom else top; var prevDist = dist(pivotX, pivotY, touchX, touchY)
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change != null && change.pressed) {
                                    change.consume()
                                    val currX = change.position.x / scaleRatio; val currY = change.position.y / scaleRatio
                                    val currDist = dist(pivotX, pivotY, currX, currY); val scale = currDist / prevDist
                                    if (scale > 0.1f && scale < 5f) { onScaleSelection(scale, pivotX, pivotY); prevDist = currDist }
                                }
                            } while (event.changes.any { it.pressed })
                            return@awaitEachGesture
                        }
                        val isInside = touchX >= left - 40f && touchX <= right + 40f && touchY >= top - 40f && touchY <= bottom + 40f
                        if (isInside) {
                            var prevTx = touchX; var prevTy = touchY
                            do {
                                val event = awaitPointerEvent(); val change = event.changes.firstOrNull { it.id == down.id }
                                if (change != null && change.pressed) { change.consume(); val currTx = change.position.x / scaleRatio; val currTy = change.position.y / scaleRatio; onMoveSelection(currTx - prevTx, currTy - prevTy); prevTx = currTx; prevTy = currTy }
                            } while (event.changes.any { it.pressed })
                            return@awaitEachGesture
                        }
                    }
                    onCommitSelection()
                    val path = Path().apply { moveTo(touchX, touchY) }; val points = mutableListOf(PointData(touchX, touchY)); currentPath = path; currentPoints = points; var prevX = touchX; var prevY = touchY
                    do {
                        val event = awaitPointerEvent(); val change = event.changes.firstOrNull { it.id == down.id }
                        if (change != null && change.pressed) { change.consume(); val x = change.position.x / scaleRatio; val y = change.position.y / scaleRatio; path.lineTo(x, y); points.add(PointData(x, y)); prevX = x; prevY = y; trigger++ }
                    } while (event.changes.any { it.pressed })
                    if (updatedSelectionMode == 0 && points.size > 2) path.lineTo(points.first().x, points.first().y)
                    onProcessSelection(points.toList()); currentPath = null; currentPoints = mutableListOf()
                    return@awaitEachGesture
                }

                val isHardwareEraser = down.type == PointerType.Eraser
                val activeEraser = updatedTool == DrawingTool.ERASER || isHardwareEraser
                currentIsEraser = activeEraser

                if (activeEraser && updatedEraserType == 1 && isAllowedTouch) {
                    do {
                        val event = awaitPointerEvent(); val change = event.changes.firstOrNull { it.id == down.id }
                        if (change != null && change.pressed) { change.consume(); val x = change.position.x / scaleRatio; val y = change.position.y / scaleRatio; val hitRadiusSq = 2500f; updatedStrokes.toList().forEach { stroke -> if (stroke.points.any { p -> val dx = p.x - x; val dy = p.y - y; (dx * dx + dy * dy) < hitRadiusSq }) { onStrokeRemove(stroke) } } }
                    } while (event.changes.any { it.pressed })
                    return@awaitEachGesture
                }

                val startX = down.position.x / scaleRatio; val startY = down.position.y / scaleRatio
                val path = Path().apply { moveTo(startX, startY) }
                val points = mutableListOf(PointData(startX, startY))
                var prevX = startX; var prevY = startY

                if (isAllowedTouch && updatedTool != DrawingTool.TEXT) { currentPath = path; currentPoints = points }

                var isZooming = false
                var isHoldingShape = false // <-- BANDERA PARA AUTO-SHAPE

                do {
                    // AQUÍ ESTÁ LA MAGIA: Esperamos hasta 500 milisegundos
                    val event = withTimeoutOrNull(500) { awaitPointerEvent() }

                    if (event == null) {
                        // Si event es null, significa que pasaron 500ms y no moviste el dedo. ¡ACTIVAR AUTO-SHAPE!
                        if (!isHoldingShape && !isZooming && isAllowedTouch && updatedTool != DrawingTool.TEXT && points.size > 15) {
                            val perfectShape = detectAndSnapShape(points)
                            if (perfectShape != null) {
                                isHoldingShape = true
                                points.clear()
                                points.addAll(perfectShape)

                                path.reset()
                                path.moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) { path.lineTo(points[i].x, points[i].y) }
                                trigger++
                            }
                        }
                    } else {
                        // Si event NO es null, el dedo se sigue moviendo o levantando normalmente
                        if (event.changes.size > 1) isZooming = true
                        if (!isZooming && isAllowedTouch && updatedTool != DrawingTool.TEXT) {
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change != null && change.pressed) {
                                change.consume()
                                if (!isHoldingShape) { // Solo dibujar si NO se ha activado la figura perfecta
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
                        }
                    }
                } while (event == null || event.changes.any { it.pressed })

                if (!isZooming && isAllowedTouch && updatedTool != DrawingTool.TEXT) {
                    if (!isHoldingShape) path.lineTo(prevX, prevY)
                }
                if ((!isZooming || points.size > 3) && isAllowedTouch && updatedTool != DrawingTool.TEXT) {
                    onStrokeAdd(StrokeData(points = points.toList(), colorArgb = updatedColor.toArgb(), strokeWidth = updatedStrokeWidth, isEraser = currentIsEraser, isHighlighter = updatedTool == DrawingTool.HIGHLIGHTER && !isHardwareEraser))
                }
                currentPath = null
                currentPoints = mutableListOf()
            }
        }
    ) {
        trigger; val virtualWidth = 1080f; val scaleRatio = size.width / virtualWidth
        withTransform({ scale(scaleX = scaleRatio, scaleY = scaleRatio, pivot = Offset.Zero) }) {
            baseImagePainters.forEach { (img, painter) -> translate(left = img.x, top = img.y) { with(painter) { draw(size = Size(img.width, img.height)) } } }
            strokes.forEach { strokeData -> val color = if (strokeData.isEraser) Color.Transparent else Color(strokeData.colorArgb).copy(alpha = if (strokeData.isHighlighter) 0.4f else 1f); val blend = if (strokeData.isEraser) BlendMode.Clear else if (strokeData.isHighlighter) BlendMode.Multiply else BlendMode.SrcOver; drawPath(path = strokeData.toPath(), color = color, style = Stroke(width = strokeData.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend) }
            texts.forEach { textData -> val fontInfo = customFonts.find { it.name == textData.fontName }; val tf = TypefaceManager.getTypeface(context, textData.fontName, fontInfo?.fileName); drawContext.canvas.nativeCanvas.apply { val paint = android.graphics.Paint().apply { color = textData.colorArgb; textSize = textData.fontSize; typeface = tf; isAntiAlias = true }; drawText(textData.text, textData.x, textData.y, paint) } }

            if (updatedIsSelectionActive) {
                updatedSelectedStrokes.forEach { strokeData -> val color = Color(strokeData.colorArgb).copy(alpha = if (strokeData.isHighlighter) 0.4f else 1f); drawPath(path = strokeData.toPath(), color = color, style = Stroke(width = strokeData.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = if (strokeData.isHighlighter) BlendMode.Multiply else BlendMode.SrcOver) }
                updatedSelectedTexts.forEach { textData -> val fontInfo = customFonts.find { it.name == textData.fontName }; val tf = TypefaceManager.getTypeface(context, textData.fontName, fontInfo?.fileName); drawContext.canvas.nativeCanvas.apply { val paint = android.graphics.Paint().apply { color = textData.colorArgb; textSize = textData.fontSize; typeface = tf; isAntiAlias = true; alpha = 128 }; drawText(textData.text, textData.x, textData.y, paint) } }
                selectedImagePainters.forEach { (img, painter) -> translate(left = img.x, top = img.y) { with(painter) { draw(size = Size(img.width, img.height), alpha = 0.5f) } } }

                val bounds = getSelectionBounds(updatedSelectedStrokes, updatedSelectedTexts, updatedSelectedImages)
                if (bounds != Rect.Zero) {
                    val pad = 20f; val left = bounds.left - pad; val top = bounds.top - pad; val right = bounds.right + pad; val bottom = bounds.bottom + pad
                    drawRect(color = Color.Blue, topLeft = Offset(left, top), size = Size(right - left, bottom - top), style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f))), alpha = 0.5f)
                    val handleColor = Color(0xFF2196F3)
                    listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom)).forEach { corner ->
                        drawCircle(color = handleColor, radius = 25f, center = corner); drawCircle(color = Color.White, radius = 12f, center = corner)
                    }
                }
            }

            currentPath?.let { path ->
                if (updatedTool == DrawingTool.SELECTION) {
                    if (updatedSelectionMode == 0) { drawPath(path, color = Color.Gray, style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f)))) }
                    else if (currentPoints.isNotEmpty()) { val first = currentPoints.first(); val last = currentPoints.last(); drawRect(color = Color.Gray, topLeft = Offset(minOf(first.x, last.x), minOf(first.y, last.y)), size = Size(kotlin.math.abs(last.x - first.x), kotlin.math.abs(last.y - first.y)), style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f)))) }
                } else {
                    val color = if (currentIsEraser) Color.Transparent else updatedColor.copy(alpha = if (updatedTool == DrawingTool.HIGHLIGHTER) 0.4f else 1f)
                    val blend = if (currentIsEraser) BlendMode.Clear else if (updatedTool == DrawingTool.HIGHLIGHTER) BlendMode.Multiply else BlendMode.SrcOver
                    drawPath(path = path, color = color, style = Stroke(width = updatedStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
                }
            }
        }
    }
}