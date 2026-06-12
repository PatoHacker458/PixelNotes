package com.midknight.pixelnotes.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.midknight.pixelnotes.ui.components.DrawingCanvas
import com.midknight.pixelnotes.ui.components.ExpressiveIconButton
import com.midknight.pixelnotes.ui.viewmodels.DrawingTool
import com.midknight.pixelnotes.ui.viewmodels.NotesViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val customFonts by viewModel.customFonts.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) { if (!viewModel.isNoteBlank()) { viewModel.saveCurrentNote(SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())) } } }
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

    val floatingImagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.addFloatingImageToPage(viewModel.activePageIndex, it.toString())
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
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

    var isTextEditing by remember { mutableStateOf(false) }
    var currentTextInput by remember { mutableStateOf("") }
    var textEditX by remember { mutableFloatStateOf(0f) }
    var textEditY by remember { mutableFloatStateOf(0f) }
    var textEditPageIndex by remember { mutableIntStateOf(0) }

    if (isTextEditing) { AlertDialog(onDismissRequest = { isTextEditing = false }, title = { Text("Add Text") }, text = { OutlinedTextField(value = currentTextInput, onValueChange = { currentTextInput = it }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp), placeholder = { Text("Type something...") }) }, confirmButton = { TextButton(onClick = { if (currentTextInput.isNotBlank()) { viewModel.addTextToPage(textEditPageIndex, TextData(x = textEditX, y = textEditY, text = currentTextInput, colorArgb = viewModel.currentColor.toArgb(), fontSize = viewModel.currentTextSize, fontName = viewModel.currentFontName)) }; isTextEditing = false }) { Text("Place Text") } }, dismissButton = { TextButton(onClick = { isTextEditing = false }) { Text("Cancel") } }) }
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
                            strokes = page.drawingData,
                            selectedStrokes = if (viewModel.selectionPageIndex == 0) viewModel.selectedStrokes else emptyList(),
                            texts = page.textData,
                            selectedTexts = if (viewModel.selectionPageIndex == 0) viewModel.selectedTexts else emptyList(),
                            images = page.imageData,
                            selectedImages = if (viewModel.selectionPageIndex == 0) viewModel.selectedImages else emptyList(),
                            customFonts = customFonts,
                            isSelectionActiveOnPage = viewModel.selectionPageIndex == 0,
                            selectionMode = viewModel.selectionMode,
                            currentColor = viewModel.currentColor,
                            currentStrokeWidth = if (viewModel.currentTool == DrawingTool.ERASER) viewModel.currentEraserWidth else viewModel.currentStrokeWidth,
                            currentTool = viewModel.currentTool,
                            eraserType = viewModel.eraserType,
                            fingerDrawingEnabled = viewModel.fingerDrawingEnabled,
                            onStrokeAdd = { viewModel.addStrokeToPage(0, it) },
                            onStrokeRemove = { viewModel.removeStrokeFromPage(0, it) },
                            onTextToolTap = { x, y -> textEditX = x; textEditY = y; textEditPageIndex = 0; currentTextInput = ""; isTextEditing = true },
                            onProcessSelection = { viewModel.processSelection(0, it) },
                            onMoveSelection = { dx, dy -> viewModel.moveSelection(dx, dy) },
                            onScaleSelection = { s, px, py -> viewModel.scaleSelection(s, px, py) },
                            onCommitSelection = { viewModel.commitSelection() },
                            modifier = Modifier.fillMaxSize()
                        )

                        page.audioData.forEach { audio ->
                            Box(
                                modifier = Modifier
                                    .offset(x = with(LocalDensity.current) { (audio.x).toDp() }, y = with(LocalDensity.current) { (audio.y).toDp() })
                                    .size(48.dp).clip(CircleShape)
                                    .background(if (viewModel.activeAudioUri == audio.uri && viewModel.isPlaying) Color.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                    .clickable { viewModel.playAudio(audio.uri) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = if (viewModel.activeAudioUri == audio.uri && viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
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
                                    strokes = page.drawingData,
                                    selectedStrokes = if (viewModel.selectionPageIndex == index) viewModel.selectedStrokes else emptyList(),
                                    texts = page.textData,
                                    selectedTexts = if (viewModel.selectionPageIndex == index) viewModel.selectedTexts else emptyList(),
                                    images = page.imageData,
                                    selectedImages = if (viewModel.selectionPageIndex == index) viewModel.selectedImages else emptyList(),
                                    customFonts = customFonts,
                                    isSelectionActiveOnPage = viewModel.selectionPageIndex == index,
                                    selectionMode = viewModel.selectionMode,
                                    currentColor = viewModel.currentColor,
                                    currentStrokeWidth = if (viewModel.currentTool == DrawingTool.ERASER) viewModel.currentEraserWidth else viewModel.currentStrokeWidth,
                                    currentTool = viewModel.currentTool,
                                    eraserType = viewModel.eraserType,
                                    fingerDrawingEnabled = viewModel.fingerDrawingEnabled,
                                    onStrokeAdd = { viewModel.addStrokeToPage(index, it) },
                                    onStrokeRemove = { viewModel.removeStrokeFromPage(index, it) },
                                    onTextToolTap = { x, y -> textEditX = x; textEditY = y; textEditPageIndex = index; currentTextInput = ""; isTextEditing = true },
                                    onProcessSelection = { viewModel.processSelection(index, it) },
                                    onMoveSelection = { dx, dy -> viewModel.moveSelection(dx, dy) },
                                    onScaleSelection = { s, px, py -> viewModel.scaleSelection(s, px, py) },
                                    onCommitSelection = { viewModel.commitSelection() },
                                    modifier = Modifier.fillMaxSize()
                                )

                                page.audioData.forEach { audio ->
                                    Box(
                                        modifier = Modifier
                                            .offset(x = with(LocalDensity.current) { (audio.x).toDp() }, y = with(LocalDensity.current) { (audio.y).toDp() })
                                            .size(48.dp).clip(CircleShape)
                                            .background(if (viewModel.activeAudioUri == audio.uri && viewModel.isPlaying) Color.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                            .clickable { viewModel.playAudio(audio.uri) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = if (viewModel.activeAudioUri == audio.uri && viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
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
                                Box(modifier = Modifier.padding(vertical = 8.dp).zIndex(if (draggedItem == index) 1f else 0f).graphicsLayer(translationY = yOffset).pointerInput(Unit) { detectDragGesturesAfterLongPress(onDragStart = { draggedItem = index; dragOffset = 0f }, onDragEnd = { val targetIndex = (index + (dragOffset / 200f).toInt()).coerceIn(0, viewModel.currentPages.size - 1); viewModel.movePage(index, targetIndex); draggedItem = null; dragOffset = 0f }, onDrag = { _, dragAmount -> dragOffset += dragAmount.y }) }) {
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
                visible = viewModel.isRecording || viewModel.isPlaying,
                enter = expandVertically(), exit = shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp).zIndex(15f)
            ) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = if (viewModel.isRecording) Icons.Default.Mic else Icons.Default.PlayArrow, contentDescription = null, tint = if (viewModel.isRecording) Color.Red else MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(text = if (viewModel.isRecording) { val s = (viewModel.recordingDuration / 1000) % 60; val m = (viewModel.recordingDuration / (1000 * 60)) % 60; String.format("%02d:%02d", m, s) } else "Playing...", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    if (viewModel.isRecording) {
                        IconButton(onClick = { viewModel.stopRecording() }) { Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.Red) }
                        IconButton(onClick = { viewModel.cancelRecording() }) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                    } else {
                        IconButton(onClick = { viewModel.stopAudio() }) { Icon(Icons.Default.Stop, contentDescription = "Stop") }
                    }
                }
            }

            Row(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 16.dp, end = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).pointerInput(Unit){}.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExpressiveIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = { viewModel.closeEditing() }
                    )
                    TextField(value = viewModel.currentTitle, onValueChange = { viewModel.currentTitle = it }, modifier = Modifier.width(150.dp), singleLine = true, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                }

                Row(modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).pointerInput(Unit){}.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!viewModel.isCurrentNoteInfinite) {
                        ExpressiveIconButton(
                            icon = Icons.Filled.Layers,
                            contentDescription = "Pages",
                            onClick = { showPagesPanel = !showPagesPanel },
                            contentColor = if (showPagesPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                        ExpressiveIconButton(
                            icon = Icons.Filled.GridOn,
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
                        icon = Icons.Filled.Undo,
                        contentDescription = "Undo",
                        onClick = { viewModel.undo() }
                    )
                    ExpressiveIconButton(
                        icon = Icons.Filled.Redo,
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
                            DropdownMenuItem(text = { Text("Export Note as PDF", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showExportMenu = false; val currentDate = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date()); pdfLauncher.launch("${viewModel.currentTitle.replace(" ", "_")}_$currentDate.pdf") })
                            if (!viewModel.isCurrentNoteInfinite) { DropdownMenuItem(text = { Text("Export Current Page", fontWeight = FontWeight.Bold) }, trailingIcon = { Icon(Icons.Filled.InsertPageBreak, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, onClick = { showExportMenu = false; singlePageToExport = viewModel.currentPages[viewModel.activePageIndex]; singlePdfLauncher.launch("${viewModel.currentTitle.replace(" ", "_")}_Page_${viewModel.activePageIndex + 1}.pdf") }) }
                        }
                    }
                }
            }

            Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(visible = showToolOptions, enter = expandVertically(), exit = shrinkVertically()) {
                    Row(modifier = Modifier.padding(bottom = 16.dp).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)).pointerInput(Unit){}.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (viewModel.currentTool == DrawingTool.PEN || viewModel.currentTool == DrawingTool.HIGHLIGHTER) { colors.forEach { color -> Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(width = if (viewModel.currentColor == color) 2.dp else 1.dp, color = if (viewModel.currentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, shape = CircleShape).clickable { viewModel.currentColor = color }) }; Spacer(modifier = Modifier.width(8.dp)); Text("${(viewModel.currentStrokeWidth / 40f * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Slider(value = viewModel.currentStrokeWidth, onValueChange = { viewModel.currentStrokeWidth = it }, valueRange = 4f..40f, modifier = Modifier.width(100.dp)); IconButton(onClick = { showToolOptions = false }) { Icon(Icons.Filled.Close, contentDescription = null) } }
                        else if (viewModel.currentTool == DrawingTool.ERASER) { Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.eraserType == 0) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.eraserType = 0 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Normal", color = if (viewModel.eraserType == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }; Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.eraserType == 1) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.eraserType = 1 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Stroke", color = if (viewModel.eraserType == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }; if (viewModel.eraserType == 0) { Spacer(modifier = Modifier.width(8.dp)); Text("${(viewModel.currentEraserWidth / 100f * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Slider(value = viewModel.currentEraserWidth, onValueChange = { viewModel.currentEraserWidth = it }, valueRange = 10f..100f, modifier = Modifier.width(100.dp)) }; IconButton(onClick = { showToolOptions = false }) { Icon(Icons.Filled.Close, contentDescription = null) } }
                        else if (viewModel.currentTool == DrawingTool.SELECTION) { if (viewModel.selectedStrokes.isNotEmpty() || viewModel.selectedTexts.isNotEmpty()) { colors.forEach { color -> Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(width = 1.dp, color = Color.Transparent, shape = CircleShape).clickable { viewModel.changeSelectionColor(color.toArgb()) }) }; Spacer(modifier = Modifier.width(8.dp)); IconButton(onClick = { viewModel.deleteSelection() }) { Icon(Icons.Filled.DeleteSweep, contentDescription = "Delete Selection", tint = MaterialTheme.colorScheme.error) } } else { Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.selectionMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.selectionMode = 0 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Free Form", color = if (viewModel.selectionMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }; Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.selectionMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.selectionMode = 1 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Rectangle", color = if (viewModel.selectionMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) } }; IconButton(onClick = { showToolOptions = false }) { Icon(Icons.Filled.Close, contentDescription = null) } }
                        else if (viewModel.currentTool == DrawingTool.TEXT) { var fontMenuExpanded by remember { mutableStateOf(false) }; Box { Text(text = viewModel.currentFontName, modifier = Modifier.clickable { fontMenuExpanded = true }.padding(8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) { DropdownMenuItem(text = { Text("Default") }, onClick = { viewModel.currentFontName = "Default"; fontMenuExpanded = false }); DropdownMenuItem(text = { Text("Serif") }, onClick = { viewModel.currentFontName = "Serif"; fontMenuExpanded = false }); DropdownMenuItem(text = { Text("Monospace") }, onClick = { viewModel.currentFontName = "Monospace"; fontMenuExpanded = false }); DropdownMenuItem(text = { Text("Cursive") }, onClick = { viewModel.currentFontName = "Cursive"; fontMenuExpanded = false }); customFonts.forEach { font -> DropdownMenuItem(text = { Text(font.name) }, onClick = { viewModel.currentFontName = font.name; fontMenuExpanded = false }) }; DropdownMenuItem(text = { Text("+ Manage Fonts", color = MaterialTheme.colorScheme.primary) }, onClick = { fontMenuExpanded = false; viewModel.closeEditing(); viewModel.currentScreen = 2 }) } }; Spacer(modifier = Modifier.width(8.dp)); colors.forEach { color -> Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(width = if (viewModel.currentColor == color) 2.dp else 1.dp, color = if (viewModel.currentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, shape = CircleShape).clickable { viewModel.currentColor = color }) }; Spacer(modifier = Modifier.width(8.dp)); Text("${viewModel.currentTextSize.toInt()}px", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Slider(value = viewModel.currentTextSize, onValueChange = { viewModel.currentTextSize = it }, valueRange = 10f..150f, modifier = Modifier.width(100.dp)); IconButton(onClick = { showToolOptions = false }) { Icon(Icons.Filled.Close, contentDescription = null) } }
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
