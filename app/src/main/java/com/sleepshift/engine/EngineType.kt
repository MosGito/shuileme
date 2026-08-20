package com.sleepshift.engine

/** 时区修改引擎类型 */
enum class EngineType {
    /** 设备所有者（原 Device Owner 通道，逐步退场但保留能力） */
    DEVICE_OWNER,

    /** Shizuku 授权（主要开发目标，M1 引入） */
    SHIZUKU,

    /** Root shell（高级功能，M2 引入） */
    ROOT,

    /** 无可用引擎 */
    NONE,
}
