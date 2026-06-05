package com.midknight.pixelnotes.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.midknight.pixelnotes.ui.components.DrawingCanvas

@Composable
fun DrawingScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        DrawingCanvas()
    }
}