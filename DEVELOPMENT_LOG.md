# SleepShift 开发日志

> 记录重要的技术问题与解决方案。项目状态见 [PROJECT_STATUS.md](./PROJECT_STATUS.md)。
> 遇到权限错误、API 不可用、Android 版本差异、Device Owner 配置等问题时，按下列格式追加记录。

---

## 2026-08-19

### 1. Compose BOM 2026.08.00 与 AGP 8.13 不兼容

- **日期**：2026-08-19
- **问题**：首次构建 `assembleDebug` 在 `:app:checkDebugAarMetadata` 失败，报 22 个 AAR 元数据错误。`androidx.compose.*:1.12.0` 要求 compileSdk 37 且要求 AGP 9.1.0+，而当前项目 compileSdk 36 / AGP 8.13.2。
- **尝试方案**：
  1. 直接升级 AGP 到 9.x + compileSdk 37 + Gradle 9.x（改动面大，AGP 9 有破坏性改动，放弃）
  2. 查询各 BOM 版本对应的 Compose 版本，选择兼容 AGP 8.13 的版本
- **结果**：查询 BOM `2025.08.00` 的 pom，确认其解析 Compose 1.9.0（仅需 compileSdk 36）。
- **最终解决方式**：在 `gradle/libs.versions.toml` 将 `composeBom` 从 `2026.08.00` 改为 `2025.08.00`。重新构建 `BUILD SUCCESSFUL`。

### 2. Gradle 官方发行源下载极慢

- **日期**：2026-08-19
- **问题**：从 `services.gradle.org` 下载 gradle-8.14.3-bin.zip 速度仅 ~37 KB/s，预计 1 小时。
- **尝试方案**：尝试增加 retry 参数、保持连接，均无效。
- **结果**：放弃官方源。
- **最终解决方式**：改用腾讯镜像 `https://mirrors.cloud.tencent.com/gradle/gradle-8.14.3-bin.zip`，实测 ~11 MB/s，14 秒完成。并在 `gradle/wrapper/gradle-wrapper.properties` 中把 distributionUrl 指向腾讯镜像，保证以后 `gradlew` 下载快。

### 3. curl 直连 GitHub / 证书吊销检查失败

- **日期**：2026-08-19
- **问题**：`curl -L` 下载 GitHub 上的 Temurin JDK MSI 报 `curl: (56) Recv failure: Connection was reset`；下载 Google CDN 报 `CRYPT_E_REVOCATION_OFFLINE`。
- **尝试方案**：加 `--retry 3` 重试无效。
- **结果**：直连 GitHub 不稳定（时通时断），winget 下载同 URL 却成功。
- **最终解决方式**：
  - 能走 winget 的走 winget（winget 自带下载通道较稳定）
  - 其余加 `--ssl-no-revoke` 跳过证书吊销检查（Google/腾讯等可信官方源安全）

### 4. Windows PowerShell 5.1 解析含中文的 .ps1 静默失败

- **日期**：2026-08-19
- **问题**：通过 `Start-Process -Verb RunAs` 提升执行含中文字符的 PowerShell 脚本时，脚本完全不执行（日志文件未创建），但纯 ASCII 脚本同方式可正常执行。
- **尝试方案**：怀疑 UAC 未批准 / 参数传参问题，逐一排查。
- **结果**：定位到编码问题——Windows PowerShell 5.1 对无 BOM 的 .ps1 按系统代码页（中文系统为 GBK）解析，UTF-8 中文字节导致整段脚本解析失败。
- **最终解决方式**：所有需提权执行的 .ps1 脚本一律写成**纯 ASCII**（英文日志/注释），写入执行日志文件确认运行。

### 5. Android Studio 的 NSIS 安装器忽略 `/D=` 安装目录参数

- **日期**：2026-08-19
- **问题**：`android-studio-*.exe /S /D=D:\AndroidDev\Android Studio` 静默安装后，实际仍装到 `C:\Program Files\Android\Android Studio`（exit 199）。NSIS 文档要求 `/D=` 无引号且在最后，但该安装器脚本内部硬编码了安装路径。
- **尝试方案**：尝试 PowerShell 传递（带引号）、MSYS2_ARG_CONV_EXCL 禁用路径转换，均被忽略。
- **结果**：确认该版本安装器无法通过命令行指定目录。
- **最终解决方式**：先正常装到默认位置，再用 `robocopy /MOVE`（`MSYS2_ARG_CONV_EXCL="*"` 防止 `/MOVE` 被 MSYS 转换）把整个目录迁移到 `D:\AndroidDev\Android Studio`，随后以管理员脚本更新注册表 `UninstallString` / `InstallLocation` 指向新路径，并修正开始菜单快捷方式。

