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

## 当前开发阶段

**阶段 4：Scheduler 重构**（下一步）

- 原始时区保存（首次启用时写保护，DataStore `originalTimezoneId`）
- epoch 计算：下次 startTime（原始时区）+ `realWindowMin`（恢复按显示时间达 restoreTime 那一刻）
- 动态时区 ID：`buildShiftTimeZoneId` → `GMT±HH:MM`
- 武装/取消精确闹钟（`AlarmManager.setExactAndAllowWhileIdle` + `RTC_WAKEUP`）
- 策略状态推进（`armedEpochDay` 防同夜重复推进）+ `OffsetStrategy` 接入
- 状态写入 `SchedulerState`（DataStore），原始时区写保护复用

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
| `app/src/main/AndroidManifest.xml` | 声明 `DeviceAdminReceiver` + `RECEIVE_BOOT_COMPLETED` + 入口 Activity（阶段 5 将注册 AlarmReceiver/BootReceiver） |
| `app/src/main/java/com/sleepshift/admin/DeviceAdminReceiver.kt` | Device Owner 接收器（阶段 4/6 加 onEnabled/onDisabled 回调） |
| `app/src/main/AndroidManifest.xml` | 声明 `DeviceAdminReceiver` + `RECEIVE_BOOT_COMPLETED` + Application（`.SleepShiftApplication`） |
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
| `app/src/main/java/com/sleepshift/data/SettingsRepository.kt` | **Preferences DataStore 仓库**（分层存储 + 原始时区写保护） |
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
| 4 | **Scheduler 重构**（原始时区、epoch 计算、动态 GMT±HH:MM、武装/取消、策略推进） | ⏳ 当前 |
| 4 | Scheduler 重构（原始时区保存、epoch 计算、动态 GMT±HH:MM、武装/取消、策略状态推进 + 防重复武装标记） | 待做 |
| 5 | Receiver 适配（AlarmReceiver extra 传参 + 重新武装；BootReceiver 重启重注册；DeviceAdminReceiver 接 Scheduler） | 待做 |
| 6 | DPM 接入（`setTimeZone` 动态 ID + 防御校验；**Debug 测试入口：立即偏移/立即恢复**，UI 按钮 + adb 广播） | 待做 |
| 7 | 模拟器端到端测试（三模式各验证一晚、重启持久性、边界） | 待做 |

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
- **模拟器当前残留配置**：`enabled=true`、`offsetMin=45` 已持久化（阶段 3 验证产物），后续测试注意。
