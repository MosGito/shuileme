package com.sleepshift.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sleepshift.model.DEFAULT_FLUCTUATION_RANGE_MIN
import com.sleepshift.model.DEFAULT_GRADUAL_STEP_MIN
import com.sleepshift.model.DEFAULT_OFFSET_MIN
import com.sleepshift.model.DEFAULT_RESTORE_TIME_MIN
import com.sleepshift.model.DEFAULT_START_TIME_MIN
import com.sleepshift.model.SchedulerState
import com.sleepshift.model.SleepShiftMode
import com.sleepshift.model.SleepShiftSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sleepshift_settings")

/** 当前配置结构版本；未来升级结构时递增，并在 migrate() 中补充逐版本迁移逻辑 */
private const val CURRENT_SETTINGS_VERSION = 1

/**
 * SleepShift 配置仓库（Preferences DataStore）——数据访问唯一入口。
 *
 * 分层约定：
 * - 用户配置 [SleepShiftSettings] 与内部运行状态 [SchedulerState] 分开存储，不混用；
 * - `originalTimezoneId` 具有写保护：仅在首次写入，之后不可覆盖；
 * - 所有写入自动维护 `settingsVersion`，为未来配置结构升级/迁移预留通道。
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val START_TIME_MIN = intPreferencesKey("start_time_min")
        val RESTORE_TIME_MIN = intPreferencesKey("restore_time_min")
        val OFFSET_MIN = intPreferencesKey("offset_min")
        val MODE = intPreferencesKey("mode")
        val GRADUAL_STEP_MIN = intPreferencesKey("gradual_step_min")
        val FLUCTUATION_RANGE_MIN = intPreferencesKey("fluctuation_range_min")
        val SETTINGS_VERSION = intPreferencesKey("settings_version")
        // 内部运行时状态（仅 Scheduler 写入）
        val GRADUAL_PROGRESS_DAYS = intPreferencesKey("gradual_progress_days")
        val FLUCTUATION_PREV_OFFSET_MIN = intPreferencesKey("fluctuation_prev_offset_min")
        val CURRENT_OFFSET_MIN = intPreferencesKey("current_offset_min")
        val ORIGINAL_TIMEZONE_ID = stringPreferencesKey("original_timezone_id")
        val ARMED_EPOCH_DAY = longPreferencesKey("armed_epoch_day")
    }

    /** 用户配置流 */
    val settings: Flow<SleepShiftSettings> = dataStore.data.map { it.toSettings() }

    /** 内部运行时状态流（UI 只读展示） */
    val schedulerState: Flow<SchedulerState> = dataStore.data.map { it.toSchedulerState() }

    /** 配置结构版本流 */
    val settingsVersion: Flow<Int> = dataStore.data.map { it[Keys.SETTINGS_VERSION] ?: 0 }

    suspend fun updateSettings(transform: (SleepShiftSettings) -> SleepShiftSettings) {
        dataStore.edit { prefs ->
            prefs.migrate()
            val next = transform(prefs.toSettings())
            prefs[Keys.ENABLED] = next.enabled
            prefs[Keys.START_TIME_MIN] = next.startTimeMin
            prefs[Keys.RESTORE_TIME_MIN] = next.restoreTimeMin
            prefs[Keys.OFFSET_MIN] = next.offsetMin
            prefs[Keys.MODE] = next.mode.ordinal
            prefs[Keys.GRADUAL_STEP_MIN] = next.gradualStepMin
            prefs[Keys.FLUCTUATION_RANGE_MIN] = next.fluctuationRangeMin
        }
    }

    suspend fun updateSchedulerState(transform: (SchedulerState) -> SchedulerState) {
        dataStore.edit { prefs ->
            prefs.migrate()
            val current = prefs.toSchedulerState()
            val next = transform(current)
            prefs[Keys.GRADUAL_PROGRESS_DAYS] = next.gradualProgressDays
            prefs[Keys.FLUCTUATION_PREV_OFFSET_MIN] = next.fluctuationPrevOffsetMin
            prefs[Keys.CURRENT_OFFSET_MIN] = next.currentOffsetMin
            prefs[Keys.ARMED_EPOCH_DAY] = next.armedEpochDay
            // 原始时区写保护：仅在首次写入
            if (next.originalTimezoneId.isNotEmpty() && current.originalTimezoneId.isEmpty()) {
                prefs[Keys.ORIGINAL_TIMEZONE_ID] = next.originalTimezoneId
            }
        }
    }

    /**
     * 配置版本迁移。未来结构升级时：读取旧版本 → 逐版本迁移 → 写入新版本。
     * 当前版本 1（初始结构），无历史迁移。
     */
    private fun MutablePreferences.migrate() {
        var version = this[Keys.SETTINGS_VERSION] ?: 0
        while (version < CURRENT_SETTINGS_VERSION) {
            when (version) {
                // v0 -> v1：初始结构，无需迁移
            }
            version++
        }
        this[Keys.SETTINGS_VERSION] = CURRENT_SETTINGS_VERSION
    }

    private fun Preferences.toSettings() = SleepShiftSettings(
        enabled = this[Keys.ENABLED] ?: false,
        startTimeMin = this[Keys.START_TIME_MIN] ?: DEFAULT_START_TIME_MIN,
        restoreTimeMin = this[Keys.RESTORE_TIME_MIN] ?: DEFAULT_RESTORE_TIME_MIN,
        offsetMin = this[Keys.OFFSET_MIN] ?: DEFAULT_OFFSET_MIN,
        mode = SleepShiftMode.entries.getOrElse(this[Keys.MODE] ?: SleepShiftMode.FIXED.ordinal) { SleepShiftMode.FIXED },
        gradualStepMin = this[Keys.GRADUAL_STEP_MIN] ?: DEFAULT_GRADUAL_STEP_MIN,
        fluctuationRangeMin = this[Keys.FLUCTUATION_RANGE_MIN] ?: DEFAULT_FLUCTUATION_RANGE_MIN,
    )

    private fun Preferences.toSchedulerState() = SchedulerState(
        gradualProgressDays = this[Keys.GRADUAL_PROGRESS_DAYS] ?: 0,
        fluctuationPrevOffsetMin = this[Keys.FLUCTUATION_PREV_OFFSET_MIN] ?: DEFAULT_OFFSET_MIN,
        currentOffsetMin = this[Keys.CURRENT_OFFSET_MIN] ?: 0,
        originalTimezoneId = this[Keys.ORIGINAL_TIMEZONE_ID] ?: "",
        armedEpochDay = this[Keys.ARMED_EPOCH_DAY] ?: -1L,
    )
}
