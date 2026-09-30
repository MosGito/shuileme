# 睡眠人格分类系统 V2.0.7 — 数学闭环修复版

> 文档状态：理论规范（Theory & Specification），未进入实现。
> 建档日期：2026-08-26
> 适用范围：睡了么（SleepShift）「睡眠人格系统」的未来实现。
> 关联代码现状：`SleepPersonality.kt`（V1 引擎，含 STEP 2 环形统计修复、STEP 3 冷启动数据源修复）。
> 本文件不包含任何 Kotlin 实现；所有数据模型均为理论定义。
> 版本说明：V2.0.1 仅修订数学定义与分类边界规范（区域/评分关系、中心坐标、Membership Score、Secondary 邻接约束、Transition、Confidence 校准、Duration 派生变量职责、动物语义定位），不改变 V2 的产品设计、12 个人格目录与五层分层原则。
> 版本说明（V2.0.2）：V2.0.2 是 V2.0.1 的规范性修订，**不重新设计**核心 C/D/R、Membership、Confidence 模型；主要修复：deterministic tie-breaking、region/personality count 术语、Membership 语义边界、ClassificationMargin 语义边界、Special-Gate Transition、daytimeOnsetRatio、CHAMELEON 用户文案。
> 版本说明（V2.0.3）：V2.0.3 **不改变 V2.0.2 的数学模型**（C/D/R、Membership、Confidence、minMembershipScore=0.30、ClassificationMargin=0.15、daytimeOnsetRatio=0.60、WOLF/CHAMELEON gate 条件均不变）。仅正式化 Special Gate 的 **control-flow precedence**（WOLF > CHAMELEON > Base Classification），消除实现层面的 Primary classification ambiguity。
> 版本说明（V2.0.4）：V2.0.4 是**最终实现可判定性审计版**，**不改变任何数学模型**（C/D/R、Membership、Confidence、minMembershipScore=0.30、ClassificationMargin=0.15、daytimeOnsetRatio=0.60、Gate 条件、tie-break 语义均不变）。仅消除实现歧义：Boundary 语义、Secondary eligibility、CHAMELEON Secondary 确定性、priority 赋值、SLOTH/OTTER 名义阈值 vs ramp、§20.1 唯一执行权威。
> 版本说明（V2.0.5）：V2.0.5 仅修正 **WOLF Gate 的数学语义**（P0 审计结论）：`daytimeOnsetRatio` 成为 WOLF Gate 的唯一核心指标，`μ_C ∈ [09:00,18:00)` 从 Gate 条件中移除（μ_C 仍用于非倒置用户的 C 坐标）。窗口 [09:00,18:00)、阈值 0.60、有效 onset ≥ 5、C/D/R/Membership/Confidence/Secondary/Margin 等全部其他模型均不变。
> 版本说明（V2.0.6）：V2.0.6 将 `daytimeOnset` 从布尔量改为**三态**（TRUE / FALSE / UNKNOWN-INSUFFICIENT），消除"n < 5 被编码为 FALSE"导致的 CHAMELEON 误触发风险。WOLF 仅接受 TRUE、CHAMELEON 仅接受 FALSE、UNKNOWN 不触发任何 Special Gate。C/D/R/Membership/Confidence/Secondary/Margin/transitionTargets 等全部其他模型均不变。
> 版本说明（V2.0.7）：V2.0.7 修复数学闭环（P0-A / P0-B / P1-C 拍板落地）：① C 的有效定义域收窄为**夜间弧** `[18:00,24:00) ∪ [00:00,09:00)`，日间窗口 `[09:00,18:00)` → `C = undefined`（CHRONOTYPE_OUT_OF_DOMAIN），消除 12:00 断裂；② Membership 半宽 `w = 0.5 → 2/3`，`minMembershipScore = 0.30 → 0.25`（覆盖 [−1,1]²，无几何 Boundary 空洞）；③ 定义 `C undefined` 的下游分类契约（WOLF → CHAMELEON → Boundary，禁止 C=0 / 最近中心 fallback；区分 CHRONOTYPE_OUT_OF_DOMAIN 与 CHRONOTYPE_MEAN_UNDEFINED；R 不因 C undefined 自动置 0）。另按 P1-D/E/F 统一 effectiveSessions、收紧 δ 措辞、修正 SLOTH/OTTER 名义语义与数量表述。人格目录与五层架构不变。

---

## 0. Abstract

本规范定义「睡了么」睡眠人格系统 V2.0.1 的理论基础、特征空间、数学定义、人格分类模型、人格成品目录与扩展机制。

V2.0.1 修订范围：明确"连续 Membership Region + 语义边界"；正式定义各人格评分中心 `centerC / centerD`；正式定义 Membership Score；限定 Secondary 为相邻人格；以 `ClassificationMargin` 取代 `BoundaryDistance`；重新校准 Confidence（消除早期饱和）；明确 Duration 派生变量职责；澄清动物语义为意象载体。

V2.0.2 修订范围：补充 Primary 判定的 deterministic tie-break（Score → priority → stable ID）；统一"9 个 Base C×D Regions / 12 个第一阶段 PersonalityDefinition"数量术语；明确 Membership Score 的语义边界（非概率/置信度）；正式定义 ClassificationMargin（Top-1 − Top-2 eligible adjacent）；区分 Geometric Transition 与 Special-Gate Transition；新增 daytimeOnsetRatio 强化 WOLF gate；修正 CHAMELEON 用户文案。

V2.0.3 修订范围：正式化 Special Gate 的唯一合法控制流（STEP 1 WOLF → STEP 2 CHAMELEON → STEP 3 Base scoring，触发即结束）；明确 Special Gate 不是 Membership Score 竞争者（禁止与 Base score 做 argmax / 比大小）；明确 Gate 优先级是 control-flow precedence 而非 score precedence；明确 Gate Primary 的 Secondary 仍取自 transitionTargets；明确 Gate Primary 的 ClassificationMargin = null、isTransitioning = false（方案 A，与"Gate 无 Membership Score"的现有模型一致）。

V2.0.4 修订范围：正式定义 Boundary / Insufficiently Classified 的输出语义（Primary/Secondary/Margin/isTransitioning 全部确定）；正式定义 Secondary 有效候选（∈ transitionTargets 且 S ≥ 0.30）；将 CHAMELEON Secondary 机制确定性化（transitionTargets = 全部 10 个基础定义 + 统一 tie-break）；为 priority 提供显式赋值（§7 目录序，数值大者优先）；明确 SLOTH/OTTER 名义阈值（R=0.6）与连续 ramp 的关系（仅 ramp 参与评分）；将 §20.1 伪代码扩展为完整执行流水线并声明为唯一执行权威。

V2.0.5 修订范围：WOLF Gate 数学审计结论落地——μ_C 与 daytimeOnsetRatio 同源于 onset 样本但语义不同（位置统计 vs 占比统计），联合 AND 会产生互相矛盾结果与假阴性（反例：ratio=0.60 时 μ_C≈07:42、ratio=0.80 时 μ_C≈08:40），且 circular mean 在多峰/昼夜混合数据下落入样本稀疏区（合成向量长度 R 低）。WOLF 产品语义为"大多数睡眠记录发生在白天"，由 ratio 直接度量；故 Gate 唯一化为 `daytimeOnsetRatio ≥ 0.60 且有效 onset ≥ 5`。

V2.0.6 修订范围：区分"统计值为假"与"证据不足"。`daytimeOnset` 三态化：TRUE = ratio≥0.60 且 n≥5；FALSE = ratio<0.60 且 n≥5；UNKNOWN / INSUFFICIENT = n<5。WOLF Gate 仅接受 TRUE，CHAMELEON Gate 仅接受 FALSE（且 R<0.33）；UNKNOWN 不触发任何 Special Gate。同时正式标记 n≥5 与 0.60 为 design / calibration parameter（非统计显著性、非高置信度、非概率），并消除其与 Confidence n/14 的潜在语义冲突。

V2.0.7 修订范围（数学闭环）：P0-A——C 定义域限定为夜间弧，日间窗口 C=undefined（不得强制映射 Early/Late/C=0）；P0-B——Membership 联合校准（w=2/3、minMembershipScore=0.25，覆盖证明 m_C≥0.5 ⇒ S≥0.25）；P1-C——C undefined 的下游分类契约（两种原因分类、WOLF→CHAMELEON→Boundary 路由、fallback 禁令、R 独立性）；P1-D——统一 effectiveSessions（Formal 门槛 = effectiveSessions ≥ 5）；P1-E——δ 改为产品校准阈值措辞；P1-F——SLOTH/OTTER 名义语义与 ramp 措辞统一；P2——数量表述统一为"9 Base Regions → 10 Base Definitions → +2 Special Definitions = 12 Definitions"。

核心命题（P0）：**Primary Sleep Personality 由长期真实睡眠行为推断，不由用户自选标签或目标作息决定。**

系统由五个概念层组成，严格分离：

1. **Behavior Layer**（真实睡眠行为统计）——人格类型的唯一判定依据；
2. **Goal Layer**（目标作息）——仅提供 Alignment / 偏差 / 辅助修饰；
3. **Observation Layer**（月亮观察等弱信号）——仅提供 Modifier / Confidence / Narrative；
4. **Gamification Layer**（连续、成长、月亮）——仅提供 Evolution / Achievement；
5. **Presentation Layer**（动物、emoji、文案）——人格的表现形式，不参与判定。

本规范满足：可解释性 > 趣味性 > 数量；新增人格不修改核心匹配算法；所有人格结论可追溯到数据。

---

## 1. Design Philosophy（设计哲学）

### 1.1 最高层产品原则

「睡眠人格不是用户希望自己成为怎样的人，而是系统根据用户实际怎样睡觉，逐渐认识用户是什么样的人。」

人格描述的对象是**可观测的睡眠行为**，不是愿望、不是自评、不是单晚事件。

### 1.2 信息优先级（由高到低）

| 层级 | 内容 | 对 Primary Personality 的作用 |
|---|---|---|
| L1 | 真实睡眠行为（入睡/起床/时长/稳定性/频率/趋势） | **唯一决定类型** |
| L2 | 行为的稳定性、频率与长期规律 | 决定类型与置信度 |
| L3 | 目标作息与现实行为的重合程度 | 仅 Alignment / Confidence / Narrative |
| L4 | 昨晚 / 月亮观察等弱信号 | 仅 Modifier / Confidence / Narrative |
| L5 | 连续性、成长、月亮等游戏化因素 | 仅 Evolution / Achievement |

### 1.3 三个必须分离的概念

- **Primary Personality**：回答"你是什么样的睡眠行为者"——由真实行为长期推断。
- **Achievement**：回答"你曾经做过什么"——行为里程碑（如连续 7 天早起）。
- **Modifier**：回答"你最近发生了什么"——近期弱信号（如昨晚夜间活跃异常）。

三者不得混用。任何候选人格若由 Achievement 或单晚 Modifier 直接触发，不得作为 Primary Personality。

### 1.4 与外部产品的边界

本系统借鉴的仅是"用动物化表达帮助用户理解自身睡眠行为"这一**产品表达范式**（该范式在多个公开睡眠产品中出现）。

本系统不复制任何外部产品的动物集合、命名、内部算法。本系统的动物、名称、emoji、语义推导链全部为独立设计，推导方法见 §9。核心差异：**真实行为 + 目标现实偏差 + 月亮观察 + 网络语义 + emoji 表达**五要素组合，且人格定义全部可量化。

---

## 2. Terminology（术语表）

| 术语 | 定义 |
|---|---|
| Sleep Session | 一次完整睡眠记录（`sleepStartAtMs`、`wakeAtMs`、`effectiveOffsetMin`），时间基准为真实系统时间 |
| Circular Mean | 对一天内分钟数（0..1439）的环形平均，见 §4.1 |
| Circular Distance | 两个分钟数在 1440 分钟圆上的最短距离，范围 0..720 |
| ChronotypeScore (C) | 睡眠相位标准化得分，范围 [-1, +1] |
| DurationScore (D) | 睡眠时长标准化得分，范围 [-1, +1] |
| RegularityScore (R) | 规律性得分，范围 [0, 1] |
| PersonalityVector (P) | 人格向量 P = (C, D, R) |
| PersonalityRegion | 特征空间中的区域，定义为坐标范围 + 可选约束 |
| Boundary State | 用户位于两区域边界附近的状态（primary + secondary + isTransitioning） |
| Cold Start Profile | 真实数据不足（effectiveSessions < 5，§11.1）时，由用户自报当前作息构成的基线 |
| GoalAlignment | 现实作息与目标作息的偏差度量，∈ [0, 1] |
| MoonObservationProfile | 月亮观察/侦探弱信号的聚合，仅进入 Modifier 层 |
| Primary Personality | 最终展示给用户的正式人格（唯一） |
| Secondary Personality | Primary 的**相邻人格定义**（transitionTargets）中 Membership Score 最高者；无有效相邻时为 null |
| ClassificationMargin | S_primary − S_secondary（分类得分差，∈ [0,1]）；值越小，Primary 与最接近的相邻人格越难区分 |
| Membership Score | 人格定义与用户特征向量的匹配度 S_i = m_C × m_D × m_R，∈ [0,1] |
| minMembershipScore | 进入正式分类所需的最低 Membership Score，默认 0.25（V2.0.7 起）；仅为"最低有效 Membership"判定阈值，不是概率或置信度百分比 |
| Boundary / Insufficiently Classified | S_primary < minMembershipScore 时的未充分分类状态 |
| effectiveSessions | 具备有效 `sleepStartAtMs` 的会话数（= 有效 onset sessions）；Formal Personality 的 minimum evidence，阈值 = 5 |
| CHRONOTYPE_OUT_OF_DOMAIN | μ_C 统计可定义但落在日间窗口 [09:00,18:00) 时的 C 状态（C = undefined，§4.3 / §4.9） |
| CHRONOTYPE_MEAN_UNDEFINED | circular mean 退化（合成向量长度 < 1e-6，§4.1）时的 C 状态（C = undefined，§4.9） |
| 9 个 Base C×D Regions | 由 C、D 语义边界确定的 9 个几何区域；其中 Neutral×Long 区域由 SLOTH / OTTER 两个 PersonalityDefinition 按 R 语义分裂 |
| 12 个第一阶段 Primary PersonalityDefinition | 10 个基础定义（8 个单定义区域 + Neutral×Long 区域的 SLOTH / OTTER 2 个定义）+ 2 个特殊定义（CHAMELEON、WOLF） |
| SLOTH / OTTER | 同一 Base C×D Region（Neutral×Long）内的 R 语义分裂定义，**不是**额外的 C×D 网格区域 |
| CHAMELEON / WOLF | Special Gates / Special Regions，不属于 C×D 网格区域 |

