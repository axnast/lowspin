package com.lowspin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lowspin.app.ui.settings.LocalAppearance
import com.lowspin.app.ui.settings.LowspinAccentShades

@Composable
fun LowspinTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val appearance = LocalAppearance.current
    val accentIndex by appearance.accentIndex.collectAsStateWithLifecycle()
    val highContrast by appearance.highContrast.collectAsStateWithLifecycle()

    val colorScheme = remember(accentIndex, highContrast) {
        val accent = LowspinAccentShades[accentIndex.coerceIn(0, LowspinAccentShades.lastIndex)]
        darkColorScheme(
            primary = accent,
            onPrimary = LowspinOnAccent,
            primaryContainer = LowspinAccentPressed,
            onPrimaryContainer = LowspinTextPrimary,
            secondary = LowspinAccentPressed,
            onSecondary = LowspinOnAccent,
            background = LowspinBackground,
            onBackground = LowspinTextPrimary,
            surface = LowspinSurface,
            onSurface = LowspinTextPrimary,
            surfaceVariant = LowspinSurfaceHigh,
            onSurfaceVariant = if (highContrast) {
                Color(0xFFD6D6DF)
            } else {
                LowspinTextSecondary
            },
            surfaceContainer = LowspinSurface,
            surfaceContainerHigh = LowspinSurfaceHigh,
            surfaceContainerHighest = LowspinSurfaceHigh,
            error = LowspinError,
            onError = LowspinBackground,
            outline = LowspinSurfaceHigh,
            outlineVariant = LowspinSurfaceHigh
        )
    }

    val fontFamily = remember(context) { lowspinFontFamily(context) }
    val typography = remember(fontFamily) { lowspinTypography(fontFamily) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
