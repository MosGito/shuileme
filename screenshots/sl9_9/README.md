# SL-9.9 拖拽命中区域修正 · 复验报告

> 日期：2026-08-21
> 环境：SleepShift_Accept（Pixel, API 36, emulator-5556）
> 基线：SL-9.8.1（未提交）→ SL-9.9 修改未提交
> 构建/测试：`assembleDebug` + `testDebugUnitTest` ✅

## 目标（不改物理系统）
Emoji 拖拽逻辑本身正常，问题是触摸 hitbox 太小且命中区域偏移到 emoji 上方。
只修复交互命中层，不重构 PersonaBubblePhysics / 漂浮 / 碰撞 / 边界。

## 修复内容

### 气泡拖拽命中（帧级稳定坐标系）
- **命中中心修正**：`marginPx + b.x*density + emojiHalfPx(30px)` —— 以 emoji **渲染中心**为命中中心（原用 top-left，偏移到 emoji 上方）
- **命中半径扩大**：80px → **90px**（≈3x emoji 半径 ~29px）
- **拖拽公式**：`bubblePosition = bubbleStart + delta/density`（delta = currentPointer - pointerDown，帧局部稳定坐标）
- **物理不改**：失重漂浮 / 碰撞 / 边界 / 松手惯性保持

### 齿轮拉绳命中
- hitbox 从 44px → **80px**，中心对齐齿轮中心（+31px 校正）
- 绳仍为 Canvas 二次贝塞尔，拖动用 delta，松手 spring 回弹，超阈值开设置

## 验证（真实触摸操作）
| 测试 | 结果 |
|---|---|
| 从 emoji 偏左 70px 按下开始拖拽 | ✅ hit=5（命中成功） |
| 从 emoji 偏上 40px 按下拖 +200px | ✅ physics 确认气泡移动 +76dp（=200px） |
| 松手后失重漂浮 | ✅ 未改物理 |
| 齿轮拖拽开设置 | ✅ 扩大 hitbox 后 swipe 正常 |

## SL-9.9 APK
- 路径：`D:\AndroidDev\Projects\SleepShift\app\build\outputs\apk\debug\app-debug.apk`
- 大小：13,889,790 字节；SHA-256 `3ebb462e...`；构建 2026-08-21 23:24

## 真机重点测试
1. emoji 周围 ~50dp 任意位置可开始拖动
2. 拖动中 emoji 中心跟随手指无偏移
3. 松手恢复失重漂浮
4. 齿轮拉绳手感（扩大 hitbox 后）