---

## 3. Feature Space（特征空间）

人格空间为三维：

```
      Duration (Y)
          长睡
           ↑
早睡 ←——————┼——————→ 晚睡   Chronotype (X)
           ↓
          短睡
```

- **X 轴 = Chronotype（睡眠相位）**：由环形平均入睡时间定义。
- **Y 轴 = Duration（睡眠时长）**：由算术平均/中位数时长定义。
- **Z 轴 = Regularity（规律性）**：由入睡/起床/时长的环形与普通离差定义；同时作为边界置信度与过渡机制。

X + Y 决定基础行为象限；Z 决定人格的稳定程度与边界判定。

### 3.1 维度定义

| 维度 | 核心变量 | 是否 circular | 数据来源 | 标准化 | 是否影响 Primary |
|---|---|---|---|---|---|
| D1 Chronotype | 环形平均入睡时间 μ_C、环形平均起床时间 μ_W | 是 | SleepSession.sleepStartAtMs / wakeAtMs | 是（C） | 是 |
| D2 Duration | 平均时长 μ_D（D 的唯一输入）；中位时长、时长离差、短睡比例、长睡比例仅为 Explanation / Modifier / Future 候选，不进入 D | 否 | SleepSession 时长 | 是（D） | 是 |
| D3 Regularity | 入睡环形离差、起床环形离差、时长离差 | 混合 | SleepSession | 是（R） | 是（边界/置信度） |
| D4 Schedule Flexibility | weekday/weekend 差、近期位移 | 是 | 预留（当前无数据） | — | 预留，不凭空制造 |
| D5 Goal Alignment | start/wake/duration 偏差 | 混合 | 目标作息 vs 行为统计 | 是（GA） | 否（仅辅助） |
| D6 Moon Observation | recentLateNights、nightActivity、质量提示 | — | 月亮/侦探数据 | 是 | 否（仅 Modifier） |

---

## 4. Mathematical Definitions（数学定义）

约定：`M = 1440`（一天分钟数）；`minuteOfDay(t)` 返回 0..1439；所有时间量以分钟为单位，除非另有说明。

### 4.1 Circular Mean（环形平均）

对样本 `x_1..x_n ∈ [0, 1440)`：

```
θ_i = 2π · x_i / 1440
sin̄ = Σsin(θ_i) / n
cos̄ = Σcos(θ_i) / n
μ_circ = (atan2(sin̄, cos̄) mod 2π) / (2π) · 1440    （结果 ∈ [0, 1440)）
```

- 退化条件：`√(sin̄² + cos̄²) < 1e-6` 时，环形平均无统计意义，结果为"未定义"（实现返回 null）。当该退化发生在入睡环形平均（μ_C）时，C 状态为 CHRONOTYPE_MEAN_UNDEFINED（§4.9），且 σ_circ_onset 亦不可计算（§4.9），**不得自动置 0**。
- 已验证示例：23:50(1430) 与 00:10(10) → μ_circ ≈ 0；普通算术平均为 720（12:00），为错误结果。
- 该函数已在当前代码中实现（`SleepPersonality.circularMeanMinutes`）。

### 4.2 Circular Distance（环形距离）

```
circDist(a, b) = min(|a − b|, 1440 − |a − b|)     ∈ [0, 720]
```

示例：circDist(1430, 10) = 20。

### 4.3 ChronotypeScore（C）

**有效定义域（夜间弧，V2.0.7）**：
- `μ_C ∈ [18:00, 24:00) ∪ [00:00, 09:00)`：C 有效，按下方公式计算。注意这是**两个区间的并集（夜间弧）**，不是 `[18:00, 09:00)` 这种易被误读为空区间的写法；
- `μ_C ∈ [09:00, 18:00)`（日间窗口）：`C = undefined`（CHRONOTYPE_OUT_OF_DOMAIN）——统计结果存在但位于 C 语义定义域之外，**禁止**强制映射为 Early / Late / C=0；
- 夜间弧上该映射连续：12:00 断裂点位于日间窗口之外、不在 C 定义域内，故不存在 12:00 → +1 / 12:01 → −1 的跳变。

定义入睡环形平均 μ_C 相对参考点 00:00 的**有符号偏移**：

```
s = (μ_C mod 1440)
若 s > 720，则 s = s − 1440            （s ∈ (−720, 720]）
C = clamp(s / 240, −1, +1)
```

| μ_C | C | 语义 |
|---|---|---|
| 18:00 | −1.0 | 极早睡（域边界，饱和） |
| 20:00 | −1.0 | 极早睡 |
| 22:00 | −0.5 | 早睡 |
| 23:00 | −0.25 | 偏早 |
| 00:00 | 0.0 | 中性 |
| 01:00 | +0.25 | 偏晚 |
| 02:00 | +0.5 | 晚睡 |
| 04:00–08:59 | +1.0 | 极晚睡（饱和；09:00 起越界为 OUT_OF_DOMAIN） |

- C 为 circular 变量映射，**禁止**使用 `hour / 24` 线性映射。
- 特例（WOLF gate）：当 `daytimeOnset == TRUE`（用户主要昼夜倒置，§4.8）时，C 为 CHRONOTYPE_OUT_OF_DOMAIN（undefined），由 §7.12 的 WOLF 区域处理；
- `daytimeOnset` 的判定仅使用 `daytimeOnsetRatio`（见 §4.8）；**μ_C 不参与 WOLF Gate**（V2.0.5，见 §4.8 审计说明），也不使用单一均值时间或单条 session 直接判定。
- C undefined 的两种原因（OUT_OF_DOMAIN / MEAN_UNDEFINED）与下游分类契约见 §4.9。

### 4.4 DurationScore（D）

```
μ_D = arithmetic mean of durationMin（duration 非 circular 变量，禁止环形平均）
D = clamp((μ_D − 480) / 120, −1, +1)
```

| μ_D | D | 语义 |
|---|---|---|
| ≤ 360（6h） | −1.0 | 极短 |
| 440（7h20） | −1/3 | 短 |
| 480（8h） | 0.0 | 中等 |
| 520（8h40） | +1/3 | 长 |
| ≥ 600（10h） | +1.0 | 极长 |

### 4.5 RegularityScore（R）

定义各分量的环形/普通离差（RMS 口径，与现行实现一致）：

```
σ_circ_onset = √( mean( circDist(x_i, μ_circ_onset)² ) )
σ_circ_wake  = √( mean( circDist(w_i, μ_circ_wake)² ) )
σ_dur        = √( mean( (d_i − μ_D)² ) )

r_onset = 1 − min(1, σ_circ_onset / 120)
r_wake  = 1 − min(1, σ_circ_wake / 120)
r_dur   = 1 − min(1, σ_dur / 90)

R = 0.5·r_onset + 0.3·r_wake + 0.2·r_dur      ∈ [0, 1]
```

- 权重说明：入睡时间是最可靠、样本最全的信号，权重最高；起床次之；时长再次。
- 数据缺失回退：无 wake 样本时，`R = r_onset`（归一化到原尺度）。
- 当 σ_circ_onset 不可计算（入睡环形平均退化，§4.1）时：`r_onset = undefined`、`R = undefined`（**不自动置 0**，见 §4.9）。
- 该公式保持 V1 的归一化尺度（离差/阈值，阈值 120 分钟），仅将普通距离替换为环形距离。

### 4.6 Confidence（置信度）

Confidence 不是人格维度，表示"人格描述代表用户真实行为的把握程度"。

```
sampleFactor   = min(1, n / 14)
baseConfidence = 0.20 + 0.30·sampleFactor + 0.35·R + 0.10·completeness
completeness   = 含 wakeAtMs 的会话数 / 会话总数
alignPenalty   = min(0.20, GoalAlignment.startDeviation / 1200)
conf = clamp(baseConfidence − alignPenalty, 0, 0.95)
```

设计理由：V2 的 `0.25 + 0.08·min(n, 8) + 0.35·R + 0.12·completeness` 在 n = 8 时样本项即贡献 0.64，导致 Confidence 迅速逼近上限，不符合"长期行为越多、置信度逐渐提高"的意图。V2.0.1 改用样本饱和因子 `min(1, n/14)`：14 条会话才达到样本项上限，让置信度随长期数据平缓增长。

性质（必须满足）：
- n 越多 → conf 单调不减；R 越高 → conf 单调不减；completeness 越高 → conf 单调不减；
- alignPenalty 不改变 Personality Type（仅置信度）；
- conf 上限 0.95；n < 5 不产生 Formal Confidence；Cold Start Confidence 上限 0.35。

数值 sanity check（completeness = 1，无 alignPenalty）：

| n | sampleFactor | R=0 | R=0.5 | R=1 |
|---|---|---|---|---|
| 5 | 0.357 | 0.407 | 0.582 | 0.757 |
| 8 | 0.571 | 0.471 | 0.646 | 0.821 |
| 14 | 1.000 | 0.600 | 0.775 | 0.950 |
| 20 | 1.000 | 0.600 | 0.775 | 0.950 |

- n=5 时最高 0.757（R=1），n=8 时最高 0.821，均未过早饱和；仅当 n ≥ 14 且 R=1 才触及上限 0.95；
- n ≥ 14 后样本项封顶，conf 只随 R / completeness 继续变化（单调性保持）。

### 4.7 变量总表

| 变量 | 定义 | 范围 | 单位 | 数据来源 | circular | 标准化 | 影响 Primary |
|---|---|---|---|---|---|---|---|
| μ_C | 入睡环形平均 | [0,1440) | 分钟 | sessions | 是 | 否 | 是 |
| μ_W | 起床环形平均 | [0,1440) | 分钟 | sessions | 是 | 否 | 是（一致性） |
| μ_D | 时长算术平均 | ≥0 | 分钟 | sessions | 否 | 否 | 是 |
| C | ChronotypeScore | [−1,+1]（有效定义域：μ_C ∈ 夜间弧 [18:00,24:00)∪[00:00,09:00)） | — | μ_C | 是 | 是 | 是 |
| C 状态 | 数值 / CHRONOTYPE_OUT_OF_DOMAIN / CHRONOTYPE_MEAN_UNDEFINED | — | — | §4.3 / §4.9 | — | — | 是（Gate / Boundary 输入） |
| D | DurationScore | [−1,+1] | — | μ_D | 否 | 是 | 是 |
| R | RegularityScore | [0,1] | — | σ 系列 | 混合 | 是 | 是（边界） |
| daytimeOnsetRatio | 日间 onset 占比 | [0,1] | 比例 | sessions（有效 onset） | 否 | 否 | 是（WOLF gate 输入） |
| daytimeOnset | 三态：TRUE / FALSE / UNKNOWN | — | — | 派生（ratio + n） | 否 | 否 | 是（Gate 输入） |
| GA | OverallAlignment | [0,1] | — | 目标 vs 行为 | 混合 | 是 | **否** |
| MO | MoonObservationProfile | 见 §14 | — | 月亮/侦探 | 否 | 是 | **否** |
| P | PersonalityVector | (C,D,R) | — | 派生 | — | 是 | — |

### 4.8 daytimeOnsetRatio（昼伏判定比，V2.0.2）

```
daytimeOnsetRatio =
  满足 daytime onset 条件的有效 onset sessions 数量
  / 有效 onset sessions 总数量

daytimeOnset（三态，V2.0.6）：
  TRUE    = (daytimeOnsetRatio ≥ 0.60)  AND  (有效 onset sessions ≥ 5)
  FALSE   = (daytimeOnsetRatio < 0.60)  AND  (有效 onset sessions ≥ 5)
  UNKNOWN = 有效 onset sessions < 5（INSUFFICIENT：证据不足，不得解释为 FALSE）
```

- 分母为**有效 onset sessions**（具备有效 `sleepStartAtMs` 的会话；= effectiveSessions，§2 / §11.1）；分母不得包含 invalid / missing onset；
- 单条异常 daytime session 不应单独触发 WOLF：仅当大多数有效 onset（≥ 60%）落在日间窗口时，该 gate 才成立；
- `0.60` 边界使用 **≥（含等于）**：n=5 时 3/5=0.60 → TRUE；n=10 时 6/10=0.60 → TRUE；n=20 时 12/20=0.60 → TRUE；
- `0.60` 是 **design / calibration parameter**，不是统计显著性阈值，也不是概率意义上"60% 确信用户是 WOLF"；
- `有效 onset ≥ 5` 是 **design parameter / calibration parameter / minimum evidence threshold**（= effectiveSessions ≥ 5，与 Formal Personality 门槛一致，§11.1）；它**不代表统计意义上的充分样本量**，也**不代表人格判定具有高置信度**——置信度由 §4.6 的 Confidence 表达（其样本饱和因子为 n/14），两者角色不同、不冲突；
- 当有效 onset sessions < 5 时，`daytimeOnset = UNKNOWN`，不启用任何 Special Gate（进入冷启动 / 数据不足路径；正式 Matcher 因 §20.1 INPUT 门槛 n≥5 不会对 UNKNOWN 求值，但该语义必须定义）；
- 本定义不修改 μ_C 的 Circular Mean 定义，也不修改 C 的坐标系统。

**三态路由（V2.0.6）**：
- WOLF Gate 仅接受 `TRUE`；
- CHAMELEON Gate 仅接受 `FALSE`（且 R < 0.33）；
- `UNKNOWN` **不触发任何 Special Gate**，进入 Base Classification / 冷启动路径。

