package com.midknight.pixelnotes.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SideMenu(
    currentSelection: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf("Notes", "Folders", "Settings")
    val icons = listOf(Icons.Filled.Create, Icons.Filled.Folder, Icons.Filled.Settings)

    NavigationRail(modifier = modifier) {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                icon = { Icon(imageVector = icons[index], contentDescription = item) },
                label = { Text(text = item) },
                selected = currentSelection == index,
                onClick = { onOptionSelected(index) }
            )
        }
    }
}