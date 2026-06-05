package com.midknight.pixelnotes.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.midknight.pixelnotes.data.Note
import com.midknight.pixelnotes.domain.StrokeData
import com.midknight.pixelnotes.ui.components.DrawingCanvas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(onSaveNote: (Note) -> Unit) {
    val strokes = remember { mutableStateListOf<StrokeData>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Drawing") },
                actions = {
                    IconButton(onClick = {
                        val currentDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
                        val newNote = Note(
                            title = "Sketch",
                            content = "",
                            date = currentDate,
                            drawingData = strokes.toList()
                        )
                        onSaveNote(newNote)
                        strokes.clear()
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Note")
                    }
                }
            )
        }
    ) { paddingValues ->
        DrawingCanvas(
            strokes = strokes,
            modifier = Modifier.padding(paddingValues)
        )
    }
}