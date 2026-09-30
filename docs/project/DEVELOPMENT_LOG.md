# SleepShift / 睡了么 项目开发历史日志

> **本文件是本项目唯一的长期开发历史日志。** 后续所有 Codex / Claude Code 等开发对话在完成重要开发后，都必须把开发历史追加到本文件；其他文档（`PROJECT_STATUS.md`、外层 `D:\AndroidDev` 状态文档）只作为当前状态或设计参考，不再承担历史记录职责。
>
> 整理日期：2026-08-26。本次整理将旧版 `DEVELOPMENT_LOG.md`（只记录到 Phase 11-D）重写为覆盖全部可确认历史的倒序日志：系统时区轨道（P0~P11）+「睡了么」轨道（SL-0~SL-9.10）。旧日志 #1~#17 的技术细节完整保留在「附录 A」。

---

## 维护约定（每个后续会话必须遵守）

1. **倒序追加**：新阶段条目插入在「开发时间线总览」之后、现有最新条目之前（新的在上）。
2. **每条必需字段**：日期/阶段；当时开发目标；完成了哪些功能；修改了哪些重要架构或文件；遇到过哪些问题及如何解决；重要技术决策及原因；当前遗留问题；下一步计划。
3. **禁止凭空推测**：所有事实必须来自 git 提交、仓库内文档、或外层 `D:\AndroidDev` 已有记录；无法确认的信息一律标注 `【待确认】`，并注明来源。
4. **来源标注**：git 提交写短 hash；文档写文件名；真机/外部验证结果若仓库内无日志，注明「来源：外层文档」。
5. 只记录**开发历史**（发生了什么、为什么、怎么解决），不是代码文档；具体代码细节以仓库为准。

---

## 当前状态速览（整理于 2026-08-26）

| 项 | 值 |
|---|---|
| 应用 | 「睡了么」（包名 `com.sleepshift`） |
| 版本 | versionName `0.3.25` / versionCode `2`（git tag `v0.3.25-alpha`） |
| HEAD | `276c222`（本地 master）；**落后 `origin/master` 2 个提交**（`6dc9a19`/`024ce71` 为 README.md 创建与更新，2026-08-23，仅存在于远端） |
| 主产品 | 「睡了么」虚拟时间睡眠陪伴（SL 轨道，纯计算、不碰系统时间/时区） |
| 冻结轨道 | 系统时区操纵（P0~P11：Device Owner / Shizuku / TimezoneScheduler），代码保留不删 |
| 测试 | 0.3.25 基线 `testDebugUnitTest` 123 例全过（来源：`D:\AndroidDev\SleepShift_Product_State.md` 2026-08-22 更新）；`assembleDebug` 通过 |
| 当前阶段 | SL-9.10 完成、0.3.25 Alpha 基线已发布；下一步为真机 Alpha 测试与反馈迭代（真机结果仓库内暂无记录【待确认】） |
| 文档状态 | 仓库内 `PROJECT_STATUS.md` 内容停留在 Phase 11-D，未涵盖 SL 阶段（历史记录以本文件为准） |

---

## 开发时间线总览

| 日期 | 阶段 | 核心内容 | 提交 |
|---|---|---|---|
| 2026-08-22 | 0.3.25 Alpha 基线（SL-9.6~9.10） | 内测版发布：七轮真机体验修正合并 + 欢迎弹窗 + 版本包装 | `276c222`（tag `v0.3.25-alpha`） |
| 2026-08-21 | SL-9.5 | 真实体验修复（时间选择器滚动 / 设置顶部面板 / 人格标题） | `4374925` |
| 2026-08-21 | SL-9.4 | 视觉稳定化（深色夜空主题 / Glance .dp 修复 / 模拟器验收） | `d9a244b` |
| 2026-08-21 | SL-9.1~9.3 + Phase 11-C/D 收口 | 视觉细化、睡眠胶囊、沉浸视觉系统；P10/P11 未提交工作区并入 | `01ab3c2` |
| 2026-08-21 | 产品转型（SL-0/SL-1）+ SL-2~SL-8 | 「睡了么」产品迁移；虚拟时间引擎、桌面组件、提醒、月亮、人格、侦探、沉浸主页（大合并提交） | `e833b54` |
| 2026-08-21 | Phase 11-A/B | TimeShiftEngine 抽象层 + Shizuku 能力层 | `2cb975d` / `6b1dde8` |
| 2026-08-20 | 阶段 5 / 阶段 6 | 模拟器端到端验证 + 产品化 UI | `ff45add` / `8167d7b` |
| 2026-08-19 | 环境搭建 + v2 重设计 + 阶段 1~4 | 可配置时区偏移系统（数据模型 / UI / 持久化 / 调度 / Receiver / 整小时约束） | `4e186b7` ~ `edd6b0b` |
| 2026-08-20~21【日期待确认】 | 阶段 7 实验 | WRITE_SETTINGS 时区通道实验（已废弃移除，Manifest 残留注释与权限声明） | 未提交（checkpoint #35 清理） |
| 2026-08-19~21 | 检修轨道 Phase 1-10 | 模拟器环境排障（与产品开发无关，仅外层记录） | 无仓库提交 |
| 2026-08-27 | PHASE 0 架构规范化 + PHASE 1~3 人格实现 | 文档树分层；V2.0.7 特征/定义/Membership 层 | 未提交 |

---

## 零、2026-08-27｜PHASE 0 架构规范化 + 人格 PHASE 1~3（跨系统）

- **目标**：把全项目架构审计正式化为项目级架构规范与文档边界；完成 V2.0.7 人格系统特征/定义/Membership 三层实现。
- **完成**：
  - 文档树分层：`docs/architecture|personality|sleep|bubble|moon|clock|social|reminder|legacy|project`；
  - V2.0.7 规范迁移至 `docs/personality/SLEEP_PERSONALITY_SYSTEM_V2.md`（字节一致，SHA-256 不变，FROZEN）；
  - `SYSTEM_MAP.md`（系统边界/依赖矩阵/K-1~K-10 债务/迁移队列）与 `DECISIONS.md`（ADR-001~011 + 回执格式标准）；
  - 各系统 `SPEC.md` + `CHANGES.md`；`legacy/FROZEN.md`；
  - 项目日志/状态迁入 `docs/project/`（历史保留）；
  - PHASE 1（特征层）、PHASE 2（定义层）、PHASE 3（Membership 层）实现 + 单测（全量 210 通过）。
- **架构决策**：ADR-001~011（Personality 独立化、V2.0.7 唯一规范、Gate 不参与 argmax、Result 唯一契约、UI 禁调算法、Social 禁自算、Domain 禁依赖 UI、Legacy 冻结、Repository 按域收敛、SPEC/CHANGES 分离、V3/V4 内部替换）。
- **遗留债务登记**：K-1~K-10（详见 `docs/architecture/SYSTEM_MAP.md` §8），本阶段不修复。
- **下一步**：PHASE 4（PersonalityMatcher + PersonalityResult + Confidence）。
- **来源**：本条目为项目级记录；系统级细节见各 `docs/*/CHANGES.md`。

## 一、2026-08-22 01:39｜0.3.25 Alpha 基线发布（SL-9.6 ~ SL-9.10）

- **提交**：`276c222` `release: SleepWell 0.3.25 Alpha baseline (SL-9.6~SL-9.10)`，tag `v0.3.25-alpha`（75 文件，+1364/-607）
- **目标**：内测发布前的最后一轮「真机体验」修正（SL-9.6~SL-9.10 共七轮，含 final moon text fix），并完成 Alpha 包装（应用名、版本号、欢迎弹窗）。

### 完成功能（每轮均以 Pixel 模拟器截图验收，报告在 `screenshots/sl9_6/` ~ `screenshots/sl9_10/`）

