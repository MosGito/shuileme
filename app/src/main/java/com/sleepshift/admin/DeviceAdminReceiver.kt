package com.sleepshift.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sleepshift.SleepShiftApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Device Owner / Device Admin 接收器。
 * onEnabled → 武装调度；onDisabled → 取消调度。
 *
 * 注意：测试环境不一定重新触发 onEnabled，因此提供手动测试入口
 * （AlarmReceiver 的 ACTION_TEST_ARM / ACTION_TEST_CANCEL）。
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "device admin enabled")
        scope.launch {
            val app = context.applicationContext as SleepShiftApplication
            app.timezoneScheduler.arm()
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "device admin disabled")
        scope.launch {
            val app = context.applicationContext as SleepShiftApplication
            app.timezoneScheduler.cancel()
        }
    }

    companion object {
        private const val TAG = "DeviceAdminReceiver"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