### 6. 命令行会话不继承新设置的环境变量

- **日期**：2026-08-19
- **问题**：安装 JDK 21 并设置机器级 `JAVA_HOME` 后，当前 bash 会话 `java -version` 仍是旧的 Java 8，Gradle 启动 JVM 也显示 1.8。
- **尝试方案**：确认环境变量已写入注册表（`[Environment]::GetEnvironmentVariable("JAVA_HOME","Machine")` 返回正确）。
- **结果**：进程环境是会话启动时快照，后续修改不实时生效。
- **最终解决方式**：命令行调用时显式指定 `JAVA_HOME='D:\AndroidDev\JDK\jdk-21.0.12.8'`；新开的终端/Android Studio 会自动读到新环境变量。

### 7. v2 产品重设计：可配置偏移 + 按显示时间恢复

- **日期**：2026-08-19
- **背景**：原方案固定 22:00/06:00 + GMT+10。升级为用户可配置的智能睡眠干预系统：开始/恢复时间连续选择、偏移量滑动条（0-180min、15 步进）、三种偏移模式、实时效果预览、Debug 测试入口。
- **关键决策**：
  1. **偏移量可变** → 动态时区 ID：`GMT±HH:MM` = 原始 UTC 偏移 + 偏移分钟（如 +135min → `GMT+10:15`）。偏移为 15 的倍数保证分钟位 ∈ {00,15,30,45}，恒为 ICU 可解析格式。
  2. **恢复按"系统显示时间"触发**：恢复时刻 = 开始时刻 +（夜间显示时长 − 偏移）。例：start 22:30、restore 06:30、offset +120 → 真实 04:30（显示 06:30）恢复，避免"08:30 突然跳回 06:30"。
  3. **三种偏移模式**：
     - FIXED：每晚 = 目标偏移
     - GRADUAL：第 N 晚 = min(step×N, target)，逐日递增至目标封顶（step 默认 30min）
     - FLUCTUATION：target ± range，候选服从三角分布（峰值在目标值，非完全随机）+ 每日变化限幅（默认 ±30min）
  4. **持久化**：Preferences DataStore（1.1.1），用户配置 `SleepShiftSettings` 与内部状态 `SchedulerState` 分层；`originalTimezoneId` 写保护（首次写入不可覆盖）。
  5. **恢复调度防重复推进**：`armedEpochDay` 标记已武装夜晚，保证同一晚策略状态只推进一次。
- **架构**：UI → ViewModel → SettingsRepository(DataStore) → OffsetStrategy → TimezoneScheduler → AlarmReceiver → `DPM.setTimeZone()`。
- **Debug 测试入口**（阶段 6 实现）：「立即偏移 / 立即恢复」UI 按钮 + adb 广播（`am broadcast -n com.sleepshift/.AlarmReceiver -a com.sleepshift.action.TEST_SHIFT`），不依赖等待真实时间。

### 8. 阶段 3：DataStore 配置持久化

- **日期**：2026-08-19
- **改动**：`SleepShiftSettings`/`SchedulerState` 完整接入 Preferences DataStore；分层固化 UI → ViewModel → Repository → DataStore；新增 `SleepShiftApplication`（Repository 单例）与 `SettingsViewModelFactory`。
- **技术决策**：
  1. **StateFlow 驱动**：VM 用 `stateIn(viewModelScope, WhileSubscribed(5s), default)` + UI `collectAsState`；UI 方法签名不变（只把 `val settings = viewModel.settings` 改为 `by ...collectAsState()`）。
  2. **settingsVersion + migrate()**：每次写入自动维护版本（当前=1），`migrate()` 提供逐版本迁移通道，供未来结构升级。
  3. **"值未变跳过写入"守卫**：setter 先比较当前值，相同则跳过，避免轮盘/滑条快速拖动产生大量冗余写盘。
  4. **原始时区写保护下沉到 Repository**：`updateSchedulerState` 只在当前为空时写入 `originalTimezoneId`。
