# SleepShift 系统地图（SYSTEM_MAP）

> 状态：CURRENT + TARGET 并存（明确区分，禁止混淆）。
> 用途：项目级架构规范；系统边界、依赖方向、违规清单与迁移队列的唯一地图。
> 维护：本文件属 Architecture 级文档，仅由架构变更驱动；修改需 Cross-System Review。

---

## 1. 当前真实代码树（CURRENT）

```
app/src/main/java/com/sleepshift
├── MainActivity.kt / SleepShiftApplication.kt / PersonalityCardActivity.kt / DebugActivity.kt
├── AlarmReceiver.kt / BootReceiver.kt / AppCapabilities.kt / WelcomeDialog.kt
├── [LEGACY 冻结轨道]
│   ├── admin/ engine/ permission/ time/ data/ model/ strategy/ notify/
│   └── ui/（home/config/mode/onboarding/debug/components/theme）
└── [睡了么活跃轨道]
    ├── data/           ShuilemeRepository (482) ← God Repository
    ├── engine/         VirtualClockEngine
    ├── model/          SleepModels / SleepPersonality(220) / Moon* / SleepDetective /
    │                   PersonalityCardModel / Resident* / MainExperience / OnboardingState
    │   └── personality/（V2.0.7：Chronotype/DaytimeOnset/Profile/Definition/Registry/Membership；Matcher 待 PHASE 4）
    ├── reminder/       Scheduler / Receiver / Notifier / SleepCapsule / Models / Template
    ├── ui/             HomeScreen(770) / OnboardingScreen(402) / ViewModel(216) / Report / Card / NightTheme / components
    ├── widget/         Glance Small/Medium/Display/Actions/Refresh
    └── GravitySensor.kt（无引用残留）
```

## 2. Target Architecture（目标，非当前）

```
com.sleepshift.shuileme
├── data/            （唯一写者；Repository API 按域收敛）
├── domain/
│   ├── sleep/       （SleepSession / 统计 / SleepBehaviorProfile）
│   ├── personality/ （V2.0.7 全量：… / Matcher / PersonalityResult / Confidence）
│   ├── bubble/      （物理 + 碎碎念；只消费 PersonalityResult）
│   ├── moon/        （成长 / 情绪 / 事件）
│   ├── clock/       （VirtualClock；App/Widget/Dynamic Island 共用）
│   ├── social/      （卡片生成 / 导出 / 分享）
│   └── reminder/    （调度 / 通知 / 胶囊）
├── ui/              （纯展示；ViewModel 只编排、不持有算法）
└── widget/
```

## 3. 系统边界（9 系统）

| 系统 | 职责 | 对外产物（Public API） |
|---|---|---|
| A. Sleep Data / Analysis | 睡眠事实存储与统计（session、effectiveSessions、μC/μW/μD、R、completeness、SleepBehaviorProfile） | `SleepBehaviorProfile`、只读 State |
| B. Personality | V2.0.7 分类（Chronotype/Duration/Regularity/Membership/Definition/Registry/Gate/Matcher/Result/Confidence/Boundary/Secondary/Transition） | `PersonalityResult`（唯一对外契约） |
| C. Bubble | 气泡物理/拖拽/漂浮/碎碎念 | 气泡表现状态 |
| D. Moon | 月亮成长/情绪/事件/奖励 | `MoonProgress`/`MoonLife` |
| E. Clock | 虚拟时间/偏移/App 时钟/Widget/未来 Dynamic Island | `VirtualClockEngine` |
| F. Social | 人格卡片/导出/分享 | `PersonalityCardModel` |
| G. Reminder | 提醒调度/通知/睡眠胶囊/模板 | 调度与通知 API |
| H. UI / Presentation | Compose 页面/主题/导航/手势 | Screen Composables |
| I. Legacy | 冻结时区轨道 | `DebugActivity`（仅调试） |

## 4. 依赖方向（Dependency Direction）

```
Sleep Data ──► Sleep Analysis ──► SleepBehaviorProfile ──► Personality ──► PersonalityResult
                                                                    ├──► Bubble
                                                                    ├──► Social
                                                                    └──► UI

Sleep Data ──► Moon ──► UI / Feedback
Sleep Data ──► Clock ──► (App Clock | Widget | Future Dynamic Island)
Sleep Data ──► Reminder ──► Notification
```

规则：`Data → Domain → UI`；Domain 之间只允许"消费公开 Result/API"；任何系统不得依赖 UI/Compose/Activity。

## 5. Dependency Matrix

图例：**A**=ALLOW；**R**=READ-ONLY；**I**=INTERFACE ONLY；**F**=FORBIDDEN；**?**=NEEDS REVIEW（MIGRATION REQUIRED）

| 依赖↓\被依赖→ | Sleep | Personality | Bubble | Moon | Clock | Social | Reminder | UI | Legacy |
|---|---|---|---|---|---|---|---|---|---|
| Sleep | — | F | F | F | F | F | F | I | F |
| Personality | R | — | I | F | F | I | F | I | **?**（现依赖旧引擎 circular 原语） |
| Bubble | F | R | — | F | F | F | F | I | F |
| Moon | R | F | F | — | F | F | F | I | F |
| Clock | R | F | F | F | — | F | F | I | F |
| Social | R | R | F | R | F | — | F | I | F |
| Reminder | R | **?**（未来如需人格文案 → I） | F | R | R | F | — | I | F |
| UI | R | **?**（现直调旧引擎 → 目标 I） | R | R | R | R | R | — | **?**（Debug 入口） |
| Legacy | F | F | F | F | F | F | F | F | — |

## 6. Public API Boundary

