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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepshift.shuileme.model.MoonGlow
import com.sleepshift.shuileme.model.MoonLife
import com.sleepshift.shuileme.model.MoonStage
import com.sleepshift.shuileme.model.OnboardingState
import com.sleepshift.shuileme.model.PersonalityCardGenerator
import com.sleepshift.shuileme.model.SleepPersonalityType
import com.sleepshift.shuileme.model.personality.PersonalityColdStart
import com.sleepshift.shuileme.reminder.ReminderPersonality
import com.sleepshift.shuileme.ui.components.MoonStageLayout
import com.sleepshift.shuileme.ui.components.NightMoon
import com.sleepshift.shuileme.ui.components.SleepGoalEditor
import com.sleepshift.shuileme.ui.components.TimeScrollPicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 新用户体验（SL-9 修正版）：六步引导，内容垂直居中，全部步骤支持「上一步」。
 * 1 欢迎 → 2 人格选择 → 3 睡眠目标（作息→初始人格倾向）→ 4 虚拟时间 → 5 我要睡了 → 6 人格预告。
 */
@Composable
fun ShuilemeOnboardingScreen(viewModel: ShuilemeViewModel) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var selectedPersonality by remember { mutableStateOf(ReminderPersonality.MOON) }
    var sleepTimeMin by remember { mutableStateOf(23 * 60) }
    var wakeTimeMin by remember { mutableStateOf(7 * 60) }
    // SL-9.10：当前真实作息（第二组时间）
    var currentSleepTimeMin by remember { mutableStateOf(2 * 60) }
    var currentWakeTimeMin by remember { mutableStateOf(12 * 60) }
    var offsetMin by remember { mutableFloatStateOf(120f) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { nowMs = System.currentTimeMillis(); delay(1000) } }

    // 理想睡眠时长自动计算（目标起床 - 目标入睡）
    val idealDurationMin = remember(sleepTimeMin, wakeTimeMin) {
        if (wakeTimeMin > sleepTimeMin) wakeTimeMin - sleepTimeMin
        else (24 * 60 - sleepTimeMin) + wakeTimeMin
    }
    // STEP 3（PHASE 6 / K-4）：冷启动人格基线 = V2.0.7 §11.2 Cold Start Domain，
    // 基于用户当前真实作息（currentSleepTime/currentWakeTime），不再调用旧
    // SleepPersonalityEngine.initialInclination。目标作息只参与理想时长展示，不决定初步人格。
    val initialPersonality = remember(currentSleepTimeMin, currentWakeTimeMin) {
        PersonalityColdStart.classify(currentSleepTimeMin, currentWakeTimeMin)
            ?.let { PersonalityResultAdapter.legacyType(it) }
    }

    NightSurface(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        // 步骤指示点（5 步；SL-9.8.1 step5 全屏月亮时隐藏）
        if (step != 5) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(5) { i ->
                    val active = i + 1 == step
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (active) 20.dp else 6.dp)
                            .background(
                                if (active) ShuilemeNight.Accent
                                else ShuilemeNight.TextSecondary.copy(alpha = 0.3f),
                                CircleShape,
                            )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 内容（垂直居中；SL-9.5 移除 verticalScroll：与外嵌 TimeScrollPicker 的 LazyColumn 嵌套滚动冲突，
        // 导致时间选择器无法拖动。各步骤内容在目标设备均放得下。SL-9.8.1：step5 全屏月亮时隐藏。）
        if (step != 5) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (step) {
                1 -> WelcomeStep(onNext = { step = 2 })
                2 -> PersonalityStep(
                    selected = selectedPersonality,
                    onSelect = { selectedPersonality = it },
                    onNext = { step = 3 },
                )
                3 -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    SleepGoalEditor(
                        targetSleepTimeMin = sleepTimeMin,
                        targetWakeTimeMin = wakeTimeMin,
                        currentSleepTimeMin = currentSleepTimeMin,
                        currentWakeTimeMin = currentWakeTimeMin,
                        onTargetSleepChange = { sleepTimeMin = it },
                        onTargetWakeChange = { wakeTimeMin = it },
                        onCurrentSleepChange = { currentSleepTimeMin = it },
                        onCurrentWakeChange = { currentWakeTimeMin = it },
                    )
                    Spacer(Modifier.height(8.dp))
                    NightButton(onClick = { step = 4 }) { Text("下一步") }
                }
                4 -> VirtualTimeStep(
                    nowMs = nowMs,
                    offsetMin = offsetMin,
                    onOffsetChange = { offsetMin = it },
                    onNext = { step = 5 },
                )
            }
        }
        }

        // SL-9.8.1：step5 全屏月亮（与主页同一父级布局坐标，MoonStageLayout 居中）
        if (step == 5) {
            GoodnightStep(
                initial = initialPersonality,
                onFinish = {
                    viewModel.completeOnboarding(
                        OnboardingState(
                            selectedPersonality = selectedPersonality,
                            targetSleepTime = sleepTimeMin,
                            targetWakeTime = wakeTimeMin,
                            currentSleepTime = currentSleepTimeMin,
                            currentWakeTime = currentWakeTimeMin,
                            virtualClockTutorialDone = true,
                            initialPersonality = initialPersonality,
                        )
                    )
                },
            )
        }

        // 上一步（步骤 2-4 支持返回；step5 全屏无返回）
        if (step > 1 && step != 5) {
            TextButton(onClick = { step-- }) { Text("← 上一步", color = ShuilemeNight.TextSecondary) }
        }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Text("🌙", fontSize = 72.sp)
    Spacer(Modifier.height(12.dp))
    Text("睡了么", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.TextPrimary)
    Spacer(Modifier.height(12.dp))
    Text(
        "世界不会因为你熬夜停下来，\n但你可以先骗自己早点休息。",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = ShuilemeNight.TextSecondary,
    )
    Spacer(Modifier.height(28.dp))
    NightButton(onClick = onNext) { Text("开始") }
}

