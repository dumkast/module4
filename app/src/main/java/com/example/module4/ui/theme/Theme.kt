package com.example.module4.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenPrimary = Color(0xFF388E3C)
private val GreenSecondary = Color(0xFF66BB6A)
private val GreenTertiary = Color(0xFFE6F4EA)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = GreenSecondary,
    tertiary = GreenTertiary,

    background = Color.White,
    surface = Color.White,

    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,

    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun Module4Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}