package com.taigashinohara.kikkake

enum class TriggerType(val icon: String, val label: String, val detail: String, val group: String) {
    TIME("🕐", "時刻", "", "時刻"),
    SUN_EVENT("🌅", "日の出・日の入り", "太陽に合わせて", "時刻"),
    LOCATION("📍", "場所", "着いたら／離れたら", "場所"),
    WIFI_CONNECTED("📶", "Wi-Fi", "家に着いたときなど", "つながり"),
    BLUETOOTH("🔷", "Bluetooth", "機器がつながったら", "つながり"),
    NFC("🏷️", "NFCタグ", "アプリを開いてタグをかざす", "つながり"),
    HEADSET("🎧", "ヘッドホン", "有線のみ", "つながり"),
    CHARGING_START("🔌", "充電開始", "", "充電・電池"),
    CHARGING_DONE("✅", "ケーブルを抜いたら", "充電中でも、抜けば動く", "充電・電池"),
    BATTERY_LOW("🔋", "電池が少ない", "", "充電・電池"),
    BATTERY_RISES("⚡", "電池が増えたら", "充電が進んだら", "充電・電池"),
    APP_LAUNCH("📱", "アプリを開いたら", "", "アプリ"),
    APP_CLOSED("📴", "アプリを閉じたら", "", "アプリ"),
    NOTIFICATION_POSTED("✉️", "通知が来たら", "選んだアプリの通知で", "連絡"),
    DND_STATE("🔕", "集中モード", "ON／OFFしたら", "端末の状態"),
    POWER_SAVE("🍃", "省電力モード", "ON／OFFしたら", "端末の状態"),
    AIRPLANE("✈️", "機内モード", "ON／OFFしたら", "端末の状態")
}

val TriggerGroups = listOf("時刻", "場所", "つながり", "充電・電池", "アプリ", "連絡", "端末の状態")

enum class ActionType(val icon: String, val label: String, val detail: String, val group: String) {
    NOTIFICATION("💬", "通知を出す", "好きな文でお知らせ", "知らせる"),
    SPEAK("🗣️", "声で知らせる", "好きな文を声で", "知らせる"),
    RING("📣", "音を鳴らす", "通知音", "知らせる"),
    VIBRATE("📳", "バイブする", "", "知らせる"),
    WIDGET_SET("🧩", "ウィジェットに表示", "ホーム画面の文字を変える", "知らせる"),
    AI_ASK("🤖", "AIに聞く", "答えを通知で知らせる", "AI"),
    MEDIA_PLAY_PAUSE("🎵", "音楽を再生／停止", "", "音"),
    MUSIC_PLAY("▶️", "音楽を再生", "止まっていたら再生する", "音"),
    MEDIA_NEXT("⏭️", "次の曲へ", "", "音"),
    VOLUME_UP("🔊", "音量を上げる", "メディアの音量", "音"),
    VOLUME_DOWN("🔉", "音量を下げる", "メディアの音量", "音"),
    SILENT("🔕", "マナーモード", "着信音を消す", "音"),
    SILENT_OFF("🔔", "マナーモードを解除", "着信音を元に戻す", "音"),
    DND_ON("😴", "おやすみモード", "通知をぜんぶ止める", "音"),
    DND_OFF("🌅", "おやすみモードを解除", "止めた通知を元に戻す", "音"),
    BRIGHTNESS_DOWN("🌙", "明るさを下げる", "", "画面・ライト"),
    BRIGHTNESS_UP("☀️", "明るさを戻す", "", "画面・ライト"),
    FLASHLIGHT("🔦", "ライト", "", "画面・ライト"),
    ROTATE_OFF("📐", "画面回転を固定", "自動回転をオフ", "設定を変える"),
    ROTATE_ON("🔄", "画面回転を自動に", "", "設定を変える"),
    SCREEN_KEEP("💡", "画面を消さない", "自動消灯オフ／戻す（切替）", "設定を変える"),
    WALLPAPER("🖼️", "壁紙を変える", "単色から選ぶ", "設定を変える"),
    WIFI_PANEL("🌐", "Wi-Fi設定を開く", "", "設定を変える"),
    BT_SETTINGS("🔷", "Bluetooth設定を開く", "", "設定を変える"),
    OPEN_URL("🔗", "URLを開く", "", "送る・開く"),
    VIDEO_PLAY("🎬", "動画を再生", "リンク先の動画を開く", "送る・開く"),
    PLAYLIST_PLAY("🎶", "プレイリストを再生", "リンク先を開く", "送る・開く"),
    CAMERA_OPEN("📷", "カメラを開く", "撮影はシャッターを押してね", "送る・開く"),
    SHARE_TEXT("📤", "テキストを共有", "", "送る・開く"),
    SMS_COMPOSE("📩", "メッセージ作成を開く", "", "送る・開く"),
    CALENDAR_ADD("📅", "予定を追加", "今から1時間", "記録する"),
    MEMO_SAVE("📝", "メモを保存", "ファイルに書き足す", "記録する"),
    CLIPBOARD_COPY("📋", "コピーする", "クリップボードへ", "記録する"),
    LOCATION_NOTIFY("📍", "現在地を知らせる", "通知で出す", "記録する"),
    WEATHER_NOTIFY("🌤️", "天気を知らせる", "通知で出す", "記録する"),
    WAIT("⏱️", "少し待つ", "次の動作まで間をあける", "その他"),
    WEB_REQUEST("📡", "Webに送る", "URLへGET送信（上級者向け）", "上級者向け"),
    REPEAT("🔁", "直前を繰り返す", "直前の動作をもう何回か（上級者向け）", "上級者向け"),
    COMMAND_RUN("⌨️", "コマンド実行", "シェルのコマンドを動かす（上級者向け）", "上級者向け"),
    JS_RUN("📜", "JavaScript実行", "結果を通知で出す（上級者向け）", "上級者向け"),
    LAUNCH_APP("🚀", "アプリを起動する", "", "その他")
}

