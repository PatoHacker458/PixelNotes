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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.midknight.pixelnotes.data.Note
import com.midknight.pixelnotes.data.NoteWithPages
import com.midknight.pixelnotes.data.PageEntity
import com.midknight.pixelnotes.domain.PdfExporter
import com.midknight.pixelnotes.ui.components.DrawingCanvas
import com.midknight.pixelnotes.ui.viewmodels.DrawingTool
import com.midknight.pixelnotes.ui.viewmodels.NotesViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun PaperTemplate(style: Int, modifier: Modifier = Modifier) {
    if (style == 0) return
    Canvas(modifier = modifier) {
        val spacing = 30.dp.toPx()
        val color = Color.LightGray.copy(alpha = 0.5f)
        when (style) {
            1 -> { var y = spacing; while (y < size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y), 2f); y += spacing } }
            2 -> {
                var y = spacing; while (y < size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y), 2f); y += spacing }
                var x = spacing; while (x < size.width) { drawLine(color, Offset(x, 0f), Offset(x, size.height), 2f); x += spacing }
            }
            3 -> {
                val radius = 1.5.dp.toPx(); var y = spacing
                while (y < size.height) {
                    var x = spacing; while (x < size.width) { drawCircle(color, radius, Offset(x, y)); x += spacing }
                    y += spacing
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(viewModel: NotesViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (!viewModel.isNoteBlank()) {
                    val currentDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
                    viewModel.saveCurrentNote(currentDate)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val colors = listOf(Color.Black, Color.White, Color.Red, Color.Blue, Color.Green, Color(0xFFFBC02D), Color(0xFFE91E63))
    val canvasColors = listOf(-1, Color.Black.toArgb(), Color.DarkGray.toArgb(), Color(0xFFFFF8E7).toArgb(), Color(0xFFE3F2FD).toArgb())

    val pdfLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let {
            coroutineScope.launch {
                val exporter = PdfExporter(context)
                val noteToExport = viewModel.selectedNoteWithPages?.copy(
                    note = viewModel.selectedNoteWithPages!!.note.copy(title = viewModel.currentTitle), pages = viewModel.currentPages.toList()
                ) ?: NoteWithPages(note = Note(title = viewModel.currentTitle, content = "", date = "", folder = "General"), pages = viewModel.currentPages.toList())
                exporter.exportToPdf(listOf(noteToExport), it)
                Toast.makeText(context, "Exported full Note to PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var singlePageToExport by remember { mutableStateOf<PageEntity?>(null) }
    val singlePdfLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { safeUri ->
            singlePageToExport?.let { page ->
                coroutineScope.launch {
                    val exporter = PdfExporter(context)
                    exporter.exportSinglePageToPdf(page, safeUri)
                    Toast.makeText(context, "Exported single page to PDF", Toast.LENGTH_SHORT).show()
                    singlePageToExport = null
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flag)
            viewModel.updateActivePageBackground(it.toString())
        }
    }

    var showToolOptions by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showCanvasColorMenu by remember { mutableStateOf(false) }
    var showPaperMenu by remember { mutableStateOf(false) }
    var showImageMenu by remember { mutableStateOf(false) }
    var showPagesPanel by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LaunchedEffect(listState.firstVisibleItemIndex) {
        viewModel.activePageIndex = listState.firstVisibleItemIndex
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceVariant) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
            val maxWidthPx = constraints.maxWidth.toFloat()
            val maxHeightPx = constraints.maxHeight.toFloat()
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            var gestureIntent by remember { mutableStateOf("none") }

            val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                if (gestureIntent == "none") {
                    val zoomDelta = abs(zoomChange - 1f) * 300f
                    val panDelta = offsetChange.getDistance()
                    if (panDelta > 3f && panDelta > zoomDelta) gestureIntent = "pan" else if (zoomDelta > 3f) gestureIntent = "zoom"
                }
                if (gestureIntent != "pan") scale = (scale * zoomChange).coerceIn(1f, 5f)

                val maxX = (maxWidthPx * (scale - 1f)) / 2f
                val maxY = (maxHeightPx * (scale - 1f)) / 2f

                val newOffsetY = offset.y + (offsetChange.y * 2.0f)
                var overflowY = 0f
                if (newOffsetY > maxY) overflowY = newOffsetY - maxY
                else if (newOffsetY < -maxY) overflowY = newOffsetY + maxY

                if (overflowY != 0f) {
                    listState.dispatchRawDelta(-overflowY)
                }

                offset = Offset(
                    (offset.x + (offsetChange.x * 2.0f)).coerceIn(-maxX, maxX),
                    newOffsetY.coerceIn(-maxY, maxY)
                )
            }

            LaunchedEffect(transformState.isTransformInProgress) { if (!transformState.isTransformInProgress) gestureIntent = "none" }

            // CANVAS CONTINUO
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
                        Box(modifier = Modifier.fillParentMaxHeight(0.95f).aspectRatio(1f / 1.414f).shadow(8.dp).background(if (page.canvasColor == -1) Color.White else Color(page.canvasColor))) {
                            PaperTemplate(style = page.paperStyle, modifier = Modifier.fillMaxSize())
                            page.backgroundUri?.let { uri -> AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }

                            DrawingCanvas(
                                pageIndex = index,
                                strokes = page.drawingData,
                                selectedStrokes = if (viewModel.selectionPageIndex == index) viewModel.selectedStrokes else emptyList(),
                                isSelectionActiveOnPage = viewModel.selectionPageIndex == index,
                                selectionMode = viewModel.selectionMode,
                                currentColor = viewModel.currentColor,
                                currentStrokeWidth = if (viewModel.currentTool == DrawingTool.ERASER) viewModel.currentEraserWidth else viewModel.currentStrokeWidth,
                                currentTool = viewModel.currentTool,
                                eraserType = viewModel.eraserType,
                                fingerDrawingEnabled = viewModel.fingerDrawingEnabled,
                                onStrokeAdd = { stroke -> viewModel.addStrokeToPage(index, stroke) },
                                onStrokeRemove = { stroke -> viewModel.removeStrokeFromPage(index, stroke) },
                                onProcessSelection = { points -> viewModel.processSelection(index, points) },
                                onMoveSelection = { dx, dy -> viewModel.moveSelection(dx, dy) },
                                onCommitSelection = { viewModel.commitSelection() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // PANEL LATERAL FLOTANTE DE PÁGINAS (Bloquea toques)
            androidx.compose.animation.AnimatedVisibility(
                visible = showPagesPanel,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it }),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp, top = 80.dp, bottom = 80.dp).zIndex(10f)
            ) {
                Column(
                    modifier = Modifier.width(90.dp).fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                        .pointerInput(Unit){}
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp)).padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var draggedItem by remember { mutableStateOf<Int?>(null) }
                    var dragOffset by remember { mutableFloatStateOf(0f) }

                    LazyColumn(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        items(viewModel.currentPages.size) { index ->
                            val isSelected = index == viewModel.activePageIndex
                            val yOffset = if (draggedItem == index) dragOffset else 0f
                            var showPageMenu by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier.padding(vertical = 8.dp).zIndex(if (draggedItem == index) 1f else 0f).graphicsLayer(translationY = yOffset)
                                    .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { draggedItem = index; dragOffset = 0f },
                                            onDragEnd = {
                                                val targetIndex = (index + (dragOffset / 200f).toInt()).coerceIn(0, viewModel.currentPages.size - 1)
                                                viewModel.movePage(index, targetIndex)
                                                draggedItem = null; dragOffset = 0f
                                            },
                                            onDrag = { _, dragAmount -> dragOffset += dragAmount.y }
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.size(width = 60.dp, height = 80.dp).clip(RoundedCornerShape(8.dp))
                                        .background(if (viewModel.currentPages[index].canvasColor == -1) Color.White else Color(viewModel.currentPages[index].canvasColor))
                                        .border(width = if (isSelected) 3.dp else 1.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(8.dp))
                                        .clickable { coroutineScope.launch { listState.animateScrollToItem(index) } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PaperTemplate(style = viewModel.currentPages[index].paperStyle, modifier = Modifier.fillMaxSize())
                                    Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }

                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                                    IconButton(onClick = { showPageMenu = true }, modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surface.copy(alpha=0.8f), CircleShape)) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu", modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(expanded = showPageMenu, onDismissRequest = { showPageMenu = false }) {
                                        DropdownMenuItem(text = { Text("Export as PDF") }, onClick = { showPageMenu = false; singlePageToExport = viewModel.currentPages[index]; singlePdfLauncher.launch("${viewModel.currentTitle.replace(" ", "_")}_Page_${index + 1}.pdf") })
                                        DropdownMenuItem(text = { Text("Export as Image") }, onClick = { showPageMenu = false; Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show() })
                                        if (viewModel.currentPages.size > 1) {
                                            DropdownMenuItem(text = { Text("Delete Page", color = MaterialTheme.colorScheme.error) }, onClick = { showPageMenu = false; viewModel.deletePageAt(index) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.addNewPage(); coroutineScope.launch { listState.animateScrollToItem(viewModel.currentPages.lastIndex) } }, modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Page", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            // TOP TOOLBAR UNIFICADA
            Row(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 16.dp, end = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).pointerInput(Unit){}.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.closeEditing() }) { Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
                    TextField(
                        value = viewModel.currentTitle, onValueChange = { viewModel.currentTitle = it },
                        modifier = Modifier.width(150.dp), singleLine = true,
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }

                Row(modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).pointerInput(Unit){}.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showPagesPanel = !showPagesPanel }) { Icon(Icons.Filled.Layers, contentDescription = "Pages", tint = if (showPagesPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = { viewModel.fingerDrawingEnabled = !viewModel.fingerDrawingEnabled }) { Icon(if (viewModel.fingerDrawingEnabled) Icons.Filled.TouchApp else Icons.Filled.PanTool, contentDescription = null, tint = if (viewModel.fingerDrawingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
                    Box {
                        IconButton(onClick = { showPaperMenu = true }) { Icon(Icons.Filled.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
                        DropdownMenu(expanded = showPaperMenu, onDismissRequest = { showPaperMenu = false }) {
                            DropdownMenuItem(text = { Text("Blank") }, onClick = { viewModel.updateActivePagePaperStyle(0); showPaperMenu = false })
                            DropdownMenuItem(text = { Text("Lined") }, onClick = { viewModel.updateActivePagePaperStyle(1); showPaperMenu = false })
                            DropdownMenuItem(text = { Text("Grid") }, onClick = { viewModel.updateActivePagePaperStyle(2); showPaperMenu = false })
                            DropdownMenuItem(text = { Text("Dotted") }, onClick = { viewModel.updateActivePagePaperStyle(3); showPaperMenu = false })
                        }
                    }
                    Box {
                        IconButton(onClick = { showCanvasColorMenu = true }) { Icon(Icons.Filled.FormatColorFill, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
                        DropdownMenu(expanded = showCanvasColorMenu, onDismissRequest = { showCanvasColorMenu = false }) {
                            canvasColors.forEach { colorArgb -> DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(if (colorArgb == -1) Color.White else Color(colorArgb)).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)); Spacer(modifier = Modifier.width(8.dp)); Text(if (colorArgb == -1) "Default White" else "Solid Color") } }, onClick = { viewModel.updateActivePageCanvasColor(colorArgb); showCanvasColorMenu = false }) }
                        }
                    }
                    Box {
                        IconButton(onClick = { showImageMenu = true }) { Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
                        DropdownMenu(expanded = showImageMenu, onDismissRequest = { showImageMenu = false }) {
                            DropdownMenuItem(text = { Text("Add Image to Page") }, onClick = { showImageMenu = false; photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                            DropdownMenuItem(text = { Text("Remove Image") }, onClick = { showImageMenu = false; viewModel.updateActivePageBackground(null) })
                            DropdownMenuItem(text = { Text("Import PDF") }, onClick = { showImageMenu = false; Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() })
                        }
                    }
                    IconButton(onClick = { viewModel.undo() }) { Icon(Icons.Filled.Undo, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = { viewModel.redo() }) { Icon(Icons.Filled.Redo, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) { Icon(Icons.Filled.IosShare, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(text = { Text("Export Note as PDF") }, onClick = { showExportMenu = false; val currentDate = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date()); pdfLauncher.launch("${viewModel.currentTitle.replace(" ", "_")}_$currentDate.pdf") })
                        }
                    }
                }
            }

            // BOTTOM TOOLBAR
            Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(visible = showToolOptions, enter = expandVertically(), exit = shrinkVertically()) {
                    Row(modifier = Modifier.padding(bottom = 16.dp).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)).pointerInput(Unit){}.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                        if (viewModel.currentTool == DrawingTool.PEN || viewModel.currentTool == DrawingTool.HIGHLIGHTER) {
                            colors.forEach { color -> Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(width = if (viewModel.currentColor == color) 2.dp else 1.dp, color = if (viewModel.currentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, shape = CircleShape).clickable { viewModel.currentColor = color }) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${(viewModel.currentStrokeWidth / 40f * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Slider(value = viewModel.currentStrokeWidth, onValueChange = { viewModel.currentStrokeWidth = it }, valueRange = 4f..40f, modifier = Modifier.width(100.dp))
                            IconButton(onClick = { showToolOptions = false }) { Icon(Icons.Filled.Close, contentDescription = null) }

                        } else if (viewModel.currentTool == DrawingTool.ERASER) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.eraserType == 0) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.eraserType = 0 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Normal", color = if (viewModel.eraserType == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.eraserType == 1) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.eraserType = 1 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Stroke", color = if (viewModel.eraserType == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }

                            if (viewModel.eraserType == 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${(viewModel.currentEraserWidth / 100f * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Slider(value = viewModel.currentEraserWidth, onValueChange = { viewModel.currentEraserWidth = it }, valueRange = 10f..100f, modifier = Modifier.width(100.dp))
                            }
                            IconButton(onClick = { showToolOptions = false }) { Icon(Icons.Filled.Close, contentDescription = null) }

                        } else if (viewModel.currentTool == DrawingTool.SELECTION) {
                            if (viewModel.selectedStrokes.isNotEmpty()) {
                                // MODO SELECCIÓN ACTIVA: Opciones de Edición
                                colors.forEach { color -> Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(width = 1.dp, color = Color.Transparent, shape = CircleShape).clickable { viewModel.changeSelectionColor(color.toArgb()) }) }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.deleteSelection() }) { Icon(Icons.Filled.DeleteSweep, contentDescription = "Delete Selection", tint = MaterialTheme.colorScheme.error) }
                            } else {
                                // MODO SELECCIÓN INACTIVA: Modos de Lazo
                                Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.selectionMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.selectionMode = 0 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Free Form", color = if (viewModel.selectionMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                                Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (viewModel.selectionMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { viewModel.selectionMode = 1 }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Rectangle", color = if (viewModel.selectionMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                            }
                            IconButton(onClick = { showToolOptions = false }) { Icon(Icons.Filled.Close, contentDescription = null) }

                        } else {
                            Text("Tool options coming soon", color = MaterialTheme.colorScheme.outline)
                            IconButton(onClick = { showToolOptions = false }) { Icon(Icons.Filled.Close, contentDescription = null) }
                        }
                    }
                }

                Row(modifier = Modifier.clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).pointerInput(Unit){}.padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val tools = listOf(DrawingTool.PEN to Icons.Filled.Edit, DrawingTool.HIGHLIGHTER to Icons.Filled.BorderColor, DrawingTool.ERASER to Icons.Filled.LayersClear, DrawingTool.TEXT to Icons.Filled.Title, DrawingTool.SELECTION to Icons.Filled.HighlightAlt)
                    tools.forEach { (tool, icon) ->
                        val isSelected = viewModel.currentTool == tool
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { if (isSelected) showToolOptions = !showToolOptions else { viewModel.setTool(tool); showToolOptions = true } }, contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = scale != 1f || offset != Offset.Zero, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)) {
                FloatingActionButton(onClick = { scale = 1f; offset = Offset.Zero }, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) { Icon(Icons.Default.FilterCenterFocus, contentDescription = "Reset View") }
            }
        }
    }
}