package com.midknight.pixelnotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.midknight.pixelnotes.domain.StrokeData

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val date: String,
    val drawingData: List<StrokeData> = emptyList(),
    val backgroundUri: String? = null,
    val folder: String = "General"
)