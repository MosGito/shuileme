# SleepShift 开发状态

> 本文件长期记录项目开发状态。**每完成一个独立开发阶段，必须更新本文件。**

## 项目目标

开发一个 Android **Device Owner** 应用（SleepShift），通过修改系统时区，在用户设定的睡眠窗口内让显示时间向未来偏移，形成自然睡眠暗示（如真实 22:30 → 显示 00:00）。

**v2 升级点（2026-08-19 起）**：
- 时间/偏移全部**用户可配置**（连续交互：Wheel Picker + 滑动条），不再硬编码 22:00/06:00/GMT+10
- **恢复按"系统显示时间"触发**：显示时间达到设置的恢复时间时恢复（真实触发提前偏移量），避免"08:30 跳回 06:30"
- 三种偏移策略模式：**FIXED（固定）/ GRADUAL（逐日递增）/ FLUCTUATION（目标±范围）**
- 技术要点：`DevicePolicyManager.setTimeZone()`（Device Owner）+ `AlarmManager.setExactAndAllowWhileIdle()`；偏移量动态生成时区 ID `GMT±HH:MM`（如 +135min → `GMT+10:15`）
- 持久化：Preferences DataStore（`androidx.datastore:datastore-preferences:1.1.1`）

## 已完成功能

### 环境与项目搭建（2026-08-19）
- [x] **开发环境搭建**：JDK 21 LTS（`D:\AndroidDev\JDK\jdk-21.0.12.8`）、Android Studio 2026.1.3.7、Android SDK 36、模拟器 `SleepShift_AVD`（AEHD 硬件加速），全部部署在 D 盘
- [x] **项目创建**：Kotlin + Jetpack Compose，AGP 8.13.2 / Kotlin 2.4.10 / Compose BOM 2025.08.00 / Gradle 8.14.3，compileSdk 36 / targetSdk 36 / minSdk 26
- [x] **获取 Device Owner 授权**：`dpm set-device-owner com.sleepshift/.admin.DeviceAdminReceiver` 一次成功，`list-owners` 验证 ✅

### v2 重设计（2026-08-19）
- [x] **产品架构重设计**：从固定 22:00/06:00 + GMT+10 升级为可配置的智能睡眠干预系统（时间连续选择、偏移滑动条 0-180min/15 步进、三种模式、实时预览、Debug 测试入口），详见 DEVELOPMENT_LOG #7
- [x] **阶段 1：数据模型**（`assembleDebug` ✅）
  - 新增 `SleepShiftSettings`（用户配置：enabled / startTimeMin / restoreTimeMin / offsetMin / mode / 模式参数）+ `SchedulerState`（内部运行时：进度、前一晚偏移、当前偏移、原始时区、武装夜晚标记）
  - 新增 `NightWindow` 推导模型，**编码 v2 恢复逻辑**：`realWindow = nightLength - offset`（恢复=显示时间达 restoreTime 那一刻）+ 配置合法性校验
  - 新增 `OffsetStrategy` 三实现：`FixedStrategy` / `GradualStrategy`（min(step×N, target)）/ `FluctuationStrategy`（三角分布 + 每日变化限幅）
  - 新增 `SettingsRepository`（Preferences DataStore）：用户配置/内部状态分层，`originalTimezoneId` 写保护
  - 新增纯函数 `buildShiftTimeZoneId(originalOffsetMillis, offsetMin)` → 动态 `GMT±HH:MM`
  - 依赖：`androidx.datastore:datastore-preferences:1.1.1`
- [x] **阶段 2：Compose UI**（`assembleDebug` ✅）
  - 底部导航三页：今日 / 配置 / 模式（状态式导航，未引入 navigation-compose）
  - 自研 `TimeWheelPicker`：双列轮盘（小时 0-23 + 分钟 0/15/30/45），滚动居中吸附
  - `OffsetSlider`（0-180min、15 步进）+ `LivePreview`（真实时间/偏移/显示时间，每秒刷新）
  - 配置页「今晚效果」卡片（开始/恢复 真实时刻 → 显示时刻）
  - 模式页：三模式卡片 + 参数滑条（渐进步进 / 波动范围）+ 渐进序列预览（复用策略引擎）
  - UI 由 `SettingsViewModel`（内存 `mutableStateOf`）驱动，与 `SleepShiftSettings` 完全兼容（阶段 3 接 DataStore）
  - 依赖：`androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1`
