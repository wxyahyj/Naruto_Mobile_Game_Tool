# Android版快速开始指南

## 🚀 快速开始

### 环境准备

1. **安装开发工具**
   - Android Studio (最新版本)
   - JDK 11 或更高版本
   - Android SDK (API 24+)

2. **创建项目**
   ```
   File → New → New Project
   → Empty Views Activity
   → Name: NarutoTimer
   → Language: Kotlin
   → Minimum SDK: API 24 (Android 7.0)
   ```

### 最小实现步骤

#### 步骤1：添加权限 (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

#### 步骤2：创建核心文件

按以下顺序创建文件：

```
1. model/BeanState.kt           # 豆豆状态枚举
2. model/TimerInfo.kt           # 计时器数据模型
3. model/GameConfig.kt          # 游戏配置
4. detection/PixelDetector.kt   # 像素检测器
5. detection/BeanStateDetector.kt
6. detection/DetectionEngine.kt
7. ui/FloatingTimerView.kt      # 悬浮视图
8. ui/FloatingWindowManager.kt
9. service/FloatingTimerService.kt
10. MainActivity.kt
```

#### 步骤3：测试运行

1. 连接Android设备（真机，模拟器可能无法截图）
2. 运行应用
3. 授予悬浮窗权限
4. 授予屏幕截图权限
5. 打开游戏测试

---

## 📱 核心代码片段

### 1. 像素检测（最核心）

```kotlin
// 从Bitmap获取RGB
fun getPixelRGB(bitmap: Bitmap, x: Int, y: Int): Triple<Int, Int, Int> {
    val pixel = bitmap.getPixel(x, y)
    return Triple(
        Color.red(pixel),
        Color.green(pixel),
        Color.blue(pixel)
    )
}

// 判断豆豆状态
fun checkBeanState(rgb: Triple<Int, Int, Int>): BeanState {
    val (r, g, b) = rgb
    
    // 暗青色：无豆
    if (r in 9..64 && g in 0..59 && b in 0..107) {
        return BeanState.DARK_CYAN
    }
    
    // 亮蓝色：有豆
    if (r in 0..229 && g in 116..259 && b in 166..259) {
        return BeanState.BRIGHT_BLUE
    }
    
    return BeanState.CHAOS
}
```

### 2. 屏幕截图

```kotlin
// 初始化MediaProjection
mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)

// 创建ImageReader
imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

// 创建VirtualDisplay
virtualDisplay = mediaProjection.createVirtualDisplay(
    "ScreenCapture",
    width, height, density,
    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
    imageReader.surface, null, null
)

// 获取截图
imageReader.setOnImageAvailableListener { reader ->
    val image = reader.acquireLatestImage()
    // 处理image...
}, null)
```

### 3. 悬浮窗

```kotlin
// 创建悬浮窗
val params = WindowManager.LayoutParams(
    WRAP_CONTENT, WRAP_CONTENT,
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
    PixelFormat.TRANSLUCENT
)

windowManager.addView(view, params)
```

---

## 🔧 调试技巧

### 1. 查看检测结果

```kotlin
// 在检测引擎中添加日志
Log.d("Detection", "左豆: $leftCount, 右豆: $rightCount")
Log.d("RGB", "坐标($x,$y) = RGB($r,$g,$b)")
```

### 2. 截图保存

```kotlin
// 保存截图到文件，用于调试坐标
fun saveBitmap(bitmap: Bitmap, filename: String) {
    val file = File(context.filesDir, filename)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    Log.d("Save", "保存到: ${file.absolutePath}")
}
```

### 3. 坐标调试

```kotlin
// 在屏幕上显示检测点位置
class DebugView(context: Context) : View(context) {
    var points: List<Pair<Int, Int>> = emptyList()
    
    override fun onDraw(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
        }
        points.forEach { (x, y) ->
            canvas.drawCircle(x.toFloat(), y.toFloat(), 5f, paint)
        }
    }
}
```

---

## ⚠️ 常见问题

### Q1: 无法获取截图权限
**A**: 确保使用真机测试，部分模拟器不支持MediaProjection

### Q2: 悬浮窗不显示
**A**: 检查是否授予悬浮窗权限（Android 6.0+）

### Q3: 检测不准确
**A**: 
1. 检查坐标是否正确适配当前分辨率
2. 调整RGB容差值
3. 使用调试功能查看实际RGB值

### Q4: 服务被杀死
**A**: 
1. 使用前台服务
2. 添加到系统白名单
3. 处理服务重启逻辑

### Q5: 游戏更新后失效
**A**: 游戏UI可能变化，需要重新调试坐标和RGB范围

---

## 📊 性能参数建议

| 参数 | 推荐值 | 说明 |
|-----|--------|------|
| 检测间隔 | 100-200ms | 太短耗电，太长不准确 |
| RGB容差 | 3-5 | 根据设备调整 |
| 倒计时秒数 | 13.5秒 | 可根据实际情况微调 |
| 最大计时器数 | 10个 | 避免UI过于拥挤 |

---

## 🎯 下一步

1. **基础版本完成后**：
   - 添加设置界面
   - 支持多分辨率配置
   - 添加声音提醒

2. **进阶功能**：
   - 自动识别游戏模式
   - 统计数据记录
   - 云端配置同步

3. **发布准备**：
   - UI美化
   - 多语言支持
   - 用户教程

---

**提示**：详细实现请参考 [Android移植技术文档.md](./Android移植技术文档.md)
