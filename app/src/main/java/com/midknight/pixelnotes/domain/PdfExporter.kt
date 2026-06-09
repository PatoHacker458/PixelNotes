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
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.midknight.pixelnotes.data.CustomFont
import com.midknight.pixelnotes.data.NoteDatabase
import com.midknight.pixelnotes.data.NoteWithPages
import com.midknight.pixelnotes.data.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfExporter(private val context: Context) {

    private fun drawPageOnPdf(page: PageEntity, pdfCanvas: Canvas, pdfWidth: Int, pdfHeight: Int, customFonts: List<CustomFont>) {
        val bgPaint = Paint().apply {
            color = if (page.canvasColor == -1) android.graphics.Color.WHITE else page.canvasColor
            style = Paint.Style.FILL
        }
        pdfCanvas.drawRect(0f, 0f, pdfWidth.toFloat(), pdfHeight.toFloat(), bgPaint)

        if (page.backgroundUri != null) {
            if (page.backgroundUri.contains("?pdfPage=")) {
                try {
                    val parts = page.backgroundUri.split("?pdfPage=")
                    val file = File(parts[0])
                    if (file.exists()) {
                        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(fd)
                        val pageIdx = parts[1].toInt()
                        if (pageIdx < renderer.pageCount) {
                            val pdfPage = renderer.openPage(pageIdx)
                            val bmp = Bitmap.createBitmap(pdfWidth, pdfHeight, Bitmap.Config.ARGB_8888)
                            val bgCanvas = Canvas(bmp)
                            bgCanvas.drawColor(android.graphics.Color.WHITE)
                            pdfPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pdfCanvas.drawBitmap(bmp, 0f, 0f, null)
                            pdfPage.close()
                            bmp.recycle()
                        }
                        renderer.close()
                        fd.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
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
                            pdfCanvas.drawBitmap(bitmap, srcRect, Rect(0, 0, pdfWidth, pdfHeight), null)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        page.imageData.forEach { img ->
            try {
                val uri = Uri.parse(img.uri)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val destRect = Rect(img.x.toInt(), img.y.toInt(), (img.x + img.width).toInt(), (img.y + img.height).toInt())
                        pdfCanvas.drawBitmap(bitmap, null, destRect, null)
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val strokeBitmap = Bitmap.createBitmap(pdfWidth, pdfHeight, Bitmap.Config.ARGB_8888)
        val strokeCanvas = Canvas(strokeBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val clearXfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        page.drawingData.forEach { strokeData ->
            if (strokeData.isEraser) {
                paint.xfermode = clearXfermode
                paint.color = android.graphics.Color.TRANSPARENT
            } else {
                paint.xfermode = null
                paint.color = strokeData.colorArgb
            }
            paint.strokeWidth = strokeData.strokeWidth
            val path = android.graphics.Path()
            if (strokeData.points.isNotEmpty()) {
                path.moveTo(strokeData.points.first().x, strokeData.points.first().y)

                if (strokeData.points.size <= 10 || strokeData.points.size == 37) {
                    for (i in 1 until strokeData.points.size) {
                        path.lineTo(strokeData.points[i].x, strokeData.points[i].y)
                    }
                } else {
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
            }
            strokeCanvas.drawPath(path, paint)
        }

        pdfCanvas.drawBitmap(strokeBitmap, 0f, 0f, null)
        strokeBitmap.recycle()

        page.textData.forEach { textData ->
            val fontInfo = customFonts.find { it.name == textData.fontName }
            val tf = TypefaceManager.getTypeface(context, textData.fontName, fontInfo?.fileName)
            val textPaint = Paint().apply {
                color = textData.colorArgb
                textSize = textData.fontSize
                typeface = tf
                isAntiAlias = true
            }
            pdfCanvas.drawText(textData.text, textData.x, textData.y, textPaint)
        }
    }

    suspend fun exportToPdf(notes: List<NoteWithPages>, uri: Uri) {
        withContext(Dispatchers.IO) {
            val customFonts = NoteDatabase.getDatabase(context).noteDao().getAllCustomFonts().first()
            val document = PdfDocument()
            var pageIndex = 1
            notes.forEach { noteWP ->
                noteWP.pages.forEach { page ->
                    val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(1080, 1527, pageIndex++).create())
                    drawPageOnPdf(page, pdfPage.canvas, 1080, 1527, customFonts)
                    document.finishPage(pdfPage)
                }
            }
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            document.close()
        }
    }

    suspend fun exportSinglePageToPdf(page: PageEntity, uri: Uri) {
        withContext(Dispatchers.IO) {
            val customFonts = NoteDatabase.getDatabase(context).noteDao().getAllCustomFonts().first()
            val document = PdfDocument()
            val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(1080, 1527, 1).create())
            drawPageOnPdf(page, pdfPage.canvas, 1080, 1527, customFonts)
            document.finishPage(pdfPage)
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            document.close()
        }
    }

    suspend fun exportToSharedFile(notes: List<NoteWithPages>, fileName: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val customFonts = NoteDatabase.getDatabase(context).noteDao().getAllCustomFonts().first()
                val pdfDir = File(context.cacheDir, "pdfs")
                if (!pdfDir.exists()) pdfDir.mkdirs()
                val file = File(pdfDir, fileName)
                val document = PdfDocument()
                var pageIndex = 1

                notes.forEach { noteWP ->
                    noteWP.pages.forEach { page ->
                        val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(1080, 1527, pageIndex++).create())
                        drawPageOnPdf(page, pdfPage.canvas, 1080, 1527, customFonts)
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
            val customFonts = NoteDatabase.getDatabase(context).noteDao().getAllCustomFonts().first()
            val pdfDir = File(context.cacheDir, "pdfs")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            notes.mapNotNull { noteWP ->
                try {
                    val fileName = "${noteWP.note.title.replace(" ", "_")}_${noteWP.note.id}.pdf"
                    val file = File(pdfDir, fileName)
                    val document = PdfDocument()
                    var pageIndex = 1

                    noteWP.pages.forEach { page ->
                        val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(1080, 1527, pageIndex++).create())
                        drawPageOnPdf(page, pdfPage.canvas, 1080, 1527, customFonts)
                        document.finishPage(pdfPage)
                    }
                    FileOutputStream(file).use { document.writeTo(it) }
                    document.close()
                    file
                } catch(e: Exception) { null }
            }
        }
    }
}