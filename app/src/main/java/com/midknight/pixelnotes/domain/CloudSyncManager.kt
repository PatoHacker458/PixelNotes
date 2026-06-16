package com.midknight.pixelnotes.domain

import android.content.Context
import android.util.Log
import com.midknight.pixelnotes.data.NoteDatabase
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

    suspend fun backupToDrive(accountName: String, force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (accountName.isBlank()) {
                return@withContext Result.failure(Exception("Account email is empty. Please sign out and sign in again."))
            }

            Log.d(TAG, "Starting backup for: $accountName (force=$force)")

            // Use a more explicit credential setup
            val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
            credential.selectedAccount = android.accounts.Account(accountName, "com.google")

            val drive = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Pixel Notes")
                .build()

            // IMPORTANT: Create a perfectly consistent snapshot of the DB
            val snapshotFile = File(context.cacheDir, "db_snapshot.db")
            NoteDatabase.createBackupSnapshot(context, snapshotFile)
            
            val localTimestamp = NoteDatabase.getDatabase(context).noteDao().getLastUpdatedTimestamp() ?: 0L
            Log.d(TAG, "Database snapshot created for backup. TS: $localTimestamp")

            val zipFile = createBackupZip(snapshotFile)
            Log.d(TAG, "Backup ZIP created: ${zipFile.absolutePath} (${zipFile.length()} bytes)")

            // Search for existing backup
            val query = "name = '$backupFileName' and 'appDataFolder' in parents and trashed = false"
            val filesList = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ(query)
                .setFields("files(id, name, description)")
                .execute()
            val files = filesList.files

            val content = FileContent("application/zip", zipFile)
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                description = localTimestamp.toString()
            }

            if (files.isNullOrEmpty()) {
                Log.d(TAG, "No existing backup found. Creating new one.")
                fileMetadata.setName(backupFileName)
                fileMetadata.setParents(listOf("appDataFolder"))
                val created = drive.files().create(fileMetadata, content).setFields("id, description").execute()
                Log.d(TAG, "Created initial backup. ID: ${created.id}, TS: ${created.description}")
            } else {
                Log.d(TAG, "Updating cloud backup... (Local TS: $localTimestamp)")
                val updated = drive.files().update(files[0].id, fileMetadata, content).setFields("id, description").execute()
                Log.d(TAG, "Backup updated. ID: ${updated.id}, TS: ${updated.description}")
            }

            zipFile.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed for $accountName", e)
            Result.failure(e)
        }
    }

    suspend fun purgeAllCloudBackups(accountName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (accountName.isBlank()) return@withContext Result.failure(Exception("Account email is empty"))

            val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
            credential.selectedAccount = android.accounts.Account(accountName, "com.google")

            val drive = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Pixel Notes")
                .build()

            // List all files in appDataFolder
            val filesList = drive.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute()
            val files = filesList.files

            if (!files.isNullOrEmpty()) {
                Log.d(TAG, "Purging ${files.size} files from cloud...")
                files.forEach { file ->
                    drive.files().delete(file.id).execute()
                    Log.d(TAG, "Deleted: ${file.name} (${file.id})")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Purge failed for $accountName", e)
            Result.failure(e)
        }
    }

    suspend fun downloadBackupToTemp(accountName: String): File? = withContext(Dispatchers.IO) {
        try {
            if (accountName.isBlank()) return@withContext null

            val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
            credential.selectedAccount = android.accounts.Account(accountName, "com.google")

            val drive = Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Pixel Notes")
                .build()

            val query = "name = '$backupFileName' and 'appDataFolder' in parents and trashed = false"
            val filesList = drive.files().list().setSpaces("appDataFolder").setQ(query).setFields("files(id, name)").execute()
            val files = filesList.files

            if (files.isNullOrEmpty()) return@withContext null

            val tempZip = File(context.cacheDir, "merge_temp.zip")
            FileOutputStream(tempZip).use { output ->
                drive.files().get(files[0].id).executeMediaAndDownloadTo(output)
            }
            return@withContext tempZip
        } catch (e: Exception) {
            Log.e(TAG, "Download for merge failed", e)
            return@withContext null
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
            val filesList = drive.files().list().setSpaces("appDataFolder").setQ(query).setFields("files(id, name, description)").execute()
            val files = filesList.files

            if (files.isNullOrEmpty()) {
                Log.w(TAG, "Restore failed: No backup found on cloud for $accountName")
                return@withContext Result.failure(Exception("No backup found on Google Drive"))
            }

            Log.d(TAG, "Backup found: ${files[0].id}. Downloading and overwriting local data...")
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

            listOf("audio_notes", "custom_fonts", "imported_pdfs", "inserted_images").forEach { dir ->
                File(context.filesDir, dir).deleteRecursively()
            }
            Log.d(TAG, "Local data cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear local data", e)
        }
    }

    internal fun createBackupZip(dbSnapshot: File? = null): File {
        val zipFile = File(context.cacheDir, "backup_upload.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.setLevel(java.util.zip.Deflater.BEST_COMPRESSION)
            
            if (dbSnapshot != null && dbSnapshot.exists()) {
                addToZip(zos, dbSnapshot, "database/pixel_notes_database")
            } else {
                val dbFile = context.getDatabasePath("pixel_notes_database")
                if (dbFile.exists()) addToZip(zos, dbFile, "database/pixel_notes_database")
            }

            // EXCLUDE -shm and -wal files. Since we checkpointed (and optionally snapshotted),
            // the main .db file is guaranteed to be consistent.

            val dirs = listOf("audio_notes", "custom_fonts", "imported_pdfs", "inserted_images")
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

    internal fun addToZip(zos: ZipOutputStream, file: File, path: String) {
        try {
            zos.putNextEntry(ZipEntry(path))
            FileInputStream(file).use { it.copyTo(zos) }
            zos.closeEntry()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add $path to ZIP", e)
        }
    }

    internal fun extractBackupZip(zipFile: File) {
        Log.d(TAG, "Starting extraction of ZIP: ${zipFile.absolutePath}")
        
        clearLocalData()

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