| 轮次 | 内容 |
|---|---|
| SL-9.6 | M1 时间滚轮改**原生 NumberPicker**（AndroidView 嵌入，中央值=最终值、自动吸附、0-23/0-59 循环）；M2 全局字体颜色（新建 `NightButton` 深底浅字，移除 Material 默认 onPrimary 深色文字）；M3 `NightMoon` 组件复用（引导 step5 与主页同一组件）；M4 气泡改**失重漂浮**（移除重力/传感器，elapsed 驱动正弦漂移 + 四壁反弹 + 碰撞推开，`FloatDriftTest` 4 项）；M5 齿轮拉绳小夜灯（Canvas 二次贝塞尔绳 + 手指跟随 + spring 回弹）；M6 删除人格卡入口；M7 月亮观察返回按钮下沉安全区 |
| SL-9.7 | M1 全局字体系统（主题 `on*` 全浅色 #F8F8FF，字号 TITLE36/BODY22/CAPTION18，step3 picker 高度 124dp 防溢出）；M2 NightMoon 完全复用（像素级一致）；M3 Emoji 拖拽重写（窗框级 pointerInput + `onGloballyPositioned` 记录实际位置命中，delta=currentPointer-pointerDown）；M4 齿轮拖拽重写（父 Box 稳定坐标系 + spring 回弹）；M5 月亮交互增强（波纹 5dp/alpha0.7、提示上移至月亮上方 83dp、蓄力 1.1s）；M6 关闭月亮观察入口（代码保留）；M7 调试控制台收起按钮（左上小点） |
| SL-9.8 | M1 **黑字根因修复**：无显式 color 的 Text 在部分组件上下文渲染纯黑 → 主题级 `CompositionLocalProvider(LocalContentColor=#F8F8FF)` 统一文字颜色入口（step2 1793→5、step4 1264→0 黑像素）；M2 时间滚轮字号 20→26sp、容器 140dp；M3 Emoji+齿轮拖拽回归修复（根因：per-bubble `amount` 被元素自身位移抵消、`positionInRoot` 不反映 `.offset()` → 帧级 pointerInput + **physics 位置命中**（dp→px 正确映射）+ delta px→dp 换算，命中半径 80px）；M4 抽离 `MoonStageLayout` 统一月亮坐标（step5 与主页误差 ~11dp） |
| SL-9.9 | 只修交互命中层、不改物理：命中中心改为 emoji **渲染中心**（+30px 校正）、半径 80→90px（≈3x emoji 半径）；齿轮 hitbox 44→80px；验证：偏左 70px/偏上 40px 按下可拖，+200px → 物理移动 +76dp |
| SL-9.10 | M1 **双睡眠时间目标**（目标理想作息 + 当前现实作息；`SleepGoalEditor` 共享组件；`currentSleepTime/currentWakeTime` 新增持久化 4 键，兼容旧数据 target* 保留；理想时长自动计算展示 8 小时）；M2 时间选择 26→38sp、容器 180dp，step3 加 verticalScroll 适配 4 滚轮；M3 月亮文字引导上移（offset 130→100dp）；M4 **恢复昨晚月亮观察**（醒后主页自动显示卡片 → 点击进报告详情；此前入口被误删已恢复）；M5 设置→「睡眠目标」二级菜单（与初始页共享 SleepGoalEditor）；M6 睡眠人格模型扩展（保存当前/目标入睡+起床 4 键）；M7 牛马随机单 emoji（🐮/🐴，视觉长度统一） |
| final | step5 月亮文字上移（`align(TopCenter).offset(y=80dp)`）、移除 NightMoon 残留 `offset(y=21.dp)` → 引导与主页月亮像素级一致（模拟器坐标均为 541,961），文字不遮挡月亮（y=329/449 vs 月亮 y=961） |

### 内测包装（Alpha）
- `app_name` → **睡了么**（此前 e833b54 时代仍为 SleepShift）；versionName `0.3.25` / versionCode `2`
- 新增 `ui/WelcomeDialog.kt`：首次启动前置介绍层（欢迎弹窗 → onboarding）
- 新增 `components/NightMoon.kt`、`components/SleepGoalEditor.kt`；删除 `GravityInteractionTest`/`GravitySensorTest`（重力物理被 SL-9.6 移除）；`MainExperience` 物理重构（-76 行）

### 验证
- `assembleDebug` ✅ / `testDebugUnitTest` ✅（0.3.25 基线 123 例，来源：`SleepShift_Product_State.md`）
- 模拟器三项测试通过：首次启动欢迎弹窗→onboarding、月亮动画→主页、重启不重现欢迎弹窗（来源：`SleepShift_Product_State.md`）

### 遗留 / 待确认
- **真机（Redmi K80）体验**：各轮 README 均列出「真机重点测试列表」，但仓库内没有真机执行结果记录【待确认】
- 0.3.25 Alpha 内测反馈尚未收集；README（远端）列出开发中项：桌面小组件完善、催睡通知优化、整夜睡眠监测、更多人格类型、灵动岛假时钟等
- 本地 master 未包含 README.md（仅存在于 `origin/master`）【git 事实】

### 下一步（按当时记录）
真机 Alpha 安装测试（`D:\AndroidDev\Shuileme_Alpha_Test_Install.md`、`Shuileme_SL9_Real_Device_Test.md`）→ 收集反馈 → 迭代。

---

## 二、2026-08-21 20:37｜SL-9.5 真实体验修复

- **提交**：`4374925`（3 个 UI 文件 + `screenshots/sl9_5/`）
- **目标**：按 9 项重点逐项做真实体验验收（页面边框 / 深色主题 / 初始体验 / 时间选择 / 月亮交互 / 设置机关 / 人格气泡 / 气泡物理 / 页面可达性），修复发现的问题。

### 修复清单
1. **时间选择器滚动异常**（严重，引导 step3）：小时列完全卡住、分钟列滚动方向反转、默认值提交 1770（=29:30）
   - 根因 A（无法滚动）：引导内容 `Column.verticalScroll` 与外嵌 LazyColumn 嵌套滚动冲突 → 移除父级 scroll
   - 根因 B（默认值错）：reporting 在初始化时读取中心项与选中项不符触发级联（0→1→…→59）污染默认值 → reporting 仅用户滚动结束后触发（`.drop(1)`），初始化保持选中值
   - 根因 C（末项无法居中）：LazyColumn 无端部 padding，末项(23时)被 clamp 到底部 → 加 `contentPadding` 上下半槽
   - 验证：初始 23:00/07:00 正确提交（1380/420）、0→5 分滚动正常
2. **「海獭型型」重复「型」**（引导 step5 三处）：`"${displayName}型"` 对已含「型」的 displayName 重复追加 → `displayName.removeSuffix("型") + "型"`（与 SL-9.4 人格卡同法）
3. **设置抽屉方向**：原 `ModalNavigationDrawer` 从左侧展开，与「齿轮下拉」设计不符 → 重构为 `AnimatedVisibility` + `slideInVertically` 顶部面板 + 半透明遮罩 + ✕ 关闭；移除 ModalDrawerSheet/rememberDrawerState 等废弃引用

### 验证
9 项逐项 ✅（Pixel 模拟器实况截图存 `screenshots/sl9_5/`）；`assembleDebug` + `testDebugUnitTest` ✅；产物 `app-debug.apk` 可安装真机测试。

---

## 三、2026-08-21 19:24｜SL-9.4 视觉稳定化

- **提交**：`d9a244b`（18 文件，含 `screenshots/sl9_4/` + `scripts/`）
- **目标**：消除白底 / 黑字 / 启动白闪，完成全局深色夜空视觉系统，并建立 Pixel 模拟器验收流程。

### 完成
- **全局深色夜空主题**：`colors.xml` 新增 `night_sky #FF14142B`；`themes.xml` 深色 Material；MainActivity / PersonalityCardActivity `enableEdgeToEdge` + `windowLightStatusBar=false` → 状态栏/导航栏统一深色，无白底黑字
- **Glance 组件构建修复**（未提交改动原本无法编译）：
  - MediumWidget `Unresolved reference 'dp'`：`androidx.glance.unit.dp` 在 Glance 1.1.1 不存在 → `import androidx.compose.ui.unit.dp`
  - SmallWidget `Unresolved reference 'R'` + `R.dimen.*` 返回 Int 被 Glance 当资源 ID（原始 bug 未根治）→ 全部改用 `.dp`，删除无引用 `dimens.xml`
- **人格卡标题「海獭型型」**：`"${type.displayName}型"` 对已含「型」的 displayName（海獭/牛马/混沌）重复 → `displayName.removeSuffix("型") + "型"`
- **验收流程**：新建 `SleepShift_Accept`（Pixel, API 36, headless + swiftshader）AVD；6 类截图验收报告存 `screenshots/sl9_4/README.md`；辅助脚本 `scripts/shot_analyze.py`（主色/系统栏白底检测）、`scripts/widget_place.py`（组件放置）

### 环境限制
- 4x2 中组件无法在 Pixel Launcher 经 adb `input motionevent` 拖拽放置（长按提起无法驱动）→ 与 2x1 同 `.dp` 修复模式且编译通过，属**代码级验证、模拟器未视觉验证**；2x1 可经 Add 按钮放置

---

## 四、2026-08-21 18:07｜checkpoint：Phase 11-C/D 收口 + SL-9.1~9.3

