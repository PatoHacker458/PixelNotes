package com.midknight.pixelnotes.domain

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.midknight.pixelnotes.data.NoteWithPages
import com.midknight.pixelnotes.data.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SingleNotePackage(private val context: Context) {
    private val TAG = "SingleNotePackage"
    private val gson = Gson()

    suspend fun exportNote(noteWP: NoteWithPages): File? = withContext(Dispatchers.IO) {
        try {
            val tempZip = File(context.cacheDir, "${noteWP.note.title}.pxnote")
            if (tempZip.exists()) tempZip.delete()

            ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
                // 1. Add Note JSON
                val json = gson.toJson(noteWP)
                zos.putNextEntry(ZipEntry("note_data.json"))
                zos.write(json.toByteArray())
                zos.closeEntry()

                // 2. Identify and add media files
                val mediaFiles = mutableSetOf<String>()
                noteWP.pages.forEach { page ->
                    // PDFs
                    page.backgroundUri?.let { uri ->
                        if (uri.contains("imported_pdfs/")) {
                            val path = uri.split("?pdfPage=")[0]
                            mediaFiles.add(path)
                        }
                    }
                    // Images
                    page.imageData.forEach { img ->
                        val path = if (img.uri.startsWith("internal://")) {
                            File(context.filesDir, "inserted_images/${img.uri.removePrefix("internal://")}").absolutePath
                        } else img.uri
                        if (path.contains("inserted_images/") || path.contains("files/")) {
                            mediaFiles.add(path)
                        }
                    }
                    // Audio
                    page.audioData.forEach { audio ->
                        mediaFiles.add(audio.uri)
                    }
                }

                mediaFiles.forEach { filePath ->
                    val file = File(filePath)
                    if (file.exists()) {
                        zos.putNextEntry(ZipEntry("media/${file.name}"))
                        FileInputStream(file).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            return@withContext tempZip
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            return@withContext null
        }
    }

    suspend fun importNote(zipFile: File): NoteWithPages? = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            var noteDataJson: String? = null
            
            // 1. Unzip to temp
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = File(tempDir, entry.name)
                    destFile.parentFile?.mkdirs()
                    
                    if (entry.name == "note_data.json") {
                        noteDataJson = zis.bufferedReader().readText()
                    } else if (entry.name.startsWith("media/")) {
                        FileOutputStream(destFile).use { zis.copyTo(it) }
                    }
                    
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (noteDataJson == null) return@withContext null

            // 2. Deserialize and reset IDs
            val importedWP = gson.fromJson(noteDataJson, NoteWithPages::class.java)
            val timestamp = System.currentTimeMillis()
            
            // Generate a fresh note with a new title if needed (handled in VM, but let's keep it unique)
            val baseNote = importedWP.note.copy(
                id = 0, // Reset ID for insertion
                updatedAt = timestamp
            )

            val updatedPages = importedWP.pages.map { page ->
                // Map media files to their new internal locations
                val newImages = page.imageData.map { img ->
                    val fileName = if (img.uri.startsWith("internal://")) img.uri.removePrefix("internal://") else File(img.uri).name
                    val srcMedia = File(tempDir, "media/$fileName")
                    if (srcMedia.exists()) {
                        val destMedia = File(File(context.filesDir, "inserted_images"), fileName)
                        destMedia.parentFile?.mkdirs()
                        srcMedia.copyTo(destMedia, overwrite = true)
                        img.copy(uri = "internal://$fileName")
                    } else img
                }

                val newAudio = page.audioData.map { audio ->
                    val fileName = File(audio.uri).name
                    val srcMedia = File(tempDir, "media/$fileName")
                    if (srcMedia.exists()) {
                        val destMedia = File(File(context.filesDir, "audio_notes"), fileName)
                        destMedia.parentFile?.mkdirs()
                        srcMedia.copyTo(destMedia, overwrite = true)
                        audio.copy(uri = destMedia.absolutePath)
                    } else audio
                }

                var newBg = page.backgroundUri
                page.backgroundUri?.let { uri ->
                    if (uri.contains("imported_pdfs/")) {
                        val fileName = File(uri.split("?pdfPage=")[0]).name
                        val suffix = if (uri.contains("?pdfPage=")) "?pdfPage=" + uri.split("?pdfPage=")[1] else ""
                        val srcMedia = File(tempDir, "media/$fileName")
                        if (srcMedia.exists()) {
                            val destMedia = File(File(context.filesDir, "imported_pdfs"), fileName)
                            destMedia.parentFile?.mkdirs()
                            srcMedia.copyTo(destMedia, overwrite = true)
                            newBg = destMedia.absolutePath + suffix
                        }
                    }
                }

                page.copy(
                    pageId = 0,
                    noteId = 0,
                    imageData = newImages,
                    audioData = newAudio,
                    backgroundUri = newBg
                )
            }

            tempDir.deleteRecursively()
            return@withContext NoteWithPages(baseNote, updatedPages)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            return@withContext null
        }
    }
}
