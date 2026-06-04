package com.herohan.uvcapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple500 = Color(0xFF6200EE)
private val Purple700 = Color(0xFF3700B3)
private val Teal200 = Color(0xFF03DAC5)
private val DeepPink = Color(0xFFFF1493)

private val AppColorScheme = lightColorScheme(
    primary = Purple500,
    onPrimary = Color.White,
    primaryContainer = Purple700,
    secondary = Teal200,
    onSecondary = Color.Black,
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun UVCAndroidTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
