package com.taigashinohara.kikkake

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 大きな文字サイズ（fontScale 1.3）でも崩れないことの回帰検知。
// v2.2.4 で実機の大きな文字設定によりタイルの「テスト」が欠けた事故の再発防止が狙い。
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class LargeFontScreenshotTest {

    private fun shot(name: String, content: @Composable () -> Unit) {
        val dir = File("src/test/screenshots")
        dir.mkdirs()
        captureRoboImage(file = File(dir, name)) {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.3f)) {
                KikkakeTestTheme { content() }
            }
        }
    }

    @Test
    fun launch_feed_largeFont() {
        seedTestData(RuntimeEnvironment.getApplication(), onboardingDone = true, withMacros = true)
        shot("launch_feed_large.png") { KikkakeApp() }
    }

    @Test
    fun wizard_step3_batteryConstraint_largeFont() {
        shot("wizard_step3_large.png") { WizardStepForTest(3, TriggerType.TIME.name, "22:00", ConstraintType.BATTERY_ABOVE.name) }
    }
}
