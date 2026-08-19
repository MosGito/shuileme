package com.sleepshift.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sleepshift.ui.NightPlan
import com.sleepshift.ui.SettingsViewModel
import com.sleepshift.ui.computeNightPlan
import com.sleepshift.ui.components.LivePreview
import com.sleepshift.ui.components.rememberNow
import java.time.format.DateTimeFormatter

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 首页：今日状态 + 实时预览 + 下次切换 + 总开关 */
@Composable
fun HomeScreen(viewModel: SettingsViewModel) {
    val settings = viewModel.settings
    val now = rememberNow()
    val plan = computeNightPlan(now, settings)

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
        NextTransitionCard(plan)
        EnableCard(enabled = settings.enabled, onToggle = viewModel::setEnabled)
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
                plan.nowShifted -> "时间偏移中" to MaterialTheme.colorScheme.tertiary
                else -> "正常时间" to MaterialTheme.colorScheme.primary
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineMedium,
                color = statusColor,
            )
            if (plan.valid) {
                Text(
                    text = "当前偏移 +${plan.offsetMin} 分钟",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun NextTransitionCard(plan: NightPlan) {
    if (!plan.valid) return
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "下次切换",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (plan.nowShifted) {
                Text("${plan.restoreReal?.format(TIME_FMT)} 恢复（显示 ${plan.restoreDisplay?.format(TIME_FMT)}）")
            } else {
                Text("${plan.startReal?.format(TIME_FMT)} 开始偏移（显示 ${plan.startDisplay?.format(TIME_FMT)}）")
            }
        }
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
                    text = if (enabled) "每日自动偏移已开启" else "已停用",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}
