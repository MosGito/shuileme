package com.sleepshift.shuileme.ui.components

import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sleepshift.shuileme.ui.ShuilemeNight

/**
 * SL-9.8.1：时间滚轮 —— Android 原生 NumberPicker（AndroidView 嵌入）。
 *
 * 原生组件保证 value 语义正确（中央值 = 最终值，无 offset/index 分离）；
 * 自定义 Compose WheelPicker 曾尝试「选中放大」但出现 value 污染（programmatic scroll
 * 触发 snap 反馈），回退到原生方案保证核心原则：手指停止位置 = 中央高亮 = 当前 value。
 */
@Composable
fun TimeScrollPicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NativeNumberWheel(0, 23, hour, "时", onHourChange, Modifier.weight(1f))
        NativeNumberWheel(0, 59, minute, "分", onMinuteChange, Modifier.weight(1f))
    }
}

@Composable
private fun NativeNumberWheel(
    min: Int,
    max: Int,
    selected: Int,
    suffix: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 中央高亮条（半透明白，衬托选中值）
        Box(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(ShuilemeNight.CardStrong),
        )
        val suppress = java.util.concurrent.atomic.AtomicBoolean(false)
        AndroidView(
            factory = { ctx ->
                NumberPicker(ctx).apply {
                    minValue = min
                    maxValue = max
                    value = selected.coerceIn(min, max)
                    wrapSelectorWheel = true
                    setFormatter { v -> "$v$suffix" }
                    // 深色主题：浅色文字 + 透明背景 + 细分隔线
                    setTextColor(ShuilemeNight.TextPrimary.toArgb())
                    setTextSize(38f) // SL-9.10：滚轮数字大字号（26→38sp）
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    selectionDividerHeight = 2
                    setOnValueChangedListener { _, _, newVal ->
                        // SL-9.8.1：程序化同步（update 设 value）会触发回调 → 用 flag 抑制，避免反馈污染
                        if (!suppress.get()) onSelect(newVal)
                    }
                }
            },
            update = { picker ->
                val target = selected.coerceIn(min, max)
                if (picker.value != target) {
                    suppress.set(true)
                    picker.value = target
                    suppress.set(false)
                }
            },
        )
    }
}
