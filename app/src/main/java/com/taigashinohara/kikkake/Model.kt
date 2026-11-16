package com.taigashinohara.kikkake

enum class TriggerType(val label: String, val detail: String) {
    TIME("時刻", "決めた時刻になったら"),
    BATTERY_LOW("電池が少ない", "電池が少なくなったら"),
    WIFI_CONNECTED("Wi-Fi", "Wi-Fiにつながったら"),
    CHARGING_START("充電開始", "充電が始まったら"),
    APP_LAUNCH("アプリを開く", "選んだアプリを開いたら")
}

enum class ActionType(val label: String, val detail: String) {
    NOTIFICATION("通知を出す", "お知らせを表示する"),
    VOLUME_UP("音量を上げる", "メディア音量を上げる"),
    BRIGHTNESS_DOWN("明るさを下げる", "画面を暗くする"),
    SILENT("マナーモード", "着信音を消す"),
    LAUNCH_APP("アプリを開く", "選んだアプリを起動する"),
    FLASHLIGHT("ライト", "ライトをつけ／消す")
}

enum class ConstraintType(val label: String) {
    NONE("条件なし"), WEEKDAY("平日だけ"), NIGHT("夜だけ")
}

data class ActionItem(val type: ActionType, val param: String = "")

data class Macro(
    val id: Long,
    val name: String,
    val trigger: TriggerType,
    val triggerParam: String = "",
    val actions: List<ActionItem> = listOf(ActionItem(ActionType.NOTIFICATION)),
    val constraintType: ConstraintType = ConstraintType.NONE,
    val enabled: Boolean = true,
    val lastRunAt: Long = 0L
)

object Logic {
    fun autoName(trigger: TriggerType, actions: List<ActionItem>): String {
        if (actions.isEmpty()) return trigger.label
        val first = actions.first().type.label
        return if (actions.size == 1) "${trigger.label}で$first" else "${trigger.label}で$first など${actions.size}件"
    }

    fun summary(m: Macro): String {
        val t = when {
            m.trigger == TriggerType.APP_LAUNCH && m.triggerParam.isNotBlank() -> "${m.trigger.label}（${m.triggerParam.substringAfterLast('.')}）"
            m.triggerParam.isNotBlank() -> "${m.trigger.label}（${m.triggerParam}）"
            else -> m.trigger.label
        }
        val a = m.actions.joinToString("＋") { item ->
            if (item.type == ActionType.LAUNCH_APP && item.param.isNotBlank()) "${item.type.label}（${item.param.substringAfterLast('.')}）" else item.type.label
        }
        val c = if (m.constraintType != ConstraintType.NONE) " / ${m.constraintType.label}" else ""
        return "$t → $a$c"
    }

    fun constraintOk(type: ConstraintType, dayOfWeek: Int, hour: Int): Boolean = when (type) {
        ConstraintType.NONE -> true
        ConstraintType.WEEKDAY -> dayOfWeek in 2..6
        ConstraintType.NIGHT -> hour >= 21 || hour < 6
    }

    fun batteryCrossed(prev: Int, now: Int, threshold: Int): Boolean = prev > threshold && now <= threshold

    fun wifiConnectedEdge(prev: Boolean, now: Boolean): Boolean = !prev && now
}

data class Template(val title: String, val macroName: String, val trigger: TriggerType, val param: String, val actions: List<ActionItem>, val constraintType: ConstraintType = ConstraintType.NONE)

val Templates = listOf(
    Template("22時に静かに", "夜の静かモード", TriggerType.TIME, "22:00", listOf(ActionItem(ActionType.SILENT), ActionItem(ActionType.BRIGHTNESS_DOWN)), ConstraintType.NONE),
    Template("朝の準備", "朝のお知らせ", TriggerType.TIME, "07:00", listOf(ActionItem(ActionType.VOLUME_UP), ActionItem(ActionType.NOTIFICATION)), ConstraintType.WEEKDAY),
    Template("電池を節約", "電池節約", TriggerType.BATTERY_LOW, "20%", listOf(ActionItem(ActionType.BRIGHTNESS_DOWN), ActionItem(ActionType.NOTIFICATION)), ConstraintType.NONE),
    Template("充電お知らせ", "充電お知らせ", TriggerType.CHARGING_START, "", listOf(ActionItem(ActionType.NOTIFICATION)), ConstraintType.NONE)
)
