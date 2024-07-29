package dev.brahmkshatriya.echo.utils

import android.os.CountDownTimer

abstract class PauseCountDown(
    private var millisInFuture: Long, private var interval: Long = 1000
) {
    private lateinit var countDownTimer: CountDownTimer
    private var remainingTime: Long = 0
    private var isTimerPaused: Boolean = true

    init {
        this.remainingTime = millisInFuture
    }

    @Synchronized
    fun start() {
        if (isTimerPaused) {
            countDownTimer = object : CountDownTimer(remainingTime, interval) {
                override fun onFinish() {
                    onTimerFinish()
                    reset()
                }

                override fun onTick(millisUntilFinished: Long) {
                    remainingTime = millisUntilFinished
                    onTimerTick(millisUntilFinished)
                }

            }.apply { start() }
            isTimerPaused = false
        }
    }

    fun pause() {
        if (!isTimerPaused) countDownTimer.cancel()
        isTimerPaused = true
    }

    fun reset() {
        countDownTimer.cancel()
        remainingTime = millisInFuture
        isTimerPaused = true
    }

    abstract fun onTimerTick(millisUntilFinished: Long)
    abstract fun onTimerFinish()

}