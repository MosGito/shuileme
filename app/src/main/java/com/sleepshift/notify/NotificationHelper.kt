package com.sleepshift.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** 非侵入式通知（低优先级、无声音震动）：睡眠模式开始 / 恢复 */
object NotificationHelper {

    private const val CHANNEL_ID = "sleep_mode"
    private const val NOTIFICATION_ID = 1001

    fun notifySleepModeStarted(context: Context) =
        notify(context, "睡眠模式已启动", "手机时间已提前，今晚安心入睡吧")

    fun notifyTimeRestored(context: Context) =
        notify(context, "正常时间已恢复", "手机时间已恢复正常")

    private fun notify(context: Context, title: String, body: String) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return // 未授权则不打扰
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "睡眠模式", NotificationManager.IMPORTANCE_LOW).apply {
                description = "睡眠模式开始 / 恢复通知"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
