package com.lowspin.app.ui.settings

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The three calm violet shades offered in Settings. */
val LowspinAccentShades = listOf(
    Color(0xFFA78BFA),
    Color(0xFFB39DFF),
    Color(0xFF8E7BEA)
)

/** Plain values provided once at the root so list rows never collect state. */
data class LowspinListStyle(
    val rowHeight: Dp = 64.dp,
    val showArt: Boolean = true,
    val largeTouchTargets: Boolean = false
)

val LocalAppearance = staticCompositionLocalOf<AppearanceSettings> {
    error("AppearanceSettings has not been provided")
}

val LocalListStyle = staticCompositionLocalOf { LowspinListStyle() }

/**
 * Appearance and accessibility preferences. Stored locally, no permissions.
 */
class AppearanceSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("lowspin_appearance", Context.MODE_PRIVATE)

    private val _accentIndex = MutableStateFlow(prefs.getInt(KEY_ACCENT, 0))
    val accentIndex: StateFlow<Int> = _accentIndex.asStateFlow()

    private val _textScale = MutableStateFlow(prefs.getFloat(KEY_TEXT_SCALE, 1f))
    val textScale: StateFlow<Float> = _textScale.asStateFlow()

    private val _compact = MutableStateFlow(prefs.getBoolean(KEY_COMPACT, false))
    val compact: StateFlow<Boolean> = _compact.asStateFlow()

    private val _showArt = MutableStateFlow(prefs.getBoolean(KEY_ART, true))
    val showArt: StateFlow<Boolean> = _showArt.asStateFlow()

    private val _reduceMotion = MutableStateFlow(prefs.getBoolean(KEY_MOTION, false))
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    private val _highContrast = MutableStateFlow(prefs.getBoolean(KEY_CONTRAST, false))
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()

    private val _largeTargets = MutableStateFlow(prefs.getBoolean(KEY_TARGETS, false))
    val largeTargets: StateFlow<Boolean> = _largeTargets.asStateFlow()

    fun setAccent(index: Int) = store { putInt(KEY_ACCENT, index.coerceIn(0, 2)) }
        .also { _accentIndex.value = index.coerceIn(0, 2) }

    fun setTextScale(scale: Float) = store { putFloat(KEY_TEXT_SCALE, scale) }
        .also { _textScale.value = scale }

    fun setCompact(value: Boolean) = store { putBoolean(KEY_COMPACT, value) }
        .also { _compact.value = value }

    fun setShowArt(value: Boolean) = store { putBoolean(KEY_ART, value) }
        .also { _showArt.value = value }

    fun setReduceMotion(value: Boolean) = store { putBoolean(KEY_MOTION, value) }
        .also { _reduceMotion.value = value }

    fun setHighContrast(value: Boolean) = store { putBoolean(KEY_CONTRAST, value) }
        .also { _highContrast.value = value }

    fun setLargeTargets(value: Boolean) = store { putBoolean(KEY_TARGETS, value) }
        .also { _largeTargets.value = value }

    private fun store(edit: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(edit).apply()
    }

    private companion object {
        const val KEY_ACCENT = "accent"
        const val KEY_TEXT_SCALE = "text_scale"
        const val KEY_COMPACT = "compact"
        const val KEY_ART = "show_art"
        const val KEY_MOTION = "reduce_motion"
        const val KEY_CONTRAST = "high_contrast"
        const val KEY_TARGETS = "large_targets"
    }
}
