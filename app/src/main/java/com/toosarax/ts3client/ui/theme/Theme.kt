package com.toosarax.ts3client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ts3Colors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    secondary = Color(0xFF81D4FA),
    background = Color(0xFF101820),
    surface = Color(0xFF1B2A41),
    onPrimary = Color(0xFF001018),
    onBackground = Color(0xFFE8F1FF),
    onSurface = Color(0xFFE8F1FF),
)

@Composable
fun Ts3Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Ts3Colors,
        content = content,
    )
}