| 系统 | CURRENT（现状） | TARGET（目标） |
|---|---|---|
| Sleep | `ShuilemeRepository`（全公开）、`SleepBehaviorProfile.from` | 窄化：数据读写 API + 分析 API |
| Personality | `chronotypeScore/mAxis/score/deterministicArgmax`（PHASE 1–3） | `PersonalityMatcher.classify → PersonalityResult`（PHASE 4+） |
| Bubble | `PersonaBubblePhysics`/`ResidentTalkSystem` | 同左（消费 Result.type） |
| Moon | `MoonProgress.evaluate/applyResult` | 同左 |
| Clock | `VirtualClockEngine` | 同左 |
| Social | `PersonalityCardGenerator` | 同左（但禁自算人格，见 K-2） |
| Reminder | `ShuilemeReminderScheduler/Notifier/SleepCapsule` | 同左 |

## 7. Forbidden Dependencies

- ❌ UI/Activity → Personality 内部算法（K-1/K-4）
- ❌ Social → 自行执行人格计算（K-2）
- ❌ 任何新系统 → Legacy（K-6 属迁移问题）
- ❌ Domain → Compose/Widget/Activity
- ❌ Bubble/Moon/Clock → Personality 数学内部
- ❌ 跨域写共享可变状态

## 8. Current Violations（K 登记，登记 ≠ 本阶段修复）

| ID | 问题 | Severity | Current Code | Why It Matters | Target Resolution | Migration Phase |
|---|---|---|---|---|---|---|
| K-1 | `ShuilemeViewModel.personalityState` 直接调 `SleepPersonalityEngine.compute` | High | `SleepPersonalityEngine.compute(PersonalityInput(…))` | UI 持有算法；人格数学变更破坏 UI | 改 `Matcher.classify → PersonalityResult` | PHASE 6 集成 |
| K-2 | `PersonalityCardActivity` 重复执行人格计算（漏传 deviation） | High | `SleepPersonalityEngine.compute(PersonalityInput(…))`（Card 内） | 卡片与主页结果可不一致 | Card 消费统一 Result/API | PHASE 6 |
| K-3 | `SleepBehaviorProfile` 依赖旧 `SleepPersonalityEngine` circular 原语 | Medium | `import SleepPersonalityEngine; circularMeanMinutes/circularDistance` | 新域绑定旧引擎 | circular 原语下沉共享统计层（或新域自持，二选一） | PHASE 6 前 |
| K-4 | Onboarding / SleepGoalEditor 调用 `initialInclination` | High | `SleepPersonalityEngine.initialInclination(current…)` | UI 直调内部算法 | 冷启动基线走统一 API | PHASE 6 |
| K-5 | `ShuilemeState` 为 God State（9 域字段） | Medium | `data class ShuilemeState(…sessions/moonProgress/moonLife/reminderProfile/onboarding/detective…)` | 域间耦合、全量重放 | 按域拆 State 片段（单一 DataStore 保留） | PHASE 7+ |
| K-6 | Legacy `WelcomeDialog` 依赖 shuileme theme | Low | `import com.sleepshift.shuileme.ui.ShuilemeNight` | 冻结轨污染活跃轨 | 主题常量上提共享层 | PHASE 7+ |
| K-7 | shuileme UI 依赖 `DebugActivity` | Low | `ShuilemeHomeScreen → import com.sleepshift.DebugActivity` | UI 直连调试入口 | FeatureFlag / 外部路由 | PHASE 7+ |
| K-8 | Widget/Reminder 依赖具体 `MainActivity` | Low | `SleepCapsule/Notifier/Widgets → import com.sleepshift.MainActivity` | 跨系统依赖入口类 | Intent 常量 / Deep link | PHASE 7+ |
| K-9 | `ShuilemeRepository` 为 God Repository（6+ 域） | Medium | 25+ 方法、30+ Keys 单类 | 域边界不可见 | 按域外观收敛（共享 DataStore 委托） | PHASE 7+ |
| K-10 | `ShuilemeHomeScreen` 为 God Compose Screen（770 行） | Medium | 单 Composable 聚合 10+ 职责 | 难测难改 | 拆 `BubbleLayer/MoonArea/SettingsPanel/DetectiveCard` | PHASE 7+ |

## 9. Migration Queue

1. **PHASE 4–5**：Personality Matcher / Result / Confidence + 单测（不改规范）。
2. **PHASE 6**：集成——ViewModel/Card/Onboarding/GoalEditor 全部改消费 `PersonalityResult`/公开 API；K-1/K-2/K-4 关闭；K-3 收敛 circular 原语归属。
3. **PHASE 7+**：God State/Repository 按域收敛（K-5/K-9）；HomeScreen 拆分（K-10）；跨轨清理（K-6/K-7/K-8）。
4. 每阶段：测试 → assembleDebug → 实现↔规范双向审计 → 人工确认 →（审计通过后按纪律）commit。

## 10. Future Extension Points

- **Bubble 碎碎念**：消费 `PersonalityResult.type` + 文案池。
- **Moon Growth**：消费 `SleepResult`（合格判定），不依赖人格。
- **Dynamic Island Clock**：消费 Clock 域公开 API。
- **Widget**：消费 Clock + SleepData（只读）+ PersonalityResult（只读）。
- **Personality Card / Social**：消费 `PersonalityResult`（禁止自算，K-2）。
- **新睡眠分析指标**：扩展 Sleep Analysis（Profile 字段），不改 Personality 数学。
- **人格 V3/V4**：只替换 Personality Domain 内部（Definition/Matcher），对外保持 `PersonalityResult` 契约（ADR-011）。

