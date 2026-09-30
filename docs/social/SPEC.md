# Social 系统规范（SPEC）

> 状态：CURRENT（SL-6.5 / STEP 3）。

## 职责

- 人格分享卡：`PersonalityCardGenerator`（MBTI 式、≥5 晚正式、冷启动初始卡）、PNG 导出、ACTION_SEND 分享。
- 卡片文案/emoji 装饰（表现层）。

## 消费

- **只消费 `PersonalityResult`**（目标）；禁止自己调用 `PersonalityMatcher` 或旧 `SleepPersonalityEngine`。
- 可读 SleepData（会话统计展示）与 Moon（卡片数据）。

## MIGRATION REQUIRED

- CURRENT：`PersonalityCardActivity` 独立 `SleepPersonalityEngine.compute()` 且漏传 `sleepTargetDeviationMin`（K-2）。
- TARGET：卡片从统一 PersonalityResult/API 取数，单一计算入口。
