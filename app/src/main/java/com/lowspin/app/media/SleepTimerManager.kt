package com.lowspin.app.media

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SleepTimerManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("lowspin_sleep_timer", Context.MODE_PRIVATE)

    private val _endsAt = MutableStateFlow(prefs.getLong(KEY_ENDS_AT, 0L))
    val endsAt: StateFlow<Long> = _endsAt.asStateFlow()

    private val _mode = MutableStateFlow(prefs.getString(KEY_MODE, "off") ?: "off")
    val mode: StateFlow<String> = _mode.asStateFlow()

    val isActive: Boolean get() = _endsAt.value > 0L || _mode.value == "track"

    fun startMinutes(minutes: Int) {
        _mode.value = "time"
        _endsAt.value = System.currentTimeMillis() + minutes * 60_000L
        persist()
    }

    fun startEndOfTrack() {
        _mode.value = "track"
        _endsAt.value = 0L
        persist()
    }

    fun cancel() {
        _mode.value = "off"
        _endsAt.value = 0L
        persist()
    }

    /** Returns true when the timer was set for end-of-track and fires on auto-advance. */
    fun consumeTrackEnd(): Boolean {
        if (_mode.value != "track" || _endsAt.value != 0L) return false
        cancel()
        return true
    }

    private fun persist() {
        prefs.edit()
            .putLong(KEY_ENDS_AT, _endsAt.value)
            .putString(KEY_MODE, _mode.value)
            .apply()
    }

    private companion object {
        const val KEY_ENDS_AT = "ends_at"
        const val KEY_MODE = "mode"
    }
}
