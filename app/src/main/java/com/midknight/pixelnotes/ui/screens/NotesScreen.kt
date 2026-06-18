package com.midknight.pixelnotes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.midknight.pixelnotes.data.FolderEntity
import com.midknight.pixelnotes.data.NoteWithPages
import com.midknight.pixelnotes.domain.HapticManager
import com.midknight.pixelnotes.ui.components.*
import com.midknight.pixelnotes.ui.viewmodels.NotesViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<NoteWithPages>,
    folders: List<FolderEntity>,
    currentFolder: String,
    selectedNotes: List<NoteWithPages>,
    userEmail: String?,
    userName: String?,
    userPhotoUri: String?,
    onNoteClick: (NoteWithPages) -> Unit,
    onNoteLongClick: (NoteWithPages) -> Unit,
    onCreateNewNote: (Boolean) -> Unit,
    onFolderSelected: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onMenuClick: () -> Unit,
    onClearSelection: () -> Unit,
    onActionMove: () -> Unit,
    onActionExport: () -> Unit,
    onActionShare: () -> Unit,
    onActionDelete: () -> Unit,
    onActionRestore: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onLocalBackup: () -> Unit,
    onLocalRestore: () -> Unit,
    onExportPxNote: (NoteWithPages) -> Unit,
    onImportPxNote: () -> Unit,
    onImportPdf: () -> Unit,
    viewModel: NotesViewModel
) {
    val context = LocalContext.current
    val haptic = remember { HapticManager(context) }
    var searchQuery by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToRename by remember { mutableStateOf<FolderEntity?>(null) }
    var newFolderName by remember { mutableStateOf("") }
    var showFabMenu by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Selection and Gesture state
    val folderBounds = remember { mutableStateMapOf<String, Rect>() }
    val noteBounds = remember { mutableStateMapOf<Int, Rect>() }
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    var containerPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var initialSelectionNotes by remember { mutableStateOf(setOf<Int>()) }
    var initialSelectionFolders by remember { mutableStateOf(setOf<String>()) }
    var lastGestureTime by remember { mutableLongStateOf(0L) }
    var isGestureActive by remember { mutableStateOf(false) }
    var isGuardActive by remember { mutableStateOf(false) }
    
    LaunchedEffect(lastGestureTime) {
        if (lastGestureTime > 0) {
            isGuardActive = true
            kotlinx.coroutines.delay(800)
            isGuardActive = false
        }
    }
    
    // Smooth Autoscroll logic
    var dragScrollDirection by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(dragScrollDirection) {
        if (dragScrollDirection != 0f) {
            while (true) {
                gridState.scrollBy(dragScrollDirection)
                kotlinx.coroutines.delay(16) // Paced at ~60fps to prevent ANR
            }
        }
    }

    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder") },
            text = { Text("Delete '${folderToDelete?.name}' and all its contents?") },
            confirmButton = { 
                ExpressiveButton(
                    text = "Delete", 
                    onClick = { 
                        folderToDelete?.let { onDeleteFolder(it.path) }
                        folderToDelete = null 
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    isSquareEdge = true
                )
            },
            dismissButton = { 
                ExpressiveButton(
                    text = "Cancel", 
                    onClick = { 
                        folderToDelete = null 
                    },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    isSquareEdge = true
                )
            }
        )
    }

    if (folderToRename != null) {
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = { Text("Rename Folder") },
            text = { OutlinedTextField(value = newFolderName, onValueChange = { newFolderName = it }, label = { Text("New Name") }) },
            confirmButton = { 
                ExpressiveButton(
                    text = "Save", 
                    onClick = { 
                        haptic.click()
                        folderToRename?.let { onRenameFolder(it.path, newFolderName) }
                        folderToRename = null 
                    },
                    isSquareEdge = true
                )
            },
            dismissButton = { 
                ExpressiveButton(
                    text = "Cancel", 
                    onClick = { 
                        haptic.click()
                        folderToRename = null 
                    },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    isSquareEdge = true
                )
            }
        )
    }

    if (viewModel.showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showEmptyTrashDialog = false },
            title = { Text("Empty Trash") },
            text = { Text("Permanently delete all notes in the trash? This cannot be undone.") },
            confirmButton = {
                ExpressiveButton(
                    text = "Empty Trash",
                    onClick = {
                        haptic.heavyClick()
                        viewModel.emptyTrash()
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    isSquareEdge = true
                )
            },
            dismissButton = {
                ExpressiveButton(
                    text = "Cancel",
                    onClick = {
                        haptic.click()
                        viewModel.showEmptyTrashDialog = false
                    },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    isSquareEdge = true
                )
            }
        )
    }

    val displayedNotes = notes.filter { 
        (currentFolder == "All Notes" && !it.note.inTrash) || (it.note.folder == currentFolder && !it.note.inTrash) || (currentFolder == "Trash" && it.note.inTrash)
    }.filter { it.note.title.contains(searchQuery, ignoreCase = true) }

    val displayedFolders = if (currentFolder != "Trash" && searchQuery.isBlank()) {
        folders.filter { it.parentPath == (if (currentFolder == "All Notes") null else currentFolder) }
    } else emptyList()

    // Clear bounds when content changes to prevent phantom selections
    LaunchedEffect(displayedNotes, displayedFolders) {
        noteBounds.clear()
        folderBounds.clear()
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedNotes.isEmpty() && viewModel.selectedFolders.isEmpty(),
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpressiveIconButton(icon = Icons.Default.Menu, contentDescription = "Menu", onClick = onMenuClick)
                        
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search your notes...") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear") } },
                            singleLine = true
                        )

                        // Profile Picture / Account Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showProfileDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (userPhotoUri != null) {
                                AsyncImage(
                                    model = userPhotoUri,
                                    contentDescription = "Account",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = "Account", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }

                // CONTEXTUAL SELECTION BAR
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedNotes.isNotEmpty() || viewModel.selectedFolders.isNotEmpty(),
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpressiveIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Clear Selection",
                            onClick = onClearSelection,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        val totalSelected = selectedNotes.size + viewModel.selectedFolders.size
                        Text(
                            text = "$totalSelected selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 8.dp).weight(1f)
                        )
                        
                        if (currentFolder == "Trash") {
                            ExpressiveIconButton(icon = Icons.Default.Restore, contentDescription = "Restore", onClick = onActionRestore, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ExpressiveIconButton(icon = Icons.Default.DeleteForever, contentDescription = "Delete Permanently", onClick = onActionDelete, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        } else {
                            if (selectedNotes.size == 1 && viewModel.selectedFolders.isEmpty()) {
                                ExpressiveIconButton(icon = Icons.Default.FileUpload, contentDescription = "Export .pxnote", onClick = { onExportPxNote(selectedNotes.first()) }, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            if (viewModel.selectedFolders.isEmpty()) {
                                ExpressiveIconButton(icon = Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move", onClick = onActionMove, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                ExpressiveIconButton(icon = Icons.Default.PictureAsPdf, contentDescription = "Merge PDF", onClick = onActionExport, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                ExpressiveIconButton(icon = Icons.Default.Share, contentDescription = "Share", onClick = onActionShare, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            ExpressiveIconButton(icon = Icons.Default.Delete, contentDescription = "Trash", onClick = onActionDelete, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentFolder == "Trash" && displayedNotes.isNotEmpty()) {
                ExpressiveFAB(
                    icon = Icons.Default.DeleteSweep,
                    onClick = { viewModel.showEmptyTrashDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            } else if (selectedNotes.isEmpty() && viewModel.selectedFolders.isEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = showFabMenu) {
                        Column(modifier = Modifier.padding(bottom = 16.dp), horizontalAlignment = Alignment.End) {
                            ExpressiveIconButton(
                                icon = Icons.Default.FileOpen,
                                contentDescription = "Import .pxnote",
                                onClick = { showFabMenu = false; onImportPxNote() },
                                modifier = Modifier.padding(bottom = 8.dp),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                size = 56.dp
                            )
                            ExpressiveIconButton(
                                icon = Icons.Default.PictureAsPdf,
                                contentDescription = "Import PDF",
                                onClick = { showFabMenu = false; onImportPdf() },
                                modifier = Modifier.padding(bottom = 8.dp),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                size = 56.dp
                            )
                            ExpressiveIconButton(
                                icon = Icons.Default.Description,
                                contentDescription = "A4 Note",
                                onClick = { showFabMenu = false; onCreateNewNote(false) },
                                modifier = Modifier.padding(bottom = 8.dp),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                size = 56.dp
                            )
                            ExpressiveIconButton(
                                icon = Icons.Default.AllOut,
                                contentDescription = "Infinite Canvas",
                                onClick = { showFabMenu = false; onCreateNewNote(true) },
                                modifier = Modifier.padding(bottom = 8.dp),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                size = 56.dp
                            )
                        }
                    }
                    ExpressiveFAB(
                        icon = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val fullHeight = this.constraints.maxHeight.toFloat()
            Box(modifier = Modifier.fillMaxSize()
                .onGloballyPositioned { containerPositionInWindow = it.positionInWindow() }
                .pointerInput(displayedNotes, displayedFolders) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        isGestureActive = true
                        val windowOffset = offset + containerPositionInWindow
                        // Find starting index
                        val noteFound = noteBounds.entries.find { it.value.contains(windowOffset) }
                        val folderFound = folderBounds.entries.find { it.value.contains(windowOffset) }
                        
                        if (noteFound != null) {
                            val noteWP = displayedNotes.find { it.note.id == noteFound.key }
                            if (noteWP != null) {
                                dragStartIndex = displayedFolders.size + displayedNotes.indexOf(noteWP)
                                if (!selectedNotes.any { it.note.id == noteWP.note.id }) {
                                    haptic.selection()
                                    onNoteLongClick(noteWP)
                                }
                            }
                        } else if (folderFound != null) {
                            val folder = displayedFolders.find { it.path == folderFound.key }
                            if (folder != null) {
                                dragStartIndex = displayedFolders.indexOf(folder)
                                if (!viewModel.selectedFolders.any { it.path == folder.path }) {
                                    haptic.selection()
                                    viewModel.toggleFolderSelection(folder)
                                }
                            }
                        }
                        
                        initialSelectionNotes = selectedNotes.map { it.note.id }.toSet()
                        initialSelectionFolders = viewModel.selectedFolders.map { it.path }.toSet()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val currentPos = change.position
                        val windowPos = currentPos + containerPositionInWindow
                        
                        val scrollThreshold = 150f
                        dragScrollDirection = if (currentPos.y < scrollThreshold) {
                            -15f * (1f - (currentPos.y / scrollThreshold))
                        } else if (currentPos.y > fullHeight - scrollThreshold) {
                            15f * (1f - ((fullHeight - currentPos.y) / scrollThreshold))
                        } else {
                            0f
                        }

                        val noteFound = noteBounds.entries.find { it.value.contains(windowPos) }
                        val folderFound = folderBounds.entries.find { it.value.contains(windowPos) }
                        
                        val dragCurrentIndex = if (noteFound != null) {
                            val noteWP = displayedNotes.find { it.note.id == noteFound.key }
                            if (noteWP != null) displayedFolders.size + displayedNotes.indexOf(noteWP) else -1
                        } else if (folderFound != null) {
                            val folder = displayedFolders.find { it.path == folderFound.key }
                            if (folder != null) displayedFolders.indexOf(folder) else -1
                        } else -1

                        if (dragStartIndex != -1 && dragCurrentIndex != -1) {
                            val start = if (dragStartIndex < dragCurrentIndex) dragStartIndex else dragCurrentIndex
                            val end = if (dragStartIndex < dragCurrentIndex) dragCurrentIndex else dragStartIndex
                            
                            displayedFolders.forEachIndexed { index, folder ->
                                val inRange = index in start..end
                                val isSelected = viewModel.selectedFolders.any { it.path == folder.path }
                                if (inRange && !isSelected) {
                                    haptic.tick()
                                    viewModel.toggleFolderSelection(folder)
                                } else if (!inRange && isSelected && !initialSelectionFolders.contains(folder.path)) {
                                    haptic.tick()
                                    viewModel.toggleFolderSelection(folder)
                                }
                            }
                            displayedNotes.forEachIndexed { index, noteWP ->
                                val globalIndex = displayedFolders.size + index
                                val inRange = globalIndex in start..end
                                val isSelected = selectedNotes.any { it.note.id == noteWP.note.id }
                                if (inRange && !isSelected) {
                                    haptic.tick()
                                    onNoteLongClick(noteWP)
                                } else if (!inRange && isSelected && !initialSelectionNotes.contains(noteWP.note.id)) {
                                    haptic.tick()
                                    onNoteLongClick(noteWP)
                                }
                            }
                        }
                    },
                    onDragEnd = { 
                        dragStartIndex = -1
                        dragScrollDirection = 0f
                        lastGestureTime = System.currentTimeMillis()
                        isGestureActive = false
                    },
                    onDragCancel = { 
                        dragStartIndex = -1
                        dragScrollDirection = 0f
                        lastGestureTime = System.currentTimeMillis()
                        isGestureActive = false
                    }
                )
            }) {
                if (displayedFolders.isEmpty() && displayedNotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = if (searchQuery.isNotBlank()) "No matching results found" else "Empty folder", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    AnimatedContent(
                        targetState = currentFolder to searchQuery,
                        transitionSpec = {
                            val isSearchChange = targetState.second != initialState.second
                            if (isSearchChange) {
                                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                            } else {
                                (slideInHorizontally(animationSpec = tween(400)) { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally(animationSpec = tween(400)) { width -> -width / 2 } + fadeOut()
                                )
                            }
                        },
                        label = "GridTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { (targetFolder, targetQuery) ->
                        LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 220.dp), 
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp), 
                        horizontalArrangement = Arrangement.spacedBy(16.dp), 
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                            items(displayedFolders, key = { it.path }) { folderItem ->
                                FolderCard(
                                    folder = folderItem,
                                    isSelected = viewModel.selectedFolders.any { it.path == folderItem.path },
                                    onClick = { 
                                        if (viewModel.selectedFolders.isNotEmpty() || selectedNotes.isNotEmpty()) {
                                            haptic.click()
                                            viewModel.toggleFolderSelection(folderItem)
                                        } else {
                                            searchQuery = ""
                                            onFolderSelected(folderItem.path)
                                        }
                                    },
                                    onLongClick = {},
                                    onPositioned = { rect -> folderBounds[folderItem.path] = rect },
                                    enabled = !isGestureActive && !isGuardActive
                                )
                            }
                            items(displayedNotes, key = { it.note.id }) { noteWP ->
                                val isSelected = selectedNotes.any { it.note.id == noteWP.note.id }
                                NoteCard(
                                    noteWithPages = noteWP,
                                    isSelected = isSelected,
                                    onClick = { 
                                        haptic.click()
                                        if (selectedNotes.isNotEmpty() || viewModel.selectedFolders.isNotEmpty() || currentFolder == "Trash") onNoteLongClick(noteWP) else onNoteClick(noteWP) 
                                    },
                                    onLongClick = {},
                                    onPositioned = { rect -> noteBounds[noteWP.note.id] = rect },
                                    enabled = !isGestureActive && !isGuardActive
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        ProfileDialog(
            userEmail = userEmail,
            userName = userName,
            userPhotoUri = userPhotoUri,
            isSyncing = viewModel.isSyncing,
            onDismiss = { showProfileDialog = false },
            onSignOut = { onSignOutClick(); showProfileDialog = false },
            onSignIn = { onSignInClick(); showProfileDialog = false },
            onBackup = { viewModel.backupToCloud(context) },
            onRestore = { viewModel.restoreFromCloudManual(context) },
            onPurge = { viewModel.purgeCloudData(context) },
            onLocalBackup = onLocalBackup,
            onLocalRestore = onLocalRestore
        )
    }
}

@Composable
fun ProfileDialog(
    userEmail: String?,
    userName: String?,
    userPhotoUri: String?,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
    onLocalBackup: () -> Unit,
    onLocalRestore: () -> Unit
) {
    var localIsRestoring by remember { mutableStateOf(false) }
    var showPurgeConfirm by remember { mutableStateOf(false) }

    if (showPurgeConfirm) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirm = false },
            title = { Text("Delete Cloud Data") },
            text = { Text("Permanently delete all backups from Google Drive? This cannot be undone.") },
            confirmButton = {
                ExpressiveButton(
                    text = "Delete Everything",
                    onClick = { onPurge(); showPurgeConfirm = false },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    isSquareEdge = true
                )
            },
            dismissButton = {
                ExpressiveButton(
                    text = "Cancel",
                    onClick = { showPurgeConfirm = false },
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    isSquareEdge = true
                )
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(350.dp)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExpressiveIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    Text(
                        text = userEmail ?: "Not signed in",
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Image with Camera overlay
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userPhotoUri != null) {
                            AsyncImage(
                                model = userPhotoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (userName != null) "Hi, $userName!" else "Welcome to Pixel Notes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action List
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    Column {
                        if (userEmail == null) {
                            ProfileDialogRow(icon = Icons.Default.GroupAdd, text = "Sign in to Pixel Notes", onClick = onSignIn)
                            ProfileDialogRow(
                                icon = Icons.Default.FileUpload,
                                text = "Create Local Backup",
                                onClick = onLocalBackup
                            )
                            ProfileDialogRow(
                                icon = Icons.Default.FileDownload,
                                text = "Restore Local Backup",
                                onClick = onLocalRestore
                            )
                        } else {
                            ProfileDialogRow(
                                icon = Icons.Default.Backup, 
                                text = if (isSyncing && !localIsRestoring) "Backing up..." else "Backup Now", 
                                onClick = { onBackup() }
                            )
                            ProfileDialogRow(
                                icon = Icons.Default.CloudDownload, 
                                text = if (localIsRestoring) "Restoring..." else "Restore from Cloud", 
                                onClick = { localIsRestoring = true; onRestore() }
                            )
                            ProfileDialogRow(
                                icon = Icons.Default.DeleteForever,
                                text = "Delete Cloud Data",
                                onClick = { showPurgeConfirm = true }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ProfileDialogRow(icon = Icons.AutoMirrored.Filled.Logout, text = "Sign out", onClick = onSignOut)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDialogRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = remember { HapticManager(context) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                haptic.click()
                onClick()
            })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpressiveIconButton(
            icon = icon,
            contentDescription = null,
            onClick = onClick,
            size = 32.dp,
            iconSize = 20.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
