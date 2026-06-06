package com.midknight.pixelnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SideMenu(
    currentFolder: String,
    folders: List<String>,
    onFolderSelected: (String) -> Unit,
    onSettingsSelected: () -> Unit
) {
    val iconMap = mapOf(
        "Todas" to Icons.Default.AllInbox,
        "General" to Icons.Default.Folder,
        "Trabajo" to Icons.Default.Work,
        "Escuela" to Icons.Default.School,
        "Personal" to Icons.Default.Person
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(220.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text = "Pixel Notes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp, start = 8.dp, top = 8.dp)
        )

        folders.forEach { folderName ->
            SideMenuItem(
                text = folderName,
                icon = iconMap[folderName] ?: Icons.Default.Folder,
                isSelected = currentFolder == folderName,
                onClick = { onFolderSelected(folderName) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        SideMenuItem(
            text = "Settings",
            icon = Icons.Default.Settings,
            isSelected = false,
            onClick = onSettingsSelected
        )
    }
}

@Composable
private fun SideMenuItem(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = contentColor, style = MaterialTheme.typography.bodyLarge, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}