**V2.0.5 审计说明（μ_C 从 Gate 移除的理由）**：
- μ_C 与 daytimeOnsetRatio 同源于同一批有效 onset 样本，但语义不同：μ_C 是位置统计（circular mean），ratio 是占比统计；两者**不是**互相验证的同义指标；
- 联合 AND 条件可产生矛盾结果：反例 A（09:00×4 + 22:00，ratio=0.80，μ_C≈08:40）→ 旧 Gate 假阴性；反例 B（60% 白天 + 40% 夜间，ratio=0.60，μ_C≈07:42）→ 旧 Gate 在阈值边界假阴性；
- circular mean 在多峰 / 昼夜混合数据下会落入样本稀疏区（合成向量长度 R 明显偏低），产生反直觉结果；
- WOLF 产品语义为"**大多数睡眠记录发生在白天**"，由 ratio 直接度量；μ_C ∈ 窗口既非必要也不充分，保留它只会引入假阴性；
- 因此 WOLF Gate 唯一条件 = `daytimeOnsetRatio ≥ 0.60` 且有效 onset ≥ 5；μ_C 不再参与 Gate，仍用于非倒置用户的 C 坐标（§4.3）。

### 4.9 C undefined 的下游分类契约（V2.0.7）

**两种原因（原因必须保留，供 UI / 日志 / 调试 / 数据分析区分）**：

- `CHRONOTYPE_OUT_OF_DOMAIN`：μ_C 统计可定义，但落在日间窗口 `[09:00, 18:00)`。
  - C = undefined；**R 不受影响**（σ_circ_onset 可计算，R 按 §4.5 照常计算）；
- `CHRONOTYPE_MEAN_UNDEFINED`：circular mean 退化（合成向量长度 < 1e-6，§4.1）。
  - C = undefined；此时 σ_circ_onset 亦不可计算，`r_onset = undefined`、`R = undefined`；
  - R 的不可计算性来自 **Regularity 自身输入缺失**，不是由 C 造成；**禁止因 C undefined 自动置 R = 0**。

**C undefined 时的路由（不可绕过）**：

```
1. WOLF Gate：daytimeOnset == TRUE → Primary = WOLF（不受 C 影响）
2. CHAMELEON Gate：daytimeOnset == FALSE 且 R < 0.33（若 R undefined，CHAMELEON 不触发）
3. 两者均未触发 → Base Classification 因 C axis unavailable 无法形成完整 Membership
   → Boundary / Insufficiently Classified
```

**禁止（fallback 禁令）**：
- 禁止 `C = 0` fallback；
- 禁止选择最近人格中心；
- 禁止用其他坐标猜测 C；
- 禁止任意人格强制映射。

**Confidence**：仅当 R 可计算时产出正式值；R undefined（CHRONOTYPE_MEAN_UNDEFINED 情形）时 Confidence = undefined（不展示正式值）。

---

## 5. Classification Model（分类模型）

### 5.1 区域评分

**原则（V2.0.1）：连续 Membership Region + 语义边界。**

C/D 的 Early / Neutral / Late 是人格的**语义区域与解释边界**；正式 PersonalityMatcher **不得**先用硬边界把用户锁死到某个象限，再只在该象限内选人格。Matcher 对**所有适用的 PersonalityDefinition** 计算连续 Membership Score，取最高者。

语义硬边界仅用于：文档解释、邻接关系、反事实测试、产品语义。

每个 PersonalityDefinition 定义正式评分中心 `(centerC, centerD)` 与可选的 R 约束。轴成员度（梯形/三角，w = 2/3，V2.0.7）：

```
m_axis(x; c, w) = max(0, 1 − |x − c| / w)，w = 2/3
m_C = m_axis(C; centerC, 2/3)
m_D = m_axis(D; centerD, 2/3)
```

区域得分：

```
S_i = m_C × m_D × m_R
```

- 无 R 约束的人格：`m_R = 1`（普通人格的主要空间仍由 C × D 决定）；
- SLOTH：`m_R = clamp((0.7 − R) / 0.1, 0, 1)`（名义有效区间 R < 0.6，过渡带 0.6 ± 0.1）；
- OTTER：`m_R = clamp((R − 0.5) / 0.1, 0, 1)`（名义有效区间 R ≥ 0.6，过渡带 0.6 ± 0.1）。

**名义阈值 vs Membership ramp（V2.0.4，消除双重判定）**：
- `R < 0.6` / `R ≥ 0.6` 仅为**名义语义阈值**，用于解释、邻接关系与 Transition 文案，**不参与任何硬判定**；
- 实际参与评分的**唯一机制**是上述连续 ramp（过渡带 0.6 ± 0.1）；禁止把 "R<0.6/R≥0.6" 实现为第二个独立硬阈值；
- "R ≥ 0.6 → OTTER / R < 0.6 → SLOTH" 仅为名义语义；实际语义为：**R 约在 0.6 附近（0.5–0.7 过渡带）时 OTTER membership 较高**；
- R = 0.6 时 SLOTH 与 OTTER 的 m_R 均为 1，且共享同一 C/D 中心（S 相同），由统一 tie-break（§5.3：priority → stable ID）决出。

**几何覆盖性（V2.0.7，P0-B 联合校准）**：中心 {−2/3, 0, +2/3} 与 `w = 2/3` 保证 `m_C ≥ 0.5`（∀C ∈ [−1,1]，下确界在 C=±1 与相邻中心中点 C=±1/3 处取得），故**对不受额外 R 约束的 Base Definition**，C×D 几何 Membership 满足 `m_C × m_D ≥ 0.25` 覆盖整个 [−1,1]²——即**几何层不再产生 Boundary 空洞**。注意：完整 Membership 为 `S_i = m_C × m_D × m_R`；SLOTH / OTTER 等带 R ramp 的定义，其完整 S 还受 `m_R` 调制，**不在上述几何覆盖结论的范围内**。极端角（如 C=D=−1）不再因几何被拒为 Boundary；Boundary 仅由证据不足或 C 轴不可判定（§4.9）产生。

R 的主要职责：CHAMELEON gate、SLOTH / OTTER 分裂、Confidence、Transition / Boundary 解释；不硬塞入所有人格。

**Membership Score 语义边界（V2.0.2）**：
- Membership Score 是 **deterministic membership measure**（确定性隶属度量）；
- 它**不是** probability、confidence、classification accuracy、statistical probability，也**不是**"用户属于该人格的百分比概率"；
- `minMembershipScore = 0.25`（V2.0.7）仅为"最低有效 Membership"判定阈值：`S_primary < 0.25 → Boundary / Insufficiently Classified`，**不表示**"有 25% 概率属于该人格"，也**不表示**"分类置信度低于 25%"。

### 5.2 判定顺序（决策规则，可解释）

**Special Gate 执行优先级与控制流（V2.0.3，唯一合法顺序）**：

```
STEP 1 — WOLF Gate：
  若 daytimeOnsetRatio ≥ 0.60
     AND 有效 onset sessions ≥ 5
  → Primary = WOLF，并立即结束 Primary classification
    （不得继续进入 CHAMELEON 或 Base Personality scoring）

STEP 2 — CHAMELEON Gate（仅当 WOLF Gate 未通过时检查）：
  若 daytimeOnset == FALSE（明确不成立：n ≥ 5 且 ratio < 0.60）
     AND R < 0.33
  → Primary = CHAMELEON，并立即结束 Primary classification
    （不得继续进入 Base Personality scoring）

STEP 3 — Base Personality Classification（仅当两个 Special Gate 均未触发）：
  对 10 个基础定义计算 S_i = m_C × m_D × m_R
  → Primary = deterministic argmax(S_i)
    tie-break：1. Membership Score；2. priority；3. stable ID lexical order
```

理由（统计意义）：
- R < 0.33 表示入睡/起床/时长均高度不稳定，此时 μ_C、μ_D 的单点估计不可信，任何具体象限人格都不成立；
- 昼伏夜出者的入睡时间处于日间窗口，C 坐标无定义，须单独处理；
- Special Gates（WOLF / CHAMELEON）优先于 Base C×D 人格。
- **UNKNOWN / INSUFFICIENT（n < 5）**：不触发任何 Special Gate（§4.8 三态）；正式 Matcher 不得对 n<5 求值（§20.1 INPUT 门槛），若被调用则直接进入冷启动路径。

**Special Gate 不是 Membership Score 竞争者（V2.0.3）**：
- WOLF / CHAMELEON 是 **classification control-flow**（控制流），不是普通 PersonalityDefinition Membership 候选；
- **禁止**执行 `argmax(WOLF score, CHAMELEON score, Base personality scores)`；
- **禁止**"先计算全部 Base scores，再拿 Gate 结果与 Base scores 比较"；
- Gate 触发时直接确定 Primary，不进行任何 score 比较；
- Gate 优先级（WOLF > CHAMELEON > Base）是 **control-flow precedence**，不是 Membership Score precedence；不使用"权重更高 / 分数更高"描述。

### 5.3 输出

```
适用定义集  = 9 个 Base C×D Regions 内的全部适用定义（10 个基础定义）
Primary    = argmax_i S_i（tie-break 见下）
Secondary  = Primary 的 transitionTargets 中 S_j 最高的有效候选；无有效候选则 null
ClassificationMargin = Top-1 − Top-2 eligible adjacent（仅当 Secondary 存在时）
isTransitioning = ClassificationMargin < δ（见 §10）
```

- 若 `S_primary < minMembershipScore`（默认 0.25，V2.0.7），进入 **Boundary / Insufficiently Classified** 状态（可显示"月亮正在认识你"）；
- Membership Score 与 Confidence 是两个独立概念：前者决定分类，后者表示对分类的把握程度（见 §4.6）。
- 当 Primary ∈ {WOLF, CHAMELEON}（Special Gate 触发）时，本节的 Base argmax 流程不执行；其 Secondary 与 ClassificationMargin 处理见 §10.7。
- 当 C undefined（CHRONOTYPE_OUT_OF_DOMAIN / CHRONOTYPE_MEAN_UNDEFINED，§4.9）时，Base 无法完成 C-axis Membership → 同样进入 Boundary（见 §4.9）。

**Secondary 有效候选（eligible candidate）定义（V2.0.4）**：
- 候选必须**同时满足**：(1) 属于 `Primary.transitionTargets`；(2) Membership Score ≥ `minMembershipScore`（0.25）；
- Score = 0 或 < 0.25 的候选**不可**作为 Secondary；
- 全部候选均不达标 → `Secondary = null`（此时 ClassificationMargin 不计算、isTransitioning = false）；
- 候选精确同分 → 使用统一 tie-break（§5.3：Score → priority → stable ID）；
- 理由：复用现有 `minMembershipScore = 0.25` 作为候选可信度下限，**不新增阈值**；与 Boundary 语义一致——低于最低有效隶属度的候选不是可信的备选人格。

**Boundary / Insufficiently Classified 正式定义（V2.0.4）**：
- 触发条件：仅限 Base Classification，且 `S_primary < minMembershipScore`（0.25）或 C 轴不可判定（§4.9）；**Gate Primary 不经过该检查**（Gate 成立条件即其 Primary 有效条件，见 §10.7）；
- 输出：`Primary = null`（无正式人格）、`Secondary = null`、`ClassificationMargin = null`、`isTransitioning = false`；
- Boundary **不是**一个额外 PersonalityDefinition，**不拥有** Secondary，**不计算** Margin，**不算** Transition；
- UI 语义：显示"月亮正在认识你"类占位，不展示任何正式人格（与冷启动占位一致）。

**Primary 判定（deterministic tie-break，V2.0.2，正式规范）**：

1. **第一优先级**：Membership Score 最高者胜出；
2. **第二优先级**：仅当 Membership Score 精确同分时，`PersonalityDefinition.priority` 较高者胜出；
3. **第三优先级**：仅当 Score 与 priority 均相同时，按稳定 ID 字典序（lexicographic order）较小者胜出。

约束：
- `priority` **不得**抢走 Membership Score 明显更高的人格——它只用于解决精确同分；
- `stable ID` 只用于 priority 仍相同的最终确定；
- 最终 Primary 必须 **deterministic**，不允许出现"实现自行决定"的情况。

**priority 显式赋值（V2.0.4）**：
- `priority` 为整数，**数值越大越优先**（"较高者胜出"）；
- 第一阶段赋值 = §7 目录序：ROOSTER=1、EARLY_BIRD=2、BEAR=3、WORK_HORSE=4、HOUND=5、SLOTH=6、OTTER=7、BAT=8、NIGHT_OWL=9、OWL=10、CHAMELEON=11、WOLF=12（CHAMELEON / WOLF 不参与 Base scoring，其 priority 仅作占位）；
- 例：R = 0.6 时 SLOTH / OTTER 同分 → OTTER（priority 7）胜出。

**统一 tie-break 的适用范围（V2.0.4）**：同一规则（Score → priority → stable ID）适用于：Base Primary argmax、Secondary 候选选择（含 CHAMELEON Secondary）、SLOTH / OTTER 重叠（两者是 argmax 中的两个普通定义）。**全文仅存在这一套 tie-break**。

---

## 6. Personality Regions（人格区域）

第一阶段启用 **12 个 PersonalityDefinition**：9 个 Base C×D Regions → 10 个 Base Definitions（Neutral×Long 含 SLOTH / OTTER 两个 R-分裂定义）→ +2 Special Definitions（CHAMELEON / WOLF）= 12。

### 6.1 Base C×D Regions（9 个，由 C × D 确定）

| C \ D | 短睡 D<−1/3 | 中睡 −1/3≤D≤+1/3 | 长睡 D>+1/3 |
|---|---|---|---|
| 早睡 C<−1/3 | ROOSTER 报晓鸡 | EARLY_BIRD 早起鸟 | BEAR 安眠熊 |
| 中性 −1/3≤C≤+1/3 | WORK_HORSE 牛马 | HOUND 守序犬 | SLOTH / OTTER（按 R 分裂） |
| 晚睡 C>+1/3 | BAT 夜蝙蝠 | NIGHT_OWL 夜猫子 | OWL 昼眠枭 |

- 中性 × 长睡分裂：`R ≥ 0.6 → OTTER 安睡海獭`；`R < 0.6 → SLOTH 树懒`。