val ActionGroups = listOf("知らせる", "AI", "音", "画面・ライト", "設定を変える", "送る・開く", "記録する", "その他", "上級者向け")

enum class ConstraintType(val label: String) {
    NONE("条件なし"), WEEKDAY("平日だけ"), NIGHT("夜だけ"),
    BATTERY_ABOVE("電池が◯%以上のとき"), WIFI_ON("ネット接続中だけ"), CHARGING("充電中だけ")
}

data class ActionItem(val type: ActionType, val param: String = "")

data class Macro(
    val id: Long,
    val name: String,
    val trigger: TriggerType,
    val triggerParam: String = "",
    val actions: List<ActionItem> = listOf(ActionItem(ActionType.NOTIFICATION)),
    val constraintType: ConstraintType = ConstraintType.NONE,
    val constraintParam: String = "",
    val enabled: Boolean = true,
    val lastRunAt: Long = 0L
)

object Logic {
    // 変数タグの展開（上級者向け）：{date} {time} {weekday} {battery} {ai}
    fun expandVars(text: String, date: String, time: String, weekday: String, battery: Int, ai: String = ""): String =
        text.replace("{date}", date).replace("{time}", time).replace("{weekday}", weekday).replace("{battery}", battery.toString()).replace("{ai}", ai)

    // 「直前を繰り返す」を実際の並びに展開する（REPEAT 自体は消える。先頭の REPEAT は無効）
    fun expandRepeats(actions: List<ActionItem>): List<ActionItem> {
        val out = mutableListOf<ActionItem>()
        for (a in actions) {
            if (a.type == ActionType.REPEAT) {
                val prev = out.lastOrNull() ?: continue
                val n = when (a.param) { "2回" -> 2; "3回" -> 3; "5回" -> 5; else -> 2 }
                repeat(n) { out.add(prev) }
            } else out.add(a)
        }
        return out
    }

    fun autoName(trigger: TriggerType, actions: List<ActionItem>): String {
        if (actions.isEmpty()) return trigger.label
        val first = actions.first().type.label
        return if (actions.size == 1) "${trigger.label}で$first" else "${trigger.label}で$first など${actions.size}件"
    }

