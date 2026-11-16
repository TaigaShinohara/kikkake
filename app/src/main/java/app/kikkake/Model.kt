package app.kikkake

enum class TriggerType(val label: String, val detail: String, val available: Boolean = true) {
    TIME("時刻", "決めた時刻になったら"),
    BATTERY_LOW("電池が少ない", "電池が少なくなったら"),
    WIFI_CONNECTED("Wi-Fi", "Wi-Fiにつながったら"),
    CHARGING_START("充電開始", "充電が始まったら"),
    APP_LAUNCH("アプリを開く", "準備中：次の版で対応予定", available = false)
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

data class Macro(
    val id: Long,
    val name: String,
    val trigger: TriggerType,
    val triggerParam: String = "",
    val action: ActionType,
    val actionParam: String = "",
    val constraintType: ConstraintType = ConstraintType.NONE,
    val enabled: Boolean = true,
    val lastRunAt: Long = 0L
)

object Logic {
    fun autoName(trigger: TriggerType, action: ActionType): String =
        "${trigger.label}で${action.label}"

    fun summary(m: Macro): String {
        val t = if (m.triggerParam.isNotBlank()) "${m.trigger.label}（${m.triggerParam}）" else m.trigger.label
        val a = if (m.action == ActionType.LAUNCH_APP && m.actionParam.isNotBlank()) "${m.action.label}（${m.actionParam.substringAfterLast('.')}）" else m.action.label
        val c = if (m.constraintType != ConstraintType.NONE) " / ${m.constraintType.label}" else ""
        return "$t → $a$c"
    }

    fun summary(trigger: TriggerType, action: ActionType, param: String = ""): String {
        val t = if (param.isNotBlank()) "${trigger.label}（$param）" else trigger.label
        return "$t → ${action.label}"
    }

    fun constraintOk(type: ConstraintType, dayOfWeek: Int, hour: Int): Boolean = when (type) {
        ConstraintType.NONE -> true
        ConstraintType.WEEKDAY -> dayOfWeek in 2..6
        ConstraintType.NIGHT -> hour >= 21 || hour < 6
    }

    /** 電池がしきい値を「下回った瞬間」だけ true（連発防止） */
    fun batteryCrossed(prev: Int, now: Int, threshold: Int): Boolean = prev > threshold && now <= threshold

    /** Wi-Fi が「つながった瞬間」だけ true */
    fun wifiConnectedEdge(prev: Boolean, now: Boolean): Boolean = !prev && now
}

data class Template(val title: String, val macroName: String, val trigger: TriggerType, val param: String, val action: ActionType, val constraintType: ConstraintType = ConstraintType.NONE)

val Templates = listOf(
    Template("22時に静かに", "夜の静かモード", TriggerType.TIME, "22:00", ActionType.SILENT, ConstraintType.NONE),
    Template("朝の準備", "朝のお知らせ", TriggerType.TIME, "07:00", ActionType.NOTIFICATION, ConstraintType.WEEKDAY),
    Template("電池を節約", "電池節約", TriggerType.BATTERY_LOW, "20%", ActionType.BRIGHTNESS_DOWN, ConstraintType.NONE),
    Template("充電お知らせ", "充電お知らせ", TriggerType.CHARGING_START, "", ActionType.NOTIFICATION, ConstraintType.NONE)
)
