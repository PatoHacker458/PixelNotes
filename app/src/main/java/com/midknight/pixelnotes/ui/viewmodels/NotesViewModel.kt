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

    var currentTitle by mutableStateOf("New Note")
    var currentColor by mutableStateOf(Color.Black)
    var currentStrokeWidth by mutableFloatStateOf(8f)
    var currentEraserWidth by mutableFloatStateOf(20f)
    var eraserType by mutableIntStateOf(0)
    var fingerDrawingEnabled by mutableStateOf(true)

    var currentTextSize by mutableFloatStateOf(40f)
    var currentFontName by mutableStateOf("Default")

    var currentTool by mutableStateOf(DrawingTool.PEN)
        private set
    var selectionMode by mutableIntStateOf(0)
    val selectedStrokes = mutableStateListOf<StrokeData>()
    val selectedTexts = mutableStateListOf<TextData>()
    var selectionPageIndex by mutableIntStateOf(-1)

    val currentPages = mutableStateListOf<PageEntity>()
    var activePageIndex by mutableIntStateOf(0)

    private val undoStack = mutableListOf<EditorAction>()
    private val redoStack = mutableListOf<EditorAction>()

    val selectedNotes = mutableStateListOf<NoteWithPages>()
    var showDeleteDialog by mutableStateOf(false)
    var showMoveDialog by mutableStateOf(false)
    var showShareDialog by mutableStateOf(false)

    // PDF Import State
    var isImportingPdf by mutableStateOf(false)

    // --- TEXT & SELECTION ENGINE ---
    fun addTextToPage(pageIndex: Int, text: TextData) { commitSelection(); val page = currentPages[pageIndex]; currentPages[pageIndex] = page.copy(textData = page.textData + text); undoStack.add(EditorAction(pageIndex, null, text, true)); redoStack.clear(); activePageIndex = pageIndex }
    fun setTool(tool: DrawingTool) { if (currentTool == DrawingTool.SELECTION && tool != DrawingTool.SELECTION) commitSelection(); currentTool = tool }
    fun processSelection(pageIndex: Int, pathPoints: List<PointData>) {
        commitSelection()
        if (pathPoints.size < 3) return
        val page = currentPages[pageIndex]
        val strokesToSelect = mutableListOf<StrokeData>(); val remainingStrokes = mutableListOf<StrokeData>()
        page.drawingData.forEach { stroke -> val isSelected = stroke.points.any { p -> if (selectionMode == 0) isPointInPolygon(p, pathPoints) else isPointInRect(p, pathPoints.first(), pathPoints.last()) }; if (isSelected && !stroke.isEraser) strokesToSelect.add(stroke) else remainingStrokes.add(stroke) }
        val textsToSelect = mutableListOf<TextData>(); val remainingTexts = mutableListOf<TextData>()
        page.textData.forEach { text -> val isSelected = if (selectionMode == 0) isPointInPolygon(PointData(text.x, text.y), pathPoints) else isPointInRect(PointData(text.x, text.y), pathPoints.first(), pathPoints.last()); if (isSelected) textsToSelect.add(text) else remainingTexts.add(text) }
        if (strokesToSelect.isNotEmpty() || textsToSelect.isNotEmpty()) { selectedStrokes.addAll(strokesToSelect); selectedTexts.addAll(textsToSelect); selectionPageIndex = pageIndex; currentPages[pageIndex] = page.copy(drawingData = remainingStrokes, textData = remainingTexts) }
    }
    fun moveSelection(dx: Float, dy: Float) { selectedStrokes.clear(); selectedStrokes.addAll(selectedStrokes.map { it.translate(dx, dy) }); selectedTexts.clear(); selectedTexts.addAll(selectedTexts.map { it.copy(x = it.x + dx, y = it.y + dy) }) }
    fun commitSelection() { if ((selectedStrokes.isNotEmpty() || selectedTexts.isNotEmpty()) && selectionPageIndex != -1) { val page = currentPages[selectionPageIndex]; currentPages[selectionPageIndex] = page.copy(drawingData = page.drawingData + selectedStrokes, textData = page.textData + selectedTexts); selectedStrokes.forEach { undoStack.add(EditorAction(selectionPageIndex, it, null, true)) }; selectedTexts.forEach { undoStack.add(EditorAction(selectionPageIndex, null, it, true)) }; redoStack.clear(); selectedStrokes.clear(); selectedTexts.clear(); selectionPageIndex = -1 } }
    fun deleteSelection() { selectedStrokes.clear(); selectedTexts.clear(); selectionPageIndex = -1 }
    fun changeSelectionColor(newColorArgb: Int) { selectedStrokes.clear(); selectedStrokes.addAll(selectedStrokes.map { it.copy(colorArgb = newColorArgb) }); selectedTexts.clear(); selectedTexts.addAll(selectedTexts.map { it.copy(colorArgb = newColorArgb) }) }

    // --- PAGE MANAGEMENT ---
    fun addNewPage() { commitSelection(); currentPages.add(PageEntity(noteId = selectedNoteWithPages?.note?.id ?: 0, pageNumber = currentPages.size)); activePageIndex = currentPages.lastIndex }
    fun deletePageAt(index: Int) { commitSelection(); if (currentPages.size > 1) { currentPages.removeAt(index); if (activePageIndex >= currentPages.size) activePageIndex = currentPages.size - 1 } }
    fun movePage(fromIndex: Int, toIndex: Int) { commitSelection(); if (fromIndex == toIndex) return; val page = currentPages.removeAt(fromIndex); currentPages.add(toIndex, page); if (activePageIndex == fromIndex) activePageIndex = toIndex else if (activePageIndex in minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex)) { if (fromIndex < toIndex) activePageIndex-- else activePageIndex++ } }
    fun updateActivePageBackground(uri: String?) { if(currentPages.isNotEmpty()) currentPages[activePageIndex] = currentPages[activePageIndex].copy(backgroundUri = uri) }
    fun updateActivePagePaperStyle(style: Int) { if(currentPages.isNotEmpty()) currentPages[activePageIndex] = currentPages[activePageIndex].copy(paperStyle = style) }
    fun updateActivePageCanvasColor(color: Int) { if(currentPages.isNotEmpty()) currentPages[activePageIndex] = currentPages[activePageIndex].copy(canvasColor = color) }

    // --- CONTINUOUS DRAWING ENGINE ---
    fun addStrokeToPage(pageIndex: Int, stroke: StrokeData) { val page = currentPages[pageIndex]; currentPages[pageIndex] = page.copy(drawingData = page.drawingData + stroke); undoStack.add(EditorAction(pageIndex, stroke, null, true)); redoStack.clear(); activePageIndex = pageIndex }
    fun removeStrokeFromPage(pageIndex: Int, stroke: StrokeData) { val page = currentPages[pageIndex]; currentPages[pageIndex] = page.copy(drawingData = page.drawingData.filter { it !== stroke }); undoStack.add(EditorAction(pageIndex, stroke, null, false)); redoStack.clear(); activePageIndex = pageIndex }
    fun undo() { commitSelection(); val action = undoStack.removeLastOrNull() ?: return; redoStack.add(action); val page = currentPages[action.pageIndex]; if (action.stroke != null) { if (action.isAdd) currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData.filter { it !== action.stroke }) else currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData + action.stroke) } else if (action.text != null) { if (action.isAdd) currentPages[action.pageIndex] = page.copy(textData = page.textData.filter { it.id != action.text.id }) else currentPages[action.pageIndex] = page.copy(textData = page.textData + action.text) }; activePageIndex = action.pageIndex }
    fun redo() { commitSelection(); val action = redoStack.removeLastOrNull() ?: return; undoStack.add(action); val page = currentPages[action.pageIndex]; if (action.stroke != null) { if (action.isAdd) currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData + action.stroke) else currentPages[action.pageIndex] = page.copy(drawingData = page.drawingData.filter { it !== action.stroke }) } else if (action.text != null) { if (action.isAdd) currentPages[action.pageIndex] = page.copy(textData = page.textData + action.text) else currentPages[action.pageIndex] = page.copy(textData = page.textData.filter { it.id != action.text.id }) }; activePageIndex = action.pageIndex }
    fun isNoteBlank(): Boolean { if (currentTitle != "New Note" || currentPages.size > 1) return false; val p = currentPages.firstOrNull() ?: return true; return p.drawingData.isEmpty() && p.textData.isEmpty() && p.backgroundUri == null && p.paperStyle == 0 && p.canvasColor == -1 }

    // --- ROUTING & SAVING ---
    fun openNoteForEditing(noteWP: NoteWithPages?) {
        selectedNoteWithPages = noteWP; undoStack.clear(); redoStack.clear(); currentColor = Color.Black; currentStrokeWidth = 8f; currentEraserWidth = 20f; currentTextSize = 40f; currentFontName = "Default"; setTool(DrawingTool.PEN); currentPages.clear()
        if (noteWP != null && noteWP.pages.isNotEmpty()) { currentTitle = noteWP.note.title; currentPages.addAll(noteWP.pages); activePageIndex = 0 } else { currentTitle = "New Note"; currentPages.add(PageEntity(noteId = 0, pageNumber = 0)); activePageIndex = 0 }
        currentScreen = 1
    }

    fun closeEditing() {
        commitSelection()
        if (!isNoteBlank()) saveCurrentNote(SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date()))
        selectedNoteWithPages = null
        currentScreen = 0
    }

    fun saveCurrentNote(date: String) {
        commitSelection()
        val targetFolder = selectedNoteWithPages?.note?.folder ?: if (currentFolderFilter == "Todas") "General" else currentFolderFilter
        val noteToSave = selectedNoteWithPages?.note?.copy(title = currentTitle, date = date) ?: Note(title = currentTitle, content = "", date = date, folder = targetFolder)
        val pagesSnapshot = currentPages.toList()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val noteId = if (noteToSave.id == 0) dao.insertNote(noteToSave).toInt() else { dao.updateNote(noteToSave); noteToSave.id }
            dao.deletePagesByNoteId(noteId)
            val pagesToInsert = pagesSnapshot.mapIndexed { index, page -> page.copy(pageId = 0, noteId = noteId, pageNumber = index) }
            pagesToInsert.chunked(100).forEach { chunk -> dao.insertPages(chunk) }
        }
    }

    // --- FONT & STORAGE MANAGEMENT ---
    fun importFont(context: android.content.Context, uri: android.net.Uri, fontName: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val fontsDir = java.io.File(context.filesDir, "custom_fonts"); if (!fontsDir.exists()) fontsDir.mkdirs()
                var ext = ".ttf"
                val cursor = context.contentResolver.query(uri, null, null, null, null); cursor?.use { if (it.moveToFirst()) { val name = it.getString(it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)); if (name.endsWith(".otf", true)) ext = ".otf" } }
                val fileName = "font_${System.currentTimeMillis()}$ext"; val destFile = java.io.File(fontsDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input -> java.io.FileOutputStream(destFile).use { output -> input.copyTo(output) } }
                dao.insertCustomFont(com.midknight.pixelnotes.data.CustomFont(name = fontName, fileName = fileName))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteFont(context: android.content.Context, font: com.midknight.pixelnotes.data.CustomFont) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try { val file = java.io.File(java.io.File(context.filesDir, "custom_fonts"), font.fileName); if (file.exists()) file.delete(); dao.deleteCustomFont(font) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun recoverStorageSpace(context: android.content.Context, onComplete: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var deletedBytes = 0L
            try {
                val pdfDir = java.io.File(context.filesDir, "imported_pdfs")
                if (pdfDir.exists()) { pdfDir.listFiles()?.forEach { file -> deletedBytes += file.length(); file.delete() } }
            } catch (e: Exception) { e.printStackTrace() }
            val mbRecovered = deletedBytes / (1024 * 1024)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onComplete("Se han liberado $mbRecovered MB de archivos fantasma.") }
        }
    }

    // --- REAL-TIME PDF IMPORT ---
    fun importPdfDocument(context: android.content.Context, uri: android.net.Uri) {
        isImportingPdf = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var pdfName = "Imported Document"
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { if (it.moveToFirst()) pdfName = it.getString(it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)).removeSuffix(".pdf") }

            val importer = com.midknight.pixelnotes.domain.PdfImporter(context)
            val result = importer.importPdfRealTime(uri)

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (result != null) {
                    val pdfPath = result.first
                    val pageCount = result.second
                    commitSelection()
                    if (isNoteBlank()) {
                        currentTitle = pdfName
                        currentPages.clear()
                        for (i in 0 until pageCount) currentPages.add(PageEntity(noteId = selectedNoteWithPages?.note?.id ?: 0, pageNumber = i, backgroundUri = "$pdfPath?pdfPage=$i"))
                        activePageIndex = 0
                    } else {
                        val startIdx = currentPages.size
                        for (i in 0 until pageCount) currentPages.add(PageEntity(noteId = selectedNoteWithPages?.note?.id ?: 0, pageNumber = startIdx + i, backgroundUri = "$pdfPath?pdfPage=$i"))
                        activePageIndex = currentPages.lastIndex
                    }
                }
                isImportingPdf = false
            }
        }
    }

    // AQUÍ APLICAMOS TU IDEA DE BORRADO DE ARCHIVOS EN LA ELIMINACIÓN DE NOTA
    fun deleteSelectedNotes() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            selectedNotes.forEach { noteWP ->
                // Recolector de Basura Inteligente: Destruye el archivo PDF físico asociado a esta nota
                noteWP.pages.forEach { page ->
                    page.backgroundUri?.let { uri ->
                        if (uri.contains("?pdfPage=")) {
                            val path = uri.split("?pdfPage=")[0]
                            val file = java.io.File(path)
                            if (file.exists()) file.delete()
                        }
                    }
                }
                dao.deleteNote(noteWP.note)
                dao.deletePagesByNoteId(noteWP.note.id)
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                clearSelection()
            }
        }
    }

    fun toggleSelection(note: NoteWithPages) { if (selectedNotes.any { it.note.id == note.note.id }) selectedNotes.removeAll { it.note.id == note.note.id } else selectedNotes.add(note) }
    fun clearSelection() { selectedNotes.clear() }
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