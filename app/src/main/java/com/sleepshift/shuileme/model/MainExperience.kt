package com.sleepshift.shuileme.model

/** 首页布局区域（SL-8：仅 5 区，无统计/开发信息） */
enum class HomeSection { PERSONALITY, MOON, VIRTUAL_TIME, BUBBLES, SETTINGS }

/** 首页布局状态 */
data class HomeLayoutState(val visibleSections: Set<HomeSection> = HomeSection.entries.toSet()) {
    companion object {
        val EXPECTED_SECTIONS = HomeSection.entries.toSet()
    }
}

/**
 * 月亮光效（SL-8）：根据 MoonLife 状态输出光晕强度。
 * 健康 → 光晕增强；普通 → 正常柔光；低状态 → 光晕减弱。
 */
object MoonGlow {
    fun glowIntensity(moonLife: MoonLife): Float = when {
        moonLife.mood == MoonMood.CELEBRATING || moonLife.stage == MoonStage.FULL -> 1f
        moonLife.mood == MoonMood.GENTLE -> 0.7f
        moonLife.mood == MoonMood.DISAPPOINTED -> 0.3f
        else -> 0.5f
    }

    fun glowAlpha(intensity: Float): Float = 0.15f + intensity.coerceIn(0f, 1f) * 0.45f

    fun glowScale(intensity: Float): Float = 1f + intensity.coerceIn(0f, 1f) * 0.25f

    fun glowBlur(intensity: Float): Float = 20f + intensity.coerceIn(0f, 1f) * 24f
}

/** 人格气泡（SL-8 装饰，非角色/非宠物） */
data class Bubble(val x: Float, val y: Float, val vx: Float = 0f, val vy: Float = 0f, val emoji: String)

/**
 * 人格气泡物理（SL-8，纯函数可测）：重力下落 + 底部碰撞弹跳 + 速度。
 * 传感器倾斜（SensorManager）为增强项，首版用固定重力动画循环。
 */
data class PersonaBubblePhysics(
    val width: Float = 400f,
    val height: Float = 800f,
    val bubbles: List<Bubble> = emptyList(),
) {
    fun step(gravityY: Float, dt: Float, bounce: Float = 0.5f): PersonaBubblePhysics {
        val next = bubbles.map { b ->
            val ny = b.vy + gravityY * dt
            var y = b.y + ny * dt
            var vy = ny
            if (y > height) { y = height; vy = -vy * bounce }
            if (y < 0f) { y = 0f; vy = -vy * bounce }
            var x = b.x + b.vx * dt
            if (x > width) x = width
            if (x < 0f) x = 0f
            b.copy(x = x, y = y, vy = vy)
        }
        return copy(bubbles = next)
    }

    companion object {
        /** 确定性散布（seed 驱动） */
        fun scatter(emojis: List<String>, width: Float, height: Float, seed: Int): List<Bubble> =
            emojis.mapIndexed { i, e ->
                val h = (i + 1) * 2654435761L + seed * 40503L + 1L
                val v = ((h ushr 32) xor h) and 0x7fffffffL
                Bubble(
                    x = (v % width.toLong()).toFloat(),
                    y = ((v shr 3) % height.toLong()).toFloat(),
                    vx = (((v shr 7) % 20L).toFloat()) - 10f,
                    emoji = e,
                )
            }
    }
}

/** 月亮长按手势状态机（SL-8，防误触） */
enum class SleepGestureState { IDLE, PRESSED, READY, CONFIRMED }

data class SleepGestureTrigger(
    val state: SleepGestureState = SleepGestureState.IDLE,
    val pressedAt: Long = 0L,
) {
    fun onDown(now: Long): SleepGestureTrigger = copy(state = SleepGestureState.PRESSED, pressedAt = now)

    fun onTick(now: Long, longPressThreshold: Long = 500L): SleepGestureTrigger =
        if (state == SleepGestureState.PRESSED && now - pressedAt >= longPressThreshold)
            copy(state = SleepGestureState.READY)
        else this

    fun onConfirm(): SleepGestureTrigger =
        if (state == SleepGestureState.READY) copy(state = SleepGestureState.CONFIRMED) else this

    fun onCancel(): SleepGestureTrigger = copy(state = SleepGestureState.IDLE, pressedAt = 0L)
}
