# Clock 系统规范（SPEC）

> 状态：CURRENT（SL-2 / SL-3 / SL-9）。

## 职责

- `VirtualClockEngine`：`virtualNow = realNow + offsetMin·60000`；偏移范围 0~240（calibration parameter）。
- 目标睡眠窗口内偏移、窗口外显示真实时间（SL-9 窗口化语义）。
- App 内虚拟时钟渲染、Glance Widget（2x1/4x2）、未来 Dynamic Island / Live Activity。

## 依赖

- 消费 Sleep Data 的 offset/target 配置（只读）。
- **不得依赖 Personality**（人格数学或 Result 均不进入 Clock 计算）。

## Public API

- `VirtualClockEngine`（纯 Kotlin）、`ShuilemeWidgets`/`ShuilemeWidgetDisplay`（Widget 渲染）。

## MIGRATION REQUIRED

- CURRENT：`ShuilemeWidgetDisplay.statusLine` 曾含虚拟感知时长（已修，见 CHANGES）；`SleepCapsule`/Widget 直接依赖 `MainActivity`（K-8）。
- TARGET：Widget/胶囊经 Intent 常量/Deep link 打开应用。
