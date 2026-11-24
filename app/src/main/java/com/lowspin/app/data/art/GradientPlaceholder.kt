package com.lowspin.app.data.art

import android.graphics.Color as AndroidColor
import kotlin.math.abs

/**
 * Deterministic offline cover art used when a file has no embedded picture.
 * Lowspin keeps every placeholder in the violet / indigo / magenta family so
 * the library stays calm and visually stable across restarts.
 */
object GradientPlaceholder {

    private fun seedValue(seed: String): Long =
        abs(seed.hashCode().toLong()) and 0x7FFFFFFFL

    /** Restricted to purple, indigo and magenta hues (260..316). */
    fun hue(seed: String): Float = 260f + (seedValue(seed) % 57L).toFloat()

    fun topColor(seed: String): Int =
        AndroidColor.HSVToColor(floatArrayOf(hue(seed), 0.42f, 0.32f))

    fun bottomColor(seed: String): Int =
        AndroidColor.HSVToColor(floatArrayOf((hue(seed) + 12f) % 360f, 0.50f, 0.10f))

    fun primaryColor(seed: String): Int =
        AndroidColor.HSVToColor(floatArrayOf(hue(seed), 0.48f, 0.34f))
}
