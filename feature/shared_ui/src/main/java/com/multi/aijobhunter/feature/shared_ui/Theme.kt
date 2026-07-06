package com.multi.aijobhunter.feature.shared_ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PureBlack = Color(0xFF000000)
val TerminalDarkGray = Color(0xFF121212)
val TerminalMediumGray = Color(0xFF1E1E1E)
val TerminalLightGray = Color(0xFF2C2C2C)
val MutedText = Color(0xFF8E8E93)
val NeonGreen = Color(0xFF00FF66)
val NeonGreenDim = Color(0xFF00B344)
val NeonRed = Color(0xFFFF3333)
val NeonAmber = Color(0xFFFF9900)
val PureWhite = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = PureBlack,
    secondary = TerminalMediumGray,
    onSecondary = PureWhite,
    background = PureBlack,
    onBackground = PureWhite,
    surface = TerminalDarkGray,
    onSurface = PureWhite,
    error = NeonRed,
    onError = PureWhite
)

val MonospaceTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp
    )
)

@Composable
fun AiJobHunterTheme(
    darkTheme: Boolean = true, // Force dark theme by default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MonospaceTypography,
        content = content
    )
}
