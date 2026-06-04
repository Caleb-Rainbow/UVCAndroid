package com.herohan.uvcdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC5),
    onSecondary = Color.Black,
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun UVCAndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColorScheme, content = content)
}
