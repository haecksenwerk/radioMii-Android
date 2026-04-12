package com.radiomii.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.hct.Hct
import com.materialkolor.dynamiccolor.MaterialDynamicColors

// ─── mii orange palette ─────────────────────────────────────────────────────

val miiLightScheme = lightColorScheme(
    primary = Color(0xFFFA7D18),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCA),
    onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF765849),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC3),
    onSecondaryContainer = Color(0xFF2C160B),
    tertiary = Color(0xFF645F30),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEBE3AA),
    onTertiaryContainer = Color(0xFF1F1C00),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF4DED5),
    onSurfaceVariant = Color(0xFF52443D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF0EB),
    surfaceContainer = Color(0xFFF9EBE5),
    surfaceContainerHigh = Color(0xFFF7E5DE),
    surfaceContainerHighest = Color(0xFFF5DFD8),
    outline = Color(0xFF85736C),
    outlineVariant = Color(0xFFD7C2B9),
    inverseSurface = Color(0xFF362F2C),
    inverseOnSurface = Color(0xFFFBEEE9),
    inversePrimary = Color(0xFFFFB78A),
)

val miiDarkScheme = darkColorScheme(
    primary = Color(0xFFFA7D18),
    onPrimary = Color(0xFF542100),
    primaryContainer = Color(0xFF783200),
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = Color(0xFFE6BEAC),
    onSecondary = Color(0xFF432A1E),
    secondaryContainer = Color(0xFF5C4033),
    onSecondaryContainer = Color(0xFFFFDCC3),
    tertiary = Color(0xFFCEC790),
    onTertiary = Color(0xFF353106),
    tertiaryContainer = Color(0xFF4C471B),
    onTertiaryContainer = Color(0xFFEBE3AA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF201A18),
    onBackground = Color(0xFFEDE0DB),
    surface = Color(0xFF201A18),
    onSurface = Color(0xFFEDE0DB),
    surfaceVariant = Color(0xFF52443D),
    onSurfaceVariant = Color(0xFFD7C2B9),
    surfaceContainerLowest = Color(0xFF0F0D0C),
    surfaceContainerLow = Color(0xFF2B221E),
    surfaceContainer = Color(0xFF312722),
    surfaceContainerHigh = Color(0xFF382B26),
    surfaceContainerHighest = Color(0xFF3F3029),
    outline = Color(0xFF9F8D85),
    outlineVariant = Color(0xFF52443D),
    inverseSurface = Color(0xFFEDE0DB),
    inverseOnSurface = Color(0xFF201A18),
    inversePrimary = Color(0xFFFA7D18),
)

// ─── custom 10-color seed palette ────────────────────────────────────────────
// Light seeds (primary color visible in light theme)
private val CUSTOM_LIGHT_SEEDS = listOf(
    Color(0xFFc75b7a), // Rose
    Color(0xFFd17b2a), // Orange
    Color(0xFF8a8520), // Olive
    Color(0xFF4e944f), // Green
    Color(0xFF179299), // Teal      ← default (index 4)
    Color(0xFF04a5e5), // Sky
    Color(0xFF209fb5), // Sapphire
    Color(0xFF2196f3), // Blue
    Color(0xFF7287fd), // Lavender
    Color(0xFF8839ef), // Mauve
)

// Exposed for the settings color swatch UI (same order as light seeds)
val CUSTOM_SWATCH_COLORS: List<Color> = CUSTOM_LIGHT_SEEDS

// Dark seeds
private val CUSTOM_DARK_SEEDS = listOf(
    Color(0xFFf8bbd9), // Pink
    Color(0xFFffb386), // Peach
    Color(0xFFffd54f), // Amber
    Color(0xFFa5d6a7), // Green
    Color(0xFF94e2d5), // Teal       ← default (index 4)
    Color(0xFF89dceb), // Sky
    Color(0xFF74c7ec), // Sapphire
    Color(0xFF89b4fa), // Blue
    Color(0xFFb4befe), // Lavender
    Color(0xFFcba6f7), // Mauve
)

// Cache key includes a version so stale entries from older builds are never returned.
private const val SCHEME_CACHE_VERSION = 6
private val schemeCache = HashMap<Triple<Int, Boolean, Boolean>, Pair<Int, ColorScheme>>(40)

