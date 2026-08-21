package com.sleepshift.shuileme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepshift.shuileme.model.SleepPersonalityEngine
import com.sleepshift.shuileme.model.SleepPersonalityType
import com.sleepshift.shuileme.model.PersonalityCardGenerator
import com.sleepshift.shuileme.ui.ShuilemeNight

/**
 * SL-9.10：睡眠目标编辑器（共享组件）。
 *
 * 两组时间段（睡眠人格分析核心数据）：
 * - 目标睡眠时间段（理想）：targetSleepStart / targetWakeTime
 * - 当前睡眠时间段（现实）：currentSleepStart / currentWakeTime
 *
 * 理想睡眠时长 = 目标起床 - 目标入睡（自动计算，只展示，不选择）。
 * 初始页与设置页共用，一次修改两处同步。
 */
@Composable
fun SleepGoalEditor(
    targetSleepTimeMin: Int,
    targetWakeTimeMin: Int,
    currentSleepTimeMin: Int,
    currentWakeTimeMin: Int,
    onTargetSleepChange: (Int) -> Unit,
    onTargetWakeChange: (Int) -> Unit,
    onCurrentSleepChange: (Int) -> Unit,
    onCurrentWakeChange: (Int) -> Unit,
    showInitialInclination: Boolean = true,
) {
    // 理想睡眠时长 = 目标起床 - 目标入睡（跨午夜取正差）
    val idealDuration = if (targetWakeTimeMin > targetSleepTimeMin) targetWakeTimeMin - targetSleepTimeMin
    else (24 * 60 - targetSleepTimeMin) + targetWakeTimeMin

    Text("你的睡眠目标", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.TextPrimary)
    Spacer(Modifier.height(4.dp))

    // ── 第一组：目标睡眠时间段（理想状态）──
    GoalGroupHeader("目标睡眠时间段", "希望未来保持的作息")
    Label("目标入睡时间")
    TimeScrollPicker(
        hour = targetSleepTimeMin / 60,
        minute = targetSleepTimeMin % 60,
        onHourChange = { h -> onTargetSleepChange(h * 60 + targetSleepTimeMin % 60) },
        onMinuteChange = { m -> onTargetSleepChange((targetSleepTimeMin / 60) * 60 + m) },
    )
    Label("目标起床时间")
    TimeScrollPicker(
        hour = targetWakeTimeMin / 60,
        minute = targetWakeTimeMin % 60,
        onHourChange = { h -> onTargetWakeChange(h * 60 + targetWakeTimeMin % 60) },
        onMinuteChange = { m -> onTargetWakeChange((targetWakeTimeMin / 60) * 60 + m) },
    )
    // 理想睡眠时长（自动计算，只展示）
    Row(
        Modifier.fillMaxWidth().background(ShuilemeNight.CardStrong, RoundedCornerShape(12.dp))
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text("理想睡眠时长", fontSize = 18.sp, color = ShuilemeNight.TextSecondary, modifier = Modifier.padding(start = 16.dp))
        Text("${idealDuration / 60} 小时", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.Accent, modifier = Modifier.padding(end = 16.dp))
    }

    Spacer(Modifier.height(12.dp))

    // ── 第二组：当前睡眠时间段（现实状态）──
    GoalGroupHeader("当前睡眠时间段", "你现在真实的作息")
    Label("当前入睡时间")
    TimeScrollPicker(
        hour = currentSleepTimeMin / 60,
        minute = currentSleepTimeMin % 60,
        onHourChange = { h -> onCurrentSleepChange(h * 60 + currentSleepTimeMin % 60) },
        onMinuteChange = { m -> onCurrentSleepChange((currentSleepTimeMin / 60) * 60 + m) },
    )
    Label("当前起床时间")
    TimeScrollPicker(
        hour = currentWakeTimeMin / 60,
        minute = currentWakeTimeMin % 60,
        onHourChange = { h -> onCurrentWakeChange(h * 60 + currentWakeTimeMin % 60) },
        onMinuteChange = { m -> onCurrentWakeChange((currentWakeTimeMin / 60) * 60 + m) },
    )

    if (showInitialInclination) {
        Spacer(Modifier.height(6.dp))
        val inclination = SleepPersonalityEngine.initialInclination(targetSleepTimeMin, targetWakeTimeMin, idealDuration)
        Text(
            "初步倾向：${PersonalityCardGenerator.personalityPairEmoji(inclination)} ${inclination.displayName.removeSuffix("型")}型（低置信度）",
            fontSize = 22.sp,
            color = ShuilemeNight.Accent,
        )
    }
}

@Composable
private fun GoalGroupHeader(title: String, subtitle: String) {
    Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.TextPrimary)
    Text(subtitle, fontSize = 16.sp, color = ShuilemeNight.TextSecondary)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 22.sp, color = ShuilemeNight.TextSecondary)
}
