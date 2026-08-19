package com.sleepshift

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sleepshift.admin.DeviceOwner
import com.sleepshift.time.TimezoneScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * 调度广播接收器。
 *
 * - [TimezoneScheduler.ACTION_SHIFT] / [TimezoneScheduler.ACTION_RESTORE]：由 Scheduler 的精确闹钟触发（也可 adb 手动触发）；
 * - [ACTION_TEST_ARM] / [ACTION_TEST_CANCEL]：Debug 手动测试入口（DeviceAdminReceiver 的 onEnabled 不保证测试环境触发，因此提供手动 arm/cancel）。
 *
 * **关键**：不依赖 PendingIntent 携带的 zone_id 作为最终配置（仅用于动作识别与调试日志），
 * 一律读取 DataStore 当前配置 / SchedulerState 决定执行逻辑，再调用 DevicePolicyManager.setTimeZone()。
 * 每次执行完成后重新 arm()，保证用户修改配置后下一周期生效。
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isScheduledAction = action == TimezoneScheduler.ACTION_SHIFT ||
            action == TimezoneScheduler.ACTION_RESTORE
        val isTestAction = action == ACTION_TEST_ARM ||
            action == ACTION_TEST_CANCEL ||
            action == ACTION_TEST_SET_ZONE ||
            action == ACTION_TEST_FORCE_ADVANCE ||
            action == ACTION_TEST_ACTIVE_RESTORE
        if (!isScheduledAction && !isTestAction) return
        if (isTestAction && !BuildConfig.DEBUG) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(HANDLE_TIMEOUT_MS) {
                    handle(context, action, intent)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "handle $action failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(context: Context, action: String, intent: Intent) {
        val scheduler = (context.applicationContext as SleepShiftApplication).timezoneScheduler
        Log.i(
            TAG,
            "receive action=$action extra_zone=${intent.getStringExtra(TimezoneScheduler.EXTRA_ZONE_ID)}"
        )
        when (action) {
            TimezoneScheduler.ACTION_SHIFT -> {
                scheduler.applyShift()
                scheduler.arm()
            }
            TimezoneScheduler.ACTION_RESTORE -> {
                scheduler.applyRestore()
                scheduler.arm()
            }
            ACTION_TEST_ARM -> scheduler.arm()
            ACTION_TEST_CANCEL -> scheduler.cancel()
            ACTION_TEST_FORCE_ADVANCE -> scheduler.forceAdvanceNight()
            ACTION_TEST_ACTIVE_RESTORE -> scheduler.ensureActiveWindowRestore()
            ACTION_TEST_SET_ZONE -> {
                val zone = intent.getStringExtra(TimezoneScheduler.EXTRA_ZONE_ID)
                if (zone != null) {
                    val ok = DeviceOwner.setTimeZone(context, zone)
                    Log.i(TAG, "TEST_SET_ZONE zone=$zone ok=$ok")
                }
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val HANDLE_TIMEOUT_MS = 15_000L

        const val ACTION_TEST_ARM = "com.sleepshift.action.TEST_ARM"
        const val ACTION_TEST_CANCEL = "com.sleepshift.action.TEST_CANCEL"
        const val ACTION_TEST_SET_ZONE = "com.sleepshift.action.TEST_SET_ZONE"
        const val ACTION_TEST_FORCE_ADVANCE = "com.sleepshift.action.TEST_FORCE_ADVANCE"
        const val ACTION_TEST_ACTIVE_RESTORE = "com.sleepshift.action.TEST_ACTIVE_RESTORE"
    }
}
