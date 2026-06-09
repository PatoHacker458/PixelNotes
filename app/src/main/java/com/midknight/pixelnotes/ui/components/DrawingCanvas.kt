package com.midknight.pixelnotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
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

    return if (minX == Float.MAX_VALUE) Rect(0f, 0f, 1920f, 1080f) else Rect(minX, minY, maxX, maxY)
}

@Composable
fun DrawingCanvas(
    pageIndex: Int, isInfiniteCanvas: Boolean, cameraResetTrigger: Int, strokes: List<StrokeData>, selectedStrokes: List<StrokeData>, texts: List<TextData>, selectedTexts: List<TextData>, images: List<ImageData>, selectedImages: List<ImageData>, customFonts: List<CustomFont>, isSelectionActiveOnPage: Boolean, selectionMode: Int, currentColor: Color, currentStrokeWidth: Float, currentTool: DrawingTool, eraserType: Int, fingerDrawingEnabled: Boolean,
    onStrokeAdd: (StrokeData) -> Unit, onStrokeRemove: (StrokeData) -> Unit, onTextToolTap: (Float, Float) -> Unit, onProcessSelection: (List<PointData>) -> Unit, onMoveSelection: (Float, Float) -> Unit, onScaleSelection: (Float, Float, Float) -> Unit, onCommitSelection: () -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints by remember { mutableStateOf<MutableList<PointData>>(mutableListOf()) }
    var trigger by remember { mutableIntStateOf(0) }
    var currentIsEraser by remember { mutableStateOf(false) }

    var cameraPan by remember { mutableStateOf(Offset.Zero) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    var processedResetTrigger by remember { mutableIntStateOf(0) }

    val updatedColor by rememberUpdatedState(currentColor); val updatedStrokeWidth by rememberUpdatedState(currentStrokeWidth); val updatedTool by rememberUpdatedState(currentTool); val updatedEraserType by rememberUpdatedState(eraserType); val updatedFingerDrawingEnabled by rememberUpdatedState(fingerDrawingEnabled); val updatedStrokes by rememberUpdatedState(strokes); val updatedSelectedStrokes by rememberUpdatedState(selectedStrokes); val updatedSelectedTexts by rememberUpdatedState(selectedTexts); val updatedSelectedImages by rememberUpdatedState(selectedImages); val updatedIsSelectionActive by rememberUpdatedState(isSelectionActiveOnPage); val updatedSelectionMode by rememberUpdatedState(selectionMode)

    val baseImagePainters = images.map { it to rememberAsyncImagePainter(it.uri) }
    val selectedImagePainters = selectedImages.map { it to rememberAsyncImagePainter(it.uri) }

    Canvas(modifier = modifier.fillMaxSize().pointerInput(Unit) {
        val virtualWidth = 1080f; var stylusModeActive = false
        awaitEachGesture {
            val down = awaitFirstDown()
            val isStylusOrEraser = down.type == PointerType.Stylus || down.type == PointerType.Eraser
            if (isStylusOrEraser) stylusModeActive = true
            val isAllowedTouch = if (updatedFingerDrawingEnabled) { !stylusModeActive || isStylusOrEraser } else { isStylusOrEraser }

            val currentEffectiveScale = (size.width / virtualWidth) * cameraZoom
            val toVirtual = { x: Float, y: Float -> PointData((x - cameraPan.x) / currentEffectiveScale, (y - cameraPan.y) / currentEffectiveScale) }

            if (updatedTool == DrawingTool.TEXT && isAllowedTouch) { val v = toVirtual(down.position.x, down.position.y); onTextToolTap(v.x, v.y); return@awaitEachGesture }

            if (updatedTool == DrawingTool.SELECTION && isAllowedTouch) {
                val tv = toVirtual(down.position.x, down.position.y); val touchX = tv.x; val touchY = tv.y
                val bounds = getSelectionBounds(updatedSelectedStrokes, updatedSelectedTexts, updatedSelectedImages)

                if (updatedIsSelectionActive && bounds.width > 0f) {
                    val pad = 20f; val left = bounds.left - pad; val top = bounds.top - pad; val right = bounds.right + pad; val bottom = bounds.bottom + pad
                    val hitRadius = 80f / cameraZoom
                    fun dist(x1: Float, y1: Float, x2: Float, y2: Float) = kotlin.math.hypot(x2 - x1, y2 - y1)
                    val isTL = dist(touchX, touchY, left, top) < hitRadius; val isTR = dist(touchX, touchY, right, top) < hitRadius; val isBL = dist(touchX, touchY, left, bottom) < hitRadius; val isBR = dist(touchX, touchY, right, bottom) < hitRadius

                    if (isTL || isTR || isBL || isBR) {
                        val pivotX = if (isTL || isBL) right else left; val pivotY = if (isTL || isTR) bottom else top; var prevDist = dist(pivotX, pivotY, touchX, touchY)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change != null && change.pressed) {
                                change.consume()
                                val cv = toVirtual(change.position.x, change.position.y)
                                val currDist = dist(pivotX, pivotY, cv.x, cv.y); val scale = currDist / prevDist
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
                            if (change != null && change.pressed) { change.consume(); val cv = toVirtual(change.position.x, change.position.y); onMoveSelection(cv.x - prevTx, cv.y - prevTy); prevTx = cv.x; prevTy = cv.y }
                        } while (event.changes.any { it.pressed })
                        return@awaitEachGesture
                    }
                }
                onCommitSelection()
                val path = Path().apply { moveTo(touchX, touchY) }; val points = mutableListOf(PointData(touchX, touchY, 1f)); currentPath = path; currentPoints = points; var prevX = touchX; var prevY = touchY
                do {
                    val event = awaitPointerEvent(); val change = event.changes.firstOrNull { it.id == down.id }
                    if (change != null && change.pressed) { change.consume(); val cv = toVirtual(change.position.x, change.position.y); path.lineTo(cv.x, cv.y); points.add(PointData(cv.x, cv.y, 1f)); prevX = cv.x; prevY = cv.y; trigger++ }
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
                    if (change != null && change.pressed) { change.consume(); val cv = toVirtual(change.position.x, change.position.y); val hitRadiusSq = (2500f) / cameraZoom; updatedStrokes.toList().forEach { stroke -> if (stroke.points.any { p -> val dx = p.x - cv.x; val dy = p.y - cv.y; (dx * dx + dy * dy) < hitRadiusSq }) { onStrokeRemove(stroke) } } }
                } while (event.changes.any { it.pressed })
                return@awaitEachGesture
            }

            val sv = toVirtual(down.position.x, down.position.y)
            val startP = if (down.type == PointerType.Stylus) down.pressure else 1f
            val path = Path().apply { moveTo(sv.x, sv.y) }
            val points = mutableListOf(PointData(sv.x, sv.y, startP))
            var prevX = sv.x; var prevY = sv.y

            if (isAllowedTouch && updatedTool != DrawingTool.TEXT) { currentPath = path; currentPoints = points }

            var isZooming = false
            var isHoldingShape = false

            do {
                val event = withTimeoutOrNull(500) { awaitPointerEvent() }

                if (event == null) {
                    if (!isHoldingShape && !isZooming && isAllowedTouch && updatedTool != DrawingTool.TEXT && points.size > 15) {
                        val perfectShape = detectAndSnapShape(points)
                        if (perfectShape != null) {
                            isHoldingShape = true
                            points.clear(); points.addAll(perfectShape)
                            path.reset(); path.moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) { path.lineTo(points[i].x, points[i].y) }
                            trigger++
                        }
                    }
                } else {
                    if (event.changes.size > 1) {
                        isZooming = true
                        if (isInfiniteCanvas) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = false)
                            val oldZoom = cameraZoom
                            cameraZoom = (cameraZoom * zoomChange).coerceIn(0.1f, 50f)
                            cameraPan = cameraPan + panChange
                            cameraPan = cameraPan + (centroid - cameraPan) - (centroid - cameraPan) * (cameraZoom / oldZoom)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                            trigger++
                        }
                    } else if (!isZooming && isAllowedTouch && updatedTool != DrawingTool.TEXT) {
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change != null && change.pressed) {
                            change.consume()
                            if (!isHoldingShape) {
                                change.historical.forEach { hist ->
                                    val hv = toVirtual(hist.position.x, hist.position.y)
                                    val hp = if (change.type == PointerType.Stylus) change.pressure else 1f
                                    points.add(PointData(hv.x, hv.y, hp))
                                    val midX = (prevX + hv.x) / 2f; val midY = (prevY + hv.y) / 2f
                                    path.quadraticBezierTo(prevX, prevY, midX, midY)
                                    prevX = hv.x; prevY = hv.y
                                }
                                val cv = toVirtual(change.position.x, change.position.y)
                                val p = if (change.type == PointerType.Stylus) change.pressure else 1f
                                points.add(PointData(cv.x, cv.y, p))
                                val midX = (prevX + cv.x) / 2f; val midY = (prevY + cv.y) / 2f
                                path.quadraticBezierTo(prevX, prevY, midX, midY)
                                prevX = cv.x; prevY = cv.y
                                trigger++
                            }
                        }
                    }
                }
            } while (event == null || event.changes.any { it.pressed })

            if (!isZooming && isAllowedTouch && updatedTool != DrawingTool.TEXT) { if (!isHoldingShape) path.lineTo(prevX, prevY) }
            if ((!isZooming || points.size > 3) && isAllowedTouch && updatedTool != DrawingTool.TEXT) {
                onStrokeAdd(StrokeData(points = points.toList(), colorArgb = updatedColor.toArgb(), strokeWidth = updatedStrokeWidth, isEraser = currentIsEraser, isHighlighter = updatedTool == DrawingTool.HIGHLIGHTER && !isHardwareEraser))
            }
            currentPath = null
            currentPoints = mutableListOf()
        }
    }
    ) {
        trigger; val virtualWidth = 1080f; val effectiveScale = (size.width / virtualWidth) * cameraZoom

        if (cameraResetTrigger > processedResetTrigger) {
            processedResetTrigger = cameraResetTrigger
            if (isInfiniteCanvas) {
                val bounds = getSelectionBounds(strokes, texts, images)
                if (bounds.width > 0f) {
                    val pad = 400f
                    val targetW = bounds.width + pad * 2
                    val targetH = bounds.height + pad * 2
                    val desiredEffectiveScale = minOf(size.width / targetW, size.height / targetH)
                    cameraZoom = (desiredEffectiveScale / (size.width / virtualWidth)).coerceIn(0.1f, 50f)
                    val newScale = (size.width / virtualWidth) * cameraZoom
                    cameraPan = Offset(size.width / 2f - bounds.center.x * newScale, size.height / 2f - bounds.center.y * newScale)
                } else { cameraPan = Offset.Zero; cameraZoom = 1f }
            } else { cameraPan = Offset.Zero; cameraZoom = 1f }
        }

        withTransform({ translate(cameraPan.x, cameraPan.y); scale(scaleX = effectiveScale, scaleY = effectiveScale, pivot = Offset.Zero) }) {
            if (isInfiniteCanvas) {
                val gridColor = Color.LightGray.copy(alpha = 0.3f); val spacing = 50f; val thickness = 2f / cameraZoom
                for (i in -400..400) { drawLine(gridColor, Offset(i * spacing, -20000f), Offset(i * spacing, 20000f), thickness); drawLine(gridColor, Offset(-20000f, i * spacing), Offset(20000f, i * spacing), thickness) }
            } else {
                drawRect(color = Color.LightGray, topLeft = Offset.Zero, size = Size(1080f, 1527f), style = Stroke(width = 2f))
            }

            baseImagePainters.forEach { (img, painter) -> translate(left = img.x, top = img.y) { with(painter) { draw(size = Size(img.width, img.height)) } } }

            drawContext.canvas.nativeCanvas.apply {
                val layerCount = saveLayer(null, null)

                strokes.forEach { strokeData ->
                    val color = if (strokeData.isEraser) Color.Transparent else Color(strokeData.colorArgb).copy(alpha = if (strokeData.isHighlighter) 0.4f else 1f)
                    val blend = if (strokeData.isEraser) BlendMode.Clear else if (strokeData.isHighlighter) BlendMode.Multiply else BlendMode.SrcOver
                    val isShape = strokeData.points.size <= 10 || strokeData.points.size == 37
                    if (isShape || strokeData.isHighlighter) {
                        drawPath(path = strokeData.toPath(), color = color, style = Stroke(width = strokeData.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
                    } else {
                        if (strokeData.points.isNotEmpty()) {
                            var prev = strokeData.points.first()
                            for (i in 1 until strokeData.points.size) { val curr = strokeData.points[i]; val p1 = if (prev.p <= 0f) 1f else prev.p; val p2 = if (curr.p <= 0f) 1f else curr.p; val width = strokeData.strokeWidth * ((p1 + p2) / 2f); drawLine(color = color, start = Offset(prev.x, prev.y), end = Offset(curr.x, curr.y), strokeWidth = width, cap = StrokeCap.Round, blendMode = blend); prev = curr }
                        }
                    }
                }

                if (updatedIsSelectionActive) {
                    updatedSelectedStrokes.forEach { strokeData ->
                        val color = Color(strokeData.colorArgb).copy(alpha = if (strokeData.isHighlighter) 0.4f else 1f); val blend = if (strokeData.isHighlighter) BlendMode.Multiply else BlendMode.SrcOver; val isShape = strokeData.points.size <= 10 || strokeData.points.size == 37
                        if (isShape || strokeData.isHighlighter) { drawPath(path = strokeData.toPath(), color = color, style = Stroke(width = strokeData.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend) } else { if (strokeData.points.isNotEmpty()) { var prev = strokeData.points.first(); for (i in 1 until strokeData.points.size) { val curr = strokeData.points[i]; val p1 = if (prev.p <= 0f) 1f else prev.p; val p2 = if (curr.p <= 0f) 1f else curr.p; val width = strokeData.strokeWidth * ((p1 + p2) / 2f); drawLine(color = color, start = Offset(prev.x, prev.y), end = Offset(curr.x, curr.y), strokeWidth = width, cap = StrokeCap.Round, blendMode = blend); prev = curr } } }
                    }
                }

                currentPath?.let { path ->
                    if (updatedTool != DrawingTool.SELECTION) {
                        val color = if (currentIsEraser) Color.Transparent else updatedColor.copy(alpha = if (updatedTool == DrawingTool.HIGHLIGHTER) 0.4f else 1f); val blend = if (currentIsEraser) BlendMode.Clear else if (updatedTool == DrawingTool.HIGHLIGHTER) BlendMode.Multiply else BlendMode.SrcOver; val isLiveShape = currentPoints.size <= 10 || currentPoints.size == 37
                        if (updatedTool == DrawingTool.HIGHLIGHTER || isLiveShape) { drawPath(path = path, color = color, style = Stroke(width = updatedStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend) } else { if (currentPoints.isNotEmpty()) { var prev = currentPoints.first(); for (i in 1 until currentPoints.size) { val curr = currentPoints[i]; val p1 = if (prev.p <= 0f) 1f else prev.p; val p2 = if (curr.p <= 0f) 1f else curr.p; val width = updatedStrokeWidth * ((p1 + p2) / 2f); drawLine(color = color, start = Offset(prev.x, prev.y), end = Offset(curr.x, curr.y), strokeWidth = width, cap = StrokeCap.Round, blendMode = blend); prev = curr } } }
                    }
                }

                restoreToCount(layerCount)
            }

            texts.forEach { textData -> val fontInfo = customFonts.find { it.name == textData.fontName }; val tf = TypefaceManager.getTypeface(context, textData.fontName, fontInfo?.fileName); drawContext.canvas.nativeCanvas.apply { save(); scale(1f / effectiveScale, 1f / effectiveScale); val paint = android.graphics.Paint().apply { color = textData.colorArgb; textSize = textData.fontSize * effectiveScale; typeface = tf; isAntiAlias = true; isLinearText = true; isSubpixelText = true }; drawText(textData.text, textData.x * effectiveScale, textData.y * effectiveScale, paint); restore() } }

            if (updatedIsSelectionActive) {
                updatedSelectedTexts.forEach { textData -> val fontInfo = customFonts.find { it.name == textData.fontName }; val tf = TypefaceManager.getTypeface(context, textData.fontName, fontInfo?.fileName); drawContext.canvas.nativeCanvas.apply { save(); scale(1f / effectiveScale, 1f / effectiveScale); val paint = android.graphics.Paint().apply { color = textData.colorArgb; textSize = textData.fontSize * effectiveScale; typeface = tf; isAntiAlias = true; isLinearText = true; isSubpixelText = true; alpha = 128 }; drawText(textData.text, textData.x * effectiveScale, textData.y * effectiveScale, paint); restore() } }
                selectedImagePainters.forEach { (img, painter) -> translate(left = img.x, top = img.y) { with(painter) { draw(size = Size(img.width, img.height), alpha = 0.5f) } } }
                val bounds = getSelectionBounds(updatedSelectedStrokes, updatedSelectedTexts, updatedSelectedImages)
                if (bounds.width > 0f) { val pad = 20f; val left = bounds.left - pad; val top = bounds.top - pad; val right = bounds.right + pad; val bottom = bounds.bottom + pad; drawRect(color = Color.Blue, topLeft = Offset(left, top), size = Size(right - left, bottom - top), style = Stroke(width = 3f / cameraZoom, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f))), alpha = 0.5f); val handleColor = Color(0xFF2196F3); listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom)).forEach { corner -> drawCircle(color = handleColor, radius = 25f / cameraZoom, center = corner); drawCircle(color = Color.White, radius = 12f / cameraZoom, center = corner) } }
            }

            currentPath?.let { path ->
                if (updatedTool == DrawingTool.SELECTION) {
                    if (updatedSelectionMode == 0) { drawPath(path, color = Color.Gray, style = Stroke(width = 3f / cameraZoom, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f)))) } else if (currentPoints.isNotEmpty()) { val first = currentPoints.first(); val last = currentPoints.last(); drawRect(color = Color.Gray, topLeft = Offset(minOf(first.x, last.x), minOf(first.y, last.y)), size = Size(kotlin.math.abs(last.x - first.x), kotlin.math.abs(last.y - first.y)), style = Stroke(width = 3f / cameraZoom, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f)))) }
                }
            }
        }
    }
}