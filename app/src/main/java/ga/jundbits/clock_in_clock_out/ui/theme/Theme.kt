package ga.jundbits.clock_in_clock_out.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import ga.jundbits.clock_in_clock_out.R

@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val inColor: Color,
    val onIn: Color,
    val inContainer: Color,
    val onInContainer: Color,
    val inCaption: Color,
    val outColor: Color,
    val onOut: Color,
    val outContainer: Color,
    val onOutContainer: Color,
    val outCaption: Color,
    val danger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val avatar: Color,
    val onAvatar: Color,
    val ink: Color,
    val onInk: Color,
    val camera: Color,
    val cameraChip: Color,
    val onCameraChip: Color,
    val cameraFrame: Color,
    val isDark: Boolean,
)

/** Cobalt ID-badge look. */
private val LightColors = AppColors(
    background = Color(0xFFF4F6FB),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFE3E7F0),
    text = Color(0xFF111827),
    muted = Color(0xFF5B6478),
    inColor = Color(0xFF1D4ED8),
    onIn = Color(0xFFFFFFFF),
    inContainer = Color(0xFFE8EEFD),
    onInContainer = Color(0xFF1D4ED8),
    inCaption = Color(0xFF1E3A8A),
    outColor = Color(0xFFC2410C),
    onOut = Color(0xFFFFFFFF),
    outContainer = Color(0xFFFDEDE3),
    onOutContainer = Color(0xFFC2410C),
    outCaption = Color(0xFF9A3412),
    danger = Color(0xFFB91C1C),
    dangerContainer = Color(0xFFFDECEC),
    onDangerContainer = Color(0xFF991B1B),
    avatar = Color(0xFFDCE5FB),
    onAvatar = Color(0xFF1D4ED8),
    ink = Color(0xFF111827),
    onInk = Color(0xFFFFFFFF),
    camera = Color(0xFF1F2430),
    cameraChip = Color(0xFF2C3240),
    onCameraChip = Color(0xFFD5DAE5),
    cameraFrame = Color(0xFFFFFFFF),
    isDark = false,
)

/** Night-shift colors: mint for in, amber for out. */
private val DarkColors = AppColors(
    background = Color(0xFF0E1116),
    surface = Color(0xFF171B22),
    border = Color(0xFF262C36),
    text = Color(0xFFE8ECF2),
    muted = Color(0xFF9AA3B2),
    inColor = Color(0xFF3DDC97),
    onIn = Color(0xFF062417),
    inContainer = Color(0xFF1F3A2F),
    onInContainer = Color(0xFF3DDC97),
    inCaption = Color(0xFFA7F3D0),
    outColor = Color(0xFFFFB547),
    onOut = Color(0xFF2A1A00),
    outContainer = Color(0xFF3A2A10),
    onOutContainer = Color(0xFFFFB547),
    outCaption = Color(0xFFFFD08A),
    danger = Color(0xFFF87171),
    dangerContainer = Color(0xFF3B1518),
    onDangerContainer = Color(0xFFFCA5A5),
    avatar = Color(0xFF1F3A2F),
    onAvatar = Color(0xFF3DDC97),
    ink = Color(0xFFE8ECF2),
    onInk = Color(0xFF0E1116),
    camera = Color(0xFF07090C),
    cameraChip = Color(0xFF171B22),
    onCameraChip = Color(0xFFC9D0DB),
    cameraFrame = Color(0xFFE8ECF2),
    isDark = true,
)

private val LocalAppColors = staticCompositionLocalOf { LightColors }

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

private fun TextStyle.manrope() = copy(fontFamily = Manrope)

private val AppTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.manrope(),
        displayMedium = t.displayMedium.manrope(),
        displaySmall = t.displaySmall.manrope(),
        headlineLarge = t.headlineLarge.manrope(),
        headlineMedium = t.headlineMedium.manrope(),
        headlineSmall = t.headlineSmall.manrope(),
        titleLarge = t.titleLarge.manrope(),
        titleMedium = t.titleMedium.manrope(),
        titleSmall = t.titleSmall.manrope(),
        bodyLarge = t.bodyLarge.manrope(),
        bodyMedium = t.bodyMedium.manrope(),
        bodySmall = t.bodySmall.manrope(),
        labelLarge = t.labelLarge.manrope(),
        labelMedium = t.labelMedium.manrope(),
        labelSmall = t.labelSmall.manrope(),
    )
}

@Composable
fun ClockTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.inColor, onPrimary = colors.onIn,
            primaryContainer = colors.inContainer, onPrimaryContainer = colors.onInContainer,
            secondary = colors.outColor, onSecondary = colors.onOut,
            background = colors.background, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.background, onSurfaceVariant = colors.muted,
            surfaceContainerLowest = colors.surface, surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface,
            surfaceContainerHighest = colors.surface,
            outline = colors.border, outlineVariant = colors.border,
            error = colors.danger, errorContainer = colors.dangerContainer, onErrorContainer = colors.onDangerContainer,
            inverseSurface = colors.text, inverseOnSurface = colors.background, inversePrimary = colors.inColor,
        )
    } else {
        lightColorScheme(
            primary = colors.inColor, onPrimary = colors.onIn,
            primaryContainer = colors.inContainer, onPrimaryContainer = colors.onInContainer,
            secondary = colors.outColor, onSecondary = colors.onOut,
            background = colors.background, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.background, onSurfaceVariant = colors.muted,
            surfaceContainerLowest = colors.surface, surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface,
            surfaceContainerHighest = colors.surface,
            outline = colors.border, outlineVariant = colors.border,
            error = colors.danger, errorContainer = colors.dangerContainer, onErrorContainer = colors.onDangerContainer,
            inverseSurface = colors.text, inverseOnSurface = colors.background, inversePrimary = Color(0xFF93B4FF),
        )
    }
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
