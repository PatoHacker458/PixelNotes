package com.midknight.pixelnotes.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb

data class PointData(val x: Float, val y: Float)

data class StrokeData(
    val points: List<PointData>,
    val colorArgb: Int,
    val strokeWidth: Float
) {
    fun toPath(): Path {
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
        }
        return path
    }
}

fun Stroke.toData(points: List<PointData>): StrokeData {
    return StrokeData(
        points = points,
        colorArgb = this.color.toArgb(),
        strokeWidth = this.strokeWidth
    )
}