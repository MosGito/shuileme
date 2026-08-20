package com.sleepshift.engine

import android.content.Context

/**
 * 时区引擎管理器：持有全部候选引擎，按优先级选择当前可用引擎。
 *
 * Phase 11-A：仅注册 Device Owner 通道。
 * M1/M2：追加 Shizuku / Root 引擎，优先级 Shizuku > Root > Device Owner。
 */
class EngineManager(private val context: Context) {

    // Phase 11-B：Shizuku（优先） + Device Owner；Root 待 M2
    private val engines: List<TimeShiftEngine> =
        listOf(
            ShizukuTimeShiftEngine(context),
            DeviceOwnerTimeShiftEngine(context),
        )

    /** 当前可用引擎：按列表优先级返回第一个 [TimeShiftEngine.isAvailable] 为 true 的引擎；无则 null */
    val activeEngine: TimeShiftEngine?
        get() = engines.firstOrNull { it.isAvailable }

    /** 全部候选引擎（供状态展示 / 配置向导使用） */
    fun allEngines(): List<TimeShiftEngine> = engines
}
