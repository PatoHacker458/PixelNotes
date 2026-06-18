package com.midknight.pixelnotes.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.midknight.pixelnotes.data.FolderEntity
import com.midknight.pixelnotes.domain.HapticManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: FolderEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPositioned: (Rect) -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val haptic = remember { HapticManager(context) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { layoutCoordinates ->
                onPositioned(layoutCoordinates.boundsInWindow())
            }
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(32.dp)
            )
            .clip(RoundedCornerShape(32.dp))
            .clickable(
                enabled = enabled,
                onClick = {
                    haptic.click()
                    onClick()
                }
            ),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Box {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
