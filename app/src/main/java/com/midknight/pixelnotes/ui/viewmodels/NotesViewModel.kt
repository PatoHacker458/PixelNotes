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
import com.midknight.pixelnotes.data.NoteWithPages
import com.midknight.pixelnotes.data.PageEntity
import com.midknight.pixelnotes.domain.StrokeData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DrawingTool { PEN, HIGHLIGHTER, ERASER, TEXT, SELECTION }

class NotesViewModel(private val dao: NoteDao) : ViewModel() {
    val notes = dao.getAllNotesWithPages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = dao.getAllFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var currentFolderFilter by mutableStateOf("Todas")
    var currentScreen by mutableIntStateOf(0)
    var selectedNoteWithPages by mutableStateOf<NoteWithPages?>(null)

    // Editor State
    var currentTitle by mutableStateOf("New Note")
    val currentStrokes = mutableStateListOf<StrokeData>()
    val redoStrokes = mutableStateListOf<StrokeData>()
    var currentBackgroundUri by mutableStateOf<String?>(null)
    var currentColor by mutableStateOf(Color.Black)
    var currentStrokeWidth by mutableFloatStateOf(8f)
    var currentTool by mutableStateOf(DrawingTool.PEN)
    var eraserType by mutableIntStateOf(0)
    var currentPaperStyle by mutableIntStateOf(0)
    var currentCanvasColor by mutableIntStateOf(-1)
    var fingerDrawingEnabled by mutableStateOf(true)

    // Multi-Page State
    val currentPages = mutableStateListOf<PageEntity>()
    var currentPageIndex by mutableIntStateOf(0)

    val selectedNotes = mutableStateListOf<NoteWithPages>()
    var showDeleteDialog by mutableStateOf(false)
    var showMoveDialog by mutableStateOf(false)
    var showShareDialog by mutableStateOf(false)

    fun flushEditorToMemory() {
        if (currentPages.isNotEmpty() && currentPageIndex < currentPages.size) {
            currentPages[currentPageIndex] = currentPages[currentPageIndex].copy(
                drawingData = currentStrokes.toList(),
                backgroundUri = currentBackgroundUri,
                paperStyle = currentPaperStyle,
                canvasColor = currentCanvasColor
            )
        }
    }

    fun loadPage(index: Int) {
        flushEditorToMemory()
        currentPageIndex = index
        val page = currentPages[index]
        currentStrokes.clear()
        redoStrokes.clear()
        currentStrokes.addAll(page.drawingData)
        currentBackgroundUri = page.backgroundUri
        currentPaperStyle = page.paperStyle
        currentCanvasColor = page.canvasColor
    }

    fun addNewPage() {
        flushEditorToMemory()
        val newPage = PageEntity(noteId = selectedNoteWithPages?.note?.id ?: 0, pageNumber = currentPages.size)
        currentPages.add(newPage)
        loadPage(currentPages.size - 1)
    }

    fun deleteCurrentPage() {
        if (currentPages.size > 1) {
            currentPages.removeAt(currentPageIndex)
            if (currentPageIndex >= currentPages.size) currentPageIndex = currentPages.size - 1
            loadPage(currentPageIndex)
        }
    }

    fun addStroke(stroke: StrokeData) { currentStrokes.add(stroke); redoStrokes.clear() }
    fun removeStroke(stroke: StrokeData) { currentStrokes.remove(stroke) }
    fun undoStroke() { if (currentStrokes.isNotEmpty()) redoStrokes.add(currentStrokes.removeAt(currentStrokes.lastIndex)) }
    fun redoStroke() { if (redoStrokes.isNotEmpty()) currentStrokes.add(redoStrokes.removeAt(redoStrokes.lastIndex)) }
    fun clearCanvas() { currentStrokes.clear(); redoStrokes.clear() }

    fun toggleSelection(note: NoteWithPages) {
        if (selectedNotes.any { it.note.id == note.note.id }) selectedNotes.removeAll { it.note.id == note.note.id } else selectedNotes.add(note)
    }
    fun clearSelection() { selectedNotes.clear() }

    fun deleteSelectedNotes() {
        viewModelScope.launch {
            selectedNotes.forEach { dao.deleteNote(it.note); dao.deletePagesByNoteId(it.note.id) }
            clearSelection()
        }
    }

    fun moveSelectedNotes(newFolder: String) {
        viewModelScope.launch {
            selectedNotes.forEach { dao.updateNote(it.note.copy(folder = newFolder)) }
            clearSelection()
        }
    }

    fun createFolder(name: String, parentPath: String?) {
        val path = if (parentPath == null) name else "$parentPath/$name"
        viewModelScope.launch { dao.insertFolder(FolderEntity(path = path, name = name, parentPath = parentPath)) }
    }

    fun renameFolder(oldPath: String, newName: String) {
        val parentPath = oldPath.substringBeforeLast('/', "")
        val newPath = if (parentPath.isEmpty()) newName else "$parentPath/$newName"
        viewModelScope.launch {
            dao.renameFoldersCascade(oldPath, newPath, newName)
            dao.renameNotesFolderCascade(oldPath, newPath)
            if (currentFolderFilter == oldPath || currentFolderFilter.startsWith("$oldPath/")) currentFolderFilter = newPath + currentFolderFilter.removePrefix(oldPath)
        }
    }

    fun deleteFolder(path: String) {
        viewModelScope.launch {
            dao.deleteFolderCascade(path)
            dao.deleteNotesInFolderCascade(path)
            if (currentFolderFilter == path || currentFolderFilter.startsWith("$path/")) currentFolderFilter = "Todas"
        }
    }

    fun openNoteForEditing(noteWP: NoteWithPages?) {
        selectedNoteWithPages = noteWP
        currentStrokes.clear()
        redoStrokes.clear()
        currentColor = Color.Black
        currentStrokeWidth = 8f
        currentTool = DrawingTool.PEN
        currentPages.clear()

        if (noteWP != null && noteWP.pages.isNotEmpty()) {
            currentTitle = noteWP.note.title
            currentPages.addAll(noteWP.pages)
            loadPage(0)
        } else {
            currentTitle = "New Note"
            currentPages.add(PageEntity(noteId = 0, pageNumber = 0))
            loadPage(0)
        }
        currentScreen = 1
    }

    fun closeEditing() {
        val isBlank = selectedNoteWithPages == null && currentStrokes.isEmpty() && currentBackgroundUri == null && currentTitle == "New Note" && currentPaperStyle == 0 && currentCanvasColor == -1 && currentPages.size <= 1
        if (!isBlank) {
            val currentDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
            saveCurrentNote(currentDate)
        }
        selectedNoteWithPages = null
        currentStrokes.clear()
        currentBackgroundUri = null
        currentScreen = 0
    }

    fun saveCurrentNote(date: String) {
        flushEditorToMemory()
        val targetFolder = selectedNoteWithPages?.note?.folder ?: if (currentFolderFilter == "Todas") "General" else currentFolderFilter
        val noteToSave = selectedNoteWithPages?.note?.copy(title = currentTitle, date = date) ?: Note(title = currentTitle, content = "", date = date, folder = targetFolder)

        viewModelScope.launch {
            val noteId = if (noteToSave.id == 0) dao.insertNote(noteToSave).toInt() else { dao.updateNote(noteToSave); noteToSave.id }
            dao.deletePagesByNoteId(noteId)
            currentPages.forEachIndexed { index, page ->
                dao.insertPage(page.copy(pageId = 0, noteId = noteId, pageNumber = index))
            }
        }
    }
}

class NotesViewModelFactory(private val dao: NoteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) return NotesViewModel(dao) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}