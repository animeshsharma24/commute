package com.animesh.commutetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppDarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = DarkBackground,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    
    secondary = BrandSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    
    background = DarkBackground,
    onBackground = TextPrimary,
    
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    error = BrandDestructive,
    errorContainer = BrandDestructiveContainer,
    onErrorContainer = BrandOnDestructiveContainer,
    
    outline = DividerColor,
    outlineVariant = DividerColor
)

@Composable
fun CommuteTrackerTheme(
    darkTheme: Boolean = true, // Force dark theme
    dynamicColor: Boolean = false, // Force our custom premium color palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = Typography,
        content = content
    )
}
