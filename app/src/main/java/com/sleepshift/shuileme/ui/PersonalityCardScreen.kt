package com.sleepshift.shuileme.ui

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepshift.shuileme.model.PersonalityCardGenerator
import kotlinx.coroutines.launch
import com.sleepshift.shuileme.model.PersonalityCardModel
import com.sleepshift.shuileme.model.SleepPersonalityType

/**
 * 人格分享卡片页（SL-6.5）。
 * - 未解锁：显示"再睡几晚，解锁你的睡眠人格 🌙" + 还差 X 晚；
 * - 已解锁：展示卡片（GraphicsLayer 捕获）→ 分享 → PNG + ACTION_SEND。
 * emoji 优先，零图片资源。
 */
@Composable
fun PersonalityCardScreen(
    model: PersonalityCardModel,
    onBack: () -> Unit,
    onShare: (Bitmap) -> Unit,
) {
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("← 返回") }
        }
        Spacer(Modifier.height(8.dp))
        if (model.unlocked) {
            // 卡片本体（可捕获为 Bitmap）
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawWithContent { graphicsLayer.record { this@drawWithContent.drawContent() } },
            ) {
                CardContent(model)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        onShare(bitmap)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("分享我的睡眠人格")
            }
        } else {
            Text(
                PersonalityCardGenerator.lockedCopy(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "还差 ${model.nightsNeeded} 晚",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CardContent(model: PersonalityCardModel) {
    val type = model.personalityType ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("睡了么", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(PersonalityCardGenerator.personalityPairEmoji(type), fontSize = 40.sp)
        Text(
            model.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            model.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        DataRow("🌙", "平均入睡", model.averageSleepTime)
        DataRow("☀️", "平均起床", model.averageWakeTime)
        DataRow("💤", "平均睡眠", model.averageDuration)
        DataRow("📈", "规律程度", "${model.regularityScore}%")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            model.emojiDecoration.forEach { Text(it, fontSize = 24.sp) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "睡了么 · 一个假装时间变晚的小工具",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DataRow(emoji: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
