package com.midknight.pixelnotes.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import ir.mahozad.multiplatform.wavyslider.WaveDirection

@Composable
fun AudioPlayerSlider(
    progress: Float,
    isPlaying: Boolean,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    WavySlider(
        value = progress,
        onValueChange = onProgressChange,
        modifier = modifier,
        waveHeight = if (isPlaying) 7.dp else 0.dp,
        waveVelocity = (if (isPlaying) 15.dp else 0.dp) to WaveDirection.HEAD,
        colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary),
        trackThickness = 4.dp,
        waveLength = 25.dp
    )
}
