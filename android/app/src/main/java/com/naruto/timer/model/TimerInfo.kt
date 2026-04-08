package com.naruto.timer.model

data class TimerInfo(
    val id: Long = System.currentTimeMillis(),
    val position: Position,
    var remainingSeconds: Double = 13.5,
    val maxSeconds: Double = 13.5,
    var isRunning: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class Position {
        LEFT,
        RIGHT
    }

    fun tick(intervalMs: Long) {
        if (isRunning) {
            remainingSeconds -= intervalMs / 1000.0
            if (remainingSeconds <= 0) {
                remainingSeconds = 0.0
                isRunning = false
            }
        }
    }

    fun reset() {
        remainingSeconds = maxSeconds
        isRunning = true
    }
}
