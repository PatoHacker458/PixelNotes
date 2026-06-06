package com.midknight.pixelnotes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.midknight.pixelnotes.data.Note
import com.midknight.pixelnotes.ui.components.NoteCard

@Composable
fun NotesScreen(
    notes: List<Note>,
    onNoteClick: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit
) {
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete '${noteToDelete?.title}'?") },
            confirmButton = {
                TextButton(onClick = {
                    noteToDelete?.let { onDeleteNote(it) }
                    noteToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(notes) { note ->
            NoteCard(
                note = note,
                onClick = { onNoteClick(note) },
                onLongClick = { noteToDelete = note }
            )
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}