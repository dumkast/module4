// Theme.kt
package com.example.module4.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenPrimary = Color(0xFF2E7D32)
private val GreenSecondary = Color(0xFF4CAF50)
private val GreenTertiary = Color(0xFF81C784)
private val GreenSurface = Color(0xFFE8F5E9)
private val GreenBackground = Color(0xFFF1F8E9)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenSurface,
    onPrimaryContainer = GreenPrimary,

    secondary = GreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = GreenTertiary,
    onSecondaryContainer = Color.Black,

    tertiary = GreenTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = GreenSurface,
    onTertiaryContainer = GreenPrimary,

    background = GreenBackground,
    onBackground = Color.Black,

    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = GreenSurface,
    onSurfaceVariant = GreenPrimary,

    error = Color(0xFFBA1A1A),
    onError = Color.White
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