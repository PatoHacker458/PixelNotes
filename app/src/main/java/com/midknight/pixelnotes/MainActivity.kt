package com.midknight.pixelnotes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.midknight.pixelnotes.data.NoteDatabase
import com.midknight.pixelnotes.ui.components.SideMenu
import com.midknight.pixelnotes.ui.screens.DrawingScreen
import com.midknight.pixelnotes.ui.screens.NotesScreen
import com.midknight.pixelnotes.ui.screens.SettingsScreen
import com.midknight.pixelnotes.ui.theme.PixelNotesTheme
import com.midknight.pixelnotes.ui.viewmodels.NotesViewModel
import com.midknight.pixelnotes.ui.viewmodels.NotesViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = NoteDatabase.getDatabase(this)
        val dao = database.noteDao()

        setContent {
            PixelNotesTheme {
                val viewModel: NotesViewModel = viewModel(factory = NotesViewModelFactory(dao))
                val notes by viewModel.notes.collectAsState()
                val folders by viewModel.folders.collectAsState()
                val context = LocalContext.current

                BackHandler(enabled = viewModel.currentScreen != 0) {
                    if (viewModel.currentScreen == 1) {
                        val isBlank = viewModel.selectedNote == null && viewModel.currentStrokes.isEmpty() && viewModel.currentBackgroundUri == null && viewModel.currentTitle == "New Note"
                        if (!isBlank) {
                            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        }
                        viewModel.closeEditing()
                    } else {
                        viewModel.currentScreen = 0
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (viewModel.currentScreen != 1) {
                            SideMenu(
                                currentFolder = viewModel.currentFolderFilter,
                                folders = folders,
                                onFolderSelected = { folder ->
                                    viewModel.currentFolderFilter = folder
                                    viewModel.currentScreen = 0
                                },
                                onSettingsSelected = {
                                    viewModel.currentScreen = 2
                                },
                                onCreateFolder = { name, parent ->
                                    viewModel.createFolder(name, parent)
                                },
                                onRenameFolder = { oldPath, newName ->
                                    viewModel.renameFolder(oldPath, newName)
                                },
                                onDeleteFolder = { path ->
                                    viewModel.deleteFolder(path)
                                }
                            )
                        }

                        Surface(modifier = Modifier.weight(1f)) {
                            when (viewModel.currentScreen) {
                                0 -> NotesScreen(
                                    notes = notes,
                                    folders = folders,
                                    currentFolder = viewModel.currentFolderFilter,
                                    onNoteClick = { note ->
                                        viewModel.openNoteForEditing(note)
                                    },
                                    onDeleteNote = { note ->
                                        viewModel.deleteNote(note)
                                    },
                                    onMoveNote = { note, folder ->
                                        viewModel.moveNote(note, folder)
                                    },
                                    onFolderSelected = { folderPath ->
                                        viewModel.currentFolderFilter = folderPath
                                    },
                                    onRenameFolder = { oldPath, newName ->
                                        viewModel.renameFolder(oldPath, newName)
                                    },
                                    onDeleteFolder = { path ->
                                        viewModel.deleteFolder(path)
                                    }
                                )
                                1 -> DrawingScreen(viewModel = viewModel)
                                2 -> SettingsScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}