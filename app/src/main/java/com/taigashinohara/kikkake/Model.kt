package com.taigashinohara.kikkake

enum class TriggerType(val icon: String, val label: String, val detail: String, val group: String) {
    TIME("🕐", "時刻", "", "時刻"),
    SUN_EVENT("🌅", "日の出・日の入り", "", "時刻"),
    LOCATION("📍", "場所", "着いたら／離れたら", "場所"),
    WIFI_CONNECTED("📶", "Wi-Fi", "家に着いたときなど", "つながり"),
    BLUETOOTH("🔷", "Bluetooth", "", "つながり"),
    NFC("🏷️", "NFCタグ", "アプリを開いてタグをかざす", "つながり"),
    HEADSET("🎧", "ヘッドホン", "有線のみ", "つながり"),
    CHARGING_START("🔌", "充電開始", "", "充電・電池"),
    CHARGING_DONE("✅", "ケーブルを抜いたら", "", "充電・電池"),
    BATTERY_LOW("🔋", "電池が少ない", "", "充電・電池"),
    BATTERY_RISES("⚡", "電池が増えたら", "", "充電・電池"),
    APP_LAUNCH("📱", "アプリを開いたら", "", "アプリ"),
    APP_CLOSED("📴", "アプリを閉じたら", "", "アプリ"),
    NOTIFICATION_POSTED("✉️", "通知が来たら", "", "連絡"),
    DND_STATE("🔕", "集中モード", "ON／OFFしたら", "端末の状態"),
    POWER_SAVE("🍃", "省電力モード", "ON／OFFしたら", "端末の状態"),
    AIRPLANE("✈️", "機内モード", "ON／OFFしたら", "端末の状態")
}

val TriggerGroups = listOf("時刻", "場所", "つながり", "充電・電池", "アプリ", "連絡", "端末の状態")

enum class ActionType(val icon: String, val label: String, val detail: String, val group: String) {
    NOTIFICATION("💬", "通知を出す", "好きな文でお知らせ", "知らせる"),
    SPEAK("🗣️", "声で知らせる", "好きな文を声で", "知らせる"),
    RING("📣", "音を鳴らす", "", "知らせる"),
    VIBRATE("📳", "バイブする", "", "知らせる"),
    WIDGET_SET("🧩", "ウィジェットに表示", "ホーム画面の文字を変える", "知らせる"),
    AI_ASK("🤖", "AIに聞く", "答えを通知で知らせる", "AI"),
    MEDIA_PLAY_PAUSE("🎵", "音楽を再生／停止", "", "音"),
    MUSIC_PLAY("▶️", "音楽を再生", "止まっていたら再生する", "音"),
    MEDIA_NEXT("⏭️", "次の曲へ", "", "音"),
    VOLUME_UP("🔊", "音量を上げる", "メディアの音量", "音"),
    VOLUME_DOWN("🔉", "音量を下げる", "メディアの音量", "音"),
    SILENT("🔕", "マナーモード", "", "音"),
    SILENT_OFF("🔔", "マナーモードを解除", "", "音"),
    DND_ON("😴", "おやすみモード", "通知をぜんぶ止める", "音"),
    DND_OFF("🌅", "おやすみモードを解除", "止めた通知を元に戻す", "音"),
    BRIGHTNESS_DOWN("🌙", "明るさを下げる", "", "画面・ライト"),
    BRIGHTNESS_UP("☀️", "明るさを戻す", "", "画面・ライト"),
    FLASHLIGHT("🔦", "ライト", "", "画面・ライト"),
    ROTATE_OFF("📐", "画面回転を固定", "", "設定を変える"),
    ROTATE_ON("🔄", "画面回転を自動に", "", "設定を変える"),
    SCREEN_KEEP("💡", "画面を消さない", "自動消灯オフ／戻す（切替）", "設定を変える"),
    WALLPAPER("🖼️", "壁紙を変える", "単色から選ぶ", "設定を変える"),
    WIFI_PANEL("🌐", "Wi-Fi設定を開く", "", "設定を変える"),
    BT_SETTINGS("🔷", "Bluetooth設定を開く", "", "設定を変える"),
    OPEN_URL("🔗", "URLを開く", "", "送る・開く"),
    VIDEO_PLAY("🎬", "動画を再生", "", "送る・開く"),
    PLAYLIST_PLAY("🎶", "プレイリストを再生", "", "送る・開く"),
    CAMERA_OPEN("📷", "カメラを開く", "撮影はシャッターを押してね", "送る・開く"),
    SHARE_TEXT("📤", "テキストを共有", "", "送る・開く"),
    SMS_COMPOSE("📩", "メッセージ作成を開く", "", "送る・開く"),
    CALENDAR_ADD("📅", "予定を追加", "今から1時間", "記録する"),
    MEMO_SAVE("📝", "メモを保存", "", "記録する"),
    CLIPBOARD_COPY("📋", "コピーする", "", "記録する"),
    LOCATION_NOTIFY("📍", "現在地を知らせる", "", "記録する"),
    WEATHER_NOTIFY("🌤️", "天気を知らせる", "", "記録する"),
    WAIT("⏱️", "少し待つ", "", "その他"),
    WEB_REQUEST("📡", "Webに送る", "URLへGET送信", "上級者向け"),
    REPEAT("🔁", "直前を繰り返す", "直前の動作をもう何回か", "上級者向け"),
    COMMAND_RUN("⌨️", "コマンド実行", "シェルのコマンドを動かす", "上級者向け"),
    JS_RUN("📜", "JavaScript実行", "結果を通知で出す", "上級者向け"),
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

