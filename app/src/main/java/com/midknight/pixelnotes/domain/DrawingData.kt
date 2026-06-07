package com.midknight.pixelnotes.domain

import androidx.compose.ui.geometry.Rect
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

    fun getBounds(): Rect {
        if (points.isEmpty()) return Rect.Zero
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        points.forEach {
            if (it.x < minX) minX = it.x
            if (it.x > maxX) maxX = it.x
            if (it.y < minY) minY = it.y
            if (it.y > maxY) maxY = it.y
        }
        return Rect(minX, minY, maxX, maxY)
    }

    fun translate(dx: Float, dy: Float): StrokeData {
        return this.copy(points = points.map { PointData(it.x + dx, it.y + dy) })
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

fun isPointInPolygon(point: PointData, polygon: List<PointData>): Boolean {
    var isInside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]
        if (((pi.y > point.y) != (pj.y > point.y)) &&
            (point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x)) {
            isInside = !isInside
        }
        j = i
    }
    return isInside
}

fun isPointInRect(point: PointData, p1: PointData, p2: PointData): Boolean {
    val minX = minOf(p1.x, p2.x)
    val maxX = maxOf(p1.x, p2.x)
    val minY = minOf(p1.y, p2.y)
    val maxY = maxOf(p1.y, p2.y)
    return point.x in minX..maxX && point.y in minY..maxY
}