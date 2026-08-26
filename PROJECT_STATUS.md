# SleepShift / 睡了么 项目状态（PROJECT_STATUS.md）

> **本文件只描述「当前状态」，不复制历史。**
> 历史开发记录以 [DEVELOPMENT_LOG.md](./DEVELOPMENT_LOG.md) 为**唯一长期来源**（倒序，含每阶段目标 / 完成功能 / 架构与文件 / 问题与解决 / 技术决策 / 遗留 / 下一步）。涉及历史背景时，本文件只做一句话指路（如「详见 DEVELOPMENT_LOG §五」）。
>
> 维护约定：每完成一个独立开发阶段，**先更新 DEVELOPMENT_LOG.md，再同步本文件的「当前状态」相关小节**；两者不要互相复制大段内容。

---

## 1. 项目定位（当前）

- **产品名**：睡了么（包名 `com.sleepshift`）
- **一句话**：一个假装时间变晚、帮你早点睡的小工具 —— 虚拟时间 + 心理暗示 + 娱乐化睡眠陪伴。
- **核心机制**：真实时间 → 偏移算法 → **虚拟时间展示** → 用户感知变化。纯计算展示，**不触碰系统时间 / 时区 / 状态栏**。
- **技术红线（MVP）**：不依赖 Root / Shizuku / Device Owner / 系统时间修改。
- **历史沿革**：项目由「系统时区偏移工具 SleepShift」转型而来；系统时区路线（P0~P11：Device Owner / Shizuku / TimezoneScheduler）已**冻结保留**为高级实验资产，代码不删（详见 DEVELOPMENT_LOG §五、§七、附录 B）。

---

## 2. 当前版本与仓库状态

| 项 | 值 |
|---|---|
| 应用名 | 睡了么（`app/src/main/res/values/strings.xml`） |
| versionName / versionCode | `0.3.25` / `2`（`app/build.gradle.kts`） |
| git tag | `v0.3.25-alpha` |
| HEAD | `276c222`（2026-08-22 01:39，`release: SleepWell 0.3.25 Alpha baseline (SL-9.6~SL-9.10)`） |
| 本地 vs 远端 | 本地 master **落后 `origin/master` 2 个提交**（`6dc9a19`/`024ce71` 为 README.md 创建与更新，2026-08-23，仅存在于远端，尚未合入本地） |
| 工作区 | 仅文档改动：`DEVELOPMENT_LOG.md`（历史日志重写）、`PROJECT_STATUS.md`（本次更新）；**无业务代码改动** |

---

## 3. 技术栈与架构（当前代码状态）

### 技术栈
- Kotlin + Jetpack Compose；AGP 8.13.2 / Kotlin 2.4.10 / Compose BOM 2025.08.00 / Gradle 8.14.3；compileSdk 36 / targetSdk 36 / minSdk 26
- Preferences DataStore 1.1.1（持久化）、Jetpack Glance 1.1.1（桌面组件）、Shizuku api/provider 13.1.5（冻结轨道）
- 构建镜像：阿里云 Maven + 腾讯 Gradle 发行源（详见 DEVELOPMENT_LOG 附录 A #1/#2）

### 主产品架构（睡了么，活跃）
```
MainActivity（欢迎弹窗 → 5 步引导 → 沉浸主页）
  ├─ shuileme/ui/：ShuilemeHomeScreen / OnboardingScreen / SleepCaseReportScreen / PersonalityCardScreen / NightTheme
  │   └─ components/：NightMoon / SleepGoalEditor / TimeScrollPicker（原生 NumberPicker）
  ├─ shuileme/engine/VirtualClockEngine：真实时间 + offset = 虚拟时间（纯计算，复用 OffsetStrategy 三模式）
  ├─ shuileme/data/ShuilemeRepository（DataStore：睡眠记录 / 月亮 / 人格 / OnboardingState）
  ├─ shuileme/reminder/：提醒调度 + 通知 + 睡眠胶囊
  ├─ shuileme/widget/：Glance 2x1 / 4x2 桌面组件
  ├─ shuileme/model/：月亮生命 / 睡眠人格 / 睡眠侦探 / 人格气泡物理 / 人格卡片等模型
  └─ ui/WelcomeDialog.kt + PersonalityCardActivity.kt
```

### 冻结轨道（系统时区操纵，LEGACY/FROZEN，保留不删）
```
TimezoneScheduler → TimeShiftEngine（EngineManager：Shizuku > Device Owner）
  ├─ engine/（TimeShiftEngine / EngineType / TimeShiftResult / DeviceOwnerTimeShiftEngine / ShizukuTimeShiftEngine）
  ├─ admin/（DeviceAdminReceiver / DeviceOwner）、permission/（Shizuku 能力层）
  ├─ AlarmReceiver / BootReceiver / AppCapabilities / DebugActivity
  └─ 旧 ui/（Home / Config / Mode 三页 + TimeWheelPicker / OffsetSlider / LivePreview）
```

> 完整文件地图见 DEVELOPMENT_LOG 附录 B。注意：`GravitySensor.kt` 为 SL-9.6 移除重力物理后的历史残留文件（当前无引用）。

---

## 4. 已完成能力（0.3.25 Alpha，按当前代码现状确认）

