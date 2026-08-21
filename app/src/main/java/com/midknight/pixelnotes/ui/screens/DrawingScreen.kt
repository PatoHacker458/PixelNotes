package com.midknight.pixelnotes.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.midknight.pixelnotes.data.PageEntity
import com.midknight.pixelnotes.domain.PdfExporter
import com.midknight.pixelnotes.domain.TextData
import com.midknight.pixelnotes.ui.components.DrawingCanvas
import com.midknight.pixelnotes.ui.components.ExpressiveIconButton
import com.midknight.pixelnotes.ui.components.ExpressiveButton
import com.midknight.pixelnotes.ui.viewmodels.DrawingTool
import com.midknight.pixelnotes.ui.viewmodels.NotesViewModel
import com.midknight.pixelnotes.domain.HapticManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfPageBackground(pdfPath: String, pageIndex: Int) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(pdfPath, pageIndex) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = java.io.File(pdfPath)
                if (file.exists()) {
                    val fd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = android.graphics.pdf.PdfRenderer(fd)
                    if (pageIndex < renderer.pageCount) {
                        val page = renderer.openPage(pageIndex)
                        val scale = 2000f / page.height
                        val bmp = android.graphics.Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bmp)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        bitmap = bmp.asImageBitmap()
                    }
                    renderer.close()
                    fd.close()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    bitmap?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } ?: run { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
}

