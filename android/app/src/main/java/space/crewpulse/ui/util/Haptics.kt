package space.crewpulse.ui.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

@Composable
fun rememberHaptic(): (Int) -> Unit {
    val view = LocalView.current
    return { effect ->
        view.performHapticFeedback(effect)
    }
}

fun View.tick() = performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
fun View.confirm() = performHapticFeedback(HapticFeedbackConstants.CONFIRM)
fun View.reject() = performHapticFeedback(HapticFeedbackConstants.REJECT)