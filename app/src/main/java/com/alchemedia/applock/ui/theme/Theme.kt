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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Deep green-black surfaces with one mint accent. Alerts stay a warm coral so intruder events read as
// different from "on" states at a glance.
private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF3DDC97),
    onPrimary = Color(0xFF03261A),
    primaryContainer = Color(0xFF0F3A2A),
    onPrimaryContainer = Color(0xFFC9F7E0),
    secondary = Color(0xFF8FB8A5),
    onSecondary = Color(0xFF07140E),
    tertiary = Color(0xFFFF7A88),
    background = Color(0xFF040B08),
    onBackground = Color(0xFFE4F2EB),
    surface = Color(0xFF0A1511),
    onSurface = Color(0xFFE4F2EB),
    surfaceVariant = Color(0xFF14261F),
    onSurfaceVariant = Color(0xFF8DA89B),
    surfaceContainerLowest = Color(0xFF040B08),
    surfaceContainerLow = Color(0xFF0A1410),
    surfaceContainer = Color(0xFF0E1B16),
    surfaceContainerHigh = Color(0xFF14261F),
    surfaceContainerHighest = Color(0xFF1B332A),
    outline = Color(0xFF3A5A4C),
    outlineVariant = Color(0xFF1F382E),
    error = Color(0xFFFF7A88),
    onError = Color(0xFF3F0710),
    errorContainer = Color(0xFF4A1620),
    onErrorContainer = Color(0xFFFFD9DD),
)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0A8F5C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDF3E0),
    onPrimaryContainer = Color(0xFF04301F),
    secondary = Color(0xFF4C6B5C),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFD92D3F),
    background = Color(0xFFEFF7F3),
    onBackground = Color(0xFF08140F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF08140F),
    surfaceVariant = Color(0xFFDDEBE4),
    onSurfaceVariant = Color(0xFF55705F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6FAF8),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE1EEE8),
    surfaceContainerHighest = Color(0xFFD4E5DC),
    outline = Color(0xFF8AA597),
    outlineVariant = Color(0xFFCBDDD3),
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
    // Monospace labels give the screens a terminal-readout feel.
    labelMedium = Base.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp),
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

/** Page background: base colour, a soft mint glow from the top-right corner and a faint dot grid. */
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
                    val step = 26.dp.toPx()
                    val dot = glow.copy(alpha = 0.07f)
                    var y = step / 2
                    while (y < size.height) {
                        var x = step / 2
                        while (x < size.width) {
                            drawCircle(dot, radius = 1.1.dp.toPx(), center = Offset(x, y))
                            x += step
                        }
                        y += step
                    }
                },
            content = content,
        )
    }
}
