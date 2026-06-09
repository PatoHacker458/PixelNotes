package com.midknight.pixelnotes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllOut
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.midknight.pixelnotes.data.FolderEntity
import com.midknight.pixelnotes.data.NoteWithPages
import com.midknight.pixelnotes.ui.components.FolderCard
import com.midknight.pixelnotes.ui.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<NoteWithPages>,
    folders: List<FolderEntity>,
    currentFolder: String,
    selectedNotes: List<NoteWithPages>,
    onNoteClick: (NoteWithPages) -> Unit,
    onNoteLongClick: (NoteWithPages) -> Unit,
    onCreateNewNote: (Boolean) -> Unit,
    onFolderSelected: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToRename by remember { mutableStateOf<FolderEntity?>(null) }
    var newFolderName by remember { mutableStateOf("") }
    var showFabMenu by remember { mutableStateOf(false) }

    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder") },
            text = { Text("Delete '${folderToDelete?.name}' and all its contents?") },
            confirmButton = { TextButton(onClick = { folderToDelete?.let { onDeleteFolder(it.path) }; folderToDelete = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { folderToDelete = null }) { Text("Cancel") } }
        )
    }

    if (folderToRename != null) {
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = { Text("Rename Folder") },
            text = { OutlinedTextField(value = newFolderName, onValueChange = { newFolderName = it }, label = { Text("New Name") }) },
            confirmButton = { TextButton(onClick = { if (newFolderName.isNotBlank()) folderToRename?.let { onRenameFolder(it.path, newFolderName) }; folderToRename = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { folderToRename = null }) { Text("Cancel") } }
        )
    }

    val displayedFolders = if (currentFolder == "Trash") emptyList() else if (searchQuery.isNotBlank()) {
        folders.filter { it.name.contains(searchQuery, true) }
    } else {
        if (currentFolder == "All Notes") folders.filter { it.parentPath == null } else folders.filter { it.parentPath == currentFolder }
    }

    val displayedNotes = if (currentFolder == "Trash") {
        notes.filter { it.note.inTrash }
    } else if (searchQuery.isNotBlank()) {
        notes.filter { !it.note.inTrash && (it.note.title.contains(searchQuery, true) || it.note.content.contains(searchQuery, true)) }
    } else {
        if (currentFolder == "All Notes") notes.filter { !it.note.inTrash } else notes.filter { !it.note.inTrash && it.note.folder == currentFolder }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedNotes.isEmpty() && currentFolder != "Trash") {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = showFabMenu) {
                        Column(modifier = Modifier.padding(bottom = 16.dp), horizontalAlignment = Alignment.End) {
                            ExtendedFloatingActionButton(
                                text = { Text("A4 Note") },
                                icon = { Icon(Icons.Default.Description, contentDescription = null) },
                                onClick = { showFabMenu = false; onCreateNewNote(false) },
                                modifier = Modifier.padding(bottom = 8.dp),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                            ExtendedFloatingActionButton(
                                text = { Text("Infinite Canvas") },
                                icon = { Icon(Icons.Default.AllOut, contentDescription = null) },
                                onClick = { showFabMenu = false; onCreateNewNote(true) },
                                modifier = Modifier.padding(bottom = 8.dp),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, contentDescription = "New Note")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search in $currentFolder...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear search") } } },
                shape = RoundedCornerShape(32.dp),
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (displayedFolders.isEmpty() && displayedNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = if (searchQuery.isNotBlank()) "No matching results found" else "Empty folder", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayedFolders) { folder ->
                        FolderCard(
                            folder = folder,
                            onClick = { searchQuery = ""; onFolderSelected(folder.path) },
                            onRenameClick = { newFolderName = folder.name; folderToRename = folder },
                            onDeleteClick = { folderToDelete = folder }
                        )
                    }
                    items(displayedNotes) { noteWP ->
                        NoteCard(
                            noteWithPages = noteWP,
                            isSelected = selectedNotes.any { it.note.id == noteWP.note.id },
                            onClick = { if (selectedNotes.isNotEmpty() || currentFolder == "Trash") onNoteLongClick(noteWP) else onNoteClick(noteWP) },
                            onLongClick = { onNoteLongClick(noteWP) }
                        )
                    }
                }
            }
        }
    }
}