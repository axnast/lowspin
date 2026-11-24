package com.lowspin.app.media

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val EqualizerPresets = listOf("Flat", "Bass", "Vocals", "Acoustic", "Rock")

class EqualizerSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("lowspin_equalizer", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _preset = MutableStateFlow(prefs.getInt(KEY_PRESET, 0))
    val preset: StateFlow<Int> = _preset.asStateFlow()

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun setPreset(value: Int) {
        val safe = value.coerceIn(0, EqualizerPresets.lastIndex)
        _preset.value = safe
        prefs.edit().putInt(KEY_PRESET, safe).apply()
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_PRESET = "preset"
    }
}
