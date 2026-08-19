# SleepShift 开发状态

> 本文件长期记录项目开发状态。**每完成一个独立开发阶段，必须更新本文件。**

## 项目目标

开发一个 Android **Device Owner** 应用（SleepShift），实现：
1. 用户授权后成为 Device Owner
2. 每天晚上 22:00 自动修改系统时区，让系统显示时间向未来偏移约 2 小时
3. 凌晨 6:00 恢复原始时区
4. 使用 Kotlin + Jetpack Compose 开发

技术要点：关键 API 为 `DevicePolicyManager.setTimeZone()`（Android 6.0+ 专为 Device Owner 提供），配合 `AlarmManager.setExactAndAllowWhileIdle()` 做定时调度。

## 已完成功能

- [x] **开发环境搭建**（2026-08-19）
  - JDK 21 LTS（`D:\AndroidDev\JDK\jdk-21.0.12.8`），JAVA_HOME / PATH 已配置
  - Android Studio 2026.1.3.7（`D:\AndroidDev\Android Studio`）
  - Android SDK（`D:\AndroidDev\AndroidSdk`）：platform-tools / platform-36 / build-tools-36 / emulator 37.1.11 / system-images;android-36;google_apis;x86_64 / AEHD 驱动 2.2（已运行）
  - 模拟器 AVD：`SleepShift_AVD`（Pixel 外形，数据在 `D:\AndroidDev\AVD\SleepShift_AVD`）
  - 全部组件部署在 D 盘统一目录 `D:\AndroidDev\`，C 盘无残留
  - 验证：JDK 21 ✅ / adb 1.0.41 ✅ / AVD 识别 ✅ / AEHD 硬件加速可用 ✅
- [x] **项目创建**（2026-08-19）
  - 项目路径：`D:\AndroidDev\Projects\SleepShift`
  - Kotlin + Jetpack Compose，AGP 8.13.2 / Kotlin 2.4.10 / Compose BOM 2025.08.00 / Gradle 8.14.3
  - compileSdk 36 / targetSdk 36 / minSdk 26
  - 已声明 `DeviceAdminReceiver`（`com.sleepshift.admin.DeviceAdminReceiver`）+ device_admin.xml
  - `./gradlew assembleDebug` 构建成功，生成 `app-debug.apk`（11MB）
- [x] **获取 Device Owner 授权**（2026-08-19）
  - 模拟器 `SleepShift_AVD`（API 36 google_apis）启动正常，AEHD 硬件加速
  - 安装 debug APK 成功
  - `dpm set-device-owner com.sleepshift/.admin.DeviceAdminReceiver` 一次成功
  - 验证：`dpm list-owners` → `User 0: admin=com.sleepshift/.admin.DeviceAdminReceiver,DeviceOwner,Affiliated` ✅
  - App 可正常启动运行（MainActivity 在前台）
  - 注：debug 构建 APK 在此环境可直接授权，未遇到 `testOnly=true` 拒绝问题

## 当前开发阶段

**任务 #4：实现 22:00/6:00 时区自动切换**

- 核心 API：`DevicePolicyManager.setTimeZone()`（Device Owner 专用）
- 定时调度：`AlarmManager.setExactAndAllowWhileIdle()` 注册每天 22:00 / 6:00 的精确闹钟 + `BroadcastReceiver`
- 实现思路：先保存原始时区 ID → 22:00 设置为 `GMT+10`（相对北京 +8h 偏移 2h）→ 6:00 恢复原始时区
- 设备重启后 `BootReceiver`（`BOOT_COMPLETED`）重新注册定时器

## 遇到的问题

> 详细记录（日期/问题/尝试方案/结果/最终解决方式）见 [DEVELOPMENT_LOG.md](./DEVELOPMENT_LOG.md)。此处仅列当前仍相关的事项：

- **构建相关（已解决）**：Compose BOM 2026.08.00 需 compileSdk 37 + AGP 9.1+，BOM 已锁定 `2025.08.00`（→ Compose 1.9.0）。⚠️ 若以后升级 BOM，需同步升级 AGP/compileSdk。
- **下载相关（已解决）**：Gradle 发行源改腾讯镜像；GitHub 直连不稳定用 winget/加 `--ssl-no-revoke`。
- **环境变量**：当前 bash 会话不继承新设环境变量，命令行需显式 `JAVA_HOME='D:\AndroidDev\JDK\jdk-21.0.12.8'`。
- ~~Device Owner 预期问题~~：原担心 debug APK 带 `testOnly=true` 会被 `dpm` 拒绝——实测 debug APK 直接授权成功，未复现。若日后真机/其他镜像遇到，改用 release 签名构建。

## 已修改的重要文件

| 文件 | 说明 |
|---|---|
| `app/src/main/AndroidManifest.xml` | 声明 `DeviceAdminReceiver` + `RECEIVE_BOOT_COMPLETED` 权限 + 入口 Activity |
| `app/src/main/java/com/sleepshift/admin/DeviceAdminReceiver.kt` | Device Admin/Owner 接收器（当前为空实现，任务 #4 加回调） |
| `app/src/main/java/com/sleepshift/MainActivity.kt` | Compose 主界面（占位 UI） |
| `app/build.gradle.kts` | AGP 8.13.2 / compileSdk 36 / minSdk 26 / Compose / Java 17 |
| `gradle/libs.versions.toml` | 版本目录；**BOM 锁定 `2025.08.00`**（勿随意升级，见注意项） |
| `settings.gradle.kts` | 阿里云 maven 镜像加速（官方源兜底） |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 8.14.3，分发地址指向腾讯镜像 |
| `local.properties` | `sdk.dir=D:\AndroidDev\AndroidSdk`（已 gitignore，不入库） |
| `PROJECT_STATUS.md` / `DEVELOPMENT_LOG.md` | 状态与问题记录 |

## 下一步计划

1. **任务 #4**：实现时区自动切换模块
   - 新增 `DeviceAdminReceiver` 回调（`onEnabled`/`onDisabled` 时注册/清除定时器）
   - 新增 `TimezoneScheduler`：保存原始时区、注册每日 22:00/6:00 精确闹钟
   - 新增 `AlarmReceiver`：到点调用 `DevicePolicyManager.setTimeZone()`
   - 新增 `BootReceiver`：重启后重新注册
   - 编译验证 `assembleDebug`
2. **任务 #5**：模拟器端到端测试
   - 手动触发时区切换（改系统时间或直接发广播）验证 +2h / 恢复
   - 模拟重启验证定时器仍生效
   - 更新 PROJECT_STATUS.md / DEVELOPMENT_LOG.md

## 下次继续开发时需要注意的事项

- **命令行构建**：当前 bash 会话不继承新环境变量，构建必须显式：
  `JAVA_HOME='D:\AndroidDev\JDK\jdk-21.0.12.8' ./gradlew.bat assembleDebug`（在项目根目录）
- **adb / 模拟器**：adb 在 `D:\AndroidDev\AndroidSdk\platform-tools\adb.exe`；模拟器 `SleepShift_AVD` 已授权为 Device Owner，**直接复用即可**。若重置/重建 AVD，需重新执行 `adb shell dpm set-device-owner com.sleepshift/.admin.DeviceAdminReceiver`（前提：设备无账户、无其他 device owner）。
- **不要随意升级 Compose BOM**：`2025.08.00`（Compose 1.9.0）与 AGP 8.13.2/compileSdk 36 匹配；升级到 2026.08.00 需要同时升 AGP 9.1+ 和 compileSdk 37（会连带改 Gradle、装 platform-37）。
- **Bash 陷阱**：带斜杠的 Windows 参数（如 `/S`、`/MOVE`、`/D=`）会被 MSYS 转换，需 `MSYS2_ARG_CONV_EXCL="*"` 或改用 PowerShell；curl 到部分源需 `--ssl-no-revoke`。
- **提权脚本**：需管理员执行的 .ps1 必须**纯 ASCII**（PowerShell 5.1 按 GBK 解析无 BOM 中文脚本会静默失败），且用 `Start-Process -Verb RunAs` 方式调用。
- **时区测试提醒**：测试会真的改模拟器系统时区，测完记得恢复；模拟器时间偏移可能影响其他操作。
