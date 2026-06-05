package com.midknight.pixelnotes.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.midknight.pixelnotes.data.Note
import com.midknight.pixelnotes.domain.PdfExporter
import com.midknight.pixelnotes.domain.StrokeData
import com.midknight.pixelnotes.ui.components.DrawingCanvas
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    noteToEdit: Note?,
    onSaveNote: (Note) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val strokes = remember {
        mutableStateListOf<StrokeData>().apply {
            noteToEdit?.drawingData?.let { addAll(it) }
        }
    }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableFloatStateOf(8f) }

    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color(0xFFFBC02D), Color.White)

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val exporter = PdfExporter(context)
                val noteToExport = noteToEdit?.copy(drawingData = strokes.toList()) ?: Note(
                    title = "Export",
                    content = "",
                    date = "",
                    drawingData = strokes.toList()
                )
                exporter.exportToPdf(noteToExport, it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(noteToEdit?.title ?: "New Drawing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val currentDate = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())
                            val fileName = "PixelNotes_$currentDate.pdf"
                            pdfLauncher.launch(fileName)
                        },
                        enabled = strokes.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF")
                    }
                    IconButton(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                strokes.removeLast()
                            }
                        },
                        enabled = strokes.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = {
                        val currentDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
                        val noteToSave = noteToEdit?.copy(
                            drawingData = strokes.toList(),
                            date = currentDate
                        ) ?: Note(
                            title = "Sketch",
                            content = "",
                            date = currentDate,
                            drawingData = strokes.toList()
                        )
                        onSaveNote(noteToSave)
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Note")
                    }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (currentColor == color) 2.dp else 1.dp,
                                        color = if (color == Color.White) Color.Gray else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { currentColor = color }
                            )
                        }
                    }

                    Slider(
                        value = currentStrokeWidth,
                        onValueChange = { currentStrokeWidth = it },
                        valueRange = 2f..30f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            DrawingCanvas(
                strokes = strokes,
                currentColor = currentColor,
                currentStrokeWidth = currentStrokeWidth,
                modifier = Modifier
                    .fillMaxHeight(0.95f)
                    .aspectRatio(1f / 1.414f)
                    .shadow(8.dp)
                    .background(Color.White)
            )
        }
    }
}