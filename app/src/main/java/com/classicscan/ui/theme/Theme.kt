package com.classicscan.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import com.classicscan.R
import com.classicscan.data.ThemeMode

@Immutable
data class Accent(val primary: Color, val partner: Color)

val ACCENTS = listOf(
    Accent(Color(0xFF2F6BFF), Color(0xFF00C2D1)),
    Accent(Color(0xFF7C4DFF), Color(0xFFFF4D9D)),
    Accent(Color(0xFF00B894), Color(0xFF0984E3)),
    Accent(Color(0xFFFF6B6B), Color(0xFFFFA94D)),
    Accent(Color(0xFFFF8A00), Color(0xFFFF3D71)),
    Accent(Color(0xFFE6398C), Color(0xFF8A4DFF)),
)

private val Ink = Color(0xFF1C1B22)
private val Paper = Color(0xFFF4F2F8)

fun Color.onColor(): Color = if (luminance() > 0.45f) Ink else Color.White

private fun schemeFor(a: Accent, dark: Boolean): ColorScheme {
    val seed = a.primary
    return if (!dark) {
        val base = Color(0xFFFCFBFF)
        fun tint(f: Float) = lerp(base, seed, f)
        lightColorScheme(
            primary = lerp(seed, Color.Black, 0.08f), onPrimary = Color.White,
            primaryContainer = lerp(seed, Color.White, 0.82f), onPrimaryContainer = lerp(seed, Color.Black, 0.6f),
            secondary = lerp(seed, Color(0xFF55536A), 0.6f), onSecondary = Color.White,
            secondaryContainer = lerp(seed, Color.White, 0.86f), onSecondaryContainer = lerp(seed, Color.Black, 0.65f),
            tertiary = lerp(a.partner, Color.Black, 0.1f), onTertiary = Color.White,
            tertiaryContainer = lerp(a.partner, Color.White, 0.82f), onTertiaryContainer = lerp(a.partner, Color.Black, 0.6f),
            background = tint(0.03f), onBackground = Ink, surface = tint(0.03f), onSurface = Ink,
            surfaceVariant = tint(0.1f), onSurfaceVariant = Color(0xFF5F5D6E),
            surfaceContainerLowest = Color.White, surfaceContainerLow = tint(0.04f), surfaceContainer = tint(0.06f),
            surfaceContainerHigh = tint(0.09f), surfaceContainerHighest = tint(0.13f),
            outline = Color(0xFF8C8A9B), outlineVariant = tint(0.16f),
            error = Color(0xFFE5484D), onError = Color.White, errorContainer = Color(0xFFFFE0E0), onErrorContainer = Color(0xFF5C0A0E),
        )
    } else {
        val night = Color(0xFF131218)
        fun tint(f: Float) = lerp(night, seed, f)
        darkColorScheme(
            primary = lerp(seed, Color.White, 0.3f), onPrimary = lerp(seed, Color.Black, 0.75f),
            primaryContainer = lerp(seed, night, 0.55f), onPrimaryContainer = lerp(seed, Color.White, 0.8f),
            secondary = lerp(seed, Color(0xFFC8C5D6), 0.7f), onSecondary = night,
            secondaryContainer = lerp(seed, night, 0.72f), onSecondaryContainer = lerp(seed, Color.White, 0.85f),
            tertiary = lerp(a.partner, Color.White, 0.3f), onTertiary = lerp(a.partner, Color.Black, 0.75f),
            tertiaryContainer = lerp(a.partner, night, 0.6f), onTertiaryContainer = lerp(a.partner, Color.White, 0.8f),
            background = night, onBackground = Paper, surface = night, onSurface = Paper,
            surfaceVariant = tint(0.12f), onSurfaceVariant = Color(0xFFB3B0C2),
            surfaceContainerLowest = tint(0.05f), surfaceContainerLow = tint(0.06f), surfaceContainer = tint(0.08f),
            surfaceContainerHigh = tint(0.11f), surfaceContainerHighest = tint(0.14f),
            outline = Color(0xFF8E8B9E), outlineVariant = tint(0.2f),
            error = Color(0xFFFF8A8F), onError = Color(0xFF4A0007), errorContainer = Color(0xFF5C1A1E), onErrorContainer = Color(0xFFFFDADB),
        )
    }
}

