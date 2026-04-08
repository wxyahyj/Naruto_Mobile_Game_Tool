package com.naruto.timer.detection

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.naruto.timer.model.GameConfig
import com.naruto.timer.model.TimerInfo

class DetectionEngine(
    private val config: GameConfig,
    private val onTimerTriggered: (TimerInfo.Position) -> Unit,
    private val onBeanCountUpdated: (leftCount: Int, rightCount: Int) -> Unit
) {
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private var pixelDetector: PixelDetector? = null
    private var beanStateDetector: BeanStateDetector? = null

    private var isArenaMode = true

    private val detectionRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                performDetection()
                handler.postDelayed(this, config.detectionIntervalMs)
            }
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        handler.post(detectionRunnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(detectionRunnable)
        pixelDetector?.recycle()
    }

    fun processNewFrame(bitmap: Bitmap) {
        pixelDetector?.recycle()
        pixelDetector = PixelDetector(bitmap)
        beanStateDetector = BeanStateDetector(pixelDetector!!, config)
    }

    private fun performDetection() {
        val detector = beanStateDetector ?: return

        detector.detectAllBeans(isArenaMode)

        val (leftChanged, rightChanged) = detector.checkBeanCountChanged()

        if (leftChanged && detector.leftBeanCount < detector.lastLeftBeanCount) {
            onTimerTriggered(TimerInfo.Position.LEFT)
        }
        if (rightChanged && detector.rightBeanCount < detector.lastRightBeanCount) {
            onTimerTriggered(TimerInfo.Position.RIGHT)
        }

        onBeanCountUpdated(detector.leftBeanCount, detector.rightBeanCount)
    }

    fun setArenaMode(isArena: Boolean) {
        isArenaMode = isArena
    }
}
