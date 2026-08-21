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

/** 人格气泡（SL-8 装饰，非角色/非宠物；SL-9.8.1 补 id/radius） */
data class Bubble(
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val emoji: String,
    val id: Int = 0,
    val radius: Float = 24f,
)

/**
 * 人格气泡物理（SL-9.6，纯函数可测）：失重漂浮。
 * - 无重力方向，不依赖传感器；
 * - 每个气泡以缓慢振荡速度漂移（elapsed 驱动，纯函数确定性）；
 * - 四壁碰撞弹性反射 + 气泡间相互推开；
 * - frozen 的（被拖动）气泡位置由用户控制。
 */
data class PersonaBubblePhysics(
    val width: Float = 400f,
    val height: Float = 800f,
    val bubbles: List<Bubble> = emptyList(),
    /** SL-9.1：月亮窗台高度（气泡不越过窗台） */
    val insetBottom: Float = 0f,
    /** SL-9.2.2：被拖动的气泡索引（不受漂移影响，位置由用户控制） */
    val frozen: Set<Int> = emptySet(),
    /** SL-9.6：内部时钟，驱动失重漂移（纯函数可测） */
    val elapsed: Float = 0f,
) {
    /** SL-9.6：失重漂浮单步（无重力；气泡缓慢漂移 + 四壁反弹 + 相互碰撞） */
    fun step(dt: Float, bounce: Float = 0.5f): PersonaBubblePhysics {
        val maxX = (width - BUBBLE_SIZE).coerceAtLeast(0f)
        val maxY = (height - BUBBLE_SIZE - insetBottom).coerceAtLeast(0f)
        val t = elapsed + dt
        val next = bubbles.mapIndexed { idx, b ->
            if (idx in frozen) {
                b.copy(vx = 0f, vy = 0f) // 拖动中：不漂移
            } else {
                // 失重漂浮：速度缓慢振荡（不同气泡不同相位，慢速 ~8dp/s）
                val p1 = idx * 1.3f + t * 0.22f
                val p2 = idx * 1.9f + t * 0.18f
                val driftX = kotlin.math.sin(p1) * 8f
                val driftY = kotlin.math.cos(p2) * 7f
                var vx = b.vx + (driftX - b.vx) * 0.06f
                var vy = b.vy + (driftY - b.vy) * 0.06f
                var x = b.x + vx * dt
                var y = b.y + vy * dt
                if (x > maxX) { x = maxX; vx = -vx * bounce }
                if (x < 0f) { x = 0f; vx = -vx * bounce }
                if (y > maxY) { y = maxY; vy = -vy * bounce }
                if (y < 0f) { y = 0f; vy = -vy * bounce }
                b.copy(x = x, y = y, vx = vx, vy = vy)
            }
        }
        return copy(bubbles = resolveCollisions(next, maxX, maxY), elapsed = t)
    }

    /** 气泡间碰撞：重叠则推开（SL-9.3） */
    private fun resolveCollisions(list: List<Bubble>, maxX: Float, maxY: Float): List<Bubble> {
        val result = list.toMutableList()
        val minDist = BUBBLE_SIZE * 1.2f
        for (i in result.indices) {
            for (j in i + 1 until result.size) {
                val a = result[i]
                val b = result[j]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < minDist && dist > 0.0001f) {
                    val overlap = (minDist - dist) / 2f
                    val nx = dx / dist
                    val ny = dy / dist
                    result[i] = a.copy(
                        x = (a.x - nx * overlap).coerceIn(0f, maxX),
                        y = (a.y - ny * overlap).coerceIn(0f, maxY),
                    )
                    result[j] = b.copy(
                        x = (b.x + nx * overlap).coerceIn(0f, maxX),
                        y = (b.y + ny * overlap).coerceIn(0f, maxY),
                    )
                }
            }
        }
        return result
    }

    /** SL-9.2.2：拖动/移动某气泡（可冻结），松手后带速度漂移 */
    fun moveBubble(
        index: Int,
        x: Float,
        y: Float,
        vx: Float = 0f,
        vy: Float = 0f,
        drag: Boolean = true,
    ): PersonaBubblePhysics {
        val list = bubbles.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(
                x = x.coerceIn(0f, (width - BUBBLE_SIZE).coerceAtLeast(0f)),
                y = y.coerceIn(0f, (height - BUBBLE_SIZE - insetBottom).coerceAtLeast(0f)),
                vx = vx,
                vy = vy,
            )
        }
        val newFrozen = if (drag) frozen + index else frozen - index
        return copy(bubbles = list, frozen = newFrozen)
    }

    companion object {
        /** 气泡显示尺寸（px 代理，用于边界） */
        const val BUBBLE_SIZE = 24f

        /** 确定性散布（seed 驱动，不出界） */
        fun scatter(emojis: List<String>, width: Float, height: Float, seed: Int): List<Bubble> {
            val maxX = (width - BUBBLE_SIZE).coerceAtLeast(0f)
            val maxY = (height - BUBBLE_SIZE).coerceAtLeast(0f)
            return emojis.mapIndexed { i, e ->
                val h = (i + 1) * 2654435761L + seed * 40503L + 1L
                val v = ((h ushr 32) xor h) and 0x7fffffffL
                Bubble(
                    id = i,
                    x = (v % (maxX + 1).toLong()).toFloat(),
                    y = ((v shr 3) % (maxY + 1).toLong()).toFloat(),
                    vx = (((v shr 7) % 20L).toFloat()) - 10f,
                    emoji = e,
                )
            }
        }
    }
}

