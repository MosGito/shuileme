# SleepShift / 睡了么 项目状态（PROJECT_STATUS）

> 本文件只描述**当前状态**。历史开发记录以 `docs/project/DEVELOPMENT_LOG.md` 为唯一长期日志（本文件不复制历史）。
> 系统级规范与变更见 `docs/architecture/SYSTEM_MAP.md`、`docs/*/SPEC.md`、`docs/*/CHANGES.md`。

---

## 1. 项目定位（当前）

- 产品：**睡了么**（包名 `com.sleepshift`）——虚拟时间 + 心理暗示 + 娱乐化睡眠陪伴；纯计算，不碰系统时间/时区。
- 版本：versionName `0.3.25` / versionCode `2`（tag `v0.3.25-alpha`；HEAD `276c222` 为产品代码基线，本地 master 落后 origin 2 个 README 提交）。
- 历史沿革：由「系统时区偏移工具 SleepShift」转型；系统时区轨道已冻结（见 `docs/legacy/FROZEN.md`）。

## 2. 人格系统状态（V2.0.7）

| 项 | 状态 |
|---|---|
| V2.0.7 人格规范（SLEEP_PERSONALITY_SYSTEM_V2.md） | ✅ **PASS / IMPLEMENTATION READY / FROZEN**（1181 行，SHA-256 `BCC98655…05BF`） |
| PHASE 1 Feature Layer | ✅ PASS（Chronotype/DaytimeOnset/BehaviorProfile/R 三分量，20 测试） |
| PHASE 2 Definition Layer | ✅ PASS（PersonalityDefinition/Registry 12 定义，11 测试） |
| PHASE 3 Membership Layer | ✅ PASS（MembershipMath/deterministicArgmax，34 测试） |
| 全量单测 | ✅ 210/210 通过 |
| 当前下一阶段 | **PHASE 4：PersonalityMatcher + PersonalityResult + Confidence**（未开始） |

## 3. 当前未提交修改（均为已批准工作，不得回退）

```
M  PersonalityCardActivity.kt          （STEP 3 冷启动 current 化）
M  ShuilemeRepository.kt               （STEP 3 OnboardingState 恢复）
M  SleepModels.kt                      （已睡时长 Bug 修复）
M  SleepPersonality.kt                 （STEP 2 环形统计）
M  ShuilemeHomeScreen.kt               （已睡时长 Bug 修复）
M  ShuilemeOnboardingScreen.kt / SleepGoalEditor.kt （STEP 3）
M  ShuilemeWidgetDisplay.kt            （已睡时长 Bug 修复）
M  PersonalityCardTest.kt / SleepPersonalityTest.kt / ShuilemeWidgetDisplayTest.kt
?? app/src/main/java/com/sleepshift/shuileme/model/personality/   （PHASE 1–3 新增）
?? app/src/test/java/com/sleepshift/shuileme/model/personality/   （PHASE 1–3 测试，77 例）
?? app/src/test/java/com/sleepshift/shuileme/model/SleepDurationTest.kt
?? docs/                                                         （PHASE 0 文档树）
```

## 4. 当前架构债务（登记，未修复）

| ID | 债务 | 目标阶段 |
|---|---|---|
| K-1/K-2/K-4 | UI/Social 直接调用旧人格引擎 | PHASE 6 |
| K-3 | SleepBehaviorProfile 依赖旧引擎 circular 原语 | PHASE 6 前 |
| K-5/K-9 | God State / God Repository | PHASE 7+ |
| K-6/K-7/K-8 | 跨轨/跨系统直接依赖 | PHASE 7+ |
| K-10 | HomeScreen God Compose | PHASE 7+ |

> 详见 `docs/architecture/SYSTEM_MAP.md` §8。

## 5. 文档体系（PHASE 0 已建立）

```
docs/
├── architecture/  SYSTEM_MAP.md · DECISIONS.md（11 ADR + 回执格式）
├── personality/   SLEEP_PERSONALITY_SYSTEM_V2.md（FROZEN）· CHANGES.md
├── sleep/ bubble/ moon/ clock/ social/ reminder/   （SPEC.md + CHANGES.md）
├── legacy/        FROZEN.md
└── project/       DEVELOPMENT_LOG.md（唯一长期日志）· PROJECT_STATUS.md（本文件）
```

## 6. 下一步

1. **PHASE 4**：PersonalityMatcher（§20.1 唯一执行顺序）+ PersonalityResult + Confidence（V2.0.7 公式）。
2. PHASE 5：对应单测（§23 全量映射）。
3. PHASE 6：集成（关闭 K-1/K-2/K-4、收敛 K-3）。
4. 每阶段：测试 → assembleDebug → 实现↔规范双向审计 → 人工确认；**未审计通过前不 commit**。

