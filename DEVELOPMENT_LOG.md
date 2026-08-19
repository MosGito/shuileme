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
