package com.midknight.pixelnotes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotesWithPages(): Flow<List<NoteWithPages>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note): Int

    @Delete
    suspend fun deleteNote(note: Note): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity): Long

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<com.midknight.pixelnotes.data.PageEntity>): List<Long>

    @Update
    suspend fun updatePage(page: PageEntity): Int

    @Delete
    suspend fun deletePage(page: PageEntity): Int

    @Query("DELETE FROM pages WHERE noteId = :noteId")
    suspend fun deletePagesByNoteId(noteId: Int): Int

    @Query("SELECT * FROM folders ORDER BY path ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("UPDATE folders SET path = :newPath || SUBSTR(path, LENGTH(:oldPath) + 1), name = CASE WHEN path = :oldPath THEN :newName ELSE name END WHERE path = :oldPath OR path LIKE :oldPath || '/%'")
    suspend fun renameFoldersCascade(oldPath: String, newPath: String, newName: String): Int

    @Query("UPDATE notes SET folder = :newPath || SUBSTR(folder, LENGTH(:oldPath) + 1) WHERE folder = :oldPath OR folder LIKE :oldPath || '/%'")
    suspend fun renameNotesFolderCascade(oldPath: String, newPath: String): Int

    @Query("DELETE FROM folders WHERE path = :path OR path LIKE :path || '/%'")
    suspend fun deleteFolderCascade(path: String): Int

    @Query("DELETE FROM notes WHERE folder = :path OR folder LIKE :path || '/%'")
    suspend fun deleteNotesInFolderCascade(path: String): Int

    @Delete
    suspend fun deleteFolder(folder: FolderEntity): Int

    @Query("SELECT * FROM custom_fonts ORDER BY name ASC")
    fun getAllCustomFonts(): kotlinx.coroutines.flow.Flow<List<CustomFont>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertCustomFont(font: CustomFont): Long

    @Delete
    suspend fun deleteCustomFont(font: CustomFont): Int
}