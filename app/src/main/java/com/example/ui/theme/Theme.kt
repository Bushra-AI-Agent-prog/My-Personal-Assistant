package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = VioletNeon,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = PurpleAccent,
    background = CosmicBackground,
    onBackground = CosmicTextPrimary,
    surface = CosmicSurface,
    onSurface = CosmicTextPrimary,
    surfaceVariant = CosmicSurfaceGlass,
    onSurfaceVariant = CosmicTextSecondary,
    outline = CosmicCardBorder,
    error = RoseError,
    onError = Color.White
)

private val AmoledColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF072E44),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = VioletNeon,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = PurpleAccent,
    background = Color.Black,
    onBackground = Color(0xFFF8FAFC),
    surface = Color.Black,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF111116),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF27272A),
    error = RoseError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CyanAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = VioletNeon,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF6B21A8),
    tertiary = PurpleAccent,
    background = CrystalBackground,
    onBackground = CrystalTextPrimary,
    surface = CrystalSurface,
    onSurface = CrystalTextPrimary,
    surfaceVariant = CrystalSurfaceGlass,
    onSurfaceVariant = CrystalTextSecondary,
    outline = CrystalCardBorder,
    error = RoseError,
    onError = Color.White
)

@Composable
fun BushraTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.AMOLED_BLACK -> AmoledColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Retain backwards compatibility wrapper
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BushraTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        content = content
    )
}
