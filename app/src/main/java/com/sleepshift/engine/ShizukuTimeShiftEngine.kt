package com.sleepshift.engine

import android.content.Context
import android.os.ParcelFileDescriptor
import com.sleepshift.permission.ShizukuManager
import com.sleepshift.permission.ShizukuPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shizuku 通道引擎（Phase 11-C 最小 shell 实验 + 11-D SystemUI 刷新）。
 *
 * 通过 [Shizuku.newProcess] 以 shell 权限执行：
 *   1) settings put global auto_time_zone 0/1
 *   2) service call alarm 3 s16 "<zoneId>"
 *   3) [refreshSystemUiClock]：setTimeZone 成功后发送时钟刷新广播
 *      （HyperOS 状态栏时间不随 setTimeZone 自动刷新，Phase 11-C 真机确认）。
 * 捕获 exit code / stdout / stderr，统一转换为 [TimeShiftResult]。
 * 不假设命令成功；未授权 / 异常一律返回失败。
 */
class ShizukuTimeShiftEngine(private val context: Context) : TimeShiftEngine {

    override val type: EngineType = EngineType.SHIZUKU

    override val isAvailable: Boolean
        get() = ShizukuManager.isShizukuRunning() && ShizukuPermission.isGranted()

    override suspend fun setTimeZone(zoneId: String): TimeShiftResult = withContext(Dispatchers.IO) {
        if (!isAvailable) {
            TimeShiftResult.fail(type, "Shizuku 未运行或未授权，拒绝执行 setTimeZone($zoneId)")
        } else {
            // 1) 关闭自动时区（否则固定时区可能被系统自动回写）
            val auto = execShell("settings put global auto_time_zone 0")
            if (auto.exitCode != 0) {
                TimeShiftResult.fail(
                    type,
                    "关闭自动时区失败 exit=${auto.exitCode} out=${auto.output}",
                )
            } else {
                // 2) 通过 alarm 服务设置时区
                val set = execShell("service call alarm 3 s16 \"$zoneId\"")
                if (set.exitCode != 0) {
                    TimeShiftResult.fail(
                        type,
                        "setTimeZone($zoneId) 失败 exit=${set.exitCode} out=${set.output}",
                    )
                } else {
                    // 3) 时区已生效；触发 SystemUI 时钟刷新（best-effort，失败不判定失败）
                    val refresh = runRefreshCommands(zoneId)
                    TimeShiftResult.ok(
                        type,
                        "setTimeZone($zoneId) OK; auto_tz disabled;\nSystemUI 刷新：\n$refresh",
                    )
                }
            }
        }
    }

    /**
     * 仅触发 SystemUI 时钟刷新（Phase 11-D 调试入口）。
     * 不修改时区，仅发送刷新广播，用于真机隔离验证哪个广播对 HyperOS 状态栏生效。
     */
    suspend fun refreshSystemUiClock(zoneId: String): TimeShiftResult = withContext(Dispatchers.IO) {
        if (!isAvailable) {
            TimeShiftResult.fail(type, "Shizuku 未运行或未授权，拒绝执行 refreshSystemUiClock")
        } else {
            TimeShiftResult.ok(type, "SystemUI 刷新广播已发送：\n${runRefreshCommands(zoneId)}")
        }
    }

    override suspend fun setAutoTimeZoneEnabled(enabled: Boolean): TimeShiftResult = withContext(Dispatchers.IO) {
        if (!isAvailable) {
            TimeShiftResult.fail(type, "Shizuku 未运行或未授权，拒绝执行 setAutoTimeZone($enabled)")
        } else {
            val value = if (enabled) "1" else "0"
            val r = execShell("settings put global auto_time_zone $value")
            if (r.exitCode == 0) {
                TimeShiftResult.ok(type, "auto_time_zone=$value OK; ${r.output}")
            } else {
                TimeShiftResult.fail(type, "auto_time_zone=$value 失败 exit=${r.exitCode} out=${r.output}")
            }
        }
    }

    /**
     * P12-A 可行性实验（Debug 专用，不参与正式调度）：Clock Shift 技术路线。
     *
     * 验证 Shizuku shell 是否可：
     *   1) 关闭自动确定日期和时间（`settings put global auto_time 0`）
     *   2) 设置系统当前时间（`date` 两种格式 + `cmd alarm help` 探测；目标 = 当前 + 2min 取整到分钟）
     *   3) 恢复自动确定日期和时间（`settings put global auto_time 1`）
     *
     * 不修改正式架构 / Scheduler；仅由 DebugActivity 手动触发。
     * ⚠️ 本实验会真实尝试修改设备系统时间；实验结束恢复 auto_time=1（设备重新联网对时，不会残留偏移）。
     * 返回逐步真实执行结果（exit code + stdout/stderr）。
     */
    suspend fun debugClockShiftTest(): TimeShiftResult = withContext(Dispatchers.IO) {
        if (!isAvailable) {
            TimeShiftResult.fail(type, "Shizuku 未运行或未授权，拒绝执行 debugClockShiftTest")
        } else {
            val sb = StringBuilder()
            sb.append("=== P12-A Clock Shift 可行性实验 ===\n")

            // 基线
            sb.append("基线: ").append(runCmd("当前时间", "date \"+%Y-%m-%d %H:%M:%S\"")).append('\n')
            sb.append("auto_time=").append(readSetting("auto_time")).append('\n')

            // 1) 关闭自动时间
            sb.append(runCmd("关闭自动时间", "settings put global auto_time 0")).append('\n')
            sb.append("禁用后 auto_time=").append(readSetting("auto_time")).append('\n')

            // 2) 设置时间（目标 = 当前 + 2min，取整到分钟）
            val targetMs = (System.currentTimeMillis() + 120_000L) / 60_000L * 60_000L
            val target = Date(targetMs)
            val targetStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(target)
            val legacyStr = SimpleDateFormat("MMddHHmmyyyy.ss", Locale.US).format(target)
            sb.append("目标时间: $targetStr\n")
            sb.append(runCmd("尝试 date -s", "date -s \"$targetStr\"")).append('\n')
            sb.append(runCmd("尝试 date legacy", "date $legacyStr")).append('\n')
            sb.append(runCmd("探测 cmd alarm help", "cmd alarm help")).append('\n')
            val after = execShell("date \"+%Y-%m-%d %H:%M:%S\"")
            val afterStr = after.output.trim()
            sb.append("设置后读回: exit=${after.exitCode} $afterStr\n")
            sb.append("生效判定: ${if (afterStr == targetStr) "已达目标" else "未达目标（可能无 SET_TIME 权限）"}\n")

            // 3) 恢复自动时间
            sb.append(runCmd("恢复自动时间", "settings put global auto_time 1")).append('\n')
            sb.append("恢复后 auto_time=").append(readSetting("auto_time")).append('\n')

            TimeShiftResult.ok(type, sb.toString())
        }
    }

