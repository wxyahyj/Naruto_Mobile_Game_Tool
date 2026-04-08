package com.naruto.timer.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.naruto.timer.model.TimerInfo

class FloatingTimerView(context: Context) : FrameLayout(context) {

    private val textView: TextView

    init {
        setBackgroundColor(Color.parseColor("#FFDCF6"))
        setPadding(16, 8, 16, 8)

        textView = TextView(context).apply {
            textSize = 18f
            setTextColor(Color.parseColor("#25B7A8"))
            gravity = Gravity.CENTER
        }
        addView(textView, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))
    }

    fun updateDisplay(timer: TimerInfo) {
        val positionText = if (timer.position == TimerInfo.Position.LEFT) "左" else "右"
        val seconds = timer.remainingSeconds

        textView.text = String.format("%s: %.1f秒", positionText, seconds)

        when {
            seconds <= 3 -> textView.setTextColor(Color.RED)
            seconds <= 7 -> textView.setTextColor(Color.parseColor("#FF791B"))
            else -> textView.setTextColor(Color.parseColor("#25B7A8"))
        }
    }
}
