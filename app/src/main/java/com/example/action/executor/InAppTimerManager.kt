package com.example.action.executor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ActiveTimerState(
    val isRunning: Boolean = false,
    val initialSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val label: String = "Timer",
    val isCompleted: Boolean = false
) {
    val formattedRemaining: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
}

class InAppTimerManager(private val scope: CoroutineScope) {

    private val _timerState = MutableStateFlow(ActiveTimerState())
    val timerState: StateFlow<ActiveTimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    var onTimerFinished: ((label: String) -> Unit)? = null

    fun startTimer(seconds: Int, label: String = "Timer") {
        cancelTimer()
        if (seconds <= 0) return

        _timerState.value = ActiveTimerState(
            isRunning = true,
            initialSeconds = seconds,
            remainingSeconds = seconds,
            label = label
        )

        timerJob = scope.launch(Dispatchers.Default) {
            while (isActive && _timerState.value.remainingSeconds > 0) {
                delay(1000)
                val cur = _timerState.value.remainingSeconds - 1
                if (cur <= 0) {
                    _timerState.value = _timerState.value.copy(
                        isRunning = false,
                        remainingSeconds = 0,
                        isCompleted = true
                    )
                    onTimerFinished?.invoke(label)
                    break
                } else {
                    _timerState.value = _timerState.value.copy(remainingSeconds = cur)
                }
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = ActiveTimerState(isRunning = false, remainingSeconds = 0)
    }
}