    fun summary(m: Macro): String {
        val t = when {
            m.trigger == TriggerType.SUN_EVENT -> if (m.triggerParam.startsWith("sunrise")) "日の出" else "日の入り"
            m.trigger == TriggerType.LOCATION -> {
                val parts = m.triggerParam.split("@")
                val name = parts.getOrNull(2)?.ifBlank { "場所" } ?: "場所"
                if (parts.getOrNull(0) == "leave") "${name}を離れたら" else "${name}に着いたら"
            }
            m.trigger == TriggerType.NOTIFICATION_POSTED && m.triggerParam.isNotBlank() -> "${m.trigger.label}（${m.triggerParam.substringBefore("|").substringAfterLast('.')}）"
            m.trigger == TriggerType.APP_LAUNCH && m.triggerParam.isNotBlank() -> "${m.trigger.label}（${m.triggerParam.substringAfterLast('.')}）"
            m.trigger == TriggerType.APP_CLOSED && m.triggerParam.isNotBlank() -> "${m.trigger.label}（${m.triggerParam.substringAfterLast('.')}）"
            m.trigger == TriggerType.TIME && m.triggerParam.isNotBlank() -> {
                val days = timeDaysOf(m.triggerParam)
                val base = "${m.trigger.label}（${m.triggerParam.substringBefore("@")}）"
                if (days == null) base else "$base・${daysLabel(days)}"
            }
            (m.trigger == TriggerType.DND_STATE || m.trigger == TriggerType.POWER_SAVE || m.trigger == TriggerType.AIRPLANE) -> "${m.trigger.label}（${if (m.triggerParam == "off") "OFF" else "ON"}）"
            m.triggerParam.isNotBlank() && m.trigger != TriggerType.BLUETOOTH -> "${m.trigger.label}（${m.triggerParam}）"
            else -> m.trigger.label
        }
        val a = m.actions.joinToString("＋") { item ->
            when {
                item.type == ActionType.LAUNCH_APP && item.param.isNotBlank() -> "${item.type.label}（${item.param.substringAfterLast('.')}）"
                (item.type == ActionType.NOTIFICATION || item.type == ActionType.SPEAK || item.type == ActionType.OPEN_URL || item.type == ActionType.SHARE_TEXT || item.type == ActionType.SMS_COMPOSE || item.type == ActionType.CALENDAR_ADD || item.type == ActionType.MEMO_SAVE || item.type == ActionType.CLIPBOARD_COPY || item.type == ActionType.WALLPAPER || item.type == ActionType.WAIT || item.type == ActionType.WEB_REQUEST || item.type == ActionType.REPEAT || item.type == ActionType.AI_ASK || item.type == ActionType.VIDEO_PLAY || item.type == ActionType.PLAYLIST_PLAY || item.type == ActionType.WIDGET_SET) && item.param.isNotBlank() -> "${item.type.label}（${item.param}）"
                else -> item.type.label
            }
        }
        val c = if (m.constraintType != ConstraintType.NONE) " / ${m.constraintType.label}" else ""
        return "$t → $a$c"
    }

    // 時刻パラメータ "HH:mm@23456" の曜日集合（Calendar.DAY_OF_WEEK の数字）。@なしは null（毎日）
    fun timeDaysOf(param: String): Set<Int>? {
        if (!param.contains("@")) return null
        val ds = param.substringAfter("@").mapNotNull { it.digitToIntOrNull() }.filter { it in 1..7 }.toSet()
        return if (ds.isEmpty() || ds.size == 7) null else ds
    }

    fun daysLabel(days: Set<Int>): String {
        if (days == setOf(2, 3, 4, 5, 6)) return "平日"
        if (days == setOf(1, 7)) return "週末"
        val names = mapOf(1 to "日", 2 to "月", 3 to "火", 4 to "水", 5 to "木", 6 to "金", 7 to "土")
        return days.sorted().joinToString("") { names[it] ?: "" }
    }

    fun batteryRisen(prev: Int, now: Int, threshold: Int): Boolean = prev < threshold && now >= threshold