- **提交**：`01ab3c2`（43 文件，+2513/-323）—— 把此前未提交的 P10/P11 工作区（旧记录「9 文件未提交」）与 SL-9.1~9.3 视觉/组件修正一并收口
- **背景**：`e833b54`（SL-8）之后到 `d9a244b`（SL-9.4）之前的连续未提交改动，在此 checkpoint 统一入库；同时更新 `DEVELOPMENT_LOG.md`（+35）与 `PROJECT_STATUS.md`（+55）至 Phase 11-D 状态（即旧版文件内容）

### 内容
- **Phase 11-C/D 收口**（详见 §七）：
  - `ShizukuTimeShiftEngine` 完整实现（+206 行：shell 时区命令 + SystemUI 刷新广播 + `refreshSystemUiClock` 调试入口）
  - `DebugActivity`/`DebugScreen` 完整实现（能力自检 SELFTEST + 时区测试 + 控制台展开/收起）
  - `AppCapabilities`/`OnboardingScreen` 增 Shizuku 授权引导卡；Manifest 补充（+9）
- **SL-9.1~9.3（设计文档见外层 `Shuileme_SL9_*.md`）**：视觉细化（SL-9.1）、组件修复 + **睡眠胶囊**（`SleepCapsule`「我醒啦」动作，+116 行，SL-9.2）、沉浸视觉系统（`NightTheme` +74，SL-9.3）与 UX 修正
- **SL-2 引擎与测试补强**：`VirtualClockEngine`（+32）、`MainExperience`（+189）、新增 `GravitySensor` 与重力物理（后被 SL-9.6 移除）、新增测试文件（BubbleBoundary / MoonRipple / PersonaBubble / SleepTimePicker / TypographyAccessibility / SleepCapsule / Widget 等）

---

## 五、2026-08-21｜产品转型「睡了么」+ SL-0~SL-8（合并提交 `e833b54`）

- **提交**：`e833b54` `feat: complete SL-8 immersive sleep experience baseline`（47 文件，+4649/-7）
- **目标**：把「睡了么」从设计文档落地为可安装 Alpha 应用（SL-2~SL-8 全部功能），并完成与冻结轨道（SleepShift 系统时区）的状态体系拆分
- **当时版本**：versionName `1.0` / versionCode `1`，应用标签仍为 **SleepShift**（「睡了么」标签到 0.3.25 才改；来源：`Shuileme_Alpha_Test_Install.md`）

### 产品转型决策（SL-0/SL-1，2026-08-21 白天）
- **方向调整**：系统时间操纵路线**冻结**，转型「睡了么」——虚拟时间 + 心理暗示 + 娱乐化睡眠陪伴（"一个假装时间变晚，帮你早点睡的小工具"；"我不改变世界时间，我改变你看待睡眠的方式"）
- **冻结原因**（来源：`SleepShift_Legacy_State.md`）：
  1. Shizuku 无法可靠修改系统时间（设置系统时钟需 `SET_TIME` signature 权限）
  2. HyperOS/SystemUI 状态栏时间刷新不可控（时区修改成功但显示不刷新 / 双时钟相同）
  3. 普通用户门槛高（Shizuku 需 adb 激活、Device Owner 需 adb dpm）
- **新路线**：真实时间 → 偏移算法 → **虚拟时间展示** → 用户感知变化；纯计算、无系统副作用；**不依赖 Root / Shizuku / Device Owner / 系统时间修改**
- **可复用资产**：`OffsetStrategy` 三模式（FIXED/GRADUAL/FLUCTUATION）纯计算、DataStore/SettingsRepository 持久化框架、TimeShiftEngine 架构模式、纯函数单测方法
- **开发原则**（`Shuileme_Development_Rules.md`）：emoji / Unicode / 简单动画 / Compose 绘制优先（不依赖美术资源）；"有点搞笑、有点陪伴、有点自我欺骗"；核心交互极简（「我要睡了！」/「我醒啦！」）；技术红线不碰系统时间
- **状态体系**：外层 `D:\AndroidDev` 建立 `Shuileme_Product_State.md`（SL 编号，唯一状态入口）与 `SleepShift_Legacy_State.md`（P 编号冻结轨道）；仓库内 `PROJECT_STATUS.md` 保留应用状态

### SL 功能阶段（均随 `e833b54` 一次提交入库；各阶段设计文档在外层 `D:\AndroidDev\Shuileme_SL*_*.md`）

| 阶段 | 内容 | 单测数（来源：`Shuileme_Product_State.md`） |
|---|---|---|
| SL-2 | **VirtualClockEngine**：`virtualNow = realNow + offsetMin*60000`（偏移永远为正、让时间更晚），复用 FIXED/GRADUAL/FLUCTUATION；睡眠模型 + `ShuilemeRepository`（DataStore）；首页 MVP（我要睡了🌙 / 我醒啦☀️ + 虚拟大钟 + 月亮 emoji）+ 月亮成长 `MoonProgress` | 29 |
| SL-3 | **Jetpack Glance 桌面组件**：2x1（`ShuilemeSmallWidget`）/ 4x2（`ShuilemeMediumWidget`），虚拟时间 + 月亮 + 睡眠状态 + 快捷入口；`ShuilemeWidgetRefreshReceiver`（WIDGET_REFRESH/BOOT）；人格/历史数据接口预留 | 设计完成（文档），编码随 SL-8 合并 |
| SL-4 | **睡眠提醒**：「会催你睡觉的睡眠伙伴」——🌙温柔 / 😈毒舌 / 🐮🐴牛马三人格 + 睡前/熬夜/起床反馈三类型 + 45 文案模板池 + AlarmManager 调度 + 「今晚放过我🌙」+ ReminderProfile；`ShuilemeReminderScheduler/Receiver/Notifier`、`ReminderTemplate` | 40 |
| SL-4.5 | **5 步首启引导**（欢迎 / 人格选择 / 虚拟时间滑块 / 首次我要睡了+动画 / 人格预告）+ `OnboardingState` DataStore 持久化 + MainActivity 首次进入 | 75 |
| SL-5 | **月亮生命系统**：🌑🌒🌓🌔🌕 五阶段（新月宝宝→满月伙伴）+ 五情绪 + 预置事件池（每日≤1）+ 满月奖励计数 + 首页成长动画（`MoonLifeModels`/`MoonProgress`） | 50 |
| SL-6 | **睡眠人格系统**：SleepPersonalityEngine（5 基础 + 3 隐藏）+ **Emoji Persona Bubble 人格气泡**（非宠物/无名字/无独立人格）+ 动态迁移 + 碎碎念 + `EmojiPhysicsState`/`EmojiResident`/`MainExperience` | 64 |
| SL-6.5 | **人格分享卡片**：MBTI 式「我的睡眠人格」（≥5 记录解锁）+ `PersonalityCardModel`/Generator + GraphicsLayer 捕获 + PNG 导出（cacheDir/FileProvider/ACTION_SEND）+ `PersonalityCardActivity` | 70 |
| SL-7 | **睡眠侦探**：轻侦探模式（无 UsageStats）：`SleepDetectiveData`（睡眠起止/自报时长/充电估算/夜间线索）+ 三档案件报告 + 月亮观察 + 首页卡片 + `SleepCaseReportScreen` | 81 |
| SL-8 | **主界面体验重构**：「属于自己的睡眠空间」——人格头部（顶）/ 月亮宠物（中央+光效）/ 虚拟时间（月亮下）/ 装饰人格气泡（物理下落+弹跳）/ 设置抽屉（齿轮收纳）；长按月亮→确认手势；首页仅 5 项 | 88 |

### 验证与遗留
- `assembleDebug` ✅；Alpha 安装说明就绪（`Shuileme_Alpha_Test_Install.md`，含 APK SHA-256 `8975fb7d...`、Redmi K80 安装/回退步骤）
- **真机体验结果**：仓库内无 Redmi K80 安装与体验记录【待确认】（SL-9 各轮 README 有真机测试清单，未记录执行结果）

---

## 六、2026-08-21 01:41~01:50｜Phase 11-A/B：引擎抽象 + Shizuku 能力层

- **提交**：`2cb975d`（8 文件 / +153 行）→ `6b1dde8`（10 文件 / +275 行）
- **目标**：旧 Device Owner 通道在非 DO 环境（普通 ROM / HyperOS）不可用 → 引入 Shizuku/Root 多执行模式；先抽象引擎层，保证现有 DO 能力不退化

