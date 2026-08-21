package com.sleepshift.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/** SL-9.8：全局深色夜空主题 —— 所有 on* 统一为浅色（统一 TextColor 入口，禁止 Material 黑字） */
private val NightColors = darkColorScheme(
    primary = Color(0xFFFFE082),      // 月光黄（强调/链接）
    onPrimary = Color(0xFFF8F8FF),    // 浅色（禁黑字；按钮由 NightButton 显式设深底浅字）
    background = Color(0xFF14142B),   // 夜空
    surface = Color(0xFF14142B),
    surfaceVariant = Color(0xFF241E3A),
    onSurface = Color(0xFFF8F8FF),    // 近白文字（统一入口）
    onSurfaceVariant = Color(0xFFB4B9CC), // 辅助文字（白色降 alpha）
    onBackground = Color(0xFFF8F8FF),
    onSecondary = Color(0xFFF8F8FF),
    onTertiary = Color(0xFFF8F8FF),
    onError = Color(0xFFF8F8FF),
)

/**
 * SL-9.8：统一 TextColor 入口。
 * 强制 LocalContentColor = 浅色 primary —— 所有未显式指定 color 的 Text 自动继承浅色，
 * 杜绝任何 Material 默认深色文字。primary #F8F8FF ≈ #FFFFFF。
 */
@Composable
fun SleepShiftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NightColors,
        content = {
            CompositionLocalProvider(LocalContentColor provides Color(0xFFF8F8FF)) {
                content()
            }
        },
    )
}
