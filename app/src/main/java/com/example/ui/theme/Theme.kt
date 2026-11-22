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

private val VoltPulseDarkColorScheme =
  darkColorScheme(
    primary = VoltEmerald,
    onPrimary = Color(0xFF00210E),
    primaryContainer = VoltEmeraldContainerDark,
    onPrimaryContainer = Color(0xFF8CFFBD),
    secondary = VoltCyan,
    onSecondary = Color(0xFF001F2E),
    secondaryContainer = VoltCyanContainerDark,
    onSecondaryContainer = Color(0xFFB3E5FC),
    tertiary = VoltAmber,
    onTertiary = Color(0xFF281900),
    tertiaryContainer = VoltAmberContainerDark,
    onTertiaryContainer = Color(0xFFFFE082),
    error = VoltCrimson,
    onError = Color(0xFF330004),
    errorContainer = VoltCrimsonContainerDark,
    onErrorContainer = Color(0xFFFFB4B4),
    background = ObsidianBackground,
    onBackground = ObsidianTextPrimary,
    surface = ObsidianSurface,
    onSurface = ObsidianTextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianTextSecondary,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF1F2A38),
  )

private val VoltPulseLightColorScheme =
  lightColorScheme(
    primary = VoltEmeraldDark,
    onPrimary = Color.White,
    primaryContainer = VoltEmeraldContainerLight,
    onPrimaryContainer = Color(0xFF00391A),
    secondary = VoltCyanDark,
    onSecondary = Color.White,
    secondaryContainer = VoltCyanContainerLight,
    onSecondaryContainer = Color(0xFF00344F),
    tertiary = VoltAmberDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEBC2),
    onTertiaryContainer = Color(0xFF3D2600),
    error = VoltCrimsonDark,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = LabLightBackground,
    onBackground = LabLightTextPrimary,
    surface = LabLightSurface,
    onSurface = LabLightTextPrimary,
    surfaceVariant = LabLightSurfaceVariant,
    onSurfaceVariant = LabLightTextSecondary,
    surfaceContainerHigh = LabLightSurfaceHigh,
    outline = LabLightOutline,
    outlineVariant = Color(0xFFDCE4EE),
  )

val VoltPulseShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
  )

@Composable
fun VoltPulseTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) VoltPulseDarkColorScheme else VoltPulseLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = VoltPulseShapes,
    content = content,
  )
}
