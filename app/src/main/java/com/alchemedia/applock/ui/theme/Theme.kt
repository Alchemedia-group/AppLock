package com.alchemedia.applock.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Deep blue-black surfaces with one electric-blue accent. Alerts use a warm coral so intruder
// events read as different from "on" states at a glance.
private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF6EA8FF),
    onPrimary = Color(0xFF05213F),
    primaryContainer = Color(0xFF14315C),
    onPrimaryContainer = Color(0xFFD8E7FF),
    secondary = Color(0xFF8FA3C2),
    onSecondary = Color(0xFF0B1424),
    tertiary = Color(0xFFFF7A88),
    background = Color(0xFF070C16),
    onBackground = Color(0xFFE8EEF9),
    surface = Color(0xFF0D1422),
    onSurface = Color(0xFFE8EEF9),
    surfaceVariant = Color(0xFF1A2540),
    onSurfaceVariant = Color(0xFF93A3BD),
    surfaceContainerLowest = Color(0xFF070C16),
    surfaceContainerLow = Color(0xFF0E1525),
    surfaceContainer = Color(0xFF121B2E),
    surfaceContainerHigh = Color(0xFF192540),
    surfaceContainerHighest = Color(0xFF22304F),
    outline = Color(0xFF3B4B6B),
    outlineVariant = Color(0xFF243250),
    error = Color(0xFFFF7A88),
    onError = Color(0xFF3F0710),
    errorContainer = Color(0xFF4A1620),
    onErrorContainer = Color(0xFFFFD9DD),
)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF0B2A66),
    secondary = Color(0xFF52627C),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFD92D3F),
    background = Color(0xFFF2F5FB),
    onBackground = Color(0xFF0B1220),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B1220),
    surfaceVariant = Color(0xFFE3EAF5),
    onSurfaceVariant = Color(0xFF5A6A84),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFE),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE9EFF9),
    surfaceContainerHighest = Color(0xFFDDE6F4),
    outline = Color(0xFF8FA0BB),
    outlineVariant = Color(0xFFD3DCEA),
    error = Color(0xFFD92D3F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDDE0),
    onErrorContainer = Color(0xFF5A0B15),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

private val Base = Typography()
private val AppTypography = Typography(
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 16.sp),
    bodySmall = Base.bodySmall.copy(lineHeight = 18.sp),
)

@Composable
fun AppLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}

/** Page background: the base colour with a soft accent glow from the top-right corner. */
@Composable
fun AppBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    val glow = colors.primary
    CompositionLocalProvider(LocalContentColor provides colors.onBackground) {
        Box(
            modifier
                .fillMaxSize()
                .background(colors.background)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(glow.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(size.width * 0.85f, -size.height * 0.03f),
                            radius = size.width * 1.15f,
                        ),
                    )
                },
            content = content,
        )
    }
}

