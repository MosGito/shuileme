# SleepShift 架构决策记录（DECISIONS）

> 格式：ADR-xxx；状态：Proposed / Accepted / Implemented / Deprecated。
> 修改：架构决策只能由架构变更追加，禁止业务功能顺带改写。

---

## ADR-001 Personality Domain 独立化

- **Status**：Accepted（PHASE 1–3 已落地）
- **Context**：旧 `SleepPersonalityEngine` 把分类/置信度/进化/隐藏/冷启动揉在一起，UI 与 Social 直接调用内部算法。
- **Decision**：建立独立 `domain/personality`（V2.0.7 数学系统），内部算法封闭，外部只经 `PersonalityResult`/公开 API 消费。
- **Consequences**：新增人格不改外部契约；UI/Social/Bubble 只读 Result。
- **Current Implementation**：`model/personality/`（PHASE 1–3：Profile/Definition/Registry/Membership）；旧引擎仍为线上路径（K-1/K-2/K-4）。
- **Target State**：PHASE 4+ Matcher/Result/Confidence；PHASE 6 全量切换；旧引擎降级为兼容壳并退役。

## ADR-002 V2.0.7 作为唯一人格数学规范

- **Status**：Implemented（FROZEN）
- **Context**：此前多轮规范迭代存在旧参数残留风险（0.30 vs 0.25 等）。
- **Decision**：`docs/personality/SLEEP_PERSONALITY_SYSTEM_V2.md`（SHA-256 `BCC98655…05BF`，1181 行）为唯一数学/逻辑规范，FROZEN；实现不得为迁就代码修改规范。
- **Consequences**：实现↔规范冲突时以规范为准；规范变更必须走冻结评审。
- **Current Implementation**：规范已迁移至 `docs/personality/` 且字节一致。
- **Target State**：保持单一 FROZEN 副本，不建立第二份可编辑副本。

## ADR-003 Special Gate 不参与 Base argmax

- **Status**：Implemented（V2.0.7 §5.2/§10.7；PHASE 3 已按此实现）
- **Context**：WOLF/CHAMELEON 是 control-flow precedence，不是 Membership candidate。
- **Decision**：Gate 触发即定 Primary；禁止把 Gate 包装成 score 参与 argmax；Gate Primary 无 ClassificationMargin。
- **Consequences**：`MembershipMath` 只对 10 个 Base 定义评分；`deterministicArgmax` 不接受 Gate。
- **Current Implementation**：`MembershipMath.scoreAllBase` 过滤 Special Gate。
- **Target State**：PHASE 4 Matcher 严格按 §20.1 顺序执行。

## ADR-004 PersonalityResult 作为人格系统唯一对外结果契约

- **Status**：Accepted（PHASE 4 待实现）
- **Context**：现 UI/Social 消费旧 `SleepPersonalityState` 并自行调用引擎。
- **Decision**：所有消费者只读 `PersonalityResult`（primary/secondary/margin/isTransitioning/boundary/confidence/chronotype）。
- **Consequences**：UI/Bubble/Social/Widget 不再依赖人格内部；新增输出字段不影响消费者（只增不破）。
- **Current Implementation**：旧 `SleepPersonalityState` 仍被 VM/Card 使用（K-1/K-2）。
- **Target State**：PHASE 6 完成切换。

## ADR-005 UI 不得直接调用人格内部算法

- **Status**：Accepted（K-1/K-4 违反中）
- **Context**：`ShuilemeViewModel.personalityState`、`OnboardingScreen`、`SleepGoalEditor` 直接调用旧引擎。
- **Decision**：UI 层只允许通过 ViewModel 编排 + 消费 Result/API；禁止 import 人格数学内部。
- **Consequences**：人格数学变更不破坏 UI；冷启动基线走统一 API。
- **Current Implementation**：违规点 K-1/K-4。
- **Target State**：PHASE 6 关闭。

## ADR-006 Social 不得重新执行人格计算

