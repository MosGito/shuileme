package com.sleepshift.engine

/**
 * 时区修改引擎抽象。
 *
 * 每个实现对应一种权限通道（Device Owner / Shizuku / Root）。
 * 调度核心（TimezoneScheduler）只依赖本接口，不感知具体通道；
 * 后续新增引擎（Shizuku/Root）仅实现本接口并注册到 [EngineManager]。
 */
interface TimeShiftEngine {

    /** 引擎类型 */
    val type: EngineType

    /** 当前是否可用（权限通道已就绪） */
    val isAvailable: Boolean

    /** 设置系统时区；实现内部自行处理自动时区关闭（如需） */
    suspend fun setTimeZone(zoneId: String): TimeShiftResult

    /** 开关自动时区（AUTO_TIME_ZONE）；用于修改前关闭、恢复后按需开启 */
    suspend fun setAutoTimeZoneEnabled(enabled: Boolean): TimeShiftResult
}
