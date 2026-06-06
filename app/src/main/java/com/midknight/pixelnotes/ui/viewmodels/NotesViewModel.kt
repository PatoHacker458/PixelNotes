package com.midknight.pixelnotes.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.midknight.pixelnotes.data.FolderEntity
import com.midknight.pixelnotes.data.Note
import com.midknight.pixelnotes.data.NoteDao
import com.midknight.pixelnotes.domain.StrokeData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesViewModel(private val dao: NoteDao) : ViewModel() {

    val notes = dao.getAllNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = dao.getAllFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var currentFolderFilter by mutableStateOf("Todas")
    var currentScreen by mutableIntStateOf(0)
    var selectedNote by mutableStateOf<Note?>(null)

    // Editor State
    var currentTitle by mutableStateOf("New Note")
    val currentStrokes = mutableStateListOf<StrokeData>()
    var currentBackgroundUri by mutableStateOf<String?>(null)
    var currentColor by mutableStateOf(Color.Black)
    var currentStrokeWidth by mutableFloatStateOf(8f)
    var isEraserMode by mutableStateOf(false)

    // Bulk Selection State
    val selectedNotes = mutableStateListOf<Note>()
    var showDeleteDialog by mutableStateOf(false)
    var showMoveDialog by mutableStateOf(false)
    var showShareDialog by mutableStateOf(false)

    fun toggleSelection(note: Note) {
        if (selectedNotes.any { it.id == note.id }) {
            selectedNotes.removeAll { it.id == note.id }
        } else {
            selectedNotes.add(note)
        }
    }

    fun clearSelection() {
        selectedNotes.clear()
    }

    fun deleteSelectedNotes() {
        viewModelScope.launch {
            selectedNotes.forEach { dao.deleteNote(it) }
            clearSelection()
        }
    }

    fun moveSelectedNotes(newFolder: String) {
        viewModelScope.launch {
            selectedNotes.forEach { dao.updateNote(it.copy(folder = newFolder)) }
            clearSelection()
        }
    }

    fun createFolder(name: String, parentPath: String?) {
        val path = if (parentPath == null) name else "$parentPath/$name"
        viewModelScope.launch {
            dao.insertFolder(FolderEntity(path = path, name = name, parentPath = parentPath))
        }
    }

    fun renameFolder(oldPath: String, newName: String) {
        val parentPath = oldPath.substringBeforeLast('/', "")
        val newPath = if (parentPath.isEmpty()) newName else "$parentPath/$newName"
        viewModelScope.launch {
            dao.renameFoldersCascade(oldPath, newPath, newName)
            dao.renameNotesFolderCascade(oldPath, newPath)
            if (currentFolderFilter == oldPath || currentFolderFilter.startsWith("$oldPath/")) {
                currentFolderFilter = newPath + currentFolderFilter.removePrefix(oldPath)
            }
        }
    }

    fun deleteFolder(path: String) {
        viewModelScope.launch {
            dao.deleteFolderCascade(path)
            dao.deleteNotesInFolderCascade(path)
            if (currentFolderFilter == path || currentFolderFilter.startsWith("$path/")) {
                currentFolderFilter = "Todas"
            }
        }
    }

    fun openNoteForEditing(note: Note?) {
        selectedNote = note
        currentStrokes.clear()
        currentColor = Color.Black
        currentStrokeWidth = 8f
        isEraserMode = false

        if (note != null) {
            currentTitle = note.title
            currentStrokes.addAll(note.drawingData)
            currentBackgroundUri = note.backgroundUri
        } else {
            currentTitle = "New Note"
            currentBackgroundUri = null
        }
        currentScreen = 1
    }

    fun closeEditing() {
        if (!(selectedNote == null && currentStrokes.isEmpty() && currentBackgroundUri == null && currentTitle == "New Note")) {
            val currentDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
            saveCurrentNote(currentDate)
        }
        selectedNote = null
        currentStrokes.clear()
        currentBackgroundUri = null
        currentScreen = 0
    }

    fun saveCurrentNote(date: String) {
        val targetFolder = selectedNote?.folder ?: if (currentFolderFilter == "Todas") "General" else currentFolderFilter
        val note = selectedNote?.copy(
            title = currentTitle,
            drawingData = currentStrokes.toList(),
            backgroundUri = currentBackgroundUri,
            date = date
        ) ?: Note(
            title = currentTitle,
            content = "",
            date = date,
            drawingData = currentStrokes.toList(),
            backgroundUri = currentBackgroundUri,
            folder = targetFolder
        )

        viewModelScope.launch {
            if (note.id == 0) {
                val newId = dao.insertNote(note)
                selectedNote = note.copy(id = newId.toInt())
            } else {
                dao.updateNote(note)
                selectedNote = note
            }
        }
    }
}

class NotesViewModelFactory(private val dao: NoteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}