    // タイル用の短い要約（v2.10.0）：きっかけ＋最初の動作＋「ほかN件」だけ。動作の文面までは出さない
    fun tileSummary(m: Macro): String {
        val (triggerPart, actionPart) = tileSummaryParts(m)
        return if (actionPart == null) triggerPart else "$triggerPart → $actionPart"
    }

    // タイルのチップ表示用に、きっかけ側と動作側を分けて返す（v2.13.2）。動作が無ければ actionPart は null
    // アプリ名の括弧書き（アプリを開いたら（YouTube）等）はチップでは落とす。チップは短さが命で、詳しくは編集で見る
    fun tileSummaryParts(m: Macro): Pair<String, String?> {
        var triggerPart = summary(m).substringBefore(" → ")
        if (m.trigger == TriggerType.APP_LAUNCH || m.trigger == TriggerType.APP_CLOSED || m.trigger == TriggerType.NOTIFICATION_POSTED) {
            triggerPart = triggerPart.substringBefore("（")
        }
        val first = m.actions.firstOrNull()?.type?.label ?: return triggerPart to null
        val actionPart = if (m.actions.size > 1) "$first ほか${m.actions.size - 1}件" else first
        return triggerPart to actionPart
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
    Template("AIに聞く朝のひとこと", "朝のAIアドバイス", TriggerType.TIME, "07:00", listOf(ActionItem(ActionType.AI_ASK, "今日の天気に合う服装を、一言で教えて"), ActionItem(ActionType.SPEAK, "{ai}")), ConstraintType.WEEKDAY, "AIの答えを、そのまま声で読み上げ。朝の服装選びを肩代わり。"),
    Template("帰宅モード", "帰宅モード", TriggerType.WIFI_CONNECTED, "", listOf(ActionItem(ActionType.BRIGHTNESS_UP), ActionItem(ActionType.VOLUME_UP), ActionItem(ActionType.NOTIFICATION, "おかえり。明るさと音量を戻したよ")), ConstraintType.NONE, "家のWi-Fiにつながったら、昼モードに自動で復帰。設定を開く手間が消える。"),
    Template("Bluetoothで音楽スタート", "Bluetoothで音楽", TriggerType.BLUETOOTH, "", listOf(ActionItem(ActionType.MUSIC_PLAY)), ConstraintType.NONE, "車やスピーカーがつながった瞬間、音楽が勝手に流れ出す。"),
    Template("LINEが来たら声でお知らせ", "LINEの声お知らせ", TriggerType.NOTIFICATION_POSTED, "jp.naver.line.android|", listOf(ActionItem(ActionType.SPEAK, "LINEが来たよ")), ConstraintType.NONE, "大事な連絡を、画面を見なくても気づける。相手はあとから編集で変えられる。"),
    Template("おやすみモードと連動", "おやすみ連動", TriggerType.DND_STATE, "on", listOf(ActionItem(ActionType.BRIGHTNESS_DOWN), ActionItem(ActionType.WIDGET_SET, "🌙 おやすみ中")), ConstraintType.NONE, "集中モードにしたら、画面もホーム画面も夜仕様に切り替わる。"),
    Template("着陸したら現地の天気", "着陸したら天気", TriggerType.AIRPLANE, "off", listOf(ActionItem(ActionType.WEATHER_NOTIFY)), ConstraintType.NONE, "機内モードを切った瞬間、着いた場所の天気が届く。旅行の最初の一手が決まる。"),
    Template("音楽で起きる朝", "音楽で起きる朝", TriggerType.TIME, "07:30", listOf(ActionItem(ActionType.MUSIC_PLAY), ActionItem(ActionType.VOLUME_UP)), ConstraintType.WEEKDAY, "アラーム音より、好きな音楽で起きる方が気持ちいい。"),
    Template("YouTubeを閉じたら一息", "見終わりの合図", TriggerType.APP_CLOSED, "com.google.android.youtube", listOf(ActionItem(ActionType.SPEAK, "おつかれさま。目を休めてね")), ConstraintType.NONE, "見終わったあとの区切りを、スマホが作ってくれる。"),
    Template("夕方の天気チェック", "夕方の天気チェック", TriggerType.TIME, "18:00", listOf(ActionItem(ActionType.WEATHER_NOTIFY)), ConstraintType.NONE, "一日の終わりに天気を確認。明日の支度の参考に、ちょうどいい。"),
    Template("日の入りに夜モード", "日の入りに夜モード", TriggerType.SUN_EVENT, "sunset@34.6937,135.5023", listOf(ActionItem(ActionType.BRIGHTNESS_DOWN), ActionItem(ActionType.SILENT)), ConstraintType.NONE, "太陽に合わせて動く。座標は大阪のもの、編集で自分の場所に変えてね。"),
    Template("日の出に朝モード", "日の出に朝モード", TriggerType.SUN_EVENT, "sunrise@34.6937,135.5023", listOf(ActionItem(ActionType.BRIGHTNESS_UP), ActionItem(ActionType.SILENT_OFF)), ConstraintType.NONE, "夜が明けたら、スマホも朝仕様に。座標は大阪のもの、編集で変えられる。"),
    Template("家の近くに着いたら", "帰宅の合図", TriggerType.LOCATION, "arrive@34.6937,135.5023", listOf(ActionItem(ActionType.NOTIFICATION, "家の近くに着いたよ")), ConstraintType.NONE, "場所でも発火できる。座標は大阪駅あたり、編集で自宅に変えると帰宅の合図になる。"),
    Template("毎晩の電池ログ", "電池ログ", TriggerType.TIME, "23:00", listOf(ActionItem(ActionType.MEMO_SAVE, "今日の電池は{battery}%")), ConstraintType.NONE, "毎晩の電池残量がメモに自動で溜まる。減り方のクセが見えてくる。"),
    Template("AIに夕飯を聞く", "AIの夕飯提案", TriggerType.TIME, "17:00", listOf(ActionItem(ActionType.AI_ASK, "卵とキャベツがある。夕飯のおかずを1つ提案して"), ActionItem(ActionType.SPEAK, "{ai}")), ConstraintType.NONE, "献立の悩みを、AIに丸投げ。答えは声で読み上げてくれる。"),
    Template("AIに今日の運勢", "AI運勢", TriggerType.TIME, "08:00", listOf(ActionItem(ActionType.AI_ASK, "今日の運勢を一言で占って"), ActionItem(ActionType.SPEAK, "{ai}")), ConstraintType.NONE, "毎朝、AIの占いが声で届く。いい日も悪い日も、まず笑える。"),
    Template("お昼のサイコロ", "サイコロ", TriggerType.TIME, "12:00", listOf(ActionItem(ActionType.JS_RUN, "Math.floor(Math.random()*6)+1")), ConstraintType.NONE, "実はJavaScriptも動く。結果は通知で届く、お昼の暇つぶし。"),
    Template("請求書メールは強めに", "請求書アラート", TriggerType.NOTIFICATION_POSTED, "com.google.android.gm|請求", listOf(ActionItem(ActionType.VIBRATE), ActionItem(ActionType.REPEAT, "3回"), ActionItem(ActionType.SPEAK, "請求書のメールが来たよ")), ConstraintType.NONE, "キーワードで絞って、バイブ3連打＋声。大事なメールだけ強めに知らせる。"),
    Template("Kindleを開いたら画面を消さない", "読書モード", TriggerType.APP_LAUNCH, "com.amazon.kindle", listOf(ActionItem(ActionType.SCREEN_KEEP)), ConstraintType.NONE, "読書中に画面が消えるイライラとお別れ。開くたびにON／OFFが切り替わる。"),
    Template("ベッドで画面を固定", "ベッドモード", TriggerType.TIME, "22:30", listOf(ActionItem(ActionType.ROTATE_OFF)), ConstraintType.NONE, "寝転んで見ても、画面がクルクル回らなくなる。"),
    Template("夜は壁紙も夜色に", "夜の壁紙", TriggerType.TIME, "23:00", listOf(ActionItem(ActionType.WALLPAPER, "黒")), ConstraintType.NONE, "壁紙まで自動で夜仕様。色は黒・白・ピンク・水色・紫・オレンジから編集で選べる。"),
    Template("朝イチに集中タイム", "集中タイム登録", TriggerType.TIME, "09:00", listOf(ActionItem(ActionType.CALENDAR_ADD, "集中タイム")), ConstraintType.WEEKDAY, "その日の予定に「今から1時間の集中タイム」が自動で入る。"),
    Template("LINEを開いたらお礼文をコピー", "お礼文コピー", TriggerType.APP_LAUNCH, "jp.naver.line.android", listOf(ActionItem(ActionType.CLIPBOARD_COPY, "ありがとうございます！助かりました")), ConstraintType.NONE, "開いた瞬間に定型文がコピー済み。あとは貼るだけ。文は編集で変えられる。"),
    // v2.9.0 追加分（便利・面白い・すごい を20件）
    Template("玄関タグで帰宅セット", "帰宅セット", TriggerType.NFC, "", listOf(ActionItem(ActionType.BRIGHTNESS_UP), ActionItem(ActionType.SILENT_OFF), ActionItem(ActionType.NOTIFICATION, "おかえり。明るさと音を戻したよ")), ConstraintType.NONE, "玄関に貼ったNFCタグにかざすだけ。帰宅後の切り替えが、ワンタッチで終わる。"),
    Template("省電力になったら節電モード", "省電力で節電", TriggerType.POWER_SAVE, "on", listOf(ActionItem(ActionType.BRIGHTNESS_DOWN), ActionItem(ActionType.NOTIFICATION, "省電力モードになったので、明るさも下げたよ")), ConstraintType.NONE, "端末の省電力に、画面の明るさまで連動させる。"),
    Template("省電力が終わったら明るさ戻す", "省電力から復帰", TriggerType.POWER_SAVE, "off", listOf(ActionItem(ActionType.BRIGHTNESS_UP)), ConstraintType.NONE, "省電力が解けたら、明るさも自動で元どおり。戻し忘れがなくなる。"),
    Template("機内モードで読書の準備", "機内の読書準備", TriggerType.AIRPLANE, "on", listOf(ActionItem(ActionType.SCREEN_KEEP), ActionItem(ActionType.BRIGHTNESS_DOWN)), ConstraintType.NONE, "機内モードにしたら、画面は消えず明るさは控えめ。飛行機の中の読書仕様。"),
    Template("集中が終わったらおつかれさま", "集中おわり", TriggerType.DND_STATE, "off", listOf(ActionItem(ActionType.SPEAK, "おつかれさま。集中タイム、終わりだよ"), ActionItem(ActionType.WIDGET_SET, "おつかれさま ☕")), ConstraintType.NONE, "集中モードを切った瞬間、ねぎらいの声とホーム画面のひとこと。"),
    Template("充電が半分でメモ", "充電メモ", TriggerType.BATTERY_RISES, "50%", listOf(ActionItem(ActionType.MEMO_SAVE, "{time} 電池{battery}%")), ConstraintType.NONE, "充電の進み具合がメモに残る。あとで見ると、充電のクセが分かる。"),
    Template("お昼の姿勢リセット", "姿勢リセット", TriggerType.TIME, "12:15", listOf(ActionItem(ActionType.VIBRATE), ActionItem(ActionType.SPEAK, "背筋を伸ばして、深呼吸しよ")), ConstraintType.WEEKDAY, "平日のお昼、バイブと声で姿勢をリセット。午後の縮こまり防止。"),
    Template("地図を開いたら現在地", "地図で現在地", TriggerType.APP_LAUNCH, "com.google.android.apps.maps", listOf(ActionItem(ActionType.LOCATION_NOTIFY)), ConstraintType.NONE, "マップを開いたら、今いる場所が通知にも出る。待ち合わせの共有が速くなる。"),
    Template("車につながったら地図", "車の地図", TriggerType.BLUETOOTH, "", listOf(ActionItem(ActionType.LAUNCH_APP, "com.google.android.apps.maps")), ConstraintType.NONE, "車のBluetoothがつながった瞬間、地図が開く。出発の流れが止まらない。"),
    Template("会議の通知で予定を仮登録", "会議の仮登録", TriggerType.NOTIFICATION_POSTED, "com.google.android.gm|会議", listOf(ActionItem(ActionType.CALENDAR_ADD, "会議（通知から仮登録）")), ConstraintType.NONE, "「会議」のメールが来たら、予定に仮登録まで自動。あとで直すだけになる。"),
    Template("家族のLINEは音つきで", "家族のLINE", TriggerType.NOTIFICATION_POSTED, "jp.naver.line.android|お母さん", listOf(ActionItem(ActionType.RING), ActionItem(ActionType.SPEAK, "お母さんからLINEだよ")), ConstraintType.NONE, "名前で絞って、大事な人の連絡だけ音つきで。名前は編集で変えられる。"),
    Template("朝のおみくじ", "朝のおみくじ", TriggerType.TIME, "08:30", listOf(ActionItem(ActionType.JS_RUN, "['大吉','中吉','小吉','吉'][Math.floor(Math.random()*4)]")), ConstraintType.NONE, "毎朝のおみくじが通知で届く。JavaScriptで引く、ささやかな運試し。"),
    Template("午後のほめ言葉", "ほめ言葉", TriggerType.TIME, "15:00", listOf(ActionItem(ActionType.AI_ASK, "がんばっている人を、一言で褒めて"), ActionItem(ActionType.SPEAK, "{ai}")), ConstraintType.WEEKDAY, "平日の午後、AIのほめ言葉が声で届く。あとひと踏ん張りの栄養。"),
    Template("スマホを置く時間", "スマホを置く", TriggerType.TIME, "20:00", listOf(ActionItem(ActionType.WALLPAPER, "ピンク"), ActionItem(ActionType.SPEAK, "スマホは置いて、自分の時間にしよ")), ConstraintType.NONE, "壁紙の色が変わったら、スマホを置く合図。夜の時間を取り戻す。"),
    Template("夜の帰宅でライト", "帰宅ライト", TriggerType.LOCATION, "arrive@34.6937,135.5023", listOf(ActionItem(ActionType.FLASHLIGHT), ActionItem(ActionType.NOTIFICATION, "おかえり。ライトをつけたよ")), ConstraintType.NONE, "家の近くに着いたら、ライトが点く。暗い玄関先の足元を照らす。座標は編集で自宅に。"),
    Template("朝のニュースを開く", "朝のニュース", TriggerType.TIME, "07:45", listOf(ActionItem(ActionType.OPEN_URL, "https://news.google.com/")), ConstraintType.WEEKDAY, "平日の朝、ニュースが開いた状態で待っている。世の中チェックの習慣化。"),
    Template("帰りの電車で読書", "帰りの読書", TriggerType.TIME, "18:45", listOf(ActionItem(ActionType.LAUNCH_APP, "com.amazon.kindle")), ConstraintType.WEEKDAY, "平日の夕方、Kindleが開く。帰りの電車が読書時間に変わる。"),
    Template("お昼寝タイマー", "お昼寝タイマー", TriggerType.TIME, "13:00", listOf(ActionItem(ActionType.WAIT, "20分"), ActionItem(ActionType.RING), ActionItem(ActionType.SPEAK, "お昼寝おわり。すっきり起きよ")), ConstraintType.NONE, "20分待ってから鳴らす、昼寝専用の目覚まし。タイマーを掛ける手間なし。"),
    Template("電池ピンチの助け", "電池ピンチ", TriggerType.BATTERY_LOW, "10%", listOf(ActionItem(ActionType.SPEAK, "電池が10パーセント。充電してね"), ActionItem(ActionType.LOCATION_NOTIFY)), ConstraintType.NONE, "10%を切ったら声で警告＋現在地を通知に残す。万が一の居場所メモにもなる。"),
    Template("端末の容量チェック", "容量チェック", TriggerType.TIME, "21:00", listOf(ActionItem(ActionType.COMMAND_RUN, "df -h /data")), ConstraintType.NONE, "コマンド実行で空き容量を通知。実はシェルも動く、上級者向けの本気機能。")
)
