package com.example.ui.theme

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

/**
 * App Theme Mode option
 */
enum class AppThemeSetting(val label: String, val subtitle: String) {
    SYSTEM("System Default", "Dynamic wallpaper colors & system dark/light mode"),
    DARK("Dark Theme", "Dynamic wallpaper or Olive Material You dark theme"),
    LIGHT("Light Theme", "Dynamic wallpaper or Olive Material You light theme")
}

// Fallback Olive / Moss Green Material You Dark Scheme (from user's uploaded screenshot)
private val OliveDarkColorScheme = darkColorScheme(
    primary = SoundAmpPrimaryNeon,
    onPrimary = Color(0xFF1F3701),
    primaryContainer = SoundAmpPrimaryContainer,
    onPrimaryContainer = SoundAmpOnPrimaryContainer,
    secondary = SoundAmpSecondaryBlue,
    onSecondary = Color(0xFF2A331E),
    secondaryContainer = SoundAmpSecondaryContainer,
    onSecondaryContainer = SoundAmpOnSecondaryContainer,
    tertiary = SoundAmpTertiaryBoost,
    onTertiary = Color(0xFF003735),
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

// Fallback Olive / Sage Material You Light Scheme
private val OliveLightColorScheme = lightColorScheme(
    primary = SoundAmpPrimaryLightMode,
    onPrimary = Color.White,
    primaryContainer = SoundAmpPrimaryContainerLight,
    onPrimaryContainer = SoundAmpOnPrimaryContainerLight,
    secondary = SoundAmpSecondaryLightMode,
    onSecondary = Color.White,
    secondaryContainer = SoundAmpSecondaryContainerLight,
    onSecondaryContainer = SoundAmpOnSecondaryContainerLight,
    tertiary = Color(0xFF386663),
    onTertiary = Color.White,
    tertiaryContainer = SoundAmpOnTertiaryContainer,
    onTertiaryContainer = Color(0xFF00201E),
    background = SoundAmpBgLight,
    onBackground = SoundAmpTextPrimaryLight,
    surface = SoundAmpSurfaceLight,
    onSurface = SoundAmpTextPrimaryLight,
    surfaceVariant = SoundAmpCardLight,
    onSurfaceVariant = SoundAmpTextSecondaryLight,
    outline = SoundAmpOutlineLight,
    error = Color(0xFFBA1A1A)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

/**
 * Resolves the active ColorScheme:
 * 1. On Android 12+ (API 31+), extracts dynamic Material You colors directly from the system wallpaper
 *    using dynamicDarkColorScheme / dynamicLightColorScheme.
 * 2. On Android 11 and below (API <= 30, where native wallpaper dynamic color extraction is unavailable)
 *    or if extraction fails, uses the Olive / Moss Green Material You color theme from the uploaded reference image.
 */
private fun resolveDynamicOrOliveColorScheme(context: Context, isDark: Boolean): ColorScheme {
    val fallback = if (isDark) OliveDarkColorScheme else OliveLightColorScheme
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return runCatching {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }.getOrDefault(fallback)
    }
    return fallback
}

@Composable
fun MyApplicationTheme(
    themeSetting: AppThemeSetting = AppThemeSetting.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeSetting) {
        AppThemeSetting.SYSTEM -> isSystemDark
        AppThemeSetting.DARK -> true
        AppThemeSetting.LIGHT -> false
    }

    val colorScheme = remember(context, isDark) {
        resolveDynamicOrOliveColorScheme(context, isDark)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