正式评分中心（PersonalityDefinition.centerC / centerD，V2.0.1 正式字段）：

| 定义 | centerC | centerD |
|---|---|---|
| ROOSTER | −2/3 | −2/3 |
| EARLY_BIRD | −2/3 | 0 |
| BEAR | −2/3 | +2/3 |
| WORK_HORSE | 0 | −2/3 |
| HOUND | 0 | 0 |
| SLOTH / OTTER | 0 | +2/3 |
| BAT | +2/3 | −2/3 |
| NIGHT_OWL | +2/3 | 0 |
| OWL | +2/3 | +2/3 |

- SLOTH 与 OTTER 共用 C/D 中心，仅由 R 条件（0.6 ± 0.1 连续过渡带）区分；
- SLOTH / OTTER 是同一 Base C×D Region（Neutral×Long）内的两个 PersonalityDefinition（R 语义分裂），**不是**额外的 C×D 网格区域。
- R 名义阈值（0.6）与连续 ramp（0.6 ± 0.1）的关系见 §5.1：仅 ramp 参与评分，两者不构成双重判定机制。
- "R ≥ 0.6 → OTTER / R < 0.6 → SLOTH" 仅为名义语义（解释 / 邻接）；实际语义为 R 约在 0.6 附近（0.5–0.7）时 OTTER membership 较高。

### 6.2 Special Gates（特殊区域，2 个）

| 区域 | 触发条件 |
|---|---|
| CHAMELEON 变色龙 | `daytimeOnset == FALSE`（n ≥ 5 且 ratio < 0.60）且 `R < 0.33`（任意 C、D） |
| WOLF 夜行狼 | `daytimeOnset == TRUE`（daytimeOnsetRatio ≥ 0.60 且有效 onset ≥ 5，见 §4.8；μ_C 不参与 Gate） |

### 6.3 空间图（文字规范）

```
                Duration (Y)  长睡
                    ↑
      安眠熊         |      昼眠枭
      早起鸟         |      夜猫子
      报晓鸡         |      夜蝙蝠
早睡 ←——————————————┼——————————————→ 晚睡  Chronotype (X)
      树懒/安睡海獭   |      夜行狼
      守序犬         |      （日间相位）
      牛马           |
                    ↓
                  短睡

Regularity (Z)：R<0.33 → 变色龙（整列通吃）；
R≥0.6 且 中性×长睡 → 安睡海獭（取代树懒）。
```

> 图中 夜行狼 仅示意其非几何相位位置（日间相位）；WOLF / CHAMELEON 是 Special Gates，**不属于** C×D 网格区域，不参与 Base Membership 评分。

---

## 7. Final Personality Catalogue（人格成品目录）

> 每个候选人格必须通过五步验证：统计特征 → 行为特征 → 动物行为意象 → 中文文化语义 → 网络语言/emoji。
> 每个人格均给出 22 项定义（见 §7.13 的字段规范）。

### 7.1 ROOSTER 报晓鸡

| 字段 | 值 |
|---|---|
| ID / 英文名 | ROOSTER |
| 中文正式名称 | 报晓鸡 |
| 动物基底 | 公鸡 |
| 修饰词 | 报晓（黎明即鸣） |
| Emoji | 🐓 ⏰（动物 + 天然闹钟） |
| 核心行为区域 | Early × Short（centerC = −2/3，centerD = −2/3） |
| Chronotype 范围 | C ∈ [−1, −1/3]（入睡 ≤ 约 22:40） |
| Duration 范围 | D ∈ [−1, −1/3]（时长 < 约 7h20） |
| Regularity 要求 | 无硬约束（R 低时优先被 CHAMELEON 接管） |
| 简洁用户解释 | "你通常在晚上 10 点半前入睡，但睡得不够久，像公鸡一样天没亮就醒。" |
| 为什么用公鸡 | 公鸡天亮即啼、起得早且不会赖床，是"早醒 + 睡眠少"的天然意象 |
| 文化/网络依据 | 「金鸡报晓」；网络语境"鸡你太美"不采用；采用"公鸡打鸣 = 早起闹钟" |
| 与相邻人格区别 | 与早起鸟（Early × Medium）区别在时长；与牛马（Neutral × Short）区别在相位 |
| 典型用户画像 | 22:00 前睡、06:00 前醒、总时长不足 7 小时的中老年或晨型人 |
| 边界条件 | D 越过 −1/3 → 早起鸟；C 越过 −1/3 → 牛马 |
| 过渡方向 | → EARLY_BIRD（时长增加）；→ WORK_HORSE（相位后移） |
| 未来可扩展 modifier | 晨间清醒度、起床闹钟依赖度 |

### 7.2 EARLY_BIRD 早起鸟

| 字段 | 值 |
|---|---|
| ID / 英文名 | EARLY_BIRD |
| 中文正式名称 | 早起鸟 |
| 动物基底 | 鸟 |
| 修饰词 | 早起 |
| Emoji | 🐦 🌅（动物 + 黎明） |
| 核心行为区域 | Early × Medium（centerC = −2/3，centerD = 0） |
| Chronotype 范围 | C ∈ [−1, −1/3] |
| Duration 范围 | D ∈ [−1/3, +1/3]（约 7h20 – 8h40） |
| Regularity 要求 | 无硬约束 |
| 简洁用户解释 | "你早睡早起，睡眠时长正常，符合'早起的鸟儿有虫吃'。" |
| 为什么用鸟 | 鸟类的晨鸣行为与"早起"直接对应 |
| 文化/网络依据 | 「早起的鸟儿有虫吃」俗语；网络"早起鸟打卡" |
| 与相邻人格区别 | 与报晓鸡区别在时长；与守序犬区别在相位；与安眠熊区别在时长 |
| 典型用户画像 | 23:00 前睡、07:00 前后醒、睡足 8 小时的规律晨型人 |
| 边界条件 | D 降 → 报晓鸡；D 升 → 安眠熊；C 升 → 守序犬 |
| 过渡方向 | → ROOSTER / BEAR / HOUND |
| 未来可扩展 modifier | 晨型强度（起床后清醒时长） |

### 7.3 BEAR 安眠熊

| 字段 | 值 |
|---|---|
| ID / 英文名 | BEAR |
| 中文正式名称 | 安眠熊 |
| 动物基底 | 熊 |
| 修饰词 | 安眠（早睡 + 长眠） |
| Emoji | 🐻 ❄️（动物 + 冬眠） |
| 核心行为区域 | Early × Long（centerC = −2/3，centerD = +2/3） |
| Chronotype 范围 | C ∈ [−1, −1/3] |
| Duration 范围 | D ∈ [+1/3, +1]（时长 > 约 8h40） |
| Regularity 要求 | 无硬约束 |
| 简洁用户解释 | "你睡得很早也睡得很久，像进入冬眠的熊一样需要充足睡眠。" |
| 为什么用熊 | 熊冬眠数月的长眠行为与"早睡 + 长睡"直接对应 |
| 文化/网络依据 | 「冬眠」网络梗；"睡熊"意象 |
| 与相邻人格区别 | 与早起鸟区别在时长；与树懒/安睡海獭区别在相位 |
| 典型用户画像 | 21:30–22:00 睡、07:30 后醒、时长 9 小时以上的长睡者 |
| 边界条件 | D 降 → 早起鸟；C 升 → 树懒/安睡海獭 |
| 过渡方向 | → EARLY_BIRD / SLOTH / OTTER |
| 未来可扩展 modifier | 季节波动（冬长夏短） |

### 7.4 WORK_HORSE 牛马

| 字段 | 值 |
|---|---|
| ID / 英文名 | WORK_HORSE |
| 中文正式名称 | 牛马 |
| 动物基底 | 牛/马 |
| 修饰词 | 高强度劳动（牺牲睡眠） |
| Emoji | 🐂 💼（动物 + 工作） |
| 核心行为区域 | Neutral × Short（centerC = 0，centerD = −2/3） |
| Chronotype 范围 | C ∈ [−1/3, +1/3]（约 22:40 – 01:20 入睡） |
| Duration 范围 | D ∈ [−1, −1/3]（时长 < 约 7h20） |
| Regularity 要求 | 无硬约束 |
| 简洁用户解释 | "你的入睡时间不算极端，但睡眠时长明显不足，睡眠受到高负荷工作或生活节奏的挤压。" |
| 为什么用牛/马 | 牛马为高强度役用动物，网络语"牛马"指过度劳作、睡眠被牺牲的人 |
| 文化/网络依据 | 网络流行语"牛马"；"做牛做马" |
| 与相邻人格区别 | 与报晓鸡/夜蝙蝠区别在相位；与守序犬区别在时长 |
| 典型用户画像 | 23:30–01:00 睡、06:00–07:30 醒、时长 5–7 小时的忙碌者 |
| 边界条件 | D 升 → 守序犬；C 降 → 报晓鸡；C 升 → 夜蝙蝠 |
| 过渡方向 | → HOUND / ROOSTER / BAT |
| 未来可扩展 modifier | 加班频率、周末补觉幅度 |

### 7.5 HOUND 守序犬

| 字段 | 值 |
|---|---|
| ID / 英文名 | HOUND |
| 中文正式名称 | 守序犬 |
| 动物基底 | 犬 |
| 修饰词 | 守序（准时、规律） |
| Emoji | 🐕 ⏰（动物 + 准点） |
| 核心行为区域 | Neutral × Medium（centerC = 0，centerD = 0） |
| Chronotype 范围 | C ∈ [−1/3, +1/3] |
| Duration 范围 | D ∈ [−1/3, +1/3] |
| Regularity 要求 | 无硬约束（R 高时典型） |
| 简洁用户解释 | "你的作息时间与时长都处于常规范围，且相当稳定，像忠于时刻表的家犬。" |
| 为什么用犬 | 家犬有强烈的时刻感（到点吃饭、到点睡觉），是"守时规律"的动物意象 |
| 文化/网络依据 | 「狗记性」引申的时刻感；"规律得像狗一样"的日常表达 |
| 与相邻人格区别 | 与早起鸟/夜猫子区别在相位；与牛马/树懒区别在时长 |
| 典型用户画像 | 23:30–00:30 睡、07:30–08:30 醒、时长 8 小时左右的普通人 |
| 边界条件 | C 降 → 早起鸟；C 升 → 夜猫子；D 降 → 牛马；D 升 → 树懒/安睡海獭 |
| 过渡方向 | → EARLY_BIRD / NIGHT_OWL / WORK_HORSE / SLOTH |
| 未来可扩展 modifier | 工作日/周末位移 |

### 7.6 SLOTH 树懒

| 字段 | 值 |
|---|---|
| ID / 英文名 | SLOTH |
| 中文正式名称 | 树懒 |
| 动物基底 | 树懒 |
| 修饰词 | 长睡、节奏慢 |
| Emoji | 🦥 🛋️（动物 + 躺平） |
| 核心行为区域 | Neutral × Long，R < 0.6（centerC = 0，centerD = +2/3） |
| Chronotype 范围 | C ∈ [−1/3, +1/3] |
| Duration 范围 | D ∈ [+1/3, +1] |
| Regularity 要求 | R < 0.6 |
| 简洁用户解释 | "你睡得很久但时间不太稳定，像树懒一样随时都能睡、也随时醒。" |
| 为什么用树懒 | 树懒以长时间睡眠与极慢节奏著称 |
| 文化/网络依据 | "树懒式生活"、"躺平"网络语 |
| 与相邻人格区别 | 与安睡海獭区别在规律性（R 阈值 0.6）；与守序犬区别在时长 |
| 典型用户画像 | 00:00 前后睡、睡 9–11 小时但每天时间漂移的人 |
| 边界条件 | R 升过 0.6 → 安睡海獭；D 降 → 守序犬；C 升 → 昼眠枭 |
| 过渡方向 | → OTTER / HOUND / OWL |
| 未来可扩展 modifier | 午睡倾向、睡眠惯性 |

### 7.7 OTTER 安睡海獭

| 字段 | 值 |
|---|---|
| ID / 英文名 | OTTER |
| 中文正式名称 | 安睡海獭 |
| 动物基底 | 海獭 |
| 修饰词 | 安睡（长睡 + 高规律） |
| Emoji | 🦦 🌊（动物 + 栖息海面） |
| 核心行为区域 | Neutral × Long，R ≥ 0.6（centerC = 0，centerD = +2/3） |
| Chronotype 范围 | C ∈ [−1/3, +1/3] |
| Duration 范围 | D ∈ [+1/3, +1] |
| Regularity 要求 | R ≥ 0.6 |
| 简洁用户解释 | "你睡得久而且很规律，像海獭一样安稳地漂浮在自己的睡眠里。" |
| 为什么用海獭 | 海獭常手拉手漂浮睡眠，姿态安稳，是"长睡且稳定"的意象 |
| 文化/网络依据 | 海獭睡觉的可爱影像在网络广泛传播；"睡得香"正面联想 |
| 与相邻人格区别 | 与树懒（同区域低 R）区别在规律性；与安眠熊区别在相位 |
| 典型用户画像 | 00:00 前后睡、每天 9–11 小时、入睡波动 < 30 分钟的人 |
| 边界条件 | R 降过 0.6 → 树懒；D 降 → 守序犬；C 升 → 昼眠枭 |
| 过渡方向 | → SLOTH / HOUND / OWL |
| 未来可扩展 modifier | 睡前仪式感、安稳度 |

### 7.8 BAT 夜蝙蝠

