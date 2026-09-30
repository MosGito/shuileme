# Legacy 冻结轨道（FROZEN）

## 范围（冻结包）

```
com.sleepshift.admin      DeviceAdminReceiver / DeviceOwner
com.sleepshift.data       SettingsRepository
com.sleepshift.engine     TimeShiftEngine 抽象 + DeviceOwner/Shizuku 实现 + EngineManager
com.sleepshift.model      SleepShiftModels
com.sleepshift.permission ShizukuManager / Permission / Capability
com.sleepshift.strategy   OffsetStrategy 三实现
com.sleepshift.time       TimezoneScheduler
com.sleepshift.notify     NotificationHelper
com.sleepshift.ui         旧三页 UI + 主题
（以及全局：AlarmReceiver / BootReceiver / AppCapabilities / DebugActivity）
```

## 规则

- ❌ 不增加新业务。
- ❌ 新系统不得依赖 Legacy。
- ✅ 只允许 bug / security / build compatibility 修复。
- `DebugActivity` 是 Legacy 调试入口（能力自检 + Shizuku 时区测试 + SystemUI 刷新测试）。
- Legacy→shuileme 反向依赖（如 `WelcomeDialog → ShuilemeNight`）属于迁移问题（K-6），须清理，不得扩大。

## 保留价值

- 三模式偏移算法纯计算部分（已被 VirtualClockEngine 复用思想）。
- DataStore/Repository 模式（已被 ShuilemeRepository 复用）。
- 引擎抽象模式（Personality Domain 可平行参考）。
- 调试/诊断工具（Debug Console）。

## MIGRATION REQUIRED

- CURRENT：`WelcomeDialog`（Legacy ui）依赖 shuileme 主题（K-6）。
- TARGET：主题常量上提共享层后，Legacy 完全隔离。

