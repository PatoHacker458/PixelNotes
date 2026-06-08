package com.midknight.pixelnotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, PageEntity::class, FolderEntity::class, CustomFont::class], version = 9, exportSchema = false)
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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Crear la nueva tabla de páginas
                database.execSQL("CREATE TABLE IF NOT EXISTS `pages` (`pageId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `noteId` INTEGER NOT NULL, `pageNumber` INTEGER NOT NULL, `drawingData` TEXT NOT NULL, `backgroundUri` TEXT, `paperStyle` INTEGER NOT NULL, `canvasColor` INTEGER NOT NULL)")

                // 2. Extraer los datos visuales antiguos e insertarlos como la Página 0 de cada Nota
                database.execSQL("INSERT INTO `pages` (`noteId`, `pageNumber`, `drawingData`, `backgroundUri`, `paperStyle`, `canvasColor`) SELECT `id`, 0, `drawingData`, `backgroundUri`, `paperStyle`, `canvasColor` FROM `notes`")

                // 3. Reconstruir la tabla notes eliminando las propiedades visuales
                database.execSQL("CREATE TABLE IF NOT EXISTS `notes_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `date` TEXT NOT NULL, `folder` TEXT NOT NULL)")
                database.execSQL("INSERT INTO `notes_new` (`id`, `title`, `content`, `date`, `folder`) SELECT `id`, `title`, `content`, `date`, `folder` FROM `notes`")
                database.execSQL("DROP TABLE `notes`")
                database.execSQL("ALTER TABLE `notes_new` RENAME TO `notes`")
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Añadir soporte para textos flotantes a las páginas existentes
                database.execSQL("ALTER TABLE `pages` ADD COLUMN `textData` TEXT NOT NULL DEFAULT '[]'")
                // Crear la tabla para el gestor de fuentes instaladas
                database.execSQL("CREATE TABLE IF NOT EXISTS `custom_fonts` (`name` TEXT NOT NULL, `fileName` TEXT NOT NULL, PRIMARY KEY(`name`))")
            }
        }

        fun getDatabase(context: android.content.Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "pixel_notes_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}