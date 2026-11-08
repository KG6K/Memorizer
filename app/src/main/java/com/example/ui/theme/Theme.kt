package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * App Theme Mode option
 */
enum class AppThemeSetting(val label: String, val subtitle: String) {
    SYSTEM("System Default", "Follows device appearance automatically"),
    DARK("Dark Theme", "Sound Amplifier midnight studio console"),
    LIGHT("Light Theme", "High-clarity clinical sound lab")
}

private val DarkColorScheme = darkColorScheme(
    primary = SoundAmpPrimaryNeon,
    onPrimary = Color(0xFF00201A),
    primaryContainer = SoundAmpPrimaryContainer,
    onPrimaryContainer = SoundAmpOnPrimaryContainer,
    secondary = SoundAmpSecondaryBlue,
    onSecondary = Color(0xFF001F2D),
    secondaryContainer = SoundAmpSecondaryContainer,
    onSecondaryContainer = SoundAmpOnSecondaryContainer,
    tertiary = SoundAmpTertiaryBoost,
    onTertiary = Color(0xFF261900),
    tertiaryContainer = SoundAmpTertiaryContainer,
    onTertiaryContainer = SoundAmpOnTertiaryContainer,
    background = SoundAmpBgDark,
    onBackground = SoundAmpTextPrimaryDark,
    surface = SoundAmpSurfaceDark,
    onSurface = SoundAmpTextPrimaryDark,
    surfaceVariant = SoundAmpCardDark,
    onSurfaceVariant = SoundAmpTextSecondaryDark,
    surfaceContainerHigh = SoundAmpCardElevatedDark,
    outline = SoundAmpOutlineDark,
    error = SoundAmpError
)

private val LightColorScheme = lightColorScheme(
    primary = SoundAmpPrimaryLightMode,
    onPrimary = Color.White,
    primaryContainer = SoundAmpPrimaryContainerLight,
    onPrimaryContainer = SoundAmpOnPrimaryContainerLight,
    secondary = SoundAmpSecondaryLightMode,
    onSecondary = Color.White,
    secondaryContainer = SoundAmpSecondaryContainerLight,
    onSecondaryContainer = SoundAmpOnSecondaryContainerLight,
    tertiary = SoundAmpTertiaryBoost,
    onTertiary = Color(0xFF261900),
    background = SoundAmpBgLight,
    onBackground = SoundAmpTextPrimaryLight,
    surface = SoundAmpSurfaceLight,
    onSurface = SoundAmpTextPrimaryLight,
    surfaceVariant = SoundAmpCardLight,
    onSurfaceVariant = SoundAmpTextSecondaryLight,
    outline = SoundAmpOutlineLight,
    error = SoundAmpError
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun MyApplicationTheme(
    themeSetting: AppThemeSetting = AppThemeSetting.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeSetting) {
        AppThemeSetting.SYSTEM -> isSystemDark
        AppThemeSetting.DARK -> true
        AppThemeSetting.LIGHT -> false
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
