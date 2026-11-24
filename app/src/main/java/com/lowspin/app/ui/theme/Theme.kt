package com.lowspin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private val LowspinColorScheme = darkColorScheme(
    primary = LowspinAccent,
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
    onSurfaceVariant = LowspinTextSecondary,
    surfaceContainer = LowspinSurface,
    surfaceContainerHigh = LowspinSurfaceHigh,
    surfaceContainerHighest = LowspinSurfaceHigh,
    error = LowspinError,
    onError = LowspinBackground,
    outline = LowspinSurfaceHigh,
    outlineVariant = LowspinSurfaceHigh
)

@Composable
fun LowspinTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Font loading is deliberately unchanged: Plus Jakarta Sans with a
    // sans-serif fallback when the bundled assets are missing.
    val fontFamily = remember(context) { lowspinFontFamily(context) }
    val typography = remember(fontFamily) { lowspinTypography(fontFamily) }

    MaterialTheme(
        colorScheme = LowspinColorScheme,
        typography = typography,
        content = content
    )
}
