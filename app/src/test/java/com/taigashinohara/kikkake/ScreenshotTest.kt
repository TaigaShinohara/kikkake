package com.taigashinohara.kikkake

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 実機なし検証（標準 2026-11-23）：主要画面の見た目を JVM 上で描いてゴールデン画像と照合する。
// 記録: ./run-tests.sh record ／ 照合: ./run-tests.sh（既定）
// カバー範囲＝起動直後の画面（ホーム）／オンボーディング入口／作り替えた画面（ウィザード）。
// Paparazzi は Robolectric の android.os.Build と layoutlib の内包クラス差で落ちるため、
// Robolectric 上でそのまま撮れる Roborazzi を使う（QRいっき読み等と同方式）。
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class ScreenshotTest {

    private fun shot(name: String, content: @Composable () -> Unit) {
        val dir = File("src/test/screenshots")
        dir.mkdirs()
        captureRoboImage(file = File(dir, name)) {
            KikkakeTestTheme { content() }
        }
    }

    @Test
    fun home_withMacros() {
        seedTestData(RuntimeEnvironment.getApplication(), onboardingDone = true, withMacros = true)
        shot("home_with_macros.png") { KikkakeApp() }
    }

    @Test
    fun onboarding_firstPage() {
        shot("onboarding_first.png") { OnboardingScreen {} }
    }

    @Test
    fun wizard_step1_timeTrigger() {
        shot("wizard_step1.png") { WizardStepForTest(1, TriggerType.TIME.name, "22:00", ConstraintType.NONE.name) }
    }

    @Test
    fun wizard_step3_batteryConstraint() {
        shot("wizard_step3.png") { WizardStepForTest(3, TriggerType.TIME.name, "22:00", ConstraintType.BATTERY_ABOVE.name) }
    }

    @Test
    fun feed_firstCards() {
        val app = RuntimeEnvironment.getApplication()
        seedTestData(app, onboardingDone = true, withMacros = true)
        val macros = MacroStore(app).load()
        shot("feed_first.png") { FeedScreen(macros = macros, onUse = {}, onBack = {}) }
    }
}