- **验证发现（重要）**：**Device Owner 应用无法被 `am force-stop` 杀死**（进程持续存活），因此"关闭 App 再打开"的持久化验证不可行；改用 `adb reboot`（更强验证）。实测：改 enabled/偏移 → 重启模拟器 → 配置从磁盘恢复 ✅。
- **验证命令备忘**：
  - 启动：`D:/AndroidDev/AndroidSdk/emulator/emulator.exe -avd SleepShift_AVD -no-window`
  - 等待启动：`adb wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'`
  - UI 检查：`adb exec-out uiautomator dump /dev/tty`
  - 数据文件：`adb shell run-as com.sleepshift ls -la files/datastore/`（debug 可 run-as）

### 9. 阶段 4-A：TimezoneScheduler 核心

- **日期**：2026-08-19
- **改动**：新增 `com.sleepshift.time.TimezoneScheduler`（零硬编码），纯计算 `planNight`/`computeNextShiftEpoch`/`computeRestoreEpoch`/`buildShiftZoneId`；接入 FIXED/GRADUAL/FLUCTUATION；`SchedulerState` 扩展 `armed`/`nextShiftEpoch`/`nextRestoreEpoch`；Manifest 加 `SCHEDULE_EXACT_ALARM`；新增 JVM 单测（5 例）。
- **关键决策与修复**：
  1. **恢复时刻 = 开始时刻 + realWindowMin**（真实窗口 = 夜间显示时长 − **当晚实际偏移**）。注意不能用 `settings.offsetMin` 算 realWindow——GRADUAL/FLUCTUATION 下当晚偏移来自策略结果，必须用策略推进后的值。
  2. **`strategyFor` 必须放 companion**：`planNight` 在伴生对象内，Kotlin 不允许伴生对象访问实例方法，否则级联类型推断错误（表现为 `advancedState` 被推断为 `Map.Entry` 的诡异报错）。
  3. **`buildShiftTimeZoneId` 小时位补零**：`GMT+8:00` → `GMT+08:00`，格式统一（`GMT±HH:MM`）。
  4. **PendingIntent 用 `setClassName(packageName, "com.sleepshift.AlarmReceiver")` 字符串定位**：阶段 4-A 无需 Receiver 类即可编译；阶段 4-B 创建同名 Receiver 注册后即生效，无需改 Scheduler。
  5. **`armedEpochDay` 防重复推进**：同一晚重复武装时复用 `currentOffsetMin`，不再次推进策略进度。
- **验证**：`testDebugUnitTest` 5/5 通过（22:30/06:30/+120 → shift=8/19 22:30、restore=8/20 04:30、动态时区 GMT+10:00/+10:15/+11:00、渐进 30→60→90→120、未启用返回 invalid）。
- **注意**：PendingIntent 的 extra（zone_id）在 AlarmReceiver 触发时读取；取消时用相同 action+requestCode（extra 不参与 PendingIntent 身份比较）。

### 10. 阶段 4-B：Receiver 适配（含重大平台发现）

- **日期**：2026-08-19
- **改动**：新增 `AlarmReceiver`（SHIFT/RESTORE + TEST_ARM/TEST_CANCEL/TEST_SET_ZONE，goAsync+协程+15s 超时，执行后重新 arm）、`BootReceiver`（BOOT_COMPLETED/MY_PACKAGE_REPLACED，`ensureActiveWindowRestore` 处理偏移状态重启）、`DeviceAdminReceiver`（onEnabled→arm / onDisabled→cancel）、`DeviceOwner`（DPM.setTimeZone 封装）。
- **⚠️ 重大平台发现 1：`setTimeZone` 只应用 IANA 时区 ID**（Android 16 / API 36）：
  - 自定义 `GMT±HH:MM`（含整小时 `GMT+02:00`、无冒号 `GMT+10`、ISO `+08:00`）**全部静默忽略**：返回 true 但时区不变（`persist.sys.timezone` 不变）。
  - IANA ID（`Asia/Shanghai`、`Etc/GMT-2`）正常生效。
  - **影响**：v2 的 15 分钟步进偏移（0-180）在本平台无法完整实现——只有整小时总偏移可用。
  - **对策**：`TimezoneScheduler.buildShiftZoneId` 改为 IANA 感知——整小时总偏移 → `Etc/GMT±H`（POSIX 符号反转，UTC+H → Etc/GMT-H）；分数偏移回退 `GMT±HH:MM`（本平台静默无效，运行时仅告警）。
