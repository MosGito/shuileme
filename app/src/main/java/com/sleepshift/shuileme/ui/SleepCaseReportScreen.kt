package com.sleepshift.shuileme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepshift.shuileme.model.MorningDetectiveReport

/**
 * 「月亮昨晚观察报告」（SL-9.3 视觉统一）：夜空 + 星点 + 月亮 + 半透明玻璃卡片，内容居中。
 * 数据仅供娱乐，非医疗结论。
 */
@Composable
fun SleepCaseReportScreen(report: MorningDetectiveReport, onBack: () -> Unit) {
    NightSurface(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // 顶部返回
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            TextButton(onClick = onBack) { Text("← 返回", color = ShuilemeNight.TextSecondary) }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // 半透明玻璃卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .background(ShuilemeNight.CardStrong, RoundedCornerShape(28.dp))
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🌙", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text("昨晚月亮观察", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.TextPrimary)
                Spacer(Modifier.height(20.dp))
                // 案件等级
                Text(report.caseLevel.emoji, fontSize = 56.sp)
                Text(
                    report.caseLevel.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ShuilemeNight.Accent,
                )
                Spacer(Modifier.height(20.dp))
                // 观察结论
                Text(
                    report.observation,
                    fontSize = 18.sp,
                    color = ShuilemeNight.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "案件结论：${report.conclusion}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ShuilemeNight.Accent,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    "数据仅供娱乐，月亮不是医生",
                    fontSize = 15.sp,
                    color = ShuilemeNight.TextSecondary,
                )
            }
        }
    }
}
