package com.midknight.pixelnotes.domain

data class AudioData(
    val id: String,
    val uri: String,
    val x: Float,
    val y: Float,
    val durationMs: Long
)