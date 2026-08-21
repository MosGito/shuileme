package com.sleepshift.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepshift.shuileme.ui.ShuilemeNight

/**
 * SL-9.10 内测包装：首次启动欢迎弹窗。
 * 半透明深色玻璃卡片 + 夜空背景 + 浅色文字 + 月光黄强调；仅首次启动显示。
 */
@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .background(Color(0xE614142B), RoundedCornerShape(28.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🌙", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text("欢迎来到「睡了么」", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.Accent)
            Spacer(Modifier.height(14.dp))

            Text(
                "这是一个帮助你观察睡眠习惯，\n探索睡眠人格的小工具。",
                fontSize = 17.sp, color = ShuilemeNight.TextPrimary, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "你可以设置理想睡眠时间，记录当前睡眠状态，\n与月亮一起进入每一个夜晚。",
                fontSize = 16.sp, color = ShuilemeNight.TextSecondary, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            SectionTitle("已实现功能")
            FeatureText("· 睡眠目标设置")
            FeatureText("· 睡眠人格初步分析")
            FeatureText("· 月亮互动体验")
            FeatureText("· 人格气泡陪伴")
            FeatureText("· 睡眠时间调整体验")

            Spacer(Modifier.height(14.dp))
            SectionTitle("开发中的功能")
            FeatureText("· 桌面小组件")
            FeatureText("· 催睡通知")
            FeatureText("· 整夜睡眠监测")

            Spacer(Modifier.height(14.dp))
            SectionTitle("未来计划")
            FeatureText("· 修复睡眠时间监测中的问题")
            FeatureText("· 更多睡眠人格类型")
            FeatureText("· 灵动岛常驻「假」时钟")
            FeatureText("· 根据授权等级分级的系统时钟修改")
            FeatureText("· 睡眠人格卡片生成与分享")
            FeatureText("· 人格气泡碎碎念")

            Spacer(Modifier.height(16.dp))
            Text(
                "感谢参与内测。如果遇到问题，或有新的想法和建议，欢迎向开发者反馈。",
                fontSize = 15.sp, color = ShuilemeNight.TextSecondary, textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))
            // 月光黄「开始探索」按钮
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(ShuilemeNight.Accent.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("开始探索", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.TextPrimary)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ShuilemeNight.Accent)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun FeatureText(text: String) {
    Text(text, fontSize = 16.sp, color = ShuilemeNight.TextPrimary)
}