- [x] **阶段 3：配置保存（DataStore 持久化）**（`assembleDebug` ✅ + 模拟器验证 ✅）
  - 分层固化：UI → SettingsViewModel → SettingsRepository → DataStore，Repository 为唯一数据入口
  - 新增 `SleepShiftApplication` 持有 Repository 单例；`SettingsViewModelFactory` 注入 ViewModel
  - `SettingsViewModel` 改 StateFlow（`stateIn` + `collectAsState`），UI 方法签名不变，修改即保存
  - Repository 增加 `settingsVersion`（版本 1）+ `migrate()` 逐版本迁移框架（未来结构升级通道）
  - setter 加"值未变跳过写入"守卫，避免轮盘/滑条拖动产生冗余写盘
  - **模拟器验证**：改 enabled（关→开）、偏移（120→45 分钟）→ 重启模拟器 → 配置从磁盘恢复 ✅
  - 技术发现：DO 应用 `am force-stop` 受保护（杀不掉），持久化验证改用模拟器重启
- [x] **阶段 4-A：TimezoneScheduler 核心**（单测 5/5 ✅ + `assembleDebug` ✅）
  - 全新 `TimezoneScheduler`：**零硬编码**（无 22:00/06:00/GMT+10/固定 120），全部来自 `SleepShiftSettings`
  - 纯计算（JVM 可单测）：`planNight` / `computeNextShiftEpoch`（原始时区下次 startTime）/ `computeRestoreEpoch`（= 开始 + realWindow）/ `buildShiftZoneId`
  - 恢复逻辑：真实窗口 = 夜间显示时长 − **当晚实际偏移**（+120min → 真实 04:30 恢复、显示 06:30）
  - 策略接入：FIXED/GRADUAL/FLUCTUATION 每晚推进实际偏移，`armedEpochDay` 防同夜重复推进
  - 状态管理：`SchedulerState` 新增 `armed` / `nextShiftEpoch` / `nextRestoreEpoch`（DataStore 持久化）
  - 闹钟武装：`setExactAndAllowWhileIdle` + `RTC_WAKEUP`，PendingIntent 目标 `AlarmReceiver`（阶段 4-B 注册）；Manifest 增 `SCHEDULE_EXACT_ALARM`
  - JVM 单测（`testDebugUnitTest`）：22:30/06:30/+120 → shift=22:30、restore=04:30 等 5 例全过
  - 修复：`strategyFor` 移入 companion（伴生对象不能调实例方法）；`buildShiftTimeZoneId` 小时位补零
- [x] **阶段 4-B：Receiver 适配 + DPM 接入**（单测 7/7 ✅ + `assembleDebug` ✅ + 模拟器 adb 验证 ✅）
  - `AlarmReceiver`：ACTION_SHIFT / ACTION_RESTORE（**不信任 PendingIntent extra**，读 DataStore 当前状态计算时区）+ TEST_ARM / TEST_CANCEL / TEST_SET_ZONE（Debug 手动入口）
  - `goAsync` + 协程 + 15s 超时；每次执行后重新 arm()，下一周期使用新配置
  - `BootReceiver`：BOOT_COMPLETED / MY_PACKAGE_REPLACED 重新武装；`ensureActiveWindowRestore` 处理"偏移状态重启"场景
  - `DeviceAdminReceiver`：onEnabled → arm()；onDisabled → cancel()
  - `DeviceOwner`：DPM.setTimeZone 封装（DO 校验 + 自动关闭自动时区）
  - **模拟器验证**：TEST_ARM → SHIFT（+180min → `Etc/GMT-3` → 时区 **+0300**）→ RESTORE（GMT → **+0000**）✅
  - 修复：同一晚重武装时 FIXED 按当前设置重算偏移（否则改设置后下一周期不生效）
- [x] **阶段 4-C：偏移粒度约束为整小时**（单测 7/7 ✅ + `assembleDebug` ✅ + 模拟器 ✅）
  - 产品决策：受平台约束，偏移粒度改为**整小时**（0/60/120/180，`Etc/GMT±H`）
  - `OFFSET_STEP_MIN=60`；渐进步进 60/120；波动范围 0/60；波动每日变化限幅 60
  - `settingsVersion` 升到 **v2**，`migrate()` v1→v2 归一化旧 15 分钟值；读取时防御性归一化
  - UI 滑条均改整小时步进；模拟器验证中点吸附到 +120 ✅

## 当前开发阶段

**阶段 5：模拟器端到端测试**（下一步，**待确认后开始**）

- GRADUAL / FLUCTUATION 各验证一晚（FIXED 已验证）
- 模拟器重启持久性验证（BootReceiver 重新武装 + 偏移状态重启恢复）
- 边界：启用晚于开始时间、禁用即恢复、原始时区写保护

## 遇到的问题

> 详细记录（日期/问题/尝试方案/结果/最终解决方式）见 [DEVELOPMENT_LOG.md](./DEVELOPMENT_LOG.md)。此处仅列当前仍相关的事项：

