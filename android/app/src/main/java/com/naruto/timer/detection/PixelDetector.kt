package com.naruto.timer.detection

import android.graphics.Bitmap
import android.graphics.Color

class PixelDetector(private val bitmap: Bitmap) {

    fun getPixelRGB(x: Int, y: Int): Triple<Int, Int, Int> {
        if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) {
            return Triple(0, 0, 0)
        }

        val pixel = bitmap.getPixel(x, y)
        return Triple(
            Color.red(pixel),
            Color.green(pixel),
            Color.blue(pixel)
        )
    }

    fun checkRGBInRange(
        target: Triple<Int, Int, Int>,
        minRange: Triple<Int, Int, Int>,
        maxRange: Triple<Int, Int, Int>,
        tolerance: Int = 4
    ): Boolean {
        val (r, g, b) = target
        val (minR, minG, minB) = minRange
        val (maxR, maxG, maxB) = maxRange

        return r in (minR - tolerance)..(maxR + tolerance) &&
               g in (minG - tolerance)..(maxG + tolerance) &&
               b in (minB - tolerance)..(maxB + tolerance)
    }

    fun getMultiplePixelsRGB(points: List<Pair<Int, Int>>): List<Triple<Int, Int, Int>> {
        return points.map { (x, y) -> getPixelRGB(x, y) }
    }

    fun recycle() {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
