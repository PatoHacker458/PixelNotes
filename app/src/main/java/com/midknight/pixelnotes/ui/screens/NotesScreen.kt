package com.midknight.pixelnotes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllOut
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.midknight.pixelnotes.data.FolderEntity
import com.midknight.pixelnotes.data.NoteWithPages
import com.midknight.pixelnotes.domain.HapticManager
import com.midknight.pixelnotes.ui.components.*
import com.midknight.pixelnotes.ui.viewmodels.NotesViewModel
import kotlinx.coroutines.launch

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

    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder") },
            text = { Text("Delete '${folderToDelete?.name}' and all its contents?") },
            confirmButton = { 
                ExpressiveButton(
                    text = "Delete", 
                    onClick = { folderToDelete?.let { onDeleteFolder(it.path) }; folderToDelete = null },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    isSquareEdge = true
                )
            },
            dismissButton = { 
                ExpressiveButton(
                    text = "Cancel", 
                    onClick = { folderToDelete = null },
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
                    onClick = { viewModel.emptyTrash() },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    isSquareEdge = true
                )
            },
            dismissButton = {
                ExpressiveButton(
                    text = "Cancel",
                    onClick = { viewModel.showEmptyTrashDialog = false },
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
            confirmButton = { TextButton(onClick = { if (newFolderName.isNotBlank()) folderToRename?.let { onRenameFolder(it.path, newFolderName) }; folderToRename = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { folderToRename = null }) { Text("Cancel") } }
        )
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
            if (currentFolder == "Trash") {
                ExpressiveFAB(
                    icon = Icons.Default.DeleteSweep,
                    onClick = { viewModel.showEmptyTrashDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            } else if (selectedNotes.isEmpty()) {
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
                // NORMAL TOP BAR
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedNotes.isEmpty(),
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpressiveIconButton(
                            icon = Icons.Default.Menu,
                            contentDescription = "Menu",
                            onClick = onMenuClick
                        )
                        
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search in $currentFolder...") },
                            trailingIcon = { if (searchQuery.isNotEmpty()) { ExpressiveIconButton(icon = Icons.Default.Clear, contentDescription = "Clear search", onClick = { searchQuery = "" }, size = 32.dp, iconSize = 18.dp) } },
                            shape = RoundedCornerShape(32.dp),
                            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )

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
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }

                // CONTEXTUAL SELECTION BAR
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedNotes.isNotEmpty(),
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpressiveIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Clear Selection",
                            onClick = onClearSelection,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${selectedNotes.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 8.dp).weight(1f)
                        )
                        
                        if (currentFolder == "Trash") {
                            ExpressiveIconButton(icon = Icons.Default.Restore, contentDescription = "Restore", onClick = onActionRestore, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ExpressiveIconButton(icon = Icons.Default.DeleteForever, contentDescription = "Delete Permanently", onClick = onActionDelete, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        } else {
                            if (selectedNotes.size == 1) {
                                ExpressiveIconButton(icon = Icons.Default.FileUpload, contentDescription = "Export .pxnote", onClick = { onExportPxNote(selectedNotes.first()) }, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            ExpressiveIconButton(icon = Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move", onClick = onActionMove, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ExpressiveIconButton(icon = Icons.Default.PictureAsPdf, contentDescription = "Merge PDF", onClick = onActionExport, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ExpressiveIconButton(icon = Icons.Default.Share, contentDescription = "Share", onClick = onActionShare, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ExpressiveIconButton(icon = Icons.Default.Delete, contentDescription = "Trash", onClick = onActionDelete, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

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
                    columns = GridCells.Adaptive(minSize = 220.dp), 
                    modifier = Modifier.fillMaxSize(), 
                    contentPadding = PaddingValues(16.dp), 
                    horizontalArrangement = Arrangement.spacedBy(16.dp), 
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayedFolders, key = { it.path }) { folderItem ->
                        var isVisible by remember(folderItem.path) { mutableStateOf(false) }
                        LaunchedEffect(folderItem.path) { isVisible = true }
                        
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300))
                        ) {
                            FolderCard(
                                folder = folderItem,
                                onClick = { searchQuery = ""; onFolderSelected(folderItem.path) },
                                onRenameClick = { newFolderName = folderItem.name; folderToRename = folderItem },
                                onDeleteClick = { folderToDelete = folderItem }
                            )
                        }
                    }
                    items(displayedNotes, key = { it.note.id }) { noteWP ->
                        var isVisible by remember(noteWP.note.id) { mutableStateOf(false) }
                        LaunchedEffect(noteWP.note.id) { isVisible = true }

                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.85f, animationSpec = tween(400))
                        ) {
                            NoteCard(
                                noteWithPages = noteWP,
                                isSelected = selectedNotes.any { it.note.id == noteWP.note.id },
                                onClick = { 
                                    haptic.click()
                                    if (selectedNotes.isNotEmpty() || currentFolder == "Trash") onNoteLongClick(noteWP) else onNoteClick(noteWP) 
                                },
                                onLongClick = { 
                                    haptic.selection()
                                    onNoteLongClick(noteWP) 
                                }
                            )
                        }
                    }
                }
            }
            }
        }
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
