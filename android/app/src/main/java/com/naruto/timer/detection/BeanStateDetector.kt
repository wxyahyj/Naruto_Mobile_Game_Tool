package com.naruto.timer.detection

import com.naruto.timer.model.BeanState
import com.naruto.timer.model.GameConfig

class BeanStateDetector(
    private val pixelDetector: PixelDetector,
    private val config: GameConfig
) {
    private val leftBeanStates = MutableList(6) { BeanState.CHAOS }
    private val rightBeanStates = MutableList(6) { BeanState.CHAOS }

    var leftBeanCount: Int = 0
        private set
    var rightBeanCount: Int = 0
        private set

    private var lastLeftBeanCount: Int = 0
    private var lastRightBeanCount: Int = 0

    fun detectAllBeans(isArenaMode: Boolean = true) {
        val leftPoints = if (isArenaMode) config.leftBeansArena else config.leftBeansTraining
        val rightPoints = if (isArenaMode) config.rightBeansArena else config.rightBeansTraining

        leftPoints.forEachIndexed { index, point ->
            val rgb = pixelDetector.getPixelRGB(point.first, point.second)
            leftBeanStates[index] = BeanState.fromRGB(rgb, config.rgbTolerance)
        }

        rightPoints.forEachIndexed { index, point ->
            val rgb = pixelDetector.getPixelRGB(point.first, point.second)
            rightBeanStates[index] = BeanState.fromRGB(rgb, config.rgbTolerance)
        }

        lastLeftBeanCount = leftBeanCount
        lastRightBeanCount = rightBeanCount

        leftBeanCount = calculateBeanCount(leftBeanStates, isLeft = true)
        rightBeanCount = calculateBeanCount(rightBeanStates, isLeft = false)
    }

    private fun calculateBeanCount(states: List<BeanState>, isLeft: Boolean): Int {
        val isHashirama = checkHashirama(isLeft)

        return when {
            states.all { it == BeanState.DARK_CYAN } -> 0

            states[0] == BeanState.BRIGHT_BLUE &&
            states.subList(1, 4).all { it == BeanState.DARK_CYAN } -> 1

            states[0] == BeanState.BRIGHT_BLUE &&
            states[1] == BeanState.BRIGHT_BLUE &&
            states.subList(2, 4).all { it == BeanState.DARK_CYAN } -> 2

            states[0] == BeanState.BRIGHT_BLUE &&
            states[1] == BeanState.BRIGHT_BLUE &&
            states[2] == BeanState.BRIGHT_BLUE &&
            states[3] == BeanState.DARK_CYAN -> 3

            states.subList(0, 4).all { it == BeanState.BRIGHT_BLUE || it == BeanState.RED_GOLD } -> 4

            isHashirama && states.subList(0, 5).all { it == BeanState.RED_GOLD } -> {
                if (states[5] == BeanState.RED_GOLD) 6 else 5
            }

            states[0] == BeanState.RED_GOLD &&
            states.subList(1, 4).all { it == BeanState.DARK_CYAN } -> 1

            else -> if (isLeft) leftBeanCount else rightBeanCount
        }
    }

    private fun checkHashirama(isLeft: Boolean): Boolean {
        val points = if (isLeft) config.leftHashiramaPoints else config.rightHashiramaPoints

        for (point in points) {
            val rgb = pixelDetector.getPixelRGB(point.first, point.second)
            val inRange = pixelDetector.checkRGBInRange(
                rgb,
                config.hashiramaRGBRange.min,
                config.hashiramaRGBRange.max,
                config.rgbTolerance
            )
            if (inRange) return true
        }

        return false
    }

    fun checkBeanCountChanged(): Pair<Boolean, Boolean> {
        val leftChanged = leftBeanCount != lastLeftBeanCount
        val rightChanged = rightBeanCount != lastRightBeanCount
        return Pair(leftChanged, rightChanged)
    }

    fun getBeanStateDescription(): String {
        return "左侧: $leftBeanCount 豆 | 右侧: $rightBeanCount 豆"
    }
}