### 主产品「睡了么」
- **虚拟时间引擎**：FIXED / GRADUAL / FLUCTUATION 三模式偏移，纯计算无系统副作用
- **睡眠记录**：「我要睡了🌙 / 我醒啦☀️」打卡，睡眠起止与时长本地持久化
- **月亮陪伴系统**：五阶段成长（🌑→🌕）、情绪、点击波纹、睡前向月亮道晚安、「昨晚月亮观察」报告卡
- **睡眠人格系统**：基础人格类型 + 倾向分析 + 主页人格气泡（失重漂浮 / 拖拽 / 碰撞）+ 碎碎念
- **人格分享卡**：MBTI 式「我的睡眠人格」生成、PNG 导出分享
- **睡眠侦探**：轻量睡眠案件报告（无 UsageStats 权限）
- **睡眠提醒**：三人格文案（温柔 / 毒舌 / 牛马）+ 模板池 + 调度 + 睡眠胶囊
- **双睡眠时间目标**：理想作息 + 当前作息（SleepGoalEditor 共享组件，4 键持久化）
- **首启体验**：欢迎弹窗 + 5 步引导（含原生时间滚轮），OnboardingState 持久化
- **沉浸式主页**：深色夜空主题、齿轮拉绳设置面板、人格气泡失重漂浮、月亮交互
- **桌面组件**：Glance 2x1 / 4x2（虚拟时间 + 月亮 + 睡眠状态 + 快捷入口）+ 周期刷新
- **Alpha 包装**：应用名「睡了么」、versionName 0.3.25 / versionCode 2

### 冻结轨道（遗留能力，非当前产品路径）
- Device Owner 时区通道、Shizuku shell 时区通道（真机验证记录见 DEVELOPMENT_LOG §七）、TimezoneScheduler 调度核心、调试页
- 上述能力与三模式偏移算法 / DataStore 框架等**历史资产保留**，未来仅作为高级实验室选项（详见 DEVELOPMENT_LOG 附录 A、附录 B）

---

## 5. 测试与验证状态

| 项 | 状态 | 说明 |
|---|---|---|
| `assembleDebug` | ✅ | 0.3.25 基线构建通过（记录于 DEVELOPMENT_LOG §一） |
| `testDebugUnitTest` | ✅ 记录为 123 例全过 | 记录于 DEVELOPMENT_LOG §一；原始出处为外层状态文档；当前整理时未重新运行【待确认】 |
| 模拟器验收 | ✅ | SL-9.4~9.10 每轮均有 Pixel 模拟器截图验收（`screenshots/sl9_4/` ~ `sl9_10/`） |
| 真机（Redmi K80） | 【待确认】 | SL-9 各轮 README 列有真机测试清单，但仓库内无执行结果记录 |
| Alpha 内测反馈 | 未开始 | 0.3.25 Alpha 反馈尚未收集 |

---

## 6. 当前已知问题

| 问题 | 状态 | 来源 |
|---|---|---|
| 真机体验与 Alpha 内测反馈缺失 | 【待确认】 | DEVELOPMENT_LOG §一 |
| 4x2 桌面组件仅代码级验证（Pixel Launcher 拖拽限制，无法模拟器放置） | 已知，未真机复验【待确认】 | DEVELOPMENT_LOG §三 |
| 本地 master 未包含 README.md（仅远端） | 仓库事实 | git 状态 |
| `WRITE_SETTINGS` 权限声明与实验注释残留（阶段 7 实验产物，无代码使用） | 已知，保留 | 代码现状 / DEVELOPMENT_LOG §十 |
| `GravitySensor.kt` 无引用残留 | 已知，保留 | 代码现状 / DEVELOPMENT_LOG 附录 B |
| 冻结轨道遗留：HyperOS 状态栏时间刷新不可控、Shizuku 无法可靠修改系统时间 | 已冻结，**不再阻塞主产品** | DEVELOPMENT_LOG §七 |
| 睡眠时间监测准确性、催睡通知完善、整夜监测等 | 开发中/反馈期待方向 | README（远端）所列 |

---

## 7. 正在进行的工作

- **真机 Alpha 测试**：按 `D:\AndroidDev\Shuileme_Alpha_Test_Install.md` 与 `Shuileme_SL9_Real_Device_Test.md` 计划进行；仓库内暂无执行记录【待确认】。
- 截至 2026-08-26 整理时，无其他活跃开发任务记录（以 DEVELOPMENT_LOG 为准）。

---

## 8. 下一步计划

1. 真机安装 **0.3.25 Alpha** → 收集内测反馈。
2. 按反馈迭代（README 与历史记录所列方向）：桌面小组件完善 / 催睡通知优化 / 整夜睡眠监测 / 更多人格类型 / 人格卡片分享 / 气泡碎碎念 / 灵动岛假时钟等。
3. 决定是否把仅存于远端的 README.md 合入本地 master。
4. 每次重要开发完成后：**先更新 DEVELOPMENT_LOG.md（历史），再同步更新本文件（当前状态）**。

---

## 9. 开发环境与常用命令（速查）

- 构建：`JAVA_HOME='D:\AndroidDev\JDK\jdk-21.0.12.8' ./gradlew.bat assembleDebug`（当前 shell 不继承新环境变量，必须显式指定）
- 单测：`JAVA_HOME='D:\AndroidDev\JDK\jdk-21.0.12.8' ./gradlew.bat testDebugUnitTest`
- adb：`D:\AndroidDev\AndroidSdk\platform-tools\adb.exe`；模拟器 `SleepShift_AVD`（DO 已授权）/ `SleepShift_AVD_v2` / `SleepShift_Accept`（Pixel 验收）
- 冻结轨道验证命令（dpm 授权 / appops / 时区测试广播等）与平台约束速查：详见 DEVELOPMENT_LOG 附录 C

---

## 10. 待确认清单

- 真机（Redmi K80）SL-9 各轮与 0.3.25 Alpha 的体验结果
- Alpha 内测反馈内容
- 4x2 桌面组件真机视觉与交互
- 当前 `testDebugUnitTest` 实际运行结果（历史记录为 123 例全过）
- 阶段 7 实验的精确日期等历史细节（见 DEVELOPMENT_LOG §十，不影响当前状态）

