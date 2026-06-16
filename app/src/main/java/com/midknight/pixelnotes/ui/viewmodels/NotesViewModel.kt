package com.midknight.pixelnotes.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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

data class EditorAction(val pageIndex: Int, val stroke: StrokeData?, val text: TextData?, val image: com.midknight.pixelnotes.domain.ImageData?, val audio: com.midknight.pixelnotes.domain.AudioData? = null, val isAdd: Boolean)
class NotesViewModel(private val dao: NoteDao, private val context: android.content.Context) : ViewModel() {
    val notes = dao.getAllNotesWithPages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = dao.getAllFolders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customFonts = dao.getAllCustomFonts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val prefs = context.getSharedPreferences("pixel_notes_prefs", android.content.Context.MODE_PRIVATE)

    var currentFolderFilter by mutableStateOf("All Notes")
    var currentScreen by mutableIntStateOf(0)
    var selectedNoteWithPages by mutableStateOf<NoteWithPages?>(null)
    var isCurrentNoteInfinite by mutableStateOf(false)
    var cameraResetTrigger by mutableIntStateOf(0)
    var cameraPan by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
    var cameraZoom by mutableFloatStateOf(1f)

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
    val selectedImages = mutableStateListOf<com.midknight.pixelnotes.domain.ImageData>()
    var selectionPageIndex by mutableIntStateOf(-1)

    private val clipboardStrokes = mutableListOf<StrokeData>()
    private val clipboardTexts = mutableListOf<TextData>()
    private val clipboardImages = mutableListOf<com.midknight.pixelnotes.domain.ImageData>()

    val isSelectionEmpty get() = selectedStrokes.isEmpty() && selectedTexts.isEmpty() && selectedImages.isEmpty()
    val isClipboardEmpty get() = clipboardStrokes.isEmpty() && clipboardTexts.isEmpty() && clipboardImages.isEmpty()

    val currentPages = mutableStateListOf<PageEntity>()
    var activePageIndex by mutableIntStateOf(0)

    private val undoStack = mutableListOf<EditorAction>()
    private val redoStack = mutableListOf<EditorAction>()

    val selectedNotes = mutableStateListOf<NoteWithPages>()
    var showDeleteDialog by mutableStateOf(false)
    var showMoveDialog by mutableStateOf(false)
    var showShareDialog by mutableStateOf(false)
    var showEmptyTrashDialog by mutableStateOf(false)

    var isImportingPdf by mutableStateOf(false)
    var pendingCameraUri by mutableStateOf<android.net.Uri?>(null)
    var isRecording by mutableStateOf(false)
    var recordingDuration by mutableLongStateOf(0L)
    var recordingAmplitude by mutableFloatStateOf(0f)
    private var mediaRecorder: android.media.MediaRecorder? = null
    private var currentRecordingFile: java.io.File? = null
    private var recordingJob: kotlinx.coroutines.Job? = null
    var isPlaying by mutableStateOf(false)
    var playbackProgress by mutableFloatStateOf(0f)
    var currentAudioDuration by mutableLongStateOf(0L)
    private var mediaPlayer: android.media.MediaPlayer? = null
    var activeAudioUri by mutableStateOf<String?>(null)
    var activeAudioId by mutableStateOf<String?>(null)
    var activeAudioPageIndex by mutableIntStateOf(-1)
    private var playbackJob: kotlinx.coroutines.Job? = null
    var isSyncing by mutableStateOf(false)
    var userEmail by mutableStateOf<String?>(null)
    var userName by mutableStateOf<String?>(null)
    var userPhotoUri by mutableStateOf<String?>(null)
    val isUserSignedIn get() = userEmail != null

    init {
        userEmail = prefs?.getString("user_email", null)
        userName = prefs?.getString("user_name", null)
        userPhotoUri = prefs?.getString("user_photo", null)
    }

