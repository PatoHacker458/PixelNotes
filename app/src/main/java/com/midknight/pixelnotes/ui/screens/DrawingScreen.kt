package com.midknight.pixelnotes.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.midknight.pixelnotes.data.Note
import com.midknight.pixelnotes.domain.PdfExporter
import com.midknight.pixelnotes.ui.components.DrawingCanvas
import com.midknight.pixelnotes.ui.viewmodels.NotesViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(viewModel: NotesViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val isBlank = viewModel.selectedNote == null && viewModel.currentStrokes.isEmpty() && viewModel.currentBackgroundUri == null && viewModel.currentTitle == "New Note"
                if (!isBlank) {
                    val currentDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
                    viewModel.saveCurrentNote(currentDate)
                    Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color(0xFFFBC02D))

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val exporter = PdfExporter(context)
                val noteToExport = viewModel.selectedNote?.copy(
                    title = viewModel.currentTitle,
                    drawingData = viewModel.currentStrokes.toList(),
                    backgroundUri = viewModel.currentBackgroundUri
                ) ?: Note(
                    title = viewModel.currentTitle,
                    content = "",
                    date = "",
                    drawingData = viewModel.currentStrokes.toList(),
                    backgroundUri = viewModel.currentBackgroundUri
                )
                exporter.exportToPdf(listOf(noteToExport), it)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flag)
            viewModel.currentBackgroundUri = it.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = viewModel.currentTitle,
                        onValueChange = { viewModel.currentTitle = it },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val isBlank = viewModel.selectedNote == null && viewModel.currentStrokes.isEmpty() && viewModel.currentBackgroundUri == null && viewModel.currentTitle == "New Note"
                        if (!isBlank) {
                            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        }
                        viewModel.closeEditing()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Icon(Icons.Filled.Image, contentDescription = "Add Background") }

                    IconButton(
                        onClick = {
                            val currentDate = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())
                            val fileName = "${viewModel.currentTitle.replace(" ", "_")}_$currentDate.pdf"
                            pdfLauncher.launch(fileName)
                        },
                        enabled = viewModel.currentStrokes.isNotEmpty()
                    ) { Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF") }

                    IconButton(
                        onClick = { viewModel.undoStroke() },
                        enabled = viewModel.currentStrokes.isNotEmpty()
                    ) { Icon(Icons.Filled.Undo, contentDescription = "Undo") }

                    IconButton(
                        onClick = { viewModel.redoStroke() },
                        enabled = viewModel.redoStrokes.isNotEmpty()
                    ) { Icon(Icons.Filled.Redo, contentDescription = "Redo") }

                    IconButton(
                        onClick = { viewModel.clearCanvas() },
                        enabled = viewModel.currentStrokes.isNotEmpty()
                    ) { Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear Canvas") }

                    IconButton(onClick = {
                        val currentDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
                        viewModel.saveCurrentNote(currentDate)
                        Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Filled.Save, contentDescription = "Save Note") }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (viewModel.currentColor == color && !viewModel.isEraserMode) 2.dp else 1.dp,
                                        color = if (viewModel.currentColor == color && !viewModel.isEraserMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.currentColor = color
                                        viewModel.isEraserMode = false
                                    }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (viewModel.isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.isEraserMode = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("E", fontWeight = FontWeight.Bold, color = if (viewModel.isEraserMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Slider(
                        value = viewModel.currentStrokeWidth,
                        onValueChange = { viewModel.currentStrokeWidth = it },
                        valueRange = 2f..50f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFE0E0E0))
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val maxWidthPx = constraints.maxWidth.toFloat()
            val maxHeightPx = constraints.maxHeight.toFloat()

            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            var gestureIntent by remember { mutableStateOf("none") }

            val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                if (gestureIntent == "none") {
                    val zoomDelta = abs(zoomChange - 1f) * 300f
                    val panDelta = offsetChange.getDistance()

                    if (panDelta > 3f && panDelta > zoomDelta) {
                        gestureIntent = "pan"
                    } else if (zoomDelta > 3f) {
                        gestureIntent = "zoom"
                    }
                }

                if (gestureIntent != "pan") {
                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                }

                val maxX = (maxWidthPx * (scale - 1f)) / 2f
                val maxY = (maxHeightPx * (scale - 1f)) / 2f

                val panMultiplier = 2.0f

                offset = Offset(
                    x = (offset.x + (offsetChange.x * panMultiplier)).coerceIn(-maxX, maxX),
                    y = (offset.y + (offsetChange.y * panMultiplier)).coerceIn(-maxY, maxY)
                )
            }

            LaunchedEffect(transformState.isTransformInProgress) {
                if (!transformState.isTransformInProgress) {
                    gestureIntent = "none"
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = transformState),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.95f)
                        .aspectRatio(1f / 1.414f)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .shadow(8.dp)
                        .background(Color.White)
                ) {
                    viewModel.currentBackgroundUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Background Template",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    DrawingCanvas(
                        strokes = viewModel.currentStrokes,
                        currentColor = viewModel.currentColor,
                        currentStrokeWidth = viewModel.currentStrokeWidth,
                        isEraserMode = viewModel.isEraserMode,
                        onStrokeAdd = { viewModel.addStroke(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}