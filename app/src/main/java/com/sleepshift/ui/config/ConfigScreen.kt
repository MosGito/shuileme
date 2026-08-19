package com.sleepshift.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sleepshift.ui.SettingsViewModel
import com.sleepshift.ui.computeNightPlan
import com.sleepshift.ui.components.LivePreview
import com.sleepshift.ui.components.OffsetSlider
import com.sleepshift.ui.components.TimeWheelPicker
import com.sleepshift.ui.components.rememberNow
import com.sleepshift.ui.configExplanation
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 配置页：实时解释 + 开始/恢复时间轮盘 + 偏移滑动条 + 今晚效果 */
@Composable
fun ConfigScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    val now = rememberNow()
    val plan = computeNightPlan(now, settings)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun guard(change: () -> Unit) {
        // 睡眠模式中修改 → 提示下一周期生效
        if (plan.nowShifted) {
            scope.launch { snackbarHostState.showSnackbar("修改将在下一周期生效") }
        }
        change()
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("配置", style = MaterialTheme.typography.headlineMedium)

            // 实时解释文本（无需理解时区）
            Card(Modifier.fillMaxWidth()) {
                Text(
                    text = configExplanation(plan),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }

            LivePreview(offsetMin = settings.offsetMin)

            SectionCard(title = "开始偏移时间", hint = "真实时间到达该时刻时，手机时间会提前") {
                TimeWheelPicker(
                    minuteOfDay = settings.startTimeMin,
                    onMinuteOfDayChange = { guard { viewModel.setStartTimeMin(it) } },
                )
            }

            SectionCard(title = "恢复时间", hint = "手机显示时间到达该时刻时恢复正常") {
                TimeWheelPicker(
                    minuteOfDay = settings.restoreTimeMin,
                    onMinuteOfDayChange = { guard { viewModel.setRestoreTimeMin(it) } },
                )
            }

            SectionCard(title = "提前量", hint = "手机时间比真实时间提前多少（整小时）") {
                OffsetSlider(
                    offsetMin = settings.offsetMin,
                    onOffsetChange = { guard { viewModel.setOffsetMin(it) } },
                )
            }

            if (plan.valid) {
                SectionCard(title = "今晚效果") {
                    Text("${plan.startReal?.format(TIME_FMT)} 开始 → 手机显示 ${plan.startDisplay?.format(TIME_FMT)}")
                    Text("${plan.restoreReal?.format(TIME_FMT)} 恢复 → 手机显示 ${plan.restoreDisplay?.format(TIME_FMT)}")
                }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        text = "设置无效：请确保开始时间与恢复时间至少间隔 1 小时，且提前量小于夜间时长。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
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
