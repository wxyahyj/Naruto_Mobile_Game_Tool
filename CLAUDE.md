# CLAUDE.md — AI Assistant Guide for Naruto_Mobile_Game_Tool

## Project Overview

**天王寺科学忍具 (Tenwaji Science Ninja Tool)** is a free, open-source automation assistant for the mobile game *Naruto: Slugfest* (火影忍者手游). It runs as a Windows overlay on top of the game's PC emulator and uses screen pixel RGB detection to provide real-time combat assistance.

- **Author**: 凤灯幽夜 (Bilibili UID: 11897940)
- **License**: See `LICENSE`
- **Community**: QQ Group 548419960
- **Current version**: V4.8.0
- **Language**: Python (PyQt6 GUI, Windows-targeted)

---

## Repository Structure

```
Naruto_Mobile_Game_Tool/
├── 天王寺科学忍具V4.8.py          # Main source file (V4.8, current)
├── 天王寺科学忍具V4.8[常规].exe   # Normal (GUI) build
├── 天王寺科学忍具V4.8[控制台].exe # Console build
├── RinaTool_V4.8_Console.exe      # Alias console build
├── RinaTool_V4.8_Normal.exe       # Alias normal build
├── 截图.png                        # Screenshot shown in README
├── README.md
├── LICENSE
├── 文件/                           # Runtime assets folder
│   ├── icon/                      # Application icons (1.ico–6.ico, random on startup)
│   ├── BG/                        # Background image layers (璃奈酱 layered PNGs)
│   ├── RINA1.ico
│   ├── 收款码2.png                 # Donation QR code
│   ├── X轴范围显示.png / 范围显示.png
│   ├── 决斗场设置.png / 训练营设置.png
│   ├── 诫训.txt                   # Embedded code executed at startup (anti-abuse)
│   └── 内嵌内容/
│       ├── 配置.txt               # Default config (loaded at runtime)
│       ├── 开发日志.txt           # Developer diary / changelog
│       ├── 科学忍具教程By凤灯幽夜.docx
│       ├── 视频教程低清版.mp4
│       ├── 背景图片/              # Optional animated GIF background
│       └── 各种配置示例/          # Per-resolution config examples
│           ├── 配置_1920x1080标准.txt
│           ├── 配置_2560x1440.txt
│           ├── 配置_3840x2160.txt
│           └── 配置_凤灯幽夜.txt
└── V4.5历史版/                    # Historical V4.5 archive (Python + EXE + assets)
```

### Key asset lookup at runtime
- When running as a packaged EXE (`sys.frozen`), assets are resolved relative to `sys._MEIPASS` via `路径修正()`.
- When running from source, assets are resolved relative to `__file__`.
- An external `配置.txt` placed **next to the EXE/script** is loaded at startup and overrides `自定义设置`.

---

## Architecture

The entire application lives in a **single Python file**: `天王寺科学忍具V4.8.py`.

### Startup sequence
1. `发送打开次数()` — sends a TCP "+1" ping to `182.92.178.158:12345` (usage counter; non-blocking, 5 s timeout).
2. `强行设置DPI()` — forces DPI-unaware mode via `shcore`/`user32` before Qt initializes.
3. `自定义设置` dict — large flat dictionary holding every tunable parameter (pixel coords, RGB ranges, UI geometry, keybinds, etc.).
4. `_load()` — executes `文件/诫训.txt` into globals (anti-abuse/integrity check).
5. `加载配置()` — executes external `配置.txt` into globals, overriding `自定义设置` values.
6. Global state dicts populated from `自定义设置`: `左右豆豆数量`, `各个豆豆状态`, `豆豆颜色区间`, `各个坐标点位`, `柱间检测相关`, etc.
7. Random startup notification via `显示通知()` (system tray toast with random Aqours lyric).
8. PyQt6 `QApplication` + `根窗口` shown as an always-on-top, frameless, translucent overlay.

### Threading model (PyQt6)
All background work runs in `QThread` subclasses; GUI updates are done **only via Qt signals** (never from worker threads directly).

| Class | Purpose |
|---|---|
| `按键监听线程` | Listens for keypresses via `pynput.keyboard`; emits `AnJianAnXiaXinHao(str)` |
| `屏幕检测线程` | Main loop — takes one screenshot per tick, emits `XiangSuJianCeXinHao(list)` with `["截屏"]` then `["检测"]` commands |
| `根窗口` | Main Qt window; receives signals and does all detection logic + GUI updates on the main thread |

