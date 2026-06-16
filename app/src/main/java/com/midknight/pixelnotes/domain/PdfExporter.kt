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

    private fun getPageBounds(page: PageEntity, customFonts: List<CustomFont>): Rect {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE

        page.drawingData.forEach { stroke -> val b = stroke.getBounds(); if (b.left < minX) minX = b.left; if (b.top < minY) minY = b.top; if (b.right > maxX) maxX = b.right; if (b.bottom > maxY) maxY = b.bottom }
        val paint = android.graphics.Paint()
        page.textData.forEach { textData -> val fontInfo = customFonts.find { it.name == textData.fontName }; val tf = TypefaceManager.getTypeface(context, textData.fontName, fontInfo?.fileName); paint.typeface = tf; paint.textSize = textData.fontSize; val width = paint.measureText(textData.text); if (textData.x < minX) minX = textData.x; if (textData.y - textData.fontSize < minY) minY = textData.y - textData.fontSize; if (textData.x + width > maxX) maxX = textData.x + width; if (textData.y > maxY) maxY = textData.y }
        page.imageData.forEach { img -> if (img.x < minX) minX = img.x; if (img.y < minY) minY = img.y; if (img.x + img.width > maxX) maxX = img.x + img.width; if (img.y + img.height > maxY) maxY = img.y + img.height }

        return if (minX == Float.MAX_VALUE) Rect(0, 0, 1920, 1080) else Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
    }

    private fun drawPageOnPdf(page: PageEntity, pdfCanvas: Canvas, pdfWidth: Int, pdfHeight: Int, customFonts: List<CustomFont>, isInfinite: Boolean) {
        val bounds = if (isInfinite) getPageBounds(page, customFonts) else Rect(0, 0, 1080, 1527)
        val pad = if (isInfinite) 400 else 0
        val offsetX = if (isInfinite) -bounds.left + pad else 0
        val offsetY = if (isInfinite) -bounds.top + pad else 0

        if (isInfinite) {
            pdfCanvas.drawColor(if (page.canvasColor == -1) android.graphics.Color.parseColor("#F5F5F5") else page.canvasColor)
            val linePaint = Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 2f; style = Paint.Style.STROKE; alpha = 76 }
            val spacing = 50f
            var x = (offsetX % spacing).toFloat()
            while (x < pdfWidth) { pdfCanvas.drawLine(x, 0f, x, pdfHeight.toFloat(), linePaint); x += spacing }
            var y = (offsetY % spacing).toFloat()
            while (y < pdfHeight) { pdfCanvas.drawLine(0f, y, pdfWidth.toFloat(), y, linePaint); y += spacing }
        } else {
            val bgPaint = Paint().apply { color = if (page.canvasColor == -1) android.graphics.Color.WHITE else page.canvasColor; style = Paint.Style.FILL }
            pdfCanvas.drawRect(0f, 0f, pdfWidth.toFloat(), pdfHeight.toFloat(), bgPaint)

            if (page.paperStyle > 0) {
                val linePaint = Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 2f; style = Paint.Style.STROKE; alpha = 128 }
                val spacing = 90f
                when (page.paperStyle) {
                    1 -> { var y = spacing; while(y < pdfHeight) { pdfCanvas.drawLine(0f, y, pdfWidth.toFloat(), y, linePaint); y += spacing } }
                    2 -> {
                        var y = spacing; while(y < pdfHeight) { pdfCanvas.drawLine(0f, y, pdfWidth.toFloat(), y, linePaint); y += spacing }
                        var x = spacing; while(x < pdfWidth) { pdfCanvas.drawLine(x, 0f, x, pdfHeight.toFloat(), linePaint); x += spacing }
                    }
                    3 -> {
                        linePaint.style = Paint.Style.FILL
                        var y = spacing; while(y < pdfHeight) {
                            var x = spacing; while(x < pdfWidth) { pdfCanvas.drawCircle(x, y, 3f, linePaint); x += spacing }
                            y += spacing
                        }
                    }
                }
            }
        }

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
                            val destRect = Rect(offsetX, offsetY, pdfWidth + offsetX, pdfHeight + offsetY)
                            pdfCanvas.drawBitmap(bmp, null, destRect, null)
                            pdfPage.close()
                            bmp.recycle()
                        }
                        renderer.close()
                        fd.close()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            } else {
                try {
                    val bgUri = Uri.parse(page.backgroundUri)
                    context.contentResolver.openInputStream(bgUri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            val targetRatio = pdfWidth.toFloat() / pdfHeight.toFloat()
                            val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                            val srcRect = if (bitmapRatio > targetRatio) { val newWidth = (bitmap.height * targetRatio).toInt(); val xOffset = (bitmap.width - newWidth) / 2; Rect(xOffset, 0, xOffset + newWidth, bitmap.height) } else { val newHeight = (bitmap.width / targetRatio).toInt(); val yOffset = (bitmap.height - newHeight) / 2; Rect(0, yOffset, bitmap.width, yOffset + newHeight) }
                            val destRect = Rect(offsetX, offsetY, pdfWidth + offsetX, pdfHeight + offsetY)
                            pdfCanvas.drawBitmap(bitmap, srcRect, destRect, null)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        page.imageData.forEach { img ->
            try {
                val resolvedUri = if (img.uri.startsWith("internal://")) {
                    val fileName = img.uri.removePrefix("internal://")
                    Uri.fromFile(java.io.File(context.filesDir, "inserted_images/$fileName"))
                } else if (img.uri.contains("inserted_images/")) {
                    val fileName = img.uri.substringAfterLast("/")
                    Uri.fromFile(java.io.File(context.filesDir, "inserted_images/$fileName"))
                } else {
                    Uri.parse(img.uri)
                }

                context.contentResolver.openInputStream(resolvedUri)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply { inMutable = true }
                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    if (bitmap != null) {
                        val destRect = Rect(img.x.toInt() + offsetX, img.y.toInt() + offsetY, (img.x + img.width).toInt() + offsetX, (img.y + img.height).toInt() + offsetY)
                        val imgPaint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }
                        pdfCanvas.drawBitmap(bitmap, null, destRect, imgPaint)
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        val strokeBitmap = Bitmap.createBitmap(pdfWidth, pdfHeight, Bitmap.Config.ARGB_8888)
        val strokeCanvas = Canvas(strokeBitmap)
        strokeCanvas.translate(offsetX.toFloat(), offsetY.toFloat())

        val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
        val clearXfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        page.drawingData.forEach { strokeData ->
            if (strokeData.isEraser) { paint.xfermode = clearXfermode; paint.color = android.graphics.Color.TRANSPARENT } else { paint.xfermode = null; paint.color = strokeData.colorArgb }
            val isShape = strokeData.points.size <= 10 || strokeData.points.size == 37
            if (isShape || strokeData.isHighlighter) {
                paint.strokeWidth = strokeData.strokeWidth
                val path = android.graphics.Path()
                if (strokeData.points.isNotEmpty()) {
                    path.moveTo(strokeData.points.first().x, strokeData.points.first().y)
                    if (isShape) { for (i in 1 until strokeData.points.size) { path.lineTo(strokeData.points[i].x, strokeData.points[i].y) } } else { var prevX = strokeData.points.first().x; var prevY = strokeData.points.first().y; for (i in 1 until strokeData.points.size) { val currentX = strokeData.points[i].x; val currentY = strokeData.points[i].y; val midX = (prevX + currentX) / 2f; val midY = (prevY + currentY) / 2f; path.quadTo(prevX, prevY, midX, midY); prevX = currentX; prevY = currentY }; path.lineTo(prevX, prevY) }
                }
                strokeCanvas.drawPath(path, paint)
            } else {
                if (strokeData.points.isNotEmpty()) {
                    var prev = strokeData.points.first()
                    for (i in 1 until strokeData.points.size) { val curr = strokeData.points[i]; val p1 = if (prev.p <= 0f) 1f else prev.p; val p2 = if (curr.p <= 0f) 1f else curr.p; paint.strokeWidth = strokeData.strokeWidth * ((p1 + p2) / 2f); strokeCanvas.drawLine(prev.x, prev.y, curr.x, curr.y, paint); prev = curr }
                }
            }
        }

        pdfCanvas.drawBitmap(strokeBitmap, 0f, 0f, null)
        strokeBitmap.recycle()

        page.textData.forEach { textData -> val fontInfo = customFonts.find { it.name == textData.fontName }; val tf = TypefaceManager.getTypeface(context, textData.fontName, fontInfo?.fileName); val textPaint = Paint().apply { color = textData.colorArgb; textSize = textData.fontSize; typeface = tf; isAntiAlias = true }; pdfCanvas.drawText(textData.text, textData.x + offsetX, textData.y + offsetY, textPaint) }
    }

    suspend fun exportToPdf(notes: List<NoteWithPages>, uri: Uri) {
        withContext(Dispatchers.IO) {
            val customFonts = NoteDatabase.getDatabase(context).noteDao().getAllCustomFonts().first()
            val document = PdfDocument()
            var pageIndex = 1
            notes.forEach { noteWP ->
                val isInfinite = noteWP.note.isInfinite
                noteWP.pages.forEach { page ->
                    val bounds = if (isInfinite) getPageBounds(page, customFonts) else Rect(0, 0, 1080, 1527)
                    val pad = if (isInfinite) 400 else 0
                    val pdfWidth = if (isInfinite) (bounds.width() + pad * 2).coerceAtLeast(100) else 1080
                    val pdfHeight = if (isInfinite) (bounds.height() + pad * 2).coerceAtLeast(100) else 1527

                    val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, pageIndex++).create())
                    drawPageOnPdf(page, pdfPage.canvas, pdfWidth, pdfHeight, customFonts, isInfinite)
                    document.finishPage(pdfPage)
                }
            }
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
            document.close()
        }
    }

    suspend fun exportSinglePageToPdf(page: PageEntity, uri: Uri, isInfinite: Boolean) {
        withContext(Dispatchers.IO) {
            val customFonts = NoteDatabase.getDatabase(context).noteDao().getAllCustomFonts().first()
            val document = PdfDocument()

            val bounds = if (isInfinite) getPageBounds(page, customFonts) else Rect(0, 0, 1080, 1527)
            val pad = if (isInfinite) 400 else 0
            val pdfWidth = if (isInfinite) (bounds.width() + pad * 2).coerceAtLeast(100) else 1080
            val pdfHeight = if (isInfinite) (bounds.height() + pad * 2).coerceAtLeast(100) else 1527

            val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create())
            drawPageOnPdf(page, pdfPage.canvas, pdfWidth, pdfHeight, customFonts, isInfinite)
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
                    val isInfinite = noteWP.note.isInfinite
                    noteWP.pages.forEach { page ->
                        val bounds = if (isInfinite) getPageBounds(page, customFonts) else Rect(0, 0, 1080, 1527)
                        val pad = if (isInfinite) 400 else 0
                        val pdfWidth = if (isInfinite) (bounds.width() + pad * 2).coerceAtLeast(100) else 1080
                        val pdfHeight = if (isInfinite) (bounds.height() + pad * 2).coerceAtLeast(100) else 1527

                        val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, pageIndex++).create())
                        drawPageOnPdf(page, pdfPage.canvas, pdfWidth, pdfHeight, customFonts, isInfinite)
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

                    val isInfinite = noteWP.note.isInfinite
                    noteWP.pages.forEach { page ->
                        val bounds = if (isInfinite) getPageBounds(page, customFonts) else Rect(0, 0, 1080, 1527)
                        val pad = if (isInfinite) 400 else 0
                        val pdfWidth = if (isInfinite) (bounds.width() + pad * 2).coerceAtLeast(100) else 1080
                        val pdfHeight = if (isInfinite) (bounds.height() + pad * 2).coerceAtLeast(100) else 1527

                        val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, pageIndex++).create())
                        drawPageOnPdf(page, pdfPage.canvas, pdfWidth, pdfHeight, customFonts, isInfinite)
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