| 字段 | 值 |
|---|---|
| ID / 英文名 | BAT |
| 中文正式名称 | 夜蝙蝠 |
| 动物基底 | 蝙蝠 |
| 修饰词 | 深夜活动、睡眠不足 |
| Emoji | 🦇 🌃（动物 + 深夜城市） |
| 核心行为区域 | Late × Short（centerC = +2/3，centerD = −2/3） |
| Chronotype 范围 | C ∈ [+1/3, +1]（入睡 ≥ 约 01:20） |
| Duration 范围 | D ∈ [−1, −1/3] |
| Regularity 要求 | 无硬约束 |
| 简洁用户解释 | "你通常凌晨 1 点后才睡，而且睡得很少，像昼伏夜出的蝙蝠。" |
| 为什么用蝙蝠 | 蝙蝠为夜行性动物，是"夜间活动"的意象载体；"短睡"是该人格区域的行为统计属性，不主张蝙蝠在生物学意义上必然睡得少 |
| 文化/网络依据 | "夜行动物"网络语；蝙蝠的夜间生态 |
| 与相邻人格区别 | 与牛马区别在相位；与夜猫子区别在时长 |
| 典型用户画像 | 01:30 后睡、07:00 前醒、时长 5–6 小时的夜班/游戏/加班人群 |
| 边界条件 | D 升 → 夜猫子；C 降 → 牛马 |
| 过渡方向 | → NIGHT_OWL / WORK_HORSE |
| 未来可扩展 modifier | 深夜亮屏频率 |

### 7.9 NIGHT_OWL 夜猫子

| 字段 | 值 |
|---|---|
| ID / 英文名 | NIGHT_OWL |
| 中文正式名称 | 夜猫子 |
| 动物基底 | 猫 |
| 修饰词 | 深夜精神 |
| Emoji | 🐱 🌙（动物 + 月亮） |
| 核心行为区域 | Late × Medium（centerC = +2/3，centerD = 0） |
| Chronotype 范围 | C ∈ [+1/3, +1] |
| Duration 范围 | D ∈ [−1/3, +1/3] |
| Regularity 要求 | 无硬约束 |
| 简洁用户解释 | "你通常在凌晨入睡，睡眠时长正常，属于典型的夜间型人格。" |
| 为什么用猫 | 猫科动物夜行性 + 中文"夜猫子"是长期既定的夜间型称谓 |
| 文化/网络依据 | 「夜猫子」网络语；"月亮的朋友，太阳的陌生猫" |
| 与相邻人格区别 | 与守序犬区别在相位；与夜蝙蝠/昼眠枭区别在时长 |
| 典型用户画像 | 01:00–03:00 睡、09:00–11:00 醒、时长 7–8 小时的人 |
| 边界条件 | D 降 → 夜蝙蝠；D 升 → 昼眠枭；C 降 → 守序犬 |
| 过渡方向 | → BAT / OWL / HOUND |
| 未来可扩展 modifier | 深夜效率感、晨起痛苦度 |

### 7.10 OWL 昼眠枭

| 字段 | 值 |
|---|---|
| ID / 英文名 | OWL |
| 中文正式名称 | 昼眠枭 |
| 动物基底 | 猫头鹰 |
| 修饰词 | 昼伏（天亮才睡） |
| Emoji | 🦉 ☀️（动物 + 白昼） |
| 核心行为区域 | Late × Long（centerC = +2/3，centerD = +2/3） |
| Chronotype 范围 | C ∈ [+1/3, +1] |
| Duration 范围 | D ∈ [+1/3, +1] |
| Regularity 要求 | 无硬约束 |
| 简洁用户解释 | "你凌晨很晚才睡但睡得够久，醒来时间通常已经进入白天或中午以后，像白天栖息、夜间捕食的猫头鹰。" |
| 为什么用猫头鹰 | 猫头鹰夜行捕食、白昼栖息，是"晚睡 + 长眠 + 昼伏"的自然意象 |
| 文化/网络依据 | 「夜枭」文学意象；"昼伏夜出"成语 |
| 与相邻人格区别 | 与夜猫子区别在时长；与树懒/安睡海獭区别在相位 |
| 典型用户画像 | 03:00–05:00 睡、12:00 后醒、时长 9 小时以上的深度夜型人 |
| 边界条件 | D 降 → 夜猫子；C 降 → 树懒/安睡海獭 |
| 过渡方向 | → NIGHT_OWL / SLOTH / OTTER |
| 未来可扩展 modifier | 昼夜颠倒程度、光照暴露 |

### 7.11 CHAMELEON 变色龙

| 字段 | 值 |
|---|---|
| ID / 英文名 | CHAMELEON |
| 中文正式名称 | 变色龙 |
| 动物基底 | 变色龙 |
| 修饰词 | 随变（作息不稳定） |
| Emoji | 🦎 🎲（动物 + 不确定骰子） |
| 核心行为区域 | 全空间，R < 0.33（无 centerC / centerD；C、D 无定义） |
| Chronotype 范围 | 无（C 不参与判定） |
| Duration 范围 | 无（D 不参与判定） |
| Regularity 要求 | R < 0.33 |
| 简洁用户解释 | "你的睡眠时间和时长变化较大，还没有形成稳定的睡眠节奏。" |
| 为什么用变色龙 | 变色龙以体色多变著称，"多变"是该人格的核心 |
| 文化/网络依据 | "作息不固定"网络语；变色龙随环境变色的意象（比喻作息多变，非声称每天入睡时间都随机） |
| 与相邻人格区别 | 与所有基础定义的区别在于：无稳定相位/时长信号，不参与象限判定 |
| 典型用户画像 | 入睡、起床与睡眠时长均出现较大波动的轮班/极不规律人群（示例：入睡时间经常跨越数小时；示例性描述，非 R < 0.33 的数学等价条件） |
| 边界条件 | R 升过 0.33 → 按当时的 (C, D) 进入对应基础定义（Special-Gate Transition，见 §10.6） |
| 过渡方向 | → 全部 10 个基础定义中按统一 tie-break 选出的最高有效候选（Special-Gate Transition；transitionTargets 与确定性规则见 §10.7） |
| 未来可扩展 modifier | 轮班制标记、周内/周末分裂 |

### 7.12 WOLF 夜行狼

| 字段 | 值 |
|---|---|
| ID / 英文名 | WOLF |
| 中文正式名称 | 夜行狼 |
| 动物基底 | 狼 |
| 修饰词 | 昼夜相位倒置（白天入睡、夜间活动） |
| Emoji | 🐺 🌌（动物 + 夜空） |
| 核心行为区域 | daytimeOnset == TRUE（无 centerC / centerD；daytimeOnsetRatio ≥ 0.60 且有效 onset ≥ 5，见 §4.8；μ_C 不参与 Gate） |
| Chronotype 范围 | 无定义（相位倒置，C = undefined：CHRONOTYPE_OUT_OF_DOMAIN，§4.3 / §4.9；禁止 C=0 fallback） |
| Duration 范围 | 不约束（通常 D ∈ [0, +1]） |
| Regularity 要求 | 无硬约束 |
| 简洁用户解释 | "你在白天入睡、夜间活动，属于昼夜颠倒的夜行作息，像在夜间巡行的狼。" |
| 为什么用狼 | 狼为典型的夜行群居捕猎动物，"昼伏夜出"是其生态特征 |
| 文化/网络依据 | 「狼性」网络语；"夜行狼"的夜间活动意象 |
| 与相邻人格区别 | 与夜猫子/昼眠枭的区别：入睡落在日间窗口，相位坐标无效，独立成区；核心是昼夜相位倒置，不是普通晚睡（NIGHT_OWL） |
| 典型用户画像 | 夜班护士、夜班司机、长期跨时区作息者（如 12:00 睡、20:00 起） |
| 边界条件 | 相位恢复正常（daytimeOnsetRatio < 0.60）→ 按 C、D 进入对应基础定义 |
| 过渡方向 | → NIGHT_OWL / OWL（Special-Gate Transition；transitionTargets = {NIGHT_OWL, OWL}，确定性选择规则见 §10.7） |
| 未来可扩展 modifier | 轮班周期、昼夜光照暴露 |

### 7.13 人格定义字段规范

每个人格定义必须包含以下 22 项（理论数据模型，见 §15）：

`id / name / animal / modifier / emoji[2] / englishName / centerC / centerD / region / chronotypeRange / durationRange / regularityRange / priority / minMembershipScore / userExplanation / animalRationale / culturalBasis / neighborDifference / typicalProfile / boundaryCondition / transitionTargets / futureModifier`

---

## 8. Naming System（命名规则）

### 8.1 硬性规则

1. **动物基底**：所有 Primary Personality 必须基于动物；
2. 名称形式：`[动物基底]` 或 `[动物基底] + [行为修饰]`；
3. 名称必须能自然提示睡眠行为特征；
4. **禁止**纯幻想称号（月亮守护者、夜行幽灵等）作为 Primary Personality；此类称号仅允许作为 Achievement / Badge / Modifier；
5. 每个名称必须通过 §8.2–§8.5 四项测试。

### 8.2 Two Emoji Test（双 emoji 测试）

人格必须可用两个 emoji 表达，且两个 emoji 分别承担：**动物身份** + **行为/文化语义**。

| 人格 | Emoji | 动物身份 | 行为/文化语义 |
|---|---|---|---|
| 报晓鸡 | 🐓 ⏰ | 公鸡 | 天然闹钟（报晓） |
| 早起鸟 | 🐦 🌅 | 鸟 | 黎明 |
| 安眠熊 | 🐻 ❄️ | 熊 | 冬眠 |
| 牛马 | 🐂 💼 | 牛 | 工作 |
| 守序犬 | 🐕 ⏰ | 犬 | 准点 |
| 树懒 | 🦥 🛋️ | 树懒 | 躺平 |
| 安睡海獭 | 🦦 🌊 | 海獭 | 海面安稳漂浮 |
| 夜蝙蝠 | 🦇 🌃 | 蝙蝠 | 深夜城市 |
| 夜猫子 | 🐱 🌙 | 猫 | 月亮/夜晚 |
| 昼眠枭 | 🦉 ☀️ | 猫头鹰 | 白昼 |
| 变色龙 | 🦎 🎲 | 变色龙 | 不固定（骰子） |
| 夜行狼 | 🐺 🌌 | 狼 | 夜空 |

禁止 `🐱 ❤️` 式无信息组合（第二 emoji 必须携带行为或文化语义）。

### 8.3 Explainability Test（可解释性测试）

点击人格后必须能用一句话解释，格式模板：

```
"你通常在 X 入睡、Y 醒来、睡眠时长约 Z 小时，并且这种作息已保持了 W 的稳定性，因此被归为「人格名」。"
```

其中 X / Y / Z / W 全部来自数据统计（环形平均入睡/起床、平均时长、规律度描述词）。

### 8.4 Boundary Test（边界测试）

每个相邻人格对必须能说明"什么条件下 A 变为 B"（见 §7 各人格"边界条件"）。

### 8.5 Counterfactual Test（反事实测试）

示例：夜猫子（入睡环形平均 02:00，C=+0.5；时长 8h，D=0）连续一个月将入睡时间逐步提前至 23:30（C 降至 −0.125）：

- 期间 C 连续穿过 +1/3 边界进入中性带；
- 人格应依次为：夜猫子 →（边界态：夜猫子/守序犬 过渡）→ 守序犬；
- **禁止**因为某个 if 阈值从夜猫子直接跳到报晓鸡或昼眠枭。

本测试要求：人格移动在特征空间中连续，禁止跳变。

---

## 9. Animal Derivation Method（动物推导标准方法）

每个候选人格必须按以下链逐层推导并记录：

```
统计特征（C、D、R 数值区间）
→ 行为特征（自然语言描述：何时睡、睡多久、稳不稳定）
→ 动物行为意象（该行为在动物界的对应）
→ 中文文化语义（成语/俗语/文学意象）
→ 网络语言（现代网络中的对应表达）
→ emoji（动物 + 行为双编码）
```

若任一层无法严格对应，该候选人格不成立。示例（夜猫子）：

```
夜间相位 + 中等时长 + 较稳定
→ 长期夜间活动、白天困倦
→ 猫科动物夜行性
→ 中文"夜猫子"称谓
→ 网络语"夜猫子""修仙"
→ 🐱🌙
```

### 9.1 动物语义定位（V2.0.1 明确）

- 动物是**行为意象 / 文化语义载体**，不要求在生物学意义上与 C/D/R 三个数学维度一一对应；
- 真正的分类由人格数学区域（centerC / centerD / R 约束）负责；
- 例如 BAT 表达"夜行 / 深夜活动"；"短睡"是该 PersonalityRegion 的行为统计属性，**不是**对蝙蝠生物学睡眠时长的断言；HOUND 同理（"守时"是意象，不是犬科行为学结论）；
- 禁止编造未经证实的动物行为事实作为人格依据。

---

## 10. Transition Model（过渡模型）

### 10.1 ClassificationMargin（分类得分差）

正式定义（V2.0.2）：

```
ClassificationMargin =
  Top-1 Membership Score − Top-2 eligible adjacent Membership Score      ∈ [0, 1]
```

- **Top-1 = Primary**（Membership Score 最高者）；
- **Top-2 = Primary 的 transitionTargets 中 Membership Score 最高的有效候选**（即 Secondary）；
- 它不是几何距离，不是时间距离，不是概率，不是 Confidence；
- Secondary 仅取 Primary 的 transitionTargets 中得分最高者；无有效相邻候选时 Secondary = null，此时不计算 Margin、不进入过渡态；
- 该值越小，表示 Primary 与最接近的相邻人格越难区分。
### 10.2 Transition Threshold（δ）

推荐：`δ = 0.15`。

校准意义（V2.0.7）：`δ = 0.15` 是**产品上的 transition threshold / calibration parameter**——当 ClassificationMargin < 0.15 时，Primary 与最接近的相邻候选得分差很小，产品上视为"正在过渡"。它**不构成统计显著性声明**：Membership Score 是确定性隶属度量，不是统计估计量，无标准误 / bootstrap / 置信区间；该阈值与 0.60、n≥5 一样属于 calibration parameter。δ 可随样本量增长收窄（未来可用 `δ(n) = 0.15 − 0.05·min(1, n/20)`，本阶段固定 0.15）。

### 10.3 状态输出

```
Primary    = 得分最高者（同分按 §5.3 的 deterministic tie-break）
Secondary  = Primary 相邻定义中得分最高者（无相邻则 null）
isTransitioning = ClassificationMargin < δ
若 S_primary < minMembershipScore（0.25）→ Boundary / Insufficiently Classified
```

