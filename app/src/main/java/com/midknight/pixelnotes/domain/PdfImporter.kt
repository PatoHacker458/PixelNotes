package com.midknight.pixelnotes.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfImporter(private val context: Context) {
    suspend fun importPdfToImages(uri: Uri, noteIdPrefix: String): List<String> = withContext(Dispatchers.IO) {
        val outputUris = mutableListOf<String>()
        try {
            val fileDescriptor: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor != null) {
                val pdfRenderer = PdfRenderer(fileDescriptor)
                val pdfDir = File(context.filesDir, "imported_pdfs")
                if (!pdfDir.exists()) pdfDir.mkdirs()

                for (i in 0 until pdfRenderer.pageCount) {
                    val page = pdfRenderer.openPage(i)

                    // Renderizamos al doble de la resolución original para evitar pixelación al hacer Zoom
                    val width = (page.width * 2)
                    val height = (page.height * 2)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE) // Fondo blanco

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val fileName = "pdf_${noteIdPrefix}_page_$i.jpg"
                    val file = File(pdfDir, fileName)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    outputUris.add(file.absolutePath)
                    page.close()
                    bitmap.recycle()
                }
                pdfRenderer.close()
                fileDescriptor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        outputUris
    }
}