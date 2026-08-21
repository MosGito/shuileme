# SL-9.6 真机体验修正 · 复验报告

> 日期：2026-08-21
> 环境：SleepShift_Accept（Pixel, API 36, emulator-5556）
> 基线：4374925 → 本次修改未提交
> 构建/测试：`assembleDebug` + `testDebugUnitTest` ✅

## 七模块完成状态

| # | 模块 | 结果 | 验证 |
|---|---|---|---|
| M1 | 时间滚轮选择器重构 | ✅ | 原生 NumberPicker（AndroidView）：中央值=最终值、滚动吸附、0-23/0-59 循环；滚动到 1时 → 提交 1:00 |
| M2 | 全局字体颜色 | ✅ | 新建 NightButton（暗琥珀底+近白字），移除 Material 默认 onPrimary 深色文字；全页近黑像素=0 |
| M3 | 初始月亮复用 | ✅ | 共享 NightMoon（光晕+呼吸波纹+月亮本体），引导 step5 与主页同一组件；点击→波纹→无缝进主页 |
| M4 | 气泡改失重漂浮 | ✅ | 移除重力/传感器；气泡随机散布全窗框（y336-1222）+ 持续漂浮；拖动 delta 跟随无半屏偏移；FloatDriftTest 4 项 |
| M5 | 齿轮拉绳小夜灯 | ✅ | Canvas 二次贝塞尔绳（非字符"|"）；齿轮跟随手指、绳弯曲拉长、松手弹簧回弹；下拉超阈值打开设置 |
| M6 | 删除人格卡入口 | ✅ | 主页人格名不可点击、设置面板无「人格卡片」按钮；数据模型保留 |
| M7 | 月亮观察返回按钮 | ✅ | statusBarsPadding + 更多间距，按钮下移至安全区（中心Y 147） |

## 关键修复点

### M1 时间滚轮（原生方案）
- 弃用自定义 Compose LazyColumn 滚轮（offset/index 映射误差无法根治）
- 改用 `android.widget.NumberPicker`（AndroidView 嵌入）：原生保证高亮值=最终值、自动吸附、wrapSelectorWheel 循环
- 深色主题：浅色文字 + 半透明中央高亮条 + 细分隔线
- 验证：小时/分钟列滚动正常，提交值 = 中央显示值

### M4 失重漂浮物理
- `PersonaBubblePhysics.step(dt)` 去掉 gravityX/gravityY，改为 elapsed 驱动的正弦漂移（纯函数确定性）
- 保留四壁反弹 + 气泡碰撞推开 + 边界钳制
- 拖动改用 delta 增量（`b.x + amount.x`），杜绝 local/window 坐标混用的半屏偏移
- 删除 `resolveGravity`、`GravitySensor` 依赖、重力 toast UI；相关测试重写为 FloatDriftTest

### M5 齿轮拉绳
- Canvas `Path.quadraticTo` 二次贝塞尔绳，上端接屏幕外、下端接齿轮
- 齿轮通过 `detectDragGestures` 跟随手指（dragX/dragY），spring 动画松手回弹
- 控制点随下拉弯曲（弹性绳效果），无字符"|"

## 复验截图（screenshots/sl9_6/）
01 原生时间选择器 / 02 滚动后 / 03 欢迎按钮浅色字 / 04 月亮相遇页（复用月亮）/ 05 点击波纹 / 06 气泡散布 / 07 齿轮绳 / 08 报告返回按钮 / 09 主页最终

## 改动文件（11 个）
- 新增：`components/NightMoon.kt`、测试 `model/FloatDriftTest.kt`
- 修改：ShuilemeHomeScreen（气泡失重/齿轮绳/去卡片入口）、ShuilemeOnboardingScreen（月亮复用/按钮）、TimeScrollPicker（原生重写）、NightTheme（NightButton）、PersonalityCardScreen、SleepCaseReportScreen、MainExperience（物理）、2 个测试
- 删除：GravityInteractionTest、GravitySensorTest
