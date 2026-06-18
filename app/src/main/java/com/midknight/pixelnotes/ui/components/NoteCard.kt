package com.midknight.pixelnotes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllOut
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.midknight.pixelnotes.data.NoteWithPages
import com.midknight.pixelnotes.domain.HapticManager

@Composable
fun NoteCard(
    noteWithPages: NoteWithPages,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPositioned: (Rect) -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val haptic = remember { HapticManager(context) }
    val scale by animateFloatAsState(targetValue = if (isSelected) 0.95f else 1f, label = "ScaleAnimation")
    val note = noteWithPages.note

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
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
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${noteWithPages.pages.size} page(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                    )

                    Icon(
                        imageVector = if (note.isInfinite) Icons.Default.AllOut else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