- **构建相关（已解决）**：Compose BOM 2026.08.00 需 compileSdk 37 + AGP 9.1+，BOM 已锁定 `2025.08.00`。⚠️ 若升级 BOM 需同步升级 AGP/compileSdk。
- **下载相关（已解决）**：Gradle 发行源改腾讯镜像；GitHub 直连不稳定用 winget/加 `--ssl-no-revoke`。
- **环境变量**：当前 bash 会话不继承新设环境变量，命令行需显式 `JAVA_HOME='D:\AndroidDev\JDK\jdk-21.0.12.8'`。
- **v2 恢复逻辑**：恢复按**显示时间**触发（真实 04:30 恢复 +120min 偏移，显示 06:30）。⚠️ 注意系统时区在偏移窗口内确实提前 2 小时。
- **时区测试提醒**：测试会真的改模拟器系统时区，测完记得恢复。

## 已修改的重要文件

| 文件 | 说明 |
|---|---|
| `app/src/main/AndroidManifest.xml` | 声明 Application（`.SleepShiftApplication`）+ `DeviceAdminReceiver` + `RECEIVE_BOOT_COMPLETED` + `SCHEDULE_EXACT_ALARM`（阶段 4-B 注册 AlarmReceiver/BootReceiver） |
| `app/src/main/java/com/sleepshift/admin/DeviceAdminReceiver.kt` | Device Owner 接收器（阶段 4-B 加 onEnabled/onDisabled 回调） |
| `app/src/main/java/com/sleepshift/SleepShiftApplication.kt` | Application 单例容器，持有 `SettingsRepository` |
| `app/src/main/java/com/sleepshift/MainActivity.kt` | 入口 Activity，`viewModels` 注入 Repository 工厂 |
| `app/src/main/java/com/sleepshift/ui/SleepShiftApp.kt` | 底部导航壳（三页状态式切换） |
| `app/src/main/java/com/sleepshift/ui/SettingsViewModel.kt` | 设置 ViewModel（StateFlow 驱动，DataStore 持久化） |
| `app/src/main/java/com/sleepshift/ui/SettingsViewModelFactory.kt` | ViewModel 工厂（注入 Repository 单例） |
| `app/src/main/java/com/sleepshift/ui/NightPlan.kt` | 今晚窗口展示推算（真实→显示时刻） |
| `app/src/main/java/com/sleepshift/ui/home/HomeScreen.kt` | 首页：状态/预览/下次切换/总开关 |
| `app/src/main/java/com/sleepshift/ui/config/ConfigScreen.kt` | 配置页：实时预览 + 轮盘 + 滑动条 |
| `app/src/main/java/com/sleepshift/ui/mode/ModeScreen.kt` | 模式页：三模式 + 参数 |
| `app/src/main/java/com/sleepshift/ui/components/TimeWheelPicker.kt` | 自研双列轮盘选择器 |
| `app/src/main/java/com/sleepshift/ui/components/OffsetSlider.kt` | 偏移滑动条（0-180、15 步进） |
| `app/src/main/java/com/sleepshift/ui/components/LivePreview.kt` | 实时效果预览 |
| `app/src/main/java/com/sleepshift/ui/components/CurrentTime.kt` | 每秒刷新时钟 |
| `app/src/main/java/com/sleepshift/model/SleepShiftModels.kt` | **v2 数据模型**：SleepShiftSettings / SchedulerState / NightWindow / 默认值 / 校验 / `buildShiftTimeZoneId` |
| `app/src/main/java/com/sleepshift/strategy/OffsetStrategy.kt` | **偏移策略引擎**：FIXED / GRADUAL / FLUCTUATION |
| `app/src/main/java/com/sleepshift/data/SettingsRepository.kt` | **Preferences DataStore 仓库**（分层存储 + 原始时区写保护 + settingsVersion） |
| `app/src/main/java/com/sleepshift/time/TimezoneScheduler.kt` | **调度核心**：planNight 纯计算 + 策略接入 + 状态 + 精确闹钟武装 + 执行偏移/恢复 + IANA 感知时区映射（零硬编码） |
| `app/src/test/java/com/sleepshift/time/TimezoneSchedulerTest.kt` | JVM 单测：epoch/时区 ID/渐进策略/重武装重算（7 例全过） |
| `app/src/main/java/com/sleepshift/AlarmReceiver.kt` | 精确闹钟接收器：SHIFT/RESTORE + Debug 测试入口（goAsync + 协程） |
| `app/src/main/java/com/sleepshift/BootReceiver.kt` | 开机/更新后重新武装 + 偏移窗口恢复兜底 |
| `app/src/main/java/com/sleepshift/admin/DeviceOwner.kt` | DPM.setTimeZone 封装（DO 校验 + 自动关自动时区） |
| `app/build.gradle.kts` | AGP 8.13.2 / compileSdk 36 / minSdk 26 / Compose / Java 17 / + datastore |
| `gradle/libs.versions.toml` | 版本目录；BOM 锁定 `2025.08.00`；+ datastore 1.1.1 |
| `settings.gradle.kts` | 阿里云 maven 镜像加速（官方源兜底） |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 8.14.3，分发地址指向腾讯镜像 |
| `local.properties` | `sdk.dir=D:\AndroidDev\AndroidSdk`（已 gitignore，不入库） |
| `PROJECT_STATUS.md` / `DEVELOPMENT_LOG.md` | 状态与问题记录 |