    // 日の出・日の入りの時刻（その日の0時からの分数、現地時間）。NOAA近似式。計算できない場合は -1
    fun sunEventMinutes(lat: Double, lng: Double, dayOfYear: Int, tzOffsetMinutes: Int, sunrise: Boolean): Int {
        val rad = Math.PI / 180.0
        val lngHour = lng / 15.0
        val t = dayOfYear + ((if (sunrise) 6.0 else 18.0) - lngHour) / 24.0
        val m = 0.9856 * t - 3.289
        var l = m + 1.916 * Math.sin(m * rad) + 0.020 * Math.sin(2 * m * rad) + 282.634
        l = ((l % 360.0) + 360.0) % 360.0
        var ra = Math.atan(0.91764 * Math.tan(l * rad)) / rad
        ra = ((ra % 360.0) + 360.0) % 360.0
        ra += Math.floor(l / 90.0) * 90.0 - Math.floor(ra / 90.0) * 90.0
        ra /= 15.0
        val sinDec = 0.39782 * Math.sin(l * rad)
        val cosDec = Math.cos(Math.asin(sinDec))
        val cosH = (Math.cos(90.833 * rad) - sinDec * Math.sin(lat * rad)) / (cosDec * Math.cos(lat * rad))
        if (cosH > 1.0 || cosH < -1.0) return -1
        var h = if (sunrise) 360.0 - Math.acos(cosH) / rad else Math.acos(cosH) / rad
        h /= 15.0
        val tt = h + ra - 0.06571 * t - 6.622
        val ut = ((tt - lngHour) % 24.0 + 24.0) % 24.0
        return ((Math.round(ut * 60.0).toInt() + tzOffsetMinutes) % 1440 + 1440) % 1440
    }

    fun constraintOk(type: ConstraintType, dayOfWeek: Int, hour: Int): Boolean = when (type) {
        ConstraintType.NONE -> true
        ConstraintType.WEEKDAY -> dayOfWeek in 2..6
        ConstraintType.NIGHT -> hour >= 21 || hour < 6
        // 電池・ネット・充電の条件は端末状態が要るので Engine 側で判定する（ここでは通す）
        ConstraintType.BATTERY_ABOVE, ConstraintType.WIFI_ON, ConstraintType.CHARGING -> true
    }

    fun batteryCrossed(prev: Int, now: Int, threshold: Int): Boolean = prev > threshold && now <= threshold

    fun wifiConnectedEdge(prev: Boolean, now: Boolean): Boolean = !prev && now
}

data class Template(val title: String, val macroName: String, val trigger: TriggerType, val param: String, val actions: List<ActionItem>, val constraintType: ConstraintType = ConstraintType.NONE, val note: String = "", val badge: String = "")

