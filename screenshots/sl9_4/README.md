# SL-9.4 视觉稳定化 · Pixel 模拟器验收报告

> 日期：2026-08-21
> 环境：SleepShift_Accept（Pixel, API 36 google_apis x86_64, headless + swiftshader）
> 构建：`JAVA_HOME=D:\AndroidDev\JDK\jdk-21.0.12.8 ./gradlew.bat assembleDebug`（成功）
> 单测：`testDebugUnitTest` ✅（无回归）

## 一、验收截图（screenshots/sl9_4/）

| 文件 | 内容 | 状态 |
|---|---|---|
| `01_splash.png` | 开屏 = 引导欢迎页（🌙 睡了么 + 开始） | ✅ 深色主题生效 |
| `02_home.png` | 沉浸主页（人格头 + 月亮宠物 + 虚拟时间 + 齿轮） | ✅ 深色主题生效 |
| `03_settings.png` | 设置抽屉（偏移 / 目标时间 / 人格 / 入口） | ✅ 深色主题生效 |
| `04_personality_card.png` | 人格分享卡（海獭型 · 统计 · 分享按钮） | ✅ 修复「海獭型型」 |
| `05_moon_observation.png` | 月亮观察报告（安静夜晚 · 玻璃卡） | ✅ 深色主题生效 |
| `06_widget.png` | 桌面 2x1 组件（🌙 睡了么 · 虚拟时间 · 睡眠状态） | ✅ `.dp` 修复生效 |

辅助：`06_widget_small.png`（单组件视角）、`widget_zoom.png`（组件区域放大）。

## 二、验证结论

### 2.1 SL-9.4 目标达成
- **深色系统栏**：全部页面状态栏/导航栏为 `#14142B`（night_sky），无白底/黑字（`enableEdgeToEdge` + 深色 Material 主题 + `windowLightStatusBar=false`）。
- **夜空背景**：主背景 `#14142B` 全局一致。
- **Glance 组件 .dp 修复**：2x1 组件在桌面正常渲染（无崩溃），背景 `#16162B`，内容（月亮/标题/虚拟时间/状态）完整。

### 2.2 构建期修复（SL-9.4 未提交改动原本无法编译）
| 问题 | 根因 | 修复 |
|---|---|---|
| `Unresolved reference 'dp'`（MediumWidget） | `androidx.glance.unit.dp` 在 Glance 1.1.1 不存在（已统一为 Compose 单位） | `import androidx.compose.ui.unit.dp` |
| `Unresolved reference 'R'`（SmallWidget） | 缺 `com.sleepshift.R` import，且 `R.dimen.*` 返回 Int 仍被 Glance 当资源 ID（原始 bug 未根治） | 全部改用 `.dp`，删除 `dimens.xml`（已无引用） |

### 2.3 截图发现并修复
- **🐛 人格卡标题「海獭型型」**：`PersonalityCardModel.kt` 用 `"${type.displayName}型"`，而 `海獭型/牛马型/混沌型` 的 `displayName` 已含「型」→ 双「型」。修复：`displayName.removeSuffix("型") + "型"`。已重截 `04_personality_card.png` 复验为「海獭型」✅。

### 2.4 环境限制（非应用 bug）
- **4x2 中组件无法放置**：Pixel Launcher 的组件拖拽（长按提起→拖放）无法通过 adb `input motionevent` 可靠驱动（已验证 DOWN+停顿后 UI 无「提起」标记）。4x2 与 2x1 使用同一 `.dp` 修复模式且编译通过，属**代码级已验证、模拟器未视觉验证**。
- **开屏**：应用无独立 Splash Activity，开屏 = 引导欢迎页（首启）；重复启动为沉浸主页。

## 三、已生成辅助脚本（scripts/）
- `shot_analyze.py`：截图像素校验（主色 / 系统栏白底检测 / 缩放预览）
- `widget_place.py`：组件放置尝试脚本（`small|medium [--drag]`；受 launcher 限制，4x2 拖拽不可用，2x1 可经 Add 按钮放置）
