package com.inkflow.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkflow.app.data.model.AccentPalette
import com.inkflow.app.data.model.ThemeMode

val InkFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val InkFlowTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun InkFlowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentPalette: AccentPalette = AccentPalette.AMBER_QUILL,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDark) {
        val primary = Color(accentPalette.darkPrimary)
        val secondary = Color(accentPalette.darkSecondary)
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF141210),
            primaryContainer = primary.copy(alpha = 0.22f),
            onPrimaryContainer = Color(0xFFFAF7F2),
            secondary = secondary,
            onSecondary = Color(0xFF141210),
            secondaryContainer = secondary.copy(alpha = 0.18f),
            onSecondaryContainer = Color(0xFFF5F1EB),
            background = Color(0xFF12100E),        // Warm Espresso Charcoal (Zero AI blue)
            onBackground = Color(0xFFFAF7F2),
            surface = Color(0xFF1C1917),           // Warm Dark Stone Surface
            onSurface = Color(0xFFFAF7F2),
            surfaceVariant = Color(0xFF292524),    // Warm Graphite Card
            onSurfaceVariant = Color(0xFFA8A29E),
            outline = Color(0xFF44403C)
        )
    } else {
        val primary = Color(accentPalette.lightPrimary)
        val secondary = Color(accentPalette.lightSecondary)
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.14f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.copy(alpha = 0.15f),
            onSecondaryContainer = secondary,
            background = Color(0xFFF6F3EC),        // Warm Parchment Ivory
            onBackground = Color(0xFF1C1917),
            surface = Color(0xFFFFFDF9),           // Crisp Studio Paper
            onSurface = Color(0xFF1C1917),
            surfaceVariant = Color(0xFFEBE5DA),    // Warm Sand Pill Surface
            onSurfaceVariant = Color(0xFF57534E),
            outline = Color(0xFFD6CFC2)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = InkFlowShapes,
        typography = InkFlowTypography,
        content = content
    )
}
