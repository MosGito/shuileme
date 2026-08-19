@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.sleepshift.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.debounce
import kotlin.math.roundToInt

/**
 * 自研双列轮盘时间选择器（小时列 + 分钟列）。
 * 滚动选择、居中吸附；分钟步进 15 分钟（0/15/30/45）。
 * 值类型为当天第几分钟（0-1439），与 SleepShiftSettings.startTimeMin / restoreTimeMin 一致。
 */
@Composable
fun TimeWheelPicker(
    minuteOfDay: Int,
    onMinuteOfDayChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hour = minuteOfDay / 60
    val minute = ((minuteOfDay % 60) + 7) / 15 * 15 // 吸附到最近 15 分钟档

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WheelColumn(
            items = (0..23).toList(),
            selectedValue = hour,
            format = { "%02d".format(it) },
            onValueSelected = { h -> onMinuteOfDayChange(h * 60 + minute) },
            modifier = Modifier.weight(1f),
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WheelColumn(
            items = listOf(0, 15, 30, 45),
            selectedValue = minute,
            format = { "%02d".format(it) },
            onValueSelected = { m -> onMinuteOfDayChange(hour * 60 + m) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WheelColumn(
    items: List<Int>,
    selectedValue: Int,
    format: (Int) -> String,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemHeight = 56.dp
    val visibleItems = 3
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }.roundToInt()
    val viewportHeight = itemHeight * visibleItems
    val initialIndex = items.indexOf(selectedValue).coerceAtLeast(0)
    val scrollState = rememberScrollState(initial = initialIndex * itemHeightPx)

    // 滚动停止后吸附到最近项
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .debounce(120)
            .collect { offset ->
                val targetIndex = (offset.toFloat() / itemHeightPx).roundToInt().coerceIn(0, items.lastIndex)
                val targetOffset = targetIndex * itemHeightPx
                if (targetOffset != offset) scrollState.animateScrollTo(targetOffset)
            }
    }

    val selectedIndex = (scrollState.value.toFloat() / itemHeightPx).roundToInt().coerceIn(0, items.lastIndex)

    // 选择变化 → 上报外部
    LaunchedEffect(selectedIndex) {
        val value = items[selectedIndex]
        if (value != selectedValue) onValueSelected(value)
    }

    // 外部值变化 → 滚动跟随（如程序化改值）
    LaunchedEffect(selectedValue) {
        val idx = items.indexOf(selectedValue).coerceAtLeast(0)
        if (idx != selectedIndex) scrollState.animateScrollTo(idx * itemHeightPx)
    }

    Box(
        modifier = modifier.height(viewportHeight),
        contentAlignment = Alignment.Center,
    ) {
        // 中心选择指示条
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(vertical = (viewportHeight - itemHeight) / 2),
        ) {
            items.forEachIndexed { index, value ->
                Text(
                    text = format(value),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == selectedIndex) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    ),
                )
            }
        }
    }
}
