# SL-9.10 真机体验修正 · 复验报告

> 日期：2026-08-22
> 环境：SleepShift_Accept（Pixel, API 36, emulator-5556）
> 基线：SL-9.9（未提交）→ SL-9.10 修改未提交
> 构建/测试：`assembleDebug` + `testDebugUnitTest` ✅

## 模块验证结果（每项模拟器截图确认）

| # | 模块 | 结果 | 修改原因与验证 |
|---|---|---|---|
| M1 | 睡眠目标双时间段 | ✅ | 两组时间（目标理想 + 当前现实）；删除理想时长按钮改为自动计算展示（8 小时）；兼容旧数据（targetSleepTime/targetWakeTime 保留 + currentSleepTime/currentWakeTime 新增保存） |
| M2 | 时间选择大字号 | ✅ | NumberPicker 26→38sp，容器 140→180dp；滚轮数字为视觉主体；step3 加 verticalScroll 适配 4 滚轮 |
| M3 | 月亮文字引导 | ✅ | 文字偏移 130→100dp 上移，围绕月亮形成引导路径 |
| M4 | 恢复昨晚月亮观察 | ✅ | 醒后主页自动显示卡片（🌙昨晚月亮观察 · 🕵️ + observation）；点击进入报告详情；设置入口保持移除 |
| M5 | 设置睡眠目标二级菜单 | ✅ | 设置→「睡眠目标」→共享 SleepGoalEditor（与初始页同一组件）；不再直接显示目标时间按钮 |
| M6 | 睡眠人格模型扩展 | ✅ | 保存当前/目标入睡+起床时间（4 键）；结构可扩展 |
| M7 | 牛马随机单 emoji | ✅ | WORK_HORSE 每次随机 🐮 或 🐴，视觉长度统一 |

## 关键实现

### M1+M5 共享 SleepGoalEditor
- 新组件 `SleepGoalEditor`：目标睡眠时间段 + 当前睡眠时间段 + 理想时长自动计算展示
- 初始页 step3 与设置「睡眠目标」二级菜单共用同一组件（一次修改两处同步）
- 数据：`OnboardingState`/`ShuilemeState` 增 `currentSleepTime/currentWakeTime`；`setSleepGoal` 保存 4 键

### M4 月亮观察恢复
- 主页底部卡片（点击→报告详情）；报告数据（detectiveReport）逻辑保留
- 设置入口保持移除（此前误删整功能已恢复）

## SL-9.10 APK
- 路径：`D:\AndroidDev\Projects\SleepShift\app\build\outputs\apk\debug\app-debug.apk`
- 大小：13,892,505 字节；SHA-256 `36b339fc...`；构建 2026-08-22 00:15

## 真机重点测试（Redmi K80）
1. 初始页 step3 双时间段滚轮大字号可滚、value 准确
2. 设置→睡眠目标→编辑→保存→返回主页验证生效
3. 睡眠→唤醒→主页出现昨晚月亮观察→点击看详情
4. 牛马提醒单 emoji 长度统一
5. 全页面浅色字体、字号提升
