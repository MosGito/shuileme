package com.sleepshift

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.sleepshift.shuileme.model.personality.PersonalityMatcher
import com.sleepshift.shuileme.model.personality.SleepBehaviorProfile
import com.sleepshift.shuileme.ui.PersonalityCardExporter
import com.sleepshift.shuileme.ui.PersonalityCardScreen
import com.sleepshift.shuileme.ui.PersonalityResultAdapter
import com.sleepshift.ui.theme.SleepShiftTheme

/**
 * 人格分享卡片页（SL-6.5）。
 * 读取 DataStore → V2.0.7 Personality Domain 判定 → 生成卡片 → 展示/分享。
 * PHASE 6（K-2 关闭）：不再独立调用旧 SleepPersonalityEngine.compute；
 * 判定唯一来自 PersonalityMatcher.classify（与 ViewModel 同一 Domain 入口）。
 */
class PersonalityCardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val repository = ShuilemeRepository(application)
        setContent {
            SleepShiftTheme {
                var model by remember { mutableStateOf<PersonalityCardModel?>(null) }
                LaunchedEffect(Unit) {
                    val state = repository.current()
                    val profile = SleepBehaviorProfile.from(state.sessions)
                    val result = PersonalityMatcher.classify(profile, state.targetSleepTimeMin)
                    model = if (result.isClassified) {
                        PersonalityCardGenerator.generate(
                            state,
                            PersonalityResultAdapter.legacyState(result, profile),
                        )
                    } else {
                        // SL-9.3：初始人格倾向立即可查看（低置信度）
                        state.onboarding.initialPersonality?.let { initial ->
                            // STEP 3：冷启动卡片基线 = 当前真实作息（与 HomeScreen 同一 initialPersonality 来源）
                            PersonalityCardGenerator.generateInitial(initial, state.currentSleepTimeMin, state.currentWakeTimeMin)
                        } ?: PersonalityCardGenerator.generate(
                            state,
                            PersonalityResultAdapter.legacyState(result, profile),
                        )
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
