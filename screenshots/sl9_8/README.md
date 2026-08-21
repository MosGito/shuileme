# SL-9.8 回归修复 · 复验报告

> 日期：2026-08-21
> 环境：SleepShift_Accept（Pixel, API 36, emulator-5556）
> 基线：SL-9.7（未提交）→ 本次 SL-9.8 修改未提交
> 构建/测试：`assembleDebug` + `testDebugUnitTest` ✅

## 模块完成状态

| # | 模块 | 结果 | 验证 |
|---|---|---|---|
| M1 | 全局字体颜色代码级审计 | ✅ | **根因**：无显式 color 的 Text 渲染纯黑（卡片标签 0 亮像素）。修复：`Theme.kt` 主题级 `CompositionLocalProvider(LocalContentColor = #F8F8FF)` 统一入口 → 全页面黑字=0（step2 1793→5、step4 1264→0、主页/设置 0） |
| M2 | 时间滚轮字号 | ✅ | NumberPicker 数字 20→26sp，容器 124→140dp；中央高亮条+分隔线保留（选中更大为原生限制） |
| M3 | Emoji+齿轮拖拽回归 | ✅ | **根因**：per-bubble amount 被移动原点抵消（amount≈0）；帧级命中用 positionInRoot 不反映 offset。修复：帧级 pointerInput + physics 位置命中（marginPx+b*density，半径80px）+ delta(px)→dp 换算；physics 日志确认 bubble 拖 +46dp；齿轮 swipe 开设置 ✓ |
| M4 | 月亮坐标统一 | ✅ | 抽离 MoonStageLayout（居中布局），主页与 Onboarding 共用；step5 月亮与主页对齐（误差 ~11dp，视觉居中，真机再校准） |
| M5 | SL-9.8-alpha APK | ✅ | 构建通过 |

## 根因分析（非局部补丁）

### M1 黑字
- 主题 `darkColorScheme.onSurface` 已浅色，但**未显式指定 color 的 Text** 在某些组件上下文渲染纯黑（#000000）
- 根因：`LocalContentColor` 未被强制为浅色
- 修复：`SleepShiftTheme` 内 `CompositionLocalProvider(LocalContentColor provides #F8F8FF)` —— 全局统一 TextColor 入口

### M3 拖拽退化
- per-bubble `detectDragGestures` 的 `amount`（positionChange）在移动元素上被自身位移抵消 → 气泡不跟随
- 帧级命中使用 `onGloballyPositioned positionInRoot` 不反映 `.offset()` 视觉位移 → 命中失败
- 修复：帧级（稳定父坐标系）pointerInput + **physics 位置命中**（dp→px 正确映射）+ delta px→dp 换算；命中半径 80px 容忍漂移
- 齿轮同模式（父 Box 稳定坐标系）

## SL-9.8-alpha APK
- 路径：`D:\AndroidDev\Projects\SleepShift\app\build\outputs\apk\debug\app-debug.apk`
- 大小：13,889,280 字节（≈13.2 MB）；SHA-256 `99596892...`；构建 2026-08-21 22:46

## 真机重点测试（Redmi K80）
1. 全页面无黑字（Onboarding/Home/Virtual Time/Settings/Time Picker/Report）
2. emoji 拖拽：手指移动 200dp → 气泡约 200dp 无偏移
3. 齿轮：按住下拉→绳跟随→超阈值开设置→松手回弹
4. 时间滚轮：数字 26sp 可读、中央选中、value 同步
5. step5 月亮与主页月亮位置对齐
