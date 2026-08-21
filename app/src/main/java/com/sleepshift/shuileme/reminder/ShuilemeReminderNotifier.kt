package com.sleepshift.shuileme.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sleepshift.MainActivity
import com.sleepshift.R
import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.model.SleepResultReason

/**
 * 提醒通知器（SL-4）。
 * - 通道：睡前 DEFAULT / 熬夜 DEFAULT（不受静默窗口限制）/ 起床反馈 LOW（遵守静默）；
 * - 通知动作：「我要睡了 🌙」打开 App、「今晚放过我 🌙」触发 [ShuilemeReminderReceiver] 的 NIGHT_OFF。
 * 标准 Android Notification，不依赖 Root / Shizuku / Device Owner / 外部 SDK。
 */
object ShuilemeReminderNotifier {

    const val ACTION_NIGHT_OFF = "com.sleepshift.action.NIGHT_OFF"
    private const val CH_SLEEP = "shuileme_sleep"
    private const val CH_LATE = "shuileme_late"
    private const val CH_WAKE = "shuileme_wake"
    private const val ID_SLEEP = 2001
    private const val ID_LATE = 2002
    private const val ID_WAKE = 2003

    /** 今晚放过我的提示文案 */
    const val NIGHT_OFF_MESSAGE = "今晚月亮休息一下，明天继续养月亮"

    fun notifySleepReminder(context: Context, state: ShuilemeState) {
        ensureChannels(context)
        val variant = state.sleepCount
        val text = ReminderTemplate.pick(state.reminderProfile.personality, ReminderType.SLEEP, variant)
        show(
            context, CH_SLEEP, ID_SLEEP,
            title = "${state.reminderProfile.personality.emoji} ${state.reminderProfile.personality.displayName}",
            text = text,
            withNightOffAction = true,
        )
    }

    fun notifyLateReminder(context: Context, state: ShuilemeState) {
        ensureChannels(context)
        val variant = state.sleepCount + 1
        val text = ReminderTemplate.pick(state.reminderProfile.personality, ReminderType.LATE, variant)
        show(
            context, CH_LATE, ID_LATE,
            title = "${state.reminderProfile.personality.emoji} 熬夜警告",
            text = text,
            withNightOffAction = true,
        )
    }

    fun notifyWakeFeedback(context: Context, state: ShuilemeState) {
        ensureChannels(context)
        val result = state.moonProgress.lastSleepResult
        val p = state.reminderProfile.personality
        val text = when {
            result == null -> "☀️ 醒啦！月亮记住了"
            result.isQualified -> "睡得好！${formatDuration(result.durationMinutes)}，${p.emoji} 说月亮长大了"
            result.reason == SleepResultReason.TOO_SHORT -> "睡得太短了（${formatDuration(result.durationMinutes)}），今晚早点，月亮想长大"
            else -> "睡太晚啦，月亮没长大。今晚早点睡，把月亮找回来"
        }
        show(context, CH_WAKE, ID_WAKE, title = "☀️ 我醒啦", text = text, withNightOffAction = false)
    }

    private fun show(
        context: Context,
        channelId: String,
        notifId: Int,
        title: String,
        text: String,
        withNightOffAction: Boolean,
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "我要睡了 🌙", openAppPendingIntent(context))
        if (withNightOffAction) {
            builder.addAction(0, "今晚放过我 🌙", nightOffPendingIntent(context))
        }
        runCatching { NotificationManagerCompat.from(context).notify(notifId, builder.build()) }
    }

    private fun openAppPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun nightOffPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 1,
            Intent(context, ShuilemeReminderReceiver::class.java).setAction(ACTION_NIGHT_OFF),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_SLEEP, "睡前提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "到该睡的时间提醒你"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_LATE, "熬夜提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "熬夜时提醒你（23:00-08:00 可触发，每晚最多 2 次）"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_WAKE, "起床反馈", NotificationManager.IMPORTANCE_LOW).apply {
                description = "醒来后的月亮反馈（遵守静默窗口）"
            }
        )
    }

    private fun formatDuration(totalMin: Long): String {
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h <= 0L -> "$m 分钟"
            m == 0L -> "$h 小时"
            else -> "$h 小时 $m 分钟"
        }
    }
}