- 当 Primary ∈ {WOLF, CHAMELEON}（Special Gate）时，按 §10.7：ClassificationMargin = null、isTransitioning = false，Secondary 仍按 transitionTargets 计算。

Transition 表示"当前特征空间中两个**相邻**人格的 Membership Score 接近"。它：
- 不是 Achievement；
- 不是 Modifier；
- 不是 Goal Alignment；
- 不直接改变 Primary（Primary 恒为得分最高者）。

未来 UI 可展示："你正在从 夜猫子 → 守序犬 过渡"。

### 10.4 单调性约束

同一用户连续计算时，除非数据发生真实变化，人格不得在两个不相邻区域间跳变；相邻区域跳变必须伴随 C 或 D 越过对应边界。

特殊 Gate 的进入/退出遵循 §10.6 的 Special-Gate Transition，不受几何邻接约束。

### 10.5 Transition Stability / Hysteresis（未来预留）

V2.0.1 **不使用历史人格状态**参与 Primary 计算（当前模型无记忆），因此不声称已解决抖动问题。未来可预留以下机制（reserved fields / future mechanism，本阶段不实现）：

```
previousPrimary
previousSecondary
switchHysteresis
consecutiveEvidenceCount
```

### 10.6 Geometric Transition 与 Special-Gate Transition（V2.0.2）

transitionTargets 包含两种语义关系：

- **A. Geometric Transition（几何过渡）**：普通 Base C×D Regions 之间的语义邻接关系（两个基础定义之间，如 NIGHT_OWL ↔ HOUND）；
- **B. Special-Gate Transition（特殊 Gate 过渡）**：涉及 CHAMELEON / WOLF 的过渡关系——**由 Gate 解除/恢复后最相关的普通 Personality 候选所形成的语义过渡关系**（"最相关"的确定性选择规则见 §10.7）：
  - CHAMELEON **不**与所有人格"几何相邻"：其 transitionTargets = 全部 10 个基础定义（当 R 恢复到 0.33 以上时，按当时的 C/D 进入其中 Membership Score 最高的有效候选，见 §10.7）；
  - WOLF 的候选**不是**纯 C/D 空间邻接：其 transitionTargets = {NIGHT_OWL, OWL}（相位恢复正常后的候选集合，选择规则见 §10.7）。

**Secondary 总原则不变**：`Secondary = Primary 的 transitionTargets 中 Membership Score 最高的有效候选；无有效候选则 null`。不恢复"全人格取次高分"的旧逻辑。

### 10.7 Special Gate 的执行优先级与返回语义（V2.0.3）

**执行优先级（control-flow precedence，非 score precedence）**：

```
WOLF > CHAMELEON > Base Classification
```

**Primary 返回语义**：
- WOLF / CHAMELEON Gate 触发时，Primary 直接由控制流确定；
- 本模型**从未定义** WOLF / CHAMELEON 的 Membership Score（两者无 centerC / centerD，见 §7.11 / §7.12；§22 中心列均为 "—（gate）"）；
- 因此 Gate Primary **不参与**普通 Base Membership Score 的任何 argmax 或 margin 计算；
- Gate Primary **不经过** `minMembershipScore` 检查：`minMembershipScore = 0.25` 仅适用于 Base Classification（§5.3 / §18）；不得出现"WOLF/CHAMELEON 因 score < 0.25 而失效"的解释——**Gate 的成立条件本身就是其 Primary 有效条件**。

**Secondary 规则（Gate Primary）**：
- `Secondary = Primary 的 transitionTargets 中 Membership Score 最高的有效候选；无有效候选则 null`；
- WOLF → transitionTargets = **{NIGHT_OWL, OWL}**；Secondary = 两者中按统一 tie-break（Score → priority → stable ID）选出的最高有效候选（eligibility 见 §5.3）。因 WOLF 的 C 为 CHRONOTYPE_OUT_OF_DOMAIN（undefined，§4.3 / §4.9），这两个候选的 m_C 不可计算（S 不可得），在 eligibility 规则下 Secondary 通常为 null（确定性结果）；
- CHAMELEON → transitionTargets = **全部 10 个基础定义**（R 恢复后按当时 C/D 可能进入任意基础定义）；Secondary = 这 10 个候选按统一 tie-break 选出的最高有效候选（eligibility 见 §5.3）；该规则将"最相关"**确定性化**，无主观语义；
- 不得因 Primary 来自 Special Gate 而恢复"全人格取第二高分"。

**ClassificationMargin / isTransitioning（Gate Primary，唯一一致方案 = 方案 A）**：
- 当 Primary ∈ {WOLF, CHAMELEON} 时：`ClassificationMargin = null`（不参与计算），`isTransitioning = false`；
- Gate Primary **不计算** Top-1 / Top-2 Base Membership Margin；Gate **不参与** Base argmax；
- Gate **不因** Secondary 候选的 Base Membership Score 接近而自动 Transition（isTransitioning 固定为 false，即使 Secondary 存在且得分接近）；
- 若 Gate Primary 存在 Secondary，它仅用于展示 / 恢复候选，不参与 margin；
- 理由：Gate 无 Membership Score，`Top-1 − Top-2 eligible adjacent`（§10.1）对 Gate Primary 无法定义；其"过渡"语义由 §10.6 的 Special-Gate Transition（Gate 解除/恢复）表达，不通过 ClassificationMargin；
- 当 Primary 为基础定义时，ClassificationMargin 按 §10.1 照常计算。

---

## 11. Cold Start Model（冷启动模型）

### 11.1 定义

- **effectiveSessions**：具备有效 `sleepStartAtMs` 的会话数（= 有效 onset sessions，§2）；Formal Personality 的 minimum evidence，阈值 = 5（V2.0.7 统一概念，P1-D）；
- **Cold Start Profile**：真实数据不足（effectiveSessions < 5）时，使用用户首次填写的当前作息 `currentSleepTime / currentWakeTime` 作为自报基线；
- **Formal Personality**：effectiveSessions ≥ 5 时，由真实 SleepSession 的统计（C、D、R）计算。

自报基线 ≠ 真实行为。冷启动人格的置信度有硬上限。

### 11.2 本阶段规则（已批准：Hard Switch）

```
effectiveSessions < 5：展示 Cold Start Personality = f(currentSleepTime, currentWakeTime)
effectiveSessions ≥ 5：展示 Formal Personality = f(真实 sessions)
```

- 冷启动人格标记为"初步倾向（低置信度）"，conf 上限 0.35；
- 目标作息不参与冷启动人格计算；
- 切换为硬切换，不实现渐进融合。
- 冷启动同样在 12 个第一阶段 PersonalityDefinition 体系内输出初步人格，但不产生正式 Confidence 与 Transition 状态。

### 11.3 未来扩展（预留，不实现）

预留 Prior / Bayesian / Weighted Transition：

```
priorWeight(n) = max(0, 1 − n / 5)
finalVector(n) = priorWeight(n) · baselineVector + (1 − priorWeight(n)) · observedVector
```

该公式仅作为理论预留；在获得更多真实数据验证前不启用。

---

## 12. Goal Alignment Model（目标重合度模型）

### 12.1 定义

```
startDeviation    = circDist(μ_C, targetSleepTime)
wakeDeviation     = circDist(μ_W, targetWakeTime)
durationDeviation = |μ_D − targetDuration|

OverallAlignment = 1 − min(1, (0.5·startDeviation + 0.3·wakeDeviation + 0.2·durationDeviation) / 240)
                  ∈ [0, 1]
```

- 单位：分钟；阈值 240 分钟为"完全错位"尺度。
- 目标作息数据缺失时，GA 不参与任何计算（安全降级）。

### 12.2 职责边界（强制）

GA **允许**影响：
- confidence（alignment penalty，上限 0.20，见 §4.6）；
- narrative / secondary descriptor（文案级，指"目标与现实存在较大差距"等描述文字，**非** Secondary Personality）；
- narrative / recommendation。

GA **禁止**影响：
- primaryType（人格类型）；
- 人格区域判定（C、D、R 均不含目标作息）。

正确示例："你是夜猫子，但你设定的目标是 23:00 入睡，目前现实作息与目标存在较大差距。"
错误示例："因为你目标 23:00，所以你不是夜猫子。"

---

## 13. Moon Observation Modifier Model（月亮观察修饰模型）

### 13.1 MoonObservationProfile

由月亮/侦探弱信号聚合，字段（理论定义）：

```
recentLateNights      （近因熬夜次数）
nightActivityHints    （夜间活动线索均值）
sleepQualityHints     （合格率趋势）
moonRewardPending     （满月奖励待展示）
```

### 13.2 修饰层（Modifier Layer）

```
confidenceModifier   ∈ [−0.05, +0.05]
evolutionModifier    ∈ [−0.10, +0.10]
narrativeModifier    （文案级）
```

### 13.3 硬性约束

- 单晚观察对任意区域得分的影响 `|ΔS_i| < 0.02`；
- MoonObservation **不得**直接改变 Primary Personality；
- 禁止出现"昨晚手机玩到 4 点 → 今天人格变成夜行幽灵"式跳变；
- 未来可用作二级描述："夜猫子 + 夜间活动频繁"。

---

## 14. Personality Explanation Model（人格解释模型）

未来点击人格展示以下内容，每一项必须可追溯到数据：

```
【你为什么是这个人格】
Primary Evidence：
  平均入睡时间（环形平均）    ← μ_C
  平均起床时间（环形平均）    ← μ_W
  平均睡眠时长              ← μ_D
  规律度                  ← R
Secondary Evidence：
  与目标作息偏差            ← GA（若存在目标作息）
  最近趋势                ← 近期 C、D 移动方向
  Moon Observation        ← MO 修饰（若有）
Confidence：高/中/低（按 conf 分档：≥0.7 / ≥0.45 / <0.45）
Transition：正在向 XX 型靠近（若 isTransitioning）
```

要求：人格结论与数据之间必须存在一一对应关系，禁止无数据支撑的文案。

---

## 15. Personality Definition Data Structure（人格定义数据结构）

理论数据模型（非 Kotlin 代码）：

```
PersonalityDefinition {
  id                     // 稳定标识
  name                   // 中文正式名称
  animal                 // 动物基底
  modifier               // 行为修饰词
  emoji[2]               // 双 emoji（动物 + 行为/文化）
  englishName            // 英文内部名称
  centerC                // 正式评分中心（C 轴）
  centerD                // 正式评分中心（D 轴）
  chronotypeRegion       // C 轴区间
  durationRegion         // D 轴区间
  regularityRange        // R 约束（可选）
  priority               // 仅用于精确同分决胜（第二优先级）；不得覆盖更高 Membership Score
  minMembershipScore     // Membership Score 阈值（默认 0.25，V2.0.7）；与 Confidence 分离
  description            // 官方描述
  culturalMeaning        // 文化/网络语义依据
  explanationTemplate    // 可解释性模板
  transitionTargets      // 相邻区域列表
}
```

V2.0.1 已移除语义模糊的 `minConfidence` 字段：分类阈值统一为 `minMembershipScore`（Membership Score 层面），把握度由全局 Confidence（§4.6）单独表达。

该结构支持：新增人格 = 新增一条定义；不修改核心匹配算法。

`transitionTargets` 含两种语义：Geometric Transition（Base C×D 区域间邻接）与 Special-Gate Transition（CHAMELEON / WOLF 的 Gate 解除/恢复候选），见 §10.6。

---

## 16. Extensibility Architecture（可扩展性架构）

```
新增一个区域/人格     → 新增 PersonalityDefinition（不动 Matcher）
新增统计特征         → 扩展 Feature Extraction Layer
新增表现（emoji/文案）→ 扩展 Presentation Layer
新增成就             → 扩展 Achievement Layer
新增修饰             → 扩展 Modifier Layer
```

分层约束：Feature / Classification / Presentation / Achievement / Modifier 五层之间只允许单向依赖（Feature → Classification → Presentation；Achievement / Modifier 独立，不进入 Classification）。

---

## 17. Achievement / Modifier Separation（成就与修饰分离）

| 概念 | 定义 | 示例 |
|---|---|---|
| Primary Personality | 你是什么样的睡眠行为者 | 夜猫子 |
| Achievement | 你曾经做过什么（里程碑） | 连续 7 天早起；满月守护 |
| Modifier | 你最近发生了什么 | 昨晚夜间活动异常 |

现有类型重定义（只定义归属，不删除代码）：

| 现有类型 | V2 归属 | 说明 |
|---|---|---|
| NIGHT_OWL | Primary（保留，重定义区域为 Late × Medium） | 夜猫子 |
| EARLY_BIRD | Primary（保留） | 早起鸟 |
| WORK_HORSE | Primary（保留） | 牛马 |
| OTTER | Primary（**语义重定义**为 Neutral × Long × R≥0.6） | 安睡海獭 |
| CHAOS | Replace → CHAMELEON（R < 0.33 区域） | 变色龙 |
| MOON_GUARDIAN | Achievement | 满月守护（成就，非人格） |
| NIGHT_GHOST | Achievement / Modifier | 连续熬夜相关成就/修饰 |
| PROCASTINATOR | Achievement | 短睡拖延相关成就 |

---

## 18. Validation Rules（校验规则）