- **⚠️ 重大平台发现 2：`AUTO_TIME_ZONE=1` 时 `setTimeZone` 返回 false**：`DeviceOwner.setTimeZone` 已自动关闭（DO 可写 `Settings.Global.AUTO_TIME_ZONE`）。
- **⚠️ 环境发现 3：`SCHEDULE_EXACT_ALARM` 需授权**：Manifest 声明不自动授予（DO 亦非豁免）；模拟器 `adb shell appops set com.sleepshift SCHEDULE_EXACT_ALARM allow`；`scheduleAlarms` 无权限时回退 `setAlarmClock`（无需权限、Doze 下精确触发）。
- **修复 4：同一晚重武装 FIXED 偏移重算**：`armedEpochDay` 防重复推进守卫导致"用户改偏移后重武装复用旧偏移"（TEST_ARM 后仍是 45）。改为：同一晚重武装时 FIXED 按当前 `settings.offsetMin` 重算，GRADUAL/FLUCTUATION 保留已推进值。
- **验证**：单测 7/7；模拟器 adb——`TEST_ARM` → `SHIFT`（+180min → `Etc/GMT-3` → 时区 `+0300`）→ `RESTORE`（GMT → `+0000`）✅。
- **adb 测试命令**：
  - `am broadcast -n com.sleepshift/.AlarmReceiver -a com.sleepshift.action.TEST_ARM`
  - `am broadcast -n com.sleepshift/.AlarmReceiver -a com.sleepshift.action.SHIFT`
  - `am broadcast -n com.sleepshift/.AlarmReceiver -a com.sleepshift.action.RESTORE`
  - `am broadcast -n com.sleepshift/.AlarmReceiver -a com.sleepshift.action.TEST_CANCEL`
  - `am broadcast -n com.sleepshift/.AlarmReceiver -a com.sleepshift.action.TEST_SET_ZONE --es zone_id "Asia/Shanghai"`
- **待产品决策**：分数偏移（15 分钟步进）本平台无法经 setTimeZone 生效，需在"约束为整小时"与"保留分数偏移（部分设备可用）"间抉择。

### 11. 阶段 4-C：偏移粒度约束为整小时（产品决策落地）

- **日期**：2026-08-19
- **决策**：受平台约束（见 #10），偏移粒度改为**整小时**（0/60/120/180）。
- **改动**：
  - `OFFSET_STEP_MIN=60`；渐进步进 60/120（`MIN/MAX_GRADUAL_STEP_MIN`）；波动范围 0/60（`MIN/MAX_FLUCTUATION_RANGE_MIN`）；波动每日变化限幅 `maxDailyDeltaMin=60`。
  - `settingsVersion` **v1→v2**：`migrate()` 将旧 15 分钟步进值（offsetMin/gradualStepMin/fluctuationRangeMin）归一化为整小时；`toSettings()` 读取时防御性归一化（避免迁移前读到非法粒度）。
  - UI：偏移滑条 steps=2（0/60/120/180）、渐进步进 0/120、波动范围 0/60。
- **验证**：单测 7/7；模拟器滑条中点点击吸附到 +120（整小时）✅；`assembleDebug` ✅。
- **说明**：`buildShiftZoneId` 的 `GMT±HH:MM` 分数回退路径保留（防御，正常产品流程不再产生分数偏移）。

### 12. 阶段 5：模拟器端到端验证（发现并修复 4 个问题）