    /**
     * P11 时区状态诊断（Debug 专用，只读）。
     *
     * 通过 Shizuku shell 收集系统时区真实状态，判断 HyperOS「双时钟显示相同」问题
     * 究竟是 SystemUI 刷新问题，还是时区状态未真正生效：
     *   - getprop persist.sys.timezone        持久化时区（真实生效依据）
     *   - settings get global time_zone       框架层时区 ID
     *   - settings get global auto_time_zone  自动时区开关
     *   - date / date -u                       本地 / UTC 当前时间
     *   - dumpsys alarm（截取）                AlarmManager 内部状态（权限允许时）
     * 不修改任何状态；仅由 DebugActivity 手动触发。结果进入 DebugScreen 可复制日志。
     */
    suspend fun debugTimezoneDiagnosis(): TimeShiftResult = withContext(Dispatchers.IO) {
        if (!isAvailable) {
            TimeShiftResult.fail(type, "Shizuku 未运行或未授权，拒绝执行 debugTimezoneDiagnosis")
        } else {
            val sb = StringBuilder()
            sb.append("=== P11 Timezone Diagnosis ===\n")
            sb.append(runCmd("getprop persist.sys.timezone", "getprop persist.sys.timezone")).append('\n')
            sb.append(runCmd("settings get global time_zone", "settings get global time_zone")).append('\n')
            sb.append(runCmd("settings get global auto_time_zone", "settings get global auto_time_zone")).append('\n')
            sb.append(runCmd("date (本地)", "date \"+%Y-%m-%d %H:%M:%S %Z %z\"")).append('\n')
            sb.append(runCmd("date -u (UTC)", "date -u \"+%Y-%m-%d %H:%M:%S\"")).append('\n')
            sb.append(runCmd("dumpsys alarm (前60行)", "dumpsys alarm | head -60")).append('\n')
            TimeShiftResult.ok(type, sb.toString())
        }
    }

    /** 执行命令并格式化输出（[label] exit=N out=...） */
    private fun runCmd(label: String, command: String): String {
        val r = execShell(command)
        return "[$label] exit=${r.exitCode} out=${r.output.trim()}"
    }

    /** 读取 global 设置值 */
    private fun readSetting(key: String): String {
        val r = execShell("settings get global $key")
        return if (r.exitCode == 0) r.output.trim() else "读取失败 exit=${r.exitCode}"
    }

    /**
     * 依次发送三类时钟刷新广播（shell 可发送 protected broadcast），逐条记录执行详情。
     * 1) TIMEZONE_CHANGED —— 语义正确；重发以防原始广播丢失或 SystemUI 未处理
     * 2) TIME_SET       —— 手动改时广播；部分 SystemUI 实现监听
     * 3) TIME_TICK      —— 每分钟时钟节拍；强制时钟立即重绘
     * 均为 best-effort：某条失败不中断后续，也不影响 setTimeZone 的成败判定。
     */
    private fun runRefreshCommands(zoneId: String): String =
        refreshCommands(zoneId).joinToString("\n") { cmd ->
            val r = execShell(cmd)
            "  [$cmd]\n    → exit=${r.exitCode} ${r.output.trim()}"
        }

    private fun refreshCommands(zoneId: String): List<String> = listOf(
        "am broadcast -a android.intent.action.TIMEZONE_CHANGED --es time-zone \"$zoneId\"",
        "am broadcast -a android.intent.action.TIME_SET",
        "am broadcast -a android.intent.action.TIME_TICK",
    )

    private data class ShellResult(val exitCode: Int, val output: String)

    /**
     * 通过 Shizuku binder（IShizukuService.newProcess）以 `sh -c` 执行。
     * stderr 重定向到 stdout 一并捕获；waitFor() 取真实 exit code。
     */
    private fun execShell(command: String): ShellResult {
        return try {
            val service = IShizukuService.Stub.asInterface(Shizuku.getBinder())
            val process = service.newProcess(arrayOf("sh", "-c", "$command 2>&1"), null, null)
            val output = ParcelFileDescriptor.AutoCloseInputStream(process.inputStream)
                .bufferedReader().readText()
            val exitCode = process.waitFor()
            ShellResult(exitCode, output)
        } catch (e: Throwable) {
            ShellResult(-1, "exec 异常: ${e::class.simpleName}: ${e.message}")
        }
    }
}