### Phase 11-A（`2cb975d`）
- 新增 `engine` 包：`TimeShiftEngine`（type/isAvailable/setTimeZone/setAutoTimeZoneEnabled）、`TimeShiftResult`（引擎无关结果）、`EngineType`（DEVICE_OWNER/SHIZUKU/ROOT/NONE）、`EngineManager`（优先级：Shizuku > Device Owner）、`DeviceOwnerTimeShiftEngine`
- `TimezoneScheduler` 构造注入 `TimeShiftEngine`（不再直接依赖 DeviceOwner），applyShift/applyRestore 统一走引擎；`SleepShiftApplication` 接线 EngineManager；`DeviceOwner` 补 `setAutoTimeZone`

### Phase 11-B（`6b1dde8`）
- 新增 `permission` 包：`ShizukuManager`（安装/运行/binder 状态检测，只读）、`ShizukuPermission`（isGranted/requestPermission）、`CapabilityState`（activeEngine 推导）、`CapabilityResolver`（综合能力聚合）
- 依赖 `dev.rikka.shizuku:api:13.1.5` + `provider:13.1.5`；Manifest 注册 `ShizukuProvider`（authorities=`${applicationId}.shizuku`）
- `ShizukuTimeShiftEngine` 骨架（isAvailable = 运行 + 已授权；setTimeZone 未实现）；EngineManager 注册 Shizuku（优先）+ Device Owner
- `DebugActivity` 能力调试页（独立 Activity，主流程加「开发测试」临时入口）

### 验证
`assembleDebug` ✅；SleepShift_AVD_v2 启动验证（activeEngine=NONE、无崩溃、主流程无回归、ShizukuProvider 优雅初始化）✅

---

## 七、2026-08-21｜Phase 11-C/D：Shizuku shell 时区修改 + 真机验证 + SystemUI 刷新

- **实现收口**：`01ab3c2`（此前为未提交工作区；旧记录称「9 文件未提交」）
- **目标**：让 Shizuku 通道真正可改时区；解决 HyperOS 状态栏时间不刷新问题；为普通用户提供 Shizuku 授权引导

### Phase 11-C（Shizuku shell 时区修改）
- `ShizukuTimeShiftEngine.setTimeZone` 通过 `IShizukuService.newProcess` 以 `sh -c` 执行：`settings put global auto_time_zone 0` + `service call alarm 3 s16 "<zone>"`；捕获 exit/stdout/stderr
- `setAutoTimeZoneEnabled` 同理；DebugActivity 启动自检 + DebugScreen 时区测试入口；主界面「开发测试」临时入口
- **真机验证（Redmi K80 + HyperOS，来源：外层文档 `Shuileme_Product_State.md`/`SleepShift_Legacy_State.md`）**：Shizuku 安装/运行/授权 ✅、engine 执行时区命令 ✅、auto_time_zone 关闭 ✅、系统设置时区切换 ✅；**仓库内无真机日志**
- **遗留**：状态栏/SystemUI 时间不刷新（HyperOS timezone refresh/SystemUI cache）→ Phase 11-D
- **环境发现**：模拟器（SleepShift_AVD_v2）无 Shizuku，DebugActivity 自检正确失败（SELFTEST success=false）

### Phase 11-D（SystemUI 刷新修复 + Shizuku 授权引导）
- **D-A/B**：`setTimeZone` 成功后追加三类时钟刷新广播（shell best-effort）：`TIMEZONE_CHANGED`（--es time-zone）、`TIME_SET`、`TIME_TICK`；新增公开 `refreshSystemUiClock(zoneId)` 独立调试入口（DebugScreen「仅刷新 SystemUI」按钮）；时区修改成功仍判成功（刷新失败不影响主操作判定）
- **D-C**：`AppCapabilities` 识别 Shizuku 通道（`timezoneControlReady` = Shizuku 授权或 Device Owner）；Onboarding Step 2 `ShizukuGuideCard` 三态引导（未安装→shizuku.rikka.app / 未运行→打开 Shizuku 应用 / 未授权→授权按钮）+ Device Owner 备选说明；`Shizuku.addRequestPermissionResultListener` 授权后自动重查状态
- **验证**：`assembleDebug` ✅（39 tasks）；`testDebugUnitTest` 12/12 无回归 ✅
- **待真机确认**：三条广播对 HyperOS 状态栏的实际生效情况【待确认】；兜底方案 `killall com.android.systemui` 重启 SystemUI

### 冻结结论（后续）
因「无法可靠修改系统时间 + 状态栏刷新不可控 + 用户门槛高」，系统时区路线整体冻结为高级实验（P17），Shizuku / Device Owner 引擎与 Debug 工具保留不删。

---

## 八、2026-08-20｜阶段 5 端到端验证 + 阶段 6 产品化 UI

- **提交**：`ff45add`（阶段 5）→ `8167d7b`（阶段 6）

### 阶段 5（`ff45add`）
- **目标**：模拟器端到端验证 FIXED/GRADUAL/FLUCTUATION 三模式、重启、边界场景
- **验证通过**：FIXED 三档（+60/+120/+180 → 时区 +0100/+0200/+0300）；GRADUAL 逐天推进（`TEST_FORCE_ADVANCE`：day1 +60 / day2 +120 / day3 +180）；FLUCTUATION 20 次推进分布（120×14 / 180×5 / 60×1，整小时、范围内、无负值）+ 1000 样本属性单测；正常重启重新武装；单测扩至 12/12
- **修复 4 问题**：
  1. 禁用未联动（setEnabled(false) 原来只改 DataStore）→ VM 停用→`cancel()`+`applyRestore()`，启用→`arm()`
  2. 改配置未重新武装（旧闹钟按旧时刻旧配置触发）→ 所有 setter 写库后 `scheduler.arm()`（`armedEpochDay` 守卫保证同夜不重复推进，FIXED 同夜按新设置重算）
  3. FLUCTUATION 取整偏差（`OFFSET_STEP_HALF=7` 是 15 步进旧值）→ 改为 `OFFSET_STEP_MIN / 2`
  4. 偏移状态重启决策不可测 → 抽纯函数 `computeActiveRestoreEpoch` + 单测
- **环境发现（非应用 bug）**：模拟器（google_apis）每次启动将时区强制重置为 GMT（禁用 GMS、auto_time=0 均无效）→ 「偏移状态重启」无法模拟器真实复现；真机 `persist.sys.timezone` 保留；逻辑纯函数化单测兜底
- **调试教训**：FLUCTUATION「恒 120」一度误判为随机性问题，实为 UI 点击未真正选中模式（"+60" 是渐进步进而非波动范围）→ 测试脚本要显式断言「模式已选中」再操作

### 阶段 6（`8167d7b`）
- **目标**：产品化 UI 与用户体验（未重构 Scheduler/Receiver/DataStore）
- 3 步首次引导 `OnboardingScreen`（理念介绍 → 能力检查 → 就绪 + 通知授权；`onboardingDone` DataStore 追加键，应用级状态）
- 今日主页：状态「正常时间/睡眠模式」、今晚计划卡片、关闭确认弹窗（"关闭后手机时间将恢复正常"）
- 配置页：实时解释文本 + 睡眠模式中修改 Snackbar（"修改将在下一周期生效"）；模式页：普通语言说明 + 适合人群
- `NotificationHelper`：IMPORTANCE_LOW 非侵入式，applyShift/applyRestore 成功后发送（仅成功时）
- 小坑：引导 Step 3 通知授权按钮的初始状态需查真实权限（`ContextCompat.checkSelfPermission`），否则已授权也显示按钮

---

## 九、2026-08-19｜环境搭建 + v2 重设计 + 阶段 1~4

- **提交**：`4e186b7`（初始化）→ `a77a2c6`（阶段1）→ `5f6b808`（阶段2）→ `6e7e637`（阶段3）→ `74e7906`（4-A）→ `4a77f5b`（4-B）→ `edd6b0b`（4-C）
- **背景**：原方案固定 22:00/06:00 + GMT+10；v2 升级为**用户可配置的智能睡眠干预系统**（开始/恢复时间连续选择、偏移滑动条 0-180min/15 步进、三偏移模式、实时效果预览、Debug 测试入口）

### 环境搭建（问题细节见附录 A #1~#6）
- JDK 21 LTS（`D:\AndroidDev\JDK\jdk-21.0.12.8`）、Android Studio 2026.1.3.7、Android SDK 36、模拟器 `SleepShift_AVD`（AEHD 硬件加速），全部部署在 D 盘
- 项目：Kotlin + Jetpack Compose，AGP 8.13.2 / Kotlin 2.4.10 / Compose BOM 2025.08.00 / Gradle 8.14.3，compileSdk 36 / targetSdk 36 / minSdk 26
- Device Owner 授权：`dpm set-device-owner com.sleepshift/.admin.DeviceAdminReceiver` 一次成功，`list-owners` 验证 ✅

