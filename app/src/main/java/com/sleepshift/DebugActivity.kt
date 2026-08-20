package com.sleepshift

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sleepshift.permission.CapabilityResolver
import com.sleepshift.ui.debug.DebugScreen
import com.sleepshift.ui.theme.SleepShiftTheme

/**
 * 能力状态调试页（开发验证用；独立 Activity，不参与主流程）。
 * 启动：adb shell am start -n com.sleepshift/.DebugActivity
 */
class DebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = CapabilityResolver(this).resolve()
        Log.i(
            TAG,
            "CAPABILITY deviceOwner=${state.deviceOwner} shizukuInstalled=${state.shizukuInstalled} " +
                "shizukuRunning=${state.shizukuRunning} shizukuGranted=${state.shizukuPermissionGranted} " +
                "root=${state.rootAvailable} activeEngine=${state.activeEngine}",
        )
        setContent {
            SleepShiftTheme {
                DebugScreen(state)
            }
        }
    }

    companion object {
        private const val TAG = "DebugActivity"
    }
}
