# Moon 系统规范（SPEC）

> 状态：CURRENT（SL-5 / SL-9.10）。

## 职责

- `MoonProgress`：每晚合格判定（时长 ≥ 420min 且入睡 ≤ 目标 + 30min）、成长 +20%、满月奖励重置。
- `MoonLife`：五阶段、五情绪、事件池、累计满月奖励、近因熬夜次数。
- 月亮观察（SleepDetective 报告）为 Moon/侦探域输出，仅供展示。

## 依赖

- 消费 `SleepResult`（合格判定输入，来自 Sleep Data）。
- **不得依赖 Personality 内部数学**；未来如需人格类型展示，只消费 `PersonalityResult`。

## 不得负责

- ❌ Personality classification
- ❌ Bubble physics
- ❌ Clock rendering

## MIGRATION REQUIRED

- CURRENT：`MoonProgress`/`MoonLife` 与 session 存储同在 `ShuilemeRepository`/`ShuilemeState`（God State，K-5）。
- TARGET：Moon 状态独立于 Sleep Analysis 之外（同 DataStore，域外观分离）。