### 阶段 1（`a77a2c6`）数据模型
- `SleepShiftSettings`（用户配置）+ `SchedulerState`（内部状态）+ `NightWindow`（编码 v2 恢复逻辑：`realWindow = nightLength − offset`）+ `OffsetStrategy` 三实现 + `SettingsRepository`（Preferences DataStore，分层存储 + `originalTimezoneId` 写保护）+ 纯函数 `buildShiftTimeZoneId`

### 阶段 2（`5f6b808`）Compose UI
- 底部导航三页（今日/配置/模式，状态式导航，未引入 navigation-compose）
- 自研 `TimeWheelPicker` 双列轮盘（0-23 + 0/15/30/45，滚动居中吸附）+ `OffsetSlider` + `LivePreview`（每秒刷新）+ 配置页「今晚效果」卡片 + 模式页三模式卡片与参数
- UI 由 `SettingsViewModel`（内存 `mutableStateOf`）驱动，阶段 3 接 DataStore

### 阶段 3（`6e7e637`）持久化
- UI → VM → Repository → DataStore 全链路；`SleepShiftApplication` 单例 + `SettingsViewModelFactory` 注入；VM 改 StateFlow（`stateIn(viewModelScope, WhileSubscribed(5s))` + `collectAsState`）
- `settingsVersion` + `migrate()` 逐版本迁移框架；setter「值未变跳过写入」守卫（避免轮盘/滑条拖动冗余写盘）；`originalTimezoneId` 写保护下沉 Repository
- 验证发现：**Device Owner 应用 `am force-stop` 杀不掉** → 持久化验证改用 `adb reboot` ✅（改 enabled/偏移 → 重启 → 配置从磁盘恢复）

### 阶段 4-A（`74e7906`）TimezoneScheduler 核心
- 新增 `time/TimezoneScheduler.kt`：**零硬编码**纯计算 `planNight` / `computeNextShiftEpoch` / `computeRestoreEpoch` / `buildShiftZoneId`；策略每晚推进；`setExactAndAllowWhileIdle` + `RTC_WAKEUP` 武装；`armedEpochDay` 防同夜重复推进；Manifest 加 `SCHEDULE_EXACT_ALARM`；单测 5/5
- 关键修复：`strategyFor` 必须放 companion（伴生对象不能调实例方法，否则级联类型推断错误）；`buildShiftTimeZoneId` 小时位补零（`GMT+8:00`→`GMT+08:00`）；PendingIntent 用 `setClassName` 字符串定位（4-B 注册 Receiver 后无需改 Scheduler）

### 阶段 4-B（`4a77f5b`）Receiver 适配（含重大平台发现）
- `AlarmReceiver`（SHIFT/RESTORE + TEST_ARM/CANCEL/SET_ZONE；**不信任 PendingIntent extra**，读 DataStore 当前状态计算时区；goAsync + 协程 + 15s 超时；执行后重新 arm）
- `BootReceiver`（BOOT_COMPLETED / MY_PACKAGE_REPLACED 重新武装；`ensureActiveWindowRestore` 处理偏移状态重启）
- `DeviceAdminReceiver`（onEnabled→arm / onDisabled→cancel）；`DeviceOwner`（DPM.setTimeZone 封装 + 自动关自动时区）
- ⚠️ 平台发现 1：`setTimeZone` 只应用 **IANA 时区 ID**（`GMT±HH:MM` 含 `GMT+02:00`/`GMT+10`/`+08:00` 全部静默忽略）→ `buildShiftZoneId` 改 IANA 感知：整小时总偏移 → `Etc/GMT±H`（POSIX 符号反转，UTC+H → Etc/GMT-H）；分数偏移回退 `GMT±HH:MM`（运行时仅告警）
- ⚠️ 平台发现 2：`AUTO_TIME_ZONE=1` 时 `setTimeZone` 返回 false → DeviceOwner 自动关闭
- ⚠️ 发现 3：`SCHEDULE_EXACT_ALARM` Manifest 声明不自动授予（DO 亦非豁免）→ 模拟器 `appops set ... allow`；`scheduleAlarms` 无权限回退 `setAlarmClock`（无需权限、Doze 下精确触发）
- 修复：同一晚重武装 FIXED 按当前 `settings.offsetMin` 重算（否则改设置后下一周期不生效）；GRADUAL/FLUCTUATION 保留已推进值
- 验证：单测 7/7 + 模拟器 adb（TEST_ARM → SHIFT +180min → `Etc/GMT-3` → +0300 → RESTORE → +0000）✅

### 阶段 4-C（`edd6b0b`）偏移粒度约束为整小时
- **产品决策**：受平台约束（见附录 A #10），偏移粒度改为**整小时**（0/60/120/180，`Etc/GMT±H`）
- `OFFSET_STEP_MIN=60`；渐进步进 60/120；波动范围 0/60；波动每日变化限幅 `maxDailyDeltaMin=60`
- `settingsVersion` **v1→v2**：`migrate()` 将旧 15 分钟步进值归一化为整小时；`toSettings()` 读取时防御性归一化
- UI 滑条 steps=2（0/60/120/180）、渐进步进 0/120、波动范围 0/60；验证：单测 7/7 + 滑条中点吸附 +120 ✅

### v2 关键技术决策汇总（细节见附录 A #7）
1. 偏移量可变 → 动态时区 ID（`GMT±HH:MM` = 原始 UTC 偏移 + 偏移分钟；后受平台约束改整小时 IANA `Etc/GMT±H`）
2. 恢复按「系统显示时间」触发：恢复时刻 = 开始时刻 +（夜间显示时长 − 偏移），避免"08:30 突然跳回 06:30"
3. 三偏移模式：FIXED / GRADUAL（min(step×N, target) 逐日封顶）/ FLUCTUATION（target ± range，三角分布 + 每日变化限幅）
4. 持久化分层（用户配置 / 内部状态）+ `originalTimezoneId` 写保护（首次写入不可覆盖）
5. `armedEpochDay` 标记已武装夜晚，保证同一晚策略状态只推进一次

---

## 十、2026-08-20~21【日期待确认】｜阶段 7 实验：WRITE_SETTINGS 时区通道（已废弃）

- **内容**：测试 `WRITE_SETTINGS` 通道能否绕过 Device Owner 直接修改系统时区（`testTzBypass` 实验，写入 AlarmReceiver）
- **结局**：checkpoint #35「Phase11 清理」——实验无外部调用方、未提交，已移除（AlarmReceiver 恢复 HEAD 版本）；Manifest 中 `WRITE_SETTINGS` 权限声明与实验注释保留但**无代码使用**
- **归类**：产品开发探索（时区权限路线），失败/废弃，不代表最终方案
- **来源**：`D:\AndroidDev\SLEEPSHIFT_HISTORY_EXPORT.md`（一次性历史导出）；精确日期未记录【待确认】

---

## 十一、2026-08-19~21｜检修轨道 Phase 1-10：模拟器环境排障（非产品开发）

> **编号冲突提醒**：项目存在两套独立编号。产品轨道为「阶段N」（git 提交）与 P 编号（重构后）；**检修轨道 Phase 1-10 全部是模拟器环境排障**，与产品功能无关，仅记录于外层 `D:\AndroidDev`（CURRENT_STATE.md / PHASE8_* / logs / tools）。

| Phase | 内容 | 结论 |
|---|---|---|
| 1-6 | 安全框架（safe_diag / run_safe_emulator_test）、恢复机制（CURRENT_STATE / checkpoint / resume_repair）、故障分析自动化（analyze_failure / TEST_PROTOCOL）、实验记录（experiment_tracker / EXPERIMENT_RULES）、AI 交接系统（generate_handoff / AI_HANDOFF） | 工具链 |
| 7 | baseline 实验 ×3（默认 / swiftshader / 360s 均 TIMEOUT） | guest 早期启动卡死 |
| 8 | CPU/Hypervisor 只读诊断 → E1 kernel panic（EXT4 fs-verity inode#263157 损坏） | 指向文件系统/镜像损坏 |
| 9 | system 镜像完整性验证（SHA 比对 + 干净镜像冷启动成功） | 镜像非根因 |
| 10 | 实例层检查 → 根因 = **userdata/state 损坏** → SleepShift_AVD_v2（全新 userdata）110.5s 启动成功 | **解决** |

---

## 睡了么关键产品规则与决策

