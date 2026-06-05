package com.midknight.pixelnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.midknight.pixelnotes.ui.components.SideMenu
import com.midknight.pixelnotes.ui.screens.NotesScreen
import com.midknight.pixelnotes.ui.screens.PlaceholderScreen
import com.midknight.pixelnotes.ui.theme.PixelNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PixelNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableIntStateOf(0) }

                    Row(modifier = Modifier.fillMaxSize()) {
                        SideMenu(
                            currentSelection = currentScreen,
                            onOptionSelected = { newScreen -> currentScreen = newScreen }
                        )

                        Surface(modifier = Modifier.weight(1f)) {
                            when (currentScreen) {
                                0 -> NotesScreen()
                                1 -> PlaceholderScreen(title = "Folders")
                                2 -> PlaceholderScreen(title = "Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}