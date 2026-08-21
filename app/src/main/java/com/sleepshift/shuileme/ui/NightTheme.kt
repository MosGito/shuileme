package com.sleepshift.shuileme.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/**
 * SL-9.1：全局深色夜空配色（统一主界面 / 侦探 / 分享卡 / 设置）。
 * 深色夜空背景 + 星点元素 + 月光黄色强调 + 半透明卡片。
 */
object ShuilemeNight {
    /** 夜空背景 */
    val Sky = Color(0xFF14142B)
    /** 月光黄强调色 */
    val Accent = Color(0xFFFFE082)
    /** 半透明卡片 */
    val Card = Color(0x14FFFFFF)
    /** 半透明强卡片 */
    val CardStrong = Color(0x26FFFFFF)
    val TextPrimary = Color(0xFFF8F8FF)
    val TextSecondary = Color(0xFFB4B9CC)
    val TextTertiary = Color(0x99FFFFFF) // 更低优先级辅助文字
}

/** SL-9.7：统一文字颜色入口（所有 Text 默认走这里，禁 Material onBackground/黑字） */
object ShuilemeTextColors {
    /** 主文字：接近白色 */
    val Primary = Color(0xFFF8F8FF)
    /** 次文字：白色降 alpha */
    val Secondary = Color(0xFFB4B9CC)
    /** 强调/链接：月光黄 */
    val Accent = Color(0xFFFFE082)
}

/**
 * SL-9.6：夜光按钮 —— 暗琥珀容器 + 近白文字。
 * 禁止 Material 默认深色 onPrimary 文字；保留月光黄身份但降低容器亮度保证浅色文字可读。
 */
@Composable
fun NightButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = ShuilemeNight.Accent.copy(alpha = 0.28f),
            contentColor = ShuilemeNight.TextPrimary,
        ),
    ) { content() }
}

/** 星点数据 */
private data class StarData(val x: Float, val y: Float, val size: Float, val period: Long, val phase: Int)

/** 星点背景（SL-9.3：更多、更大、随机周期呼吸，保持夜晚氛围） */
@Composable
fun NightStarField(modifier: Modifier = Modifier, starCount: Int = 48, seed: Int = 0) {
    val stars = remember(seed) {
        List(starCount) { i ->
            StarData(
                x = ((i * 37 + seed * 13) % 100) / 100f,
                y = ((i * 53 + seed * 7) % 100) / 100f,
                size = 1.6f + (i % 4),                       // 2~5px，肉眼可见
                period = 1600L + (i * 37L) % 2200L,          // 1.6~3.8s 随机周期
                phase = (i * 53) % 360,
            )
        }
    }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { nowMs = System.currentTimeMillis(); delay(33) } }
    Canvas(modifier) {
        stars.forEach { s ->
            val t = ((nowMs % s.period) / s.period.toFloat()) * (2f * kotlin.math.PI.toFloat())
            val alpha = 0.3f + 0.4f * kotlin.math.abs(kotlin.math.sin(t + s.phase))
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = s.size,
                center = Offset(s.x * size.width, s.y * size.height),
            )
        }
    }
}

/** 深色夜空容器：夜空背景 + 星点 + 内容 */
@Composable
fun NightSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.background(ShuilemeNight.Sky)) {
        NightStarField(Modifier.fillMaxSize())
        content()
    }
}
