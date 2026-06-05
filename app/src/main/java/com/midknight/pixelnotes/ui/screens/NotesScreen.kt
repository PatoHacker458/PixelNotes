package com.midknight.pixelnotes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.midknight.pixelnotes.domain.Note
import com.midknight.pixelnotes.ui.components.NoteCard

@Composable
fun NotesScreen() {
    val dummyNotes = listOf(
        Note("Pixel Notes Project", "Clone Samsung Notes and Google Keep with USI 2.0 support and PDF export.", "Jun 05"),
        Note("Shopping List", "Eggs, Milk, Bread, Coffee, and remote batteries.", "Jun 04"),
        Note("UI Ideas", "Use Jetpack Compose for adaptive UI on Pixel Tablet.", "Jun 02"),
        Note("Physics Notes", "Thermodynamics is the branch of physics that deals with heat, work, and temperature.", "May 28"),
        Note("Reading List", "1. Dune\n2. 1984\n3. Brave New World", "May 25"),
        Note("Code Review", "Remember to push changes to GitHub at the end of the day.", "May 20")
    )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(dummyNotes) { note ->
            NoteCard(note = note)
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}