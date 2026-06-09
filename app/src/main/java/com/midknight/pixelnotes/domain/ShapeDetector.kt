package com.midknight.pixelnotes.domain

import kotlin.math.*

fun detectAndSnapShape(points: List<PointData>): List<PointData>? {
    if (points.size < 10) return null

    val start = points.first()
    val end = points.last()

    // 1. ¿ES UNA LÍNEA RECTA?
    var pathLength = 0f
    for (i in 0 until points.size - 1) {
        pathLength += hypot(points[i+1].x - points[i].x, points[i+1].y - points[i].y)
    }
    val straightDist = hypot(end.x - start.x, end.y - start.y)

    if (straightDist > 50f && pathLength / straightDist < 1.15f) {
        return listOf(start, end)
    }

    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
    points.forEach { p ->
        if (p.x < minX) minX = p.x; if (p.y < minY) minY = p.y
        if (p.x > maxX) maxX = p.x; if (p.y > maxY) maxY = p.y
    }
    val w = maxX - minX
    val h = maxY - minY
    val cx = minX + w / 2f
    val cy = minY + h / 2f

    if (straightDist < max(w, h) * 0.35f && w > 30f && h > 30f) {

        val epsilon = max(w, h) * 0.09f
        val simplified = simplifyPath(points, epsilon).toMutableList()

        simplified[simplified.lastIndex] = simplified.first()

        val edges = simplified.size - 1

        if (edges in 3..5) {

            if (edges == 4) {
                var isAxisAligned = true
                for (i in 0 until 4) {
                    val dx = abs(simplified[i+1].x - simplified[i].x)
                    val dy = abs(simplified[i+1].y - simplified[i].y)
                    // Si dibujó un "diamante" inclinado, respetamos su inclinación
                    if (dx > w * 0.2f && dy > h * 0.2f) {
                        isAxisAligned = false
                        break
                    }
                }
                if (isAxisAligned) {
                    return listOf(
                        PointData(minX, minY), PointData(maxX, minY),
                        PointData(maxX, maxY), PointData(minX, maxY),
                        PointData(minX, minY)
                    )
                }
            }
            return simplified

        } else {
            val ovalPoints = mutableListOf<PointData>()
            val a = w / 2f
            val b = h / 2f
            for (i in 0..36) {
                val angle = i * 10.0 * PI / 180.0
                ovalPoints.add(PointData((cx + a * cos(angle)).toFloat(), (cy + b * sin(angle)).toFloat()))
            }
            return ovalPoints
        }
    }

    return null
}

private fun simplifyPath(points: List<PointData>, epsilon: Float): List<PointData> {
    if (points.size < 3) return points
    var maxDistance = 0f
    var index = 0
    val end = points.size - 1

    for (i in 1 until end) {
        val dist = perpendicularDistance(points[i], points[0], points[end])
        if (dist > maxDistance) {
            maxDistance = dist
            index = i
        }
    }

    return if (maxDistance > epsilon) {
        val left = simplifyPath(points.subList(0, index + 1), epsilon)
        val right = simplifyPath(points.subList(index, end + 1), epsilon)
        left.dropLast(1) + right
    } else {
        listOf(points[0], points[end])
    }
}

private fun perpendicularDistance(pt: PointData, lineStart: PointData, lineEnd: PointData): Float {
    val dx = lineEnd.x - lineStart.x
    val dy = lineEnd.y - lineStart.y
    val mag = hypot(dx, dy)
    if (mag == 0f) return hypot(pt.x - lineStart.x, pt.y - lineStart.y)
    return abs(dy * pt.x - dx * pt.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x) / mag
}