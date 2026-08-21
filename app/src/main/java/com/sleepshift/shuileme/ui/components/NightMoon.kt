package com.sleepshift.shuileme.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepshift.shuileme.model.MoonRipple
import com.sleepshift.shuileme.ui.ShuilemeNight

/**
 * SL-9.8：月亮舞台布局 —— 主页与 Onboarding 共用。
 * 统一 moonOffsetX/Y/Size：月亮始终位于舞台中心（Alignment.Center），
 * 保证两个页面月亮坐标/大小完全一致。
 */
@Composable
fun MoonStageLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        content()
    }
}

/**
 * SL-9.6：共享月亮 —— 光晕 + 常驻呼吸波纹 + 月亮本体。
 *
 * 主页 MoonWithGlow 与引导最后一步「今夜，向月亮道个晚安」复用同一组件，
 * 保证初始月亮与主页月亮视觉完全一致（不创建第二个月亮）。
 *
 * - onMoonClick：短按回调（引导页用它播放波纹后进入主页）；
 * - moonTextModifier：主页注入长按手势（pointerInput）用；
 * - tapRippleTick：每次自增触发一次点击增强波纹。
 */
@Composable
fun NightMoon(
    emoji: String,
    modifier: Modifier = Modifier,
    glowAlpha: Float = 1f,
    glowScale: Float = 1f,
    tapRippleTick: Int = 0,
    onMoonClick: (() -> Unit)? = null,
    moonTextModifier: Modifier = Modifier,
) {
    val breathTransition = rememberInfiniteTransition(label = "moonBreath")
    val breathPhase by breathTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "breathPhase",
    )
    val ripple by animateFloatAsState(
        targetValue = if (tapRippleTick > 0) 1f else 0f,
        animationSpec = tween(900),
        label = "ripple",
    )

    Box(modifier, contentAlignment = Alignment.Center) {
        // 常驻呼吸波纹（SL-9.7：保留，稍加亮）
        Canvas(Modifier.size(180.dp)) {
            drawCircle(
                color = ShuilemeNight.Accent.copy(alpha = MoonRipple.alpha(breathPhase) * 1.6f),
                radius = size.minDimension / 2f * (0.4f + MoonRipple.radius(breathPhase) * 0.6f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        // 点击增强波纹（SL-9.7：更粗更亮更明显）
        if (ripple > 0.01f) {
            Canvas(Modifier.size(170.dp)) {
                drawCircle(
                    color = Color(0xFFFFE082).copy(alpha = (1f - ripple) * 0.7f),
                    radius = size.minDimension / 2f * (0.5f + ripple * 1.1f),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        // 淡黄月光
        Box(
            Modifier
                .size(190.dp * glowScale)
                .alpha(glowAlpha)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFE082), Color(0x00FFE082)),
                            center = center,
                            radius = size.minDimension / 2f,
                        ),
                        radius = size.minDimension / 2f,
                    )
                },
        )
        // 月亮本体
        BasicText(
            text = emoji,
            style = TextStyle(
                fontSize = 96.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            ),
            modifier = moonTextModifier
                .then(if (onMoonClick != null) Modifier.clickable { onMoonClick() } else Modifier),
        )
    }
}
