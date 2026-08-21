package com.sleepshift.shuileme.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sleepshift.MainActivity
import com.sleepshift.shuileme.data.ShuilemeState

/**
 * 4x2 中组件（SL-3 + SL-9.4 修复）：虚拟时间 · 月亮成长 · 连续次数 · 一键按钮。
 * 尺寸必须用 .dp（Glance 的 Int 参数会被当作资源 ID 导致崩溃）。
 */
class ShuilemeMediumWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // SL-9.2：防御性加载（读取失败回退默认，保证组件始终可渲染）
        val state = ShuilemeWidgets.loadWidgetState(context)
        val nowMs = System.currentTimeMillis()
        provideContent { MediumContent(state, nowMs) }
    }
}

class ShuilemeMediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShuilemeMediumWidget()
}

@Composable
private fun MediumContent(state: ShuilemeState, nowMs: Long) {
    val context = LocalContext.current
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF16162B))
            .padding(12.dp)
            .clickable(openApp),
    ) {
        // 第一行：月亮 + 虚拟时间
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(ShuilemeWidgetDisplay.moonEmoji(state, nowMs), style = TextStyle(fontSize = 26.sp))
            Spacer(GlanceModifier.width(10.dp))
            Text(
                ShuilemeWidgetDisplay.virtualTimeText(state, nowMs),
                style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White)),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(
            ShuilemeWidgetDisplay.statusLine(state, nowMs),
            style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color(0xFFB8B8D0))),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(6.dp))
        // 统计行
        Row(GlanceModifier.fillMaxWidth()) {
            Text(
                ShuilemeWidgetDisplay.growthText(state),
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color(0xFFB8B8D0))),
            )
            Spacer(GlanceModifier.width(12.dp))
            Text(
                ShuilemeWidgetDisplay.streakText(state),
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color(0xFFB8B8D0))),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        // 快捷按钮（右下）
        Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.End) {
            QuickActionButton(state)
        }
    }
}

@Composable
private fun QuickActionButton(state: ShuilemeState) {
    val isSleeping = state.isSleeping
    val action = if (isSleeping) actionRunCallback<WakeUpAction>() else actionRunCallback<StartSleepAction>()
    Column(
        modifier = GlanceModifier
            .clickable(action)
            .background(if (isSleeping) Color(0xFF2E5E4E) else Color(0xFF3D3D7A))
            .padding(10.dp),
    ) {
        Text(
            if (isSleeping) "我醒啦 ☀️" else "我要睡了 🌙",
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White)),
        )
    }
}
