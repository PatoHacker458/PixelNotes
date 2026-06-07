package com.midknight.pixelnotes.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.midknight.pixelnotes.data.NoteWithPages
import com.midknight.pixelnotes.data.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfExporter(private val context: Context) {

    private fun drawPageOnPdf(page: PageEntity, pdfCanvas: Canvas, pdfWidth: Int, pdfHeight: Int) {
        val bgPaint = Paint().apply {
            color = if (page.canvasColor == -1) android.graphics.Color.WHITE else page.canvasColor
            style = Paint.Style.FILL
        }
        pdfCanvas.drawRect(0f, 0f, pdfWidth.toFloat(), pdfHeight.toFloat(), bgPaint)

        if (page.paperStyle > 0) {
            val spacing = 80f
            val paperPaint = Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 2f; alpha = 100; style = Paint.Style.FILL }
            when (page.paperStyle) {
                1 -> { var y = spacing; while (y < pdfHeight) { pdfCanvas.drawLine(0f, y, pdfWidth.toFloat(), y, paperPaint); y += spacing } }
                2 -> {
                    var y = spacing; while (y < pdfHeight) { pdfCanvas.drawLine(0f, y, pdfWidth.toFloat(), y, paperPaint); y += spacing }
                    var x = spacing; while (x < pdfWidth) { pdfCanvas.drawLine(x, 0f, x, pdfHeight.toFloat(), paperPaint); x += spacing }
                }
                3 -> {
                    var y = spacing; while (y < pdfHeight) {
                        var x = spacing; while (x < pdfWidth) { pdfCanvas.drawCircle(x, y, 3f, paperPaint); x += spacing }
                        y += spacing
                    }
                }
            }
        }

        if (page.backgroundUri != null) {
            try {
                val bgUri = Uri.parse(page.backgroundUri)
                context.contentResolver.openInputStream(bgUri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val targetRatio = pdfWidth.toFloat() / pdfHeight.toFloat()
                        val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val srcRect = if (bitmapRatio > targetRatio) {
                            val newWidth = (bitmap.height * targetRatio).toInt()
                            val xOffset = (bitmap.width - newWidth) / 2
                            Rect(xOffset, 0, xOffset + newWidth, bitmap.height)
                        } else {
                            val newHeight = (bitmap.width / targetRatio).toInt()
                            val yOffset = (bitmap.height - newHeight) / 2
                            Rect(0, yOffset, bitmap.width, yOffset + newHeight)
                        }
                        val destRect = Rect(0, 0, pdfWidth, pdfHeight)
                        pdfCanvas.drawBitmap(bitmap, srcRect, destRect, null)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        val strokeBitmap = Bitmap.createBitmap(pdfWidth, pdfHeight, Bitmap.Config.ARGB_8888)
        val strokeCanvas = Canvas(strokeBitmap)
        val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
        val clearXfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        page.drawingData.forEach { strokeData ->
            if (strokeData.isEraser) { paint.xfermode = clearXfermode; paint.color = android.graphics.Color.TRANSPARENT }
            else { paint.xfermode = null; paint.color = strokeData.colorArgb }
            paint.strokeWidth = strokeData.strokeWidth
            val path = android.graphics.Path()
            if (strokeData.points.isNotEmpty()) {
                path.moveTo(strokeData.points.first().x, strokeData.points.first().y)
                var prevX = strokeData.points.first().x
                var prevY = strokeData.points.first().y
                for (i in 1 until strokeData.points.size) {
                    val currentX = strokeData.points[i].x
                    val currentY = strokeData.points[i].y
                    val midX = (prevX + currentX) / 2f
                    val midY = (prevY + currentY) / 2f
                    path.quadTo(prevX, prevY, midX, midY)
                    prevX = currentX
                    prevY = currentY
                }
                path.lineTo(prevX, prevY)
            }
            strokeCanvas.drawPath(path, paint)
        }
        pdfCanvas.drawBitmap(strokeBitmap, 0f, 0f, null)
        strokeBitmap.recycle()
    }

    suspend fun exportToPdf(notes: List<NoteWithPages>, uri: Uri) {
        withContext(Dispatchers.IO) {
            val pdfWidth = 1080
            val pdfHeight = 1527
            val document = PdfDocument()
            var pageIndex = 1

            notes.forEach { noteWP ->
                noteWP.pages.forEach { page ->
                    val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, pageIndex++).create()
                    val pdfPage = document.startPage(pageInfo)
                    drawPageOnPdf(page, pdfPage.canvas, pdfWidth, pdfHeight)
                    document.finishPage(pdfPage)
                }
            }
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            document.close()
        }
    }

    suspend fun exportToSharedFile(notes: List<NoteWithPages>, fileName: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDir = File(context.cacheDir, "pdfs")
                if (!pdfDir.exists()) pdfDir.mkdirs()
                val file = File(pdfDir, fileName)
                val pdfWidth = 1080
                val pdfHeight = 1527
                val document = PdfDocument()
                var pageIndex = 1

                notes.forEach { noteWP ->
                    noteWP.pages.forEach { page ->
                        val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, pageIndex++).create()
                        val pdfPage = document.startPage(pageInfo)
                        drawPageOnPdf(page, pdfPage.canvas, pdfWidth, pdfHeight)
                        document.finishPage(pdfPage)
                    }
                }
                FileOutputStream(file).use { document.writeTo(it) }
                document.close()
                file
            } catch (e: Exception) { null }
        }
    }

    suspend fun exportToSharedFiles(notes: List<NoteWithPages>): List<File> {
        return withContext(Dispatchers.IO) {
            val pdfDir = File(context.cacheDir, "pdfs")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            notes.mapNotNull { noteWP ->
                try {
                    val fileName = "${noteWP.note.title.replace(" ", "_")}_${noteWP.note.id}.pdf"
                    val file = File(pdfDir, fileName)
                    val pdfWidth = 1080
                    val pdfHeight = 1527
                    val document = PdfDocument()
                    var pageIndex = 1

                    noteWP.pages.forEach { page ->
                        val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, pageIndex++).create()
                        val pdfPage = document.startPage(pageInfo)
                        drawPageOnPdf(page, pdfPage.canvas, pdfWidth, pdfHeight)
                        document.finishPage(pdfPage)
                    }
                    FileOutputStream(file).use { document.writeTo(it) }
                    document.close()
                    file
                } catch(e: Exception) { null }
            }
        }
    }

    suspend fun exportSinglePageToPdf(page: PageEntity, uri: Uri) {
        withContext(Dispatchers.IO) {
            val pdfWidth = 1080
            val pdfHeight = 1527
            val document = PdfDocument()

            val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create()
            val pdfPage = document.startPage(pageInfo)
            drawPageOnPdf(page, pdfPage.canvas, pdfWidth, pdfHeight)
            document.finishPage(pdfPage)

            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            document.close()
        }
    }
}