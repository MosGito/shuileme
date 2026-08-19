package com.sleepshift.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.sleepshift.model.SleepShiftMode
import com.sleepshift.model.SleepShiftSettings

/**
 * 设置 ViewModel。
 *
 * 阶段 2：内存状态（mutableStateOf），UI 状态模型与 [SleepShiftSettings] 完全兼容；
 * 阶段 3：切换为 SettingsRepository(DataStore) 驱动，保持本类方法签名不变。
 */
class SettingsViewModel : ViewModel() {

    var settings by mutableStateOf(SleepShiftSettings())
        private set

    fun updateSettings(transform: (SleepShiftSettings) -> SleepShiftSettings) {
        settings = transform(settings)
    }

    fun setEnabled(value: Boolean) = updateSettings { it.copy(enabled = value) }
    fun setStartTimeMin(value: Int) = updateSettings { it.copy(startTimeMin = value) }
    fun setRestoreTimeMin(value: Int) = updateSettings { it.copy(restoreTimeMin = value) }
    fun setOffsetMin(value: Int) = updateSettings { it.copy(offsetMin = value) }
    fun setMode(value: SleepShiftMode) = updateSettings { it.copy(mode = value) }
    fun setGradualStepMin(value: Int) = updateSettings { it.copy(gradualStepMin = value) }
    fun setFluctuationRangeMin(value: Int) = updateSettings { it.copy(fluctuationRangeMin = value) }
}
