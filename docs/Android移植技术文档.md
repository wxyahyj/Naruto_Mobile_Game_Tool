# 火影忍者手游替身计时器 - Android移植技术文档

## 📋 目录

1. [项目概述](#项目概述)
2. [技术架构](#技术架构)
3. [核心模块实现](#核心模块实现)
4. [权限处理](#权限处理)
5. [关键代码实现](#关键代码实现)
6. [分辨率适配方案](#分辨率适配方案)
7. [性能优化](#性能优化)
8. [注意事项](#注意事项)

---

## 项目概述

### 原项目功能
- **替身计时器**：通过检测游戏中的"豆豆"数量变化，自动触发倒计时
- **核心原理**：基于屏幕像素RGB检测，识别豆豆状态（暗青、亮蓝、赤金）
- **检测频率**：100ms间隔

### Android版本目标
- 保留核心替身计时功能
- 移除模拟点击功能
- 使用悬浮窗显示倒计时
- 支持多分辨率适配

---

## 技术架构

### 整体架构图

```
┌─────────────────────────────────────────────────────┐
│                  Android 应用层                      │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐      ┌──────────────────┐        │
│  │ MainActivity │─────►│ PermissionHelper │        │
│  │  (启动界面)   │      │   (权限管理)      │        │
│  └──────┬───────┘      └──────────────────┘        │
│         │                                           │
│         │ 启动服务                                   │
│         ▼                                           │
│  ┌──────────────────────────────────────────┐      │
│  │        FloatingTimerService              │      │
│  │        (悬浮窗+截图+检测 主服务)           │      │
│  └──────┬─────────────────────────┬─────────┘      │
│         │                         │                 │
│         │ 管理                     │ 触发            │
│         ▼                         ▼                 │
│  ┌──────────────┐      ┌──────────────────┐        │
│  │FloatingView  │      │ DetectionEngine  │        │
│  │ (悬浮UI组件)  │◄─────│   (像素检测引擎)  │        │
│  └──────────────┘      └────────┬─────────┘        │
│                                 │                   │
│                                 │ 使用              │
│                                 ▼                   │
│                        ┌──────────────────┐         │
│                        │ ScreenCapture    │         │
│                        │   (截图模块)      │         │
│                        └──────────────────┘         │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 项目结构

```
app/
├── src/main/
│   ├── java/com/naruto/timer/
│   │   ├── MainActivity.kt                    # 主Activity
│   │   ├── service/
│   │   │   ├── FloatingTimerService.kt        # 核心服务
│   │   │   └── ScreenCaptureService.kt        # 截图服务（可选独立）
│   │   ├── ui/
│   │   │   ├── FloatingTimerView.kt           # 悬浮倒计时视图
│   │   │   ├── FloatingWindowManager.kt       # 悬浮窗管理
│   │   │   └── SettingsActivity.kt            # 设置界面
│   │   ├── detection/
│   │   │   ├── DetectionEngine.kt             # 检测引擎
│   │   │   ├── PixelDetector.kt               # 像素检测器
│   │   │   ├── BeanStateDetector.kt           # 豆豆状态检测
│   │   │   └── CharacterDetector.kt           # 角色检测（柱间等）
│   │   ├── model/
│   │   │   ├── BeanState.kt                   # 豆豆状态枚举
│   │   │   ├── TimerInfo.kt                   # 计时器信息
│   │   │   └── GameConfig.kt                  # 游戏配置
│   │   ├── utils/
│   │   │   ├── ConfigManager.kt               # 配置管理
│   │   │   ├── ResolutionAdapter.kt           # 分辨率适配
│   │   │   └── PermissionHelper.kt            # 权限辅助
│   │   └── data/
│   │       └── CoordinateConfig.kt            # 坐标配置数据
│   ├── res/
│   │   ├── layout/
│   │   ├── values/
│   │   └── drawable/
│   └── AndroidManifest.xml
└── build.gradle
```

---

## 核心模块实现

### 1. 数据模型

#### BeanState.kt - 豆豆状态枚举

```kotlin
package com.naruto.timer.model

/**
 * 豆豆状态枚举
 * 对应PC版的"暗青"、"亮蓝"、"赤金"、"混沌"
 */
enum class BeanState {
    DARK_CYAN,      // 暗青 - 无豆状态
    BRIGHT_BLUE,    // 亮蓝 - 有豆状态
    RED_GOLD,       // 赤金 - 柱间特殊状态
    CHAOS;          // 混沌 - 未知状态
    
    companion object {
        /**
         * 根据RGB值判断豆豆状态
         * @param rgb RGB三元组
         * @param tolerance RGB容差值
         * @return 豆豆状态
         */
        fun fromRGB(
            rgb: Triple<Int, Int, Int>,
            tolerance: Int = 4
        ): BeanState {
            val (r, g, b) = rgb
            
            // 暗青色检测：RGB范围 (13,3,1) ~ (60,55,103)
            if (r in (13 - tolerance)..(60 + tolerance) &&
                g in (3 - tolerance)..(55 + tolerance) &&
                b in (1 - tolerance)..(103 + tolerance)) {
                return DARK_CYAN
            }
            
            // 亮蓝色检测：RGB范围 (0,120,170) ~ (225,255,255)
            if (r in (0 - tolerance)..(225 + tolerance) &&
                g in (120 - tolerance)..(255 + tolerance) &&
                b in (170 - tolerance)..(255 + tolerance)) {
                return BRIGHT_BLUE
            }
            
            // 赤金色检测：RGB范围 (179,19,1) ~ (255,255,206)
            if (r in (179 - tolerance)..(255 + tolerance) &&
                g in (19 - tolerance)..(255 + tolerance) &&
                b in (1 - tolerance)..(206 + tolerance)) {
                return RED_GOLD
            }
            
            return CHAOS
        }
    }
}
```

#### TimerInfo.kt - 计时器信息

```kotlin
package com.naruto.timer.model

/**
 * 计时器信息
 */
data class TimerInfo(
    val id: Long = System.currentTimeMillis(),      // 唯一ID
    val position: Position,                          // 左侧或右侧
    var remainingSeconds: Double = 13.5,            // 剩余秒数
    val maxSeconds: Double = 13.5,                  // 最大秒数
    var isRunning: Boolean = true,                  // 是否运行中
    val createdAt: Long = System.currentTimeMillis() // 创建时间
) {
    enum class Position {
        LEFT,   // 左侧
        RIGHT   // 右侧
    }
    
    /**
     * 更新剩余时间
     */
    fun tick(intervalMs: Long) {
        if (isRunning) {
            remainingSeconds -= intervalMs / 1000.0
            if (remainingSeconds <= 0) {
                remainingSeconds = 0.0
                isRunning = false
            }
        }
    }
    
    /**
     * 重置计时器
     */
    fun reset() {
        remainingSeconds = maxSeconds
        isRunning = true
    }
}
```

#### GameConfig.kt - 游戏配置

```kotlin
package com.naruto.timer.model

/**
 * 游戏配置
 * 包含所有坐标和RGB范围配置
 */
data class GameConfig(
    // 基础设置
    val countdownSeconds: Double = 13.5,        // 倒计时秒数
    val rgbTolerance: Int = 4,                  // RGB容差值
    val detectionIntervalMs: Long = 100,        // 检测间隔(毫秒)
    
    // 豆豆位置坐标 - 决斗场模式
    val leftBeansArena: List<Pair<Int, Int>>,   // 左侧豆豆坐标
    val rightBeansArena: List<Pair<Int, Int>>,  // 右侧豆豆坐标
    
    // 豆豆位置坐标 - 训练营模式
    val leftBeansTraining: List<Pair<Int, Int>>,
    val rightBeansTraining: List<Pair<Int, Int>>,
    
    // 柱间检测坐标
    val leftHashiramaPoints: List<Pair<Int, Int>>,
    val rightHashiramaPoints: List<Pair<Int, Int>>,
    
    // 模式检测点位
    val arenaDetectionPoint: Pair<Int, Int>,    // 决斗场检测点
    val trainingDetectionPoint: Pair<Int, Int>, // 训练营检测点
    
    // RGB范围配置
    val darkCyanRange: RGBRange,                // 暗青色范围
    val brightBlueRange: RGBRange,              // 亮蓝色范围
    val redGoldRange: RGBRange,                 // 赤金色范围
    val hashiramaRGBRange: RGBRange             // 柱间检测RGB范围
) {
    /**
     * RGB范围数据类
     */
    data class RGBRange(
        val min: Triple<Int, Int, Int>,
        val max: Triple<Int, Int, Int>
    )
    
    companion object {
        /**
         * 创建默认配置（1920x1080分辨率）
         */
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
```

### 2. 检测引擎

#### PixelDetector.kt - 像素检测器

```kotlin
package com.naruto.timer.detection

import android.graphics.Bitmap
import android.graphics.Color

/**
 * 像素检测器
 * 负责从Bitmap中提取RGB值并进行范围判断
 */
class PixelDetector(private val bitmap: Bitmap) {
    
    /**
     * 获取指定坐标的RGB值
     * @param x X坐标
     * @param y Y坐标
     * @return RGB三元组 (R, G, B)
     */
    fun getPixelRGB(x: Int, y: Int): Triple<Int, Int, Int> {
        // 边界检查
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
    
    /**
     * 检测RGB值是否在指定范围内
     * @param target 目标RGB值
     * @param minRange 最小范围
     * @param maxRange 最大范围
     * @param tolerance 容差值
     * @return 是否在范围内
     */
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
    
    /**
     * 批量获取多个坐标的RGB值
     * @param points 坐标列表
     * @return RGB值列表
     */
    fun getMultiplePixelsRGB(points: List<Pair<Int, Int>>): List<Triple<Int, Int, Int>> {
        return points.map { (x, y) -> getPixelRGB(x, y) }
    }
    
    /**
     * 释放资源
     */
    fun recycle() {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
```

#### BeanStateDetector.kt - 豆豆状态检测器

```kotlin
package com.naruto.timer.detection

import com.naruto.timer.model.BeanState
import com.naruto.timer.model.GameConfig

/**
 * 豆豆状态检测器
 * 负责检测左右两侧的豆豆状态
 */
class BeanStateDetector(
    private val pixelDetector: PixelDetector,
    private val config: GameConfig
) {
    // 当前豆豆状态
    private val leftBeanStates = MutableList(6) { BeanState.CHAOS }
    private val rightBeanStates = MutableList(6) { BeanState.CHAOS }
    
    // 当前豆豆数量
    var leftBeanCount: Int = 0
        private set
    var rightBeanCount: Int = 0
        private set
    
    // 上一次豆豆数量（用于检测变化）
    private var lastLeftBeanCount: Int = 0
    private var lastRightBeanCount: Int = 0
    
    /**
     * 检测所有豆豆状态
     * @param isArenaMode 是否为决斗场模式
     */
    fun detectAllBeans(isArenaMode: Boolean = true) {
        val leftPoints = if (isArenaMode) config.leftBeansArena else config.leftBeansTraining
        val rightPoints = if (isArenaMode) config.rightBeansArena else config.rightBeansTraining
        
        // 检测左侧豆豆
        leftPoints.forEachIndexed { index, point ->
            val rgb = pixelDetector.getPixelRGB(point.first, point.second)
            leftBeanStates[index] = BeanState.fromRGB(rgb, config.rgbTolerance)
        }
        
        // 检测右侧豆豆
        rightPoints.forEachIndexed { index, point ->
            val rgb = pixelDetector.getPixelRGB(point.first, point.second)
            rightBeanStates[index] = BeanState.fromRGB(rgb, config.rgbTolerance)
        }
        
        // 计算豆豆数量
        lastLeftBeanCount = leftBeanCount
        lastRightBeanCount = rightBeanCount
        
        leftBeanCount = calculateBeanCount(leftBeanStates, isLeft = true)
        rightBeanCount = calculateBeanCount(rightBeanStates, isLeft = false)
    }
    
    /**
     * 计算豆豆数量
     * 根据豆豆状态组合判断实际数量
     */
    private fun calculateBeanCount(states: List<BeanState>, isLeft: Boolean): Int {
        // 检查是否为柱间（需要特殊处理）
        val isHashirama = checkHashirama(isLeft)
        
        // 根据豆豆状态组合判断数量
        // 参考 PC 版的检测逻辑
        return when {
            // 0豆：所有豆都是暗青
            states.all { it == BeanState.DARK_CYAN } -> 0
            
            // 1豆：第一个亮蓝，其余暗青
            states[0] == BeanState.BRIGHT_BLUE && 
            states.subList(1, 4).all { it == BeanState.DARK_CYAN } -> 1
            
            // 2豆：前两个亮蓝，其余暗青
            states[0] == BeanState.BRIGHT_BLUE && 
            states[1] == BeanState.BRIGHT_BLUE &&
            states.subList(2, 4).all { it == BeanState.DARK_CYAN } -> 2
            
            // 3豆：前三个亮蓝，第四个暗青
            states[0] == BeanState.BRIGHT_BLUE && 
            states[1] == BeanState.BRIGHT_BLUE &&
            states[2] == BeanState.BRIGHT_BLUE &&
            states[3] == BeanState.DARK_CYAN -> 3
            
            // 4豆：前四个都是亮蓝或赤金
            states.subList(0, 4).all { it == BeanState.BRIGHT_BLUE || it == BeanState.RED_GOLD } -> 4
            
            // 柱间特殊状态：5豆或6豆
            isHashirama && states.subList(0, 5).all { it == BeanState.RED_GOLD } -> {
                if (states[5] == BeanState.RED_GOLD) 6 else 5
            }
            
            // 六尾鸣人特殊状态：赤金色豆豆
            states[0] == BeanState.RED_GOLD && 
            states.subList(1, 4).all { it == BeanState.DARK_CYAN } -> 1
            
            else -> leftBeanCount // 保持上一次的值
        }
    }
    
    /**
     * 检测是否为柱间角色
     */
    private fun checkHashirama(isLeft: Boolean): Boolean {
        val points = if (isLeft) config.leftHashiramaPoints else config.rightHashiramaPoints
        
        // 检测所有柱间检测点
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
    
    /**
     * 检测豆豆数量是否发生变化
     * @return Pair<左侧是否变化, 右侧是否变化>
     */
    fun checkBeanCountChanged(): Pair<Boolean, Boolean> {
        val leftChanged = leftBeanCount != lastLeftBeanCount
        val rightChanged = rightBeanCount != lastRightBeanCount
        return Pair(leftChanged, rightChanged)
    }
    
    /**
     * 获取当前豆豆状态描述
     */
    fun getBeanStateDescription(): String {
        return "左侧: $leftBeanCount 豆 | 右侧: $rightBeanCount 豆"
    }
}
```

#### DetectionEngine.kt - 检测引擎

```kotlin
package com.naruto.timer.detection

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.naruto.timer.model.GameConfig
import com.naruto.timer.model.TimerInfo

/**
 * 检测引擎
 * 协调截图、像素检测、状态判断的核心引擎
 */
class DetectionEngine(
    private val config: GameConfig,
    private val onTimerTriggered: (TimerInfo.Position) -> Unit,
    private val onBeanCountUpdated: (leftCount: Int, rightCount: Int) -> Unit
) {
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    
    private var pixelDetector: PixelDetector? = null
    private var beanStateDetector: BeanStateDetector? = null
    
    // 当前游戏模式
    private var isArenaMode = true
    
    // 检测任务
    private val detectionRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                performDetection()
                handler.postDelayed(this, config.detectionIntervalMs)
            }
        }
    }
    
    /**
     * 启动检测引擎
     */
    fun start() {
        if (isRunning) return
        isRunning = true
        handler.post(detectionRunnable)
    }
    
    /**
     * 停止检测引擎
     */
    fun stop() {
        isRunning = false
        handler.removeCallbacks(detectionRunnable)
        pixelDetector?.recycle()
    }
    
    /**
     * 处理新的截图
     * @param bitmap 截图Bitmap
     */
    fun processNewFrame(bitmap: Bitmap) {
        pixelDetector?.recycle()
        pixelDetector = PixelDetector(bitmap)
        beanStateDetector = BeanStateDetector(pixelDetector!!, config)
    }
    
    /**
     * 执行检测
     */
    private fun performDetection() {
        val detector = beanStateDetector ?: return
        
        // 检测所有豆豆状态
        detector.detectAllBeans(isArenaMode)
        
        // 检测豆豆数量变化
        val (leftChanged, rightChanged) = detector.checkBeanCountChanged()
        
        // 触发计时器
        if (leftChanged && detector.leftBeanCount < detector.lastLeftBeanCount) {
            onTimerTriggered(TimerInfo.Position.LEFT)
        }
        if (rightChanged && detector.rightBeanCount < detector.lastRightBeanCount) {
            onTimerTriggered(TimerInfo.Position.RIGHT)
        }
        
        // 更新UI显示
        onBeanCountUpdated(detector.leftBeanCount, detector.rightBeanCount)
    }
    
    /**
     * 切换游戏模式
     */
    fun setArenaMode(isArena: Boolean) {
        isArenaMode = isArena
    }
}
```

### 3. 悬浮窗服务

#### FloatingTimerService.kt - 悬浮窗服务

```kotlin
package com.naruto.timer.service

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
import android.view.Gravity
import android.view.WindowManager
import com.naruto.timer.detection.DetectionEngine
import com.naruto.timer.model.GameConfig
import com.naruto.timer.model.TimerInfo
import com.naruto.timer.ui.FloatingTimerView
import com.naruto.timer.ui.FloatingWindowManager

/**
 * 悬浮计时器服务
 * 负责屏幕截图、检测、悬浮窗显示
 */
class FloatingTimerService : Service() {
    
    // 悬浮窗管理
    private lateinit var windowManager: WindowManager
    private lateinit var floatingWindowManager: FloatingWindowManager
    
    // 屏幕截图相关
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    // 屏幕尺寸
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0
    
    // 检测引擎
    private lateinit var detectionEngine: DetectionEngine
    private lateinit var gameConfig: GameConfig
    
    // 计时器列表
    private val timers = mutableListOf<TimerInfo>()
    private val timerHandler = Handler(Looper.getMainLooper())
    
    // 计时器更新任务
    private val timerUpdateRunnable = object : Runnable {
        override fun run() {
            updateAllTimers()
            timerHandler.postDelayed(this, 100) // 100ms更新一次
        }
    }
    
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        
        var isRunning = false
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        
        // 初始化WindowManager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 获取屏幕尺寸
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi
        
        // 初始化配置
        gameConfig = GameConfig.createDefault1920x1080()
        
        // 初始化悬浮窗
        floatingWindowManager = FloatingWindowManager(this, windowManager)
        
        // 初始化检测引擎
        detectionEngine = DetectionEngine(
            config = gameConfig,
            onTimerTriggered = { position -> addNewTimer(position) },
            onBeanCountUpdated = { left, right -> 
                floatingWindowManager.updateBeanCount(left, right)
            }
        )
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 获取MediaProjection参数
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        
        if (resultCode != -1 && resultData != null) {
            startScreenCapture(resultCode, resultData)
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        
        // 停止检测
        detectionEngine.stop()
        
        // 停止计时器更新
        timerHandler.removeCallbacks(timerUpdateRunnable)
        
        // 释放截图资源
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        
        // 移除悬浮窗
        floatingWindowManager.removeAllViews()
    }
    
    /**
     * 启动屏幕截图
     */
    private fun startScreenCapture(resultCode: Int, resultData: Intent) {
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
        
        // 创建ImageReader
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
        
        // 创建VirtualDisplay
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
        
        // 启动检测引擎
        detectionEngine.start()
        
        // 启动计时器更新
        timerHandler.post(timerUpdateRunnable)
    }
    
    /**
     * 处理截图图像
     */
    private fun processImage(image: Image) {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * screenWidth
        
        // 创建Bitmap
        val bitmap = Bitmap.createBitmap(
            screenWidth + rowPadding / pixelStride,
            screenHeight,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        
        // 裁剪掉多余部分
        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
        bitmap.recycle()
        
        // 传递给检测引擎
        detectionEngine.processNewFrame(croppedBitmap)
    }
    
    /**
     * 添加新的计时器
     */
    private fun addNewTimer(position: TimerInfo.Position) {
        val timer = TimerInfo(position = position)
        timers.add(timer)
        floatingWindowManager.addTimerView(timer)
        
        // 限制最大计时器数量
        if (timers.size > 10) {
            val removed = timers.removeAt(0)
            floatingWindowManager.removeTimerView(removed.id)
        }
    }
    
    /**
     * 更新所有计时器
     */
    private fun updateAllTimers() {
        val iterator = timers.iterator()
        while (iterator.hasNext()) {
            val timer = iterator.next()
            timer.tick(100)
            
            if (!timer.isRunning && timer.remainingSeconds <= 0) {
                // 计时结束，移除
                floatingWindowManager.removeTimerView(timer.id)
                iterator.remove()
            } else {
                // 更新显示
                floatingWindowManager.updateTimerDisplay(timer)
            }
        }
    }
}
```

### 4. 悬浮窗UI

#### FloatingTimerView.kt - 悬浮计时器视图

```kotlin
package com.naruto.timer.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.naruto.timer.model.TimerInfo

/**
 * 悬浮计时器视图
 * 显示单个倒计时
 */
class FloatingTimerView(context: Context) : FrameLayout(context) {
    
    private val textView: TextView
    
    init {
        // 设置背景
        setBackgroundColor(Color.parseColor("#FFDCF6"))
        setPadding(16, 8, 16, 8)
        
        // 创建文本视图
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
    
    /**
     * 更新显示
     */
    fun updateDisplay(timer: TimerInfo) {
        val positionText = if (timer.position == TimerInfo.Position.LEFT) "左" else "右"
        val seconds = timer.remainingSeconds
        
        textView.text = String.format("%s: %.1f秒", positionText, seconds)
        
        // 根据剩余时间改变颜色
        when {
            seconds <= 3 -> textView.setTextColor(Color.RED)
            seconds <= 7 -> textView.setTextColor(Color.parseColor("#FF791B"))
            else -> textView.setTextColor(Color.parseColor("#25B7A8"))
        }
    }
}
```

#### FloatingWindowManager.kt - 悬浮窗管理器

```kotlin
package com.naruto.timer.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.naruto.timer.model.TimerInfo

/**
 * 悬浮窗管理器
 * 管理所有悬浮视图
 */
class FloatingWindowManager(
    private val context: Context,
    private val windowManager: WindowManager
) {
    // 主容器
    private val mainContainer: FrameLayout
    
    // 豆豆数量显示
    private val beanCountView: TextView
    
    // 计时器容器
    private val timerContainer: LinearLayout
    
    // 计时器视图映射
    private val timerViews = mutableMapOf<Long, FloatingTimerView>()
    
    init {
        // 创建主容器
        mainContainer = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        
        // 创建豆豆数量显示
        beanCountView = TextView(context).apply {
            textSize = 18f
            setTextColor(Color.parseColor("#EC137A"))
            setBackgroundColor(Color.WHITE)
            setPadding(16, 8, 16, 8)
            text = "左: 0 豆 | 右: 0 豆"
        }
        
        // 创建计时器容器
        timerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 50, 0, 0) // 顶部留出空间给豆豆数量显示
        }
        
        // 添加到主容器
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
        
        // 添加到窗口
        addToWindow()
    }
    
    /**
     * 添加到窗口
     */
    private fun addToWindow() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
    
    /**
     * 更新豆豆数量显示
     */
    fun updateBeanCount(leftCount: Int, rightCount: Int) {
        beanCountView.text = "左: $leftCount 豆 | 右: $rightCount 豆"
    }
    
    /**
     * 添加计时器视图
     */
    fun addTimerView(timer: TimerInfo) {
        val view = FloatingTimerView(context)
        view.updateDisplay(timer)
        timerContainer.addView(view)
        timerViews[timer.id] = view
    }
    
    /**
     * 更新计时器显示
     */
    fun updateTimerDisplay(timer: TimerInfo) {
        timerViews[timer.id]?.updateDisplay(timer)
    }
    
    /**
     * 移除计时器视图
     */
    fun removeTimerView(timerId: Long) {
        timerViews[timerId]?.let {
            timerContainer.removeView(it)
            timerViews.remove(timerId)
        }
    }
    
    /**
     * 移除所有视图
     */
    fun removeAllViews() {
        windowManager.removeView(mainContainer)
        timerViews.clear()
    }
}
```

### 5. 主Activity

#### MainActivity.kt

```kotlin
package com.naruto.timer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.naruto.timer.service.FloatingTimerService

class MainActivity : AppCompatActivity() {
    
    private val REQUEST_CODE_OVERLAY_PERMISSION = 1001
    private val REQUEST_CODE_MEDIA_PROJECTION = 1002
    
    private lateinit var mediaProjectionManager: MediaProjectionManager
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 初始化MediaProjectionManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        // 检查权限并启动服务
        checkPermissionsAndStart()
    }
    
    /**
     * 检查权限并启动服务
     */
    private fun checkPermissionsAndStart() {
        if (!checkOverlayPermission()) {
            requestOverlayPermission()
        } else {
            startMediaProjection()
        }
    }
    
    /**
     * 检查悬浮窗权限
     */
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    /**
     * 请求悬浮窗权限
     */
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }
    
    /**
     * 启动MediaProjection请求
     */
    private fun startMediaProjection() {
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
                    finish()
                }
            }
            
            REQUEST_CODE_MEDIA_PROJECTION -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    startFloatingService(resultCode, data)
                } else {
                    Toast.makeText(this, "需要屏幕截图权限", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
    
    /**
     * 启动悬浮窗服务
     */
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
        
        // 关闭Activity，服务会在后台运行
        finish()
    }
}
```

---

## 权限处理

### AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.naruto.timer">

    <!-- 前台服务权限 -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    
    <!-- Android 14+ 前台服务类型 -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    
    <!-- 悬浮窗权限 -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    
    <!-- 开机自启动（可选） -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.NarutoTimer">
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- 悬浮窗服务 -->
        <service
            android:name=".service.FloatingTimerService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="mediaProjection" />
            
    </application>

</manifest>
```

### 权限请求流程

```
启动应用
    │
    ├── 检查悬浮窗权限
    │   ├── 无权限 → 请求权限 → 用户授权
    │   └── 有权限 → 继续
    │
    ├── 请求MediaProjection权限
    │   └── 用户授权
    │
    └── 启动前台服务
        ├── 创建悬浮窗
        ├── 开始屏幕截图
        └── 启动检测引擎
```

---

## 分辨率适配方案

### ResolutionAdapter.kt

```kotlin
package com.naruto.timer.utils

import android.util.DisplayMetrics
import android.view.WindowManager
import com.naruto.timer.model.GameConfig

/**
 * 分辨率适配器
 * 根据实际屏幕分辨率调整坐标配置
 */
class ResolutionAdapter(
    private val windowManager: WindowManager
) {
    // 基准分辨率（PC版默认）
    private val baseWidth = 1920
    private val baseHeight = 1080
    
    // 实际屏幕分辨率
    private var actualWidth = 0
    private var actualHeight = 0
    
    init {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        actualWidth = metrics.widthPixels
        actualHeight = metrics.heightPixels
    }
    
    /**
     * 缩放坐标
     * @param baseX 基准X坐标
     * @param baseY 基准Y坐标
     * @return 缩放后的坐标
     */
    fun scaleCoordinate(baseX: Int, baseY: Int): Pair<Int, Int> {
        val scaledX = (baseX * actualWidth / baseWidth)
        val scaledY = (baseY * actualHeight / baseHeight)
        return Pair(scaledX, scaledY)
    }
    
    /**
     * 缩放坐标列表
     */
    fun scaleCoordinates(baseCoordinates: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        return baseCoordinates.map { (x, y) -> scaleCoordinate(x, y) }
    }
    
    /**
     * 创建适配后的游戏配置
     */
    fun createAdaptedConfig(): GameConfig {
        val baseConfig = GameConfig.createDefault1920x1080()
        
        return GameConfig(
            countdownSeconds = baseConfig.countdownSeconds,
            rgbTolerance = baseConfig.rgbTolerance,
            detectionIntervalMs = baseConfig.detectionIntervalMs,
            
            // 缩放所有坐标
            leftBeansArena = scaleCoordinates(baseConfig.leftBeansArena),
            rightBeansArena = scaleCoordinates(baseConfig.rightBeansArena),
            leftBeansTraining = scaleCoordinates(baseConfig.leftBeansTraining),
            rightBeansTraining = scaleCoordinates(baseConfig.rightBeansTraining),
            leftHashiramaPoints = scaleCoordinates(baseConfig.leftHashiramaPoints),
            rightHashiramaPoints = scaleCoordinates(baseConfig.rightHashiramaPoints),
            arenaDetectionPoint = scaleCoordinate(
                baseConfig.arenaDetectionPoint.first,
                baseConfig.arenaDetectionPoint.second
            ),
            trainingDetectionPoint = scaleCoordinate(
                baseConfig.trainingDetectionPoint.first,
                baseConfig.trainingDetectionPoint.second
            ),
            
            // RGB范围不需要缩放
            darkCyanRange = baseConfig.darkCyanRange,
            brightBlueRange = baseConfig.brightBlueRange,
            redGoldRange = baseConfig.redGoldRange,
            hashiramaRGBRange = baseConfig.hashiramaRGBRange
        )
    }
    
    /**
     * 获取屏幕信息
     */
    fun getScreenInfo(): String {
        return "屏幕分辨率: ${actualWidth}x${actualHeight}"
    }
}
```

### 支持的分辨率配置

| 分辨率 | 宽高比 | 缩放比例 | 备注 |
|--------|--------|----------|------|
| 1920x1080 | 16:9 | 1.0x | 基准分辨率 |
| 2560x1440 | 16:9 | 1.33x | 2K分辨率 |
| 3840x2160 | 4K | 2.0x | 4K分辨率 |
| 1280x720 | 16:9 | 0.67x | HD分辨率 |
| 1080x1920 | 9:16 | - | 竖屏（需特殊处理） |

---

## 性能优化

### 1. 截图优化

```kotlin
// 使用ImageReader池化技术
class ImageReaderPool(private val size: Int, width: Int, height: Int) {
    private val pool = ArrayDeque<ImageReader>()
    
    init {
        repeat(size) {
            pool.add(ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1))
        }
    }
    
    fun acquire(): ImageReader? = pool.pollFirst()
    fun release(reader: ImageReader) = pool.addLast(reader)
}
```

### 2. 检测优化

```kotlin
// 只检测变化区域
class OptimizedDetectionEngine {
    private var lastFrameHash: Int = 0
    
    fun shouldDetect(bitmap: Bitmap): Boolean {
        // 计算关键区域的哈希值
        val hash = calculateRegionHash(bitmap)
        if (hash == lastFrameHash) {
            return false // 画面未变化，跳过检测
        }
        lastFrameHash = hash
        return true
    }
}
```

### 3. 内存优化

```kotlin
// Bitmap复用
class BitmapPool {
    private val pool = mutableMapOf<String, Bitmap>()
    
    fun getOrCreate(width: Int, height: Int): Bitmap {
        val key = "${width}x${height}"
        return pool.getOrPut(key) {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
    }
}
```

---

## 注意事项

### 1. 法律风险

⚠️ **重要提示**：
- 此类辅助工具可能违反游戏用户协议
- 可能被判定为外挂/作弊软件
- 存在账号封禁风险
- 应用商店可能拒绝上架

### 2. 技术限制

```
Android版本要求：
├── 最低版本：Android 7.0 (API 24)
├── 推荐版本：Android 10+ (API 29+)
└── 原因：MediaProjection API稳定性

设备要求：
├── 需要悬浮窗权限
├── 需要屏幕截图权限
└── 部分厂商ROM可能限制后台服务
```

### 3. 兼容性问题

```
已知问题：
├── MIUI：需要额外开启"后台弹出界面"权限
├── EMUI：可能需要手动将应用加入白名单
├── ColorOS：后台服务可能被系统杀死
└── 游戏更新后坐标可能失效
```

### 4. 电池消耗

```
优化建议：
├── 检测间隔不低于100ms
├── 游戏暂停时停止检测
├── 使用前台服务降低被杀死概率
└── 提供省电模式选项
```

---

## 开发路线图

### 第一阶段：基础功能（1-2周）
- [x] 项目框架搭建
- [x] 权限处理
- [x] 屏幕截图服务
- [x] 像素检测引擎
- [x] 悬浮窗显示

### 第二阶段：核心功能（1周）
- [ ] 豆豆状态检测
- [ ] 计时器触发逻辑
- [ ] 分辨率适配

### 第三阶段：优化完善（1周）
- [ ] 性能优化
- [ ] 电池优化
- [ ] 多机型适配
- [ ] Bug修复

### 第四阶段：发布准备（可选）
- [ ] UI美化
- [ ] 用户设置界面
- [ ] 使用教程
- [ ] 测试与发布

---

## 附录

### A. 参考资源

- [MediaProjection API 文档](https://developer.android.com/reference/android/media/projection/MediaProjection)
- [AccessibilityService 指南](https://developer.android.com/guide/topics/ui/accessibility/service)
- [悬浮窗开发指南](https://developer.android.com/about/versions/oreo/background#overlay)

### B. PC版核心算法参考

详见原项目文件：`天王寺科学忍具V4.8.py`

### C. 联系方式

- 原作者：凤灯幽夜
- Bilibili UID：11897940
- QQ群：548419960

---

**文档版本**：v1.0  
**最后更新**：2026-04-08
