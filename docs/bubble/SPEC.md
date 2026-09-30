# Bubble 系统规范（SPEC）

> 状态：CURRENT（SL-6~SL-9.9 实现）→ TARGET。

## 职责

- 人格气泡：失重漂浮、四壁反弹、碰撞推开、拖拽（命中中心=emoji 渲染中心）、松手惯性。
- 气泡表现状态由人格类型驱动（emoji 组合），**不重新计算人格**。
- 碎碎念（`ResidentTalkSystem`）：每日 ≤3 次、深夜触发、场景（LATE/GOOD_SLEEP/MOON/GENERAL）由人格类型 + 月亮状态选择。

## 消费

- 只消费 `PersonalityResult.type`（目标）/ 旧 `SleepPersonalityState.primaryType`（当前，K-4 相关）与 Bubble-specific state。
- 禁止 import 人格数学内部（`MembershipMath`/`Matcher`/旧引擎 classifier）。

## 依赖

- `PersonaBubblePhysics`（纯函数物理）、`ResidentEngine.bubbleEmojis`（表现映射）、`ResidentTalkSystem`（文案池）。
- 不得依赖 Moon growth / Clock / Social 内部。

## MIGRATION REQUIRED

- CURRENT：`ShuilemeHomeScreen` 内联气泡物理 + 拖拽 + 齿轮（God Screen，K-10）；气泡来源 = `personality.primaryType ?: onboarding.initialPersonality`。
- TARGET：拆 `BubbleLayer` 子组件；数据源切换为新 `PersonalityResult`。
