package com.antigravity.peqconfigurator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.antigravity.peqconfigurator.data.AppSettings
import com.antigravity.peqconfigurator.data.ColorPalette
import com.antigravity.peqconfigurator.data.ThemeMode

// Universal Backgrounds (Dark)
private val UniversalDarkBackground = Color(0xFF121212)
private val UniversalDarkSurface = Color(0xFF1E1E1E)
private val UniversalDarkSurfaceVariant = Color(0xFF2C2C2C)
private val UniversalDarkOnBackground = Color(0xFFE3E3E3)
private val UniversalDarkOnSurface = Color(0xFFE3E3E3)
private val UniversalDarkOnSurfaceVariant = Color(0xFFC4C4C4)
private val UniversalDarkOutline = Color(0xFF8E8E8E)
private val UniversalDarkOutlineVariant = Color(0xFF444444)

// Universal Backgrounds (Light)
private val UniversalLightBackground = Color(0xFFF8F9FA)
private val UniversalLightSurface = Color(0xFFFFFFFF)
private val UniversalLightSurfaceVariant = Color(0xFFE9ECEF)
private val UniversalLightOnBackground = Color(0xFF1C1D1F)
private val UniversalLightOnSurface = Color(0xFF1C1D1F)
private val UniversalLightOnSurfaceVariant = Color(0xFF4A4D51)
private val UniversalLightOutline = Color(0xFF79747E)
private val UniversalLightOutlineVariant = Color(0xFFCAC4D0)

// Cyberpunk Cyan Palette
private val CyberpunkDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF76F5FF),
    secondary = Color(0xFF9D4EDD),
    onSecondary = Color(0xFF2A0054),
    secondaryContainer = Color(0xFF4D008D),
    onSecondaryContainer = Color(0xFFE9B3FF),
    tertiary = Color(0xFFFF2A85),
    onTertiary = Color(0xFF3E001D),
    tertiaryContainer = Color(0xFF6C0035),
    onTertiaryContainer = Color(0xFFFFD9E2),
    background = UniversalDarkBackground,
    surface = UniversalDarkSurface,
    surfaceVariant = UniversalDarkSurfaceVariant,
    onBackground = UniversalDarkOnBackground,
    onSurface = UniversalDarkOnSurface,
    onSurfaceVariant = UniversalDarkOnSurfaceVariant,
    outline = UniversalDarkOutline,
    outlineVariant = UniversalDarkOutlineVariant
)

private val CyberpunkLightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF80F9FF),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF7B1FA2),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3D8FF),
    onSecondaryContainer = Color(0xFF2B004F),
    tertiary = Color(0xFFC2185B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3E001D),
    background = UniversalLightBackground,
    surface = UniversalLightSurface,
    surfaceVariant = UniversalLightSurfaceVariant,
    onBackground = UniversalLightOnBackground,
    onSurface = UniversalLightOnSurface,
    onSurfaceVariant = UniversalLightOnSurfaceVariant,
    outline = UniversalLightOutline,
    outlineVariant = UniversalLightOutlineVariant
)

// Android Green Palette
private val AndroidGreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF005227),
    onPrimaryContainer = Color(0xFF79FFAC),
    secondary = Color(0xFF00B0FF),
    onSecondary = Color(0xFF00344F),
    secondaryContainer = Color(0xFF004B71),
    onSecondaryContainer = Color(0xFF79D5FF),
    tertiary = Color(0xFFFFB300),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4100),
    onTertiaryContainer = Color(0xFFFFE38B),
    background = UniversalDarkBackground,
    surface = UniversalDarkSurface,
    surfaceVariant = UniversalDarkSurfaceVariant,
    onBackground = UniversalDarkOnBackground,
    onSurface = UniversalDarkOnSurface,
    onSurfaceVariant = UniversalDarkOnSurfaceVariant,
    outline = UniversalDarkOutline,
    outlineVariant = UniversalDarkOutlineVariant
)

private val AndroidGreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF008940),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF79FFAC),
    onPrimaryContainer = Color(0xFF00210B),
    secondary = Color(0xFF0277BD),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCBE6FF),
    onSecondaryContainer = Color(0xFF001E30),
    tertiary = Color(0xFFEF6C00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFF3B1700),
    background = UniversalLightBackground,
    surface = UniversalLightSurface,
    surfaceVariant = UniversalLightSurfaceVariant,
    onBackground = UniversalLightOnBackground,
    onSurface = UniversalLightOnSurface,
    onSurfaceVariant = UniversalLightOnSurfaceVariant,
    outline = UniversalLightOutline,
    outlineVariant = UniversalLightOutlineVariant
)

// Deep Violet Palette
private val DeepVioletDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB388FF),
    onPrimary = Color(0xFF22005D),
    primaryContainer = Color(0xFF360090),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFFF4081),
    onSecondary = Color(0xFF3E001D),
    secondaryContainer = Color(0xFF5D002E),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFF00E5FF),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFF76F5FF),
    background = UniversalDarkBackground,
    surface = UniversalDarkSurface,
    surfaceVariant = UniversalDarkSurfaceVariant,
    onBackground = UniversalDarkOnBackground,
    onSurface = UniversalDarkOnSurface,
    onSurfaceVariant = UniversalDarkOnSurfaceVariant,
    outline = UniversalDarkOutline,
    outlineVariant = UniversalDarkOutlineVariant
)

