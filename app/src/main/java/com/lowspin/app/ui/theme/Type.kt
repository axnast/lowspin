package com.lowspin.app.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private data class AssetFont(val path: String, val weight: FontWeight)

private val assetFonts = listOf(
    AssetFont("fonts/PlusJakartaSans-Regular.ttf", FontWeight.Normal),
    AssetFont("fonts/PlusJakartaSans-Medium.ttf", FontWeight.Medium),
    AssetFont("fonts/PlusJakartaSans-SemiBold.ttf", FontWeight.SemiBold),
    AssetFont("fonts/PlusJakartaSans-Bold.ttf", FontWeight.Bold),
    AssetFont("fonts/PlusJakartaSans-ExtraBold.ttf", FontWeight.ExtraBold)
)

/**
 * Loads the bundled Plus Jakarta Sans files from assets at runtime. If the
 * .ttf files are absent, the app compiles and runs with the platform sans
 * family instead of failing the build over a cosmetic detail.
 */
fun lowspinFontFamily(context: Context): FontFamily {
    val assets = context.assets
    val available = runCatching { assets.list("fonts")?.toSet() ?: emptySet() }
        .getOrDefault(emptySet())

    val fonts = assetFonts.mapNotNull { entry ->
        val fileName = entry.path.substringAfterLast('/')
        if (fileName !in available) return@mapNotNull null
        runCatching { Font(entry.path, assets, entry.weight) }.getOrNull()
    }

    return if (fonts.isEmpty()) FontFamily.SansSerif else FontFamily(fonts)
}

fun lowspinTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.ExtraBold
        ),
        displayMedium = base.displayMedium.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.ExtraBold
        ),
        displaySmall = base.displaySmall.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.ExtraBold
        ),
        headlineLarge = base.headlineLarge.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp
        ),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold
        ),
        titleMedium = base.titleMedium.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold
        ),
        titleSmall = base.titleSmall.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold
        ),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold
        ),
        labelMedium = base.labelMedium.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium
        ),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily)
    )
}
