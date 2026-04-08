package com.naruto.timer.model

enum class BeanState {
    DARK_CYAN,
    BRIGHT_BLUE,
    RED_GOLD,
    CHAOS;

    companion object {
        fun fromRGB(rgb: Triple<Int, Int, Int>, tolerance: Int = 4): BeanState {
            val (r, g, b) = rgb

            if (r in (13 - tolerance)..(60 + tolerance) &&
                g in (3 - tolerance)..(55 + tolerance) &&
                b in (1 - tolerance)..(103 + tolerance)) {
                return DARK_CYAN
            }

            if (r in (0 - tolerance)..(225 + tolerance) &&
                g in (120 - tolerance)..(255 + tolerance) &&
                b in (170 - tolerance)..(255 + tolerance)) {
                return BRIGHT_BLUE
            }

            if (r in (179 - tolerance)..(255 + tolerance) &&
                g in (19 - tolerance)..(255 + tolerance) &&
                b in (1 - tolerance)..(206 + tolerance)) {
                return RED_GOLD
            }

            return CHAOS
        }
    }
}
