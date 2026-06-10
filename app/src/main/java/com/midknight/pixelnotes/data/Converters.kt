package com.midknight.pixelnotes.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.midknight.pixelnotes.domain.AudioData
import com.midknight.pixelnotes.domain.ImageData
import com.midknight.pixelnotes.domain.StrokeData
import com.midknight.pixelnotes.domain.TextData

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStrokeList(value: List<StrokeData>): String = gson.toJson(value)

    @TypeConverter
    fun toStrokeList(value: String): List<StrokeData> {
        val type = object : TypeToken<List<StrokeData>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromTextList(value: List<TextData>): String = gson.toJson(value)

    @TypeConverter
    fun toTextList(value: String): List<TextData> {
        val type = object : TypeToken<List<TextData>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromImageList(value: List<ImageData>): String = gson.toJson(value)

    @TypeConverter
    fun toImageList(value: String): List<ImageData> {
        val type = object : TypeToken<List<ImageData>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromAudioList(value: List<AudioData>): String = gson.toJson(value)

    @TypeConverter
    fun toAudioList(value: String): List<AudioData> {
        val type = object : TypeToken<List<AudioData>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}