@Composable
fun PaperTemplate(style: Int, modifier: Modifier = Modifier) {
    if (style == 0) return
    Canvas(modifier = modifier) {
        val spacing = 30.dp.toPx()
        val color = Color.LightGray.copy(alpha = 0.5f)
        when (style) {
            1 -> { for (y in (spacing.toInt()..size.height.toInt() step spacing.toInt())) { drawLine(color, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 2f) } }
            2 -> { 
                for (x in (spacing.toInt()..size.width.toInt() step spacing.toInt())) { drawLine(color, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 2f) }
                for (y in (spacing.toInt()..size.height.toInt() step spacing.toInt())) { drawLine(color, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 2f) }
            }
            3 -> { 
                for (x in (spacing.toInt()..size.width.toInt() step spacing.toInt())) {
                    for (y in (spacing.toInt()..size.height.toInt() step spacing.toInt())) {
                        drawCircle(color, 2f, Offset(x.toFloat(), y.toFloat()))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(viewModel: NotesViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = remember { HapticManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val customFonts by viewModel.customFonts.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp
    
    val listState = rememberLazyListState()

    val backgroundPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.updateActivePageBackground(it.toString())
        }
    }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.addFloatingImageToPage(viewModel.activePageIndex, it.toString()) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
        if (success) viewModel.pendingCameraUri?.let { viewModel.addFloatingImageToPage(viewModel.activePageIndex, it.toString()) }
    }

    var showToolOptions by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showCanvasColorMenu by remember { mutableStateOf(false) }
    var showPaperMenu by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showPagesPanel by remember { mutableStateOf(false) }
    var showBottomBar by remember { mutableStateOf(true) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPos by remember { mutableStateOf(Offset.Zero) }
    var showColorPicker by remember { mutableStateOf(false) }

    var isTextEditing by remember { mutableStateOf(false) }
    var editingTextData by remember { mutableStateOf<TextData?>(null) }
    var currentTextInput by remember { mutableStateOf("") }
    var selectedFontName by remember { mutableStateOf("Default") }
    var fontMenuExpanded by remember { mutableStateOf(false) }
    var textEditPageIndex by remember { mutableIntStateOf(-1) }
    var textEditVirtualPos by remember { mutableStateOf(Offset.Zero) }
    
    val canvasColors = listOf(-1, 0xFFFFFFFF.toInt(), 0xFFF5F5DC.toInt(), 0xFFE0F2F1.toInt(), 0xFFFCE4EC.toInt(), 0xFFFFF3E0.toInt())

    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceVariant) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
            val maxWidthPx = constraints.maxWidth.toFloat()
            val maxHeightPx = constraints.maxHeight.toFloat()
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            var pointerCount by remember { mutableIntStateOf(0) }

            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo }.collect { layoutInfo ->
                    val visibleItems = layoutInfo.visibleItemsInfo
                    if (visibleItems.isNotEmpty()) {
                        val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                        val mostVisible = visibleItems.minByOrNull { Math.abs((it.offset + it.size / 2) - viewportCenter) }
                        mostVisible?.let { viewModel.activePageIndex = it.index }
                    }
                }
            }
            
            val onGesture = { centroid: Offset, panChange: Offset, zoomChange: Float ->
                if (showPagesPanel) { }
                else {
                    val oldScale = scale
                    scale = (scale * zoomChange).coerceIn(1f, 15f)
                    val zoomFactor = scale / oldScale
                    val newOffset = centroid + (offset - centroid) * zoomFactor + panChange
                    val maxX = (maxWidthPx * (scale - 1f)) / 2f
                    val maxY = (maxHeightPx * (scale - 1f)) / 2f
                    var finalOffsetY = newOffset.y
                    var overflowY = 0f
                    if (finalOffsetY > maxY) { overflowY = finalOffsetY - maxY; finalOffsetY = maxY }
                    else if (finalOffsetY < -maxY) { overflowY = finalOffsetY + maxY; finalOffsetY = -maxY }
                    if (overflowY != 0f) { coroutineScope.launch { listState.dispatchRawDelta(-overflowY) } }
                    offset = Offset(newOffset.x.coerceIn(if (maxX > 0) -maxX else 0f, if (maxX > 0) maxX else 0f), finalOffsetY)
                }
            }

            if (viewModel.isCurrentNoteInfinite) {
                val page = viewModel.currentPages.firstOrNull()
                if (page != null) {
                    Box(modifier = Modifier.fillMaxSize().background(if (page.canvasColor == -1) Color(0xFFF5F5F5) else Color(page.canvasColor)).clipToBounds()) {
                        DrawingCanvas(
                            pageIndex = 0,
                            isInfiniteCanvas = true,
                            cameraResetTrigger = viewModel.cameraResetTrigger,
                            cameraPan = viewModel.cameraPan,
                            cameraZoom = viewModel.cameraZoom,
                            strokes = page.drawingData,
                            selectedStrokes = if (viewModel.selectionPageIndex == 0) viewModel.selectedStrokes else emptyList(),
                            texts = page.textData,
                            selectedTexts = if (viewModel.selectionPageIndex == 0) viewModel.selectedTexts else emptyList(),
                            images = page.imageData,
                            selectedImages = if (viewModel.selectionPageIndex == 0) viewModel.selectedImages else emptyList(),
                            customFonts = customFonts,
                            isSelectionActiveOnPage = viewModel.selectionPageIndex == 0,
                            isClipboardEmpty = viewModel.isClipboardEmpty,
                            selectionMode = viewModel.selectionMode,
                            currentColor = viewModel.currentColor,
                            currentStrokeWidth = if (viewModel.currentTool == DrawingTool.ERASER) viewModel.currentEraserWidth else viewModel.currentStrokeWidth,
                            currentTool = viewModel.currentTool,
                            eraserType = viewModel.eraserType,
                            fingerDrawingEnabled = viewModel.fingerDrawingEnabled,
                            onStrokeAdd = { viewModel.addStrokeToPage(0, it) },
                            onStrokeRemove = { viewModel.removeStrokeFromPage(0, it) },
                            onStrokesRemove = { viewModel.removeStrokesFromPage(0, it) },
                            onTextToolTap = { x, y -> 
                                textEditPageIndex = 0
                                editingTextData = null
                                currentTextInput = ""
                                selectedFontName = viewModel.currentFontName
                                isTextEditing = true
                                textEditVirtualPos = Offset(x, y)
                            },
                            onTextEdit = { textData ->
                                textEditPageIndex = 0
                                editingTextData = textData
                                currentTextInput = textData.text
                                selectedFontName = textData.fontName
                                isTextEditing = true
                            },
                            onProcessSelection = { points -> viewModel.processSelection(0, points) },
                            onMoveSelection = { dx, dy -> viewModel.moveSelection(dx, dy) },
                            onScaleSelection = { s, px, py -> viewModel.scaleSelection(s, px, py) },
                            onCommitSelection = { viewModel.commitSelection() },
                            onSelectionLongPress = { pos -> contextMenuPos = pos; showContextMenu = true },
                            onCameraChange = { p, z -> viewModel.cameraPan = p; viewModel.cameraZoom = z },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                val density = LocalDensity.current
                val baseHeightDp = with(density) { (maxHeightPx * 0.95f).toDp() }
                
                Row(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = showPagesPanel,
                        enter = slideInHorizontally(initialOffsetX = { -it }),
                        exit = slideOutHorizontally(targetOffsetX = { -it })
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxHeight().width(200.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                Text("Pages", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp))
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(viewModel.currentPages.size) { index ->
                                        val isSelected = viewModel.activePageIndex == index
                                        Card(
                                            onClick = { 
                                                haptic.click()
                                                viewModel.activePageIndex = index
                                                coroutineScope.launch { listState.animateScrollToItem(index) }
                                            },
                                            modifier = Modifier.fillMaxWidth().aspectRatio(1f / 1.414f),
                                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                                Box(modifier = Modifier.fillMaxSize().background(if (viewModel.currentPages[index].canvasColor == -1) Color.White else Color(viewModel.currentPages[index].canvasColor)).clip(RoundedCornerShape(2.dp))) {
                                                    PaperTemplate(style = viewModel.currentPages[index].paperStyle, modifier = Modifier.fillMaxSize().alpha(0.3f))
                                                }
                                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
                                                IconButton(
                                                    onClick = { viewModel.deletePageAt(index) },
                                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Page", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        OutlinedButton(
                                            onClick = { viewModel.addNewPage() },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Text("Add Page")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .pointerInput(Unit) { 
                                awaitEachGesture { 
                                    while (true) { 
                                        val event = awaitPointerEvent()
                                        pointerCount = event.changes.count { it.pressed && it.type == PointerType.Touch }
                                        if (event.changes.all { !it.pressed }) { pointerCount = 0; break } 
                                    } 
                                } 
                            }
                            .pointerInput(Unit) { 
                                detectTransformGestures { centroid, pan, zoom, _ -> 
                                    if (pointerCount >= 2) { 
                                        onGesture(centroid - Offset(maxWidthPx / 2f, maxHeightPx / 2f), pan, zoom) 
                                    }
                                } 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        LazyColumn(
                            state = listState,
                            userScrollEnabled = scale == 1f && pointerCount == 1,
                            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(vertical = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            items(viewModel.currentPages.size) { index ->
                                val page = viewModel.currentPages[index]
                                Box(modifier = Modifier.height(baseHeightDp).aspectRatio(1f / 1.414f).shadow(8.dp).background(if (page.canvasColor == -1) Color.White else Color(page.canvasColor))) {
                                    PaperTemplate(style = page.paperStyle, modifier = Modifier.fillMaxSize())
                                    page.backgroundUri?.let { uriStr ->
                                        if (uriStr.contains("?pdfPage=")) { val parts = uriStr.split("?pdfPage="); PdfPageBackground(pdfPath = parts[0], pageIndex = parts[1].toInt()) }
                                        else { AsyncImage(model = uriStr, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                    }
                                    DrawingCanvas(
                                        pageIndex = index,
                                        isInfiniteCanvas = false,
                                        cameraResetTrigger = viewModel.cameraResetTrigger,
                                        cameraPan = viewModel.cameraPan,
                                        cameraZoom = viewModel.cameraZoom,
                                        strokes = page.drawingData,
                                        selectedStrokes = if (viewModel.selectionPageIndex == index) viewModel.selectedStrokes else emptyList(),
                                        texts = page.textData,
                                        selectedTexts = if (viewModel.selectionPageIndex == index) viewModel.selectedTexts else emptyList(),
                                        images = page.imageData,
                                        selectedImages = if (viewModel.selectionPageIndex == index) viewModel.selectedImages else emptyList(),
                                        customFonts = customFonts,
                                        isSelectionActiveOnPage = viewModel.selectionPageIndex == index,
                                        isClipboardEmpty = viewModel.isClipboardEmpty,
                                        selectionMode = viewModel.selectionMode,
                                        currentColor = viewModel.currentColor,
                                        currentStrokeWidth = if (viewModel.currentTool == DrawingTool.ERASER) viewModel.currentEraserWidth else viewModel.currentStrokeWidth,
                                        currentTool = viewModel.currentTool,
                                        eraserType = viewModel.eraserType,
                                        fingerDrawingEnabled = viewModel.fingerDrawingEnabled,
                                        onStrokeAdd = { viewModel.addStrokeToPage(index, it) },
                                        onStrokeRemove = { viewModel.removeStrokeFromPage(index, it) },
                                        onStrokesRemove = { viewModel.removeStrokesFromPage(index, it) },
                                        onTextToolTap = { x, y -> 
                                            textEditPageIndex = index
                                            editingTextData = null
                                            currentTextInput = ""
                                            selectedFontName = viewModel.currentFontName
                                            isTextEditing = true
                                            textEditVirtualPos = Offset(x, y)
                                        },
                                        onTextEdit = { textData ->
                                            textEditPageIndex = index
                                            editingTextData = textData
                                            currentTextInput = textData.text
                                            selectedFontName = textData.fontName
                                            isTextEditing = true
                                        },
                                        onProcessSelection = { points -> viewModel.processSelection(index, points) },
                                        onMoveSelection = { dx, dy -> viewModel.moveSelection(dx, dy) },
                                        onScaleSelection = { s, px, py -> viewModel.scaleSelection(s, px, py) },
                                        onCommitSelection = { viewModel.commitSelection() },
                                        onSelectionLongPress = { pos -> contextMenuPos = pos; showContextMenu = true },
                                        onCameraChange = { _, _ -> },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, start = 16.dp, end = 16.dp).align(Alignment.TopCenter)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    ExpressiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tooltip = "Back", onClick = { viewModel.saveCurrentNote(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())); onBack() }, containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    
                    val topBarScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExpressiveIconButton(icon = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tooltip = "Undo", onClick = { viewModel.undo() })
                            ExpressiveIconButton(icon = Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tooltip = "Redo", onClick = { viewModel.redo() })
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                            ExpressiveIconButton(icon = Icons.Filled.Add, contentDescription = "Add", tooltip = "Add", onClick = { showAddMenu = true })
                            DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                                DropdownMenuItem(text = { Text("Add Page") }, trailingIcon = { Icon(Icons.Filled.InsertPageBreak, contentDescription = null) }, onClick = { viewModel.addNewPage(); showAddMenu = false })
                                DropdownMenuItem(text = { Text("Import Image") }, trailingIcon = { Icon(Icons.Filled.Image, contentDescription = null) }, onClick = { showAddMenu = false; imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                                DropdownMenuItem(text = { Text("Take Photo") }, trailingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) }, onClick = { showAddMenu = false; val uri = viewModel.createImageUri(context); if (uri != null) { viewModel.pendingCameraUri = uri; cameraLauncher.launch(uri) } })
                            }
                            ExpressiveIconButton(icon = Icons.Filled.IosShare, contentDescription = "Export", tooltip = "Export", onClick = { showExportMenu = true })
                            DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                                DropdownMenuItem(text = { Text("Export as PDF") }, trailingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) }, onClick = { 
                                    showExportMenu = false
                                    coroutineScope.launch { 
                                        PdfExporter(context).exportToPdf(listOf(viewModel.selectedNoteWithPages!!), viewModel.selectedNoteWithPages!!.note.id.let { Uri.parse("content://fake") })
                                        Toast.makeText(context, "Exporting...", Toast.LENGTH_SHORT).show()
                                    }
                                })
                                DropdownMenuItem(text = { Text("Export as .pxnote") }, trailingIcon = { Icon(Icons.Filled.FileUpload, contentDescription = null) }, onClick = { 
                                    showExportMenu = false
                                    coroutineScope.launch {
                                        com.midknight.pixelnotes.domain.SingleNotePackage(context).exportNote(viewModel.selectedNoteWithPages!!)
                                    }
                                })
                            }
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))

                        Row(
                            modifier = Modifier.widthIn(max = screenWidth.dp / 2f).pointerInput(Unit) {}.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent { drawContent(); val leftStrength = (topBarScrollState.value.toFloat() / 80f).coerceIn(0f, 1f); val rightStrength = ((topBarScrollState.maxValue - topBarScrollState.value).toFloat() / 80f).coerceIn(0f, 1f); drawRect(brush = Brush.horizontalGradient(0f to Color.Black.copy(alpha = 1f - leftStrength), 0.25f to Color.Black, startX = 0f, endX = size.width), blendMode = BlendMode.DstIn); drawRect(brush = Brush.horizontalGradient(0.75f to Color.Black, 1f to Color.Black.copy(alpha = 1f - rightStrength), startX = 0f, endX = size.width), blendMode = BlendMode.DstIn) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.horizontalScroll(topBarScrollState).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!viewModel.isCurrentNoteInfinite) {
                                    ExpressiveIconButton(icon = Icons.Filled.Layers, contentDescription = "Pages", tooltip = "Pages", onClick = { showPagesPanel = !showPagesPanel }, contentColor = if (showPagesPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    ExpressiveIconButton(icon = if (showBottomBar) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Toggle Toolbar", tooltip = if (showBottomBar) "Hide Toolbar" else "Show Toolbar", onClick = { showBottomBar = !showBottomBar }, contentColor = if (showBottomBar) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    ExpressiveIconButton(icon = if (showBottomBar) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Toggle Toolbar", tooltip = if (showBottomBar) "Hide Toolbar" else "Show Toolbar", onClick = { showBottomBar = !showBottomBar }, contentColor = if (showBottomBar) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                ExpressiveIconButton(icon = if (viewModel.fingerDrawingEnabled) Icons.Filled.TouchApp else Icons.Filled.PanTool, contentDescription = "Toggle Finger/Stylus", tooltip = if (viewModel.fingerDrawingEnabled) "Finger Drawing: On" else "Finger Drawing: Off", onClick = { viewModel.fingerDrawingEnabled = !viewModel.fingerDrawingEnabled }, contentColor = if (viewModel.fingerDrawingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                Box {
                                    val paperIcon = when (if (viewModel.currentPages.isNotEmpty()) viewModel.currentPages[viewModel.activePageIndex].paperStyle else 0) { 1 -> Icons.Filled.ViewHeadline; 2 -> Icons.Filled.GridOn; 3 -> Icons.Filled.Grain; else -> Icons.Filled.CheckBoxOutlineBlank }
                                    ExpressiveIconButton(icon = paperIcon, contentDescription = "Paper Style", tooltip = "Paper Style", onClick = { showPaperMenu = true })
                                    DropdownMenu(expanded = showPaperMenu, onDismissRequest = { showPaperMenu = false }) {
                                        DropdownMenuItem(text = { Text("Blank", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.CheckBoxOutlineBlank, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.updateActivePagePaperStyle(0); showPaperMenu = false })
                                        DropdownMenuItem(text = { Text("Lined", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.ViewHeadline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.updateActivePagePaperStyle(1); showPaperMenu = false })
                                        DropdownMenuItem(text = { Text("Grid", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.updateActivePagePaperStyle(2); showPaperMenu = false })
                                        DropdownMenuItem(text = { Text("Dotted", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.Grain, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.updateActivePagePaperStyle(3); showPaperMenu = false })
                                    }
                                }
                                Box {
                                    ExpressiveIconButton(icon = Icons.Filled.FormatColorFill, contentDescription = "Canvas Color", tooltip = "Canvas Color", onClick = { showCanvasColorMenu = true })
                                    DropdownMenu(expanded = showCanvasColorMenu, onDismissRequest = { showCanvasColorMenu = false }) {
                                        canvasColors.forEach { colorArgb -> DropdownMenuItem(text = { Text(if (colorArgb == -1) "Default White" else "Solid Color", fontWeight = FontWeight.Bold) }, trailingIcon = { Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(if (colorArgb == -1) Color.White else Color(colorArgb)).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)) }, onClick = { viewModel.updateActivePageCanvasColor(colorArgb); showCanvasColorMenu = false }) }
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        DropdownMenuItem(text = { Text("Set Page Background", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.Wallpaper, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showCanvasColorMenu = false; backgroundPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showBottomBar, enter = expandVertically(expandFrom = Alignment.Bottom) + slideInVertically(initialOffsetY = { it }), exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + slideOutVertically(targetOffsetY = { it }), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).zIndex(5f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(visible = showToolOptions, enter = expandVertically(), exit = shrinkVertically()) {
                        val optionsScrollState = rememberScrollState()
                        Row(modifier = Modifier.padding(bottom = 16.dp).widthIn(max = screenWidth.dp - 32.dp).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)).pointerInput(Unit) {}.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Box(modifier = Modifier.weight(1f, fill = false).graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent { drawContent(); val leftStrength = (optionsScrollState.value.toFloat() / 80f).coerceIn(0f, 1f); val rightStrength = ((optionsScrollState.maxValue - optionsScrollState.value).toFloat() / 80f).coerceIn(0f, 1f); drawRect(brush = Brush.horizontalGradient(0f to Color.Black.copy(alpha = 1f - leftStrength), 0.25f to Color.Black, startX = 0f, endX = size.width), blendMode = BlendMode.DstIn); drawRect(brush = Brush.horizontalGradient(0.75f to Color.Black, 1f to Color.Black.copy(alpha = 1f - rightStrength), startX = 0f, endX = size.width), blendMode = BlendMode.DstIn) }) {
                                Row(modifier = Modifier.horizontalScroll(optionsScrollState).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    if (viewModel.currentTool == DrawingTool.PEN || viewModel.currentTool == DrawingTool.HIGHLIGHTER) {
                                        ColorSelectionRow(selectedColor = viewModel.currentColor, recentColors = viewModel.recentColors, onColorPickRequest = { showColorPicker = true }, onColorSelected = { viewModel.addToRecentColors(it) }, haptic = haptic)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${(viewModel.currentStrokeWidth / 40f * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Slider(value = viewModel.currentStrokeWidth, onValueChange = { if (it.toInt() != viewModel.currentStrokeWidth.toInt()) haptic.tick(); viewModel.currentStrokeWidth = it }, valueRange = 1f..40f, modifier = Modifier.width(100.dp))
                                    } else if (viewModel.currentTool == DrawingTool.ERASER) {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.eraserType == 0) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { haptic.click(); viewModel.eraserType = 0 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Normal", color = if (viewModel.eraserType == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                                        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.eraserType == 1) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { haptic.click(); viewModel.eraserType = 1 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Stroke", color = if (viewModel.eraserType == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${(viewModel.currentEraserWidth / 100f * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Slider(value = viewModel.currentEraserWidth, onValueChange = { if (it.toInt() != viewModel.currentEraserWidth.toInt()) haptic.tick(); viewModel.updateEraserWidth(it) }, valueRange = 1f..100f, modifier = Modifier.width(100.dp))
                                    } else if (viewModel.currentTool == DrawingTool.SELECTION) {
                                        if (viewModel.selectedStrokes.isNotEmpty() || viewModel.selectedTexts.isNotEmpty()) {
                                            ColorSelectionRow(selectedColor = viewModel.currentColor, recentColors = viewModel.recentColors, onColorPickRequest = { showColorPicker = true }, onColorSelected = { viewModel.addToRecentColors(it); viewModel.changeSelectionColor(it.toArgb()) }, haptic = haptic)
                                            if (viewModel.selectedTexts.isNotEmpty()) {
                                                IconButton(onClick = { val textToEdit = viewModel.selectedTexts.first(); textEditPageIndex = viewModel.selectionPageIndex; editingTextData = textToEdit; currentTextInput = textToEdit.text; selectedFontName = textToEdit.fontName; isTextEditing = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit Selected Text") }
                                                Box { IconButton(onClick = { fontMenuExpanded = true }) { Icon(Icons.Filled.Title, contentDescription = "Change Font") }; DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) { DropdownMenuItem(text = { Text("Default") }, onClick = { haptic.click(); viewModel.changeSelectionFont("Default"); fontMenuExpanded = false }); DropdownMenuItem(text = { Text("Serif") }, onClick = { haptic.click(); viewModel.changeSelectionFont("Serif"); fontMenuExpanded = false }); DropdownMenuItem(text = { Text("Monospace") }, onClick = { haptic.click(); viewModel.changeSelectionFont("Monospace"); fontMenuExpanded = false }); DropdownMenuItem(text = { Text("Cursive") }, onClick = { haptic.click(); viewModel.changeSelectionFont("Cursive"); fontMenuExpanded = false }); customFonts.forEach { font -> DropdownMenuItem(text = { Text(font.name) }, onClick = { haptic.click(); viewModel.changeSelectionFont(font.name); fontMenuExpanded = false }) } } }
                                            }
                                            IconButton(onClick = { viewModel.copySelection() }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy") }
                                            IconButton(onClick = { viewModel.duplicateSelection() }) { Icon(Icons.Filled.ContentPasteGo, contentDescription = "Duplicate") }
                                            IconButton(onClick = { viewModel.deleteSelection() }) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete") }
                                        }
                                        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.selectionMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { haptic.click(); viewModel.selectionMode = 0 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Lasso", color = if (viewModel.selectionMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                                        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.selectionMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { haptic.click(); viewModel.selectionMode = 1 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Rectangle", color = if (viewModel.selectionMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                                    } else if (viewModel.currentTool == DrawingTool.TEXT) {
                                        Box { ExpressiveButton(text = selectedFontName, onClick = { fontMenuExpanded = true }, isSquareEdge = true); DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) { DropdownMenuItem(text = { Text("Default") }, onClick = { haptic.click(); viewModel.currentFontName = "Default"; selectedFontName = "Default"; fontMenuExpanded = false }); DropdownMenuItem(text = { Text("Serif") }, onClick = { haptic.click(); viewModel.currentFontName = "Serif"; selectedFontName = "Serif"; fontMenuExpanded = false }); DropdownMenuItem(text = { Text("Monospace") }, onClick = { haptic.click(); viewModel.currentFontName = "Monospace"; selectedFontName = "Monospace"; fontMenuExpanded = false }); DropdownMenuItem(text = { Text("Cursive") }, onClick = { haptic.click(); viewModel.currentFontName = "Cursive"; selectedFontName = "Cursive"; fontMenuExpanded = false }); customFonts.forEach { font -> DropdownMenuItem(text = { Text(font.name) }, onClick = { haptic.click(); viewModel.currentFontName = font.name; selectedFontName = font.name; fontMenuExpanded = false }) } } }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        ColorSelectionRow(selectedColor = viewModel.currentColor, recentColors = viewModel.recentColors, onColorPickRequest = { showColorPicker = true }, onColorSelected = { viewModel.addToRecentColors(it) }, haptic = haptic)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${viewModel.currentTextSize.toInt()}px", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Slider(value = viewModel.currentTextSize, onValueChange = { if (it.toInt() != viewModel.currentTextSize.toInt()) haptic.tick(); viewModel.currentTextSize = it }, valueRange = 10f..150f, modifier = Modifier.width(100.dp))
                                    }
                                }
                            }
                            IconButton(onClick = { showToolOptions = false }, modifier = Modifier.padding(end = 8.dp)) { Icon(Icons.Filled.Close, contentDescription = null) }
                        }
                    }
                    Row(modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).pointerInput(Unit){}.padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val tools = listOf(DrawingTool.PEN to (Icons.Filled.Edit to "Pen"), DrawingTool.HIGHLIGHTER to (Icons.Filled.BorderColor to "Highlighter"), DrawingTool.ERASER to (Icons.Filled.LayersClear to "Eraser"), DrawingTool.TEXT to (Icons.Filled.Title to "Text"), DrawingTool.SELECTION to (Icons.Filled.HighlightAlt to "Lasso"))
                        tools.forEach { (tool, iconInfo) -> val (icon, label) = iconInfo; val isSelected = viewModel.currentTool == tool; ExpressiveIconButton(icon = icon, contentDescription = label, tooltip = label, onClick = { if (isSelected) showToolOptions = !showToolOptions else { viewModel.setTool(tool); showToolOptions = true } }, containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
            if (viewModel.isCurrentNoteInfinite) { AnimatedVisibility(visible = true, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)) { ExpressiveIconButton(icon = Icons.Default.FilterCenterFocus, contentDescription = "Reset View", tooltip = "Reset View", onClick = { viewModel.cameraResetTrigger++ }, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, size = 56.dp, iconSize = 28.dp) } }
        }
    }

    if (showColorPicker) { ColorPickerDialog(initialColor = viewModel.currentColor, onColorSelected = { viewModel.addToRecentColors(it) }, onDismiss = { showColorPicker = false }) }
    
    if (isTextEditing) {
        AlertDialog(
            onDismissRequest = { isTextEditing = false },
            title = { Text(if (editingTextData == null) "Add Text" else "Edit Text") },
            text = {
                Column {
                    OutlinedTextField(value = currentTextInput, onValueChange = { currentTextInput = it }, label = { Text("Text") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Box {
                        ExpressiveButton(text = selectedFontName, onClick = { fontMenuExpanded = true }, isSquareEdge = true)
                        DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Default") }, onClick = { selectedFontName = "Default"; fontMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Serif") }, onClick = { selectedFontName = "Serif"; fontMenuExpanded = false })
                            customFonts.forEach { font -> DropdownMenuItem(text = { Text(font.name) }, onClick = { selectedFontName = font.name; fontMenuExpanded = false }) }
                        }
                    }
                }
            },
            confirmButton = {
                ExpressiveButton(text = "Save", onClick = {
                    if (editingTextData == null) {
                        viewModel.addTextToPage(textEditPageIndex, TextData(x = textEditVirtualPos.x, y = textEditVirtualPos.y, text = currentTextInput, fontSize = viewModel.currentTextSize, colorArgb = viewModel.currentColor.toArgb(), fontName = selectedFontName))
                    } else {
                        viewModel.updateTextOnPage(textEditPageIndex, editingTextData!!, currentTextInput, selectedFontName)
                    }
                    isTextEditing = false
                })
            },
            dismissButton = { ExpressiveButton(text = "Cancel", onClick = { isTextEditing = false }, containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface) }
        )
    }
}

@Composable
fun ColorSelectionRow(selectedColor: Color, recentColors: List<Color>, onColorPickRequest: () -> Unit, onColorSelected: (Color) -> Unit, haptic: HapticManager) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red))).border(2.dp, Color.White, CircleShape).clickable { haptic.click(); onColorPickRequest() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Palette, contentDescription = "Pick Color", tint = Color.White, modifier = Modifier.size(20.dp)) }
        recentColors.forEach { color -> Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(width = if (selectedColor == color) 2.dp else 1.dp, color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f), shape = CircleShape).clickable { haptic.click(); onColorSelected(color) }) }
    }
}

@Composable
fun ColorPickerDialog(initialColor: Color, onColorSelected: (Color) -> Unit, onDismiss: () -> Unit) {
    var h by remember { mutableFloatStateOf(0f) }
    var s by remember { mutableFloatStateOf(0f) }
    var v by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(initialColor) { val hsv = FloatArray(3); android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv); h = hsv[0]; s = hsv[1]; v = hsv[2] }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Pick Color") }, text = { Column { Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(16.dp)).background(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))))); Spacer(modifier = Modifier.height(16.dp)); Text("Hue: ${h.toInt()}°", fontWeight = FontWeight.Bold); Slider(value = h, onValueChange = { h = it }, valueRange = 0f..360f); Text("Saturation: ${(s * 100).toInt()}%", fontWeight = FontWeight.Bold); Slider(value = s, onValueChange = { s = it }, valueRange = 0f..1f); Text("Value: ${(v * 100).toInt()}%", fontWeight = FontWeight.Bold); Slider(value = v, onValueChange = { v = it }, valueRange = 0f..1f) } }, confirmButton = { ExpressiveButton(text = "Select", onClick = { onColorSelected(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))); onDismiss() }) }, dismissButton = { ExpressiveButton(text = "Cancel", onClick = onDismiss, containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface) })
}
