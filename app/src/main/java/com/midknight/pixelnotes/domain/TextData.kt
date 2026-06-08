package com.midknight.pixelnotes.domain

data class TextData(
    val id: String = java.util.UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var text: String,
    var colorArgb: Int,
    var fontSize: Float,
    var fontName: String
)