package com.naruto.timer.model

data class GameConfig(
    val countdownSeconds: Double = 13.5,
    val rgbTolerance: Int = 4,
    val detectionIntervalMs: Long = 100,

    val leftBeansArena: List<Pair<Int, Int>>,
    val rightBeansArena: List<Pair<Int, Int>>,

    val leftBeansTraining: List<Pair<Int, Int>>,
    val rightBeansTraining: List<Pair<Int, Int>>,

    val leftHashiramaPoints: List<Pair<Int, Int>>,
    val rightHashiramaPoints: List<Pair<Int, Int>>,

    val arenaDetectionPoint: Pair<Int, Int>,
    val trainingDetectionPoint: Pair<Int, Int>,

    val darkCyanRange: RGBRange,
    val brightBlueRange: RGBRange,
    val redGoldRange: RGBRange,
    val hashiramaRGBRange: RGBRange
) {
    data class RGBRange(
        val min: Triple<Int, Int, Int>,
        val max: Triple<Int, Int, Int>
    )

    companion object {
        fun createDefault1920x1080(): GameConfig {
            return GameConfig(
                leftBeansArena = listOf(
                    206 to 110, 237 to 110, 267 to 110, 297 to 110, 328 to 110, 358 to 110
                ),
                rightBeansArena = listOf(
                    1708 to 110, 1677 to 110, 1646 to 110, 1616 to 110, 1586 to 110, 1555 to 110
                ),
                leftBeansTraining = listOf(
                    187 to 105, 217 to 105, 247 to 105, 278 to 105, 308 to 105, 338 to 105
                ),
                rightBeansTraining = listOf(
                    1669 to 108, 1639 to 108, 1608 to 108, 1577 to 108, 1547 to 108, 1516 to 108
                ),
                leftHashiramaPoints = listOf(
                    120 to 20, 120 to 30, 130 to 20, 130 to 20
                ),
                rightHashiramaPoints = listOf(
                    1785 to 15, 1785 to 25, 1795 to 15, 1795 to 25
                ),
                arenaDetectionPoint = 65 to 445,
                trainingDetectionPoint = 950 to 970,
                darkCyanRange = RGBRange(
                    Triple(13, 3, 1),
                    Triple(60, 55, 103)
                ),
                brightBlueRange = RGBRange(
                    Triple(0, 120, 170),
                    Triple(225, 255, 255)
                ),
                redGoldRange = RGBRange(
                    Triple(179, 19, 1),
                    Triple(255, 255, 206)
                ),
                hashiramaRGBRange = RGBRange(
                    Triple(49, 49, 31),
                    Triple(50, 50, 33)
                )
            )
        }
    }
}
