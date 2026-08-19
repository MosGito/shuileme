package com.sleepshift.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sleepshift.ui.formatOffsetFriendly
import java.time.format.DateTimeFormatter

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * 实时效果预览（核心体验组件）：
 * 真实时间 → 提前 → 手机显示，让用户立即理解"手机时间会提前多少"。
 */
@Composable
fun LivePreview(offsetMin: Int, modifier: Modifier = Modifier) {
    val now = rememberNow()
    val shifted = now.plusMinutes(offsetMin.toLong())
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PreviewRow(label = "真实时间", value = now.format(TIME_FMT))
            PreviewRow(label = "提前", value = formatOffsetFriendly(offsetMin))
            PreviewRow(label = "手机显示", value = shifted.format(TIME_FMT), emphasized = true)
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String, emphasized: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
