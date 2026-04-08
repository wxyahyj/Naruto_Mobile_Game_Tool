package com.naruto.timer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.naruto.timer.service.FloatingTimerService

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY_PERMISSION = 1001
    private val REQUEST_CODE_MEDIA_PROJECTION = 1002

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var statusText: TextView
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)

        startButton.setOnClickListener {
            checkPermissionsAndStart()
        }

        updateStatus("点击开始按钮启动计时器")
    }

    private fun checkPermissionsAndStart() {
        if (!checkOverlayPermission()) {
            requestOverlayPermission()
        } else {
            startMediaProjection()
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }

    private fun startMediaProjection() {
        updateStatus("请求屏幕截图权限...")
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, REQUEST_CODE_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_OVERLAY_PERMISSION -> {
                if (checkOverlayPermission()) {
                    startMediaProjection()
                } else {
                    Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show()
                    updateStatus("需要悬浮窗权限")
                }
            }

            REQUEST_CODE_MEDIA_PROJECTION -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    startFloatingService(resultCode, data)
                } else {
                    Toast.makeText(this, "需要屏幕截图权限", Toast.LENGTH_SHORT).show()
                    updateStatus("需要屏幕截图权限")
                }
            }
        }
    }

    private fun startFloatingService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, FloatingTimerService::class.java).apply {
            putExtra(FloatingTimerService.EXTRA_RESULT_CODE, resultCode)
            putExtra(FloatingTimerService.EXTRA_RESULT_DATA, data)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        updateStatus("计时器已启动\n返回游戏即可使用")
        Toast.makeText(this, "计时器已启动", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus(status: String) {
        statusText.text = status
    }
}
