package com.sleepshift.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** SL-9.3：全局深色夜空主题（禁白底黑字；月光黄强调） */
private val NightColors = darkColorScheme(
    primary = Color(0xFFFFE082),      // 月光黄
    onPrimary = Color(0xFF14142B),
    background = Color(0xFF14142B),   // 夜空
    surface = Color(0xFF14142B),
    surfaceVariant = Color(0xFF241E3A),
    onSurface = Color(0xFFF5F5FA),    // 浅色文字
    onSurfaceVariant = Color(0xFF9AA0B5),
    onBackground = Color(0xFFF5F5FA),
)

@Composable
fun SleepShiftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NightColors,
        content = content,
    )
}