> 本节整理「睡了么」当前实现所依据的关键产品规则与决策（2026-08-26 最终审计后补充）。证据优先级：**当前代码（权威）> 仓库内记录（本日志 / screenshots README）> 外层设计文档（`D:\AndroidDev`，只读参考）**。无法从代码确认的明确标注【待确认】，不以设计文档推断为已实现。

### 1. 时间诚实模式（看真实时间的安全出口）

- **当前实现状态**：代码中未发现主页「查看真实时间」开关/切换入口；仅引导页（Onboarding）虚拟时间教学步骤有「真实时间」对照行（`ShuilemeOnboardingScreen.kt` 的 `StepRow("真实时间", real)`），主页时钟区只显示虚拟时间（`ShuilemeHomeScreen.kt`）。
- **结论**：【待确认/未实现】——依据当前代码无法确认该模式已实现。SL-2 设计文档（`D:\AndroidDev\Shuileme_SL2_VirtualClock_Design.md` 待确认决策 #7）要求"必须提供看真实时间开关"，但**不据此推断为已实现**。

### 2. SL-2 冻结参数（虚拟时间引擎）

| 参数 | 值 | 证据 |
|---|---|---|
| 偏移范围 | 0~240 分钟 | `shuileme/engine/VirtualClockEngine.kt`（MIN_OFFSET_MIN=0 / MAX_OFFSET_MIN=240）；主页偏移滑条 `valueRange = 0f..240f` |
| 步进 | 15 分钟 | `ShuilemeHomeScreen.kt` 偏移滑条 `updateOffset((it / 15f).roundToInt() * 15)`；SL-2 设计文档 §1.4 |
| 默认偏移 | 120 分钟 | `VirtualClockEngine.kt`（DEFAULT_OFFSET_MIN=120）；`ShuilemeRepository.kt` 默认 `currentOffsetMin=120` |
| 渐进模式默认步进 | 30 分钟 | `VirtualClockEngine.kt`（DEFAULT_GRADUAL_STEP_MIN=30）；SL-2 设计文档 §1.5 示例 day1 +30 |

### 3. 月亮成长规则

- **合格夜判定**：睡眠时长 ≥ 7 小时（420min）**且** 入睡时间 ≤ 目标入睡时间 + 30 分钟（`shuileme/model/MoonProgress.kt`：`DEFAULT_QUALIFIED_DURATION_MIN = 420L`、注释「入睡时间 ≤ 目标入睡时间 + 30min」）。
  - ⚠️ 与 SL-2 设计文档（"入睡 ≤ 目标入睡时间"，无宽限）存在差异：**实现更宽松，以代码为准**。
- **成长**：每个合格夜 growth +20%（`MoonProgress.kt` 注释「合格一晚 → growth +20%」）。
- **满月奖励**：growth 达 100% → 🌕 满月 → 触发奖励状态 🌝，成长与连续计数重置（`MoonProgress.kt` 注释与逻辑）。

### 4. 提醒频控

- **熬夜提醒每晚最多 2 次**：`shuileme/reminder/ReminderModels.kt`（`lateReminderMaxPerNight = 2`）；`ShuilemeReminderReceiver.kt` 检查 `lateReminderCount >= maxPerNight` 后跳过，并 `incrementLateReminder` 计数。
- **23:00~08:00 每小时检查点自续**：`ReminderModels.kt`（`lateCheckpointHours = [23,0,1,2,3,4,5,6,7]`）；`ShuilemeReminderScheduler.scheduleLateCheckpoint` 排下一个未来检查点，接收器处理后再自续。
- **静默窗语义（保留现状）**：`quietStartHour=23` / `quietEndHour=8`；睡前提醒在静默窗内不触发（`ShuilemeReminderReceiver.isInQuietWindow`），熬夜提醒不受静默窗限制（`ReminderModels.kt` 注释）。
- 补充：睡前提醒在目标入睡时间前 `sleepReminderAdvanceMin`（默认 30 分钟）触发，默认启用睡觉/熬夜两类提醒；「今晚放过我 🌙」（NIGHT_OFF）当日生效（`ShuilemeReminderReceiver.handleNightOff`）。

### 5. 睡眠人格

- **正式人格需要 5 晚解锁**：`shuileme/model/SleepPersonality.kt`（`MIN_SESSIONS = 5`）。
- **初始倾向可立即查看**：`SleepPersonality.kt`（`initialInclination(...)`）；主页在正式人格前显示引导期选择的 `onboarding.initialPersonality`（`ShuilemeHomeScreen.kt`「🌙 月亮正在认识你」占位）。来源补充：SL-9.3 附录（`D:\AndroidDev\Shuileme_SL9_3_UX_Correction.md`：初始人格倾向立即可查看、5 晚用于正式升级）。

### 6. 虚拟时间语义（窗口化）

- **当前实现**：目标睡眠窗口 `[targetSleepTime, targetWakeTime)` 内显示 虚拟时间 = 真实时间 + 偏移；**窗口外显示真实时间**（`VirtualClockEngine.virtualTimeMs(realTimeMs, sleepStartMin, wakeMin, zone)` + `isInSleepWindow`，支持跨午夜）。
- ⚠️ **与早期「恒偏移」设计存在差异**：SL-2 冻结规格（`Shuileme_SL2_VirtualClock_Design.md`）与本日志 §五 的记录公式为 `virtualNow = realNow + offsetMin*60_000`（恒偏移）；SL-9 起主页改为**窗口化**（代码注释「SL-9：目标睡眠窗口内虚拟时间」）。**以当前代码为准**，§五 公式为简化表述。

### 7. SL-9.1 真机反馈歧义【待确认】

- `D:\AndroidDev\Shuileme_SL9_1_Visual_Refinement.md` 标题/正文声称「基于 Redmi K80 真机反馈」进行视觉一致性修正；
- `D:\AndroidDev\Shuileme_SL9_Real_Device_Test.md` 记录当时「真机未连接（adb devices 为空）——本记录为模板，验证结果待设备连接后回填」；
- 两者相互矛盾，且仓库内无真机执行日志。**标记【待确认】，不判断哪一方为真**。

---

## 附录 A：历史技术问题与解决方案速查（旧 DEVELOPMENT_LOG #1~#17 完整保留）

> 以下条目保留旧版 `DEVELOPMENT_LOG.md` 的完整记录（日期 / 问题 / 尝试方案 / 结果 / 最终解决方式），供未来会话查阅。

### #1 Compose BOM 2026.08.00 与 AGP 8.13 不兼容（2026-08-19）
- 首次构建 `assembleDebug` 在 `:app:checkDebugAarMetadata` 失败，报 22 个 AAR 元数据错误；`androidx.compose.*:1.12.0` 要求 compileSdk 37 且要求 AGP 9.1.0+，而项目 compileSdk 36 / AGP 8.13.2
- 尝试：直接升级 AGP 9.x + compileSdk 37 + Gradle 9.x（改动面大，AGP 9 有破坏性改动，放弃）；查询各 BOM 版本对应 Compose 版本
- 解决：`gradle/libs.versions.toml` composeBom `2026.08.00` → `2025.08.00`（解析 Compose 1.9.0，仅需 compileSdk 36）；重新构建 `BUILD SUCCESSFUL`

### #2 Gradle 官方发行源下载极慢（2026-08-19）
- 从 `services.gradle.org` 下载 gradle-8.14.3-bin.zip 仅 ~37 KB/s；retry 参数无效
- 解决：腾讯镜像 `https://mirrors.cloud.tencent.com/gradle/gradle-8.14.3-bin.zip`（~11 MB/s，14 秒）；`gradle/wrapper/gradle-wrapper.properties` distributionUrl 指向腾讯镜像

### #3 curl 直连 GitHub / 证书吊销检查失败（2026-08-19）
- `curl -L` 下载 Temurin JDK MSI 报 `curl: (56) Recv failure: Connection was reset`；下载 Google CDN 报 `CRYPT_E_REVOCATION_OFFLINE`；`--retry 3` 无效
- 解决：能走 winget 的走 winget（下载通道较稳定）；其余加 `--ssl-no-revoke` 跳过证书吊销检查

### #4 Windows PowerShell 5.1 解析含中文 .ps1 静默失败（2026-08-19）
- `Start-Process -Verb RunAs` 提升执行含中文 .ps1 时脚本完全不执行；纯 ASCII 脚本正常
- 根因：PowerShell 5.1 对无 BOM .ps1 按系统代码页（中文系统 GBK）解析，UTF-8 中文字节导致整段解析失败
- 解决：所有需提权执行的 .ps1 一律纯 ASCII（英文日志/注释），写入执行日志文件确认运行

