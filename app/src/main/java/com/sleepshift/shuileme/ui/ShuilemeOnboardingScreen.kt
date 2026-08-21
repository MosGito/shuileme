package com.sleepshift.shuileme.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepshift.shuileme.model.OnboardingState
import com.sleepshift.shuileme.reminder.ReminderPersonality
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 新用户体验（SL-4.5）：五步引导。
 * 1 欢迎 → 2 人格选择 → 3 虚拟时间体验 → 4 第一次我要睡了 → 5 人格预告。
 * emoji 优先，零图片资源；不修改系统时间。
 */
@Composable
fun ShuilemeOnboardingScreen(viewModel: ShuilemeViewModel) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var selectedPersonality by remember { mutableStateOf(ReminderPersonality.MOON) }
    var offsetMin by remember { mutableFloatStateOf(120f) }
    var slept by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { nowMs = System.currentTimeMillis(); delay(1000) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(5) { i ->
                val active = i + 1 == step
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (active) 22.dp else 6.dp)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            CircleShape,
                        )
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (step) {
                1 -> WelcomeStep(onNext = { step = 2 })
                2 -> PersonalityStep(
                    selected = selectedPersonality,
                    onSelect = { selectedPersonality = it },
                    onNext = { step = 3 },
                )
                3 -> VirtualTimeStep(
                    nowMs = nowMs,
                    offsetMin = offsetMin,
                    onOffsetChange = { offsetMin = it },
                    onNext = { step = 4 },
                )
                4 -> FirstSleepStep(
                    personality = selectedPersonality,
                    slept = slept,
                    onSleep = { viewModel.startSleep(System.currentTimeMillis()); slept = true },
                    onContinue = { step = 5 },
                )
                5 -> PreviewStep(onFinish = {
                    viewModel.completeOnboarding(
                        OnboardingState(
                            selectedPersonality = selectedPersonality,
                            virtualClockTutorialDone = true,
                        )
                    )
                })
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Text("🌙", fontSize = 72.sp)
    Spacer(Modifier.height(12.dp))
    Text("睡了么", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Text(
        "世界不会因为你熬夜停下来，\n但你可以先骗自己早点休息。",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(28.dp))
    Button(onClick = onNext) { Text("开始") }
}

@Composable
private fun PersonalityStep(
    selected: ReminderPersonality,
    onSelect: (ReminderPersonality) -> Unit,
    onNext: () -> Unit,
) {
    Text("选一个提醒你的小伙伴", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(12.dp))
    PersonaCard(ReminderPersonality.MOON, "温柔月亮", "今天辛苦啦，月亮想你了", selected == ReminderPersonality.MOON) {
        onSelect(ReminderPersonality.MOON)
    }
    PersonaCard(ReminderPersonality.SHARP, "毒舌朋友", "第几个最后一把了？", selected == ReminderPersonality.SHARP) {
        onSelect(ReminderPersonality.SHARP)
    }
    PersonaCard(ReminderPersonality.WORK_HORSE, "牛马提醒", "明天还要当牛马，先睡", selected == ReminderPersonality.WORK_HORSE) {
        onSelect(ReminderPersonality.WORK_HORSE)
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = onNext) { Text("下一步") }
}

@Composable
private fun PersonaCard(
    persona: ReminderPersonality,
    name: String,
    preview: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                if (selected) 2.dp else 0.dp,
                MaterialTheme.colorScheme.primary,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text("${persona.emoji} $name", style = MaterialTheme.typography.titleMedium)
        Text(
            preview,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VirtualTimeStep(
    nowMs: Long,
    offsetMin: Float,
    onOffsetChange: (Float) -> Unit,
    onNext: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val real = fmt.format(Instant.ofEpochMilli(nowMs).atZone(zone))
    val virtual = fmt.format(Instant.ofEpochMilli(nowMs + (offsetMin * 60_000L).toLong()).atZone(zone))
    Text("体验虚拟时间", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        "让应用里的时间，假装变晚",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    StepRow("真实时间", real)
    StepRow("偏移量", "+${offsetMin.toInt()} 分钟")
    StepRow("虚拟时间", virtual, highlight = true)
    Spacer(Modifier.height(8.dp))
    Text(
        "$real + ${offsetMin.toInt()} 分钟 = $virtual",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Slider(
        value = offsetMin,
        onValueChange = onOffsetChange,
        valueRange = 0f..240f,
        steps = 15,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "不修改系统时间，只影响这里的显示",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onNext) { Text("下一步") }
}

@Composable
private fun StepRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FirstSleepStep(
    personality: ReminderPersonality,
    slept: Boolean,
    onSleep: () -> Unit,
    onContinue: () -> Unit,
) {
    Text("第一次说晚安", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))
    AnimatedVisibility(visible = slept, enter = fadeIn() + scaleIn(initialScale = 0.7f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🌙", fontSize = 72.sp)
            Text("${personality.emoji}：晚安啦", style = MaterialTheme.typography.titleMedium)
            Text(
                "已入睡 · 月亮开始成长",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(24.dp))
    if (!slept) {
        Button(
            onClick = onSleep,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        ) {
            Text("我要睡了 🌙", fontSize = 24.sp)
        }
    } else {
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("继续 →") }
    }
}

@Composable
private fun PreviewStep(onFinish: () -> Unit) {
    Text("🫧", fontSize = 56.sp)
    Spacer(Modifier.height(12.dp))
    Text("你的睡眠人格正在观察中", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        "需要 5 晚睡眠数据。\n以后可以解锁：睡眠人格卡片。",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("完成") }
}