### Core detection algorithm (豆 = chakra beads)
- One screenshot per tick (default 100 ms) using Qt's `QGuiApplication.primaryScreen().grabWindow()`.
- 12 pixel coordinates are sampled from that single screenshot — 6 left-side, 6 right-side chakra beads.
- Each pixel's RGB is compared against three color ranges:
  - **暗青** (dark teal) → 0 chakra / empty bead
  - **亮蓝** (bright blue) → 1–3 chakra
  - **赤金** (red-gold) → 4–6 chakra / stun state
- A bead count change (`N → N-1`) triggers the substitution (替身) timer countdown.
- Special characters (柱間 / Hashirama) are detected by additional pixel checks on the character avatar area; they use 6 beads instead of 4.
- 六尾鸣人 (Six-Tails Naruto) detected similarly; its bead color range overrides the default.

### Configuration system
All settings live in the `自定义设置` dict (lines ~74–320). An external `配置.txt` file (placed next to the script/EXE) can **override any key** using `exec()`. Pre-made templates for common resolutions are in `文件/内嵌内容/各种配置示例/`.

---

## Features

| Feature | Key setting | Default |
|---|---|---|
| Substitution (替身) timer | `倒计时秒数` | 13.5 s |
| RGB tolerance | `RGB容差值` | 4 |
| Detection rate | `检测速率` | 100 ms |
| Auto Neji acupressure (自动点穴) | `点穴开关` | 关闭 |
| Auto summoning scroll tracking (记牌) | `记牌开关` | 开启 |
| Auto replay saving (回放) | `回放开关` | 开启 |
| X-axis range display | via settings window | — |
| Mode | `基础模式` | 决斗场 |

### Keybinds (configurable)
| Key | Action |
|---|---|
| `z` | Simulate left substitution |
| `c` | Simulate right substitution |
| `v` | Clear all countdown timers |
| `x` | Close program |

---

## Development Conventions

### Language
- **All identifiers, function names, variable names, class names, dict keys, and comments are written in Chinese**. This is intentional and a core project convention. Do not rename them to English.
- English is used only for PyQt6/Python standard library identifiers that require it.

### Code style
- A single large flat `自定义设置` dict holds all configuration. New tunable parameters should be added there.
- State dictionaries (e.g. `左右豆豆数量`, `各个豆豆状态`) use `{"状态": ...}` or `{"左方..": ..., "右方..": ...}` patterns — keep this consistent.
- Workers communicate with the GUI **exclusively via PyQt6 signals** — never call GUI methods directly from a `QThread`.
- Prefer modifying `自定义设置` and loading logic rather than hardcoding values elsewhere.

### Building / packaging
The EXEs are produced with **PyInstaller**. Key notes from the dev log:
- Having both PyQt5 and PyQt6 installed simultaneously breaks the PyInstaller build — use a clean venv with only PyQt6.
- The `--onefile` bundle uses `sys._MEIPASS` for asset paths; `路径修正()` handles this.
- The `文件/内嵌内容/` folder is copied to the user's working directory on first run via `召出内容()`.

### Dependencies (Python)
```
PyQt6
pynput
mouse
```
Standard library: `sys`, `os`, `shutil`, `random`, `ctypes`, `socket`

### Running from source
```bash
python 天王寺科学忍具V4.8.py
```
- Requires Windows (uses `ctypes.windll`, Win32 DPI APIs, and screen capture APIs).
- Game emulator should be run in fullscreen at one of the supported resolutions (1920×1080, 2560×1440, 3840×2160).
- Place a `配置.txt` next to the script to override resolution-specific coordinates.

---

## Important Constraints for AI Assistants

1. **This is a Windows-only tool** — do not suggest cross-platform abstractions.
2. **Chinese identifiers are intentional** — preserve them; do not translate variable/function names.
3. **Single-file architecture** — the entire application is one `.py` file by design.
4. **Configuration via dict override** — all coordinate tuning belongs in `自定义设置` or external `配置.txt`, not hardcoded in logic.
5. **GUI thread safety** — any background detection or keyboard listener code must communicate back to the main window via `pyqtSignal`/`pyqtSlot`, never direct method calls.
6. **Asset paths** — always route asset paths through `路径修正()` to support both source and PyInstaller execution contexts.
7. **The `诫训.txt` mechanism** — this file is executed at startup. Do not remove or bypass it.
8. **Version history** — the `V4.5历史版/` folder is an archive; do not modify it when updating the main tool.
