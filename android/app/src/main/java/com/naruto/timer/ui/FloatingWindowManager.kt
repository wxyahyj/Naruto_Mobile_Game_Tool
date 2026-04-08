package com.naruto.timer.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.naruto.timer.model.TimerInfo

class FloatingWindowManager(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val mainContainer: FrameLayout
    private val beanCountView: TextView
    private val timerContainer: LinearLayout
    private val timerViews = mutableMapOf<Long, FloatingTimerView>()

    init {
        mainContainer = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        beanCountView = TextView(context).apply {
            textSize = 18f
            setTextColor(Color.parseColor("#EC137A"))
            setBackgroundColor(Color.WHITE)
            setPadding(16, 8, 16, 8)
            text = "左: 0 豆 | 右: 0 豆"
        }

        timerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 50, 0, 0)
        }

        mainContainer.addView(beanCountView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ))

        mainContainer.addView(timerContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        addToWindow()
    }

    private fun addToWindow() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager.addView(mainContainer, params)
    }

    fun updateBeanCount(leftCount: Int, rightCount: Int) {
        beanCountView.text = "左: $leftCount 豆 | 右: $rightCount 豆"
    }

    fun addTimerView(timer: TimerInfo) {
        val view = FloatingTimerView(context)
        view.updateDisplay(timer)
        timerContainer.addView(view)
        timerViews[timer.id] = view
    }

    fun updateTimerDisplay(timer: TimerInfo) {
        timerViews[timer.id]?.updateDisplay(timer)
    }

    fun removeTimerView(timerId: Long) {
        timerViews[timerId]?.let {
            timerContainer.removeView(it)
            timerViews.remove(timerId)
        }
    }

    fun removeAllViews() {
        try {
            windowManager.removeView(mainContainer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        timerViews.clear()
    }
}
