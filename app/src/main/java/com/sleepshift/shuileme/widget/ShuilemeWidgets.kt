package com.sleepshift.shuileme.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.sleepshift.shuileme.data.ShuilemeRepository
import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.reminder.SleepCapsule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 组件刷新入口 + 15 分钟周期刷新调度（SL-3）。
 * - [refreshAll]：事件刷新（startSleep/wakeUp/满月奖励 后调用）；
 * - [ShuilemeWidgetRefreshScheduler.schedule]：AlarmManager 15 分钟周期刷新（虚拟时间展示随周期更新）；
 * - [ShuilemeWidgetRefreshReceiver]：接收周期刷新广播 + BOOT_COMPLETED 重排。
 */
object ShuilemeWidgets {
    /** 刷新全部组件（2x1 + 4x2） */
    fun refreshAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { refreshWidget(context, ShuilemeSmallWidget(), ShuilemeSmallWidget::class.java) }
            runCatching { refreshWidget(context, ShuilemeMediumWidget(), ShuilemeMediumWidget::class.java) }
        }
    }

    /** SL-9.2：防御性加载组件状态（读取失败回退默认，保证组件始终可渲染） */
    suspend fun loadWidgetState(context: Context): ShuilemeState =
        runCatching { ShuilemeRepository(context).current() }.getOrElse { ShuilemeState() }

    private suspend fun refreshWidget(context: Context, widget: GlanceAppWidget, clazz: Class<out GlanceAppWidget>) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(clazz).forEach { widget.update(context, it) }
    }
}

object ShuilemeWidgetRefreshScheduler {
    const val ACTION_REFRESH = "com.sleepshift.action.WIDGET_REFRESH"
    private const val REFRESH_INTERVAL_MS = 15 * 60_000L

    /** 排一个 15 分钟周期的非精确闹钟（虚拟时间随周期更新） */
    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ShuilemeWidgetRefreshReceiver::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + REFRESH_INTERVAL_MS,
            REFRESH_INTERVAL_MS,
            pi,
        )
    }
}

class ShuilemeWidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> ShuilemeWidgetRefreshScheduler.schedule(context)
            ShuilemeWidgetRefreshScheduler.ACTION_REFRESH -> {
                ShuilemeWidgets.refreshAll(context)
                // SL-9.2：周期刷新睡眠胶囊（更新虚拟时间/月亮）
                CoroutineScope(Dispatchers.IO).launch { SleepCapsule.refresh(context) }
            }
        }
    }
}
