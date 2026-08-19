package com.sleepshift

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * 开机 / 应用更新后重新武装调度。
 *
 * 设备可能在偏移状态下重启（时区仍为 GMT±HH:MM）：
 * 先 [com.sleepshift.time.TimezoneScheduler.ensureActiveWindowRestore] 保证当前偏移窗口的恢复闹钟不被漏掉，
 * 再 [com.sleepshift.time.TimezoneScheduler.arm] 武装下一夜晚——不覆盖当前正确状态。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(HANDLE_TIMEOUT_MS) {
                    val scheduler = (context.applicationContext as SleepShiftApplication).timezoneScheduler
                    scheduler.ensureActiveWindowRestore()
                    scheduler.arm()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "boot re-arm failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val HANDLE_TIMEOUT_MS = 15_000L
    }
}