- **Status**：Accepted（K-2 违反中）
- **Context**：`PersonalityCardActivity` 独立 `compute()` 且漏传 `sleepTargetDeviationMin`。
- **Decision**：Social 只消费 `PersonalityResult`；禁止自调 `PersonalityMatcher` 或旧 `SleepPersonalityEngine`。
- **Consequences**：卡片与主页结果强制一致；单一计算入口。
- **Current Implementation**：K-2。
- **Target State**：PHASE 6 关闭。

## ADR-007 Domain 不依赖 Compose / UI

- **Status**：Accepted（现状基本符合；K-3 例外）
- **Context**：Domain 层出现 import Android/UI 将破坏可测性与移植性。
- **Decision**：`domain/*` 禁止依赖 Compose/Widget/Activity；只允许纯 Kotlin。
- **Consequences**：纯 JVM 单测；Domain 可复用。
- **Current Implementation**：`model/personality/*` 纯 Kotlin ✅；`SleepBehaviorProfile` 依赖旧引擎 circular 原语（K-3，属旧引擎耦合而非 UI）。
- **Target State**：K-3 收敛后完全自洽。

## ADR-008 Legacy 冻结，不允许新系统依赖

- **Status**：Accepted（Implemented）
- **Context**：系统时区轨道（admin/data/engine/model/permission/strategy/time/notify/ui）已冻结。
- **Decision**：不新增业务；只允许 bug/security/build 兼容修复；新系统不得依赖；Legacy→shuileme 反向依赖视为迁移问题（K-6）。
- **Consequences**：`DebugActivity` 为唯一 Legacy 调试入口。
- **Current Implementation**：Legacy 代码保留；`WelcomeDialog` 反向依赖 shuileme 主题（K-6）。
- **Target State**：清理 K-6 后 Legacy 完全隔离。

## ADR-009 单一 DataStore 保留，但 Repository API 按 Domain 边界收敛

- **Status**：Accepted（K-5/K-9 待处理）
- **Context**：`ShuilemeRepository` 单类承载 6+ 域状态与写入。
- **Decision**：单一 DataStore 文件与单一写者保留；Repository 外观按域分组/注释契约，域间不互读内部 Keys。
- **Consequences**：状态一致性保持，域边界可见，未来拆域不影响存储。
- **Current Implementation**：K-9（God Repository）、K-5（God State）。
- **Target State**：PHASE 7+ 按域收敛。

## ADR-010 系统级 SPEC 与 CHANGES 分离

- **Status**：Implemented（本阶段建立）
- **Context**：此前仅一份项目日志与一份人格规范，系统级变更无处沉淀。
- **Decision**：每个系统一份 `SPEC.md`（冻结语义/契约）+ `CHANGES.md`（变更历史）；项目级 `project/DEVELOPMENT_LOG.md` 只记录阶段/架构/跨系统/版本/release/migration。
- **Consequences**：改 A 系统只追加 A 的 CHANGES，不得顺带改 B 的规范。
- **Current Implementation**：`docs/` 分层已建立。
- **Target State**：持续维护。

## ADR-011 未来 Personality V3/V4 通过内部替换保持 PersonalityResult 外部契约稳定

- **Status**：Proposed
- **Context**：人格体系需长期演进，不能因换代重写所有消费者。
- **Decision**：新版本只替换 `PersonalityDefinition`/`Matcher` 内部实现；对外 `PersonalityResult` 字段只增不破。
- **Consequences**：UI/Bubble/Social/Widget 不因人格换代而改动。
- **Current Implementation**：V2.0.7 为第一代实现。
- **Target State**：V3/V4 内部替换。

---

## 工作回执格式（标准，自本阶段起强制）

后续 Codex 工作回执必须包含：

- A. Phase
- B. Scope
- C. Files Changed
- D. Files NOT Changed
- E. Test Result
- F. Git Status
- G. Architecture Tree（ASCII 树状图）
- H. Dependency Changes
- I. Spec ↔ Implementation Changes
- J. Known Risks
- K. Next Recommended Phase
- L. Stop Point

