package com.midknight.pixelnotes.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertPageBreak
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import coil.compose.AsyncImage
import com.midknight.pixelnotes.data.Note
import com.midknight.pixelnotes.data.NoteWithPages
import com.midknight.pixelnotes.data.PageEntity
import com.midknight.pixelnotes.domain.PdfExporter
import com.midknight.pixelnotes.domain.TextData
import com.midknight.pixelnotes.ui.components.AudioPlayerSlider
import com.midknight.pixelnotes.ui.components.DrawingCanvas
import com.midknight.pixelnotes.ui.components.ExpressiveIconButton
import com.midknight.pixelnotes.ui.viewmodels.DrawingTool
import com.midknight.pixelnotes.ui.viewmodels.NotesViewModel
import com.midknight.pixelnotes.domain.HapticManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfPageBackground(pdfPath: String, pageIndex: Int) {
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
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
            1 -> { var y = spacing; while (y < size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y), 2f); y += spacing } }
            2 -> { var y = spacing; while (y < size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y), 2f); y += spacing }; var x = spacing; while (x < size.width) { drawLine(color, Offset(x, 0f), Offset(x, size.height), 2f); x += spacing } }
            3 -> { val radius = 1.5.dp.toPx(); var y = spacing; while (y < size.height) { var x = spacing; while (x < size.width) { drawCircle(color, radius, Offset(x, y)); x += spacing }; y += spacing } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(viewModel: NotesViewModel) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isSmallScreen = screenWidth < 600
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val customFonts by viewModel.customFonts.collectAsState()
    val haptic = remember { HapticManager(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> 
            if (event == Lifecycle.Event.ON_STOP) { 
                if (!viewModel.isNoteBlank() && !viewModel.isSyncing) { 
                    viewModel.saveCurrentNote(SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())) 
                } 
            } 
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) { viewModel.startRecording(context) }
        else { Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show() }
    }

    val colors = listOf(Color.Black, Color.White, Color.Red, Color.Blue, Color.Green, Color(0xFFFBC02D), Color(0xFFE91E63))
    val canvasColors = listOf(-1, Color.Black.toArgb(), Color.DarkGray.toArgb(), Color(0xFFFFF8E7).toArgb(), Color(0xFFE3F2FD).toArgb())

    val pdfLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { coroutineScope.launch { val exporter = PdfExporter(context); val noteToExport = viewModel.selectedNoteWithPages?.copy(note = viewModel.selectedNoteWithPages!!.note.copy(title = viewModel.currentTitle), pages = viewModel.currentPages.toList()) ?: NoteWithPages(note = Note(title = viewModel.currentTitle, content = "", date = "", folder = "General", isInfinite = viewModel.isCurrentNoteInfinite), pages = viewModel.currentPages.toList()); exporter.exportToPdf(listOf(noteToExport), it); Toast.makeText(context, "Exported full Note to PDF", Toast.LENGTH_SHORT).show() } }
    }
    var singlePageToExport by remember { mutableStateOf<PageEntity?>(null) }
    val singlePdfLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { safeUri -> singlePageToExport?.let { page -> coroutineScope.launch { val exporter = PdfExporter(context); exporter.exportSinglePageToPdf(page, safeUri, viewModel.isCurrentNoteInfinite); Toast.makeText(context, "Exported single page to PDF", Toast.LENGTH_SHORT).show(); singlePageToExport = null } } }
    }

    val pdfImportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri -> uri?.let { viewModel.importPdfDocument(context, it) } }

    val pxNoteExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            val noteToExport = viewModel.selectedNoteWithPages?.copy(
                note = viewModel.selectedNoteWithPages!!.note.copy(title = viewModel.currentTitle),
                pages = viewModel.currentPages.toList()
            ) ?: NoteWithPages(
                note = Note(title = viewModel.currentTitle, content = "", date = "", folder = "General", isInfinite = viewModel.isCurrentNoteInfinite),
                pages = viewModel.currentPages.toList()
            )
            viewModel.exportSingleNote(context, noteToExport, it)
        }
    }

    val floatingImagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.addFloatingImageToPage(viewModel.activePageIndex, it.toString())
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = object : ActivityResultContracts.TakePicture() {
            override fun createIntent(context: Context, input: Uri): Intent {
                return super.createIntent(context, input).apply {
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
    ) { success ->
        if (success) { viewModel.pendingCameraUri?.let { uri -> viewModel.addFloatingImageToPage(viewModel.activePageIndex, uri.toString()) } }
    }

    val backgroundPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.updateActivePageBackground(it.toString())
        }
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

    var isTextEditing by remember { mutableStateOf(false) }
    var editingTextData by remember { mutableStateOf<TextData?>(null) }
    var currentTextInput by remember { mutableStateOf("") }
    var selectedFontName by remember { mutableStateOf("Default") }
    var textEditX by remember { mutableFloatStateOf(0f) }
    var textEditY by remember { mutableFloatStateOf(0f) }
    var textEditPageIndex by remember { mutableIntStateOf(0) }

    if (isTextEditing) {
        AlertDialog(
            onDismissRequest = { isTextEditing = false; editingTextData = null },
            title = { Text(if (editingTextData == null) "Add Text" else "Edit Text") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = currentTextInput,
                        onValueChange = { currentTextInput = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                        placeholder = { Text("Type something...") },
                        minLines = 3
                    )
                    
                    Text("Font", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    val allFontNames = listOf("Default", "Serif", "Monospace", "Cursive") + customFonts.map { it.name }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(allFontNames) { fontName ->
                            val isSelected = selectedFontName == fontName
                            val fontInfo = customFonts.find { it.name == fontName }
                            val tf = com.midknight.pixelnotes.domain.TypefaceManager.getTypeface(context, fontName, fontInfo?.fileName)
                            val fontFamily = FontFamily(tf)
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(width = 1.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(12.dp))
                                    .clickable { selectedFontName = fontName }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = fontName,
                                    fontFamily = fontFamily,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (currentTextInput.isNotBlank()) {
                        val existing = editingTextData
                        if (existing != null) {
                            viewModel.updateTextOnPage(textEditPageIndex, existing, currentTextInput, selectedFontName)
                        } else {
                            viewModel.addTextToPage(
                                textEditPageIndex,
                                com.midknight.pixelnotes.domain.TextData(
                                    x = textEditX,
                                    y = textEditY,
                                    text = currentTextInput,
                                    colorArgb = viewModel.currentColor.toArgb(),
                                    fontSize = viewModel.currentTextSize,
                                    fontName = selectedFontName,
                                    maxWidth = 600f
                                )
                            )
                        }
                    }
                    isTextEditing = false
                    editingTextData = null
                }) { Text(if (editingTextData == null) "Place Text" else "Update Text") }
            },
            dismissButton = { TextButton(onClick = { isTextEditing = false; editingTextData = null }) { Text("Cancel") } }
        )
    }
    if (viewModel.isImportingPdf) { AlertDialog(onDismissRequest = {}, title = { Text("Opening Document", fontWeight = FontWeight.Bold) }, text = { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(); Spacer(modifier = Modifier.width(16.dp)); Text("Loading PDF engine...") } }, confirmButton = {}, properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) }

    val listState = rememberLazyListState()
    LaunchedEffect(listState.firstVisibleItemIndex) { viewModel.activePageIndex = listState.firstVisibleItemIndex }

    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceVariant) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
            val maxWidthPx = constraints.maxWidth.toFloat()
            val maxHeightPx = constraints.maxHeight.toFloat()
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 15f)
                val maxX = (maxWidthPx * (scale - 1f)) / 2f
                val maxY = (maxHeightPx * (scale - 1f)) / 2f

                val newOffsetY = offset.y + offsetChange.y
                var overflowY = 0f
                if (newOffsetY > maxY) overflowY = newOffsetY - maxY
                else if (newOffsetY < -maxY) overflowY = newOffsetY + maxY

                if (overflowY != 0f) {
                    coroutineScope.launch { listState.dispatchRawDelta(-overflowY) }
                }

                offset = Offset(
                    (offset.x + offsetChange.x).coerceIn(if (maxX > 0) -maxX else 0f, if (maxX > 0) maxX else 0f),
                    newOffsetY.coerceIn(if (maxY > 0) -maxY else 0f, if (maxY > 0) maxY else 0f)
                )
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
                            onTextToolTap = { x, y -> textEditX = x; textEditY = y; textEditPageIndex = 0; currentTextInput = ""; selectedFontName = viewModel.currentFontName; editingTextData = null; isTextEditing = true },
                            onTextEdit = { textData -> textEditPageIndex = 0; editingTextData = textData; currentTextInput = textData.text; selectedFontName = textData.fontName; isTextEditing = true },
                            onProcessSelection = { viewModel.processSelection(0, it) },
                            onMoveSelection = { dx, dy -> viewModel.moveSelection(dx, dy) },
                            onScaleSelection = { s, px, py -> viewModel.scaleSelection(s, px, py) },
                            onCommitSelection = { viewModel.commitSelection() },
                            onSelectionLongPress = { pos -> contextMenuPos = pos; showContextMenu = true },
                            onCameraChange = { pan, zoom -> viewModel.cameraPan = pan; viewModel.cameraZoom = zoom },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (showContextMenu && (!viewModel.isSelectionEmpty || !viewModel.isClipboardEmpty)) {
                            DropdownMenu(
                                expanded = showContextMenu,
                                onDismissRequest = { showContextMenu = false },
                                offset = with(LocalDensity.current) { androidx.compose.ui.unit.DpOffset(contextMenuPos.x.toDp(), (contextMenuPos.y - maxHeightPx).toDp()) }
                            ) {
                                if (!viewModel.isSelectionEmpty) {
                                    DropdownMenuItem(
                                        text = { Text("Copy") },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                        onClick = { viewModel.copySelection(); showContextMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duplicate") },
                                        leadingIcon = { Icon(Icons.Default.ContentPasteGo, contentDescription = null) },
                                        onClick = { viewModel.duplicateSelection(); showContextMenu = false }
                                    )
                                }
                                if (!viewModel.isClipboardEmpty) {
                                    DropdownMenuItem(
                                        text = { Text("Paste") },
                                        leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                                        onClick = { viewModel.pasteSelection(0, contextMenuPos.x, contextMenuPos.y); showContextMenu = false }
                                    )
                                }
                                if (!viewModel.isSelectionEmpty) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = { viewModel.deleteSelection(); showContextMenu = false }
                                    )
                                }
                            }
                        }

                        page.audioData.forEach { audio ->
                            key(audio.id) {
                                val virtualWidth = 1080f
                                val effectiveScale = (maxWidthPx / virtualWidth) * viewModel.cameraZoom
                                val actualX = audio.x * effectiveScale + viewModel.cameraPan.x
                                val actualY = audio.y * effectiveScale + viewModel.cameraPan.y
                                
                                var audioOffset by remember(audio.id) { mutableStateOf(Offset(actualX, actualY)) }
                                val isActive = viewModel.activeAudioId == audio.id
                                Box(
                                    modifier = Modifier
                                        .offset(x = with(LocalDensity.current) { (actualX).toDp() }, y = with(LocalDensity.current) { (actualY).toDp() })
                                        .size(48.dp).clip(CircleShape)
                                        .background(if (isActive && viewModel.isPlaying) Color.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                        .border(if (isActive) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        .pointerInput(audio.id) {
                                            detectDragGestures(
                                                onDragEnd = { 
                                                    val finalX = (audioOffset.x - viewModel.cameraPan.x) / effectiveScale
                                                    val finalY = (audioOffset.y - viewModel.cameraPan.y) / effectiveScale
                                                    viewModel.updateAudioPosition(0, audio.id, finalX, finalY) 
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                audioOffset += dragAmount
                                            }
                                        }
                                        .pointerInput(audio.id) {
                                            detectTapGestures(
                                                onTap = { 
                                                    haptic.click()
                                                    viewModel.playAudio(audio.uri, audio.id, 0) 
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = if (isActive && viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                val density = LocalDensity.current
                val baseHeightDp = with(density) { (maxHeightPx * 0.95f).toDp() }

                Box(modifier = Modifier.fillMaxSize().transformable(state = transformState), contentAlignment = Alignment.Center) {
                    LazyColumn(
                        state = listState,
                        userScrollEnabled = scale == 1f,
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
                                    onTextToolTap = { x, y -> textEditX = x; textEditY = y; textEditPageIndex = index; currentTextInput = ""; selectedFontName = viewModel.currentFontName; editingTextData = null; isTextEditing = true },
                                    onTextEdit = { textData -> textEditPageIndex = index; editingTextData = textData; currentTextInput = textData.text; selectedFontName = textData.fontName; isTextEditing = true },
                                    onProcessSelection = { viewModel.processSelection(index, it) },
                                    onMoveSelection = { dx, dy -> viewModel.moveSelection(dx, dy) },
                                    onScaleSelection = { s, px, py -> viewModel.scaleSelection(s, px, py) },
                                    onCommitSelection = { viewModel.commitSelection() },
                                    onSelectionLongPress = { pos -> contextMenuPos = pos; showContextMenu = true },
                                    onCameraChange = { pan, zoom -> viewModel.cameraPan = pan; viewModel.cameraZoom = zoom },
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (showContextMenu && viewModel.activePageIndex == index && (!viewModel.isSelectionEmpty || !viewModel.isClipboardEmpty)) {
                                    DropdownMenu(
                                        expanded = showContextMenu,
                                        onDismissRequest = { showContextMenu = false },
                                        offset = with(LocalDensity.current) { androidx.compose.ui.unit.DpOffset(contextMenuPos.x.toDp(), (contextMenuPos.y - (maxHeightPx * 0.95f)).toDp()) }
                                    ) {
                                        if (!viewModel.isSelectionEmpty) {
                                            DropdownMenuItem(
                                                text = { Text("Copy") },
                                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                                onClick = { viewModel.copySelection(); showContextMenu = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Duplicate") },
                                                leadingIcon = { Icon(Icons.Default.ContentPasteGo, contentDescription = null) },
                                                onClick = { viewModel.duplicateSelection(); showContextMenu = false }
                                            )
                                        }
                                        if (!viewModel.isClipboardEmpty) {
                                            DropdownMenuItem(
                                                text = { Text("Paste") },
                                                leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                                                onClick = { viewModel.pasteSelection(index, contextMenuPos.x, contextMenuPos.y); showContextMenu = false }
                                            )
                                        }
                                        if (!viewModel.isSelectionEmpty) {
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                                onClick = { viewModel.deleteSelection(); showContextMenu = false }
                                            )
                                        }
                                    }
                                }

                                page.audioData.forEach { audio ->
                                    key(audio.id) {
                                        val virtualWidth = 1080f
                                        val pageActualWidthPx = (maxHeightPx * 0.95f) / 1.414f
                                        val pageScale = pageActualWidthPx / virtualWidth
                                        
                                        var audioOffset by remember(audio.id) { mutableStateOf(Offset(audio.x * pageScale, audio.y * pageScale)) }
                                        val isActive = viewModel.activeAudioId == audio.id
                                        Box(
                                            modifier = Modifier
                                                .offset(x = with(LocalDensity.current) { (audio.x * pageScale).toDp() }, y = with(LocalDensity.current) { (audio.y * pageScale).toDp() })
                                                .size(48.dp).clip(CircleShape)
                                                .background(if (isActive && viewModel.isPlaying) Color.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                                .border(if (isActive) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                .pointerInput(audio.id) {
                                                    detectDragGestures(
                                                        onDragEnd = { 
                                                            viewModel.updateAudioPosition(index, audio.id, audioOffset.x / pageScale, audioOffset.y / pageScale) 
                                                        }
                                                    ) { change, dragAmount ->
                                                        change.consume()
                                                        audioOffset += dragAmount
                                                    }
                                                }
                                                .pointerInput(audio.id) {
                                                    detectTapGestures(
                                                        onTap = { 
                                                            haptic.click()
                                                            viewModel.playAudio(audio.uri, audio.id, index) 
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = if (isActive && viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(visible = showPagesPanel, enter = slideInHorizontally(initialOffsetX = { -it }), exit = slideOutHorizontally(targetOffsetX = { -it }), modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp, top = 80.dp, bottom = 80.dp).zIndex(10f)) {
                    Column(modifier = Modifier.width(90.dp).fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)).pointerInput(Unit){}.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp)).padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        var draggedItem by remember { mutableStateOf<Int?>(null) }; var dragOffset by remember { mutableFloatStateOf(0f) }
                        LazyColumn(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            items(viewModel.currentPages.size) { index ->
                                val isSelected = index == viewModel.activePageIndex; val yOffset = if (draggedItem == index) dragOffset else 0f; var showPageMenu by remember { mutableStateOf(false) }
                                Box(modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .zIndex(if (draggedItem == index) 1f else 0f)
                                    .graphicsLayer(translationY = yOffset)
                                    .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { draggedItem = index; dragOffset = 0f },
                                            onDragEnd = {
                                                val targetIndex = (index + (dragOffset / 200f).toInt()).coerceIn(0, viewModel.currentPages.size - 1)
                                                viewModel.movePage(index, targetIndex)
                                                draggedItem = null
                                                dragOffset = 0f
                                            },
                                            onDrag = { _, dragAmount -> dragOffset += dragAmount.y }
                                        )
                                    }
                                    .clickable {
                                        if (viewModel.selectionPageIndex != -1 && viewModel.selectionPageIndex != index) {
                                            viewModel.dropSelectionToPage(index)
                                            coroutineScope.launch { listState.animateScrollToItem(index) }
                                        } else {
                                            coroutineScope.launch { listState.animateScrollToItem(index) }
                                        }
                                    }
                                ) {
                                    Box(modifier = Modifier.size(width = 60.dp, height = 80.dp).clip(RoundedCornerShape(8.dp)).background(if (viewModel.currentPages[index].canvasColor == -1) Color.White else Color(viewModel.currentPages[index].canvasColor)).border(width = if (isSelected) 3.dp else 1.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(8.dp)).clickable { coroutineScope.launch { listState.animateScrollToItem(index) } }, contentAlignment = Alignment.Center) { PaperTemplate(style = viewModel.currentPages[index].paperStyle, modifier = Modifier.fillMaxSize()); Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                                        IconButton(onClick = { showPageMenu = true }, modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surface.copy(alpha=0.8f), CircleShape)) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu", modifier = Modifier.size(16.dp)) }
                                        DropdownMenu(expanded = showPageMenu, onDismissRequest = { showPageMenu = false }) { DropdownMenuItem(text = { Text("Export as PDF") }, onClick = { showPageMenu = false; singlePageToExport = viewModel.currentPages[index]; singlePdfLauncher.launch("${viewModel.currentTitle.replace(" ", "_")}_Page_${index + 1}.pdf") }); if (viewModel.currentPages.size > 1) { DropdownMenuItem(text = { Text("Delete Page", color = MaterialTheme.colorScheme.error) }, onClick = { showPageMenu = false; viewModel.deletePageAt(index) }) } }
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.addNewPage(); coroutineScope.launch { listState.animateScrollToItem(viewModel.currentPages.lastIndex) } }, modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)) { Icon(Icons.Filled.Add, contentDescription = "Add Page", tint = MaterialTheme.colorScheme.onPrimary) }
                    }
                }
            }

            AnimatedVisibility(
                visible = viewModel.isPlaying || viewModel.isRecording,
                enter = expandVertically(), exit = shrinkVertically(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp).zIndex(20f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (viewModel.isRecording) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Red)
                            val s = (viewModel.recordingDuration / 1000) % 60
                            val m = (viewModel.recordingDuration / (1000 * 60)) % 60
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", m, s),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.stopRecording() }) { Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.Red) }
                            IconButton(onClick = { viewModel.cancelRecording() }) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                        }
                    }
else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconButton(onClick = { 
                                haptic.click()
                                if (viewModel.isPlaying) viewModel.pauseAudio() else viewModel.resumeAudio() 
                            }) {
                                Icon(if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                AudioPlayerSlider(
                                    progress = viewModel.playbackProgress,
                                    isPlaying = viewModel.isPlaying,
                                    onProgressChange = { viewModel.seekAudio(it) }
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    val currentS = (viewModel.playbackProgress * viewModel.currentAudioDuration / 1000).toLong() % 60
                                    val currentM = (viewModel.playbackProgress * viewModel.currentAudioDuration / (1000 * 60)).toLong() % 60
                                    val totalS = (viewModel.currentAudioDuration / 1000) % 60
                                    val totalM = (viewModel.currentAudioDuration / (1000 * 60)) % 60
                                    Text(String.format(Locale.getDefault(), "%02d:%02d", currentM, currentS), style = MaterialTheme.typography.labelSmall)
                                    Text(String.format(Locale.getDefault(), "%02d:%02d", totalM, totalS), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            IconButton(onClick = { 
                                haptic.click()
                                viewModel.deleteAudioNote(viewModel.activeAudioPageIndex, viewModel.activeAudioId ?: "") 
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { 
                                haptic.click()
                                viewModel.stopAudio() 
                            }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                        }
                    }
                }
            }

            Row(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 16.dp, end = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).pointerInput(Unit){}.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExpressiveIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = { viewModel.navigateBack() }
                    )
                    TextField(value = viewModel.currentTitle, onValueChange = { viewModel.currentTitle = it }, modifier = Modifier.width(if (isSmallScreen) 100.dp else 150.dp), singleLine = true, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                }

                Spacer(modifier = Modifier.width(8.dp))

                val topBarScrollState = rememberScrollState()
                
                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .widthIn(max = if (isSmallScreen) screenWidth.dp - 150.dp else screenWidth.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .pointerInput(Unit) {}
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            val leftStrength = (topBarScrollState.value.toFloat() / 80f).coerceIn(0f, 1f)
                            val rightStrength = ((topBarScrollState.maxValue - topBarScrollState.value).toFloat() / 80f).coerceIn(0f, 1f)

                            // Continuous Alpha Mask for Left Edge
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0f to Color.Black.copy(alpha = 1f - leftStrength),
                                    0.25f to Color.Black,
                                    startX = 0f, endX = size.width
                                ),
                                blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                            )
                            // Continuous Alpha Mask for Right Edge
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0.75f to Color.Black,
                                    1f to Color.Black.copy(alpha = 1f - rightStrength),
                                    startX = 0f, endX = size.width
                                ),
                                blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(topBarScrollState).padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!viewModel.isCurrentNoteInfinite) {
                            ExpressiveIconButton(
                                icon = Icons.Filled.Layers,
                                contentDescription = "Pages",
                                onClick = { showPagesPanel = !showPagesPanel },
                                contentColor = if (showPagesPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            ExpressiveIconButton(
                                icon = if (showBottomBar) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle Toolbar",
                                onClick = { showBottomBar = !showBottomBar },
                                contentColor = if (showBottomBar) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            ExpressiveIconButton(
                                icon = if (showBottomBar) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle Toolbar",
                                onClick = { showBottomBar = !showBottomBar },
                                contentColor = if (showBottomBar) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        ExpressiveIconButton(
                            icon = if (viewModel.fingerDrawingEnabled) Icons.Filled.TouchApp else Icons.Filled.PanTool,
                            contentDescription = "Toggle Finger/Stylus",
                            onClick = { viewModel.fingerDrawingEnabled = !viewModel.fingerDrawingEnabled },
                            contentColor = if (viewModel.fingerDrawingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )

                        Box {
                            val paperIcon = when (if (viewModel.currentPages.isNotEmpty()) viewModel.currentPages[viewModel.activePageIndex].paperStyle else 0) {
                                1 -> Icons.Filled.ViewHeadline
                                2 -> Icons.Filled.GridOn
                                3 -> Icons.Filled.Grain
                                else -> Icons.Filled.CheckBoxOutlineBlank
                            }
                            ExpressiveIconButton(
                                icon = paperIcon,
                                contentDescription = "Paper Style",
                                onClick = { showPaperMenu = true }
                            )
                            DropdownMenu(expanded = showPaperMenu, onDismissRequest = { showPaperMenu = false }) {
                                DropdownMenuItem(text = { Text("Blank", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.CheckBoxOutlineBlank, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.updateActivePagePaperStyle(0); showPaperMenu = false })
                                DropdownMenuItem(text = { Text("Lined", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.ViewHeadline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.updateActivePagePaperStyle(1); showPaperMenu = false })
                                DropdownMenuItem(text = { Text("Grid", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.updateActivePagePaperStyle(2); showPaperMenu = false })
                                DropdownMenuItem(text = { Text("Dotted", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.Grain, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { viewModel.updateActivePagePaperStyle(3); showPaperMenu = false })
                            }
                        }

                        Box {
                            ExpressiveIconButton(
                                icon = Icons.Filled.FormatColorFill,
                                contentDescription = "Canvas Color",
                                onClick = { showCanvasColorMenu = true }
                            )
                            DropdownMenu(expanded = showCanvasColorMenu, onDismissRequest = { showCanvasColorMenu = false }) {
                                canvasColors.forEach { colorArgb -> DropdownMenuItem(text = { Text(if (colorArgb == -1) "Default White" else "Solid Color", fontWeight = FontWeight.Bold) }, trailingIcon = { Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(if (colorArgb == -1) Color.White else Color(colorArgb)).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)) }, onClick = { viewModel.updateActivePageCanvasColor(colorArgb); showCanvasColorMenu = false }) }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(text = { Text("Set Page Background", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.Wallpaper, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showCanvasColorMenu = false; backgroundPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                                DropdownMenuItem(text = { Text("Clear Background", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.LayersClear, contentDescription = null, tint = MaterialTheme.colorScheme.error) }, onClick = { showCanvasColorMenu = false; viewModel.updateActivePageBackground(null) })
                            }
                        }

                        Box {
                            ExpressiveIconButton(
                                icon = Icons.Filled.Add,
                                contentDescription = "Add Content",
                                onClick = { showAddMenu = true }
                            )
                            DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                                DropdownMenuItem(text = { Text("Import PDF", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showAddMenu = false; pdfImportLauncher.launch("application/pdf") })
                                DropdownMenuItem(text = { Text("Insert Image", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showAddMenu = false; floatingImagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                                DropdownMenuItem(text = { Text("Take Photo", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showAddMenu = false; viewModel.pendingCameraUri = viewModel.createImageUri(context); viewModel.pendingCameraUri?.let { cameraLauncher.launch(it) } })
                                DropdownMenuItem(text = { Text("Record Audio", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showAddMenu = false; micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) })
                            }
                        }

                        ExpressiveIconButton(
                            icon = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            onClick = { viewModel.undo() }
                        )
                        ExpressiveIconButton(
                            icon = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            onClick = { viewModel.redo() }
                        )

                        Box {
                            ExpressiveIconButton(
                                icon = Icons.Filled.IosShare,
                                contentDescription = "Share",
                                onClick = { showExportMenu = true }
                            )
                            DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                                DropdownMenuItem(text = { Text("Export as PDF", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showExportMenu = false; val currentDate = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date()); pdfLauncher.launch("${viewModel.currentTitle.replace(" ", "_")}_$currentDate.pdf") })
                                DropdownMenuItem(text = { Text("Export as Pixel Note", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showExportMenu = false; pxNoteExportLauncher.launch("${viewModel.currentTitle}.pxnote") })
                                if (!viewModel.isCurrentNoteInfinite) { DropdownMenuItem(text = { Text("Export Current Page", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.InsertPageBreak, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showExportMenu = false; singlePageToExport = viewModel.currentPages[viewModel.activePageIndex]; singlePdfLauncher.launch("${viewModel.currentTitle.replace(" ", "_")}_Page_${viewModel.activePageIndex + 1}.pdf") }) }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showBottomBar,
                enter = expandVertically(expandFrom = Alignment.Bottom) + slideInVertically(initialOffsetY = { it }),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).zIndex(5f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(visible = showToolOptions, enter = expandVertically(), exit = shrinkVertically()) {
                        val optionsScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .widthIn(max = screenWidth.dp - 32.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                .pointerInput(Unit) {}
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Masked Area for scrollable items
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                    .drawWithContent {
                                        drawContent()
                                        val leftStrength = (optionsScrollState.value.toFloat() / 80f).coerceIn(0f, 1f)
                                        val rightStrength = ((optionsScrollState.maxValue - optionsScrollState.value).toFloat() / 80f).coerceIn(0f, 1f)

                                        // Continuous Alpha Mask for Left Edge
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                0f to Color.Black.copy(alpha = 1f - leftStrength),
                                                0.25f to Color.Black,
                                                startX = 0f, endX = size.width
                                            ),
                                            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                                        )
                                        // Continuous Alpha Mask for Right Edge
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                0.75f to Color.Black,
                                                1f to Color.Black.copy(alpha = 1f - rightStrength),
                                                startX = 0f, endX = size.width
                                            ),
                                            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(optionsScrollState)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically, 
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (viewModel.currentTool == DrawingTool.PEN || viewModel.currentTool == DrawingTool.HIGHLIGHTER) {
                                        colors.forEach { color ->
                                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(width = if (viewModel.currentColor == color) 2.dp else 1.dp, color = if (viewModel.currentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, shape = CircleShape).clickable { viewModel.currentColor = color })
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${(viewModel.currentStrokeWidth / 40f * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Slider(
                                            value = viewModel.currentStrokeWidth, 
                                            onValueChange = { 
                                                if (it.toInt() != viewModel.currentStrokeWidth.toInt()) haptic.tick()
                                                viewModel.currentStrokeWidth = it 
                                            }, 
                                            valueRange = 4f..40f, 
                                            modifier = Modifier.width(100.dp)
                                        )
                                    }
                                    else if (viewModel.currentTool == DrawingTool.ERASER) {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.eraserType == 0) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { haptic.click(); viewModel.eraserType = 0 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Normal", color = if (viewModel.eraserType == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                                        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.eraserType == 1) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { haptic.click(); viewModel.eraserType = 1 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Stroke", color = if (viewModel.eraserType == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                                        if (viewModel.eraserType == 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("${(viewModel.currentEraserWidth / 100f * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Slider(
                                                value = viewModel.currentEraserWidth, 
                                                onValueChange = { 
                                                    if (it.toInt() != viewModel.currentEraserWidth.toInt()) haptic.tick()
                                                    viewModel.currentEraserWidth = it 
                                                }, 
                                                valueRange = 10f..100f, 
                                                modifier = Modifier.width(100.dp)
                                            )
                                        }
                                    }
                                    else if (viewModel.currentTool == DrawingTool.SELECTION) {
                                        if (viewModel.selectedStrokes.isNotEmpty() || viewModel.selectedTexts.isNotEmpty()) {
                                            colors.forEach { color ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                        .border(width = 1.dp, color = Color.Transparent, shape = CircleShape)
                                                        .clickable { viewModel.changeSelectionColor(color.toArgb()) }
                                                )
                                            }
                                            
                                            if (viewModel.selectedTexts.isNotEmpty()) {
                                                IconButton(onClick = { 
                                                    val textToEdit = viewModel.selectedTexts.first()
                                                    textEditPageIndex = viewModel.selectionPageIndex
                                                    editingTextData = textToEdit
                                                    currentTextInput = textToEdit.text
                                                    selectedFontName = textToEdit.fontName
                                                    isTextEditing = true
                                                }) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Selected Text")
                                                }

                                                var showFontMenu by remember { mutableStateOf(false) }
                                                Box {
                                                    IconButton(onClick = { showFontMenu = true }) {
                                                        Icon(Icons.Filled.Title, contentDescription = "Change Selection Font")
                                                    }
                                                    DropdownMenu(expanded = showFontMenu, onDismissRequest = { showFontMenu = false }) {
                                                        val allFontNames = listOf("Default", "Serif", "Monospace", "Cursive") + customFonts.map { it.name }
                                                        allFontNames.forEach { fontName ->
                                                            DropdownMenuItem(
                                                                text = { 
                                                                    val fontInfo = customFonts.find { it.name == fontName }
                                                                    val tf = com.midknight.pixelnotes.domain.TypefaceManager.getTypeface(context, fontName, fontInfo?.fileName)
                                                                    Text(fontName, fontFamily = FontFamily(tf)) 
                                                                },
                                                                onClick = {
                                                                    viewModel.changeSelectionFont(fontName)
                                                                    showFontMenu = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            IconButton(onClick = { viewModel.deleteSelection() }) {
                                                Icon(Icons.Filled.DeleteSweep, contentDescription = "Delete Selection", tint = MaterialTheme.colorScheme.error)
                                            }
                                        } else {
                                            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.selectionMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.selectionMode = 0 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Free Form", color = if (viewModel.selectionMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                                            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.selectionMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.selectionMode = 1 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Rectangle", color = if (viewModel.selectionMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                    else if (viewModel.currentTool == DrawingTool.TEXT) {
                                        var fontMenuExpanded by remember { mutableStateOf(false) }
                                        Box { 
                                            Text(text = viewModel.currentFontName, modifier = Modifier.clickable { haptic.click(); fontMenuExpanded = true }.padding(8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) { 
                                                DropdownMenuItem(text = { Text("Default") }, onClick = { haptic.click(); viewModel.currentFontName = "Default"; fontMenuExpanded = false })
                                                DropdownMenuItem(text = { Text("Serif") }, onClick = { haptic.click(); viewModel.currentFontName = "Serif"; fontMenuExpanded = false })
                                                DropdownMenuItem(text = { Text("Monospace") }, onClick = { haptic.click(); viewModel.currentFontName = "Monospace"; fontMenuExpanded = false })
                                                DropdownMenuItem(text = { Text("Cursive") }, onClick = { haptic.click(); viewModel.currentFontName = "Cursive"; fontMenuExpanded = false })
                                                customFonts.forEach { font -> DropdownMenuItem(text = { Text(font.name) }, onClick = { haptic.click(); viewModel.currentFontName = font.name; fontMenuExpanded = false }) }
                                                DropdownMenuItem(text = { Text("+ Manage Fonts", color = MaterialTheme.colorScheme.primary) }, onClick = { haptic.click(); fontMenuExpanded = false; viewModel.closeEditing(); viewModel.currentScreen = 2 }) 
                                            } 
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        colors.forEach { color -> Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(width = if (viewModel.currentColor == color) 2.dp else 1.dp, color = if (viewModel.currentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, shape = CircleShape).clickable { haptic.click(); viewModel.currentColor = color }) }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${viewModel.currentTextSize.toInt()}px", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Slider(
                                            value = viewModel.currentTextSize, 
                                            onValueChange = { 
                                                if (it.toInt() != viewModel.currentTextSize.toInt()) haptic.tick()
                                                viewModel.currentTextSize = it 
                                            }, 
                                            valueRange = 10f..150f, 
                                            modifier = Modifier.width(100.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { showToolOptions = false }, modifier = Modifier.padding(end = 8.dp)) { Icon(Icons.Filled.Close, contentDescription = null) }
                        }
                    }
                    Row(modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).pointerInput(Unit){}.padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val tools = listOf(DrawingTool.PEN to Icons.Filled.Edit, DrawingTool.HIGHLIGHTER to Icons.Filled.BorderColor, DrawingTool.ERASER to Icons.Filled.LayersClear, DrawingTool.TEXT to Icons.Filled.Title, DrawingTool.SELECTION to Icons.Filled.HighlightAlt)
                        tools.forEach { (tool, icon) -> 
                            val isSelected = viewModel.currentTool == tool
                            ExpressiveIconButton(
                                icon = icon,
                                contentDescription = null,
                                onClick = { if (isSelected) showToolOptions = !showToolOptions else { viewModel.setTool(tool); showToolOptions = true } },
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (viewModel.isCurrentNoteInfinite) {
                AnimatedVisibility(visible = true, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)) {
                    ExpressiveIconButton(
                        icon = Icons.Default.FilterCenterFocus,
                        contentDescription = "Reset View",
                        onClick = { viewModel.cameraResetTrigger++ },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 56.dp,
                        iconSize = 28.dp
                    )
                }
            } else {
                AnimatedVisibility(visible = scale != 1f || offset != Offset.Zero, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)) {
                    ExpressiveIconButton(
                        icon = Icons.Default.FilterCenterFocus,
                        contentDescription = "Reset View",
                        onClick = { scale = 1f; offset = Offset.Zero },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 56.dp,
                        iconSize = 28.dp
                    )
                }
            }
        }
    }
}
