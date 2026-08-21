package com.sleepshift.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.sleepshift.engine.TimeShiftResult
import com.sleepshift.permission.CapabilityState
import kotlinx.coroutines.launch

/**
 * 统一 Debug Console（长期开发诊断工具，不参与主流程）。
 *
 * 布局：
 *  - 顶部：固定高度输出区（内部垂直滚动）+ 复制全部输出 / 清空日志
 *  - 中部：能力状态
 *  - 下方：测试工具区（Shizuku 测试 / P11 诊断 / P11 时区测试 / SystemUI 刷新 / P12-A Clock Shift）
 * 整个页面支持上下滚动；所有测试结果追加进统一日志 buffer。
 */
@Composable
fun DebugScreen(
    state: CapabilityState,
    onTestSetTimezone: suspend (String) -> TimeShiftResult,
    onRefreshSystemUi: suspend (String) -> TimeShiftResult,
    onDebugClockShift: suspend () -> TimeShiftResult,
    onTimezoneDiagnosis: suspend () -> TimeShiftResult,
) {
    var zoneInput by remember { mutableStateOf("GMT+08:00") }
    var fullLog by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    /** 追加一条实验结果进统一日志 buffer */
    fun appendLog(label: String, r: TimeShiftResult) {
        fullLog = fullLog + buildString {
            append("\n========== ").append(label).append(" ==========\n")
            append("success=").append(r.success).append("  engine=").append(r.engineType).append('\n')
            append(r.message).append('\n')
        }
        copied = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // ════════ 固定 Debug Console ════════
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Debug Console",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(fullLog))
                    copied = true
                },
            ) { Text("复制全部输出") }
            TextButton(onClick = { fullLog = ""; copied = false }) { Text("清空日志") }
        }
        if (copied) {
            Text(
                "已复制到剪贴板 ✓",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                if (fullLog.isBlank()) "（暂无日志：运行下方测试工具后，输出显示于此并可一键复制）" else fullLog,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(20.dp))

        // ════════ 能力状态 ════════
        Text("能力状态", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        CapabilityRow("Device Owner", state.deviceOwner)
        CapabilityRow("Shizuku 已安装", state.shizukuInstalled)
        CapabilityRow("Shizuku 运行中", state.shizukuRunning)
        CapabilityRow("Shizuku 已授权", state.shizukuPermissionGranted)
        CapabilityRow("Root 可用", state.rootAvailable)
        Text("Active Engine: ${state.activeEngine}", style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(20.dp))

        // ════════ 测试工具区 ════════
        Text("测试工具", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // 1) Shizuku 测试
        Text("1. Shizuku 测试", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = zoneInput,
            onValueChange = { zoneInput = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { appendLog("Shizuku 测试", onTestSetTimezone(zoneInput.trim())) } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Test Shizuku Set Timezone") }

        Spacer(Modifier.height(16.dp))

        // 2) P11 Timezone Diagnosis
        Text("2. P11 Timezone Diagnosis", style = MaterialTheme.typography.titleSmall)
        Text(
            "只读收集 persist.sys.timezone / time_zone / auto_time_zone / date / dumpsys alarm，判断时区是否真正生效（双时钟显示相同排查）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { appendLog("P11 Timezone Diagnosis", onTimezoneDiagnosis()) } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("运行诊断") }

        Spacer(Modifier.height(16.dp))

        // 3) P11 Timezone Test
        Text("3. P11 Timezone Test", style = MaterialTheme.typography.titleSmall)
        Text(
            "设置时区 → 触发 SystemUI 刷新（完整 P11 假设验证：确认时区生效 + 状态栏刷新）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    val zone = zoneInput.trim()
                    appendLog("P11 Timezone Test - setTimeZone", onTestSetTimezone(zone))
                    appendLog("P11 Timezone Test - refresh", onRefreshSystemUi(zone))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("设置时区 + SystemUI 刷新") }

        Spacer(Modifier.height(16.dp))

        // 4) SystemUI Refresh 测试
        Text("4. SystemUI Refresh 测试", style = MaterialTheme.typography.titleSmall)
        Text(
            "仅发送 TIMEZONE_CHANGED / TIME_SET / TIME_TICK 三类广播，隔离验证哪条对 HyperOS 状态栏生效。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { appendLog("SystemUI Refresh 测试", onRefreshSystemUi(zoneInput.trim())) } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("仅刷新 SystemUI（测试广播）") }

        Spacer(Modifier.height(16.dp))

        // 5) P12-A Clock Shift
        Text("5. P12-A Clock Shift 实验", style = MaterialTheme.typography.titleSmall)
        Text(
            "⚠️ 会真实尝试修改系统时间（关闭 auto_time → date 设置 → 恢复 auto_time），实验后设备重新联网对时。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { appendLog("P12-A Clock Shift 实验", onDebugClockShift()) } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("运行 P12-A 实验（改系统时间）") }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CapabilityRow(label: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            if (ok) "✔" else "✘",
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
