package com.sleepshift.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sleepshift.model.MAX_OFFSET_MIN
import com.sleepshift.model.MIN_OFFSET_MIN
import com.sleepshift.model.OFFSET_STEP_MIN
import kotlin.math.roundToInt

/**
 * 偏移量滑动条：0~180 分钟，15 分钟步进，实时显示结果（+N 分钟）。
 */
@Composable
fun OffsetSlider(
    offsetMin: Int,
    onOffsetChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stepCount = (MAX_OFFSET_MIN - MIN_OFFSET_MIN) / OFFSET_STEP_MIN - 1
    Column(modifier) {
        Slider(
            value = offsetMin.toFloat(),
            onValueChange = { raw ->
                val snapped = ((raw / OFFSET_STEP_MIN).roundToInt() * OFFSET_STEP_MIN)
                    .coerceIn(MIN_OFFSET_MIN, MAX_OFFSET_MIN)
                onOffsetChange(snapped)
            },
            valueRange = MIN_OFFSET_MIN.toFloat()..MAX_OFFSET_MIN.toFloat(),
            steps = stepCount,
        )
        Text(
            text = "+$offsetMin 分钟",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
