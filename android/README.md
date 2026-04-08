# 火影计时器 Android版

## 📱 项目简介

这是火影忍者手游替身计时器的Android版本，基于原PC版移植。

### 功能特点
- ✅ 替身计时器（自动检测豆豆数量变化）
- ✅ 悬浮窗显示
- ✅ 多分辨率适配
- ✅ 后台运行

---

## 🚀 GitHub云端构建（无需本地环境）

### 方法一：自动构建

1. **Fork 本仓库**
   - 点击右上角 `Fork` 按钮

2. **启用 GitHub Actions**
   - 进入你Fork的仓库
   - 点击 `Actions` 标签
   - 如果提示启用，点击 `I understand my workflows, go ahead and enable them`

3. **触发构建**
   - 方式1：修改 `android` 目录下的任意文件并提交
   - 方式2：点击 `Actions` → `Android CI` → `Run workflow`

4. **下载APK**
   - 构建完成后，点击对应的workflow运行记录
   - 在页面底部的 `Artifacts` 区域下载 `app-debug` 或 `app-release`
   - 解压后得到APK文件

### 方法二：使用 GitHub Codespaces

1. **打开 Codespaces**
   - 点击仓库的 `Code` 按钮
   - 选择 `Codespaces` 标签
   - 点击 `Create codespace on main`

2. **在云端编辑代码**
   - 等待环境初始化完成
   - 在左侧文件树中编辑 `android` 目录下的文件

3. **构建APK**
   - 打开终端（Terminal）
   - 运行命令：
     ```bash
     cd android
     chmod +x gradlew
     ./gradlew assembleDebug
     ```
   - APK位置：`android/app/build/outputs/apk/debug/app-debug.apk`

4. **下载APK**
   - 在文件树中找到APK文件
   - 右键 → `Download`

---

## 📦 项目结构

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/naruto/timer/
│   │   │   ├── MainActivity.kt           # 主Activity
│   │   │   ├── model/                    # 数据模型
│   │   │   │   ├── BeanState.kt          # 豆豆状态枚举
│   │   │   │   ├── TimerInfo.kt          # 计时器信息
│   │   │   │   └── GameConfig.kt         # 游戏配置
│   │   │   ├── detection/                # 检测引擎
│   │   │   │   ├── PixelDetector.kt      # 像素检测
│   │   │   │   ├── BeanStateDetector.kt  # 豆豆状态检测
│   │   │   │   └── DetectionEngine.kt    # 检测引擎
│   │   │   ├── service/
│   │   │   │   └── FloatingTimerService.kt # 悬浮窗服务
│   │   │   └── ui/
│   │   │       ├── FloatingTimerView.kt  # 悬浮计时器视图
│   │   │       └── FloatingWindowManager.kt
│   │   ├── res/                          # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```

---

## 🔧 本地开发（可选）

### 环境要求
- Android Studio (最新版本)
- JDK 17+
- Android SDK (API 24+)

### 本地构建步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/你的用户名/Naruto_Mobile_Game_Too.git
   cd Naruto_Mobile_Game_Too/android
   ```

2. **打开项目**
   - 用 Android Studio 打开 `android` 目录
   - 等待 Gradle 同步完成

3. **构建APK**
   - 点击 `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 或运行：`./gradlew assembleDebug`

---

## 📱 安装与使用

### 安装APK

1. 将APK传输到Android手机
2. 点击APK文件安装
3. 如果提示"未知来源"，请在设置中允许安装

### 使用步骤

1. **启动应用**
   - 打开"火影计时器"应用

2. **授予权限**
   - 点击"开始"按钮
   - 授予悬浮窗权限
   - 授予屏幕截图权限

3. **开始游戏**
   - 返回游戏即可使用
   - 悬浮窗会自动显示豆豆数量和倒计时

---

## ⚙️ 配置说明

### 分辨率适配

默认配置为 1920x1080 分辨率。如需适配其他分辨率：

1. 编辑 `GameConfig.kt`
2. 修改坐标配置：
   ```kotlin
   // 示例：适配 2560x1440
   leftBeansArena = listOf(
       275 to 147, 316 to 147, 356 to 147, ...
   )
   ```

### 调整参数

在 `GameConfig.kt` 中可以调整：

| 参数 | 默认值 | 说明 |
|-----|--------|------|
| countdownSeconds | 13.5 | 倒计时秒数 |
| rgbTolerance | 4 | RGB容差值 |
| detectionIntervalMs | 100 | 检测间隔(毫秒) |

---

## ⚠️ 注意事项

### 法律风险
- 此类辅助工具可能违反游戏用户协议
- 存在账号封禁风险
- 仅供学习研究使用

### 技术限制
- 需要 Android 7.0 (API 24) 及以上
- 需要悬浮窗权限
- 需要屏幕截图权限
- 部分厂商ROM可能限制后台服务

### 兼容性
- MIUI：需开启"后台弹出界面"权限
- EMUI：需将应用加入白名单
- ColorOS：后台服务可能被系统杀死

---

## 🛠️ 常见问题

### Q: 构建失败怎么办？
A: 
1. 检查 GitHub Actions 日志
2. 确认修改的是 `android` 目录下的文件
3. 确认代码语法正确

### Q: APK安装失败？
A: 
1. 卸载旧版本后重新安装
2. 检查是否授予安装权限

### Q: 悬浮窗不显示？
A: 
1. 检查是否授予悬浮窗权限
2. 检查应用是否在后台运行

### Q: 检测不准确？
A: 
1. 检查分辨率配置是否正确
2. 调整 RGB 容差值
3. 查看日志确认检测结果

---

## 📄 许可证

本项目基于原项目移植，遵循相同的开源协议。

---

## 🙏 致谢

- 原作者：凤灯幽夜
- Bilibili UID：11897940
- QQ群：548419960

---

## 📚 相关文档

- [Android移植技术文档](../docs/Android移植技术文档.md)
- [Android快速开始指南](../docs/Android快速开始指南.md)
