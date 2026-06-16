package com.midknight.pixelnotes.domain

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfImporter(private val context: Context) {
    suspend fun importPdfRealTime(uri: Uri): Pair<String, Int>? = withContext(Dispatchers.IO) {
        try {
            val pdfDir = File(context.filesDir, "imported_pdfs")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            val fileName = "doc_${System.currentTimeMillis()}.pdf"
            val destFile = File(pdfDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val fd = ParcelFileDescriptor.open(destFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val pageCount = renderer.pageCount
            renderer.close()
            fd.close()

            return@withContext Pair(destFile.absolutePath, pageCount)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}