package com.lowspin.app.media

import androidx.media3.common.Player

/**
 * ExoPlayer's shuffle order starts a fresh permutation when it wraps. That can
 * put the track that just finished back at the front. This small guard skips
 * that single immediate repeat while leaving repeat-one and manual selections
 * untouched.
 */
class SmartShuffleManager {

    private var lastPlayedIndex = -1

    fun shouldSkipImmediateRepeat(
        nextIndex: Int,
        reason: Int,
        shuffleEnabled: Boolean,
        repeatMode: Int,
        queueSize: Int
    ): Boolean {
        if (!shuffleEnabled || queueSize <= 1) return false
        if (repeatMode == Player.REPEAT_MODE_ONE) return false
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) return false
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return false
        return nextIndex == lastPlayedIndex
    }

    fun onPlayed(index: Int) {
        lastPlayedIndex = index
    }
}
