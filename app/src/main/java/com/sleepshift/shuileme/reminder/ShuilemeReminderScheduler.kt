package com.sleepshift.shuileme.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sleepshift.shuileme.data.ShuilemeRepository
import com.sleepshift.shuileme.data.ShuilemeState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * 提醒调度器（SL-4，AlarmManager）。
 * - [scheduleAll]：睡前提醒（目标-30min 精确闹钟）+ 熬夜提醒首检查点（自续）；
 * - [scheduleLateCheckpoint]：熬夜检查点（23:00~07:00 每小时），由接收器处理后自续下一个。
 * 不依赖 Root/Shizuku/Device Owner。
 */
object ShuilemeReminderScheduler {

    const val ACTION_SLEEP_REMINDER = "com.sleepshift.action.SLEEP_REMINDER"
    const val ACTION_LATE_CHECKPOINT = "com.sleepshift.action.LATE_CHECKPOINT"
    private const val REQ_SLEEP = 201
    private const val REQ_LATE = 202

    /** 睡前提醒 + 熬夜首检查点（App 启动 / 提醒配置变更时调用） */
    suspend fun scheduleAll(context: Context) {
        val state = ShuilemeRepository(context).current()
        val profile = state.reminderProfile
        scheduleSleepReminder(context, state, profile)
        scheduleLateCheckpoint(context, profile)
    }

    private fun scheduleSleepReminder(context: Context, state: ShuilemeState, profile: ReminderProfile) {
        if (!profile.enabledSleepReminder) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val trigger = todayAtMs(state.targetSleepTimeMin - profile.sleepReminderAdvanceMin) ?: return
        if (trigger <= System.currentTimeMillis()) return
        val pi = PendingIntent.getBroadcast(
            context, REQ_SLEEP,
            Intent(context, ShuilemeReminderReceiver::class.java).setAction(ACTION_SLEEP_REMINDER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setExactSafe(am, trigger, pi)
    }

    /** 熬夜检查点：排下一个未来时刻（接收器处理后自续） */
    fun scheduleLateCheckpoint(context: Context, profile: ReminderProfile) {
        if (!profile.enabledLateReminder) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val next = profile.lateCheckpointHours.mapNotNull { todayAtMs(it) }.firstOrNull { it > now } ?: return
        val pi = PendingIntent.getBroadcast(
            context, REQ_LATE,
            Intent(context, ShuilemeReminderReceiver::class.java).setAction(ACTION_LATE_CHECKPOINT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setExactSafe(am, next, pi)
    }

    private fun setExactSafe(am: AlarmManager, trigger: Long, pi: PendingIntent) {
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } catch (e: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    /** 今天该分钟数（当日 00:00 起）对应的 epoch ms；越界返回 null */
    private fun todayAtMs(minOfDay: Int): Long? {
        if (minOfDay < 0 || minOfDay >= 24 * 60) return null
        return LocalDate.now()
            .atTime(LocalTime.of(minOfDay / 60, minOfDay % 60))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
