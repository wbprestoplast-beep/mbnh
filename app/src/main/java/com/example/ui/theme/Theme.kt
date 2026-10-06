package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BrandTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF164E63),
    secondary = BrandCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = MedGreen,
    onTertiary = Color.White,
    background = BgLight,
    onBackground = InkLight,
    surface = CardLight,
    onSurface = InkLight,
    surfaceVariant = ChipLight,
    onSurfaceVariant = SubLight,
    outline = LineLight,
    outlineVariant = Color(0xFFCBD5E1),
    error = MedDanger,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandCyanDark,
    onPrimary = Color(0xFF083344),
    primaryContainer = Color(0xFF164E63),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = BrandAccentDark,
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF0E7490),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = MedGreenDark,
    onTertiary = Color(0xFF052E16),
    background = BgDark,
    onBackground = InkDark,
    surface = CardDark,
    onSurface = InkDark,
    surfaceVariant = ChipDark,
    onSurfaceVariant = SubDark,
    outline = LineDark,
    outlineVariant = Color(0xFF334155),
    error = MedDangerDark,
    onError = Color(0xFF450A0A)
)

enum class AppThemeSetting {
    AUTO,
    LIGHT,
    DARK
}

@Composable
fun HospitalTheme(
    themeSetting: AppThemeSetting = AppThemeSetting.AUTO,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        AppThemeSetting.AUTO -> isSystemInDarkTheme()
        AppThemeSetting.LIGHT -> false
        AppThemeSetting.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