private val DeepVioletLightColorScheme = lightColorScheme(
    primary = Color(0xFF651FFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF1E0060),
    secondary = Color(0xFFC51162),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF3E001D),
    tertiary = Color(0xFF0091EA),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCBE6FF),
    onTertiaryContainer = Color(0xFF001E30),
    background = UniversalLightBackground,
    surface = UniversalLightSurface,
    surfaceVariant = UniversalLightSurfaceVariant,
    onBackground = UniversalLightOnBackground,
    onSurface = UniversalLightOnSurface,
    onSurfaceVariant = UniversalLightOnSurfaceVariant,
    outline = UniversalLightOutline,
    outlineVariant = UniversalLightOutlineVariant
)

// Sunset Amber Palette
private val SunsetAmberDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB300),
    onPrimary = Color(0xFF402D00),
    primaryContainer = Color(0xFF5C4100),
    onPrimaryContainer = Color(0xFFFFE38B),
    secondary = Color(0xFFFF5722),
    onSecondary = Color(0xFF431300),
    secondaryContainer = Color(0xFF632100),
    onSecondaryContainer = Color(0xFFFFDBD1),
    tertiary = Color(0xFF00E5FF),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFF76F5FF),
    background = UniversalDarkBackground,
    surface = UniversalDarkSurface,
    surfaceVariant = UniversalDarkSurfaceVariant,
    onBackground = UniversalDarkOnBackground,
    onSurface = UniversalDarkOnSurface,
    onSurfaceVariant = UniversalDarkOnSurfaceVariant,
    outline = UniversalDarkOutline,
    outlineVariant = UniversalDarkOutlineVariant
)

private val SunsetAmberLightColorScheme = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF3B1700),
    secondary = Color(0xFFBF360C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBD1),
    onSecondaryContainer = Color(0xFF3A0B00),
    tertiary = Color(0xFF00838F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF80F9FF),
    onTertiaryContainer = Color(0xFF002022),
    background = UniversalLightBackground,
    surface = UniversalLightSurface,
    surfaceVariant = UniversalLightSurfaceVariant,
    onBackground = UniversalLightOnBackground,
    onSurface = UniversalLightOnSurface,
    onSurfaceVariant = UniversalLightOnSurfaceVariant,
    outline = UniversalLightOutline,
    outlineVariant = UniversalLightOutlineVariant
)

// Ocean Blue Palette
private val OceanBlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFF448AFF),
    onPrimary = Color(0xFF002C71),
    primaryContainer = Color(0xFF0040A0),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF76F5FF),
    tertiary = Color(0xFFFF4081),
    onTertiary = Color(0xFF3E001D),
    tertiaryContainer = Color(0xFF5D002E),
    onTertiaryContainer = Color(0xFFFFD9E2),
    background = UniversalDarkBackground,
    surface = UniversalDarkSurface,
    surfaceVariant = UniversalDarkSurfaceVariant,
    onBackground = UniversalDarkOnBackground,
    onSurface = UniversalDarkOnSurface,
    onSurfaceVariant = UniversalDarkOnSurfaceVariant,
    outline = UniversalDarkOutline,
    outlineVariant = UniversalDarkOutlineVariant
)

private val OceanBlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A43),
    secondary = Color(0xFF00838F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF80F9FF),
    onSecondaryContainer = Color(0xFF002022),
    tertiary = Color(0xFFC2185B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3E001D),
    background = UniversalLightBackground,
    surface = UniversalLightSurface,
    surfaceVariant = UniversalLightSurfaceVariant,
    onBackground = UniversalLightOnBackground,
    onSurface = UniversalLightOnSurface,
    onSurfaceVariant = UniversalLightOnSurfaceVariant,
    outline = UniversalLightOutline,
    outlineVariant = UniversalLightOutlineVariant
)

/** Fills M3 surfaceContainer* roles by tinting `surface` with `primary`, so custom palettes stay on-theme. */
private fun ColorScheme.withTonalContainers(): ColorScheme {
    fun tone(a: Float) = androidx.compose.ui.graphics.lerp(surface, primary, a)
    return copy(
        surfaceDim = androidx.compose.ui.graphics.lerp(surface, Color.Black, 0.10f),
        surfaceBright = tone(0.12f),
        surfaceContainerLowest = androidx.compose.ui.graphics.lerp(surface, Color.Black, 0.06f),
        surfaceContainerLow = tone(0.03f),
        surfaceContainer = tone(0.06f),
        surfaceContainerHigh = tone(0.09f),
        surfaceContainerHighest = tone(0.12f)
    )
}

/** M3 Expressive shape scale: generously rounded containers, pill-shaped small controls. */
val ExpressiveShapes = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AndroidPEQConfiguratorTheme(
    appSettings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val isDark = when (appSettings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme: ColorScheme = when (appSettings.colorPalette) {
        ColorPalette.SYSTEM_DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) CyberpunkDarkColorScheme else CyberpunkLightColorScheme
            }
        }
        ColorPalette.CYBERPUNK_CYAN -> if (isDark) CyberpunkDarkColorScheme else CyberpunkLightColorScheme
        ColorPalette.ANDROID_GREEN -> if (isDark) AndroidGreenDarkColorScheme else AndroidGreenLightColorScheme
        ColorPalette.DEEP_VIOLET -> if (isDark) DeepVioletDarkColorScheme else DeepVioletLightColorScheme
        ColorPalette.SUNSET_AMBER -> if (isDark) SunsetAmberDarkColorScheme else SunsetAmberLightColorScheme
        ColorPalette.OCEAN_BLUE -> if (isDark) OceanBlueDarkColorScheme else OceanBlueLightColorScheme
    }

    val themed = if (appSettings.colorPalette == ColorPalette.SYSTEM_DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorScheme // dynamic schemes already define all container roles
    } else {
        colorScheme.withTonalContainers()
    }

    MaterialExpressiveTheme(
        colorScheme = themed,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = Typography,
        content = content
    )
}
