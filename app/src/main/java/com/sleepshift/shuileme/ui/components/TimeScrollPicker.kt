package com.sleepshift.shuileme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepshift.shuileme.ui.ShuilemeNight

/**
 * SL-9.3：真轮盘时间选择器（0-23 时 / 0-59 分）。
 * 用户拖动数字列，中央高亮区数字成为当前值；禁止点击数字改变。
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
        NumberWheel(0..23, hour, "时", onHourChange, Modifier.weight(1f))
        NumberWheel(0..59, minute, "分", onMinuteChange, Modifier.weight(1f))
    }
}

@Composable
private fun NumberWheel(
    range: IntRange,
    selected: Int,
    suffix: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemHeight = 40.dp
    val containerHeight = 140.dp
    val halfPad = (containerHeight - itemHeight) / 2
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val centerOffset = with(density) { halfPad.toPx() }.toInt()

    // SL-9.5 修复：
    // - contentPadding 上下留半槽：首项（index=0）在 scroll=0 即天然居中，末项也能滚到中央；
    // - 初始化只把 index>0 的选中项滚到中央（index=0 保持自然居中）；
    // - 值上报与吸附都绑定「用户滚动结束」，初始化后不再触发，杜绝级联改默认值。
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .filter { it > 0 }
            .first()
        val index = (selected - range.first).coerceIn(0, range.count() - 1)
        if (index > 0) {
            listState.scrollToItem(index, scrollOffset = centerOffset)
        }
        initialized = true
    }

    // 中央高亮项判断（初始化后：仅在用户滚动结束后读取，避免初始抖动污染）
    LaunchedEffect(listState, initialized) {
        if (!initialized) return@LaunchedEffect
        snapshotFlow { listState.isScrollInProgress }
            .drop(1)
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling) {
                    val info = listState.layoutInfo
                    val center = (info.viewportEndOffset - info.viewportStartOffset) / 2f
                    val centerIdx = info.visibleItemsInfo
                        .firstOrNull { center >= it.offset && center < it.offset + it.size }
                        ?.index ?: return@collect
                    // 松手吸附：中央项精确居中（真轮盘手感）
                    listState.animateScrollToItem(centerIdx, scrollOffset = centerOffset)
                    // 上报当前值（值随用户滚动变化）
                    onSelect(range.first + centerIdx.coerceIn(0, range.count() - 1))
                }
            }
    }

    Box(
        modifier = modifier.height(containerHeight),
        contentAlignment = Alignment.Center,
    ) {
        // 中央高亮条
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(ShuilemeNight.CardStrong),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = halfPad),
        ) {
            items(range.count()) { i ->
                val v = range.first + i
                Text(
                    "$v$suffix",
                    fontSize = 20.sp,
                    fontWeight = if (v == selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (v == selected) ShuilemeNight.TextPrimary else ShuilemeNight.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                )
            }
        }
    }
}
