package com.sleepshift.ui.mode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sleepshift.model.MAX_FLUCTUATION_RANGE_MIN
import com.sleepshift.model.MAX_GRADUAL_STEP_MIN
import com.sleepshift.model.MIN_FLUCTUATION_RANGE_MIN
import com.sleepshift.model.MIN_GRADUAL_STEP_MIN
import com.sleepshift.model.SchedulerState
import com.sleepshift.model.SleepShiftMode
import com.sleepshift.model.SleepShiftSettings
import com.sleepshift.strategy.GradualStrategy
import com.sleepshift.ui.SettingsViewModel
import kotlin.math.roundToInt

/** 模式页：三种偏移策略选择 + 模式参数 */
@Composable
fun ModeScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("偏移模式", style = MaterialTheme.typography.headlineMedium)

        ModeCard(
            title = "固定模式",
            desc = "每晚固定偏移相同时长，适合稳定规律作息。",
            selected = settings.mode == SleepShiftMode.FIXED,
            onSelect = { viewModel.setMode(SleepShiftMode.FIXED) },
        ) {
            Text("每晚固定偏移 +${settings.offsetMin} 分钟")
        }

        ModeCard(
            title = "渐进模式",
            desc = "每天逐步增加偏移，逐渐养成早睡习惯。",
            selected = settings.mode == SleepShiftMode.GRADUAL,
            onSelect = { viewModel.setMode(SleepShiftMode.GRADUAL) },
        ) {
            GradualContent(settings = settings, onStepChange = viewModel::setGradualStepMin)
        }

        ModeCard(
            title = "自然波动",
            desc = "在目标偏移附近小幅波动，减少机械感。",
            selected = settings.mode == SleepShiftMode.FLUCTUATION,
            onSelect = { viewModel.setMode(SleepShiftMode.FLUCTUATION) },
        ) {
            FluctuationContent(settings = settings, onRangeChange = viewModel::setFluctuationRangeMin)
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    desc: String,
    selected: Boolean,
    onSelect: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelect)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RadioButton(selected = selected, onClick = onSelect)
            }
            if (selected) content()
        }
    }
}

@Composable
private fun GradualContent(settings: SleepShiftSettings, onStepChange: (Int) -> Unit) {
    ValueRow("目标偏移", "+${settings.offsetMin} 分钟")
    ValueRow("每日步进", "+${settings.gradualStepMin} 分钟")
    Slider(
        value = settings.gradualStepMin.toFloat(),
        onValueChange = { raw ->
            val snapped = ((raw / 60).roundToInt() * 60)
                .coerceIn(MIN_GRADUAL_STEP_MIN, MAX_GRADUAL_STEP_MIN)
            onStepChange(snapped)
        },
        valueRange = MIN_GRADUAL_STEP_MIN.toFloat()..MAX_GRADUAL_STEP_MIN.toFloat(),
        steps = 0, // 60 / 120（整小时）
    )
    Text(
        text = gradualPreviewText(settings),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FluctuationContent(settings: SleepShiftSettings, onRangeChange: (Int) -> Unit) {
    ValueRow("目标偏移", "+${settings.offsetMin} 分钟")
    ValueRow("波动范围", "±${settings.fluctuationRangeMin} 分钟")
    Slider(
        value = settings.fluctuationRangeMin.toFloat(),
        onValueChange = { raw ->
            val snapped = ((raw / 60).roundToInt() * 60)
                .coerceIn(MIN_FLUCTUATION_RANGE_MIN, MAX_FLUCTUATION_RANGE_MIN)
            onRangeChange(snapped)
        },
        valueRange = MIN_FLUCTUATION_RANGE_MIN.toFloat()..MAX_FLUCTUATION_RANGE_MIN.toFloat(),
        steps = 0, // 0 / 60（整小时）
    )
    Text(
        text = "每晚实际偏移 = 目标 ± 范围（三角分布），且相邻两晚变化不超过 60 分钟。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

/** 用真实策略引擎推算渐进序列预览 */
private fun gradualPreviewText(settings: SleepShiftSettings): String {
    val target = settings.offsetMin
    val step = settings.gradualStepMin
    if (target <= step) return "第 1 晚即达 +$target 分钟"
    val nights = buildList {
        var state = SchedulerState()
        var result = GradualStrategy.next(settings, state)
        while (result.offsetMin < target && size < 5) {
            add(result.offsetMin)
            state = result.state
            result = GradualStrategy.next(settings, state)
        }
    }
    val seq = nights.joinToString(" → ") { "+$it" }
    return "第1晚 $seq → +$target（目标）"
}
