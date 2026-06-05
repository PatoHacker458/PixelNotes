package com.midknight.pixelnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.midknight.pixelnotes.data.Note
import com.midknight.pixelnotes.data.NoteDatabase
import com.midknight.pixelnotes.ui.components.SideMenu
import com.midknight.pixelnotes.ui.screens.DrawingScreen
import com.midknight.pixelnotes.ui.screens.NotesScreen
import com.midknight.pixelnotes.ui.screens.PlaceholderScreen
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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableIntStateOf(0) }
                    var selectedNote by remember { mutableStateOf<Note?>(null) }

                    Row(modifier = Modifier.fillMaxSize()) {
                        SideMenu(
                            currentSelection = currentScreen,
                            onOptionSelected = { newScreen ->
                                if (newScreen == 1) selectedNote = null
                                currentScreen = newScreen
                            }
                        )

                        Surface(modifier = Modifier.weight(1f)) {
                            when (currentScreen) {
                                0 -> NotesScreen(
                                    notes = notes,
                                    onNoteClick = { note ->
                                        selectedNote = note
                                        currentScreen = 1
                                    }
                                )
                                1 -> DrawingScreen(
                                    noteToEdit = selectedNote,
                                    onSaveNote = { note ->
                                        if (note.id == 0) {
                                            viewModel.saveNote(note)
                                        } else {
                                            viewModel.updateNote(note)
                                        }
                                        selectedNote = null
                                        currentScreen = 0
                                    },
                                    onNavigateBack = {
                                        selectedNote = null
                                        currentScreen = 0
                                    }
                                )
                                2 -> PlaceholderScreen(title = "Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}