## 下一步计划（v2 七阶段）

| 阶段 | 内容 | 状态 |
|---|---|---|
| 1 | 数据模型（Settings + DataStore + 策略引擎） | ✅ 完成 |
| 2 | Compose UI（导航 + Wheel Picker + 滑动条 + 实时预览，内存假数据） | ✅ 完成 |
| 3 | 配置保存（UI → ViewModel → Repository → DataStore 全链路 + 模拟器重启验证） | ✅ 完成 |
| 4-A | TimezoneScheduler 核心（纯计算 / 策略接入 / 状态 / 闹钟武装，零硬编码） | ✅ 完成 |
| 4-B | Receiver 适配 + DPM 接入 + adb 测试入口 | ✅ 完成 |
| 4-C | 偏移粒度约束为整小时（settingsVersion v2 迁移） | ✅ 完成 |
| 5 | **模拟器端到端测试**（GRADUAL/FLUCTUATION 各验证一晚；重启持久性） | ⏳ 当前 |

## 下次继续开发时需要注意的事项

- **命令行构建**：当前 bash 会话不继承新环境变量，构建必须显式：
  `JAVA_HOME='D:\AndroidDev\JDK\jdk-21.0.12.8' ./gradlew.bat assembleDebug`（在项目根目录）
- **adb / 模拟器**：adb 在 `D:\AndroidDev\AndroidSdk\platform-tools\adb.exe`；模拟器 `SleepShift_AVD` 已授权为 Device Owner，直接复用。若重置 AVD，需重新 `adb shell dpm set-device-owner com.sleepshift/.admin.DeviceAdminReceiver`。
- **不要随意升级 Compose BOM**：`2025.08.00`（Compose 1.9.0）与 AGP 8.13.2/compileSdk 36 匹配；升级到 2026.08.00 需同时升 AGP 9.1+ 和 compileSdk 37。
- **Bash 陷阱**：带斜杠的 Windows 参数（如 `/S`、`/MOVE`、`/D=`）会被 MSYS 转换，需 `MSYS2_ARG_CONV_EXCL="*"` 或改用 PowerShell；curl 到部分源需 `--ssl-no-revoke`。
- **提权脚本**：需管理员执行的 .ps1 必须**纯 ASCII**（PowerShell 5.1 按 GBK 解析无 BOM 中文脚本会静默失败）。
- **v2 调度语义**：恢复时刻 = 开始时刻 +（夜间显示时长 − 偏移）；偏移随策略每晚变化，需用 `armedEpochDay` 标记防止同夜重复推进策略状态。
- **DataStore**：`originalTimezoneId` 仅在首次启用写入（写保护）；策略状态字段仅 Scheduler 写、UI 只读。
- **DO 应用 force-stop 受限**：`am force-stop` 杀不掉 Device Owner 应用进程，App 重启/持久化验证用 `adb reboot`（更严格的验证方式）。
- **⚠️ 平台约束（已决策）：setTimeZone 只应用 IANA 时区 ID**：Android 16 (API 36) 自定义 `GMT±HH:MM` 会被静默忽略（返回 true 不生效）。**已决策约束偏移为整小时**（`Etc/GMT±H`），`OFFSET_STEP_MIN=60`，settingsVersion v2 迁移旧值。
- **⚠️ 自动时区必须关闭**：`AUTO_TIME_ZONE=1` 时 `setTimeZone` 返回 false；应用在 `DeviceOwner.setTimeZone` 内自动关闭（DO 可写）。
- **⚠️ SCHEDULE_EXACT_ALARM 需授权**：Manifest 声明不自动授予（DO 也非豁免）；模拟器需 `adb shell appops set com.sleepshift SCHEDULE_EXACT_ALARM allow`；`scheduleAlarms` 已加 `setAlarmClock` 回退（无权限时兜底）。
- **模拟器当前残留状态**：`enabled=true`、`offsetMin=120`（阶段 4-C 滑条验证产物）、已武装、时区已恢复 GMT。
- **JVM 单测**：`JAVA_HOME='D:\AndroidDev\JDK\jdk-21.0.12.8' ./gradlew.bat testDebugUnitTest`（纯计算验证，不依赖模拟器/真实时间）。
