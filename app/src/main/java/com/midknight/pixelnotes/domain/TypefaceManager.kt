package com.midknight.pixelnotes.domain

import android.content.Context
import android.graphics.Typeface
import java.io.File

object TypefaceManager {
    private val cache = mutableMapOf<String, Typeface>()

    fun getTypeface(context: Context, fontName: String, fileName: String?): Typeface {
        if (fontName == "Default" || fontName.isEmpty()) return Typeface.DEFAULT
        if (fontName == "Serif") return Typeface.SERIF
        if (fontName == "Monospace") return Typeface.MONOSPACE
        if (fontName == "Cursive") return Typeface.create("cursive", Typeface.NORMAL)

        if (cache.containsKey(fontName)) return cache[fontName]!!

        return try {
            if (fileName != null) {
                val file = File(context.filesDir, "custom_fonts/$fileName")
                if (file.exists()) {
                    val tf = Typeface.createFromFile(file)
                    cache[fontName] = tf
                    return tf
                }
            }
            Typeface.DEFAULT
        } catch (e: Exception) {
            e.printStackTrace()
            Typeface.DEFAULT
        }
    }
}