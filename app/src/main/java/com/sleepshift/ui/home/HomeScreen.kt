package com.sleepshift.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sleepshift.ui.NightPlan
import com.sleepshift.ui.SettingsViewModel
import com.sleepshift.ui.computeNightPlan
import com.sleepshift.ui.components.LivePreview
import com.sleepshift.ui.components.rememberNow
import com.sleepshift.ui.formatOffsetFriendly
import java.time.format.DateTimeFormatter

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 首页：今日状态 + 实时预览 + 今晚计划 + 总开关（含关闭确认） */
@Composable
fun HomeScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    val now = rememberNow()
    val plan = computeNightPlan(now, settings)
    var showDisableConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("今日", style = MaterialTheme.typography.headlineMedium)
        StatusCard(plan)
        LivePreview(offsetMin = plan.offsetMin)
        NightPlanCard(plan)
        EnableCard(
            enabled = settings.enabled,
            onToggle = { newValue ->
                if (settings.enabled && !newValue) {
                    showDisableConfirm = true
                } else {
                    viewModel.setEnabled(newValue)
                }
            },
        )
    }

    if (showDisableConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            title = { Text("关闭 SleepShift？") },
            text = { Text("关闭后手机时间将恢复正常。") },
            confirmButton = {
                TextButton(onClick = {
                    showDisableConfirm = false
                    viewModel.setEnabled(false)
                }) { Text("关闭") }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun StatusCard(plan: NightPlan) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "今日状态",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val (statusText, statusColor) = when {
                !plan.valid -> "配置无效" to MaterialTheme.colorScheme.error
                plan.nowShifted -> "睡眠模式" to MaterialTheme.colorScheme.tertiary
                else -> "正常时间" to MaterialTheme.colorScheme.primary
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineMedium,
                color = statusColor,
            )
            if (plan.valid) {
                Text(
                    text = "当前提前 ${formatOffsetFriendly(plan.offsetMin)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun NightPlanCard(plan: NightPlan) {
    if (!plan.valid) return
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "今晚计划",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PlanRow("开始", "${plan.startReal?.format(TIME_FMT)}")
            PlanRow("提前", formatOffsetFriendly(plan.offsetMin))
            PlanRow("恢复", "${plan.restoreReal?.format(TIME_FMT)}")
        }
    }
}

@Composable
private fun PlanRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EnableCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("启用 SleepShift", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (enabled) "睡眠模式已开启" else "已停用",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}
