# Sleep Data & Analysis — 变更历史

> 只追加，不倒写；系统级变更在此记录，项目级变更见 `docs/project/DEVELOPMENT_LOG.md`。

## 2026-08-26（PHASE 1，V2.0.7 特征层）

- 新增 `personality/SleepBehaviorProfile`：μ_C/μ_W/μ_D/σ 系列/三分量 R（undefined 传播）/completeness/daytimeOnset 三态。
- 新增 `effectiveSessions` 定义（= 有效 onset 会话数，Formal 门槛 = 5）。
- 修复语义：mean null → R=undefined（不再自动置 0）。

## 2026-08-26（已睡时长 Bug 修复，跨系统）

- `SleepModels.kt` 新增 `realSleepElapsedMin`：睡眠时长展示严格基于真实时间，不受虚拟偏移影响。

## 历史（SL 阶段，来自项目日志）

- SL-2：`SleepSession`/`SleepState` 数据模型、DataStore 睡眠记录。
- SL-4.5：OnboardingState（target/current 作息）持久化。
- SL-7：SleepDetectiveData（弱信号）接入。
- SL-9.10：双睡眠目标（currentSleepTime/currentWakeTime 4 键）。
- STEP 3：OnboardingState 恢复读取 current 作息。
