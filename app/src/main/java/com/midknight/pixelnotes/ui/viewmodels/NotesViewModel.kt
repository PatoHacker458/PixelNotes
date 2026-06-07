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

data class StrokeAction(val pageIndex: Int, val stroke: StrokeData, val isAdd: Boolean)

class NotesViewModel(private val dao: NoteDao) : ViewModel() {
    val notes = dao.getAllNotesWithPages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = dao.getAllFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var currentFolderFilter by mutableStateOf("Todas")
    var currentScreen by mutableIntStateOf(0)
    var selectedNoteWithPages by mutableStateOf<NoteWithPages?>(null)

    // Global Editor State
    var currentTitle by mutableStateOf("New Note")
    var currentColor by mutableStateOf(Color.Black)
    var currentStrokeWidth by mutableFloatStateOf(8f)
    var currentEraserWidth by mutableFloatStateOf(20f) // Nuevo ancho independiente
    var currentTool by mutableStateOf(DrawingTool.PEN)
    var eraserType by mutableIntStateOf(0)
    var fingerDrawingEnabled by mutableStateOf(true)

    // Multi-Page Continuous State
    val currentPages = mutableStateListOf<PageEntity>()
    var activePageIndex by mutableIntStateOf(0)

    private val undoStack = mutableListOf<StrokeAction>()
    private val redoStack = mutableListOf<StrokeAction>()

    // Bulk Actions
    val selectedNotes = mutableStateListOf<NoteWithPages>()
    var showDeleteDialog by mutableStateOf(false)
    var showMoveDialog by mutableStateOf(false)
    var showShareDialog by mutableStateOf(false)

    // --- PAGE MANAGEMENT ---

    fun addNewPage() {
        val newPage = PageEntity(noteId = selectedNoteWithPages?.note?.id ?: 0, pageNumber = currentPages.size)
        currentPages.add(newPage)
        activePageIndex = currentPages.lastIndex
    }

    fun deletePageAt(index: Int) {
        if (currentPages.size > 1) {
            currentPages.removeAt(index)
            if (activePageIndex >= currentPages.size) activePageIndex = currentPages.size - 1
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val page = currentPages.removeAt(fromIndex)
        currentPages.add(toIndex, page)
        if (activePageIndex == fromIndex) activePageIndex = toIndex
        else if (activePageIndex in minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex)) {
            if (fromIndex < toIndex) activePageIndex-- else activePageIndex++
        }
    }

    fun updateActivePageBackground(uri: String?) {
        if(currentPages.isEmpty()) return
        currentPages[activePageIndex] = currentPages[activePageIndex].copy(backgroundUri = uri)
    }

    fun updateActivePagePaperStyle(style: Int) {
        if(currentPages.isEmpty()) return
        currentPages[activePageIndex] = currentPages[activePageIndex].copy(paperStyle = style)
    }

    fun updateActivePageCanvasColor(color: Int) {
        if(currentPages.isEmpty()) return
        currentPages[activePageIndex] = currentPages[activePageIndex].copy(canvasColor = color)
    }

    // --- CONTINUOUS DRAWING ENGINE ---

    fun addStrokeToPage(pageIndex: Int, stroke: StrokeData) {
        val page = currentPages[pageIndex]
        currentPages[pageIndex] = page.copy(drawingData = page.drawingData + stroke)
        undoStack.add(StrokeAction(pageIndex, stroke, true))
        redoStack.clear()
        activePageIndex = pageIndex
    }

    fun removeStrokeFromPage(pageIndex: Int, stroke: StrokeData) {
        val page = currentPages[pageIndex]
        // Usamos filtrado absoluto por referencia para evitar bugs de borrado
        currentPages[pageIndex] = page.copy(drawingData = page.drawingData.filter { it !== stroke })
        undoStack.add(StrokeAction(pageIndex, stroke, false))
        redoStack.clear()
        activePageIndex = pageIndex
    }

    fun undo() {
        val action = undoStack.removeLastOrNull() ?: return
        redoStack.add(action)
        val page = currentPages[action.pageIndex]
        if (action.isAdd) {
            currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData.filter { it !== action.stroke })
        } else {
            currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData + action.stroke)
        }
        activePageIndex = action.pageIndex
    }

    fun redo() {
        val action = redoStack.removeLastOrNull() ?: return
        undoStack.add(action)
        val page = currentPages[action.pageIndex]
        if (action.isAdd) {
            currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData + action.stroke)
        } else {
            currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData.filter { it !== action.stroke })
        }
        activePageIndex = action.pageIndex
    }

    fun isNoteBlank(): Boolean {
        if (currentTitle != "New Note" || currentPages.size > 1) return false
        val p = currentPages.firstOrNull() ?: return true
        return p.drawingData.isEmpty() && p.backgroundUri == null && p.paperStyle == 0 && p.canvasColor == -1
    }

    fun clearCanvas() {
        if (currentPages.isEmpty()) return
        currentPages[activePageIndex] = currentPages[activePageIndex].copy(drawingData = emptyList())
    }

    // --- ROUTING & SAVING ---

    fun openNoteForEditing(noteWP: NoteWithPages?) {
        selectedNoteWithPages = noteWP
        undoStack.clear()
        redoStack.clear()
        currentColor = Color.Black
        currentStrokeWidth = 8f
        currentEraserWidth = 20f
        currentTool = DrawingTool.PEN
        currentPages.clear()

        if (noteWP != null && noteWP.pages.isNotEmpty()) {
            currentTitle = noteWP.note.title
            currentPages.addAll(noteWP.pages)
            activePageIndex = 0
        } else {
            currentTitle = "New Note"
            currentPages.add(PageEntity(noteId = 0, pageNumber = 0))
            activePageIndex = 0
        }
        currentScreen = 1
    }

    fun closeEditing() {
        if (!isNoteBlank()) {
            val currentDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
            saveCurrentNote(currentDate)
        }
        selectedNoteWithPages = null
        currentScreen = 0
    }

    fun saveCurrentNote(date: String) {
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

    // --- BULK ACTIONS ---
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
}

class NotesViewModelFactory(private val dao: NoteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) return NotesViewModel(dao) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}