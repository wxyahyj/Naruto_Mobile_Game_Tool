package com.naruto.timer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.naruto.timer.R
import com.naruto.timer.detection.DetectionEngine
import com.naruto.timer.model.GameConfig
import com.naruto.timer.model.TimerInfo
import com.naruto.timer.ui.FloatingWindowManager

class FloatingTimerService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingWindowManager: FloatingWindowManager

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    private lateinit var detectionEngine: DetectionEngine
    private lateinit var gameConfig: GameConfig

    private val timers = mutableListOf<TimerInfo>()
    private val timerHandler = Handler(Looper.getMainLooper())

    private val timerUpdateRunnable = object : Runnable {
        override fun run() {
            updateAllTimers()
            timerHandler.postDelayed(this, 100)
        }
    }

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val NOTIFICATION_CHANNEL_ID = "naruto_timer_channel"
        const val NOTIFICATION_ID = 1

        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi

        gameConfig = GameConfig.createDefault1920x1080()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        floatingWindowManager = FloatingWindowManager(this, windowManager)

        detectionEngine = DetectionEngine(
            config = gameConfig,
            onTimerTriggered = { position -> addNewTimer(position) },
            onBeanCountUpdated = { left, right ->
                floatingWindowManager.updateBeanCount(left, right)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode != -1 && resultData != null) {
            startScreenCapture(resultCode, resultData)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false

        detectionEngine.stop()
        timerHandler.removeCallbacks(timerUpdateRunnable)

        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()

        floatingWindowManager.removeAllViews()
    }

    private fun startScreenCapture(resultCode: Int, resultData: Intent) {
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)

        imageReader = ImageReader.newInstance(
            screenWidth, screenHeight,
            PixelFormat.RGBA_8888, 2
        ).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                image?.let {
                    processImage(it)
                    it.close()
                }
            }, null)
        }

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        detectionEngine.start()
        timerHandler.post(timerUpdateRunnable)
    }

    private fun processImage(image: Image) {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * screenWidth

        val bitmap = Bitmap.createBitmap(
            screenWidth + rowPadding / pixelStride,
            screenHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
        bitmap.recycle()

        detectionEngine.processNewFrame(croppedBitmap)
    }

    private fun addNewTimer(position: TimerInfo.Position) {
        val timer = TimerInfo(position = position)
        timers.add(timer)
        floatingWindowManager.addTimerView(timer)

        if (timers.size > 10) {
            val removed = timers.removeAt(0)
            floatingWindowManager.removeTimerView(removed.id)
        }
    }

    private fun updateAllTimers() {
        val iterator = timers.iterator()
        while (iterator.hasNext()) {
            val timer = iterator.next()
            timer.tick(100)

            if (!timer.isRunning && timer.remainingSeconds <= 0) {
                floatingWindowManager.removeTimerView(timer.id)
                iterator.remove()
            } else {
                floatingWindowManager.updateTimerDisplay(timer)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "火影计时器",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "替身计时器后台服务"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("火影计时器")
            .setContentText("正在运行中...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