/**
 * 月亮手势状态机（SL-9）：点击显示提示（TAPPED 由 UI 层状态）；长按蓄力 → 完成一圈触发。
 * IDLE → PRESSED → CHARGING（进度 0→1）→ COMPLETED（触发）；松开早 → 回 IDLE。
 */
enum class SleepGestureState { IDLE, PRESSED, CHARGING, COMPLETED }

data class SleepGestureTrigger(
    val state: SleepGestureState = SleepGestureState.IDLE,
    val chargeProgress: Float = 0f,
) {
    fun onPress(): SleepGestureTrigger = copy(state = SleepGestureState.PRESSED, chargeProgress = 0f)

    fun onLongPress(): SleepGestureTrigger = copy(state = SleepGestureState.CHARGING, chargeProgress = 0f)

    fun onCharge(progress: Float): SleepGestureTrigger =
        if (state == SleepGestureState.CHARGING) copy(chargeProgress = progress.coerceIn(0f, 1f)) else this

    fun onChargeComplete(): SleepGestureTrigger =
        if (state == SleepGestureState.CHARGING) copy(state = SleepGestureState.COMPLETED, chargeProgress = 1f) else this

    fun onCancel(): SleepGestureTrigger = copy(state = SleepGestureState.IDLE, chargeProgress = 0f)
}

/**
 * SL-9.2.2：月亮常驻呼吸波纹（低透明度、缓慢扩散，类似水面/夜灯光晕）。
 * 点击时叠加短促增强波纹。
 */
object MoonRipple {

    /** 呼吸相位 0..1（循环，phase=0 起点，phase=1 消散） */
    fun breathPhase(nowMs: Long, periodMs: Long = 4000L): Float {
        val p = (nowMs % periodMs) / periodMs.toFloat()
        return p.coerceIn(0f, 1f)
    }

    /** 波纹半径比例 0..1 */
    fun radius(phase: Float): Float = phase.coerceIn(0f, 1f)

    /** 波纹透明度（低）：起点较高 → 扩散渐隐 */
    fun alpha(phase: Float): Float = (1f - phase.coerceIn(0f, 1f)) * 0.15f

    /** 点击增强波纹（0..1 衰减） */
    fun clickBoost(elapsedMs: Long, durationMs: Long = 900L): Float {
        val p = (elapsedMs / durationMs.toFloat()).coerceIn(0f, 1f)
        return (1f - p).coerceIn(0f, 1f)
    }
}

/**
 * SL-9.2.2：字体层级（参考多邻国阅读体验，深色背景高对比）。
 * 人格名称（视觉焦点）> 主标题 > 正文 > 辅助。
 */
object ShuilemeTypography {
    const val TITLE_SP = 36f      // 你的睡眠人格是（SL-9.7 放大）
    const val PERSONA_SP = 40f    // 夜猫子型（视觉焦点）
    const val BODY_SP = 22f       // 正文（SL-9.7 放大）
    const val CAPTION_SP = 18f    // 辅助说明（SL-9.7 放大）

    /** 深色背景下文字是否足够亮（可读性检查） */
    fun isLightEnough(r: Int, g: Int, b: Int): Boolean {
        val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        return lum >= 0.45f
    }
}
