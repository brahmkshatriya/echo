package dev.brahmkshatriya.echo.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PauseTimer(
    private val scope: CoroutineScope,
    private var millisInFuture: Long,
    private val onTimerFinish: () -> Unit,
) {
    private var job: Job? = null
    private var lastPauseTime: Long = System.currentTimeMillis()
    private var isTimerPaused: Boolean = true

    fun resume() {
        if (isTimerPaused) {
            val diff = System.currentTimeMillis() - lastPauseTime
            val remainingTime =  millisInFuture - diff
            if (remainingTime < 0) return
            job = scope.launch {
                delay(remainingTime)
                onTimerFinish()
            }
            isTimerPaused = false
        }
    }

    fun pause() {
        job?.cancel()
        lastPauseTime = System.currentTimeMillis()
        isTimerPaused = true
    }

    init {
        resume()
    }
}