### #5 Android Studio NSIS 安装器忽略 `/D=` 目录参数（2026-08-19）
- `android-studio-*.exe /S /D=D:\AndroidDev\Android Studio` 仍装到 `C:\Program Files\Android\Android Studio`（exit 199）；NSIS 文档要求 `/D=` 无引号且在最后，但该安装器脚本硬编码路径
- 尝试：PowerShell 传参（带引号）、`MSYS2_ARG_CONV_EXCL` 禁用路径转换，均被忽略
- 解决：正常装到默认位置 → `robocopy /MOVE`（`MSYS2_ARG_CONV_EXCL="*"` 防止 `/MOVE` 被 MSYS 转换）迁移到 D 盘 → 管理员脚本更新注册表 `UninstallString`/`InstallLocation` + 修正开始菜单快捷方式

### #6 命令行会话不继承新设置的环境变量（2026-08-19）
- 设置机器级 `JAVA_HOME` 后当前会话 `java -version` 仍是旧 Java 8；进程环境是会话启动时快照
- 解决：命令行调用显式 `JAVA_HOME='D:\AndroidDev\JDK\jdk-21.0.12.8'`；新开终端/Android Studio 自动生效

### #7 v2 产品重设计：可配置偏移 + 按显示时间恢复（2026-08-19）
- 背景与关键决策 5 条见 §九「v2 关键技术决策汇总」；架构：UI → ViewModel → SettingsRepository(DataStore) → OffsetStrategy → TimezoneScheduler → AlarmReceiver → `DPM.setTimeZone()`
- Debug 测试入口（阶段 6 实现）：「立即偏移/立即恢复」UI 按钮 + adb 广播（`am broadcast -n com.sleepshift/.AlarmReceiver -a com.sleepshift.action.TEST_SHIFT`）

### #8 阶段 3：DataStore 配置持久化（2026-08-19）
- StateFlow 驱动（VM `stateIn` + UI `collectAsState`）；`settingsVersion` + `migrate()` 逐版本迁移；setter「值未变跳过写入」；`originalTimezoneId` 写保护下沉 Repository
- 验证发现：DO 应用 `am force-stop` 杀不掉 → 改用 `adb reboot` 验证持久化 ✅
- 验证命令备忘：模拟器启动 `emulator.exe -avd SleepShift_AVD -no-window`；等待 `adb wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'`；UI 检查 `adb exec-out uiautomator dump /dev/tty`；数据文件 `adb shell run-as com.sleepshift ls -la files/datastore/`

### #9 阶段 4-A：TimezoneScheduler 核心（2026-08-19）
- 零硬编码；恢复时刻 = 开始时刻 + realWindow（**须用当晚实际偏移**，不能用 settings.offsetMin——GRADUAL/FLUCTUATION 下当晚偏移来自策略结果）
- `strategyFor` 必须放 companion；`buildShiftTimeZoneId` 小时位补零；PendingIntent 用 `setClassName` 字符串定位；`armedEpochDay` 防重复推进
- 单测 5/5（22:30/06:30/+120 → shift=8/19 22:30、restore=8/20 04:30、动态时区 GMT+10:00/+10:15/+11:00、渐进 30→60→90→120、未启用返回 invalid）
- 注意：PendingIntent 的 extra（zone_id）在触发时读取；取消时用相同 action+requestCode（extra 不参与 PendingIntent 身份比较）

### #10 阶段 4-B：Receiver 适配（2026-08-19）
- ⚠️ 重大平台发现 1：`setTimeZone` 只应用 IANA 时区 ID（Android 16 / API 36）；自定义 `GMT±HH:MM`（含整小时 `GMT+02:00`、无冒号 `GMT+10`、ISO `+08:00`）全部静默忽略（返回 true 但 `persist.sys.timezone` 不变）；IANA ID（`Asia/Shanghai`、`Etc/GMT-2`）正常生效
  - 对策：`buildShiftZoneId` 改 IANA 感知——整小时总偏移 → `Etc/GMT±H`（POSIX 符号反转，UTC+H → Etc/GMT-H）；分数偏移回退 `GMT±HH:MM`（本平台静默无效，运行时仅告警）
- ⚠️ 平台发现 2：`AUTO_TIME_ZONE=1` 时 `setTimeZone` 返回 false；`DeviceOwner.setTimeZone` 已自动关闭（DO 可写 `Settings.Global.AUTO_TIME_ZONE`）
- ⚠️ 发现 3：`SCHEDULE_EXACT_ALARM` 需授权（DO 亦非豁免）；模拟器 `adb shell appops set com.sleepshift SCHEDULE_EXACT_ALARM allow`；无权限回退 `setAlarmClock`
- 修复：同一晚重武装 FIXED 按当前设置重算（否则 TEST_ARM 后仍是旧偏移 45）；GRADUAL/FLUCTUATION 保留已推进值
- 验证：单测 7/7；模拟器 adb TEST_ARM → SHIFT（+180min → `Etc/GMT-3` → +0300）→ RESTORE（GMT → +0000）✅
- adb 测试命令：`am broadcast -n com.sleepshift/.AlarmReceiver -a com.sleepshift.action.TEST_ARM` / `...SHIFT` / `...RESTORE` / `...TEST_CANCEL` / `...TEST_SET_ZONE --es zone_id "Asia/Shanghai"`
- 待产品决策：分数偏移（15 分钟步进）本平台无法经 setTimeZone 生效 → 后决策约束整小时（#11）

### #11 阶段 4-C：偏移粒度约束为整小时（2026-08-19）
- `OFFSET_STEP_MIN=60`；渐进步进 60/120（`MIN/MAX_GRADUAL_STEP_MIN`）；波动范围 0/60；波动每日变化限幅 `maxDailyDeltaMin=60`
- `settingsVersion` v1→v2：`migrate()` 归一化旧 15 分钟值；`toSettings()` 读取防御性归一化
- UI：偏移滑条 steps=2（0/60/120/180）、渐进步进 0/120、波动范围 0/60；验证：单测 7/7 + 模拟器滑条中点吸附 +120 ✅
- `buildShiftZoneId` 的 `GMT±HH:MM` 分数回退路径保留（防御，正常产品流程不再产生分数偏移）

### #12 阶段 5：模拟器端到端验证（2026-08-20）
- 验证结果：FIXED 三档 / GRADUAL 逐天 / FLUCTUATION 分布 / 正常重启重新武装 / 禁用即恢复 / 改配置自动重新武装，全部通过（单测 12/12）
- 修复 4 问题：禁用未联动；改配置未重新武装；FLUCTUATION 取整偏差（`OFFSET_STEP_HALF=7` 是 15 步进旧值 → `OFFSET_STEP_MIN / 2`）；`ensureActiveWindowRestore` 抽纯函数 `computeActiveRestoreEpoch` + 单测
- ⚠️ 模拟器环境发现：google_apis 模拟器每次启动将时区强制重置为 GMT，覆盖 `persist.sys.timezone`（禁用 GMS、auto_time=0 均无效）→ 「偏移状态重启」无法模拟器真实复现；真机保留；逻辑已纯函数化单测兜底
- 调试教训：FLUCTUATION「恒 120」误判为随机性问题，实为 UI 未真正选中模式——测试脚本要显式断言「模式已选中」再操作
- adb 命令：`TEST_FORCE_ADVANCE`（模拟新一晚推进策略）、`TEST_ACTIVE_RESTORE`（模拟偏移状态启动触发恢复兜底）

### #13 阶段 6：产品化 UI 与用户体验优化（2026-08-20）
- 首次启动引导 3 步 + `onboardingDone` 持久化；今日主页/配置实时解释/模式说明/通知（IMPORTANCE_LOW，仅成功时发送）
- 小坑：引导 Step 3 通知授权按钮初始状态需查真实权限（`checkSelfPermission`）；Compose 局部变量引用顺序（context 声明在状态初始化之前）
- 未在模拟器验证：配置页「睡眠模式中修改」Snackbar（需处于活动睡眠窗口，逻辑简单已代码确认）

### #14 Phase 11-A：TimeShiftEngine 抽象层（2026-08-21）
- 提交 `2cb975d`（8 文件 / 153 行）；内容见 §六

### #15 Phase 11-B：Shizuku 能力层（2026-08-21）
- 提交 `6b1dde8`（10 文件 / 275 行）；内容见 §六

### #16 Phase 11-C：Shizuku shell 时区修改 + 真机验证（2026-08-21）
- 实现与验证见 §七；真机（Redmi K80 + HyperOS）验证结果来源为外层文档；遗留 SystemUI 状态栏不刷新

