package com.ohmyhaze.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

private val LightHazeColors = lightColorScheme(
    primary = Color(0xFF2C655B),
    onPrimary = Color.White,
    secondary = Color(0xFF52675F),
    background = Color(0xFFF7F8F5),
    onBackground = Color(0xFF17201D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17201D),
    surfaceVariant = Color(0xFFEDF2EE),
    onSurfaceVariant = Color(0xFF44524D),
    error = Color(0xFFB3261E),
)

private val DarkHazeColors = darkColorScheme(
    primary = Color(0xFF8BD4C3),
    onPrimary = Color(0xFF00382F),
    secondary = Color(0xFFB6CCC4),
    onSecondary = Color(0xFF203831),
    background = Color(0xFF101917),
    onBackground = Color(0xFFE4EEE9),
    surface = Color(0xFF17211F),
    onSurface = Color(0xFFE4EEE9),
    surfaceVariant = Color(0xFF263330),
    onSurfaceVariant = Color(0xFFB5C4BE),
    error = Color(0xFFF2B8B5),
)

@Composable
fun OhMyHazeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkHazeColors else LightHazeColors,
        content = content,
    )
}
