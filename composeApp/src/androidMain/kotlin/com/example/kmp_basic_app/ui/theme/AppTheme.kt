package com.example.kmp_basic_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Notion-inspired color tokens
object NotionColors {
    // Light
    val LightBackground = Color(0xFFFFFFFF)
    val LightSurface = Color(0xFFF6F5F4)
    val LightTextPrimary = Color.Black.copy(alpha = 0.95f)
    val LightTextSecondary = Color(0xFF615D59)
    val LightTextTertiary = Color(0xFFA39E98)
    val LightPrimary = Color(0xFF0075DE)
    val LightPrimaryPressed = Color(0xFF005BAB)
    val LightBorder = Color.Black.copy(alpha = 0.10f)
    val LightError = Color(0xFFDD5B00)
    val LightSuccess = Color(0xFF2A9D99)

    // Dark
    val DarkBackground = Color(0xFF191919)
    val DarkSurface = Color(0xFF31302E)
    val DarkTextPrimary = Color.White.copy(alpha = 0.95f)
    val DarkTextSecondary = Color(0xFFA39E98)
    val DarkPrimary = Color(0xFF62AEF0)
    val DarkBorder = Color.White.copy(alpha = 0.10f)

    // Status dots
    val StatusAlive = Color(0xFF55CC44)
    val StatusDead = Color(0xFFD63D2E)
    val StatusUnknown = Color(0xFF9E9E9E)

    // Pill badge
    val PillBackground = Color(0xFFF2F9FF)
    val PillText = Color(0xFF097FE8)
}

private val NotionLightColorScheme = lightColorScheme(
    primary = NotionColors.LightPrimary,
    onPrimary = Color.White,
    primaryContainer = NotionColors.PillBackground,
    onPrimaryContainer = NotionColors.PillText,
    secondary = NotionColors.LightTextSecondary,
    onSecondary = Color.White,
    background = NotionColors.LightBackground,
    onBackground = NotionColors.LightTextPrimary,
    surface = NotionColors.LightSurface,
    onSurface = NotionColors.LightTextPrimary,
    surfaceVariant = NotionColors.LightSurface,
    onSurfaceVariant = NotionColors.LightTextSecondary,
    outline = NotionColors.LightBorder,
    outlineVariant = NotionColors.LightBorder,
    error = NotionColors.LightError,
    onError = Color.White,
    tertiary = NotionColors.LightSuccess,
    onTertiary = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAF9F8),
    surfaceContainer = NotionColors.LightSurface,
    surfaceContainerHigh = Color(0xFFEFEEED),
    surfaceContainerHighest = Color(0xFFE8E7E6),
)

private val NotionDarkColorScheme = darkColorScheme(
    primary = NotionColors.DarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1A3A5C),
    onPrimaryContainer = NotionColors.DarkPrimary,
    secondary = NotionColors.DarkTextSecondary,
    onSecondary = Color.Black,
    background = NotionColors.DarkBackground,
    onBackground = NotionColors.DarkTextPrimary,
    surface = NotionColors.DarkSurface,
    onSurface = NotionColors.DarkTextPrimary,
    surfaceVariant = NotionColors.DarkSurface,
    onSurfaceVariant = NotionColors.DarkTextSecondary,
    outline = NotionColors.DarkBorder,
    outlineVariant = NotionColors.DarkBorder,
    error = Color(0xFFFF8A65),
    onError = Color.Black,
    tertiary = Color(0xFF4DB6AC),
    onTertiary = Color.Black,
    surfaceContainerLowest = Color(0xFF141414),
    surfaceContainerLow = Color(0xFF1E1E1E),
    surfaceContainer = NotionColors.DarkSurface,
    surfaceContainerHigh = Color(0xFF3A3937),
    surfaceContainerHighest = Color(0xFF444341),
)

val NotionTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.5).sp,
        lineHeight = 48.sp
    ),
    displayMedium = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.0).sp,
        lineHeight = 40.sp
    ),
    displaySmall = TextStyle(
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.75).sp,
        lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.625).sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.125).sp,
        lineHeight = 26.sp
    ),
    titleSmall = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.125.sp,
        lineHeight = 16.sp
    ),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NotionDarkColorScheme else NotionLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NotionTypography,
        content = content
    )
}
