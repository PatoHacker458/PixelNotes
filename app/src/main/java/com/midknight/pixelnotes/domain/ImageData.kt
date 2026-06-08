package com.midknight.pixelnotes.domain

data class ImageData(
    val id: String = java.util.UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var rotation: Float = 0f,
    val uri: String
)