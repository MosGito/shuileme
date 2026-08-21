package com.sleepshift.shuileme.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import com.sleepshift.shuileme.data.ShuilemeRepository
import com.sleepshift.shuileme.data.ShuilemeState

/**
 * 2x1 小组件（SL-3）：睡了么 · 虚拟时间 · 月亮 emoji · 当前状态。
 * 整块点击 → 打开 App。emoji 优先，零图片资源。
 */
class ShuilemeSmallWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // SL-9.2：防御性加载（读取失败回退默认，保证组件始终可渲染）
        val state = ShuilemeWidgets.loadWidgetState(context)
        val nowMs = System.currentTimeMillis()
        provideContent { SmallContent(state, nowMs) }
    }
}

class ShuilemeSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShuilemeSmallWidget()
}

@Composable
private fun SmallContent(state: ShuilemeState, nowMs: Long) {
    val context = LocalContext.current
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF16162B))
            .padding(8)
            .clickable(openApp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(ShuilemeWidgetDisplay.moonEmoji(state, nowMs), style = TextStyle(fontSize = 15.sp))
            Spacer(GlanceModifier.width(6))
            Text(
                ShuilemeWidgetDisplay.title(),
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color(0xFFB8B8D0))),
            )
        }
        Spacer(GlanceModifier.height(2))
        Text(
            ShuilemeWidgetDisplay.virtualTimeText(state, nowMs),
            style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.White)),
            maxLines = 1,
        )
        Text(
            ShuilemeWidgetDisplay.statusLine(state, nowMs),
            style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color(0xFFB8B8D0))),
            maxLines = 1,
        )
    }
}
