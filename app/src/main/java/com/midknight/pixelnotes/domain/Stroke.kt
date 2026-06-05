package com.midknight.pixelnotes.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

data class Stroke(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)