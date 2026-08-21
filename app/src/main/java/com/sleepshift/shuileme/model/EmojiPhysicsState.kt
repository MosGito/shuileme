package com.sleepshift.shuileme.model

/**
 * Emoji 物理状态（SL-6 首页动画接口）。
 *
 * 暂不实现完整物理引擎；为未来预留：
 * - 手机重力感应 → [velocityX]/[velocityY]；
 * - emoji 滚动 → [rotation]；
 * - emoji 碰撞 → position 修正。
 */
data class EmojiPhysicsState(
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
) {
    companion object {
        val IDLE = EmojiPhysicsState()
    }
}
