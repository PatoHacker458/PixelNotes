package com.midknight.pixelnotes.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.midknight.pixelnotes.domain.StrokeData

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStrokeList(strokes: List<StrokeData>?): String {
        if (strokes == null) return "[]"
        return gson.toJson(strokes)
    }

    @TypeConverter
    fun toStrokeList(strokesString: String?): List<StrokeData> {
        if (strokesString.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<StrokeData>>() {}.type
        return gson.fromJson(strokesString, listType)
    }
}