# SL-9.5 真实体验修复 · 复验报告

> 日期：2026-08-21
> 环境：SleepShift_Accept（Pixel, API 36, emulator-5556）
> 基线：d9a244b → 本次修复未提交
> 构建/测试：`assembleDebug` + `testDebugUnitTest` ✅

## 一、9 项重点逐项结果

| # | 重点 | 结果 | 说明 |
|---|---|---|---|
| 1 | 页面边框 | ✅ | 窗框仅主页（框内 #26263C / 框外 #14142B），初始/设置/报告页无窗框 |
| 2 | 深色主题 | ✅ | 全部页面夜空背景 + 浅色文字，近黑像素扫描无黑字 |
| 3 | 初始体验 | ✅ | 月亮相遇页点击 🌙 无缝进主页，无独立按钮 |
| 4 | 时间选择 | ✅ **已修复** | 真滚动 + 中央指示选中 + 首尾可居中 + 默认值正确提交 |
| 5 | 月亮交互 | ✅ | 点击提示在月亮上方 (y=776<960)；长按蓄力环正常 |
| 6 | 设置机关 | ✅ **已修复** | 齿轮⚙️+挂绳+下拉打开，**面板从上方展开** + ✕ 关闭 |
| 7 | 人格气泡 | ✅ | 海獭=🌊🦦 来源匹配；顶部仅「你的睡眠人格是/海獭型」 |
| 8 | 气泡物理 | ✅ | 真实移动（8184px/1.5s）+ 拖动响应 + 窗台内不越界 |
| 9 | 页面 | ✅ | 月亮观察可进入；人格卡正常；初始人格立即可查看 |

## 二、本次修复清单

### 1. 时间选择器滚动异常（严重，引导 step3）
- **现象**：小时列完全卡住、分钟列滚动方向反转、默认值提交错误（曾提交 1770=29:30）
- **根因 A（无法滚动）**：引导内容 `Column.verticalScroll` 与外嵌 LazyColumn 嵌套滚动冲突 → 移除父级 scroll
- **根因 B（默认值错）**：reporting 在初始化时读取中心项，与选中项不符触发级联（0→1→2→…→59），污染默认值 → reporting 改为仅用户滚动结束后触发（`.drop(1)`），初始化保持选中值
- **根因 C（末项无法居中）**：LazyColumn 无端部 padding，末项(23时)被 clamp 到底部 → 加 `contentPadding` 上下半槽
- **验证**：初始 23时/0分 居中；提交 1380(23:00)/420(07:00) 正确；滚动 0→5分 正常

### 2. 引导 step5「海獭型型」重复「型」（中）
- `ShuilemeOnboardingScreen.kt` 3 处 `"${displayName}型"` 对已含「型」的 displayName（海獭/牛马/混沌）重复追加
- 修复：`displayName.removeSuffix("型") + "型"`（与 SL-9.4 人格卡同法）

### 3. 设置抽屉方向（设计）
- 原 `ModalNavigationDrawer` 从左侧展开，不符「从上方展开」
- 重构为 `AnimatedVisibility` + `slideInVertically` 顶部面板：齿轮下拉 → 面板自顶部滑下 + 半透明遮罩 + ✕ 关闭；导航按钮关闭面板
- 移除 ModalDrawerSheet/rememberDrawerState 等废弃引用

## 三、复验截图（screenshots/sl9_5/）
01 初始欢迎 / 02 月亮相遇 / 03 主页 / 04 设置（上方面板）/ 05 人格卡 / 06 月亮观察 / 07 Widget / 08 时间选择器滚动

## 四、Alpha APK 就绪
- 本次修复后 `assembleDebug` 产物 `app/build/outputs/apk/debug/app-debug.apk` 可安装到真机测试
- 3 文件改动：ShuilemeHomeScreen（设置面板）、ShuilemeOnboardingScreen（scroll+海獭型）、TimeScrollPicker（轮盘重写）
