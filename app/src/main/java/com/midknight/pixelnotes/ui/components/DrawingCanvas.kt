package com.midknight.pixelnotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.midknight.pixelnotes.data.CustomFont
import com.midknight.pixelnotes.domain.*
import com.midknight.pixelnotes.ui.viewmodels.DrawingTool
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

fun getSelectionBounds(strokes: List<StrokeData>, texts: List<TextData>, images: List<ImageData>): Rect {
    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE

    strokes.forEach { stroke -> val b = stroke.getBounds(); if (b.left < minX) minX = b.left; if (b.top < minY) minY = b.top; if (b.right > maxX) maxX = b.right; if (b.bottom > maxY) maxY = b.bottom }
    val textPaint = android.text.TextPaint()
    texts.forEach { textData ->
        textPaint.textSize = textData.fontSize
        val width = textData.maxWidth.toInt().coerceAtLeast(1)
        val layout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.text.StaticLayout.Builder.obtain(textData.text, 0, textData.text.length, textPaint, width)
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .build()
        } else {
            @Suppress("DEPRECATION")
            android.text.StaticLayout(textData.text, textPaint, width, android.text.Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
        }
        val textHeight = layout.height.toFloat()
        var actualMaxWidth = 0f
        for (i in 0 until layout.lineCount) {
            actualMaxWidth = maxOf(actualMaxWidth, layout.getLineWidth(i))
        }
        if (textData.x < minX) minX = textData.x
        if (textData.y < minY) minY = textData.y
        if (textData.x + actualMaxWidth > maxX) maxX = textData.x + actualMaxWidth
        if (textData.y + textHeight > maxY) maxY = textData.y + textHeight
    }
    images.forEach { img -> if (img.x < minX) minX = img.x; if (img.y < minY) minY = img.y; if (img.x + img.width > maxX) maxX = img.x + img.width; if (img.y + img.height > maxY) maxY = img.y + img.height }

    return if (minX == Float.MAX_VALUE) Rect(0f, 0f, 1920f, 1080f) else Rect(minX, minY, maxX, maxY)
}

