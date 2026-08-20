package com.sleepshift.engine

/** 时区操作结果（引擎无关，调度核心与 UI 统一消费） */
data class TimeShiftResult(
    val success: Boolean,
    val engineType: EngineType,
    val message: String = "",
) {
    companion object {
        fun ok(engineType: EngineType, message: String = "") =
            TimeShiftResult(success = true, engineType = engineType, message = message)

        fun fail(engineType: EngineType, message: String) =
            TimeShiftResult(success = false, engineType = engineType, message = message)
    }
}
