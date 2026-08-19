package com.sleepshift.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sleepshift.ui.SettingsViewModel
import com.sleepshift.ui.computeNightPlan
import com.sleepshift.ui.components.LivePreview
import com.sleepshift.ui.components.OffsetSlider
import com.sleepshift.ui.components.TimeWheelPicker
import com.sleepshift.ui.components.rememberNow
import java.time.format.DateTimeFormatter

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 配置页：实时预览 + 开始/恢复时间轮盘 + 偏移滑动条 + 今晚效果 */
@Composable
fun ConfigScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    val now = rememberNow()
    val plan = computeNightPlan(now, settings)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("配置", style = MaterialTheme.typography.headlineMedium)

        // 核心：实时效果预览（拖动轮盘/滑条即时变化）
        LivePreview(offsetMin = settings.offsetMin)

        SectionCard(title = "开始偏移时间", hint = "真实时间到达该时刻时，系统时间向前跳") {
            TimeWheelPicker(
                minuteOfDay = settings.startTimeMin,
                onMinuteOfDayChange = viewModel::setStartTimeMin,
            )
        }

        SectionCard(title = "恢复时间", hint = "系统显示时间到达该时刻时恢复") {
            TimeWheelPicker(
                minuteOfDay = settings.restoreTimeMin,
                onMinuteOfDayChange = viewModel::setRestoreTimeMin,
            )
        }

        SectionCard(title = "偏移量", hint = "显示时间比真实时间提前多少（整小时步进）") {
            OffsetSlider(
                offsetMin = settings.offsetMin,
                onOffsetChange = viewModel::setOffsetMin,
            )
        }

        if (plan.valid) {
            SectionCard(title = "今晚效果") {
                Text("${plan.startReal?.format(TIME_FMT)} 开始 → 显示 ${plan.startDisplay?.format(TIME_FMT)}")
                Text("${plan.restoreReal?.format(TIME_FMT)} 恢复 → 显示 ${plan.restoreDisplay?.format(TIME_FMT)}")
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    text = "配置无效：请确保开始时间与恢复时间至少间隔 1 小时，且偏移量小于夜间时长。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}
