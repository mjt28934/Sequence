package com.sequence.watchorder.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * One switch for every haptic tick, mirroring [Motion].
 *
 * The Settings screen flips it; [com.sequence.watchorder.SequenceApp] keeps it in sync with the
 * saved preference. Ticks are only ever fired from a user gesture, so nothing runs while idle.
 */
object Haptics {
    var enabled: Boolean = true

    fun tick(feedback: HapticFeedback) {
        if (!enabled) return
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
