package com.aegisinput.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KeyboardLight = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    surface = Color(0xFFECEFF1),
    onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFDFE1E5),
    onSurfaceVariant = Color(0xFF5F6368),
    outline = Color(0xFFDADCE0),
)

private val KeyboardDark = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF202124),
    surface = Color(0xFF303134),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF3C4043),
    onSurfaceVariant = Color(0xFF9AA0A6),
    outline = Color(0xFF5F6368),
)

@Composable
fun AegisInputTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KeyboardDark else KeyboardLight
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