@Composable
private fun PersonalityStep(
    selected: ReminderPersonality,
    onSelect: (ReminderPersonality) -> Unit,
    onNext: () -> Unit,
) {
    Text("选一个提醒你的小伙伴", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.TextPrimary)
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
    NightButton(onClick = onNext) { Text("下一步") }
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
                if (selected) ShuilemeNight.CardStrong
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                if (selected) 2.dp else 0.dp,
                ShuilemeNight.Accent,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text("${persona.displayEmoji()} $name", style = MaterialTheme.typography.titleMedium)
        Text(
            preview,
            style = MaterialTheme.typography.bodySmall,
            color = ShuilemeNight.TextSecondary,
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
    Text("体验虚拟时间", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.TextPrimary)
    Spacer(Modifier.height(8.dp))
    Text(
        "睡眠窗口内，让应用里的时间假装变晚",
        style = MaterialTheme.typography.bodyMedium,
        color = ShuilemeNight.TextSecondary,
    )
    Spacer(Modifier.height(16.dp))
    StepRow("真实时间", real)
    StepRow("偏移量", "+${offsetMin.toInt()} 分钟")
    StepRow("虚拟时间", virtual, highlight = true)
    Spacer(Modifier.height(8.dp))
    Text(
        "$real + ${offsetMin.toInt()} 分钟 = $virtual",
        style = MaterialTheme.typography.bodySmall,
        color = ShuilemeNight.TextSecondary,
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
        color = ShuilemeNight.TextSecondary,
    )
    Spacer(Modifier.height(16.dp))
    NightButton(onClick = onNext) { Text("下一步") }
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
            color = if (highlight) ShuilemeNight.Accent else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** SL-9.8：今夜向月亮道晚安 —— 与主页共用 MoonStageLayout（月亮居中），坐标/大小完全一致。 */
@Composable
private fun GoodnightStep(initial: SleepPersonalityType?, onFinish: () -> Unit) {
    var rippleTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    // 未来主页的新月状态：BABY 阶段 + 默认月光强度（与 HomeScreen MoonWithGlow 完全一致）
    val freshIntensity = remember { MoonGlow.glowIntensity(MoonLife()) }
    val freshAlpha = remember { MoonGlow.glowAlpha(freshIntensity) }
    val freshScale = remember { MoonGlow.glowScale(freshIntensity) }
    Box(Modifier.fillMaxSize()) {
        // 月亮舞台：与主页同款居中布局（SL-9.8）
        MoonStageLayout(Modifier.fillMaxSize()) {
            NightMoon(
                emoji = MoonStage.BABY.emoji,
                // 全屏 MoonStageLayout 已使月亮居中；不再额外 offset（避免与主页偏差 21dp）
                glowAlpha = freshAlpha,
                glowScale = freshScale,
                tapRippleTick = rippleTick,
                onMoonClick = {
                    // 播放月亮点击波纹 → 短暂停留 → 无缝进入主页（无按钮式跳转）
                    rippleTick++
                    scope.launch { delay(600); onFinish() }
                },
            )
        }
        // SL-9.10 最终：文字组置于月亮上方（TopCenter + 固定偏移），标题→提示→留白→月亮，不遮挡
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "今夜，向月亮道个晚安",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = ShuilemeNight.Accent,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "点击月亮进入主页",
                fontSize = 20.sp,
                color = ShuilemeNight.TextSecondary.copy(alpha = 0.7f),
            )
            if (initial != null) {
                Spacer(Modifier.height(18.dp))
                Text(
                    "初步倾向：${PersonalityCardGenerator.personalityPairEmoji(initial)} ${initial.displayName.removeSuffix("型")}型（低置信度）",
                    fontSize = 18.sp,
                    color = ShuilemeNight.Accent.copy(alpha = 0.9f),
                )
                Text(
                    "睡满 5 晚解锁正式人格卡片",
                    fontSize = 16.sp,
                    color = ShuilemeNight.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun PreviewStep(initial: SleepPersonalityType?, onFinish: () -> Unit) {
    Text("🫧", fontSize = 60.sp)
    Spacer(Modifier.height(12.dp))
    Text("你的睡眠人格正在观察中", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.TextPrimary)
    Spacer(Modifier.height(8.dp))
    if (initial != null) {
        Text(
            "初步倾向：${initial.primaryEmoji}${initial.comboEmojis.last()} ${initial.displayName.removeSuffix("型")}型",
            style = MaterialTheme.typography.bodyMedium,
            color = ShuilemeNight.Accent,
        )
        Text(
            "置信度较低，睡满 5 晚解锁正式人格卡片",
            style = MaterialTheme.typography.bodySmall,
            color = ShuilemeNight.TextSecondary,
        )
    }
    Text(
        "之后每天的真实睡眠会更新它。",
        style = MaterialTheme.typography.bodySmall,
        color = ShuilemeNight.TextSecondary,
    )
    Spacer(Modifier.height(24.dp))
    NightButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("完成") }
}