### #17 Phase 11-D：SystemUI 刷新修复 + Shizuku 授权引导（2026-08-21）
- 三类刷新广播（TIMEZONE_CHANGED / TIME_SET / TIME_TICK）+ `refreshSystemUiClock` 调试入口；ShizukuGuideCard 三态引导；验证 `assembleDebug` ✅ / 单测 12/12 ✅；三条广播真机生效情况【待确认】

---

## 附录 B：重要文件地图（当前 HEAD 视角）

### 冻结轨道（系统时区操纵，LEGACY/FROZEN）
| 文件/目录 | 说明 |
|---|---|
| `time/TimezoneScheduler.kt` | 调度核心：planNight 纯计算 + 策略推进 + 闹钟武装 + IANA 时区映射（零硬编码，历史组件） |
| `model/SleepShiftModels.kt` / `strategy/OffsetStrategy.kt` | v2 数据模型 + 三模式偏移策略（**被睡了么复用**） |
| `data/SettingsRepository.kt` | Preferences DataStore 仓库（分层存储 + originalTimezoneId 写保护 + settingsVersion） |
| `admin/`（DeviceAdminReceiver/DeviceOwner） | Device Owner 通道 |
| `engine/`（TimeShiftEngine/EngineManager/DeviceOwnerTimeShiftEngine/ShizukuTimeShiftEngine） | 多引擎抽象（P8~P11） |
| `permission/`（ShizukuManager/ShizukuPermission/CapabilityState/CapabilityResolver） | Shizuku 能力层 |
| `AlarmReceiver.kt` / `BootReceiver.kt` / `AppCapabilities.kt` / `DebugActivity.kt` | 闹钟/开机接收器、能力检查、调试页 |
| `ui/`（SleepShiftApp/Home/Config/Mode/onboarding/debug + TimeWheelPicker/OffsetSlider/LivePreview） | 旧三页产品 UI |
| `notify/NotificationHelper.kt` | 非侵入通知 |

### 主产品轨道（睡了么）
| 文件/目录 | 说明 |
|---|---|
| `shuileme/engine/VirtualClockEngine.kt` | 虚拟时间引擎（真实时间 + offset = 虚拟时间，纯计算） |
| `shuileme/data/ShuilemeRepository.kt` | 睡了么 DataStore（睡眠记录/月亮/人格/OnboardingState 等） |
| `shuileme/model/` | SleepModels/MoonLifeModels/MoonProgress/SleepPersonality/SleepDetective/EmojiPhysicsState/EmojiResident/MainExperience/ResidentTalkSystem/PersonalityCardModel/OnboardingState |
| `shuileme/reminder/` | 提醒系统（ReminderModels/ReminderTemplate/Scheduler/Receiver/Notifier/SleepCapsule） |
| `shuileme/widget/` | Glance 桌面组件（Small/Medium + 刷新 + 显示逻辑） |
| `shuileme/ui/` | ShuilemeHomeScreen/OnboardingScreen/ViewModel/SleepCaseReportScreen/PersonalityCardScreen/NightTheme + components（NightMoon/SleepGoalEditor/TimeScrollPicker） |
| `ui/WelcomeDialog.kt` / `PersonalityCardActivity.kt` | 欢迎弹窗 / 人格卡分享 Activity |
| `GravitySensor.kt` | 重力传感器（SL-9.6 起已无引用，历史残留） |

### 构建与配置
| 文件 | 说明 |
|---|---|
| `app/build.gradle.kts` | AGP 8.13.2 / compileSdk 36 / minSdk 26 / versionName 0.3.25 / Compose / datastore / glance / shizuku |
| `gradle/libs.versions.toml` | BOM 锁定 2025.08.00（勿随意升级，见附录 A #1） |
| `settings.gradle.kts` | 阿里云 maven 镜像加速（官方源兜底） |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 8.14.3，分发地址指向腾讯镜像 |
| `AndroidManifest.xml` | 应用「睡了么」；DO/ShizukuProvider/各 Receiver；`WRITE_SETTINGS` 注释残留（阶段 7 实验） |

---

## 附录 C：环境与常用命令备忘

### 构建与测试
- 构建：`JAVA_HOME='D:\AndroidDev\JDK\jdk-21.0.12.8' ./gradlew.bat assembleDebug`（当前 shell 不继承新环境变量，必须显式指定）
- 单测：`JAVA_HOME='D:\AndroidDev\JDK\jdk-21.0.12.8' ./gradlew.bat testDebugUnitTest`

### 模拟器 / adb（冻结轨道验证用）
- adb 位于 `D:\AndroidDev\AndroidSdk\platform-tools\adb.exe`；模拟器 `SleepShift_AVD`（DO 已授权）、`SleepShift_AVD_v2`、`SleepShift_Accept`（Pixel 验收）
- DO 授权：`adb shell dpm set-device-owner com.sleepshift/.admin.DeviceAdminReceiver`
- 精确闹钟：`adb shell appops set com.sleepshift SCHEDULE_EXACT_ALARM allow`（重装 APK 后需重设）
- 通知权限：`adb shell pm grant com.sleepshift android.permission.POST_NOTIFICATIONS`
- 时区测试广播：TEST_ARM / TEST_SHIFT / TEST_RESTORE / TEST_CANCEL / TEST_SET_ZONE / TEST_FORCE_ADVANCE / TEST_ACTIVE_RESTORE（见附录 A #10/#12）

### 平台约束速查（已验证事实，勿重复验证）
- `setTimeZone` 只应用 IANA 时区 ID；`GMT±HH:MM` 静默忽略 → 偏移已约束整小时（`Etc/GMT±H`）
- `AUTO_TIME_ZONE=1` 时 `setTimeZone` 返回 false → 引擎内自动关闭
- DO 应用 `am force-stop` 杀不掉；持久化验证用 `adb reboot`
- google_apis 模拟器每次启动重置时区为 GMT（环境限制，非应用 bug）
- Shizuku 无法可靠修改系统时间（SET_TIME signature 权限）；HyperOS 状态栏刷新不可控 → 系统时区路线冻结

### 真机 Shizuku 激活（冻结轨道/高级实验）
- `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`（或无线调试）；重启后需重新激活；引擎可用 = Shizuku 运行 + 已授权

### 提权脚本注意
- 需管理员执行的 .ps1 必须纯 ASCII（PowerShell 5.1 按 GBK 解析无 BOM 中文脚本会静默失败）；带斜杠 Windows 参数用 `MSYS2_ARG_CONV_EXCL="*"` 或 PowerShell

---

## 附录 D：文档索引

### 仓库内
| 文件 | 用途 |
|---|---|
| `DEVELOPMENT_LOG.md` | **本文件：唯一长期开发历史日志（倒序）** |
| `PROJECT_STATUS.md` | 当前状态快照（停留在 Phase 11-D，未涵盖 SL 阶段；历史以本文件为准） |
| `screenshots/sl9_4/` ~ `sl9_10/` | SL-9 各轮验收截图 + README 报告（一手记录） |
| `scripts/` | shot_analyze.py / widget_place.py（SL-9.4 验收辅助） |

### 外层 `D:\AndroidDev`（历史/设计/状态参考，非仓库）
| 文件 | 用途 |
|---|---|
| `SLEEPSHIFT_HISTORY_EXPORT.md` | 一次性历史导出（git 提交表 + 双轨道划分 + 阶段 7 实验） |
| `Shuileme_Product_State.md` | 「睡了么」SL 轨道状态入口（SL-0~SL-9.5） |
| `SleepShift_Product_State.md` | P 编号产品状态（含 0.3.25 Alpha 完成记录，2026-08-22 更新） |
| `SleepShift_Legacy_State.md` | 冻结轨道（系统时区）结论与保留价值 |
| `Shuileme_Product_Design.md` / `Shuileme_Development_Rules.md` / `Shuileme_Feature_Backlog.md` | 产品定义/开发原则/创意池 |
| `Shuileme_SL2~SL9_*.md` | 各 SL 阶段设计文档 |
| `SleepShift_Product_Redesign.md` / `Feature_Backlog.md` | 产品重构设计与功能池 |
| `SLEEPSHIFT_MASTER_STATE.md` / `STATE_*_REPORT.md` / `CURRENT_STATE.md` | 状态体系与检修轨道历史 |
| `logs/claude_checkpoint.json` / `logs/experiments.json` | 断点与实验记录（UTF-8 BOM） |
| `tools/*.ps1` | 检修工具链（safe_diag / run_safe_emulator_test / resume_repair / analyze_failure / experiment_tracker / generate_handoff） |