    fun createImageUri(context: android.content.Context): android.net.Uri? {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "pixel_note_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    fun addFloatingImageToPage(pageIndex: Int, uri: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val imagesDir = java.io.File(context.filesDir, "inserted_images")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                
                val sourceUri = android.net.Uri.parse(uri)
                val fileName = "img_${System.currentTimeMillis()}.jpg"
                val destFile = java.io.File(imagesDir, fileName)
                
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(input)
                    if (originalBitmap != null) {
                        val maxDim = 2048f
                        val scale = if (originalBitmap.width > maxDim || originalBitmap.height > maxDim) {
                            maxDim / maxOf(originalBitmap.width, originalBitmap.height).toFloat()
                        } else 1f
                        
                        val finalBitmap = if (scale < 1f) {
                            android.graphics.Bitmap.createScaledBitmap(
                                originalBitmap, 
                                (originalBitmap.width * scale).toInt(), 
                                (originalBitmap.height * scale).toInt(), 
                                true
                            )
                        } else originalBitmap

                        java.io.FileOutputStream(destFile).use { output ->
                            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, output)
                        }
                        
                        if (finalBitmap !== originalBitmap) finalBitmap.recycle()
                        originalBitmap.recycle()
                    }
                }
                
                // Store only the relative internal URI
                val internalUri = "internal://$fileName"
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    setTool(DrawingTool.SELECTION)
                    val newImage = com.midknight.pixelnotes.domain.ImageData(
                        x = 100f, y = 100f, width = 600f, height = 600f, uri = internalUri
                    )
                    selectedImages.add(newImage)
                    selectionPageIndex = pageIndex
                    activePageIndex = pageIndex
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addTextToPage(pageIndex: Int, text: TextData) {
        setTool(DrawingTool.SELECTION)
        selectedTexts.add(text)
        selectionPageIndex = pageIndex
        activePageIndex = pageIndex
    }

    fun setTool(tool: DrawingTool) { if (currentTool == DrawingTool.SELECTION && tool != DrawingTool.SELECTION) commitSelection(); currentTool = tool }

    fun processSelection(pageIndex: Int, pathPoints: List<PointData>) {
        commitSelection()
        if (pathPoints.size < 3) return
        val page = currentPages[pageIndex]

        val textPaint = android.text.TextPaint().apply { textSize = 40f } // Temporary paint for measurement

        val strokesToSelect = mutableListOf<StrokeData>()
        val remainingStrokes = mutableListOf<StrokeData>()
        page.drawingData.forEach { stroke ->
            val isSelected = stroke.points.any { p -> if (selectionMode == 0) isPointInPolygon(p, pathPoints) else isPointInRect(p, pathPoints.first(), pathPoints.last()) }
            if (isSelected && !stroke.isEraser) strokesToSelect.add(stroke) else remainingStrokes.add(stroke)
        }

        val textsToSelect = mutableListOf<TextData>()
        val remainingTexts = mutableListOf<TextData>()
        page.textData.forEach { text ->
            textPaint.textSize = text.fontSize
            val width = text.maxWidth.toInt().coerceAtLeast(1)
            val layout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.text.StaticLayout.Builder.obtain(text.text, 0, text.text.length, textPaint, width).build()
            } else {
                @Suppress("DEPRECATION")
                android.text.StaticLayout(text.text, textPaint, width, android.text.Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
            }
            val textHeight = layout.height.toFloat()
            var actualMaxWidth = 0f
            for (i in 0 until layout.lineCount) {
                actualMaxWidth = maxOf(actualMaxWidth, layout.getLineWidth(i))
            }
            val corners = listOf(PointData(text.x, text.y), PointData(text.x + actualMaxWidth, text.y), PointData(text.x, text.y + textHeight), PointData(text.x + actualMaxWidth, text.y + textHeight))
            var isSelected = corners.any { p -> if (selectionMode == 0) isPointInPolygon(p, pathPoints) else isPointInRect(p, pathPoints.first(), pathPoints.last()) }
            if (!isSelected) { isSelected = pathPoints.any { p -> p.x >= text.x && p.x <= text.x + actualMaxWidth && p.y >= text.y && p.y <= text.y + textHeight } }
            if (isSelected) textsToSelect.add(text) else remainingTexts.add(text)
        }

        val imagesToSelect = mutableListOf<com.midknight.pixelnotes.domain.ImageData>()
        val remainingImages = mutableListOf<com.midknight.pixelnotes.domain.ImageData>()
        page.imageData.forEach { img ->
            val corners = listOf(PointData(img.x, img.y), PointData(img.x + img.width, img.y), PointData(img.x, img.y + img.height), PointData(img.x + img.width, img.y + img.height))
            var isSelected = corners.any { p -> if (selectionMode == 0) isPointInPolygon(p, pathPoints) else isPointInRect(p, pathPoints.first(), pathPoints.last()) }
            if (!isSelected) { isSelected = pathPoints.any { p -> p.x >= img.x && p.x <= img.x + img.width && p.y >= img.y && p.y <= img.y + img.height } }
            if (isSelected) imagesToSelect.add(img) else remainingImages.add(img)
        }

        if (strokesToSelect.isNotEmpty() || textsToSelect.isNotEmpty() || imagesToSelect.isNotEmpty()) {
            selectedStrokes.addAll(strokesToSelect); selectedTexts.addAll(textsToSelect); selectedImages.addAll(imagesToSelect)
            selectionPageIndex = pageIndex
            currentPages[pageIndex] = page.copy(drawingData = remainingStrokes, textData = remainingTexts, imageData = remainingImages)
        }
    }

    fun moveSelection(dx: Float, dy: Float) {
        val movedStrokes = selectedStrokes.map { it.translate(dx, dy) }
        val movedTexts = selectedTexts.map { it.copy(x = it.x + dx, y = it.y + dy) }
        val movedImages = selectedImages.map { it.copy(x = it.x + dx, y = it.y + dy) }
        selectedStrokes.clear(); selectedStrokes.addAll(movedStrokes)
        selectedTexts.clear(); selectedTexts.addAll(movedTexts)
        selectedImages.clear(); selectedImages.addAll(movedImages)
    }

    fun scaleSelection(scaleFactor: Float, pivotX: Float, pivotY: Float) {
        if (scaleFactor <= 0f || scaleFactor.isNaN()) return
        val scaledStrokes = selectedStrokes.map { stroke -> stroke.copy(strokeWidth = stroke.strokeWidth * scaleFactor, points = stroke.points.map { p -> PointData(pivotX + (p.x - pivotX) * scaleFactor, pivotY + (p.y - pivotY) * scaleFactor) }) }
        selectedStrokes.clear(); selectedStrokes.addAll(scaledStrokes)
        val scaledTexts = selectedTexts.map { text -> text.copy(x = pivotX + (text.x - pivotX) * scaleFactor, y = pivotY + (text.y - pivotY) * scaleFactor, fontSize = text.fontSize * scaleFactor, maxWidth = text.maxWidth * scaleFactor) }
        selectedTexts.clear(); selectedTexts.addAll(scaledTexts)
        val scaledImages = selectedImages.map { img -> img.copy(x = pivotX + (img.x - pivotX) * scaleFactor, y = pivotY + (img.y - pivotY) * scaleFactor, width = img.width * scaleFactor, height = img.height * scaleFactor) }
        selectedImages.clear(); selectedImages.addAll(scaledImages)
    }

    fun commitSelection() {
        if ((selectedStrokes.isNotEmpty() || selectedTexts.isNotEmpty() || selectedImages.isNotEmpty()) && selectionPageIndex != -1) {
            val page = currentPages[selectionPageIndex]
            currentPages[selectionPageIndex] = page.copy(drawingData = page.drawingData + selectedStrokes, textData = page.textData + selectedTexts, imageData = page.imageData + selectedImages)
            selectedStrokes.forEach { undoStack.add(EditorAction(selectionPageIndex, it, null, null, null, true)) }
            selectedTexts.forEach { undoStack.add(EditorAction(selectionPageIndex, null, it, null, null, true)) }
            selectedImages.forEach { undoStack.add(EditorAction(selectionPageIndex, null, null, it, null, true)) }
            redoStack.clear(); selectedStrokes.clear(); selectedTexts.clear(); selectedImages.clear(); selectionPageIndex = -1
        }
    }

    fun dropSelectionToPage(targetPageIndex: Int) {
        if ((selectedStrokes.isNotEmpty() || selectedTexts.isNotEmpty() || selectedImages.isNotEmpty()) && selectionPageIndex != -1) {
            val targetPage = currentPages[targetPageIndex]
            currentPages[targetPageIndex] = targetPage.copy(
                drawingData = targetPage.drawingData + selectedStrokes,
                textData = targetPage.textData + selectedTexts,
                imageData = targetPage.imageData + selectedImages
            )
            selectedStrokes.forEach { undoStack.add(EditorAction(targetPageIndex, it, null, null, null, true)) }
            selectedTexts.forEach { undoStack.add(EditorAction(targetPageIndex, null, it, null, null, true)) }
            selectedImages.forEach { undoStack.add(EditorAction(targetPageIndex, null, null, it, null, true)) }
            redoStack.clear(); selectedStrokes.clear(); selectedTexts.clear(); selectedImages.clear(); selectionPageIndex = -1
        }
    }

    fun deleteSelection() {
        if (selectionPageIndex != -1) {
            selectedStrokes.forEach { undoStack.add(EditorAction(selectionPageIndex, it, null, null, null, false)) }
            selectedTexts.forEach { undoStack.add(EditorAction(selectionPageIndex, null, it, null, null, false)) }
            selectedImages.forEach { undoStack.add(EditorAction(selectionPageIndex, null, null, it, null, false)) }
            redoStack.clear()
        }
        selectedStrokes.clear(); selectedTexts.clear(); selectedImages.clear(); selectionPageIndex = -1
    }
    fun changeSelectionColor(newColorArgb: Int) {
        val coloredStrokes = selectedStrokes.map { it.copy(colorArgb = newColorArgb) }
        val coloredTexts = selectedTexts.map { it.copy(colorArgb = newColorArgb) }
        selectedStrokes.clear(); selectedStrokes.addAll(coloredStrokes)
        selectedTexts.clear(); selectedTexts.addAll(coloredTexts)
    }

    fun changeSelectionFont(newFontName: String) {
        val updatedTexts = selectedTexts.map { it.copy(fontName = newFontName) }
        selectedTexts.clear(); selectedTexts.addAll(updatedTexts)
    }

    fun copySelection() {
        if (selectedStrokes.isEmpty() && selectedTexts.isEmpty() && selectedImages.isEmpty()) return
        clipboardStrokes.clear(); clipboardStrokes.addAll(selectedStrokes.map { it.copy() })
        clipboardTexts.clear(); clipboardTexts.addAll(selectedTexts.map { it.copy() })
        clipboardImages.clear(); clipboardImages.addAll(selectedImages.map { it.copy() })
    }

    fun pasteSelection(pageIndex: Int, targetX: Float? = null, targetY: Float? = null) {
        if (clipboardStrokes.isEmpty() && clipboardTexts.isEmpty() && clipboardImages.isEmpty()) return
        if (pageIndex !in currentPages.indices) return
        
        // Commit current selection before pasting new one
        commitSelection()
        
        val offset = 50f
        val dx: Float
        val dy: Float
        
        if (targetX != null && targetY != null) {
            val strokeMinX = clipboardStrokes.flatMap { it.points }.minOfOrNull { it.x } ?: Float.MAX_VALUE
            val textMinX = clipboardTexts.minOfOrNull { it.x } ?: Float.MAX_VALUE
            val imageMinX = clipboardImages.minOfOrNull { it.x } ?: Float.MAX_VALUE
            val minX = minOf(strokeMinX, textMinX, imageMinX).let { if (it == Float.MAX_VALUE) 0f else it }

            val strokeMinY = clipboardStrokes.flatMap { it.points }.minOfOrNull { it.y } ?: Float.MAX_VALUE
            val textMinY = clipboardTexts.minOfOrNull { it.y } ?: Float.MAX_VALUE
            val imageMinY = clipboardImages.minOfOrNull { it.y } ?: Float.MAX_VALUE
            val minY = minOf(strokeMinY, textMinY, imageMinY).let { if (it == Float.MAX_VALUE) 0f else it }
            
            dx = targetX - minX
            dy = targetY - minY
        } else {
            dx = offset
            dy = offset
        }

        val pastedStrokes = clipboardStrokes.map { it.copy(points = it.points.map { p -> p.copy(x = p.x + dx, y = p.y + dy) }) }
        val pastedTexts = clipboardTexts.map { it.copy(id = java.util.UUID.randomUUID().toString(), x = it.x + dx, y = it.y + dy) }
        val pastedImages = clipboardImages.map { it.copy(id = java.util.UUID.randomUUID().toString(), x = it.x + dx, y = it.y + dy) }

        // Auto-select pasted items
        selectedStrokes.addAll(pastedStrokes)
        selectedTexts.addAll(pastedTexts)
        selectedImages.addAll(pastedImages)
        selectionPageIndex = pageIndex
        activePageIndex = pageIndex
    }

    fun duplicateSelection() {
        if (selectionPageIndex == -1) return
        copySelection()
        pasteSelection(selectionPageIndex)
    }

    fun addNewPage() { commitSelection(); currentPages.add(PageEntity(noteId = selectedNoteWithPages?.note?.id ?: 0, pageNumber = currentPages.size)); activePageIndex = currentPages.lastIndex }
    fun deletePageAt(index: Int) { commitSelection(); if (currentPages.size > 1) { currentPages.removeAt(index); if (activePageIndex >= currentPages.size) activePageIndex = currentPages.size - 1 } }
    fun movePage(fromIndex: Int, toIndex: Int) { commitSelection(); if (fromIndex == toIndex) return; val page = currentPages.removeAt(fromIndex); currentPages.add(toIndex, page); if (activePageIndex == fromIndex) activePageIndex = toIndex else if (activePageIndex in minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex)) { if (fromIndex < toIndex) activePageIndex-- else activePageIndex++ } }
    fun updateActivePageBackground(uri: String?) { if(currentPages.isNotEmpty()) currentPages[activePageIndex] = currentPages[activePageIndex].copy(backgroundUri = uri) }
    fun updateActivePagePaperStyle(style: Int) { if(currentPages.isNotEmpty()) currentPages[activePageIndex] = currentPages[activePageIndex].copy(paperStyle = style) }
    fun updateActivePageCanvasColor(color: Int) { if(currentPages.isNotEmpty()) currentPages[activePageIndex] = currentPages[activePageIndex].copy(canvasColor = color) }

    fun addStrokeToPage(pageIndex: Int, stroke: StrokeData) { val page = currentPages[pageIndex]; currentPages[pageIndex] = page.copy(drawingData = page.drawingData + stroke); undoStack.add(EditorAction(pageIndex, stroke, null, null, null, true)); redoStack.clear(); activePageIndex = pageIndex }
    fun removeStrokeFromPage(pageIndex: Int, stroke: StrokeData) { val page = currentPages[pageIndex]; currentPages[pageIndex] = page.copy(drawingData = page.drawingData.filter { it !== stroke }); undoStack.add(EditorAction(pageIndex, stroke, null, null, null, false)); redoStack.clear(); activePageIndex = pageIndex }
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
        } else if (action.image != null) {
            if (action.isAdd) currentPages[action.pageIndex] = page.copy(imageData = page.imageData.filter { it.id != action.image.id })
            else currentPages[action.pageIndex] = page.copy(imageData = page.imageData + action.image)
        } else if (action.audio != null) {
            if (action.isAdd) currentPages[action.pageIndex] = page.copy(audioData = page.audioData.filter { it.id != action.audio.id })
            else currentPages[action.pageIndex] = page.copy(audioData = page.audioData + action.audio)
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
        } else if (action.image != null) {
            if (action.isAdd) currentPages[action.pageIndex] = page.copy(imageData = page.imageData + action.image)
            else currentPages[action.pageIndex] = page.copy(imageData = page.imageData.filter { it.id != action.image.id })
        } else if (action.audio != null) {
            if (action.isAdd) currentPages[action.pageIndex] = page.copy(audioData = page.audioData + action.audio)
            else currentPages[action.pageIndex] = page.copy(audioData = page.audioData.filter { it.id != action.audio.id })
        }
        activePageIndex = action.pageIndex
    }
    fun isNoteBlank(): Boolean { if (currentTitle != "New Note" || currentPages.size > 1) return false; val p = currentPages.firstOrNull() ?: return true; return p.drawingData.isEmpty() && p.textData.isEmpty() && p.imageData.isEmpty() && p.backgroundUri == null && p.paperStyle == 0 && p.canvasColor == -1 }

    fun navigateBack(): Boolean {
        if (currentScreen == 1) {
            closeEditing()
            return true
        }
        if (currentScreen == 2) {
            currentScreen = 0
            return true
        }
        if (currentScreen == 0) {
            if (currentFolderFilter == "Trash") {
                currentFolderFilter = "All Notes"
                return true
            }
            if (currentFolderFilter != "All Notes") {
                if (currentFolderFilter.contains("/")) {
                    currentFolderFilter = currentFolderFilter.substringBeforeLast("/")
                } else {
                    currentFolderFilter = "All Notes"
                }
                return true
            }
        }
        return false
    }

    fun navigateHome() {
        currentScreen = 0
        currentFolderFilter = "All Notes"
    }

    fun openNoteForEditing(noteWP: NoteWithPages?, isInfinite: Boolean = false) {
        selectedNoteWithPages = noteWP; undoStack.clear(); redoStack.clear(); currentColor = Color.Black; currentStrokeWidth = 8f; currentEraserWidth = 20f; currentTextSize = 40f; currentFontName = "Default"; setTool(DrawingTool.PEN); currentPages.clear()
        isCurrentNoteInfinite = noteWP?.note?.isInfinite ?: isInfinite
        cameraResetTrigger++
        cameraPan = androidx.compose.ui.geometry.Offset.Zero
        cameraZoom = 1f
        if (noteWP != null && noteWP.pages.isNotEmpty()) { currentTitle = noteWP.note.title; currentPages.addAll(noteWP.pages); activePageIndex = 0 } else { currentTitle = "New Note"; currentPages.add(PageEntity(noteId = 0, pageNumber = 0)); activePageIndex = 0 }
        currentScreen = 1
    }

    fun closeEditing() {
        commitSelection()
        if (!isNoteBlank()) {
            val date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
            val capturedNote = selectedNoteWithPages?.note
            val capturedPages = currentPages.toList()
            val capturedTitle = currentTitle
            val capturedIsInfinite = isCurrentNoteInfinite
            val capturedFolder = capturedNote?.folder ?: if (currentFolderFilter == "All Notes" || currentFolderFilter == "Trash") "General" else currentFolderFilter
            
            viewModelScope.launch {
                saveCurrentNoteToDb(
                    date = date,
                    noteOverride = capturedNote,
                    pagesOverride = capturedPages,
                    titleOverride = capturedTitle,
                    isInfiniteOverride = capturedIsInfinite,
                    folderOverride = capturedFolder
                )
            }
        }
        selectedNoteWithPages = null
        currentScreen = 0
    }

    fun saveCurrentNote(date: String) {
        viewModelScope.launch {
            saveCurrentNoteToDb(date)
        }
    }

    private suspend fun saveCurrentNoteToDb(
        date: String,
        noteOverride: Note? = null,
        pagesOverride: List<PageEntity>? = null,
        titleOverride: String? = null,
        isInfiniteOverride: Boolean? = null,
        folderOverride: String? = null
    ) {
        commitSelection()
        val timestamp = System.currentTimeMillis()
        val noteToSave = (noteOverride ?: selectedNoteWithPages?.note)?.copy(
            title = titleOverride ?: currentTitle,
            date = date,
            folder = folderOverride ?: (selectedNoteWithPages?.note?.folder ?: if (currentFolderFilter == "All Notes" || currentFolderFilter == "Trash") "General" else currentFolderFilter),
            isInfinite = isInfiniteOverride ?: isCurrentNoteInfinite,
            updatedAt = timestamp
        ) ?: Note(
            title = titleOverride ?: currentTitle,
            content = "",
            date = date,
            folder = folderOverride ?: if (currentFolderFilter == "All Notes" || currentFolderFilter == "Trash") "General" else currentFolderFilter,
            isInfinite = isInfiniteOverride ?: isCurrentNoteInfinite,
            updatedAt = timestamp
        )
        
        val pagesSnapshot = pagesOverride ?: currentPages.toList()

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val noteId = if (noteToSave.id == 0) {
                val newId = dao.insertNote(noteToSave).toInt()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (currentScreen == 1) {
                        selectedNoteWithPages = NoteWithPages(noteToSave.copy(id = newId), pagesSnapshot)
                    }
                }
                newId
            } else {
                dao.updateNote(noteToSave)
                noteToSave.id
            }
            dao.deletePagesByNoteId(noteId)
            val pagesToInsert = pagesSnapshot.mapIndexed { index, page -> page.copy(pageId = 0, noteId = noteId, pageNumber = index) }
            pagesToInsert.chunked(100).forEach { chunk -> dao.insertPages(chunk) }
        }
    }

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
            val freedBytes = pruneOrphanFiles()
            val mbRecovered = freedBytes / (1024 * 1024)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { 
                onComplete("Freed $mbRecovered MB of ghost files.") 
            }
        }
    }

    private suspend fun pruneOrphanFiles(): Long {
        var deletedBytes = 0L
        try {
            val allNotesWP: List<com.midknight.pixelnotes.data.NoteWithPages> = dao.getNotesWithPagesSync()
            val activeFiles = mutableSetOf<String>()
            
            allNotesWP.forEach { noteWP ->
                noteWP.pages.forEach { page ->
                    // 1. Collect PDFs and Background Images
                    page.backgroundUri?.let { uri ->
                        if (uri.contains("imported_pdfs/")) {
                            val path = uri.split("?pdfPage=")[0]
                            activeFiles.add(java.io.File(path).name)
                        } else if (uri.contains("inserted_images/")) {
                            activeFiles.add(uri.substringAfterLast("/"))
                        }
                    }
                    // 2. Collect Images
                    page.imageData.forEach { img ->
                        if (img.uri.startsWith("internal://")) {
                            activeFiles.add(img.uri.removePrefix("internal://"))
                        } else if (img.uri.contains("inserted_images/")) {
                            activeFiles.add(img.uri.substringAfterLast("/"))
                        }
                    }
                    // 3. Collect Audio
                    page.audioData.forEach { audio ->
                        activeFiles.add(java.io.File(audio.uri).name)
                    }
                }
            }

            // Prune folders
            val dirs = listOf("inserted_images", "audio_notes", "imported_pdfs")
            dirs.forEach { dirName ->
                val dir = java.io.File(context.filesDir, dirName)
                if (dir.exists()) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && !activeFiles.contains(file.name)) {
                            deletedBytes += file.length()
                            file.delete()
                            android.util.Log.d("NotesViewModel", "Pruned orphan: ${file.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return deletedBytes
    }

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
                        currentTitle = pdfName; currentPages.clear()
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

    fun moveToTrash() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            selectedNotes.forEach { dao.updateNote(it.note.copy(inTrash = true, updatedAt = System.currentTimeMillis())) }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { clearSelection() }
        }
    }

    fun restoreFromTrash() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            selectedNotes.forEach { dao.updateNote(it.note.copy(inTrash = false, updatedAt = System.currentTimeMillis())) }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { clearSelection() }
        }
    }

    fun permanentlyDeleteSelected() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            selectedNotes.forEach { noteWP ->
                noteWP.pages.forEach { page -> page.backgroundUri?.let { uri -> if (uri.contains("?pdfPage=")) { val path = uri.split("?pdfPage=")[0]; val file = java.io.File(path); if (file.exists()) file.delete() } } }
                dao.deleteNote(noteWP.note)
                dao.deletePagesByNoteId(noteWP.note.id)
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { clearSelection() }
        }
    }

    fun deleteFolder(path: String) {
        viewModelScope.launch {
            dao.trashNotesInFolderCascade(path)
            dao.deleteFolderCascade(path)
            if (currentFolderFilter == path || currentFolderFilter.startsWith("$path/")) currentFolderFilter = "All Notes"
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val trashedNotes = dao.getTrashedNotesSync()
            trashedNotes.forEach { noteWP ->
                noteWP.pages.forEach { page -> page.backgroundUri?.let { uri -> if (uri.contains("?pdfPage=")) { val path = uri.split("?pdfPage=")[0]; val file = java.io.File(path); if (file.exists()) file.delete() } } }
                dao.deleteNote(noteWP.note)
                dao.deletePagesByNoteId(noteWP.note.id)
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { showEmptyTrashDialog = false }
        }
    }

    fun toggleSelection(note: NoteWithPages) { if (selectedNotes.any { it.note.id == note.note.id }) selectedNotes.removeAll { it.note.id == note.note.id } else selectedNotes.add(note) }
    fun clearSelection() { selectedNotes.clear() }
    fun moveSelectedNotes(newFolder: String) { viewModelScope.launch { selectedNotes.forEach { dao.updateNote(it.note.copy(folder = newFolder)) }; clearSelection() } }
    fun createFolder(name: String, parentPath: String?) { val path = if (parentPath == null) name else "$parentPath/$name"; viewModelScope.launch { dao.insertFolder(FolderEntity(path = path, name = name, parentPath = parentPath)) } }
    fun renameFolder(oldPath: String, newName: String) { val parentPath = oldPath.substringBeforeLast('/', ""); val newPath = if (parentPath.isEmpty()) newName else "$parentPath/$newName"; viewModelScope.launch { dao.renameFoldersCascade(oldPath, newPath, newName); dao.renameNotesFolderCascade(oldPath, newPath); if (currentFolderFilter == oldPath || currentFolderFilter.startsWith("$oldPath/")) currentFolderFilter = newPath + currentFolderFilter.removePrefix(oldPath) } }

    fun startRecording(context: android.content.Context) {
        val audioDir = java.io.File(context.filesDir, "audio_notes")
        if (!audioDir.exists()) audioDir.mkdirs()

        val fileName = "audio_${System.currentTimeMillis()}.m4a"
        currentRecordingFile = java.io.File(audioDir, fileName)

        mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.media.MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            android.media.MediaRecorder()
        }.apply {
            setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            setOutputFile(currentRecordingFile?.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                startRecordingTimer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startRecordingTimer() {
        recordingJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (isRecording) {
                recordingDuration = System.currentTimeMillis() - startTime
                recordingAmplitude = try { mediaRecorder?.maxAmplitude?.toFloat() ?: 0f } catch (e: Exception) { 0f }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        recordingJob?.cancel()
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null

        currentRecordingFile?.let { file ->
            val audioData = com.midknight.pixelnotes.domain.AudioData(
                id = java.util.UUID.randomUUID().toString(),
                uri = file.absolutePath,
                x = 50f,
                y = 50f,
                durationMs = recordingDuration
            )
            addAudioToPage(activePageIndex, audioData)
        }
        currentRecordingFile = null
    }

    fun cancelRecording() {
        if (!isRecording) return
        isRecording = false
        recordingJob?.cancel()
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) { e.printStackTrace() }
        mediaRecorder = null
        currentRecordingFile?.delete()
        currentRecordingFile = null
    }

    fun playAudio(uri: String, id: String, pageIndex: Int) {
        if (activeAudioUri == uri && activeAudioId == id) {
            if (isPlaying) {
                pauseAudio()
            } else {
                resumeAudio()
            }
            return
        }
        stopAudio()
        activeAudioUri = uri
        activeAudioId = id
        activeAudioPageIndex = pageIndex
        isPlaying = true
        mediaPlayer = android.media.MediaPlayer().apply {
            try {
                setDataSource(uri)
                prepare()
                currentAudioDuration = duration.toLong()
                start()
                startPlaybackTimer()
                setOnCompletionListener { stopAudio() }
            } catch (e: java.io.IOException) {
                e.printStackTrace()
                stopAudio()
            }
        }
    }

    private fun startPlaybackTimer() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isPlaying) {
                mediaPlayer?.let {
                    playbackProgress = it.currentPosition.toFloat() / it.duration.toFloat()
                }
                kotlinx.coroutines.delay(50)
            }
        }
    }

    fun pauseAudio() {
        isPlaying = false
        playbackJob?.cancel()
        mediaPlayer?.pause()
    }

    fun resumeAudio() {
        isPlaying = true
        mediaPlayer?.start()
        startPlaybackTimer()
    }

    fun seekAudio(progress: Float) {
        mediaPlayer?.let {
            val seekTo = (progress * it.duration).toInt()
            it.seekTo(seekTo)
            playbackProgress = progress
        }
    }

    fun stopAudio() {
        isPlaying = false
        playbackProgress = 0f
        activeAudioUri = null
        activeAudioId = null
        activeAudioPageIndex = -1
        playbackJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
    }

    fun updateAudioPosition(pageIndex: Int, audioId: String, newX: Float, newY: Float) {
        if (pageIndex !in currentPages.indices) return
        val page = currentPages[pageIndex]
        currentPages[pageIndex] = page.copy(audioData = page.audioData.map {
            if (it.id == audioId) it.copy(x = newX, y = newY) else it
        })
    }

    fun deleteAudioNote(pageIndex: Int, audioId: String) {
        if (pageIndex !in currentPages.indices) return
        val page = currentPages[pageIndex]
        val audioToDelete = page.audioData.find { it.id == audioId } ?: return
        currentPages[pageIndex] = page.copy(audioData = page.audioData.filter { it.id != audioId })
        undoStack.add(EditorAction(pageIndex, null, null, null, audioToDelete, false))
        redoStack.clear()
        if (activeAudioUri == audioToDelete.uri) stopAudio()
    }

    fun updateTextOnPage(pageIndex: Int, oldText: TextData, newText: String, newFontName: String? = null) {
        val updatedText = oldText.copy(text = newText, fontName = newFontName ?: oldText.fontName)
        
        // 1. Check if the text is in the main page list
        val page = currentPages[pageIndex]
        if (page.textData.any { it.id == oldText.id }) {
            currentPages[pageIndex] = page.copy(textData = page.textData.map { if (it.id == oldText.id) updatedText else it })
        }
        
        // 2. Check if the text is in the current selection
        val selectedIdx = selectedTexts.indexOfFirst { it.id == oldText.id }
        if (selectedIdx != -1) {
            selectedTexts[selectedIdx] = updatedText
        }

        undoStack.add(EditorAction(pageIndex, null, oldText, null, null, false))
        undoStack.add(EditorAction(pageIndex, null, updatedText, null, null, true))
        redoStack.clear()
    }

    private fun addAudioToPage(pageIndex: Int, audio: com.midknight.pixelnotes.domain.AudioData) {
        if (pageIndex !in currentPages.indices) return
        val page = currentPages[pageIndex]
        currentPages[pageIndex] = page.copy(audioData = page.audioData + audio)
        undoStack.add(EditorAction(pageIndex, null, null, null, audio, true))
        redoStack.clear()
    }

    fun signIn(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val credentialManager = androidx.credentials.CredentialManager.create(context)
                val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(com.midknight.pixelnotes.BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = androidx.credentials.GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)

                    val capturedEmail = googleIdTokenCredential.id
                    val capturedName = googleIdTokenCredential.displayName
                    val capturedPhoto = googleIdTokenCredential.profilePictureUri?.toString()

                    if (!capturedEmail.isNullOrEmpty()) {
                        userEmail = capturedEmail
                        userName = capturedName
                        userPhotoUri = capturedPhoto
                        
                        prefs.edit()
                            .putString("user_email", userEmail)
                            .putString("user_name", userName)
                            .putString("user_photo", userPhotoUri)
                            .apply()
                        
                        // AUTOMATED SYNC ON SIGN IN
                        performInitialSync(context, capturedEmail)
                    } else {
                        throw Exception("Google account has no associated email.")
                    }
                }
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                android.widget.Toast.makeText(context, "Initializing Google Sign-In... please try again", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Sign in failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performInitialSync(context: android.content.Context, email: String) {
        isSyncing = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                android.util.Log.d("HandshakeMerge", "Starting initial sync for $email")
                val syncManager = com.midknight.pixelnotes.domain.CloudSyncManager(context)
                
                // 1. Download cloud backup to a "waiting room" (temp ZIP)
                val cloudZip = syncManager.downloadBackupToTemp(email)
                
                if (cloudZip != null && cloudZip.exists()) {
                    android.util.Log.d("HandshakeMerge", "Cloud backup found (${cloudZip.length()} bytes). Starting merge.")
                    // 2. Perform a "Handshake Merge"
                    // This combines cloud notes with local ones record-by-record
                    val mergedCount = mergeCloudData(context, cloudZip)
                    android.util.Log.d("HandshakeMerge", "Merge finished. Imported $mergedCount notes from cloud.")
                    cloudZip.delete()
                } else {
                    android.util.Log.d("HandshakeMerge", "No cloud backup found to merge.")
                }
                
                // 3. Final Backup: Push the unified family to the cloud
                // This ensures the cloud now has Note A + (B, C, D)
                android.util.Log.d("HandshakeMerge", "Uploading unified backup to cloud...")
                syncManager.backupToDrive(email, force = true)
                android.util.Log.d("HandshakeMerge", "Unified backup upload complete.")
                
            } catch (e: Exception) {
                android.util.Log.e("HandshakeMerge", "Initial sync/merge failed", e)
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isSyncing = false
                }
            }
        }
    }

    private suspend fun mergeCloudData(context: android.content.Context, zipFile: java.io.File): Int {
        var importCount = 0
        try {
            val tempDir = java.io.File(context.cacheDir, "merge_extract_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            // 1. Extract ZIP to temp
            java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = java.io.File(tempDir, entry.name)
                    destFile.parentFile?.mkdirs()
                    if (!entry.isDirectory) {
                        java.io.FileOutputStream(destFile).use { out -> zis.copyTo(out) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val cloudDbFile = java.io.File(tempDir, "database/pixel_notes_database")
            if (cloudDbFile.exists()) {
                val cloudDb = android.database.sqlite.SQLiteDatabase.openDatabase(cloudDbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
                
                // Fetch all notes from cloud DB
                val cursor = cloudDb.rawQuery("SELECT * FROM notes", null)
                val localNotes = dao.getNotesWithPagesSync()
                val localTitles = localNotes.map { it.note.title }.toSet()

                android.util.Log.d("HandshakeMerge", "Cloud DB opened. Found ${cursor.count} notes in cloud backup.")

                if (cursor.moveToFirst()) {
                    do {
                        var title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                        
                        // Deduplication Strategy: If title exists, rename instead of skipping
                        // This proves the merge is working and doesn't lose any data version.
                        if (localTitles.contains(title)) {
                            title = "$title (Cloud)"
                            android.util.Log.d("HandshakeMerge", "Title conflict for '$title'. Renaming for import.")
                        }

                        val cloudId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                        val content = cursor.getString(cursor.getColumnIndexOrThrow("content")) ?: ""
                        val date = cursor.getString(cursor.getColumnIndexOrThrow("date")) ?: ""
                        val folder = cursor.getString(cursor.getColumnIndexOrThrow("folder")) ?: "General"
                        val inTrash = cursor.getInt(cursor.getColumnIndexOrThrow("inTrash")) == 1
                        val isInfinite = cursor.getInt(cursor.getColumnIndexOrThrow("isInfinite")) == 1
                        val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))

                        val newNote = Note(title = title, content = content, date = date, folder = folder, inTrash = inTrash, isInfinite = isInfinite, updatedAt = updatedAt)
                        val newId = dao.insertNote(newNote).toInt()

                        // Fetch and insert pages for this note
                        val pageCursor = cloudDb.rawQuery("SELECT * FROM pages WHERE noteId = ?", arrayOf(cloudId.toString()))
                        if (pageCursor.moveToFirst()) {
                            val pages = mutableListOf<PageEntity>()
                            val converters = com.midknight.pixelnotes.data.Converters()
                            do {
                                val pageNumber = pageCursor.getInt(pageCursor.getColumnIndexOrThrow("pageNumber"))
                                val drawingData = pageCursor.getString(pageCursor.getColumnIndexOrThrow("drawingData")) ?: "[]"
                                val textData = pageCursor.getString(pageCursor.getColumnIndexOrThrow("textData")) ?: "[]"
                                val imageData = pageCursor.getString(pageCursor.getColumnIndexOrThrow("imageData")) ?: "[]"
                                val audioData = pageCursor.getString(pageCursor.getColumnIndexOrThrow("audioData")) ?: "[]"
                                val bgUri = pageCursor.getString(pageCursor.getColumnIndexOrThrow("backgroundUri"))
                                val style = pageCursor.getInt(pageCursor.getColumnIndexOrThrow("paperStyle"))
                                val color = pageCursor.getInt(pageCursor.getColumnIndexOrThrow("canvasColor"))

                                // Fix Background URI if it points to a PDF
                                var finalBgUri = bgUri
                                if (bgUri != null && bgUri.contains("imported_pdfs/")) {
                                    val fileName = java.io.File(bgUri.split("?pdfPage=")[0]).name
                                    val suffix = if (bgUri.contains("?pdfPage=")) "?pdfPage=" + bgUri.split("?pdfPage=")[1] else ""
                                    finalBgUri = java.io.File(java.io.File(context.filesDir, "imported_pdfs"), fileName).absolutePath + suffix
                                }

                                // Remap Image URIs to portable format
                                val rawImageList = converters.toImageList(imageData)
                                val normalizedImages = rawImageList.map { img ->
                                    if (img.uri.contains("inserted_images/")) {
                                        val fileName = img.uri.substringAfterLast("/")
                                        img.copy(uri = "internal://$fileName")
                                    } else img
                                }

                                pages.add(PageEntity(
                                    noteId = newId,
                                    pageNumber = pageNumber,
                                    drawingData = converters.toStrokeList(drawingData),
                                    textData = converters.toTextList(textData),
                                    imageData = normalizedImages,
                                    audioData = converters.toAudioList(audioData),
                                    backgroundUri = finalBgUri,
                                    paperStyle = style,
                                    canvasColor = color
                                ))
                            } while (pageCursor.moveToNext())
                            dao.insertPages(pages)
                            pageCursor.close()
                        }
                        importCount++
                        android.util.Log.d("HandshakeMerge", "Imported note: $title with ID: $newId")
                    } while (cursor.moveToNext())
                }
                cursor.close()
                cloudDb.close()
            } else {
                android.util.Log.e("HandshakeMerge", "Could not find database file inside extracted ZIP.")
            }

            // 2. Copy Media Assets (Images, Audio, PDFs)
            val mediaDirs = listOf("inserted_images", "audio_notes", "imported_pdfs")
            mediaDirs.forEach { dirName ->
                val srcDir = java.io.File(tempDir, "files/$dirName")
                if (srcDir.exists()) {
                    val destDir = java.io.File(context.filesDir, dirName)
                    if (!destDir.exists()) destDir.mkdirs()
                    srcDir.listFiles()?.forEach { file ->
                        try {
                            file.copyTo(java.io.File(destDir, file.name), overwrite = true)
                        } catch (e: Exception) {
                            android.util.Log.e("HandshakeMerge", "Failed to copy media file: ${file.name}", e)
                        }
                    }
                }
            }

            tempDir.deleteRecursively()
        } catch (e: Exception) {
            android.util.Log.e("HandshakeMerge", "Error during record-level merge", e)
        }
        return importCount
    }

    fun signOut(context: android.content.Context) { 
        val email = userEmail
        isSyncing = true
        viewModelScope.launch {
            if (email != null) {
                if (notes.value.isNotEmpty()) {
                    com.midknight.pixelnotes.domain.CloudSyncManager(context).backupToDrive(email)
                }
            }
            
            // 1. Clear Memory State
            userEmail = null
            userName = null
            userPhotoUri = null
            
            // 2. Clear SharedPreferences and FORCE COMMIT to disk before process dies
            prefs.edit().clear().commit()
                
            // 3. Clear Google Credentials Cache
            try {
                val credentialManager = androidx.credentials.CredentialManager.create(context)
                credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 4. Wipe Local Database and Files
            com.midknight.pixelnotes.domain.CloudSyncManager(context).clearLocalData()

            isSyncing = false
            restartApp(context, "")
        }
    }

    private fun restartApp(context: android.content.Context, message: String) {
        if (message.isNotBlank()) android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 
            0, 
            mainIntent, 
            android.app.PendingIntent.FLAG_CANCEL_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        
        // Schedule restart for 100ms from now
        alarmManager.set(
            android.app.AlarmManager.RTC, 
            System.currentTimeMillis() + 100,
            pendingIntent
        )

        // Shutdown current process
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            kotlinx.coroutines.delay(50)
            (context as? android.app.Activity)?.finish()
            java.lang.Runtime.getRuntime().exit(0)
        }
    }

    var pendingSyncIntent by mutableStateOf<android.content.Intent?>(null)

    fun backupToCloud(context: android.content.Context) {
        val email = userEmail ?: return
        
        isSyncing = true
        viewModelScope.launch {
            if (currentScreen == 1) {
                saveCurrentNoteToDb(SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date()))
            }

            pruneOrphanFiles()

            val syncManager = com.midknight.pixelnotes.domain.CloudSyncManager(context)
            
            val result = syncManager.backupToDrive(email, force = true)
            isSyncing = false

            val exception = result.exceptionOrNull()
            if (exception is com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                pendingSyncIntent = exception.intent
            } else if (result.isSuccess) {
                android.widget.Toast.makeText(context, "Backup successful!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Backup failed: ${exception?.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun restoreFromCloudManual(context: android.content.Context) {
        val email = userEmail ?: return
        isSyncing = true
        viewModelScope.launch {
            com.midknight.pixelnotes.data.NoteDatabase.closeDatabase()
            val result = com.midknight.pixelnotes.domain.CloudSyncManager(context).restoreFromDrive(email)
            isSyncing = false
            if (result.isSuccess) {
                restartApp(context, "Restore successful! Restarting...")
            } else {
                android.widget.Toast.makeText(context, "Restore failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                com.midknight.pixelnotes.data.NoteDatabase.getDatabase(context)
            }
        }
    }

    fun purgeCloudData(context: android.content.Context) {
        val email = userEmail ?: return
        isSyncing = true
        viewModelScope.launch {
            val result = com.midknight.pixelnotes.domain.CloudSyncManager(context).purgeAllCloudBackups(email)
            isSyncing = false
            if (result.isSuccess) {
                android.widget.Toast.makeText(context, "Cloud data purged successfully", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Purge failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun createLocalBackup(context: android.content.Context, uri: android.net.Uri) {
        isSyncing = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (currentScreen == 1) {
                    saveCurrentNoteToDb(SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date()))
                }
                
                pruneOrphanFiles()
                
                val snapshotFile = java.io.File(context.cacheDir, "local_db_snapshot.db")
                com.midknight.pixelnotes.data.NoteDatabase.createBackupSnapshot(context, snapshotFile)
                
                val syncManager = com.midknight.pixelnotes.domain.CloudSyncManager(context)
                val zipFile = syncManager.createBackupZip(snapshotFile)
                
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    java.io.FileInputStream(zipFile).use { input ->
                        input.copyTo(output)
                    }
                }
                
                zipFile.delete()
                snapshotFile.delete()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isSyncing = false
                    android.widget.Toast.makeText(context, "Local backup created!", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isSyncing = false
                    android.widget.Toast.makeText(context, "Backup failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun restoreLocalBackup(context: android.content.Context, uri: android.net.Uri) {
        isSyncing = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val tempZip = java.io.File(context.cacheDir, "local_restore_temp.zip")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(tempZip).use { output ->
                        input.copyTo(output)
                    }
                }
                
                com.midknight.pixelnotes.data.NoteDatabase.closeDatabase()
                
                val syncManager = com.midknight.pixelnotes.domain.CloudSyncManager(context)
                syncManager.extractBackupZip(tempZip)
                tempZip.delete()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isSyncing = false
                    restartApp(context, "Local backup restored! Restarting...")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isSyncing = false
                    android.widget.Toast.makeText(context, "Restore failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    com.midknight.pixelnotes.data.NoteDatabase.getDatabase(context)
                }
            }
        }
    }

    fun exportSingleNote(context: android.content.Context, noteWP: NoteWithPages, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val packager = com.midknight.pixelnotes.domain.SingleNotePackage(context)
                val file = packager.exportNote(noteWP)
                file?.let {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        java.io.FileInputStream(it).use { input ->
                            input.copyTo(output)
                        }
                    }
                    it.delete()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Note exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importSingleNote(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val tempZip = java.io.File(context.cacheDir, "import_target.pxnote")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(tempZip).use { output ->
                        input.copyTo(output)
                    }
                }
                
                val packager = com.midknight.pixelnotes.domain.SingleNotePackage(context)
                val importedNote = packager.importNote(tempZip)
                tempZip.delete()

                if (importedNote != null) {
                    val noteId = dao.insertNote(importedNote.note).toInt()
                    val pages = importedNote.pages.map { it.copy(noteId = noteId) }
                    dao.insertPages(pages)
                    
                    val finalNoteWP = NoteWithPages(importedNote.note.copy(id = noteId), pages)
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Note imported: ${importedNote.note.title}", android.widget.Toast.LENGTH_SHORT).show()
                        openNoteForEditing(finalNoteWP)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class NotesViewModelFactory(private val dao: NoteDao, private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) return NotesViewModel(dao, context) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}