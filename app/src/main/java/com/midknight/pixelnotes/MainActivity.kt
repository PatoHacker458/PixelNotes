package com.midknight.pixelnotes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.midknight.pixelnotes.data.NoteDatabase
import com.midknight.pixelnotes.domain.PdfExporter
import com.midknight.pixelnotes.ui.components.ExpressiveButton
import com.midknight.pixelnotes.ui.components.SideMenu
import com.midknight.pixelnotes.ui.screens.DrawingScreen
import com.midknight.pixelnotes.ui.screens.NotesScreen
import com.midknight.pixelnotes.ui.screens.SettingsScreen
import com.midknight.pixelnotes.ui.theme.PixelNotesTheme
import com.midknight.pixelnotes.ui.viewmodels.NotesViewModel
import com.midknight.pixelnotes.ui.viewmodels.NotesViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = NoteDatabase.getDatabase(this)
        val dao = database.noteDao()

        setContent {
            PixelNotesTheme {
                val viewModel: NotesViewModel = viewModel(factory = NotesViewModelFactory(dao, applicationContext))
                val notes by viewModel.notes.collectAsState()
                val folders by viewModel.folders.collectAsState()
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                val pdfLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
                    uri?.let {
                        coroutineScope.launch {
                            val exporter = PdfExporter(context)
                            exporter.exportToPdf(viewModel.selectedNotes.toList(), it)
                            viewModel.clearSelection()
                            Toast.makeText(context, "Exported Merged PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                BackHandler(enabled = viewModel.currentScreen != 0 || viewModel.selectedNotes.isNotEmpty() || drawerState.isOpen) {
                    if (drawerState.isOpen) {
                        coroutineScope.launch { drawerState.close() }
                    } else if (viewModel.selectedNotes.isNotEmpty()) {
                        viewModel.clearSelection()
                    } else if (viewModel.currentScreen == 1) {
                        if (!viewModel.isNoteBlank()) Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        viewModel.closeEditing()
                    } else {
                        viewModel.currentScreen = 0
                    }
                }

                if (viewModel.showDeleteDialog) {
                    val isTrash = viewModel.currentFolderFilter == "Trash"
                    AlertDialog(
                        onDismissRequest = { viewModel.showDeleteDialog = false },
                        title = { Text(if (isTrash) "Delete Permanently" else "Move to Trash") },
                        text = { Text(if (isTrash) "These ${viewModel.selectedNotes.size} notes will be deleted forever." else "Move ${viewModel.selectedNotes.size} notes to the trash?") },
                        confirmButton = { 
                            ExpressiveButton(
                                text = if (isTrash) "Delete" else "Move", 
                                onClick = { if (isTrash) viewModel.permanentlyDeleteSelected() else viewModel.moveToTrash(); viewModel.showDeleteDialog = false },
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        },
                        dismissButton = { 
                            ExpressiveButton(
                                text = "Cancel", 
                                onClick = { viewModel.showDeleteDialog = false },
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }

                if (viewModel.showMoveDialog) {
                    var expanded by remember { mutableStateOf(false) }
                    val availableFolders = folders.map { it.path }
                    var selectedFolder by remember { mutableStateOf(availableFolders.firstOrNull() ?: "General") }
                    AlertDialog(
                        onDismissRequest = { viewModel.showMoveDialog = false },
                        title = { Text("Move Notes") },
                        text = {
                            Column {
                                Text("Select destination for ${viewModel.selectedNotes.size} notes:")
                                Box(modifier = Modifier.padding(top = 16.dp)) {
                                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                        OutlinedTextField(value = selectedFolder, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor())
                                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                            availableFolders.forEach { folder -> DropdownMenuItem(text = { Text(folder) }, onClick = { selectedFolder = folder; expanded = false }) }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = { 
                            ExpressiveButton(
                                text = "Move", 
                                onClick = { viewModel.moveSelectedNotes(selectedFolder); viewModel.showMoveDialog = false }
                            )
                        },
                        dismissButton = { 
                            ExpressiveButton(
                                text = "Cancel", 
                                onClick = { viewModel.showMoveDialog = false },
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }

                if (viewModel.showShareDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.showShareDialog = false },
                        title = { Text("Share Notes") },
                        text = { Text("How would you like to share ${viewModel.selectedNotes.size} notes?") },
                        confirmButton = {
                            ExpressiveButton(
                                text = "Merge as 1 PDF",
                                onClick = {
                                    viewModel.showShareDialog = false
                                    coroutineScope.launch {
                                        val exporter = PdfExporter(context)
                                        val file = exporter.exportToSharedFile(viewModel.selectedNotes.toList(), "PixelNotes_Merged.pdf")
                                        file?.let {
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Merged Notes"))
                                        }
                                        viewModel.clearSelection()
                                    }
                                }
                            )
                        },
                        dismissButton = {
                            ExpressiveButton(
                                text = "Separate PDFs",
                                onClick = {
                                    viewModel.showShareDialog = false
                                    coroutineScope.launch {
                                        val exporter = PdfExporter(context)
                                        val files = exporter.exportToSharedFiles(viewModel.selectedNotes.toList())
                                        val uris = ArrayList<Uri>(files.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) })
                                        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = "application/pdf"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Separate PDFs"))
                                        viewModel.clearSelection()
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    )
                }

                val syncLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        viewModel.backupToCloud(context)
                    }
                }

                androidx.compose.runtime.LaunchedEffect(viewModel.pendingSyncIntent) {
                    viewModel.pendingSyncIntent?.let { intent ->
                        syncLauncher.launch(intent)
                        viewModel.pendingSyncIntent = null
                    }
                }

                if (viewModel.isSyncing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(100f)
                    ) {
                        com.midknight.pixelnotes.ui.components.MorphingLoader()
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        SideMenu(
                            currentFolder = viewModel.currentFolderFilter,
                            folders = folders,
                            onFolderSelected = { folder ->
                                viewModel.currentFolderFilter = folder
                                viewModel.currentScreen = 0
                                coroutineScope.launch { drawerState.close() }
                            },
                            onSettingsSelected = {
                                viewModel.currentScreen = 2
                                coroutineScope.launch { drawerState.close() }
                            },
                            onCreateFolder = { name, parent -> viewModel.createFolder(name, parent) },
                            onRenameFolder = { oldPath, newName -> viewModel.renameFolder(oldPath, newName) },
                            onDeleteFolder = { path -> viewModel.deleteFolder(path) }
                        )
                    },
                    gesturesEnabled = viewModel.currentScreen == 0 && viewModel.selectedNotes.isEmpty()
                ) {
                    Surface(modifier = Modifier.fillMaxSize().systemBarsPadding(), color = MaterialTheme.colorScheme.background) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            when (viewModel.currentScreen) {
                                0 -> NotesScreen(
                                    notes = notes, folders = folders, currentFolder = viewModel.currentFolderFilter, selectedNotes = viewModel.selectedNotes,
                                    userEmail = viewModel.userEmail,
                                    userName = viewModel.userName,
                                    userPhotoUri = viewModel.userPhotoUri,
                                    onNoteClick = { noteWP -> viewModel.openNoteForEditing(noteWP) },
                                    onNoteLongClick = { noteWP -> viewModel.toggleSelection(noteWP) },
                                    onCreateNewNote = { isInfinite -> viewModel.openNoteForEditing(null, isInfinite) },
                                    onFolderSelected = { folderPath -> viewModel.currentFolderFilter = folderPath },
                                    onRenameFolder = { oldPath, newName -> viewModel.renameFolder(oldPath, newName) },
                                    onDeleteFolder = { path -> viewModel.deleteFolder(path) },
                                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                    onClearSelection = { viewModel.clearSelection() },
                                    onActionMove = { viewModel.showMoveDialog = true },
                                    onActionExport = {
                                        val currentDate = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())
                                        pdfLauncher.launch("PixelNotes_Merged_$currentDate.pdf")
                                    },
                                    onActionShare = {
                                        if (viewModel.selectedNotes.size == 1) {
                                            coroutineScope.launch {
                                                val exporter = PdfExporter(context)
                                                val file = exporter.exportToSharedFile(viewModel.selectedNotes.toList(), "${viewModel.selectedNotes.first().note.title}.pdf")
                                                file?.let {
                                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                                                }
                                                viewModel.clearSelection()
                                            }
                                        } else viewModel.showShareDialog = true
                                    },
                                    onActionDelete = { viewModel.showDeleteDialog = true },
                                    onActionRestore = { viewModel.restoreFromTrash() },
                                    onSignInClick = { viewModel.signIn(context) },
                                    onSignOutClick = { viewModel.signOut(context) },
                                    viewModel = viewModel
                                )
                                1 -> DrawingScreen(viewModel = viewModel)
                                2 -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
