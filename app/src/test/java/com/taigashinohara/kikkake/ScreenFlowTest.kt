package com.taigashinohara.kikkake

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
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
    fun firstLaunch_showsFeedWithTrialCard() {
        // v2.7.0：初回でも講義を挟まずフィード直行。先頭に試し置きカードが出る（C-2）
        setApp(onboardingDone = false, withMacros = false)
        rule.onNodeWithText("困りごとを1つ選んでね", substring = true).assertIsDisplayed()
        rule.onNodeWithText("電池の減りが気になる", substring = true).assertIsDisplayed()
    }

    @Test
    fun trialCard_pick_placesMacroAndShowsTryNow() {
        setApp(onboardingDone = false, withMacros = false)
        rule.onNodeWithText("電池の減りが気になる", substring = true).performClick()
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("あとで").fetchSemanticsNodes().isNotEmpty() ||
                MacroStore(ctx).load().any { it.name == "電池節約" }
        }
        if (rule.onAllNodesWithText("あとで").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("あとで").performClick()
        }
        // 着地したマイセットで、追加直後の主ボタン「今すぐ試す」が見えること（C-4）
        rule.onNodeWithText("今すぐ試す", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("電池節約").assertIsDisplayed()
    }

    @Test
    fun myset_showsTilesWithActions() {
        setApp(onboardingDone = true, withMacros = true)
        // v2.6.0：起動直後はフィード（ホーム）。フロートボタンからマイセットへ
        rule.onNodeWithText("マイセット", substring = true).performClick()
        rule.onNodeWithText("見すぎストッパー", substring = true).assertIsDisplayed()
        // タイルの操作ラベル（v2.2.5 で自動縮小に差し替えた所）が描かれていること
        rule.onAllNodesWithText("試す").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("編集").onFirst().assertIsDisplayed()
        // 削除はタイルに無い（v2.13.0で編集の中へ畳んだ）
        rule.onAllNodesWithText("削除").fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun wizard_delete_livesInEditStep3() {
        setApp(onboardingDone = true, withMacros = true)
        rule.onNodeWithText("マイセット", substring = true).performClick()
        // 1つ目のタイル（見すぎストッパー）の「編集」から入る
        rule.onAllNodesWithText("編集").onFirst().performClick()
        rule.onNodeWithText("いつ動かす？").assertIsDisplayed()
        rule.onNodeWithText("次へ").performClick()
        rule.onNodeWithText("次へ").performClick()
        // Step3に削除導線があり、押すとマイセットへ戻ってタイルが消える
        rule.onNodeWithText("この自動化を削除する").performScrollTo().performClick()
        rule.waitUntil(timeoutMillis = 5_000) { rule.onAllNodesWithText("マイセット").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("見すぎストッパー").fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun myset_golden() {
        setApp(onboardingDone = true, withMacros = true)
        rule.onNodeWithText("マイセット", substring = true).performClick()
        rule.onNodeWithText("見すぎストッパー", substring = true).assertIsDisplayed()
        rule.onRoot().captureRoboImage(File("src/test/screenshots/myset.png"))
    }

    @Test
    fun wizard_reachesStep3WithConstraintChips() {
        setApp(onboardingDone = true, withMacros = false)
        // 「＋ つくる」はマイセット側にある
        rule.onNodeWithText("マイセット", substring = true).performClick()
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

    @Test
    fun feedCard_expand_showsFlow() {
        // v2.8.0：仕組み（フローのチップ列）は畳まれていて、タップで開く
        setApp(onboardingDone = true, withMacros = false)
        rule.onNodeWithText("アプリを開いたら", substring = true).assertDoesNotExist()
        rule.onAllNodesWithText("仕組みを見る", substring = true)[0].performClick()
        rule.onNodeWithText("アプリを開いたら", substring = true).assertIsDisplayed()
    }

    @Test
    fun bookmark_saveAndReview() {
        // v2.8.0：カードのしおりマークで保存し、ブックマーク画面で見返せる（v2.13.3で絵文字から描画マークへ）
        setApp(onboardingDone = true, withMacros = false)
        rule.onAllNodesWithContentDescription("ブックマーク")[0].performClick()
        rule.onNodeWithText("ブックマーク", substring = true).performClick()
        rule.onNodeWithText("動画の見すぎストッパー").assertIsDisplayed()
        rule.onNodeWithText("1件").assertIsDisplayed()
    }

    @Test
    fun myset_customize_absorbsSettings() {
        // v2.12.0：設定関連はカスタマイズ画面へ集約。マイセットには置かない
        setApp(onboardingDone = true, withMacros = true)
        rule.onNodeWithText("マイセット", substring = true).performClick()
        rule.onNodeWithText("カスタマイズ").assertIsDisplayed()
        rule.onNodeWithText("その他の設定").assertDoesNotExist()
        rule.onNodeWithText("カスタマイズ").performClick()
        rule.onNodeWithText("AIに聞く の設定").assertIsDisplayed()
        rule.onNodeWithText("データの管理").assertIsDisplayed()
        rule.onNodeWithText("書き出し").assertIsDisplayed()
        rule.onNodeWithText("読み込み").assertIsDisplayed()
    }

    @Test
    fun myset_bookmarks_reachable() {
        // v2.12.0：ブックマークへマイセットからも行ける
        setApp(onboardingDone = true, withMacros = true)
        rule.onNodeWithText("マイセット", substring = true).performClick()
        rule.onNodeWithText("ブックマーク", substring = true).performClick()
        rule.onNodeWithText("まだブックマークがないよ", substring = true).assertIsDisplayed()
    }

    @Test
    fun feed_useTemplate_addsMacroToMySet() {
        setApp(onboardingDone = true, withMacros = true)
        // v2.6.0：起動直後がフィード（ホーム）そのもの
        rule.onNodeWithText("動画の見すぎストッパー").assertIsDisplayed()
        // フィード側も LazyColumn：画面外カードのボタンは bounds 0 で押せないため、先にカードを画面内へ
        // （v2.13.0で案内文の常時表示を廃止したため、項目 index＝テンプレ index。22時に静かに は 2）
        rule.onNode(hasScrollAction()).performScrollToIndex(2)
        // 先頭2件は投入済みマクロと同名なので「追加済み」。最初の「これを使う」＝22時に静かに
        rule.onAllNodesWithText("これを使う").onFirst().performClick()
        // 追加の保存か、許可ガイドの表示のどちらかが起きるまで待つ（ガイドは少し遅れて出ることがある）
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("あとで").fetchSemanticsNodes().isNotEmpty() ||
                MacroStore(ctx).load().any { it.name == "夜の静かモード" }
        }
        // 許可ガイドが出ている環境では閉じてから、ホームにタイルが出たことを見る
        if (rule.onAllNodesWithText("あとで").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("あとで").performClick()
        }
        // 部分一致だとスナックバーの文言（追加したよ！「夜の静かモード」）にも当たるため完全一致で探す
        rule.onNodeWithText("夜の静かモード").performScrollTo().assertIsDisplayed()
        // 追加直後のタイルには「今すぐ試す」が出ていること（C-4）
        rule.onNodeWithText("今すぐ試す", substring = true).performScrollTo().assertIsDisplayed()
    }
}
