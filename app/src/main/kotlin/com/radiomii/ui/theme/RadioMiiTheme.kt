package com.radiomii.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.radiomii.domain.model.AppSettings
import com.radiomii.domain.model.ThemeColor
import com.radiomii.domain.model.ThemeMode

@Composable
fun RadioMiiTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val isDark = isEffectiveDarkTheme(settings.themeMode)

    val colorScheme = when (settings.themeColor) {
        ThemeColor.DYNAMIC -> {
            val base = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) miiDarkScheme else miiLightScheme
            }
            if (settings.trueBlack) applyTrueBlack(base, isDark) else base
        }
        ThemeColor.CUSTOM -> customColorScheme(
            seedIndex = settings.customSourceColorIndex,
            isDark = isDark,
            trueBlack = settings.trueBlack,
        )
        ThemeColor.MII -> {
            val base = if (isDark) miiDarkScheme else miiLightScheme
            if (settings.trueBlack) applyTrueBlack(base, isDark) else base
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RadioMiiTypography,
        content = content,
    )
}

/** Returns true when the effective theme is dark, taking the system setting into account. */
@Composable
fun isEffectiveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.LIGHT  -> false
    ThemeMode.DARK   -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}