1. 类型判定唯一输入为真实行为统计（C、D、R）；
2. 目标作息只进入 GA / confidence / narrative；
3. 月亮观察只进入 Modifier 层；
4. 单晚观察不得改变 primaryType；
5. 所有候选人格必须通过 §8.2–§8.5 四项测试；
6. 所有数学量必须注明范围、单位、来源、是否 circular；
7. 人格变更必须连续（反事实测试）；
8. 新增人格不得破坏既有人格测试（回归约束）；
9. 冷启动与正式人格必须分离；
10. Achievement / Modifier 与 Personality 必须分离。
11. Secondary 只允许来自 Primary 的 transitionTargets（相邻定义）；
12. Membership Score（分类）与 Confidence（把握度）严格分离；
13. Duration 的派生变量（median / 离差 / 短睡比例 / 长睡比例）不得进入 D。
14. Primary 判定必须 deterministic：Score → priority → stable ID（§5.3）；
15. Membership Score 是确定性隶属度量，不是概率或置信度（§5.1）；
16. WOLF gate 的唯一条件 = daytimeOnsetRatio ≥ 0.60 且有效 onset ≥ 5（§4.8）；μ_C 不参与 Gate；单条异常 session 不触发。
17. Special Gate 是 control-flow precedence（WOLF > CHAMELEON > Base），不与 Base Membership Score 竞争；Gate 触发即确定 Primary（§5.2 / §10.7）。
18. minMembershipScore（0.25）仅适用于 Base Classification；Gate Primary 不经过该检查（§10.7）；
19. Boundary（S_primary < 0.25，或 C 轴不可判定 §4.9）输出 Primary=null / Secondary=null / ClassificationMargin=null / isTransitioning=false；Boundary 不是额外人格（§5.3）；
20. Secondary 有效候选须 ∈ transitionTargets 且 Score ≥ 0.25；候选同分时使用统一 tie-break（§5.3）。
21. daytimeOnset 为三态（TRUE / FALSE / UNKNOWN）；UNKNOWN（n<5）不得被当作 FALSE，不得触发 CHAMELEON（§4.8 / §5.2）。
22. C undefined（CHRONOTYPE_OUT_OF_DOMAIN / CHRONOTYPE_MEAN_UNDEFINED）→ Boundary（§4.9）；禁止 C=0 / 最近人格中心 / 任意强制映射 fallback。

### 18.1 Primary Dependency Matrix（依赖矩阵）

图例：YES = 直接参与；INDIRECT = 仅作为上层统计的输入间接参与；WEAK = 允许但幅度受限（上限明确）；NO = 禁止参与；— = 不适用。

| 输入 | Primary | Secondary | Confidence | Modifier | Achievement | Narrative |
|---|---|---|---|---|---|---|
| μ_C（入睡环形平均） | YES | INDIRECT | INDIRECT | NO | NO | YES |
| μ_W（起床环形平均） | YES | INDIRECT | INDIRECT | NO | NO | YES |
| μ_D（时长算术平均） | YES | INDIRECT | INDIRECT | NO | NO | YES |
| R（规律性） | YES（边界判定） | YES | YES（0.35 权重） | NO | NO | YES |
| Target（目标作息） | **NO** | NO | YES（penalty ≤ 0.20） | NO | NO | YES |
| GoalAlignment | **NO** | NO | YES（经 penalty） | NO | NO | YES |
| MoonObservation | **NO** | NO | WEAK（±0.05） | YES | NO | YES |
| Achievement | **NO** | NO | NO | NO | — | YES |
| Streak | **NO** | NO | NO | NO | YES | YES |

强制约束（与 P0 一致）：
- Target → Primary = **NO**；MoonObservation → Primary = **NO**；Achievement → Primary = **NO**；Streak → Primary = **NO**；
- μ_C / μ_W / μ_D / R → Primary = **YES**（唯一允许进入分类的真实行为统计量）；
- 任何输入都不得绕过上述矩阵改变 Primary Personality。

---

## 19. Current Implementation Mapping（现行实现映射）

| V1 现状 | V2 目标 | 差异 |
|---|---|---|
| `determineType` if-else 链 | 区域评分 + 决策规则 | V2 以 P=(C,D,R) 为输入 |
| 普通平均（STEP 2 已修复为环形） | 环形统计（已实现） | ✅ 一致 |
| 冷启动用 target（STEP 3 已修复为 current） | 冷启动用 current（已实现） | ✅ 一致 |
| 5 类型 + 3 隐藏 | 12 个 PersonalityDefinition（9 Base C×D Regions → 10 Base Definitions → +2 Special Definitions）+ 成就分离 | 类型扩容 + 归属调整 |
| `PersonalityInput.sleepTargetDeviationMin` 只影响 confidence | GA 模型 | 语义一致，V2 正式建模 |
| residents/bubble 由 type 派生 | Presentation Layer | 保持一致 |
| `PersonalityMetrics`（avgStart/avgWake/avgDuration/regularity） | C、D、R + GA + MO | 扩展 |
| Confidence 公式（V2：0.25 + 0.08·min(n,8) + …） | V2.0.1 sampleFactor 公式（§4.6） | 消除 n=8 过早饱和 |

---

## 20. Future Implementation Requirements（未来实现要求）

1. 将 `determineType` 迁移为区域评分（保留现行判定规则为默认区域定义）；
2. 新增 `PersonalityDefinition` 数据模型与区域注册表；
3. 实现 `PersonalityMatcher`（全定义 Membership 评分 + 相邻 Secondary + ClassificationMargin 过渡 + minMembershipScore）；
4. 实现 `GoalAlignment`（start/wake/duration 偏差 + OverallAlignment）；
5. 实现 `MoonObservationProfile`（Modifier 层）；
6. 扩展 `PersonalityMetrics` → `SleepBehaviorProfile`（C、D、R、短睡/长睡比例）；
7. 保留现有测试并新增 §21 测试矩阵；
8. 不引入任何第三方依赖、数据库、远程规则。
9. 按 §20.1 的数据流实现正式 Matcher；GA / Moon / Achievement 不得回流 Classification。
10. Primary 判定必须实现 §5.3 的 deterministic tie-break（Score → priority → stable ID）。

### 20.1 正式 Matcher 数据流（实现顺序）

```
SleepSession[]
  ↓ Feature Extraction（环形平均/离差、时长统计、completeness、n）
SleepBehaviorProfile（μ_C、μ_W、μ_D、σ 系列）
  ↓ 标准化
C / D / R / daytimeOnsetRatio / daytimeOnset
  ↓ Special Gate（WOLF → CHAMELEON）
Personality Registry（12 个第一阶段 PersonalityDefinition）
  ↓ Membership Scoring（S_i = m_C × m_D × m_R）
Primary / Secondary（Secondary 限相邻定义）
  ↓ ClassificationMargin
Transition（δ = 0.15）/ Boundary（minMembershipScore = 0.25）
  ↓ Confidence（§4.6）
Goal Alignment / Moon Modifier / Achievement（只读注入）
  ↓
Presentation
```

约束：Goal Alignment、Moon Modifier、Achievement 只能在下游（Confidence / Modifier / Narrative / Presentation）被消费，**不得回流进入 Classification**（即不得影响 Primary / Secondary / Membership Score）。

**权威伪代码（V2.0.4，唯一合法执行顺序）**：

```
INPUT: SleepSession[]（有效数据检查：effectiveSessions ≥ 5，否则走冷启动，不执行本节）

# 特征提取与标准化（§4）
μ_C, μ_W, μ_D, σ_onset, σ_wake, σ_dur, completeness
C（夜间弧内为数值，否则 CHRONOTYPE_OUT_OF_DOMAIN / CHRONOTYPE_MEAN_UNDEFINED，§4.3/§4.9）, D, R
daytimeOnsetRatio, daytimeOnset（三态 TRUE/FALSE/UNKNOWN，§4.8）

# STEP 1 — WOLF Gate
if wolfGatePasses(daytimeOnset == TRUE):           # TRUE = ratio ≥ 0.60 AND n ≥ 5
    Primary = WOLF
# STEP 2 — CHAMELEON Gate
elif chameleonGatePasses(daytimeOnset == FALSE, R < 0.33):
    Primary = CHAMELEON
# UNKNOWN（n<5）：不触发任何 Special Gate → Base / 冷启动
#   （INPUT 门槛已保证正式 Matcher 内 n ≥ 5；此分支仅用于杜绝证据不足被当作 FALSE）
# STEP 3 — Base Classification
else:
    if C == undefined:                        # CHRONOTYPE_OUT_OF_DOMAIN / MEAN_UNDEFINED
        → Boundary / Insufficiently Classified
          （C axis unavailable；禁止 C=0 / 最近中心 / 任意强制映射 fallback，§4.9）
    else:
        scores = scoreAllBasePersonalities()           # 10 个基础定义，S_i = m_C × m_D × m_R
        primaryCandidate = deterministicArgmax(scores) # tie-break: Score → priority → stable ID
        if primaryCandidate.score < minMembershipScore (0.25):
            → Boundary / Insufficiently Classified
              （Primary=null, Secondary=null, Margin=null, isTransitioning=false）
        else:
            Primary = primaryCandidate

# Secondary（Primary 确定后，统一规则）
Secondary = bestEligibleAdjacent(Primary.transitionTargets)
            # eligibility: ∈ transitionTargets AND S ≥ 0.25
            # tie: Score → priority → stable ID；无有效候选 → null

# ClassificationMargin / isTransitioning
if Primary ∈ {WOLF, CHAMELEON}:
    ClassificationMargin = null; isTransitioning = false          # §10.7
elif Primary == null（Boundary）:
    ClassificationMargin = null; isTransitioning = false          # §5.3
elif Secondary == null:
    ClassificationMargin = null; isTransitioning = false
else:
    ClassificationMargin = S_primary − S_secondary               # §10.1
    isTransitioning = ClassificationMargin < 0.15                 # §10.2

# Confidence（§4.6）
sampleFactor = min(1, n/14)
baseConfidence = 0.20 + 0.30·sampleFactor + 0.35·R + 0.10·completeness
conf = clamp(baseConfidence − alignPenalty, 0, 0.95)

FINAL OUTPUT: Primary, Secondary, ClassificationMargin, isTransitioning, Confidence
```

不得出现另一套 Primary 流程；Special Gate 触发时不得再进入 Base scoring。

**权威声明（V2.0.4）**：所有其他章节必须与上述伪代码一致；若发生冲突，**以 §20.1 为执行顺序权威**。

---

## 21. Validation Checklist（质量审查清单）

- [x] 所有人格均由真实睡眠行为定义
- [x] target 不直接决定人格
- [x] Moon Observation 不直接决定人格
- [x] 每个人格都有动物基底
- [x] 每个人格至少两个有意义 emoji
- [x] 每个人格都有文化语义
- [x] 每个人格都有明确数学区域
- [x] 每个人格都有边界
- [x] 每个人格都有相邻人格
- [x] 每个人格都有过渡方向
- [x] 不存在纯幻想人格名称（幻想称号仅作成就）
- [x] Achievement 与 Personality 分离
- [x] Modifier 与 Personality 分离
- [x] 冷启动与正式人格分离
- [x] circular statistics 已纳入理论
- [x] 新增人格不需要修改核心 Matcher
- [x] 人格解释可以追溯到数据
- [x] 未来可以扩展到 20+ 人格（12 个 PersonalityDefinition + 区域细分机制）
- [x] 不依赖任何外部厂商的人格定义
- [x] 没有直接复制 Xiaomi 的人格名称
- [x] Secondary 仅限 Primary 的相邻人格定义（transitionTargets）
- [x] Membership Score 与 Confidence 严格分离
- [x] V2.0.1 不使用历史人格状态参与分类（Hysteresis 仅预留）
- [x] Duration 派生变量（median/离差/短睡/长睡比例）不进入 D
- [x] 动物语义为意象载体，不主张生物学对应
- [x] Primary 判定 deterministic（Score → priority → stable ID）
- [x] Membership Score 不是概率/置信度
- [x] WOLF gate 使用 daytimeOnsetRatio（单条异常 session 不触发）
- [x] ClassificationMargin 使用 Top-1 与 eligible adjacent Top-2
- [x] Special Gate 控制流优先级正式化（WOLF > CHAMELEON > Base，触发即结束）
- [x] Gate Primary 不产生 Membership Score；ClassificationMargin = null、isTransitioning = false
- [x] Boundary 语义正式定义（Primary/Secondary/Margin/isTransitioning 全 null/false，非额外人格）
- [x] Secondary eligibility 正式定义（∈ transitionTargets 且 S ≥ 0.25）
- [x] Gate Primary 不经过 minMembershipScore（0.25）
- [x] §20.1 为唯一执行顺序权威（冲突时以它为准）
- [x] daytimeOnset 三态化（TRUE / FALSE / UNKNOWN；UNKNOWN ≠ FALSE，不触发 CHAMELEON）
- [x] C 有效定义域 = 夜间弧；日间窗口 → CHRONOTYPE_OUT_OF_DOMAIN（禁止 C=0 / 最近中心 fallback）
- [x] Membership 联合校准（w=2/3、minMembershipScore=0.25）覆盖 [−1,1]²，无几何 Boundary 空洞

---

## 22. Appendix：完整人格定义总表

