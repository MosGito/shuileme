# SL-9.7 真机体验修正 · 复验报告

> 日期：2026-08-21
> 环境：SleepShift_Accept（Pixel, API 36, emulator-5556）
> 基线：4374925 + SL-9.6（未提交）→ 本次 SL-9.7 修改未提交
> 构建/测试：`assembleDebug` + `testDebugUnitTest` ✅（19 测试文件）

## 七模块完成状态

| # | 模块 | 结果 | 模拟器验证 |
|---|---|---|---|
| M1 | 全局字体系统 | ✅ | 主题 on* 全浅色（统一 TextColor 入口）；NightButton 深底浅字；字号放大（标题36/正文22/辅助18）；主页近黑像素=0 |
| M2 | NightMoon 单组件复用 | ✅ | 引导 step5 与主页同一 NightMoon（🌑+同光晕 #22404C/#1B2E35 完全一致）；点击→波纹→无缝进主页 |
| M3 | Emoji 拖拽重写 | ✅ | 窗框级拖拽 + onGloballyPositioned 实际位置命中；拖 +200px → 气泡距目标 44px；delta=start+累积 |
| M4 | 齿轮拖拽重写 | ✅ | 父 Box 稳定坐标系；swipe 拖齿轮 300px 开设置；gearPosition=startGear+delta；spring 回弹 |
| M5 | 月亮交互 | ✅ | 波纹增强（5dp/alpha0.7）；呼吸波纹保留加亮；提示上移至月亮上方 83dp（目标 60-100）；蓄力 1.1s |
| M6 | 关闭月亮观察入口 | ✅ | 主页卡片与设置按钮删除；报告代码保留（供未来自动触发） |
| M7 | 控制台收起按钮 | ✅ | 左上小点默认低可见度；点按展开「调试控制台」→ 再点收起 |

## 关键实现

### M1 统一字体系统
- `Theme.kt`：`onPrimary/onSurface/onBackground/onSecondary/onTertiary/onError` 全部设为浅色 #F8F8FF，杜绝 Material 黑字
- `ShuilemeNight.TextTertiary` + `ShuilemeTextColors` 统一入口
- `ShuilemeTypography`：TITLE 36 / BODY 22 / CAPTION 18
- 字号映射应用到 4 个产品屏；step3 紧凑化（picker 高度 124dp）解决字号放大后溢出

### M2 月亮完全复用
- step5 月亮改为 `MoonStage.BABY.emoji` + 新月亮光晕参数（与 HomeScreen 一致）
- 与主页月亮像素级一致（同一 NightMoon）

### M3 Emoji 拖拽（根治偏移）
- 窗框级 `pointerInput`（稳定坐标系），`onGloballyPositioned` 记录气泡实际 root 位置做命中
- `delta = currentPointer - pointerDown`（帧局部稳定）；`bubblePosition = bubbleStart + delta/density`
- 移除 per-bubble 移动原点污染的旧方案

### M4 齿轮拉绳（根治鬼畜）
- 父 Box `pointerInput`（稳定坐标系），命中齿轮后 `gearPosition = startGear + delta`
- 绳按 gearPosition 动态绘制二次贝塞尔；松手超阈值开设置否则 spring 回弹

## SL-9.7-alpha APK
- 路径：`D:\AndroidDev\Projects\SleepShift\app\build\outputs\apk\debug\app-debug.apk`
- 大小：13,890,115 字节（≈13.2 MB）；SHA-256 `77be095c...`
- 构建时间：2026-08-21 22:10

## 真机重点测试列表（Redmi K80）
1. 全局字体可读性（所有页面无黑字、字号适中、全面屏不溢出）
2. 引导 step5 月亮与主页月亮视觉一致性
3. 气泡失重漂浮流畅度 + 拖拽 200dp 精确跟随 + 松手惯性
4. 齿轮拉绳手感（大幅拖动不鬼畜、松手回弹、下拉开设置）
5. 月亮点击波纹增强 + 提示位置 + 长按 1.1s 蓄力
6. 主页无月亮观察入口、无人格卡入口（普通用户不受影响）
7. 左上小点控制台展开/收起
