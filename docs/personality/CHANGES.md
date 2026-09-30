# Personality — 变更历史与冻结声明

> **FROZEN SPECIFICATION**：`docs/personality/SLEEP_PERSONALITY_SYSTEM_V2.md`
> （V2.0.7，1181 行，SHA-256 `BCC98655024FF5628D0BD4653A42A752BA91FA759695B6AFD71FB738271305BF`）
> 为唯一数学与逻辑规范，**其他系统不得修改**；实现不得为迁就代码修改规范。

## 规范修订历史

- **V2.0.5**：WOLF Gate 数学修正（μ_C 移出 Gate，daytimeOnsetRatio 唯一核心指标）。
- **V2.0.6**：daytimeOnset 三态修正（TRUE/FALSE/UNKNOWN；UNKNOWN ≠ FALSE）。
- **V2.0.7**：最终数学闭环（C 夜间弧、w=2/3、minMembershipScore=0.25、C undefined 下游契约、effectiveSessions、δ 校准措辞）→ IMPLEMENTATION READY。

## 实现阶段

- **PHASE 1（Feature Layer）**：ChronotypeState（Value/OutOfDomain/MeanUndefined）、夜间弧、chronotypeScore、durationScore、三分量 R（undefined 传播）、daytimeOnset 三态、SleepBehaviorProfile、effectiveSessions、completeness。
- **PHASE 2（Definition Layer）**：PersonalityId（12）、PersonalityDefinition（17 字段）、PersonalityRegistry（10 Base + 2 Gate，priority 1–12，minMembershipScore=0.25，transitionTargets 邻接表，init 自校验）。
- **PHASE 3（Membership Layer）**：mAxis(w=2/3)、mC/mD/mR、S_i=mC×mD×mR、deterministicArgmax（Score→priority→stable ID，exact tie）、Special Gate 不参与评分、undefined 无 fallback。

## 当前状态

- PHASE 4+（Matcher / PersonalityResult / Confidence）为 implementation work；**不得修改规范以迁就实现**。
- 架构债务：K-1/K-2/K-3/K-4 与本域直接相关（见 `docs/architecture/SYSTEM_MAP.md` §8）。

## PHASE 5（validation & hardening）

- **§23 全量映射验收**：52 个 sanity case 全部有可定位自动化测试（实现/测试见 `docs/architecture/SYSTEM_MAP.md` 与各测试类）。
- **边界加固测试（+11）**：effectiveSessions n=0/1/4→insufficient、n=5/6→正式判定；n<5 不触发任何 Gate；R 边界 0.33（严格小于）与 0.0/1.0；WOLF>CHAMELEON 控制流优先级；`isTransitioning` 的 0.15 精确比较（`Math.nextDown/nextUp`，无 epsilon）；argmax 输入顺序无关；CHAMELEON 文案不声称"每天随机"；Confidence n<5 仍按公式（Matcher 门槛）与 completeness 单调性。
- **API 可见性收紧**：对外 Public 仅 `PersonalityMatcher` / `PersonalityResult` / `PersonalityId` / `PersonalityDefinition` / `ChronotypeState` / `DaytimeOnsetState` / `SleepBehaviorProfile`；`MembershipMath`/`MembershipScore`/`Confidence`/`PersonalityRegistry`/`PersonalityConstants`/内部数学函数改 `internal`；`PersonalityMatcher.isTransitioning` 为 internal 纯函数。
- **边界审查**：personality 包外部依赖仅剩 `SleepPersonalityEngine` + `SleepSession`（K-3，已知）；无 Android/Compose/UI/Repository/Bubble/Moon/Clock/Social/Reminder 依赖。
- **Known Risks**：K-3 = OPEN（Personality Domain 尚未完全脱离旧人格引擎的 circular 原语；推荐未来独立 Phase 将 circular statistics 下沉 shared/domain/statistics 纯 Kotlin 层）；K-1/K-2/K-4 = DEFERRED（PHASE 6 集成关闭）；旧 SleepPersonalityEngine 仍为线上路径，未退役。
- **测试结果**：personality 专项 108 例通过；全量 253/253 通过，零回归。
