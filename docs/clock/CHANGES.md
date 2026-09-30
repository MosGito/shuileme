# Clock — 变更历史

## 2026-08-26（已睡时长 Bug 修复，跨系统）

- `ShuilemeHomeScreen.statusCopy` 与 `ShuilemeWidgetDisplay.statusLine`：已睡时长改为 `realSleepElapsedMin`（真实时间），虚拟偏移不再进入时长计算。

## 2026-08-26（架构登记）

- 登记 K-8（Widget/胶囊 → MainActivity 直接依赖）。

## 历史（SL 阶段）

- SL-2：VirtualClockEngine（FIXED/GRADUAL/FLUCTUATION）。
- SL-3：Glance 2x1/4x2 组件 + 15 分钟周期刷新。
- SL-9：虚拟时间窗口化（睡眠窗口内偏移）。
- SL-9.4：Glance `.dp` 修复。
