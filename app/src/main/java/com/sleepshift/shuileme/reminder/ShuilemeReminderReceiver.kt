package com.sleepshift.shuileme.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.sleepshift.shuileme.data.ShuilemeRepository
import com.sleepshift.shuileme.data.ShuilemeState
import com.sleepshift.shuileme.engine.VirtualClockEngine
import com.sleepshift.shuileme.widget.ShuilemeWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 提醒接收器（SL-4）。
 * - [ShuilemeReminderScheduler.ACTION_SLEEP_REMINDER]：睡前提醒；
 * - [ShuilemeReminderScheduler.ACTION_LATE_CHECKPOINT]：熬夜提醒检查点（自续下一个）；
 * - [ShuilemeReminderNotifier.ACTION_NIGHT_OFF]：今晚放过我 🌙。
 * 所有判定读取 DataStore；不依赖 Root/Shizuku/Device Owner。
 */
class ShuilemeReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ShuilemeReminderScheduler.ACTION_SLEEP_REMINDER -> handleSleepReminder(context)
                    ShuilemeReminderScheduler.ACTION_LATE_CHECKPOINT -> handleLateCheckpoint(context)
                    ShuilemeReminderNotifier.ACTION_NIGHT_OFF -> handleNightOff(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleSleepReminder(context: Context) {
        val repo = ShuilemeRepository(context)
        val state = repo.current()
        val now = System.currentTimeMillis()
        val profile = state.reminderProfile
        if (!profile.enabledSleepReminder) return
        if (isNightOff(state, now)) return
        if (state.isSleeping) return
        if (isInQuietWindow(now, profile)) return
        ShuilemeReminderNotifier.notifySleepReminder(context, state)
    }

    private suspend fun handleLateCheckpoint(context: Context) {
        val repo = ShuilemeRepository(context)
        val state = repo.current()
        val now = System.currentTimeMillis()
        val profile = state.reminderProfile
        if (!profile.enabledLateReminder) return
        if (isNightOff(state, now)) return
        if (state.isSleeping) return
        // 每晚最多 N 次
        if (state.lateReminderCount >= profile.lateReminderMaxPerNight) return
        // 熬夜线：真实过目标 或 虚拟过午夜
        if (!isLateNow(state, now)) return
        ShuilemeReminderNotifier.notifyLateReminder(context, state)
        repo.incrementLateReminder(todayEpochDay(now))
        // 自续下一个检查点
        ShuilemeReminderScheduler.scheduleLateCheckpoint(context, profile)
    }

    private suspend fun handleNightOff(context: Context) {
        val repo = ShuilemeRepository(context)
        repo.setNightOff(true, todayEpochDay(System.currentTimeMillis()))
        Toast.makeText(context, ShuilemeReminderNotifier.NIGHT_OFF_MESSAGE, Toast.LENGTH_SHORT).show()
        ShuilemeWidgets.refreshAll(context)
    }

    // ── 判定辅助 ──

    private fun isNightOff(state: ShuilemeState, nowMs: Long): Boolean {
        val today = todayEpochDay(nowMs)
        return state.nightOffActive && state.nightOffDate == today
    }

    private fun isInQuietWindow(nowMs: Long, profile: ReminderProfile): Boolean {
        val h = Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault()).hour
        return h >= profile.quietStartHour || h < profile.quietEndHour
    }

    private fun isLateNow(state: ShuilemeState, nowMs: Long): Boolean {
        val realMin = Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault()).hour * 60 +
            Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault()).minute
        val realPastTarget = realMin >= state.targetSleepTimeMin
        val virtual = VirtualClockEngine(state.toVirtualClockConfig()).virtualTimeMs(nowMs)
        val virtualHour = Instant.ofEpochMilli(virtual).atZone(ZoneId.systemDefault()).hour
        val virtualLate = virtualHour in 0..7
        return realPastTarget || virtualLate
    }

    private fun todayEpochDay(nowMs: Long): Long =
        LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), ZoneId.systemDefault()).toEpochDay()
}
