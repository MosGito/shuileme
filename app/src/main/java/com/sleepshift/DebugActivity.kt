package com.sleepshift

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.sleepshift.engine.ShizukuTimeShiftEngine
import com.sleepshift.permission.CapabilityResolver
import com.sleepshift.ui.debug.DebugScreen
import com.sleepshift.ui.theme.SleepShiftTheme
import kotlinx.coroutines.launch

/**
 * 能力状态 + Shizuku 时区测试调试页（开发验证用；独立 Activity，不参与主流程）。
 * 启动：adb shell am start -n com.sleepshift/.DebugActivity
 */
class DebugActivity : ComponentActivity() {

    private val shizukuEngine by lazy { ShizukuTimeShiftEngine(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = CapabilityResolver(this).resolve()
        Log.i(
            TAG,
            "CAPABILITY deviceOwner=${state.deviceOwner} shizukuInstalled=${state.shizukuInstalled} " +
                "shizukuRunning=${state.shizukuRunning} shizukuGranted=${state.shizukuPermissionGranted} " +
                "root=${state.rootAvailable} activeEngine=${state.activeEngine}",
        )

        // 自检：未授权时 setTimeZone / setAutoTimeZone 应正确失败（便于 logcat 验证）
        lifecycleScope.launch {
            val r1 = shizukuEngine.setTimeZone("GMT+08:00")
            Log.i(TAG, "SELFTEST setTimeZone success=${r1.success} engine=${r1.engineType} msg=${r1.message}")
            val r2 = shizukuEngine.setAutoTimeZoneEnabled(false)
            Log.i(TAG, "SELFTEST setAutoTimeZone success=${r2.success} engine=${r2.engineType} msg=${r2.message}")
        }

        setContent {
            SleepShiftTheme {
                DebugScreen(
                    state = state,
                    onTestSetTimezone = { zone -> shizukuEngine.setTimeZone(zone) },
                    onRefreshSystemUi = { zone -> shizukuEngine.refreshSystemUiClock(zone) },
                    onDebugClockShift = { shizukuEngine.debugClockShiftTest() },
                    onTimezoneDiagnosis = { shizukuEngine.debugTimezoneDiagnosis() },
                )
            }
        }
    }

    companion object {
        private const val TAG = "DebugActivity"
    }
}