| ID | 人格 | 动物 | Emoji | 中心(C,D) | Chronotype | Duration | Regularity | 核心语义 | 典型行为 | 相邻人格 |
|---|---|---|---|---|---|---|---|---|---|---|
| ROOSTER | 报晓鸡 | 公鸡 | 🐓⏰ | (−2/3, −2/3) | 早（C<−1/3） | 短（D<−1/3） | 无硬约束 | 早醒但睡不够 | 22:40 前睡、时长<7h20 | 早起鸟 / 牛马 |
| EARLY_BIRD | 早起鸟 | 鸟 | 🐦🌅 | (−2/3, 0) | 早 | 中 | 无硬约束 | 早睡早起、时长正常 | 早睡、睡足 7–8.5h | 报晓鸡 / 安眠熊 / 守序犬 |
| BEAR | 安眠熊 | 熊 | 🐻❄️ | (−2/3, +2/3) | 早 | 长 | 无硬约束 | 早睡长眠 | 早睡、时长>8h40 | 早起鸟 / 树懒 / 安睡海獭 |
| WORK_HORSE | 牛马 | 牛/马 | 🐂💼 | (0, −2/3) | 中性 | 短 | 无硬约束 | 高负荷、睡不够 | 22:40–01:20 睡、时长<7h20 | 守序犬 / 报晓鸡 / 夜蝙蝠 |
| HOUND | 守序犬 | 犬 | 🐕⏰ | (0, 0) | 中性 | 中 | 无硬约束 | 规律准点 | 常规作息、波动小 | 早起鸟 / 夜猫子 / 牛马 / 树懒 / 安睡海獭 |
| SLOTH | 树懒 | 树懒 | 🦥🛋️ | (0, +2/3) | 中性 | 长 | R<0.6 | 长睡但时间漂移 | 睡得久、每天时间不一 | 安睡海獭 / 守序犬 / 昼眠枭 |
| OTTER | 安睡海獭 | 海獭 | 🦦🌊 | (0, +2/3) | 中性 | 长 | R≥0.6 | 长睡且稳定 | 睡 9–11h、入睡波动<30min | 树懒 / 守序犬 / 昼眠枭 |
| BAT | 夜蝙蝠 | 蝙蝠 | 🦇🌃 | (+2/3, −2/3) | 晚（C>+1/3） | 短 | 无硬约束 | 深夜活动、睡眠不足 | 01:20 后睡、时长<7h20 | 夜猫子 / 牛马 |
| NIGHT_OWL | 夜猫子 | 猫 | 🐱🌙 | (+2/3, 0) | 晚 | 中 | 无硬约束 | 深夜精神、时长正常 | 01:00–03:00 睡、睡 7–8.5h | 守序犬 / 夜蝙蝠 / 昼眠枭 |
| OWL | 昼眠枭 | 猫头鹰 | 🦉☀️ | (+2/3, +2/3) | 晚 | 长 | 无硬约束 | 晚睡长眠、醒来时已进入白天/中午 | 03:00 后睡、睡 9h+ | 夜猫子 / 树懒 / 安睡海獭 |
| CHAMELEON | 变色龙 | 变色龙 | 🦎🎲 | —（R<0.33 gate） | 无（不可信） | 无 | R<0.33 | 作息随机多变（用户文案：睡眠时间和时长变化较大，未形成稳定节奏） | 入睡/起床/时长均大幅波动（示例） | 全部 10 个基础定义中最高有效候选（Special-Gate Transition，见 §10.7） |
| WOLF | 夜行狼 | 狼 | 🐺🌌 | —（日间相位 gate） | 无（日间相位） | 不限 | 无硬约束 | 昼夜颠倒、夜行 | daytimeOnsetRatio ≥ 0.60（有效 onset ≥ 5；示例：12:00 睡、20:00 起） | 夜猫子 / 昼眠枭（Special-Gate Transition） |

**空间覆盖说明**：3（相位）× 3（时长）的 9 个 Base C×D Regions 全部有区域覆盖；Neutral×Long 由 R 分裂为树懒/安睡海獭（同一区域内 2 个定义）；R<0.33 与日间相位由 2 个 Special Gates（CHAMELEON / WOLF）覆盖。**数量核对：9 个 Base C×D Regions → 10 个基础定义 + 2 个特殊定义 = 12 个第一阶段 PersonalityDefinition**。不存在"有区域无解释"。

---

## 23. Mathematical Sanity Checks（数学校验样例）

1. **Circular Mean**：`circMean(23:50, 00:10) ≈ 00:00`（普通算术平均为 12:00，属错误结果）。
2. **Chronotype（夜间弧定义域内）**：`18:00 → C = −1`；`22:40 → C = −1/3`；`23:00 → C = −0.25`；`00:00 → C = 0`；`01:20 → C = +1/3`；`02:00 → C = +0.5`；`08:59 → C = +1`。
3. **Duration**：`6h → D = −1`；`8h → D = 0`；`10h → D = +1`。
4. **Counterfactual**：真实 02:00 / 8h（C=+0.5, D=0）→ NIGHT_OWL；逐步提前至 23:30 / 8h（C=−0.125, D=0）时，C 连续穿过 +1/3 边界，经历 NIGHT_OWL / HOUND 过渡态（ClassificationMargin < 0.15）后进入 HOUND；禁止跳变到不相邻人格。
5. **R gate**：R = 0.2 时，即使 C/D 精确落在某基础定义中心，也必须先命中 CHAMELEON gate，不得绕过。
6. **daytimeOnsetRatio（单条异常不触发）**：10 条有效 onset 中仅 1 条落在日间窗口（ratio = 0.1 < 0.60）→ 不触发 WOLF。
7. **Goal 独立性**：同一组真实 sessions，target 从 23:00 改为 03:00，Primary Personality 不变；仅 GA / Confidence / Narrative 变化。
8. **Moon 独立性**：新增一次 late-night observation，|ΔS_i| < 0.02，Primary Personality 不变。
9. **WOLF gate（达标触发）**：10 条有效 onset 中 7 条为日间（ratio = 0.70 ≥ 0.60）→ WOLF（μ_C 不参与判定）。
10. **Membership exact tie**：两个定义 S_i 完全相同时 → `priority` 较高者胜出（第二优先级生效）。
11. **priority 与 stable ID 均相同**：按稳定 ID 字典序较小者胜出（第三优先级生效）。
12. **高 Score + 低 priority 胜出**：定义 A（S=0.85, priority=1）vs 定义 B（S=0.60, priority=9）→ 选 A；priority 不得覆盖明显更高的 Membership Score。
13. **ClassificationMargin 语义**：必须使用 Primary 与 eligible adjacent candidate（transitionTargets 内最高者），不得使用全人格 Top-2。
14. **Membership Score 语义**：S_primary = 0.25 表示"达到最低有效 Membership 阈值"，不表示"有 25% 概率属于该人格"或"置信度低于 25%"。
15. **CHAMELEON 文案**：R < 0.33 触发 CHAMELEON，但 user-facing explanation 不得声称"每天入睡时间都随机"。
16. **WOLF gate 优先**：WOLF gate 通过（ratio ≥ 0.60、有效 onset ≥ 5）时，即使某基础定义 Membership Score 更高，Primary 也必须为 WOLF（Base scoring 不得覆盖 Gate）。
17. **WOLF 失败 + CHAMELEON 通过**：WOLF 未通过（daytimeOnset ≠ TRUE）、且 daytimeOnset == FALSE 与 R < 0.33 成立 → Primary 必须为 CHAMELEON。
18. **两 Gate 均失败**：WOLF 与 CHAMELEON 均未触发 → 才允许进入 Base Personality scoring。
19. **两 Gate 均不触发时的 deterministic argmax**：对 10 个基础定义执行 Score → priority → stable ID 三级判定，结果确定。
20. **Special Gate Primary 的 Secondary**：Primary = WOLF / CHAMELEON 时，Secondary 仍只能来自 transitionTargets（不得全人格取次高分）。
21. **Special Gate 不与 Base score 竞争**：禁止 `argmax(WOLF score, CHAMELEON score, Base scores)`；Gate 是 control-flow，无 Membership Score。
22. **WOLF > CHAMELEON 顺序**：两者理论条件同时满足时 WOLF 优先。注：因 CHAMELEON 定义含 daytimeOnset == FALSE、WOLF 需 daytimeOnset == TRUE，实际互斥；该测试用于证明 control-flow 顺序而非实际并发场景。
23. **Gate 绕过 Base 阈值**：WOLF / CHAMELEON Gate 通过时，即使任一或全部 Base Membership Score < 0.25，Gate Primary 仍然成立（不得被 minMembershipScore 拦截）。
24. **Gate Primary 不计算 ClassificationMargin**：Primary ∈ {WOLF, CHAMELEON} → ClassificationMargin = null（可断言）。
25. **Gate Primary 不因 δ 进入 Transition**：即使 Gate Primary 的 transitionTargets 内存在得分接近的候选，isTransitioning 恒为 false。
26. **Base Primary 低于阈值 → Boundary**：S_primary < 0.25 → Primary = null（Boundary / Insufficiently Classified）。
27. **Boundary 输出确定性**：Boundary 时 Secondary = null、ClassificationMargin = null、isTransitioning = false（三值可断言）。
28. **Secondary eligibility 可测试**：候选 X ∈ transitionTargets 且 S_X = 0.24 → 不可作为 Secondary；S_X = 0.25 → 可作为 Secondary（边界值 0.25 达标）。
29. **Secondary tie 用统一 tie-break**：两个候选 S 相同 → priority 高者；priority 相同 → stable ID 字典序小者。
30. **CHAMELEON Secondary 确定性**：CHAMELEON 的 transitionTargets = 全部 10 个基础定义；Secondary = 按统一 tie-break 选出的最高有效候选（同一输入必得同一结果）。
31. **SLOTH / OTTER R=0.6 无歧义**：R = 0.6 时两者 m_R 均为 1（同中心 → S 相同），由 priority（OTTER=7 > SLOTH=6）决出；"R<0.6/R≥0.6" 仅为名义语义，不参与硬判定。
32. **§20.1 为唯一执行权威**：Primary / Secondary / ClassificationMargin / isTransitioning 的执行顺序必须与 §20.1 伪代码一致；若其他章节冲突，以 §20.1 为准。
33. **μ_C 不参与 WOLF Gate（V2.0.5）**：ratio = 0.80 但 μ_C ≈ 08:40（窗口外）→ 仍触发 WOLF（旧 AND 条件的假阴性已消除）。
34. **边界比例 60% 达标**：ratio = 0.60（5 条中 3 条日间）→ 触发 WOLF；ratio = 0.59（含小于）→ 不触发。
35. **μ_C 不救场**：构造 μ_C 落在 [09:00,18:00) 但 ratio < 0.60 → 不触发 WOLF（Gate 只看 ratio）。
36. **有效 onset ≥ 5 门槛**：n = 4 且 ratio = 1.0 → 不触发 WOLF（进入冷启动 / 数据不足路径）。
37. **n<5 即使 100% daytime 也不触发 WOLF**：n=1..4、ratio=1.0 → daytimeOnset = UNKNOWN → WOLF 不触发。
38. **n<5 不得因 daytimeOnset 状态自动触发 CHAMELEON**：n=2、ratio=0.0、R=0.20 → daytimeOnset = UNKNOWN（≠ FALSE）→ CHAMELEON 不触发。
39. **n=5、3/5 → WOLF**：ratio = 0.60 ≥ 0.60（含等于）且 n ≥ 5 → daytimeOnset = TRUE → WOLF。
40. **n=5、2/5 → 不触发 WOLF**：ratio = 0.40 < 0.60 → daytimeOnset = FALSE；若 R ≥ 0.33 亦不触发 CHAMELEON（进入 Base）。
41. **n=10、6/10 → WOLF**：ratio = 0.60 ≥ 0.60 → TRUE → WOLF。
42. **ratio = 0.60 使用 ≥**：3/5、6/10、12/20 均达标（含等于）；2/5、5/10、11/20 均不达标。
43. **insufficient evidence ≠ false**：三态语义下 n<5 → UNKNOWN；UNKNOWN 不得被任何 Gate 当作 FALSE 处理。
44. **insufficient evidence 不改变 Base Membership 数学**：S_i = m_C × m_D × m_R 在 UNKNOWN 下不变（三态仅影响 Gate 路由）。
45. **insufficient evidence 不改变 Confidence 数学**：§4.6 公式（sampleFactor = min(1, n/14) 等）在 UNKNOWN 下不变。
46. **UNKNOWN 只有一套控制流**：不触发任何 Special Gate → 进入 Base / 冷启动（§20.1 唯一权威）。
57. **C 夜间弧定义域**：μ_C = 11:59 与 12:01 → 均为 CHRONOTYPE_OUT_OF_DOMAIN（C undefined），不存在 12:00 → +1 / 12:01 → −1 断裂。
58. **C 域边界**：μ_C = 17:59 → OUT_OF_DOMAIN；μ_C = 18:00 → C = −1；μ_C = 08:59 → C = +1；μ_C = 09:00 → OUT_OF_DOMAIN。
59. **C undefined 禁止 fallback**：不得 C=0、不得选最近人格中心、不得任意强制映射（可断言）。
60. **w=2/3 覆盖性**：对不受额外 R 约束的 Base Definition，m_C ≥ 0.5（∀C∈[−1,1]）且 m_C×m_D ≥ 0.25（∀(C,D)∈[−1,1]²）——几何层无 Boundary 空洞；SLOTH / OTTER 的完整 S 受 m_R 调制，不在该结论内；极端角 C=D=−1 → m_C×m_D = 0.25 ≥ 0.25 → 非 Boundary。
61. **R 独立性**：μ_C 落入日间窗口（OUT_OF_DOMAIN）时 R 仍按 §4.5 正常计算；MEAN_UNDEFINED 时 R=undefined（均不因 C undefined 自动置 0）。
62. **阈值一致性**：活动定义中 minMembershipScore=0.25、w=2/3；旧 0.30 / 0.5 仅存于历史修订说明。

---

## 附注：实现边界声明

- 本文件为理论规范，不构成实现承诺；
- 12 个 PersonalityDefinition（9 Base C×D Regions → 10 Base Definitions → +2 Special Definitions）为第一阶段成品目录，未来可在该机制上扩展至 20+；
- 任何实现改动必须先经本文件审核通过，再映射到 Kotlin `PersonalityDefinition` / `PersonalityMatcher`。
- V2.0.1 修订仅涉及数学定义与分类边界，未改变人格目录与五层分层原则。
- V2.0.2 为规范性修订（tie-break / 术语 / 语义边界 / Special-Gate Transition / daytimeOnsetRatio / CHAMELEON 文案），未改变人格目录与核心数学模型。
- V2.0.3 仅正式化 Special Gate 的 control-flow precedence 与返回语义，未改变任何数学模型与人格目录。
- V2.0.4 为最终实现可判定性审计版：仅消除 Boundary / Secondary eligibility / CHAMELEON Secondary / priority 赋值 / SLOTH-OTTER 阈值语义 / §20.1 权威性等实现歧义，未改变任何数学模型与人格目录。
- V2.0.5 仅修正 WOLF Gate 数学语义（μ_C 从 Gate 移除，daytimeOnsetRatio 为唯一核心指标），未改变 C/D/R、Membership、Confidence、Secondary、Margin、transitionTargets 与人格目录。
- V2.0.6 将 daytimeOnset 三态化（TRUE / FALSE / UNKNOWN），消除"n<5 被当作 FALSE → CHAMELEON 误触发"风险；未改变任何数学模型与人格目录。
- V2.0.7 修复数学闭环：C 夜间弧定义域（消除 12:00 断裂）、Membership 联合校准（w=2/3、minMembershipScore=0.25）、C undefined 下游契约（§4.9）、effectiveSessions 统一、δ 校准措辞；人格目录与五层架构不变。