- **日期**：2026-08-20
- **验证结果**：FIXED 三档、GRADUAL 逐天推进、FLUCTUATION 波动分布、正常重启重新武装、禁用即恢复、改配置自动重新武装，全部通过（单测 12/12）。
- **问题修复**：
  1. **禁用未联动**：`setEnabled(false)` 原来只改 DataStore，不 cancel 闹钟/不恢复时区 → VM 改为：停用→`cancel()` + `applyRestore()`；启用→`arm()`。
  2. **改配置未重新武装**：arm 后改偏移/开始时间，旧闹钟仍在旧时刻按旧配置触发 → VM 所有 setter 改为写库后 `scheduler.arm()`（`armedEpochDay` 守卫保证同夜不重复推进，FIXED 同夜按新设置重算）。
  3. **FLUCTUATION 取整偏差**：`OFFSET_STEP_HALF=7` 是 15 步进旧值，整小时（60）下应取 30 → 改为 `OFFSET_STEP_MIN / 2`。
  4. **偏移状态重启决策逻辑不可测**：`ensureActiveWindowRestore` 抽纯函数 `computeActiveRestoreEpoch`（未偏移/已过→null，窗口内→恢复时刻）+ 单测。
- **⚠️ 模拟器环境发现（非应用 bug）**：模拟器（google_apis）每次启动将时区强制重置为 GMT，覆盖 `persist.sys.timezone`（禁用 GMS、auto_time=0 均无效）→ "偏移状态重启"无法在模拟器真实复现；真机 persist.sys.timezone 保留。相关逻辑已纯函数化单测兜底。
- **调试教训**：FLUCTUATION "恒 120" 一度误判为随机性问题，实为 UI 点击未真正选中模式（"+60" 是渐进步进而非波动范围）——**测试脚本要显式断言"模式已选中"再操作**。
- **adb 测试命令备忘**：
  - `TEST_FORCE_ADVANCE`：模拟新一晚，策略按日推进（GRADUAL/FLUCTUATION 验证）
  - `TEST_ACTIVE_RESTORE`：模拟偏移状态启动，触发当前窗口恢复闹钟兜底

### 13. 阶段 6：产品化 UI 与用户体验优化

- **日期**：2026-08-20
- **改动**（未重构 Scheduler/Receiver/DataStore）：
  - **首次启动引导** `OnboardingScreen`：3 步（理念介绍 → 能力检查 → 就绪+通知授权）。能力检查用普通语言（"系统时间控制权限"代替 Device Owner 等术语）。`onboardingDone` 为 DataStore 追加键（应用级状态，不混入用户配置）。
  - **今日主页**：状态「正常时间/睡眠模式」、今晚计划卡片（开始/提前/恢复）、关闭确认弹窗（"关闭后手机时间将恢复正常"）。
  - **配置页**：实时解释文本 + 睡眠模式中修改 Snackbar 提示（"修改将在下一周期生效"）。
  - **模式页**：普通语言说明 + 适合人群。
  - **通知** `NotificationHelper`：IMPORTANCE_LOW 非侵入式；`applyShift/applyRestore` 成功后发送（仅成功时，不打扰）。
- **验证**：模拟器全流程——引导三步完成进入主界面、引导持久化（重启不再显示）、主页状态/计划卡/关闭弹窗、配置解释文本、模式说明+适合人群、SHIFT/RESTORE 通知 ✅；单测 12/12。
- **小坑**：引导 Step 3 通知授权按钮的 `notificationGranted` 初始状态需查真实权限（`ContextCompat.checkSelfPermission`），否则已授权也显示按钮；Compose 局部变量引用顺序（context 声明在状态初始化之前）。
- **未在模拟器验证**：配置页"睡眠模式中修改"Snackbar——需处于活动睡眠窗口（当前模拟器时间不在窗口内），逻辑简单已代码确认。

---

## 2026-08-21

### 14. Phase 11-A：TimeShiftEngine 抽象层

- **日期**：2026-08-21
- **背景**：旧 Device Owner 通道在非 DO 环境（普通 ROM / HyperOS）不可用，需引入 Shizuku/Root 多执行模式。先抽象引擎层，保证现有 DO 能力不退化。
- **改动**：新增 `engine` 包——`TimeShiftEngine`（type/isAvailable/setTimeZone/setAutoTimeZoneEnabled）、`TimeShiftResult`（引擎无关结果）、`EngineType`（DEVICE_OWNER/SHIZUKU/ROOT/NONE）、`EngineManager`（按优先级选引擎：Shizuku > Device Owner > 无）、`DeviceOwnerTimeShiftEngine`（封装原 DeviceOwner 逻辑）。`TimezoneScheduler` 构造注入 `TimeShiftEngine`（不再直接依赖 DeviceOwner），applyShift/applyRestore 统一走引擎。`SleepShiftApplication` 接线 `engineManager`。`DeviceOwner` 补 `setAutoTimeZone`。
- **验证**：`assembleDebug` ✅；SleepShift_AVD_v2 安装/启动验证（Status:ok、进程存活、无崩溃）✅。
- **提交**：`2cb975d`（8 文件 / 153 行）。