fun customColorScheme(seedIndex: Int, isDark: Boolean, trueBlack: Boolean): ColorScheme {
    val cacheKey = Triple(seedIndex, isDark, trueBlack)
    schemeCache[cacheKey]?.takeIf { it.first == SCHEME_CACHE_VERSION }?.let { return it.second }

    val seeds = if (isDark) CUSTOM_DARK_SEEDS else CUSTOM_LIGHT_SEEDS
    val seed = seeds.getOrElse(seedIndex) { seeds[4] }

    val hct = Hct.fromInt(seed.toArgb())
    val scheme = SchemeTonalSpot(hct, isDark, 0.0)
    val mdColors = MaterialDynamicColors()

    val colorScheme = ColorScheme(
        primary = Color(mdColors.primary().getArgb(scheme)),
        onPrimary = Color(mdColors.onPrimary().getArgb(scheme)),
        primaryContainer = Color(mdColors.primaryContainer().getArgb(scheme)),
        onPrimaryContainer = Color(mdColors.onPrimaryContainer().getArgb(scheme)),
        inversePrimary = Color(mdColors.inversePrimary().getArgb(scheme)),
        secondary = Color(mdColors.secondary().getArgb(scheme)),
        onSecondary = Color(mdColors.onSecondary().getArgb(scheme)),
        secondaryContainer = Color(mdColors.secondaryContainer().getArgb(scheme)),
        onSecondaryContainer = Color(mdColors.onSecondaryContainer().getArgb(scheme)),
        tertiary = Color(mdColors.tertiary().getArgb(scheme)),
        onTertiary = Color(mdColors.onTertiary().getArgb(scheme)),
        tertiaryContainer = Color(mdColors.tertiaryContainer().getArgb(scheme)),
        onTertiaryContainer = Color(mdColors.onTertiaryContainer().getArgb(scheme)),
        background = Color(mdColors.background().getArgb(scheme)),
        onBackground = Color(mdColors.onBackground().getArgb(scheme)),
        surface = Color(mdColors.surface().getArgb(scheme)),
        onSurface = Color(mdColors.onSurface().getArgb(scheme)),
        surfaceVariant = Color(mdColors.surfaceVariant().getArgb(scheme)),
        onSurfaceVariant = Color(mdColors.onSurfaceVariant().getArgb(scheme)),
        surfaceTint = Color(mdColors.surfaceTint().getArgb(scheme)),
        inverseSurface = Color(mdColors.inverseSurface().getArgb(scheme)),
        inverseOnSurface = Color(mdColors.inverseOnSurface().getArgb(scheme)),
        error = Color(mdColors.error().getArgb(scheme)),
        onError = Color(mdColors.onError().getArgb(scheme)),
        errorContainer = Color(mdColors.errorContainer().getArgb(scheme)),
        onErrorContainer = Color(mdColors.onErrorContainer().getArgb(scheme)),
        outline = Color(mdColors.outline().getArgb(scheme)),
        outlineVariant = Color(mdColors.outlineVariant().getArgb(scheme)),
        scrim = Color(mdColors.scrim().getArgb(scheme)),
        surfaceBright = Color(mdColors.surfaceBright().getArgb(scheme)),
        surfaceContainer = Color(mdColors.surfaceContainer().getArgb(scheme)),
        surfaceContainerHigh = Color(mdColors.surfaceContainerHigh().getArgb(scheme)),
        surfaceContainerHighest = Color(mdColors.surfaceContainerHighest().getArgb(scheme)),
        surfaceContainerLow = Color(mdColors.surfaceContainerLow().getArgb(scheme)),
        surfaceContainerLowest = Color(mdColors.surfaceContainerLowest().getArgb(scheme)),
        surfaceDim = Color(mdColors.surfaceDim().getArgb(scheme)),
    )

    val result = if (trueBlack) applyTrueBlack(colorScheme, isDark) else colorScheme
    schemeCache[cacheKey] = Pair(SCHEME_CACHE_VERSION, result)
    return result
}

fun applyTrueBlack(base: ColorScheme, isDark: Boolean): ColorScheme = if (!isDark) base else base.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color(0xFF080808),
    surfaceContainerLow = Color(0xFF0F0F0F),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF1F1F1F),
)


