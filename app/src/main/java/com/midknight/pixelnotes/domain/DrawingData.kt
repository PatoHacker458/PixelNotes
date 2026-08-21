package com.midknight.pixelnotes.domain

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path

data class PointData(val x: Float, val y: Float, val p: Float = 1f)

data class StrokeData(
    val points: List<PointData>,
    val colorArgb: Int,
    val strokeWidth: Float,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val shapeType: Int = 0
) {
    @Transient
    private var memoizedPath: Path? = null
    
    @Transient
    private var memoizedBounds: Rect? = null

    @Transient
    private var memoizedPressurePath: Path? = null

    fun invalidateCache() {
        memoizedPath = null
        memoizedBounds = null
        memoizedPressurePath = null
    }

    fun toPath(): Path {
        memoizedPath?.let { return it }
        
        val path = Path()
        if (points.isEmpty()) return path

        path.moveTo(points.first().x, points.first().y)

        if (points.size <= 10 || points.size == 37) {
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
        } else {
            var prevX = points.first().x
            var prevY = points.first().y

            for (i in 1 until points.size) {
                val currentX = points[i].x
                val currentY = points[i].y
                val midX = (prevX + currentX) / 2f
                val midY = (prevY + currentY) / 2f

                path.quadraticTo(prevX, prevY, midX, midY)
                prevX = currentX
                prevY = currentY
            }
            path.lineTo(prevX, prevY)
        }
        
        memoizedPath = path
        return path
    }

    fun toPressurePath(): Path {
        memoizedPressurePath?.let { return it }
        
        val path = Path()
        
        val filtered = mutableListOf<PointData>()
        if (points.isNotEmpty()) {
            filtered.add(points[0])
            for (i in 1 until points.size) {
                val p1 = points[i-1]
                val p2 = points[i]
                // Only add points that have moved enough to have a direction
                if (kotlin.math.hypot(p2.x - p1.x, p2.y - p1.y) > 0.001f || i == points.size - 1) {
                    filtered.add(p2)
                }
            }
        }

        if (filtered.size < 2) {
            if (filtered.size == 1) {
                val p = filtered[0]
                val r = (strokeWidth * (if (p.p <= 0.2f) 0.2f else p.p)) / 2f
                path.addOval(Rect(p.x - r, p.y - r, p.x + r, p.y + r))
            }
            memoizedPressurePath = path
            return path
        }

        val leftPoints = mutableListOf<Offset>()
        val rightPoints = mutableListOf<Offset>()
        
        var lastNx = 0f
        var lastNy = 0f

        for (i in filtered.indices) {
            val curr = filtered[i]
            val prev = if (i > 0) filtered[i - 1] else null
            val next = if (i < filtered.size - 1) filtered[i + 1] else null

            var dx: Float
            var dy: Float
            
            if (prev != null && next != null) {
                dx = next.x - prev.x
                dy = next.y - prev.y
            } else if (next != null) {
                dx = next.x - curr.x
                dy = next.y - curr.y
            } else {
                dx = curr.x - prev!!.x
                dy = curr.y - prev.y
            }

            val mag = kotlin.math.hypot(dx, dy)
            
            val nx: Float
            val ny: Float
            
            if (mag > 0.000001f) {
                nx = -dy / mag
                ny = dx / mag
                lastNx = nx
                lastNy = ny
            } else {
                nx = lastNx
                ny = lastNy
            }

            // More generous pressure: ensure USI 2.0 low pressure is still very visible
            val pressureValue = if (curr.p <= 0.1f) 0.3f else if (curr.p < 0.2f) 0.4f else curr.p
            val r = (strokeWidth * pressureValue) / 2f

            leftPoints.add(Offset(curr.x + nx * r, curr.y + ny * r))
            rightPoints.add(Offset(curr.x - nx * r, curr.y - ny * r))
        }

        if (leftPoints.isEmpty() || rightPoints.isEmpty()) return path

        path.moveTo(leftPoints[0].x, leftPoints[0].y)
        for (i in 1 until leftPoints.size) {
            val p = leftPoints[i]
            val prev = leftPoints[i-1]
            val midX = (p.x + prev.x) / 2f
            val midY = (p.y + prev.y) / 2f
            path.quadraticTo(prev.x, prev.y, midX, midY)
        }
        path.lineTo(leftPoints.last().x, leftPoints.last().y)

        val rp = rightPoints.reversed()
        path.lineTo(rp[0].x, rp[0].y)
        for (i in 1 until rp.size) {
            val p = rp[i]
            val prev = rp[i-1]
            val midX = (p.x + prev.x) / 2f
            val midY = (p.y + prev.y) / 2f
            path.quadraticTo(prev.x, prev.y, midX, midY)
        }
        path.lineTo(rp.last().x, rp.last().y)
        
        path.close()
        
        // Add rounded caps for low point counts or start/end
        val startP = filtered.first()
        val startR = (strokeWidth * (if (startP.p <= 0.1f) 0.3f else startP.p)) / 2f
        path.addOval(Rect(startP.x - startR, startP.y - startR, startP.x + startR, startP.y + startR))
        
        val endP = filtered.last()
        val endR = (strokeWidth * (if (endP.p <= 0.1f) 0.3f else endP.p)) / 2f
        path.addOval(Rect(endP.x - endR, endP.y - endR, endP.x + endR, endP.y + endR))

        memoizedPressurePath = path
        return path
    }

    fun getBounds(): Rect {
        memoizedBounds?.let { return it }
        
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
        val bounds = Rect(minX, minY, maxX, maxY)
        memoizedBounds = bounds
        return bounds
    }

    fun translate(dx: Float, dy: Float): StrokeData {
        return this.copy(points = points.map { PointData(it.x + dx, it.y + dy, it.p) })
    }
}

fun isPointInPolygon(point: PointData, polygon: List<PointData>): Boolean {
    var isInside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]
        if (((pi.y > point.y) != (pj.y > point.y)) && (point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x)) {
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