### 15. Phase 11-B：Shizuku 能力层

- **日期**：2026-08-21
- **改动**：新增 `permission` 包——`ShizukuManager`（安装/运行/binder 状态检测，只读）、`ShizukuPermission`（isGranted / requestPermission）、`CapabilityState`（activeEngine 推导）、`CapabilityResolver`（综合能力聚合）。依赖 `dev.rikka.shizuku:api:13.1.5` + `provider:13.1.5`。Manifest 注册 `ShizukuProvider`（authorities=`${applicationId}.shizuku`）。`ShizukuTimeShiftEngine` 骨架（isAvailable = 运行 + 已授权；setTimeZone 未实现）。EngineManager 注册 Shizuku（优先）+ Device Owner。`DebugActivity` 能力调试页。
- **验证**：`assembleDebug` ✅；SleepShift_AVD_v2 验证（activeEngine=NONE、无崩溃、主流程无回归、ShizukuProvider 优雅初始化）✅。
- **提交**：`6b1dde8`（10 文件 / 275 行）。

### 16. Phase 11-C：Shizuku shell 时区修改 + 真机验证

- **日期**：2026-08-21
- **实现**：`ShizukuTimeShiftEngine.setTimeZone` 通过 `IShizukuService.newProcess` 以 `sh -c` 执行 `settings put global auto_time_zone 0` + `service call alarm 3 s16 "<zone>"`；捕获 exit/stdout/stderr；`setAutoTimeZoneEnabled` 同理。DebugActivity 加启动自检 + DebugScreen 时区测试入口；主界面加「开发测试」临时入口。
- **⚠️ 真机验证（Redmi K80 + HyperOS）**：Shizuku 安装/运行/授权 ✅、engine 执行时区命令 ✅、auto_time_zone 关闭 ✅、系统设置时区切换 ✅。
- **⚠️ 遗留问题（非权限）**：状态栏/SystemUI 时间不刷新（仍显示原时间）。定位：HyperOS timezone refresh/broadcast/SystemUI cache 问题。→ Phase 11-D 处理。
- **环境发现**：模拟器（SleepShift_AVD_v2）无 Shizuku，DebugActivity 自检正确失败（SELFTEST success=false）。

### 17. Phase 11-D：SystemUI 刷新修复 + Shizuku 授权引导

- **日期**：2026-08-21
- **D-A/B（SystemUI 刷新）**：`ShizukuTimeShiftEngine.setTimeZone` 成功后追加三类时钟刷新广播（shell best-effort）：`TIMEZONE_CHANGED`（--es time-zone）、`TIME_SET`、`TIME_TICK`；新增公开 `refreshSystemUiClock(zoneId)` 独立调试入口；DebugScreen 增「仅刷新 SystemUI（测试广播）」按钮，真机可隔离验证哪条广播对 HyperOS 生效。时区修改成功仍判成功（刷新失败不影响主操作判定）。
- **D-C（Shizuku 授权引导）**：`AppCapabilities` 识别 Shizuku 通道（`timezoneControlReady` = Shizuku 授权或 Device Owner；`timezoneControlDesc` 展示当前通道）；Onboarding Step 2 增 `ShizukuGuideCard`——未安装（前往 shizuku.rikka.app）/ 未运行（打开 Shizuku 应用）/ 未授权（授权按钮）三态引导 + 备选 Device Owner 说明；`Shizuku.addRequestPermissionResultListener` 授权后自动重查状态；`ShizukuManager.SHIZUKU_PACKAGE` 公开供引导页使用。
- **验证**：`assembleDebug` ✅（39 tasks）；`testDebugUnitTest` ✅（12/12 无回归）。
- **待真机确认**：D-A/B 三条广播对 HyperOS 状态栏的实际生效情况；D-C 授权向导实机走查。兜底方案：`killall com.android.systemui` 重启 SystemUI。
