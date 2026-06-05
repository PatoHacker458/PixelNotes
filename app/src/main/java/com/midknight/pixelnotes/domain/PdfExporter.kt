package com.midknight.pixelnotes.domain

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.midknight.pixelnotes.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfExporter(private val context: Context) {

    suspend fun exportToPdf(note: Note, uri: Uri) {
        withContext(Dispatchers.IO) {
            var maxX = 1080f
            var maxY = 1527f

            note.drawingData.forEach { stroke ->
                stroke.points.forEach { point ->
                    if (point.x > maxX) maxX = point.x
                    if (point.y > maxY) maxY = point.y
                }
            }

            val pdfWidth = maxX.toInt() + 40
            val pdfHeight = (pdfWidth * 1.414f).toInt().coerceAtLeast(maxY.toInt() + 40)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val bgPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, pdfWidth.toFloat(), pdfHeight.toFloat(), bgPaint)

            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }

            note.drawingData.forEach { strokeData ->
                paint.color = strokeData.colorArgb
                paint.strokeWidth = strokeData.strokeWidth

                val path = android.graphics.Path()
                if (strokeData.points.isNotEmpty()) {
                    path.moveTo(strokeData.points.first().x, strokeData.points.first().y)
                    for (i in 1 until strokeData.points.size) {
                        path.lineTo(strokeData.points[i].x, strokeData.points[i].y)
                    }
                }
                canvas.drawPath(path, paint)
            }

            document.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()
        }
    }
}