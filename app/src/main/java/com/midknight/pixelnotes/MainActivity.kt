package com.midknight.pixelnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        SideMenu(
                            currentSelection = viewModel.currentScreen,
                            onOptionSelected = { newScreen ->
                                if (newScreen == 1) viewModel.openNoteForEditing(null)
                                else viewModel.currentScreen = newScreen
                            }
                        )

                        Surface(modifier = Modifier.weight(1f)) {
                            when (viewModel.currentScreen) {
                                0 -> NotesScreen(
                                    notes = notes,
                                    onNoteClick = { note ->
                                        viewModel.openNoteForEditing(note)
                                    }
                                )
                                1 -> DrawingScreen(viewModel = viewModel)
                                2 -> PlaceholderScreen(title = "Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}