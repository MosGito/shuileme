package com.sleepshift.strategy

import com.sleepshift.model.MAX_OFFSET_MIN
import com.sleepshift.model.MIN_OFFSET_MIN
import com.sleepshift.model.OFFSET_STEP_MIN
import com.sleepshift.model.SchedulerState
import com.sleepshift.model.SleepShiftSettings
import kotlin.math.roundToInt
import kotlin.random.Random

/** 策略计算结果：今晚偏移 + 需要持久化的新状态 */
data class StrategyResult(val offsetMin: Int, val state: SchedulerState)

/** 偏移策略引擎：根据模式计算每晚偏移，并推进需要持久化的状态 */
interface OffsetStrategy {
    fun next(settings: SleepShiftSettings, state: SchedulerState): StrategyResult
}

/** 固定模式：每晚偏移 = 目标偏移（settings.offsetMin） */
object FixedStrategy : OffsetStrategy {
    override fun next(settings: SleepShiftSettings, state: SchedulerState): StrategyResult =
        StrategyResult(settings.offsetMin, state)
}

/** 渐进模式：第 N 晚偏移 = min(step * N, target)，逐日递增至目标后封顶保持 */
object GradualStrategy : OffsetStrategy {
    override fun next(settings: SleepShiftSettings, state: SchedulerState): StrategyResult {
        val day = state.gradualProgressDays + 1
        val offset = (day * settings.gradualStepMin).coerceAtMost(settings.offsetMin)
        return StrategyResult(offset, state.copy(gradualProgressDays = day))
    }
}

/**
 * 自然波动模式：目标值 ± 波动范围，候选值服从三角分布（峰值在目标值附近，非完全随机），
 * 步进取整到整小时（OFFSET_STEP_MIN=60）后，再限制与前一晚的变化幅度，避免剧烈跳变。
 */
class FluctuationStrategy(
    private val random: Random = Random.Default,
    private val maxDailyDeltaMin: Int = 60,
) : OffsetStrategy {
    override fun next(settings: SleepShiftSettings, state: SchedulerState): StrategyResult {
        val u1 = random.nextDouble()
        val u2 = random.nextDouble()
        // 三角分布：u1+u2-1 ∈ [-1,1)，峰值 0 → 候选集中分布于目标值附近
        var offset = settings.offsetMin + (settings.fluctuationRangeMin * (u1 + u2 - 1.0)).roundToInt()
        offset = roundToStep(offset)
        offset = offset.coerceIn(MIN_OFFSET_MIN, MAX_OFFSET_MIN)
        // 限制每日变化幅度
        val prev = state.fluctuationPrevOffsetMin
        offset = offset.coerceIn(prev - maxDailyDeltaMin, prev + maxDailyDeltaMin)
        offset = roundToStep(offset).coerceIn(MIN_OFFSET_MIN, MAX_OFFSET_MIN)
        return StrategyResult(offset, state.copy(fluctuationPrevOffsetMin = offset))
    }
}

private fun roundToStep(minutes: Int): Int = ((minutes + OFFSET_STEP_HALF) / OFFSET_STEP_MIN) * OFFSET_STEP_MIN

private const val OFFSET_STEP_HALF = OFFSET_STEP_MIN / 2