val Templates = listOf(
    Template("動画の見すぎストッパー", "見すぎストッパー", TriggerType.APP_LAUNCH, "com.google.android.youtube", listOf(ActionItem(ActionType.WAIT, "5分"), ActionItem(ActionType.VIBRATE), ActionItem(ActionType.SPEAK, "そろそろ休憩しよ。目を休めてね")), ConstraintType.NONE, "つい見すぎる動画時間を、スマホ自身に止めてもらう。", badge = "イチオシ"),
    Template("充電100%で声でお知らせ", "充電完了のお知らせ", TriggerType.BATTERY_RISES, "100%", listOf(ActionItem(ActionType.RING), ActionItem(ActionType.SPEAK, "充電が終わったよ。ケーブルを抜いてね")), ConstraintType.NONE, "挿しっぱなし防止。終わったら、声で呼ばれる。"),
    Template("22時に静かに", "夜の静かモード", TriggerType.TIME, "22:00", listOf(ActionItem(ActionType.SILENT), ActionItem(ActionType.BRIGHTNESS_DOWN)), ConstraintType.NONE, "夜中に通知音で起きる事故を、なくす。"),
    Template("朝の準備", "朝のお知らせ", TriggerType.TIME, "07:00", listOf(ActionItem(ActionType.VOLUME_UP), ActionItem(ActionType.NOTIFICATION)), ConstraintType.WEEKDAY, "平日の朝、音量を戻して通知でスタート。"),
    Template("電池を節約", "電池節約", TriggerType.BATTERY_LOW, "20%", listOf(ActionItem(ActionType.BRIGHTNESS_DOWN), ActionItem(ActionType.NOTIFICATION)), ConstraintType.NONE, "20%を切ったら、自動で節電寄りに切り替える。"),
    Template("充電お知らせ", "充電お知らせ", TriggerType.CHARGING_START, "", listOf(ActionItem(ActionType.NOTIFICATION)), ConstraintType.NONE, "充電のはじまりを、通知で確認できる。"),
    Template("ケーブルを抜いたら声で知らせる", "充電のお知らせ", TriggerType.CHARGING_DONE, "", listOf(ActionItem(ActionType.SPEAK), ActionItem(ActionType.NOTIFICATION)), ConstraintType.NONE, "抜いた瞬間に、声と通知でお知らせ。"),
    Template("ヘッドホンで音楽", "ヘッドホンで音楽", TriggerType.HEADSET, "", listOf(ActionItem(ActionType.MEDIA_PLAY_PAUSE)), ConstraintType.NONE, "つないだら、そのまま再生スタート。"),
    Template("朝の支度アシスト", "朝の支度アシスト", TriggerType.TIME, "07:00", listOf(ActionItem(ActionType.WEATHER_NOTIFY), ActionItem(ActionType.SPEAK, "おはよう。今日の天気、通知に出したよ")), ConstraintType.WEEKDAY, "起きたら天気が届く。傘の判断を、家の中で済ませられる。"),
    Template("AIに聞く朝のひとこと", "朝のAIアドバイス", TriggerType.TIME, "07:00", listOf(ActionItem(ActionType.AI_ASK, "今日の天気に合う服装を、一言で教えて"), ActionItem(ActionType.SPEAK, "{ai}")), ConstraintType.WEEKDAY, "AIの答えを、そのまま声で読み上げ。朝の服装選びを肩代わり。", badge = "AI"),
    Template("帰宅モード", "帰宅モード", TriggerType.WIFI_CONNECTED, "", listOf(ActionItem(ActionType.BRIGHTNESS_UP), ActionItem(ActionType.VOLUME_UP), ActionItem(ActionType.NOTIFICATION, "おかえり。明るさと音量を戻したよ")), ConstraintType.NONE, "家のWi-Fiにつながったら、昼モードに自動で復帰。設定を開く手間が消える。", badge = "新入り"),
    Template("Bluetoothで音楽スタート", "Bluetoothで音楽", TriggerType.BLUETOOTH, "", listOf(ActionItem(ActionType.MUSIC_PLAY)), ConstraintType.NONE, "車やスピーカーがつながった瞬間、音楽が勝手に流れ出す。", badge = "新入り"),
    Template("LINEが来たら声でお知らせ", "LINEの声お知らせ", TriggerType.NOTIFICATION_POSTED, "jp.naver.line.android|", listOf(ActionItem(ActionType.SPEAK, "LINEが来たよ")), ConstraintType.NONE, "大事な連絡を、画面を見なくても気づける。相手はあとから編集で変えられる。", badge = "新入り"),
    Template("おやすみモードと連動", "おやすみ連動", TriggerType.DND_STATE, "on", listOf(ActionItem(ActionType.BRIGHTNESS_DOWN), ActionItem(ActionType.WIDGET_SET, "🌙 おやすみ中")), ConstraintType.NONE, "集中モードにしたら、画面もホーム画面も夜仕様に切り替わる。", badge = "新入り"),
    Template("着陸したら現地の天気", "着陸したら天気", TriggerType.AIRPLANE, "off", listOf(ActionItem(ActionType.WEATHER_NOTIFY)), ConstraintType.NONE, "機内モードを切った瞬間、着いた場所の天気が届く。旅行の最初の一手が決まる。", badge = "新入り"),
    Template("音楽で起きる朝", "音楽で起きる朝", TriggerType.TIME, "07:30", listOf(ActionItem(ActionType.MUSIC_PLAY), ActionItem(ActionType.VOLUME_UP)), ConstraintType.WEEKDAY, "アラーム音より、好きな音楽で起きる方が気持ちいい。", badge = "新入り"),
    Template("YouTubeを閉じたら一息", "見終わりの合図", TriggerType.APP_CLOSED, "com.google.android.youtube", listOf(ActionItem(ActionType.SPEAK, "おつかれさま。目を休めてね")), ConstraintType.NONE, "見終わったあとの区切りを、スマホが作ってくれる。", badge = "新入り"),
    Template("夕方の天気チェック", "夕方の天気チェック", TriggerType.TIME, "18:00", listOf(ActionItem(ActionType.WEATHER_NOTIFY)), ConstraintType.NONE, "一日の終わりに天気を確認。明日の支度の参考に、ちょうどいい。", badge = "新入り")
)
