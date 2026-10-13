package com.unsa.dhh.captions.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = HighContrastYellow,
    onPrimary = PureBlack,
    secondary = PureWhite,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = HighContrastYellow,
    surface = DarkSurface,
    onSurface = PureWhite
)

val TeleprompterTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 42.sp,
    letterSpacing = 0.5.sp,
    color = HighContrastYellow
)

fun getResponsiveTeleprompterStyle(isCompactScreen: Boolean): TextStyle {
    return if (isCompactScreen) {
        TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.4.sp,
            color = HighContrastYellow
        )
    } else {
        TeleprompterTextStyle
    }
}

@Composable
fun WCAGHighContrastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
