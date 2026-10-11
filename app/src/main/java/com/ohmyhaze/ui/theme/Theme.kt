package com.ohmyhaze.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HazeColors = lightColorScheme(
    primary = Color(0xFF2C655B),
    onPrimary = Color.White,
    secondary = Color(0xFF52675F),
    background = Color(0xFFF7F8F5),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E),
)

@Composable
fun OhMyHazeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HazeColors, content = content)
}
