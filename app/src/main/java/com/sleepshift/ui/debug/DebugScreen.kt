package com.sleepshift.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sleepshift.permission.CapabilityState

/** 能力状态调试页（开发验证用；不参与主流程） */
@Composable
fun DebugScreen(state: CapabilityState) {
    Column(Modifier.padding(24.dp)) {
        Text("SleepShift 能力状态 (Debug)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        CapabilityRow("Device Owner", state.deviceOwner)
        CapabilityRow("Shizuku 已安装", state.shizukuInstalled)
        CapabilityRow("Shizuku 运行中", state.shizukuRunning)
        CapabilityRow("Shizuku 已授权", state.shizukuPermissionGranted)
        CapabilityRow("Root 可用", state.rootAvailable)
        Spacer(Modifier.height(16.dp))
        Text("Active Engine: ${state.activeEngine}", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CapabilityRow(label: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            if (ok) "✔" else "✘",
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
