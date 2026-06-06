package com.midknight.pixelnotes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.midknight.pixelnotes.data.FolderEntity

data class FolderNode(val folder: FolderEntity, val children: List<FolderNode>)

fun buildFolderTree(folders: List<FolderEntity>): List<FolderNode> {
    val map = folders.groupBy { it.parentPath }
    fun getChildren(parentPath: String?): List<FolderNode> {
        return map[parentPath]?.map { FolderNode(it, getChildren(it.path)) } ?: emptyList()
    }
    return getChildren(null)
}

@Composable
fun SideMenu(
    currentFolder: String,
    folders: List<FolderEntity>,
    onFolderSelected: (String) -> Unit,
    onSettingsSelected: () -> Unit,
    onCreateFolder: (String, String?) -> Unit
) {
    val tree = remember(folders) { buildFolderTree(folders) }
    var showDialog by remember { mutableStateOf(false) }
    var targetParentPath by remember { mutableStateOf<String?>(null) }
    var newFolderName by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        onCreateFolder(newFolderName, targetParentPath)
                    }
                    showDialog = false
                    newFolderName = ""
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Pixel Notes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = {
                targetParentPath = null
                showDialog = true
            }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Root Folder", tint = MaterialTheme.colorScheme.primary)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                SideMenuItem(
                    text = "Todas",
                    isSelected = currentFolder == "Todas",
                    depth = 0,
                    hasChildren = false,
                    isExpanded = false,
                    onToggleExpand = {},
                    onClick = { onFolderSelected("Todas") },
                    onAddSubfolder = null
                )
            }

            items(tree) { node ->
                FolderTreeNode(
                    node = node,
                    currentFolder = currentFolder,
                    depth = 0,
                    onFolderSelected = onFolderSelected,
                    onAddSubfolder = { path ->
                        targetParentPath = path
                        showDialog = true
                    }
                )
            }
        }

        SideMenuItem(
            text = "Settings",
            isSelected = false,
            depth = 0,
            hasChildren = false,
            isExpanded = false,
            onToggleExpand = {},
            onClick = onSettingsSelected,
            onAddSubfolder = null,
            iconOverride = Icons.Default.Settings
        )
    }
}

@Composable
fun FolderTreeNode(
    node: FolderNode,
    currentFolder: String,
    depth: Int,
    onFolderSelected: (String) -> Unit,
    onAddSubfolder: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    SideMenuItem(
        text = node.folder.name,
        isSelected = currentFolder == node.folder.path,
        depth = depth,
        hasChildren = node.children.isNotEmpty(),
        isExpanded = isExpanded,
        onToggleExpand = { isExpanded = !isExpanded },
        onClick = { onFolderSelected(node.folder.path) },
        onAddSubfolder = { onAddSubfolder(node.folder.path) }
    )

    AnimatedVisibility(visible = isExpanded) {
        Column {
            node.children.forEach { childNode ->
                FolderTreeNode(
                    node = childNode,
                    currentFolder = currentFolder,
                    depth = depth + 1,
                    onFolderSelected = onFolderSelected,
                    onAddSubfolder = onAddSubfolder
                )
            }
        }
    }
}

@Composable
private fun SideMenuItem(
    text: String,
    isSelected: Boolean,
    depth: Int,
    hasChildren: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    onAddSubfolder: (() -> Unit)?,
    iconOverride: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasChildren) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onToggleExpand() }
            )
        } else {
            val icon = iconOverride ?: if (text == "Todas") Icons.Default.AllInbox else Icons.Default.Folder
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        if (onAddSubfolder != null && text != "Todas") {
            IconButton(onClick = onAddSubfolder, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Subfolder", tint = contentColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}