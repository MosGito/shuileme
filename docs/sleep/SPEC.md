# Sleep Data & Analysis 系统规范（SPEC）

> 状态：CURRENT（部分）→ TARGET。本系统负责**产生睡眠事实与分析结果**。
> 上游：用户输入（onboarding current/target 作息）、真实入睡/醒来记录。

## 职责

- 存储与维护 `SleepSession`（真实时间基准：`sleepStartAtMs` / `wakeAtMs` / `effectiveOffsetMin`）。
- 定义 `effectiveSessions`（具备有效 `sleepStartAtMs` 的会话数，= 有效 onset 会话数）。
- 计算统计：μ_C（入睡环形平均）、μ_W（起床环形平均）、μ_D（时长算术平均）、σ_circ_onset / σ_circ_wake / σ_dur。
- 计算 `R = 0.5·r_onset + 0.3·r_wake + 0.2·r_dur`（无 wake → R = r_onset；σ_onset 不可算 → R = undefined，不置 0）。
- 计算 `completeness`（含 wakeAtMs 会话数 / effectiveSessions）。
- 计算 `daytimeOnsetRatio` 与 `daytimeOnset` 三态（TRUE/FALSE/UNKNOWN）。
- 产出 `SleepBehaviorProfile`（与 Personality 的输入契约）。

## 关键定义

- `μ_D`：`durations.average().toLong()`（非环形变量；禁止混入 median / ratio / 离差）。
- `daytimeOnsetRatio`：日间窗口 `[09:00, 18:00)` 有效 onset 占比；`0.60` 与 `≥5` 为 calibration parameters。
- `SleepBehaviorProfile`：`effectiveSessionCount / meanOnsetMin / meanWakeMin / meanDurationMin / σ* / regularity(Double?=undefined) / completeness / chronotype / daytimeOnsetRatio / daytimeOnset`。

## 不得负责

- ❌ Personality classification
- ❌ Bubble physics
- ❌ Moon growth
- ❌ Clock rendering
- ❌ UI

## 依赖

- 唯一写者：`ShuilemeRepository`（DataStore）。
- 可被读取：Personality（Profile）、Moon（合格判定输入）、Clock（offset 配置）、Reminder、Social（卡片统计）、UI（只读 State）。

## MIGRATION REQUIRED

- CURRENT：统计散落于旧 `SleepPersonalityEngine.computeMetrics`（onset-only R）；`SleepBehaviorProfile`（新）依赖旧引擎 circular 原语（K-3）。
- TARGET：统计归 `domain/sleep`，circular 原语下沉共享统计层或新域自持（二选一）；旧 `computeMetrics` 退役。

## CHANGES

见 `docs/sleep/CHANGES.md`。
