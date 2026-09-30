package com.sleepshift.shuileme.model.personality

/**
 * V2.0.7 §11.2 Cold Start Model 的初步人格判定（纯函数，无 Android 依赖）。
 *
 * effectiveSessions < 5 时，正式 Matcher 不执行判定（§20.1 INPUT 门槛 n ≥ 5）。
 * 本函数仅供 Onboarding / SleepGoalEditor 等冷启动展示使用：基于用户自报的
 * 「当前真实作息」（currentSleepTime / currentWakeTime）在 12 个第一阶段
 * PersonalityDefinition 体系内输出**初步人格**，但不产生正式 Confidence /
 * Transition / Margin / Gate 状态（§11.2）。
 *
 * 数学口径（与正式路径共享唯一 MembershipMath，无第二套算法）：
 * - C = chronotypeScore(currentSleepTimeMin)：单一自报 onset；落在日间窗口
 *   [09:00, 18:00) → OutOfDomain → 无 C → 返回 null（不伪造 C，不选最近中心）；
 * - D = durationScore(自报睡眠窗口时长)，窗口 = 当前起床 − 当前入睡（跨午夜取正差）；
 * - R = undefined（单一样本无法估计规律性）→ SLOTH / OTTER 的 R 依赖分量不可用，
 *   按 §4.9 规则不置 0，直接排除；其余 Base 的 mR 恒 1.0；
 * - Base scores = MembershipMath.scoreAllBase(C, D, r = null)；
 * - MembershipMath.deterministicArgmax → top；S_top < MIN_MEMBERSHIP_SCORE → null。
 *
 * 不触发任何 Special Gate（CHAMELEON / WOLF）：Gate 依赖真实 daytimeOnset 统计
 * （§4.8 / §20.1），冷启动无真实样本，UNKNOWN 语义下不启用任何 Gate。
 */
object PersonalityColdStart {

    /**
     * 由自报当前作息输出初步人格（12 定义体系）。
     *
     * @return 初步 [PersonalityId]；C 不可用或 S_top < 0.25 时为 null
     *         （UI 语义：显示「月亮正在认识你」类占位，不展示任何正式人格）。
     */
    fun classify(currentSleepTimeMin: Int, currentWakeTimeMin: Int): PersonalityId? {
        val windowMin = if (currentWakeTimeMin > currentSleepTimeMin) {
            currentWakeTimeMin - currentSleepTimeMin
        } else {
            (PersonalityConstants.MINUTES_PER_DAY - currentSleepTimeMin) + currentWakeTimeMin
        }
        val c = chronotypeScore(currentSleepTimeMin)
        val scores = MembershipMath.scoreAllBase(c, durationScore(windowMin.toLong()), r = null)
        val top = MembershipMath.deterministicArgmax(scores) ?: return null
        return top.id.takeIf { top.score >= PersonalityConstants.MIN_MEMBERSHIP_SCORE }
    }
}
