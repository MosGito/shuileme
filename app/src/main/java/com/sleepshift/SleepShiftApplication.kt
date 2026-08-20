package com.sleepshift

import android.app.Application
import com.sleepshift.data.SettingsRepository
import com.sleepshift.engine.EngineManager
import com.sleepshift.engine.DeviceOwnerTimeShiftEngine
import com.sleepshift.time.TimezoneScheduler

/**
 * 应用级单例容器。
 * Repository 作为唯一数据访问入口，由 Application 统一持有并注入 ViewModel，
 * 保证 UI 层不直接接触 DataStore。
 */
class SleepShiftApplication : Application() {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    /** 时区引擎管理器：Phase 11-A 仅含 Device Owner 引擎；M1/M2 追加 Shizuku/Root */
    val engineManager: EngineManager by lazy { EngineManager(this) }

    val timezoneScheduler: TimezoneScheduler by lazy {
        TimezoneScheduler(
            this,
            settingsRepository,
            engineManager.activeEngine ?: DeviceOwnerTimeShiftEngine(this),
        )
    }
}
