package com.sleepshift.shuileme.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sleepshift.MainActivity
import com.sleepshift.R
import com.sleepshift.shuileme.data.ShuilemeRepository
import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.engine.VirtualClockEngine
import com.sleepshift.shuileme.widget.ShuilemeWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * SL-9.2：睡眠胶囊（SleepCapsule）。
 * App 不打开时，用持续通知展示睡眠状态（真实/虚拟时间 + 月亮成长）。
 * - 不用"灵动岛"命名；不申请悬浮窗权限；
 * - 夜空 + 月光黄 + emoji 简洁信息；
 * - 点击打开主页；「我醒啦 ☀️」动作直接醒来。
 */
object SleepCapsule {

    private const val CHANNEL_ID = "shuileme_capsule"
    private const val NOTIF_ID = 3001
    const val ACTION_WAKE = "com.sleepshift.action.CAPSULE_WAKE"

    /** 纯函数：胶囊内容（可单测） */
    fun formatContent(state: ShuilemeState, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        val real = fmt.format(Instant.ofEpochMilli(nowMs).atZone(zone))
        val engine = VirtualClockEngine(state.toVirtualClockConfig())
        val virtual = fmt.format(Instant.ofEpochMilli(engine.virtualTimeMs(nowMs)).atZone(zone))
        val moon = "${state.moonLife.stage.emoji} ${state.moonLife.stage.displayName}"
        return "真实 $real · 虚拟 $virtual · $moon"
    }

    /** 显示/更新睡眠胶囊（睡眠中）；非睡眠则取消 */
    fun show(context: Context, state: ShuilemeState) {
        if (!state.isSleeping) {
            cancel(context)
            return
        }
        ensureChannel(context)
        val text = formatContent(state, System.currentTimeMillis())
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🌙 睡眠胶囊")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setContentIntent(openApp(context))
            .addAction(0, "我醒啦 ☀️", wakeAction(context))
        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build()) }
    }

    fun cancel(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIF_ID) }
    }

    /** 周期刷新（组件刷新时调用）：读状态并显示/取消 */
    suspend fun refresh(context: Context) {
        val state = ShuilemeRepository(context).current()
        show(context, state)
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "睡眠胶囊", NotificationManager.IMPORTANCE_LOW).apply {
                description = "睡眠中常驻显示（无声，不打扰）"
            }
        )
    }

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun wakeAction(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, SleepCapsuleReceiver::class.java).setAction(ACTION_WAKE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

/** 胶囊「我醒啦 ☀️」动作接收器 */
class SleepCapsuleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SleepCapsule.ACTION_WAKE) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ShuilemeRepository(context)
                if (repo.current().isSleeping) repo.wakeUp(System.currentTimeMillis())
                SleepCapsule.cancel(context)
                ShuilemeWidgets.refreshAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
