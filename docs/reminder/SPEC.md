# Reminder 系统规范（SPEC）

> 状态：CURRENT（SL-4 / SL-9.2）。

## 职责

- `ShuilemeReminderScheduler`：睡前提醒（目标−advance）、熬夜检查点（23:00–08:00 每小时自续）、`setExactAndAllowWhileIdle` + `set` 回退。
- `ShuilemeReminderReceiver`：SLEEP/LATE/NIGHT_OFF 处理；每晚熬夜 ≤ `lateReminderMaxPerNight`（默认 2）；静默窗 23:00–08:00。
- `ShuilemeReminderNotifier`：通知渠道/动作（我要睡了/今晚放过我）。
- `SleepCapsule`：睡眠中常驻通知（真实/虚拟时间 + 月亮）。
- `ReminderTemplate`：三人格三类型文案池。

## 依赖

- 消费 SleepData（target 时间、睡眠状态）与 Clock（虚拟时间判断熬夜线）。
- **不得直接依赖 Personality 内部算法**；未来如需人格文案，只消费 `PersonalityResult`（INTERFACE ONLY）。

## MIGRATION REQUIRED

- CURRENT：`SleepCapsule`/`Notifier` 直接依赖 `MainActivity`（K-8）。
- TARGET：Intent 常量 / Deep link。
