package com.midknight.pixelnotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, FolderEntity::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `folders` (`path` TEXT NOT NULL, `name` TEXT NOT NULL, `parentPath` TEXT, PRIMARY KEY(`path`))")
                database.execSQL("INSERT OR IGNORE INTO `folders` (`path`, `name`, `parentPath`) VALUES ('General', 'General', NULL)")
                database.execSQL("INSERT OR IGNORE INTO `folders` (`path`, `name`, `parentPath`) VALUES ('Trabajo', 'Trabajo', NULL)")
                database.execSQL("INSERT OR IGNORE INTO `folders` (`path`, `name`, `parentPath`) VALUES ('Escuela', 'Escuela', NULL)")
                database.execSQL("INSERT OR IGNORE INTO `folders` (`path`, `name`, `parentPath`) VALUES ('Personal', 'Personal', NULL)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `notes` ADD COLUMN `paperStyle` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `notes` ADD COLUMN `canvasColor` INTEGER NOT NULL DEFAULT -1")
            }
        }

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "pixel_notes_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}