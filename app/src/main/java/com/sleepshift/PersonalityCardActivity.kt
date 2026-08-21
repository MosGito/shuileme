package com.sleepshift

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sleepshift.shuileme.data.ShuilemeRepository
import com.sleepshift.shuileme.model.PersonalityCardGenerator
import com.sleepshift.shuileme.model.PersonalityCardModel
import com.sleepshift.shuileme.model.PersonalityInput
import com.sleepshift.shuileme.model.SleepPersonalityEngine
import com.sleepshift.shuileme.ui.PersonalityCardExporter
import com.sleepshift.shuileme.ui.PersonalityCardScreen
import com.sleepshift.ui.theme.SleepShiftTheme

/**
 * 人格分享卡片页（SL-6.5）。
 * 读取 DataStore → 计算人格 → 生成卡片 → 展示/分享。
 */
class PersonalityCardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = ShuilemeRepository(application)
        setContent {
            SleepShiftTheme {
                var model by remember { mutableStateOf<PersonalityCardModel?>(null) }
                LaunchedEffect(Unit) {
                    val state = repository.current()
                    val personality = SleepPersonalityEngine.compute(
                        PersonalityInput(
                            sessions = state.sessions,
                            moonLife = state.moonLife,
                            streakDays = state.streakDays,
                            sleepCount = state.sleepCount,
                        )
                    )
                    model = if (personality.primaryType != null) {
                        PersonalityCardGenerator.generate(state, personality)
                    } else {
                        // SL-9.3：初始人格倾向立即可查看（低置信度）
                        state.onboarding.initialPersonality?.let { initial ->
                            PersonalityCardGenerator.generateInitial(initial, state.targetSleepTimeMin, state.targetWakeTimeMin)
                        } ?: PersonalityCardGenerator.generate(state, personality)
                    }
                }
                val m = model
                if (m == null) {
                    Text("加载中…", modifier = Modifier.padding(24.dp))
                } else {
                    PersonalityCardScreen(
                        model = m,
                        onBack = { finish() },
                        onShare = { bitmap ->
                            val uri = PersonalityCardExporter.exportToFile(this, bitmap)
                            if (uri != null) {
                                startActivity(
                                    Intent.createChooser(
                                        PersonalityCardExporter.shareIntent(this, uri, m.title),
                                        "分享我的睡眠人格",
                                    )
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
