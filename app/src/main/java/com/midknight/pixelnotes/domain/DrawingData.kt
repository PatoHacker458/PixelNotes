package com.midknight.pixelnotes.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb

data class PointData(val x: Float, val y: Float)

data class StrokeData(
    val points: List<PointData>,
    val colorArgb: Int,
    val strokeWidth: Float,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false
) {
    fun toPath(): Path {
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            var prevX = points.first().x
            var prevY = points.first().y

            for (i in 1 until points.size) {
                val currentX = points[i].x
                val currentY = points[i].y
                val midX = (prevX + currentX) / 2f
                val midY = (prevY + currentY) / 2f

                path.quadraticBezierTo(prevX, prevY, midX, midY)
                prevX = currentX
                prevY = currentY
            }
            path.lineTo(prevX, prevY)
        }
        return path
    }
}

fun Stroke.toData(points: List<PointData>): StrokeData {
    return StrokeData(
        points = points,
        colorArgb = this.color.toArgb(),
        strokeWidth = this.strokeWidth,
        isEraser = false,
        isHighlighter = false
    )
}