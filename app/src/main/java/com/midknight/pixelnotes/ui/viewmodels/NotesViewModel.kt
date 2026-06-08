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
import com.midknight.pixelnotes.domain.PointData
import com.midknight.pixelnotes.domain.StrokeData
import com.midknight.pixelnotes.domain.TextData
import com.midknight.pixelnotes.domain.isPointInPolygon
import com.midknight.pixelnotes.domain.isPointInRect
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DrawingTool { PEN, HIGHLIGHTER, ERASER, TEXT, SELECTION }

data class EditorAction(val pageIndex: Int, val stroke: StrokeData?, val text: TextData?, val isAdd: Boolean)

class NotesViewModel(private val dao: NoteDao) : ViewModel() {
    val notes = dao.getAllNotesWithPages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = dao.getAllFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customFonts = dao.getAllCustomFonts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var currentFolderFilter by mutableStateOf("Todas")
    var currentScreen by mutableIntStateOf(0)
    var selectedNoteWithPages by mutableStateOf<NoteWithPages?>(null)

    // Global Editor State
    var currentTitle by mutableStateOf("New Note")
    var currentColor by mutableStateOf(Color.Black)
    var currentStrokeWidth by mutableFloatStateOf(8f)
    var currentEraserWidth by mutableFloatStateOf(20f)
    var eraserType by mutableIntStateOf(0)
    var fingerDrawingEnabled by mutableStateOf(true)

    // Text Tool State
    var currentTextSize by mutableFloatStateOf(40f)
    var currentFontName by mutableStateOf("Default")

    // Selection Tool State
    var currentTool by mutableStateOf(DrawingTool.PEN)
        private set
    var selectionMode by mutableIntStateOf(0)
    val selectedStrokes = mutableStateListOf<StrokeData>()
    val selectedTexts = mutableStateListOf<TextData>()
    var selectionPageIndex by mutableIntStateOf(-1)

    // Multi-Page Continuous State
    val currentPages = mutableStateListOf<PageEntity>()
    var activePageIndex by mutableIntStateOf(0)

    private val undoStack = mutableListOf<EditorAction>()
    private val redoStack = mutableListOf<EditorAction>()

    // Bulk Actions
    val selectedNotes = mutableStateListOf<NoteWithPages>()
    var showDeleteDialog by mutableStateOf(false)
    var showMoveDialog by mutableStateOf(false)
    var showShareDialog by mutableStateOf(false)

    // --- TEXT ENGINE ---
    fun addTextToPage(pageIndex: Int, text: TextData) {
        commitSelection()
        val page = currentPages[pageIndex]
        currentPages[pageIndex] = page.copy(textData = page.textData + text)
        undoStack.add(EditorAction(pageIndex, null, text, true))
        redoStack.clear()
        activePageIndex = pageIndex
    }

    // --- SELECTION ENGINE (LASSO TOOL) ---
    fun setTool(tool: DrawingTool) {
        if (currentTool == DrawingTool.SELECTION && tool != DrawingTool.SELECTION) commitSelection()
        currentTool = tool
    }

    fun processSelection(pageIndex: Int, pathPoints: List<PointData>) {
        commitSelection()
        if (pathPoints.size < 3) return

        val page = currentPages[pageIndex]

        // Filtrar Trazos
        val strokesToSelect = mutableListOf<StrokeData>()
        val remainingStrokes = mutableListOf<StrokeData>()
        page.drawingData.forEach { stroke ->
            val isSelected = stroke.points.any { p ->
                if (selectionMode == 0) isPointInPolygon(p, pathPoints) else isPointInRect(p, pathPoints.first(), pathPoints.last())
            }
            if (isSelected && !stroke.isEraser) strokesToSelect.add(stroke) else remainingStrokes.add(stroke)
        }

        // Filtrar Textos
        val textsToSelect = mutableListOf<TextData>()
        val remainingTexts = mutableListOf<TextData>()
        page.textData.forEach { text ->
            val isSelected = if (selectionMode == 0) isPointInPolygon(PointData(text.x, text.y), pathPoints) else isPointInRect(PointData(text.x, text.y), pathPoints.first(), pathPoints.last())
            if (isSelected) textsToSelect.add(text) else remainingTexts.add(text)
        }

        if (strokesToSelect.isNotEmpty() || textsToSelect.isNotEmpty()) {
            selectedStrokes.addAll(strokesToSelect)
            selectedTexts.addAll(textsToSelect)
            selectionPageIndex = pageIndex
            currentPages[pageIndex] = page.copy(drawingData = remainingStrokes, textData = remainingTexts)
        }
    }

    fun moveSelection(dx: Float, dy: Float) {
        val movedStrokes = selectedStrokes.map { it.translate(dx, dy) }
        selectedStrokes.clear()
        selectedStrokes.addAll(movedStrokes)

        val movedTexts = selectedTexts.map { it.copy(x = it.x + dx, y = it.y + dy) }
        selectedTexts.clear()
        selectedTexts.addAll(movedTexts)
    }

