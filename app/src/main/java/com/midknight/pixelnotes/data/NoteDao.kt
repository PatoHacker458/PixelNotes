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
abstract class NoteDao {
    @Transaction
    @Query("SELECT * FROM notes ORDER BY id DESC")
    abstract fun getAllNotesWithPages(): Flow<List<NoteWithPages>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNote(note: Note): Long

    @Update
    abstract suspend fun updateNote(note: Note): Int

    @Delete
    abstract suspend fun deleteNote(note: Note): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPage(page: PageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPages(pages: List<PageEntity>): List<Long>

    @Update
    abstract suspend fun updatePage(page: PageEntity): Int

    @Delete
    abstract suspend fun deletePage(page: PageEntity): Int

    @Query("DELETE FROM pages WHERE noteId = :noteId")
    abstract suspend fun deletePagesByNoteId(noteId: Int): Int

    @Transaction
    open suspend fun updatePagesAtomic(noteId: Int, pages: List<PageEntity>) {
        deletePagesByNoteId(noteId)
        pages.chunked(100).forEach { insertPages(it) }
    }

    @Query("SELECT * FROM folders ORDER BY path ASC")
    abstract fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertFolder(folder: FolderEntity): Long

    @Query("UPDATE folders SET path = :newPath || SUBSTR(path, LENGTH(:oldPath) + 1), name = CASE WHEN path = :oldPath THEN :newName ELSE name END WHERE path = :oldPath OR path LIKE :oldPath || '/%'")
    abstract suspend fun renameFoldersCascade(oldPath: String, newPath: String, newName: String): Int

    @Query("UPDATE notes SET folder = :newPath || SUBSTR(folder, LENGTH(:oldPath) + 1) WHERE folder = :oldPath OR folder LIKE :oldPath || '/%'")
    abstract suspend fun renameNotesFolderCascade(oldPath: String, newPath: String): Int

    @Query("DELETE FROM folders WHERE path = :path OR path LIKE :path || '/%'")
    abstract suspend fun deleteFolderCascade(path: String): Int

    @Query("UPDATE notes SET inTrash = 1 WHERE folder = :path OR folder LIKE :path || '/%'")
    abstract suspend fun trashNotesInFolderCascade(path: String): Int

    @Transaction
    @Query("SELECT * FROM notes")
    abstract suspend fun getNotesWithPagesSync(): List<NoteWithPages>

    @Transaction
    @Query("SELECT * FROM notes WHERE inTrash = 1")
    abstract suspend fun getTrashedNotesSync(): List<NoteWithPages>

    @Delete
    abstract suspend fun deleteFolder(folder: FolderEntity): Int

    @Query("SELECT * FROM custom_fonts ORDER BY name ASC")
    abstract fun getAllCustomFonts(): Flow<List<CustomFont>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCustomFont(font: CustomFont): Long

    @Delete
    abstract suspend fun deleteCustomFont(font: CustomFont): Int

    @Query("SELECT MAX(updatedAt) FROM notes")
    abstract suspend fun getLastUpdatedTimestamp(): Long?
}
