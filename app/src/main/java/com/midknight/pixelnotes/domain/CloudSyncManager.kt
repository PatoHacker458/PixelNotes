package com.midknight.pixelnotes.domain

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class CloudSyncManager(private val context: Context) {

    private val TAG = "CloudSyncManager"
    private val backupFileName = "PixelNotes_Backup.zip"

    suspend fun backupToDrive(accountName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (accountName.isBlank()) {
                return@withContext Result.failure(Exception("Account email is empty. Please sign out and sign in again."))
            }

            Log.d(TAG, "Starting backup for: $accountName")

            // Use a more explicit credential setup
            val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
            credential.selectedAccount = android.accounts.Account(accountName, "com.google")

            val drive = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Pixel Notes")
                .build()

            val zipFile = createBackupZip()

            // Search for existing backup
            val query = "name = '$backupFileName' and 'appDataFolder' in parents and trashed = false"
            val filesList = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ(query)
                .execute()
            val files = filesList.files

            val content = FileContent("application/zip", zipFile)

            if (files.isNullOrEmpty()) {
                val metadata = com.google.api.services.drive.model.File()
                metadata.setName(backupFileName)
                metadata.setParents(listOf("appDataFolder"))
                drive.files().create(metadata, content).execute()
            } else {
                val updateMetadata = com.google.api.services.drive.model.File()
                drive.files().update(files[0].id, updateMetadata, content).execute()
            }

            zipFile.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed for $accountName", e)
            Result.failure(e)
        }
    }

    suspend fun restoreFromDrive(accountName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (accountName.isBlank()) return@withContext Result.failure(Exception("Account email is empty"))

            val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
            credential.selectedAccount = android.accounts.Account(accountName, "com.google")

            val drive = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Pixel Notes")
                .build()

            val query = "name = '$backupFileName' and 'appDataFolder' in parents and trashed = false"
            val filesList = drive.files().list().setSpaces("appDataFolder").setQ(query).execute()
            val files = filesList.files

            if (files.isNullOrEmpty()) {
                Log.w(TAG, "Restore failed: No backup found on cloud for $accountName")
                return@withContext Result.failure(Exception("No backup found on Google Drive"))
            }

            Log.d(TAG, "Backup found: ${files[0].id}. Downloading...")
            val tempZip = File(context.cacheDir, "restore_temp.zip")
            FileOutputStream(tempZip).use { output ->
                drive.files().get(files[0].id).executeMediaAndDownloadTo(output)
            }

            Log.d(TAG, "Download complete. ZIP size: ${tempZip.length()} bytes. Extracting...")
            extractBackupZip(tempZip)
            tempZip.delete()
            
            Log.d(TAG, "Restore successful for $accountName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed for $accountName", e)
            Result.failure(e)
        }
    }

    fun clearLocalData() {
        try {
            com.midknight.pixelnotes.data.NoteDatabase.closeDatabase()
            val dbFile = context.getDatabasePath("pixel_notes_database")
            if (dbFile.exists()) dbFile.delete()
            File(dbFile.path + "-shm").delete()
            File(dbFile.path + "-wal").delete()

            listOf("audio_notes", "custom_fonts", "imported_pdfs").forEach { dir ->
                File(context.filesDir, dir).deleteRecursively()
            }
            Log.d(TAG, "Local data cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear local data", e)
        }
    }

    private fun createBackupZip(): File {
        val zipFile = File(context.cacheDir, "backup_upload.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val dbFile = context.getDatabasePath("pixel_notes_database")
            if (dbFile.exists()) addToZip(zos, dbFile, "database/pixel_notes_database")

            val shmFile = File(dbFile.path + "-shm")
            if (shmFile.exists()) addToZip(zos, shmFile, "database/pixel_notes_database-shm")
            val walFile = File(dbFile.path + "-wal")
            if (walFile.exists()) addToZip(zos, walFile, "database/pixel_notes_database-wal")

            val dirs = listOf("audio_notes", "custom_fonts", "imported_pdfs")
            dirs.forEach { dirName ->
                val dir = File(context.filesDir, dirName)
                if (dir.exists()) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile) addToZip(zos, file, "files/$dirName/${file.name}")
                    }
                }
            }
        }
        return zipFile
    }

    private fun addToZip(zos: ZipOutputStream, file: File, path: String) {
        try {
            zos.putNextEntry(ZipEntry(path))
            FileInputStream(file).use { it.copyTo(zos) }
            zos.closeEntry()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add $path to ZIP", e)
        }
    }

    private fun extractBackupZip(zipFile: File) {
        Log.d(TAG, "Starting extraction of ZIP: ${zipFile.absolutePath}")
        
        // 1. CLEAR CURRENT DATABASE FILES FIRST
        // This is critical to prevent SQLite from recovering from old WAL/SHM files
        val dbFile = context.getDatabasePath("pixel_notes_database")
        if (dbFile.exists()) dbFile.delete()
        File(dbFile.path + "-shm").delete()
        File(dbFile.path + "-wal").delete()

        // 2. CLEAR CURRENT MEDIA FOLDERS
        listOf("audio_notes", "custom_fonts", "imported_pdfs").forEach { 
            val dir = File(context.filesDir, it)
            if (dir.exists()) {
                val deleted = dir.deleteRecursively()
                Log.d(TAG, "Deleting local dir $it: $deleted")
            }
        }

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                Log.d(TAG, "Extracting entry: ${entry.name}")
                val destFile = if (entry.name.startsWith("database/")) {
                    val dbName = entry.name.removePrefix("database/")
                    File(context.getDatabasePath("pixel_notes_database").parentFile, dbName)
                } else if (entry.name.startsWith("files/")) {
                    File(context.filesDir, entry.name.removePrefix("files/"))
                } else {
                    null
                }

                destFile?.let {
                    it.parentFile?.mkdirs()
                    FileOutputStream(it).use { out -> zis.copyTo(out) }
                    Log.d(TAG, "Written to: ${it.absolutePath} (${it.length()} bytes)")
                }
                
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        Log.d(TAG, "Extraction finished")
    }
}