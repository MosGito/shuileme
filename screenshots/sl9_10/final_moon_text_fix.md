# SL-9.10 最终视觉修正 · step5 月亮文字位置

## 修改文件
- `app/src/main/java/com/sleepshift/shuileme/ui/ShuilemeOnboardingScreen.kt`（仅 GoodnightStep）

## 修改原因
- 文字「今夜，向月亮道个晚安」「点击月亮进入主页」位置过低，与月亮重合，视觉层级错误
- 原布局：文字在 `align(Center).offset(y=100dp)`（月亮下方）
- 目标结构：标题（顶部）→ 提示（下面）→ 留白 → 月亮（中心）

## 修改内容（不改月亮组件/复用/点击逻辑/主页）
1. **文字组移到月亮上方**：`align(TopCenter).offset(y=80dp)`（固定偏移，不依赖屏幕剩余空间）
   - 第一行「今夜，向月亮道个晚安」：30sp 加粗，月光黄 Accent
   - 第二行「点击月亮进入主页」：20sp，TextSecondary alpha 0.7
   - 初步倾向：18sp Accent alpha 0.9 / 16sp TextSecondary
2. **移除 step5 NightMoon 残留的 `offset(y=21.dp)`**：全屏 MoonStageLayout 已使月亮居中，多余偏移导致与主页差 21dp

## 截图验证结果（Pixel 模拟器）
| 检查项 | 结果 |
|---|---|
| ① 两页月亮视觉一致 | ✅ step5 月亮 (541,961) = 主页月亮 (541,961) |
| ② 文字不遮挡月亮 | ✅ 文字 y=329/449，月亮 y=961，留白 ~380px |
| ③ 点击月亮进入主页 | ✅ |
| ④ 不影响主页布局 | ✅ 主页月亮/状态/人格均正常 |

截图：`screenshots/sl9_10/06_step5_text.png`（step5 文字上方）、`07_home_after.png`（主页）

## 新 APK
- 路径：`D:\AndroidDev\Projects\SleepShift\app\build\outputs\apk\debug\app-debug.apk`
- 大小：13,892,480 字节；构建 2026-08-22 00:32

## 未提交 git
保持 SL-9.10 未提交状态，供真机测试。
