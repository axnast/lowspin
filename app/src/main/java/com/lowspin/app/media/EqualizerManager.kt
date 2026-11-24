package com.lowspin.app.media

import android.content.Context
import android.media.audiofx.Equalizer

/**
 * Wraps the platform Equalizer attached to ExoPlayer's audio session.
 * Device support varies; failures leave playback untouched.
 */
class EqualizerManager(
    private val settings: EqualizerSettings,
    @Suppress("UNUSED_PARAMETER") context: Context
) {
    private var equalizer: Equalizer? = null

    val available: Boolean get() = equalizer != null

    fun attach(audioSessionId: Int) {
        release()
        equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        apply(settings.enabled.value, settings.preset.value)
    }

    fun apply(enabled: Boolean, presetIndex: Int) {
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = enabled
            if (enabled) {
                val count = eq.numberOfPresets.toInt()
                if (count > 0) {
                    eq.usePreset((presetIndex % count).toShort())
                }
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
    }
}
