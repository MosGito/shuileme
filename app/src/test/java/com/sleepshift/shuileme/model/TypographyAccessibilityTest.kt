package com.sleepshift.shuileme.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographyAccessibilityTest {

    @Test
    fun `字体层级 - 人格名大于主标题大于正文大于辅助`() {
        assertTrue(ShuilemeTypography.PERSONA_SP > ShuilemeTypography.TITLE_SP)
        assertTrue(ShuilemeTypography.TITLE_SP > ShuilemeTypography.BODY_SP)
        assertTrue(ShuilemeTypography.BODY_SP > ShuilemeTypography.CAPTION_SP)
        assertTrue("人格名应足够大（视觉焦点）", ShuilemeTypography.PERSONA_SP >= 36f)
        assertTrue("主标题 32-40sp（SL-9.7 放大）", ShuilemeTypography.TITLE_SP in 32f..40f)
    }

    @Test
    fun `深色背景浅色文字可读 深灰不可读`() {
        // 白/浅黄（TextPrimary 245,245,250 / Accent 255,224,130）
        assertTrue(ShuilemeTypography.isLightEnough(245, 245, 250))
        assertTrue(ShuilemeTypography.isLightEnough(255, 224, 130))
        // 深灰（低对比，禁止）
        assertFalse(ShuilemeTypography.isLightEnough(70, 70, 90))
        assertFalse(ShuilemeTypography.isLightEnough(0, 0, 0))
    }
}
