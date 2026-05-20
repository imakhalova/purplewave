package com.purplewave.auction.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PurpleWavePrimary = Color(0xFF4A3AB8)
private val PurpleWaveSecondary = Color(0xFF6C63FF)

private val LightColors = lightColorScheme(
    primary = PurpleWavePrimary,
    secondary = PurpleWaveSecondary,
)

@Composable
fun PurpleWaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
