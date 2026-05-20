package com.aegisinput.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KeyboardLight = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFE5E7EB),
)

private val KeyboardDark = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    surface = Color(0xFF1F2937),
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF374151),
)

@Composable
fun AegisInputTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KeyboardDark else KeyboardLight
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
