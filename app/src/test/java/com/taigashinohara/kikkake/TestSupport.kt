package com.taigashinohara.kikkake

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// 実機なし検証（標準 2026-11-23）用の共通部品。
// MainActivity のテーマ構成をそのまま再現し、テストから本物の画面を描く。

@Composable
fun KikkakeTestTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Palette.Pink, background = Palette.Paper, surface = Palette.CardWhite)) {
        Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
            Box(Modifier.fillMaxSize()) { AppBackground(); content() }
        }
    }
}

// 検証用の初期データ。オンボーディング済み＋見本マクロ2件などを仕込む
fun seedTestData(context: Context, onboardingDone: Boolean, withMacros: Boolean, dayFeed: Boolean = false) {
    context.getSharedPreferences("state", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("macros", Context.MODE_PRIVATE).edit().clear().commit()
    if (onboardingDone) StateStore(context).onboardingDone = true
    if (dayFeed) StateStore(context).dayFeedEnabled = true
    if (withMacros) {
        MacroStore(context).save(
            listOf(
                Macro(
                    1L, "見すぎストッパー", TriggerType.APP_LAUNCH, "com.google.android.youtube",
                    listOf(ActionItem(ActionType.WAIT, "5分"), ActionItem(ActionType.VIBRATE), ActionItem(ActionType.SPEAK, "そろそろ休憩しよ。目を休めてね"))
                ),
                Macro(
                    2L, "充電完了のお知らせ", TriggerType.BATTERY_RISES, "100%",
                    listOf(ActionItem(ActionType.RING), ActionItem(ActionType.SPEAK, "充電が終わったよ。ケーブルを抜いてね"))
                )
            )
        )
    }
}

// ウィザードを任意のステップ・状態で描くためのラッパー
@Composable
fun WizardStepForTest(step: Int, triggerName: String, param: String, constraintName: String) {
    WizardScreen(
        step = step,
        triggerName = triggerName,
        param = param,
        actionNames = setOf(ActionType.NOTIFICATION.name),
        launchParam = "",
        constraintName = constraintName,
        name = "テスト用",
        isEditing = false,
        onTrigger = {},
        onParam = {},
        onToggleAction = {},
        onLaunchParam = {},
        notifParam = "お知らせ",
        speakParam = "",
        onNotifParam = {},
        onSpeakParam = {},
        actionTexts = emptyMap(),
        onActionText = { _, _ -> },
        constraintParam = "80",
        onConstraintParam = {},
        onOpenPicker = {},
        onConstraint = {},
        onName = {},
        onNext = {},
        onBack = {},
        onSave = {}
    )
}
