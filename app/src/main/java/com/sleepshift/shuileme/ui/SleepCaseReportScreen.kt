package com.sleepshift.shuileme.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepshift.shuileme.model.MorningDetectiveReport

/**
 * 睡眠案件报告页（SL-7）。
 * 娱乐化观察报告：案件等级 + 月亮观察 + 趣味结论。
 * 数据仅供娱乐，非医疗结论。
 */
@Composable
fun SleepCaseReportScreen(report: MorningDetectiveReport, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("← 返回") }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "🌙 昨晚月亮观察",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        Text(report.caseLevel.emoji, fontSize = 48.sp)
        Text(report.caseLevel.displayName, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text(report.observation, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "案件结论：${report.conclusion}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "数据仅供娱乐，月亮不是医生",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