@Composable
fun DrawingCanvas(
    pageIndex: Int,
    isInfiniteCanvas: Boolean,
    cameraResetTrigger: Int,
    cameraPan: Offset,
    cameraZoom: Float,
    strokes: List<StrokeData>,
    selectedStrokes: List<StrokeData>,
    texts: List<TextData>,
    selectedTexts: List<TextData>,
    images: List<ImageData>,
    selectedImages: List<ImageData>,
    customFonts: List<CustomFont>,
    isSelectionActiveOnPage: Boolean,
    isClipboardEmpty: Boolean,
    selectionMode: Int,
    currentColor: Color,
    currentStrokeWidth: Float,
    currentTool: DrawingTool,
    eraserType: Int,
    fingerDrawingEnabled: Boolean,
    onStrokeAdd: (StrokeData) -> Unit,
    onStrokeRemove: (StrokeData) -> Unit,
    onStrokesRemove: (List<StrokeData>) -> Unit = {},
    onTextToolTap: (Float, Float) -> Unit,
    onTextEdit: (TextData) -> Unit,
    onProcessSelection: (List<PointData>) -> Unit,
    onMoveSelection: (Float, Float) -> Unit,
    onScaleSelection: (Float, Float, Float) -> Unit,
    onCommitSelection: () -> Unit,
    onSelectionLongPress: (Offset) -> Unit,
    onCameraChange: (Offset, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = remember { HapticManager(context) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints by remember { mutableStateOf<MutableList<PointData>>(mutableListOf()) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }
    var trigger by remember { mutableIntStateOf(0) }
    var currentIsEraser by remember { mutableStateOf(false) }

    var processedResetTrigger by remember { mutableIntStateOf(0) }

    val updatedColor by rememberUpdatedState(currentColor); val updatedStrokeWidth by rememberUpdatedState(currentStrokeWidth); val updatedTool by rememberUpdatedState(currentTool); val updatedEraserType by rememberUpdatedState(eraserType); val updatedFingerDrawingEnabled by rememberUpdatedState(fingerDrawingEnabled); val updatedStrokes by rememberUpdatedState(strokes); val updatedSelectedStrokes by rememberUpdatedState(selectedStrokes); val updatedSelectedTexts by rememberUpdatedState(selectedTexts); val updatedSelectedImages by rememberUpdatedState(selectedImages); val updatedIsSelectionActive by rememberUpdatedState(isSelectionActiveOnPage)
    val updatedIsClipboardEmpty by rememberUpdatedState(isClipboardEmpty)
    val updatedSelectionMode by rememberUpdatedState(selectionMode)
    val updatedCameraZoom by rememberUpdatedState(cameraZoom)
    val updatedCameraPan by rememberUpdatedState(cameraPan)

    val resolveUri: (String) -> String = { uri ->
        if (uri.startsWith("internal://")) {
            val fileName = uri.removePrefix("internal://")
            java.io.File(context.filesDir, "inserted_images/$fileName").absolutePath
        } else if (uri.contains("inserted_images/")) {
            val fileName = uri.substringAfterLast("/")
            java.io.File(context.filesDir, "inserted_images/$fileName").absolutePath
        } else {
            uri
        }
    }

    val baseImagePainters = images.map { img: ImageData -> img to rememberAsyncImagePainter(resolveUri(img.uri)) }
    val selectedImagePainters = selectedImages.map { img: ImageData -> img to rememberAsyncImagePainter(resolveUri(img.uri)) }

    Canvas(modifier = modifier.fillMaxSize()
        .pointerInput(Unit) {
        val virtualWidth = 1080f
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val touches = event.changes.filter { it.pressed }
                if (touches.isEmpty()) continue

                val stylusChange = event.changes.find { it.type == PointerType.Stylus || it.type == PointerType.Eraser || it.type == PointerType.Mouse }
                
                val toVirtual = { p: Offset -> 
                    val currentEffectiveScale = (size.width / virtualWidth) * updatedCameraZoom
                    val sVal = if (currentEffectiveScale != 0f) currentEffectiveScale else 1f
                    PointData((p.x - updatedCameraPan.x) / sVal, (p.y - updatedCameraPan.y) / sVal) 
                }

                if (stylusChange != null) {
                    val primaryId = stylusChange.id
                    val effectiveTool = if (event.buttons.isSecondaryPressed) DrawingTool.SELECTION else updatedTool
                    
                    val now = System.currentTimeMillis()
                    val isDoubleTap = (now - lastTapTime) < 300L && kotlin.math.hypot(stylusChange.position.x - lastTapPos.x, stylusChange.position.y - lastTapPos.y) < 40f
                    lastTapTime = now
                    lastTapPos = stylusChange.position

                    if (isDoubleTap) {
                        val tv = toVirtual(stylusChange.position)
                        val textPaint = android.text.TextPaint()
                        (updatedSelectedTexts + texts).forEach { textData ->
                            textPaint.textSize = textData.fontSize
                            val width = textData.maxWidth.toInt().coerceAtLeast(1)
                            val layout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                android.text.StaticLayout.Builder.obtain(textData.text, 0, textData.text.length, textPaint, width).build()
                            } else {
                                @Suppress("DEPRECATION")
                                android.text.StaticLayout(textData.text, textPaint, width, android.text.Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
                            }
                            val textHeight = layout.height.toFloat()
                            var actualMaxWidth = 0f
                            for (i in 0 until layout.lineCount) { actualMaxWidth = maxOf(actualMaxWidth, layout.getLineWidth(i)) }
                            if (tv.x >= textData.x && tv.x <= textData.x + actualMaxWidth && tv.y >= textData.y && tv.y <= textData.y + textHeight) { onTextEdit(textData); return@awaitPointerEventScope }
                        }
                    }

                    if (effectiveTool == DrawingTool.TEXT) {
                        val v = toVirtual(stylusChange.position)
                        onTextToolTap(v.x, v.y)
                        stylusChange.consume()
                    } else if (effectiveTool == DrawingTool.SELECTION) {
                        val tv = toVirtual(stylusChange.position)
                        val touchX = tv.x; val touchY = tv.y
                        val bounds = getSelectionBounds(updatedSelectedStrokes, updatedSelectedTexts, updatedSelectedImages)
                        
                        if (updatedIsSelectionActive && bounds.width > 0f) {
                            val pad = 20f; val left = bounds.left - pad; val top = bounds.top - pad; val right = bounds.right + pad; val bottom = bounds.bottom + pad
                            val hitRadius = 80f / (if (updatedCameraZoom != 0f) updatedCameraZoom else 1f)
                            fun dist(x1: Float, y1: Float, x2: Float, y2: Float) = kotlin.math.hypot(x2 - x1, y2 - y1)
                            val isTL = dist(touchX, touchY, left, top) < hitRadius; val isTR = dist(touchX, touchY, right, top) < hitRadius; val isBL = dist(touchX, touchY, left, bottom) < hitRadius; val isBR = dist(touchX, touchY, right, bottom) < hitRadius
                            
                            if (isTL || isTR || isBL || isBR) {
                                val pivotX = if (isTL || isBL) right else left; val pivotY = if (isTL || isTR) bottom else top; var prevDist = dist(pivotX, pivotY, touchX, touchY)
                                stylusChange.consume()
                                while (true) {
                                    val evLoop = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = evLoop.changes.find { it.id == primaryId }
                                    evLoop.changes.forEach { if (it.id != primaryId) it.consume() }
                                    if (change == null || !change.pressed) break
                                    val cv = toVirtual(change.position)
                                    val currDist = dist(pivotX, pivotY, cv.x, cv.y)
                                    val scaleF = if (prevDist != 0f) currDist / prevDist else 1f
                                    if (scaleF > 0.1f && scaleF < 5f) { onScaleSelection(scaleF, pivotX, pivotY); prevDist = currDist }
                                    change.consume()
                                }
                                continue
                            }
                            if (touchX >= left - 40f && touchX <= right + 40f && touchY >= top - 40f && touchY <= bottom + 40f) {
                                var prevTx = touchX; var prevTy = touchY; var dragStarted = false
                                stylusChange.consume()
                                while (true) {
                                    val evLoop = withTimeoutOrNull(400) { awaitPointerEvent(PointerEventPass.Initial) }
                                    if (evLoop == null && !dragStarted) { onSelectionLongPress(stylusChange.position); break }
                                    val actualEv = evLoop ?: break
                                    val change = actualEv.changes.find { it.id == primaryId }
                                    actualEv.changes.forEach { if (it.id != primaryId) it.consume() }
                                    if (change == null || !change.pressed) break
                                    val cv = toVirtual(change.position)
                                    if (kotlin.math.hypot(cv.x - touchX, cv.y - touchY) > 5f) dragStarted = true
                                    if (dragStarted) { onMoveSelection(cv.x - prevTx, cv.y - prevTy); prevTx = cv.x; prevTy = cv.y }
                                    change.consume()
                                }
                                continue
                            }
                        }
                        
                        onCommitSelection(); var dragStarted = false; val path = Path().apply { moveTo(touchX, touchY) }; val points = mutableListOf(PointData(touchX, touchY, 1f)); currentPath = path; currentPoints = points
                        stylusChange.consume()
                        while (true) {
                            val evLoop = withTimeoutOrNull(400) { awaitPointerEvent(PointerEventPass.Initial) }
                            if (evLoop == null && !dragStarted) { if (updatedIsSelectionActive || !updatedIsClipboardEmpty) onSelectionLongPress(stylusChange.position); break }
                            val actualEv = evLoop ?: break
                            val change = actualEv.changes.find { it.id == primaryId }
                            actualEv.changes.forEach { if (it.id != primaryId) it.consume() }
                            if (change == null || !change.pressed) break
                            val cv = toVirtual(change.position)
                            if (kotlin.math.hypot(cv.x - touchX, cv.y - touchY) > 5f) dragStarted = true
                            if (dragStarted) { path.lineTo(cv.x, cv.y); points.add(PointData(cv.x, cv.y, 1f)); trigger++ }
                            change.consume()
                        }
                        if (dragStarted) { if (updatedSelectionMode == 0 && points.size > 2) path.lineTo(points.first().x, points.first().y); onProcessSelection(points.toList()) }
                        currentPath = null; currentPoints = mutableListOf()
                    } else {
                        currentIsEraser = effectiveTool == DrawingTool.ERASER || stylusChange.type == PointerType.Eraser
                        val sv = toVirtual(stylusChange.position)
                        
                        if (currentIsEraser && updatedEraserType == 1) {
                            val strokesToRemove = mutableSetOf<StrokeData>()
                            stylusChange.consume()
                            while (true) {
                                val evLoop = awaitPointerEvent(PointerEventPass.Initial)
                                val change = evLoop.changes.find { it.id == primaryId }
                                evLoop.changes.forEach { if (it.id != primaryId) it.consume() }
                                if (change == null || !change.pressed) break
                                val cv = toVirtual(change.position); val virtualEraserRadius = updatedStrokeWidth / 2f; val hitRadiusSq = virtualEraserRadius * virtualEraserRadius
                                updatedStrokes.forEach { s -> if (!s.isEraser && s.points.any { p -> val dx = p.x - cv.x; val dy = p.y - cv.y; (dx * dx + dy * dy) < hitRadiusSq }) strokesToRemove.add(s) }
                                if (strokesToRemove.isNotEmpty()) trigger++
                                change.consume()
                            }
                            if (strokesToRemove.isNotEmpty()) onStrokesRemove(strokesToRemove.toList())
                        } else {
                            val path = Path().apply { moveTo(sv.x, sv.y) }
                            val points = mutableListOf(PointData(sv.x, sv.y, if (stylusChange.pressure > 0f) stylusChange.pressure else 0.5f))
                            var prevX = sv.x; var prevY = sv.y
                            currentPath = path; currentPoints = points
                            stylusChange.consume()
                            var isHoldingShape = false
                            while (true) {
                                val evLoop = withTimeoutOrNull(500) { awaitPointerEvent(PointerEventPass.Initial) }
                                if (evLoop == null) { if (!isHoldingShape && points.size > 15) { val snap = detectAndSnapShape(points); if (snap != null) { haptic.heavyClick(); isHoldingShape = true; points.clear(); points.addAll(snap); path.reset(); path.moveTo(points.first().x, points.first().y); for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y); trigger++ } } }
                                val actualEv = evLoop ?: break
                                val change = actualEv.changes.find { it.id == primaryId }
                                actualEv.changes.forEach { if (it.id != primaryId) it.consume() }
                                if (change == null || !change.pressed) break
                                if (!isHoldingShape) {
                                    change.historical.forEach { h -> val hv = toVirtual(h.position); points.add(PointData(hv.x, hv.y, if (change.pressure > 0f) change.pressure else 0.5f)); val midX = (prevX + hv.x) / 2f; val midY = (prevY + hv.y) / 2f; path.quadraticTo(prevX, prevY, midX, midY); prevX = hv.x; prevY = hv.y }
                                    val cv = toVirtual(change.position); points.add(PointData(cv.x, cv.y, if (change.pressure > 0f) change.pressure else 0.5f)); val midX = (prevX + cv.x) / 2f; val midY = (prevY + cv.y) / 2f; path.quadraticTo(prevX, prevY, midX, midY); prevX = cv.x; prevY = cv.y; trigger++
                                }
                                change.consume()
                            }
                            if (!isHoldingShape) path.lineTo(prevX, prevY)
                            haptic.tick()
                            onStrokeAdd(StrokeData(points = points.toList(), colorArgb = updatedColor.toArgb(), strokeWidth = updatedStrokeWidth, isEraser = currentIsEraser, isHighlighter = effectiveTool == DrawingTool.HIGHLIGHTER))
                            currentPath = null; currentPoints = mutableListOf()
                        }
                    }
                } else if (updatedFingerDrawingEnabled && touches.size == 1) {
                    val finger = touches.first()
                    val primaryId = finger.id
                    val sv = toVirtual(finger.position)
                    val path = Path().apply { moveTo(sv.x, sv.y) }
                    val points = mutableListOf(PointData(sv.x, sv.y, 1f))
                    var prevX = sv.x; var prevY = sv.y
                    currentPath = path; currentPoints = points
                    finger.consume()

                    var isZooming = false
                    while (true) {
                        val evLoop = awaitPointerEvent(PointerEventPass.Main)
                        val change = evLoop.changes.find { it.id == primaryId }
                        if (evLoop.changes.count { it.pressed } > 1) { isZooming = true; break }
                        if (change == null || !change.pressed) break
                        val cv = toVirtual(change.position)
                        points.add(PointData(cv.x, cv.y, 1f))
                        val midX = (prevX + cv.x) / 2f; val midY = (prevY + cv.y) / 2f; path.quadraticTo(prevX, prevY, midX, midY); prevX = cv.x; prevY = cv.y
                        trigger++; change.consume()
                    }
                    if (!isZooming) { path.lineTo(prevX, prevY); onStrokeAdd(StrokeData(points = points.toList(), colorArgb = updatedColor.toArgb(), strokeWidth = updatedStrokeWidth, isEraser = updatedTool == DrawingTool.ERASER, isHighlighter = updatedTool == DrawingTool.HIGHLIGHTER)) }
                    currentPath = null; currentPoints = mutableListOf()
                } else if (touches.size > 1 && isInfiniteCanvas) {
                    do {
                        val evLoop = awaitPointerEvent(PointerEventPass.Main)
                        val touchCount = evLoop.changes.count { it.pressed && it.type == PointerType.Touch }
                        if (touchCount > 1) {
                            val zoomChange = evLoop.calculateZoom(); val panChange = evLoop.calculatePan(); val centroid = evLoop.calculateCentroid(useCurrent = false); val oldZoom = updatedCameraZoom; val newZoom = (updatedCameraZoom * zoomChange).coerceIn(0.1f, 50f); val zoomFactor = newZoom / (if (oldZoom != 0f) oldZoom else 1f); val newPan = centroid + (updatedCameraPan - centroid) * zoomFactor + panChange; onCameraChange(newPan, newZoom); evLoop.changes.forEach { it.consume() }; trigger++
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
        }
    }
    ) {
        trigger; val virtualWidth = 1080f; val effectiveScale = (size.width / virtualWidth) * cameraZoom
        if (cameraResetTrigger > processedResetTrigger) { processedResetTrigger = cameraResetTrigger; if (isInfiniteCanvas) { val b = getSelectionBounds(strokes, texts, images); if (b.width > 0f) { val pad = 400f; val targetW = b.width + pad * 2; val targetH = b.height + pad * 2; val desiredEffectiveScale = minOf(size.width / targetW, size.height / targetH); val newZoom = (desiredEffectiveScale / (size.width / virtualWidth)).coerceIn(0.1f, 50f); val newScale = (size.width / virtualWidth) * newZoom; val newPan = Offset(size.width / 2f - b.center.x * newScale, size.height / 2f - b.center.y * newScale); onCameraChange(newPan, newZoom) } else onCameraChange(Offset.Zero, 1f) } else onCameraChange(Offset.Zero, 1f) }

        withTransform({ translate(cameraPan.x, cameraPan.y); scale(scaleX = effectiveScale, scaleY = effectiveScale, pivot = Offset.Zero) }) {
            if (isInfiniteCanvas) { val gc = Color.LightGray.copy(alpha = 0.3f); val sp = 50f; val th = 2f / (if(cameraZoom!=0f) cameraZoom else 1f); for (i in -400..400) { drawLine(gc, Offset(i * sp, -20000f), Offset(i * sp, 20000f), th); drawLine(gc, Offset(-20000f, i * sp), Offset(20000f, i * sp), th) } }
            else drawRect(color = Color.LightGray, topLeft = Offset.Zero, size = Size(1080f, 1527f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))

            baseImagePainters.forEach { (img, p) -> translate(left = img.x, top = img.y) { with(p) { draw(size = Size(img.width, img.height)) } } }

            drawContext.canvas.nativeCanvas.apply {
                val layerCount = saveLayer(null, null)
                strokes.forEach { s ->
                    val color = if (s.isEraser) Color.Transparent else Color(s.colorArgb).copy(alpha = if (s.isHighlighter) 0.4f else 1f)
                    val blend = if (s.isEraser) BlendMode.Clear else if (s.isHighlighter) BlendMode.Multiply else BlendMode.SrcOver
                    val isShape = s.points.size <= 10 || s.points.size == 37
                    if (isShape || s.isHighlighter) drawPath(path = s.toPath(), color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = s.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
                    else drawPath(path = s.toPressurePath(), color = color, style = Fill, blendMode = blend)
                }
                if (updatedIsSelectionActive) {
                    updatedSelectedStrokes.forEach { s ->
                        val color = Color(s.colorArgb).copy(alpha = if (s.isHighlighter) 0.4f else 1f); val blend = if (s.isHighlighter) BlendMode.Multiply else BlendMode.SrcOver; val isShape = s.points.size <= 10 || s.points.size == 37
                        if (isShape || s.isHighlighter) drawPath(path = s.toPath(), color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = s.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
                        else drawPath(path = s.toPressurePath(), color = color, style = Fill, blendMode = blend)
                    }
                }
                currentPath?.let { path ->
                    if (updatedTool != DrawingTool.SELECTION) {
                        val color = if (currentIsEraser) Color.Transparent else updatedColor.copy(alpha = if (updatedTool == DrawingTool.HIGHLIGHTER) 0.4f else 1f); val blend = if (currentIsEraser) BlendMode.Clear else if (updatedTool == DrawingTool.HIGHLIGHTER) BlendMode.Multiply else BlendMode.SrcOver; val isLiveShape = currentPoints.size <= 10 || currentPoints.size == 37
                        if (updatedTool == DrawingTool.HIGHLIGHTER || isLiveShape) drawPath(path = path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = updatedStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
                        else if (currentPoints.isNotEmpty()) { val liveS = StrokeData(points = currentPoints.toList(), colorArgb = color.toArgb(), strokeWidth = updatedStrokeWidth, isEraser = currentIsEraser, isHighlighter = false); drawPath(path = liveS.toPressurePath(), color = color, style = Fill, blendMode = blend) }
                    }
                }
                restoreToCount(layerCount)
            }

            texts.forEach { t -> val fontInfo = customFonts.find { it.name == t.fontName }; val tf = TypefaceManager.getTypeface(context, t.fontName, fontInfo?.fileName); drawContext.canvas.nativeCanvas.apply { save(); translate(t.x, t.y); val textPaint = android.text.TextPaint().apply { color = t.colorArgb; textSize = t.fontSize; typeface = tf; isAntiAlias = true }; val width = t.maxWidth.toInt().coerceAtLeast(1); val layout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { android.text.StaticLayout.Builder.obtain(t.text, 0, t.text.length, textPaint, width).setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).build() } else { @Suppress("DEPRECATION") android.text.StaticLayout(t.text, textPaint, width, android.text.Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false) }; layout.draw(this); restore() } }

            if (updatedIsSelectionActive) {
                updatedSelectedTexts.forEach { t -> val fontInfo = customFonts.find { it.name == t.fontName }; val tf = TypefaceManager.getTypeface(context, t.fontName, fontInfo?.fileName); drawContext.canvas.nativeCanvas.apply { save(); translate(t.x, t.y); val textPaint = android.text.TextPaint().apply { color = t.colorArgb; textSize = t.fontSize; typeface = tf; isAntiAlias = true; alpha = 128 }; val width = t.maxWidth.toInt().coerceAtLeast(1); val layout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { android.text.StaticLayout.Builder.obtain(t.text, 0, t.text.length, textPaint, width).setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).build() } else { @Suppress("DEPRECATION") android.text.StaticLayout(t.text, textPaint, width, android.text.Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false) }; layout.draw(this); restore() } }
                selectedImagePainters.forEach { (img, p) -> translate(left = img.x, top = img.y) { with(p) { draw(size = Size(img.width, img.height), alpha = 0.5f) } } }
                val b = getSelectionBounds(updatedSelectedStrokes, updatedSelectedTexts, updatedSelectedImages)
                if (b.width > 0f) { val pad = 20f; val l = b.left - pad; val t = b.top - pad; val r = b.right + pad; val bot = b.bottom + pad; val ds = 10f / (if(effectiveScale!=0f) effectiveScale else 1f); drawRect(color = Color.Blue, topLeft = Offset(l, t), size = Size(r - l, bot - t), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f / (if(cameraZoom!=0f) cameraZoom else 1f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(ds, ds))), alpha = 0.5f); val hc = Color(0xFF2196F3); listOf(Offset(l, t), Offset(r, t), Offset(l, bot), Offset(r, bot)).forEach { c -> drawCircle(color = hc, radius = 25f / (if(cameraZoom!=0f) cameraZoom else 1f), center = c); drawCircle(color = Color.White, radius = 12f / (if(cameraZoom!=0f) cameraZoom else 1f), center = c) } }
            }

            currentPath?.let { path ->
                if (updatedTool == DrawingTool.SELECTION) { val ds = 10f / (if(effectiveScale!=0f) effectiveScale else 1f); if (updatedSelectionMode == 0) drawPath(path, color = Color.Gray, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f / (if(cameraZoom!=0f) cameraZoom else 1f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(ds, ds)))) else if (currentPoints.isNotEmpty()) { val f = currentPoints.first(); val l = currentPoints.last(); drawRect(color = Color.Gray, topLeft = Offset(minOf(f.x, l.x), minOf(f.y, l.y)), size = Size(kotlin.math.abs(l.x - f.x), kotlin.math.abs(l.y - f.y)), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f / (if(cameraZoom!=0f) cameraZoom else 1f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(ds, ds)))) } }
            }
        }

        val ePos = if (currentPath != null && currentIsEraser) currentPoints.lastOrNull()?.let { Offset(it.x * effectiveScale + cameraPan.x, it.y * effectiveScale + cameraPan.y) } else null
        if (ePos != null && (currentIsEraser || updatedTool == DrawingTool.ERASER)) {
            val sFact = (size.width / virtualWidth) * (if (isInfiniteCanvas) cameraZoom else 1f)
            val eRad = (updatedStrokeWidth / 2f) * sFact
            drawCircle(color = Color.Black.copy(alpha = 0.2f), radius = eRad, center = ePos, style = Fill)
        }
    }
}
