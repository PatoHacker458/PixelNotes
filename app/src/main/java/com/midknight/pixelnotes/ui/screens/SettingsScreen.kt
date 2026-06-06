package com.midknight.pixelnotes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ListItem(
                headlineContent = { Text("Dynamic Theming", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                supportingContent = { Text("Material Expressive automatically adapts to your system wallpaper colors.") },
                leadingContent = {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            ListItem(
                headlineContent = { Text("Privacy & Security", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                supportingContent = { Text("All your notes and vector drawings are stored securely encrypted on this local device.") },
                leadingContent = {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            ListItem(
                headlineContent = { Text("About Pixel Notes", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                supportingContent = { Text("Version 0.3.0-alpha\nBuilt with Jetpack Compose & Material 3") },
                leadingContent = {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}