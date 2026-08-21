package com.sleepshift.shuileme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val density = LocalDensity.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selected - range.first).coerceAtLeast(0))

    // 初始滚动到选中项居中
    LaunchedEffect(Unit) {
        val centerOffset = with(density) { ((containerHeight - itemHeight) / 2).toPx() }.toInt()
        listState.scrollToItem((selected - range.first).coerceAtLeast(0), scrollOffset = centerOffset)
    }

    // 拖动/滚动 → 中央高亮项成为当前值（禁止点击）
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val center = info.viewportStartOffset + (info.viewportEndOffset - info.viewportStartOffset) / 2f
            info.visibleItemsInfo
                .firstOrNull { center >= it.offset && center < it.offset + it.size }
                ?.index
                ?: listState.firstVisibleItemIndex
        }.collect { idx -> onSelect(range.first + idx.coerceIn(0, range.count() - 1)) }
    }

    // 松手吸附：中央项精确居中（真轮盘手感）
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling) {
                    val info = listState.layoutInfo
                    val center = info.viewportStartOffset + (info.viewportEndOffset - info.viewportStartOffset) / 2f
                    val centerIdx = info.visibleItemsInfo
                        .firstOrNull { center >= it.offset && center < it.offset + it.size }
                        ?.index ?: return@collect
                    val centerOffset = with(density) { ((containerHeight - itemHeight) / 2).toPx() }.toInt()
                    listState.animateScrollToItem(centerIdx, scrollOffset = centerOffset)
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
