package com.taigashinohara.kikkake

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 実機なし検証（標準 2026-11-23）：本物のアプリ画面を JVM 上で組み立て、
// 起動直後の表示と画面遷移が壊れていないことを交付前に確かめる。
// v2.2.4 の起動時クラッシュのような「開いた瞬間に落ちる」をここで捕まえる狙い。
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun setApp(onboardingDone: Boolean, withMacros: Boolean) {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        seedTestData(ctx, onboardingDone, withMacros)
        rule.setContent { KikkakeTestTheme { KikkakeApp() } }
    }

    @Test
    fun firstLaunch_showsOnboarding() {
        setApp(onboardingDone = false, withMacros = false)
        rule.onNodeWithText("きっかけを置くだけで、スマホが勝手に動いてくれる。").assertIsDisplayed()
    }

    @Test
    fun home_showsTilesWithActions() {
        setApp(onboardingDone = true, withMacros = true)
        rule.onNodeWithText("見すぎストッパー", substring = true).assertIsDisplayed()
        // タイルの操作ラベル（v2.2.5 で自動縮小に差し替えた所）が描かれていること
        rule.onAllNodesWithText("テスト").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("編集").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("削除").onFirst().assertIsDisplayed()
    }

    @Test
    fun wizard_reachesStep3WithConstraintChips() {
        setApp(onboardingDone = true, withMacros = false)
        rule.onNodeWithText("＋ つくる").performClick()
        rule.onNodeWithText("いつ動かす？").assertIsDisplayed()
        rule.onAllNodesWithText("時刻", substring = true).onFirst().performClick()
        rule.onNodeWithText("次へ").performClick()
        rule.onAllNodesWithText("通知を出す", substring = true).onFirst().performClick()
        rule.onNodeWithText("次へ").performClick()
        rule.onNodeWithText("名前と条件").performScrollTo().assertIsDisplayed()
        // 作り替えた画面の要：長い条件チップが並んでいること（縦崩れはスクショ側で見る）
        rule.onNodeWithText("電池が◯%以上のとき").performScrollTo().assertIsDisplayed()
    }
}
