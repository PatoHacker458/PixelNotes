package com.midknight.pixelnotes.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.midknight.pixelnotes.domain.StrokeData

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val date: String,
    val folder: String = "General"
)

@Entity(tableName = "pages")
data class PageEntity(
    @PrimaryKey(autoGenerate = true)
    val pageId: Int = 0,
    val noteId: Int,
    val pageNumber: Int,
    val drawingData: List<StrokeData> = emptyList(),
    val backgroundUri: String? = null,
    val paperStyle: Int = 0,
    val canvasColor: Int = -1
)

data class NoteWithPages(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val pages: List<PageEntity>
)