private fun ColorScheme.amoled() = copy(
    background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color(0xFF09090B),
    surfaceContainerLow = Color(0xFF0B0B0D), surfaceContainer = Color(0xFF111114), surfaceContainerHigh = Color(0xFF18181C),
    surfaceContainerHighest = Color(0xFF202025), surfaceVariant = Color(0xFF1C1C21),
)

@Immutable
data class ScanColors(
    val heroStart: Color, val heroEnd: Color, val onHero: Color,
    val success: Color, val warning: Color,

    val confetti: List<Color>,
    val isDark: Boolean,
)

val LocalScanColors = staticCompositionLocalOf {
    ScanColors(Color.Blue, Color.Cyan, Color.White, Color.Green, Color.Yellow, listOf(Color.Red), false)
}

private val fonts = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val BodyFont = FontFamily(Font(GoogleFont("Sniglet"), fonts))

private val base = Typography()
private val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = BodyFont),
    displayMedium = base.displayMedium.copy(fontFamily = BodyFont),
    displaySmall = base.displaySmall.copy(fontFamily = BodyFont),
    headlineLarge = base.headlineLarge.copy(fontFamily = BodyFont),
    headlineMedium = base.headlineMedium.copy(fontFamily = BodyFont),
    headlineSmall = base.headlineSmall.copy(fontFamily = BodyFont),
    titleLarge = base.titleLarge.copy(fontFamily = BodyFont),
    titleMedium = base.titleMedium.copy(fontFamily = BodyFont),
    titleSmall = base.titleSmall.copy(fontFamily = BodyFont),
    bodyLarge = base.bodyLarge.copy(fontFamily = BodyFont),
    bodyMedium = base.bodyMedium.copy(fontFamily = BodyFont),
    bodySmall = base.bodySmall.copy(fontFamily = BodyFont),
    labelLarge = base.labelLarge.copy(fontFamily = BodyFont),
    labelMedium = base.labelMedium.copy(fontFamily = BodyFont),
    labelSmall = base.labelSmall.copy(fontFamily = BodyFont),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(32.dp),
)

val CardShape = RoundedCornerShape(28.dp)
val ThumbShape = RoundedCornerShape(14.dp)

val dynamicColorSupported get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun isDark(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun ScanTheme(mode: ThemeMode, dynamic: Boolean, amoled: Boolean, accent: Int, content: @Composable () -> Unit) {
    val dark = isDark(mode)
    val a = ACCENTS[accent.mod(ACCENTS.size)]
    val wallpaper = dynamic && dynamicColorSupported
    val ctx = LocalContext.current
    var scheme = if (wallpaper) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        schemeFor(a, dark)
    }
    if (dark && amoled) scheme = scheme.amoled()
    val heroStart = if (wallpaper) (if (dark) scheme.primaryContainer else scheme.primary) else a.primary
    val heroEnd = if (wallpaper) (if (dark) scheme.tertiaryContainer else scheme.tertiary) else a.partner
    val onHero = if ((heroStart.luminance() + heroEnd.luminance()) / 2 > 0.28f) Ink else Color.White
    val colors = ScanColors(
        heroStart = heroStart, heroEnd = heroEnd, onHero = onHero,
        success = if (dark) Color(0xFF4ADE80) else Color(0xFF16A34A),
        warning = if (dark) Color(0xFFFFC14D) else Color(0xFFD97706),
        confetti = listOf(Color(0xFFFFC93C), Color(0xFF2ED573), Color(0xFFF15BB5), Color(0xFF3D9BFF)),
        isDark = dark,
    )
    CompositionLocalProvider(LocalScanColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