    fun commitSelection() {
        if ((selectedStrokes.isNotEmpty() || selectedTexts.isNotEmpty()) && selectionPageIndex != -1) {
            val page = currentPages[selectionPageIndex]
            currentPages[selectionPageIndex] = page.copy(drawingData = page.drawingData + selectedStrokes, textData = page.textData + selectedTexts)
            selectedStrokes.forEach { undoStack.add(EditorAction(selectionPageIndex, it, null, true)) }
            selectedTexts.forEach { undoStack.add(EditorAction(selectionPageIndex, null, it, true)) }
            redoStack.clear()
            selectedStrokes.clear()
            selectedTexts.clear()
            selectionPageIndex = -1
        }
    }

    fun deleteSelection() {
        selectedStrokes.clear()
        selectedTexts.clear()
        selectionPageIndex = -1
    }

    fun changeSelectionColor(newColorArgb: Int) {
        val recoloredStrokes = selectedStrokes.map { it.copy(colorArgb = newColorArgb) }
        selectedStrokes.clear()
        selectedStrokes.addAll(recoloredStrokes)

        val recoloredTexts = selectedTexts.map { it.copy(colorArgb = newColorArgb) }
        selectedTexts.clear()
        selectedTexts.addAll(recoloredTexts)
    }

    // --- PAGE MANAGEMENT ---
    fun addNewPage() { commitSelection(); val newPage = PageEntity(noteId = selectedNoteWithPages?.note?.id ?: 0, pageNumber = currentPages.size); currentPages.add(newPage); activePageIndex = currentPages.lastIndex }
    fun deletePageAt(index: Int) { commitSelection(); if (currentPages.size > 1) { currentPages.removeAt(index); if (activePageIndex >= currentPages.size) activePageIndex = currentPages.size - 1 } }
    fun movePage(fromIndex: Int, toIndex: Int) { commitSelection(); if (fromIndex == toIndex) return; val page = currentPages.removeAt(fromIndex); currentPages.add(toIndex, page); if (activePageIndex == fromIndex) activePageIndex = toIndex else if (activePageIndex in minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex)) { if (fromIndex < toIndex) activePageIndex-- else activePageIndex++ } }
    fun updateActivePageBackground(uri: String?) { if(currentPages.isNotEmpty()) currentPages[activePageIndex] = currentPages[activePageIndex].copy(backgroundUri = uri) }
    fun updateActivePagePaperStyle(style: Int) { if(currentPages.isNotEmpty()) currentPages[activePageIndex] = currentPages[activePageIndex].copy(paperStyle = style) }
    fun updateActivePageCanvasColor(color: Int) { if(currentPages.isNotEmpty()) currentPages[activePageIndex] = currentPages[activePageIndex].copy(canvasColor = color) }

    // --- CONTINUOUS DRAWING ENGINE ---
    fun addStrokeToPage(pageIndex: Int, stroke: StrokeData) {
        val page = currentPages[pageIndex]
        currentPages[pageIndex] = page.copy(drawingData = page.drawingData + stroke)
        undoStack.add(EditorAction(pageIndex, stroke, null, true))
        redoStack.clear()
        activePageIndex = pageIndex
    }

    fun removeStrokeFromPage(pageIndex: Int, stroke: StrokeData) {
        val page = currentPages[pageIndex]
        currentPages[pageIndex] = page.copy(drawingData = page.drawingData.filter { it !== stroke })
        undoStack.add(EditorAction(pageIndex, stroke, null, false))
        redoStack.clear()
        activePageIndex = pageIndex
    }

    fun undo() {
        commitSelection()
        val action = undoStack.removeLastOrNull() ?: return
        redoStack.add(action)
        val page = currentPages[action.pageIndex]
        if (action.stroke != null) {
            if (action.isAdd) currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData.filter { it !== action.stroke })
            else currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData + action.stroke)
        } else if (action.text != null) {
            if (action.isAdd) currentPages[action.pageIndex] = page.copy(textData = page.textData.filter { it.id != action.text.id })
            else currentPages[action.pageIndex] = page.copy(textData = page.textData + action.text)
        }
        activePageIndex = action.pageIndex
    }

    fun redo() {
        commitSelection()
        val action = redoStack.removeLastOrNull() ?: return
        undoStack.add(action)
        val page = currentPages[action.pageIndex]
        if (action.stroke != null) {
            if (action.isAdd) currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData + action.stroke)
            else currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData.filter { it !== action.stroke })
        } else if (action.text != null) {
            if (action.isAdd) currentPages[action.pageIndex] = page.copy(textData = page.textData + action.text)
            else currentPages[action.pageIndex] = page.copy(textData = page.textData.filter { it.id != action.text.id })
        }
        activePageIndex = action.pageIndex
    }

    fun isNoteBlank(): Boolean {
        if (currentTitle != "New Note" || currentPages.size > 1) return false
        val p = currentPages.firstOrNull() ?: return true
        return p.drawingData.isEmpty() && p.textData.isEmpty() && p.backgroundUri == null && p.paperStyle == 0 && p.canvasColor == -1
    }

    fun clearCanvas() { if (currentPages.isNotEmpty()) currentPages[activePageIndex] = currentPages[activePageIndex].copy(drawingData = emptyList(), textData = emptyList()) }

    // --- ROUTING & SAVING ---
    fun openNoteForEditing(noteWP: NoteWithPages?) {
        selectedNoteWithPages = noteWP
        undoStack.clear()
        redoStack.clear()
        currentColor = Color.Black
        currentStrokeWidth = 8f
        currentEraserWidth = 20f
        currentTextSize = 40f
        currentFontName = "Default"
        setTool(DrawingTool.PEN)
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
        commitSelection()
        if (!isNoteBlank()) {
            val currentDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
            saveCurrentNote(currentDate)
        }
        selectedNoteWithPages = null
        currentScreen = 0
    }

    fun saveCurrentNote(date: String) {
        commitSelection()
        val targetFolder = selectedNoteWithPages?.note?.folder ?: if (currentFolderFilter == "Todas") "General" else currentFolderFilter
        val noteToSave = selectedNoteWithPages?.note?.copy(title = currentTitle, date = date) ?: Note(title = currentTitle, content = "", date = date, folder = targetFolder)

        viewModelScope.launch {
            val noteId = if (noteToSave.id == 0) dao.insertNote(noteToSave).toInt() else { dao.updateNote(noteToSave); noteToSave.id }
            dao.deletePagesByNoteId(noteId)
            currentPages.forEachIndexed { index, page -> dao.insertPage(page.copy(pageId = 0, noteId = noteId, pageNumber = index)) }
        }
    }

    // --- FONT MANAGEMENT & BULK ACTIONS ---
    fun importFont(context: android.content.Context, uri: android.net.Uri, fontName: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val fontsDir = java.io.File(context.filesDir, "custom_fonts")
                if (!fontsDir.exists()) fontsDir.mkdirs()
                var ext = ".ttf"
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use { if (it.moveToFirst()) { val displayName = it.getString(it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)); if (displayName.endsWith(".otf", true)) ext = ".otf" } }
                val fileName = "font_${System.currentTimeMillis()}$ext"
                val destFile = java.io.File(fontsDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input -> java.io.FileOutputStream(destFile).use { output -> input.copyTo(output) } }
                dao.insertCustomFont(com.midknight.pixelnotes.data.CustomFont(name = fontName, fileName = fileName))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteFont(context: android.content.Context, font: com.midknight.pixelnotes.data.CustomFont) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = java.io.File(java.io.File(context.filesDir, "custom_fonts"), font.fileName)
                if (file.exists()) file.delete()
                dao.deleteCustomFont(font)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun toggleSelection(note: NoteWithPages) { if (selectedNotes.any { it.note.id == note.note.id }) selectedNotes.removeAll { it.note.id == note.note.id } else selectedNotes.add(note) }
    fun clearSelection() { selectedNotes.clear() }
    fun deleteSelectedNotes() { viewModelScope.launch { selectedNotes.forEach { dao.deleteNote(it.note); dao.deletePagesByNoteId(it.note.id) }; clearSelection() } }
    fun moveSelectedNotes(newFolder: String) { viewModelScope.launch { selectedNotes.forEach { dao.updateNote(it.note.copy(folder = newFolder)) }; clearSelection() } }
    fun createFolder(name: String, parentPath: String?) { val path = if (parentPath == null) name else "$parentPath/$name"; viewModelScope.launch { dao.insertFolder(FolderEntity(path = path, name = name, parentPath = parentPath)) } }
    fun renameFolder(oldPath: String, newName: String) { val parentPath = oldPath.substringBeforeLast('/', ""); val newPath = if (parentPath.isEmpty()) newName else "$parentPath/$newName"; viewModelScope.launch { dao.renameFoldersCascade(oldPath, newPath, newName); dao.renameNotesFolderCascade(oldPath, newPath); if (currentFolderFilter == oldPath || currentFolderFilter.startsWith("$oldPath/")) currentFolderFilter = newPath + currentFolderFilter.removePrefix(oldPath) } }
    fun deleteFolder(path: String) { viewModelScope.launch { dao.deleteFolderCascade(path); dao.deleteNotesInFolderCascade(path); if (currentFolderFilter == path || currentFolderFilter.startsWith("$path/")) currentFolderFilter = "Todas" } }
}

class NotesViewModelFactory(private val dao: NoteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) return NotesViewModel(dao) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}