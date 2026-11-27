package com.taigashinohara.kikkake

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.KeyEvent
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

object Engine {
    const val CHANNEL_ID = "kikkake"
    const val EXTRA_MACRO_ID = "macro_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "きっかけ", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "自動化の実行お知らせ" })
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun hasDndAccess(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        return nm.isNotificationPolicyAccessGranted
    }

    fun canWriteSettings(context: Context): Boolean = Settings.System.canWrite(context)

    fun hasUsageAccess(context: Context): Boolean {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60_000, now)
        return stats != null && stats.isNotEmpty()
    }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasBluetoothPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    fun isNotificationListenerEnabled(context: Context): Boolean =
        androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    // 許可の状態を1回だけまとめて読むスナップショット（v2.11.0）。
    // 以前はテンプレ51件ぶん missingPermKeys を回すたびに全チェックを繰り返していた。
    data class PermState(
        val notif: Boolean, val dnd: Boolean, val write: Boolean, val exact: Boolean,
        val usage: Boolean, val location: Boolean, val calendar: Boolean, val bt: Boolean, val notiflisten: Boolean
    )

    fun snapshotPermState(context: Context): PermState = PermState(
        notif = hasNotificationPermission(context),
        dnd = hasDndAccess(context),
        write = canWriteSettings(context),
        exact = canExactAlarm(context),
        usage = hasUsageAccess(context),
        location = hasLocationPermission(context),
        calendar = hasCalendarPermission(context),
        bt = hasBluetoothPermission(context),
        notiflisten = isNotificationListenerEnabled(context)
    )

    // 最後に分かっている現在地（GPS→ネットワークの順）。取れなければ null
    fun lastKnownLatLng(context: Context): Pair<Double, Double>? {
        if (!hasLocationPermission(context)) return null
        return try {
            val lm = context.getSystemService(android.location.LocationManager::class.java)
            val loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            if (loc != null) loc.latitude to loc.longitude else null
        } catch (e: Exception) { null }
    }

    fun batteryLevel(context: Context): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return 100
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return if (scale > 0) level * 100 / scale else level
    }

    fun isCharging(context: Context): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
    }

    fun isWifiConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
        } catch (e: Exception) { false }
    }

    // Webリクエスト（GET）。メインスレッドからでも詰まらないよう別スレッドで送る
    private fun sendWebRequest(rawUrl: String): Boolean {
        var ok = false
        val latch = CountDownLatch(1)
        Thread {
            try {
                val url = rawUrl.trim().let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 5000; conn.readTimeout = 5000
                conn.setRequestProperty("User-Agent", "Kikkake")
                val code = conn.responseCode
                ok = code in 200..399
                conn.disconnect()
            } catch (e: Exception) { ok = false }
            latch.countDown()
        }.start()
        try { latch.await(7, TimeUnit.SECONDS) } catch (e: Exception) { }
        return ok
    }

    private data class AiOutcome(val answer: String?, val note: String?)

    // AIに聞く：端末内AI（Gemini Nano）→ ダメなら APIキーで Gemini API、の順に試す
    private fun askAi(context: Context, prompt: String): AiOutcome {
        val dev = askOnDeviceAi(prompt)
        if (dev.answer != null) return dev
        val key = StateStore(context).aiApiKey
        if (key.isNotBlank()) {
            val api = askApiAi(key, prompt)
            if (api.answer != null) return api
        }
        return dev
    }

    private fun askOnDeviceAi(prompt: String): AiOutcome {
        return try {
            kotlinx.coroutines.runBlocking {
                val model = com.google.mlkit.genai.prompt.Generation.getClient()
                when (model.checkStatus()) {
                    com.google.mlkit.genai.common.FeatureStatus.AVAILABLE -> {
                        val text = model.generateContent(prompt).candidates.firstOrNull()?.text?.trim()
                        AiOutcome(if (text.isNullOrBlank()) null else text, null)
                    }
                    com.google.mlkit.genai.common.FeatureStatus.DOWNLOADABLE -> {
                        Thread { try { kotlinx.coroutines.runBlocking { model.download().collect { } } } catch (e: Exception) { } }.start()
                        AiOutcome(null, "AIモデルをダウンロード中")
                    }
                    else -> AiOutcome(null, null)
                }
            }
        } catch (e: Exception) { AiOutcome(null, null) }
    }

    // Gemini API（キーが必要）。ホームの「AIに聞く の設定」でキーを入れる
    private fun askApiAi(apiKey: String, prompt: String): AiOutcome {
        var result = AiOutcome(null, null)
        val latch = CountDownLatch(1)
        Thread {
            try {
                val conn = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent").openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 10000; conn.readTimeout = 30000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-goog-api-key", apiKey)
                val body = org.json.JSONObject().apply {
                    put("contents", org.json.JSONArray().put(org.json.JSONObject().apply {
                        put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", prompt)))
                    }))
                }.toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.readText() ?: ""
                if (code in 200..299) {
                    val parsed = org.json.JSONObject(text).optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")?.trim()
                    if (!parsed.isNullOrBlank()) result = AiOutcome(parsed, null)
                }
                conn.disconnect()
            } catch (e: Exception) { }
            latch.countDown()
        }.start()
        try { latch.await(35, TimeUnit.SECONDS) } catch (e: Exception) { }
        return result
    }

    // コマンド実行（上級者向け）：/system/bin/sh で動かし、出力を返す。起動自体に失敗したら null
    private fun runShellCommand(rawCmd: String): String? {
        if (rawCmd.isBlank()) return null
        return try {
            val proc = ProcessBuilder("/system/bin/sh", "-c", rawCmd).redirectErrorStream(true).start()
            val sb = StringBuilder()
            val reader = Thread {
                try { proc.inputStream.bufferedReader().forEachLine { synchronized(sb) { if (sb.length < 2000) sb.append(it).append('\n') } } } catch (e: Exception) { }
            }
            reader.start()
            if (!proc.waitFor(15, TimeUnit.SECONDS)) {
                proc.destroyForcibly(); "時間切れ（15秒で止めました）"
            } else {
                try { reader.join(1000) } catch (e: Exception) { }
                val text = sb.toString().trim()
                when {
                    text.isNotBlank() -> text.take(1000)
                    proc.exitValue() != 0 -> "コマンドがエラーで終わりました（コード ${proc.exitValue()}）"
                    else -> "（出力なし）"
                }
            }
        } catch (e: Exception) { null }
    }

    // JavaScript実行（上級者向け）：Rhino の解釈モードで動かし、print と最後の値を返す
    private fun runJavaScript(script: String): String {
        if (script.isBlank()) return "（スクリプトが空です）"
        val out = StringBuilder()
        var entered = false
        return try {
            val cx = org.mozilla.javascript.Context.enter(); entered = true
            cx.optimizationLevel = -1
            cx.languageVersion = org.mozilla.javascript.Context.VERSION_ES6
            val scope = cx.initStandardObjects()
            val printFn = object : org.mozilla.javascript.Callable {
                override fun call(c: org.mozilla.javascript.Context, s: org.mozilla.javascript.Scriptable, thisObj: org.mozilla.javascript.Scriptable, args: Array<Any?>): Any? {
                    out.append(args.joinToString(" ") { org.mozilla.javascript.Context.toString(it) }).append('\n')
                    return org.mozilla.javascript.Undefined.instance
                }
            }
            org.mozilla.javascript.ScriptableObject.putProperty(scope, "print", printFn)
            val result = cx.evaluateString(scope, script, "kikkake", 1, null)
            val tail = if (result == null || result === org.mozilla.javascript.Undefined.instance) "" else org.mozilla.javascript.Context.toString(result)
            val text = (out.toString() + tail).trim()
            if (text.isBlank()) "（出力なし）" else text.take(1000)
        } catch (e: Exception) {
            "エラー：${e.message}".take(1000)
        } finally {
            if (entered) org.mozilla.javascript.Context.exit()
        }
    }

    // 動画・プレイリストのリンクを再生アプリで開く
    private fun openMediaUrl(context: Context, rawUrl: String, video: Boolean): Boolean {
        val url = rawUrl.trim().let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
        if (url.length <= 8) return false
        val uri = android.net.Uri.parse(url)
        return try {
            val intent = if (video) Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/*") else Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent); true
        } catch (e: Exception) {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } catch (e2: Exception) { false }
        }
    }

    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun hasCalendarReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    // 今日と明日の予定の「時刻と参加者の有無」だけを読む（v2.14.0、「今日の顔」の判定用）。
    // タイトル等の本文は読まない・端末の外に送らない。許可が無い／読めない時は null（呼び出し側は曜日・時間帯の顔に落とす）。
    // 終日予定は時刻の顔判定に使わないので除く
    fun readDayEvents(context: Context, dayStart: Long): List<DayEventSpan>? {
        if (!hasCalendarReadPermission(context)) return null
        val end = dayStart + 2 * 24 * 60 * 60_000L
        fun query(withAttendee: Boolean): List<DayEventSpan> {
            val builder = android.provider.CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, dayStart)
            android.content.ContentUris.appendId(builder, end)
            // hasAttendee は Instances の列名文字列で指定する（定数が Kotlin から引けないため）。列が無い端末では例外→参加者なしで再試行する
            val proj = if (withAttendee) arrayOf(android.provider.CalendarContract.Instances.BEGIN, android.provider.CalendarContract.Instances.END, "hasAttendee", android.provider.CalendarContract.Instances.ALL_DAY)
                else arrayOf(android.provider.CalendarContract.Instances.BEGIN, android.provider.CalendarContract.Instances.END, android.provider.CalendarContract.Instances.ALL_DAY)
            val out = mutableListOf<DayEventSpan>()
            context.contentResolver.query(builder.build(), proj, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val allDay = c.getInt(if (withAttendee) 3 else 2) == 1
                    if (allDay) continue
                    out.add(DayEventSpan(c.getLong(0), c.getLong(1), withAttendee && c.getInt(2) == 1))
                }
            }
            return out
        }
        return try { query(true) } catch (e: Exception) { try { query(false) } catch (e2: Exception) { null } }
    }

    // 通知を1件出す（現在地・天気のアクション用）。成功したら true
    private fun postNotify(context: Context, macro: Macro, text: String): Boolean {
        ensureChannel(context)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(macro.name)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true).build()
        return try { NotificationManagerCompat.from(context).notify((macro.id * 10 + 7).toInt(), notif); true } catch (e: SecurityException) { false }
    }

    // Open-Meteo（キー不要）で今の天気を取る。失敗したら null
    private fun fetchWeather(lat: Double, lng: Double): String? {
        var result: String? = null
        val latch = CountDownLatch(1)
        Thread { result = fetchWeatherSync(lat, lng); latch.countDown() }.start()
        try { latch.await(6, TimeUnit.SECONDS) } catch (e: Exception) { }
        return result
    }

    private fun fetchWeatherSync(lat: Double, lng: Double): String? {
        return try {
            val url = java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current=temperature_2m,weather_code")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            val body = conn.inputStream.bufferedReader().readText()
            val cur = org.json.JSONObject(body).getJSONObject("current")
            val temp = cur.getDouble("temperature_2m")
            val code = cur.getInt("weather_code")
            val label = when (code) {
                0 -> "晴れ"; 1 -> "ほぼ晴れ"; 2 -> "くもりがち"; 3 -> "くもり"
                45, 48 -> "霧"; 51, 53, 55 -> "霧雨"; 56, 57 -> "凍る霧雨"
                61 -> "小雨"; 63 -> "雨"; 65 -> "強い雨"; 66, 67 -> "凍る雨"
                71 -> "小雪"; 73 -> "雪"; 75 -> "大雪"; 77 -> "霧雪"
                80 -> "にわか雨"; 81 -> "にわか雨"; 82 -> "激しいにわか雨"
                85, 86 -> "にわか雪"; 95 -> "雷雨"; 96, 99 -> "雷雨（ひょう）"
                else -> "不明"
            }
            "今の天気：$label ${temp}℃"
        } catch (e: Exception) { null }
    }

    fun canExactAlarm(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = context.getSystemService(AlarmManager::class.java)
        return am.canScheduleExactAlarms()
    }

    enum class ExecResult { OK, PARTIAL_OK, BLOCKED_BY_CONSTRAINT, BLOCKED_BY_PERMISSION, NOTHING_RAN }

    fun execute(context: Context, macro: Macro): Boolean {
        val r = executeInternal(context, macro, ignoreConstraint = false)
        return r == ExecResult.OK || r == ExecResult.PARTIAL_OK
    }

    fun executeTest(context: Context, macro: Macro): ExecResult = executeInternal(context, macro, ignoreConstraint = true)

    // 実行履歴：成功は「名前|時刻」、失敗は「名前|時刻|F:理由」で直近20件まで残す（UX2-6）
    private fun recordRun(context: Context, macro: Macro, ok: Boolean, reason: String = "") {
        val st = StateStore(context)
        val entry = if (ok) "${macro.name}|${System.currentTimeMillis()}" else "${macro.name}|${System.currentTimeMillis()}|F:$reason"
        val items = (listOf(entry) + st.runLog.split("\n").filter { it.isNotBlank() }).take(20)
        st.runLog = items.joinToString("\n")
    }

    private fun executeInternal(context: Context, macro: Macro, ignoreConstraint: Boolean): ExecResult {
        val cal = Calendar.getInstance()
        if (!ignoreConstraint && !Logic.constraintOk(macro.constraintType, cal.get(Calendar.DAY_OF_WEEK), cal.get(Calendar.HOUR_OF_DAY))) {
            recordRun(context, macro, false, when (macro.constraintType) {
                ConstraintType.WEEKDAY -> "平日だけの条件に合わなかった"
                ConstraintType.NIGHT -> "夜だけの条件に合わなかった"
                else -> "条件に合わなかった"
            })
            return ExecResult.BLOCKED_BY_CONSTRAINT
        }
        if (!ignoreConstraint) {
            val advFail = when (macro.constraintType) {
                ConstraintType.BATTERY_ABOVE -> if (batteryLevel(context) >= (macro.constraintParam.toIntOrNull() ?: 50)) null else "電池が${macro.constraintParam}%以上の条件に合わなかった"
                ConstraintType.WIFI_ON -> if (isWifiConnected(context)) null else "ネット接続中の条件に合わなかった"
                ConstraintType.CHARGING -> if (isCharging(context)) null else "充電中の条件に合わなかった"
                else -> null
            }
            if (advFail != null) { recordRun(context, macro, false, advFail); return ExecResult.BLOCKED_BY_CONSTRAINT }
        }
        var anyOk = false
        var blocked = false
        val varDate = java.text.SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(java.util.Date())
        val varTime = java.text.SimpleDateFormat("HH:mm", Locale.JAPAN).format(java.util.Date())
        val varWeekday = listOf("", "日", "月", "火", "水", "木", "金", "土")[cal.get(Calendar.DAY_OF_WEEK)]
        val varBattery = batteryLevel(context)
        var varAi = StateStore(context).lastAiResponse
        for (raw in Logic.expandRepeats(macro.actions)) {
            val item = raw.copy(param = Logic.expandVars(raw.param, varDate, varTime, varWeekday, varBattery, varAi))
            when (item.type) {
                ActionType.NOTIFICATION -> {
                    if (!hasNotificationPermission(context)) { blocked = true } else {
                        ensureChannel(context)
                        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle(macro.name)
                            .setContentText(item.param.ifBlank { "「${macro.name}」を実行しました" })
                            .setAutoCancel(true).build()
                        try { NotificationManagerCompat.from(context).notify(macro.id.toInt(), notif); anyOk = true } catch (e: SecurityException) { blocked = true }
                    }
                }
                ActionType.VOLUME_UP -> {
                    val am = context.getSystemService(AudioManager::class.java)
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * 0.7).toInt(), 0); anyOk = true
                }
                ActionType.VOLUME_DOWN -> {
                    val am = context.getSystemService(AudioManager::class.java)
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * 0.2).toInt(), 0); anyOk = true
                }
                ActionType.BRIGHTNESS_UP -> {
                    if (!canWriteSettings(context)) { blocked = true } else {
                        try { Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL); Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 140); anyOk = true } catch (e: Exception) { blocked = true }
                    }
                }
                ActionType.SILENT_OFF -> {
                    if (!hasDndAccess(context)) { blocked = true } else {
                        val am = context.getSystemService(AudioManager::class.java)
                        am.ringerMode = AudioManager.RINGER_MODE_NORMAL; anyOk = true
                    }
                }
                ActionType.BRIGHTNESS_DOWN -> {
                    if (!canWriteSettings(context)) { blocked = true } else {
                        try { Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL); Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 30); anyOk = true } catch (e: Exception) { blocked = true }
                    }
                }
                ActionType.SILENT -> {
                    if (!hasDndAccess(context)) { blocked = true } else {
                        val am = context.getSystemService(AudioManager::class.java)
                        am.ringerMode = AudioManager.RINGER_MODE_SILENT; anyOk = true
                    }
                }
                ActionType.LAUNCH_APP -> {
                    val pkg = item.param
                    val intent = if (pkg.isNotBlank()) context.packageManager.getLaunchIntentForPackage(pkg) else null
                    if (intent == null) { blocked = true } else {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try { context.startActivity(intent); anyOk = true } catch (e: Exception) { blocked = true }
                    }
                }
                ActionType.SPEAK -> {
                    // 声が実際に出た時だけ成功にする（UX2-1）。呼び出し側はバックグラウンドスレッド前提
                    if (speakAloud(context, item.param.ifBlank { "「${macro.name}」を実行しました" })) anyOk = true else blocked = true
                }
                ActionType.RING -> {
                    try {
                        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        RingtoneManager.getRingtone(context, uri)?.play(); anyOk = true
                    } catch (e: Exception) { }
                }
                ActionType.MEDIA_PLAY_PAUSE -> {
                    try {
                        val am = context.getSystemService(AudioManager::class.java)
                        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                        anyOk = true
                    } catch (e: Exception) { }
                }
                ActionType.MEDIA_NEXT -> {
                    try {
                        val am = context.getSystemService(AudioManager::class.java)
                        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                        anyOk = true
                    } catch (e: Exception) { }
                }
                ActionType.DND_ON -> {
                    if (!hasDndAccess(context)) { blocked = true } else {
                        val nm = context.getSystemService(NotificationManager::class.java)
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE); anyOk = true
                    }
                }
                ActionType.DND_OFF -> {
                    if (!hasDndAccess(context)) { blocked = true } else {
                        val nm = context.getSystemService(NotificationManager::class.java)
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL); anyOk = true
                    }
                }
                ActionType.VIBRATE -> {
                    try {
                        val vib = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
                        vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 120, 300), -1)); anyOk = true
                    } catch (e: Exception) { }
                }
                ActionType.FLASHLIGHT -> {
                    try {
                        val cm = context.getSystemService(CameraManager::class.java)
                        val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
                        if (id != null) {
                            val st = StateStore(context)
                            // 保存した状態ではなく実際のライトの状態を読んで切り替える（手動で消された時のずれ防止）
                            val next = !currentTorchOn(cm, id, st.torchOn)
                            cm.setTorchMode(id, next); st.torchOn = next; anyOk = true
                        }
                    } catch (e: Exception) { }
                }
                ActionType.ROTATE_OFF -> {
                    if (!canWriteSettings(context)) { blocked = true } else {
                        try { Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0); anyOk = true } catch (e: Exception) { blocked = true }
                    }
                }
                ActionType.ROTATE_ON -> {
                    if (!canWriteSettings(context)) { blocked = true } else {
                        try { Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1); anyOk = true } catch (e: Exception) { blocked = true }
                    }
                }
                ActionType.WALLPAPER -> {
                    try {
                        val colors = mapOf("黒" to 0xFF212121.toInt(), "白" to 0xFFFAFAFA.toInt(), "ピンク" to 0xFFF8BBD0.toInt(), "水色" to 0xFFB3E5FC.toInt(), "紫" to 0xFFE1BEE7.toInt(), "オレンジ" to 0xFFFFE0B2.toInt())
                        val bmp = android.graphics.Bitmap.createBitmap(1080, 2400, android.graphics.Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(colors[item.param] ?: 0xFF212121.toInt())
                        val wm = android.app.WallpaperManager.getInstance(context)
                        if (Build.VERSION.SDK_INT >= 24) wm.setBitmap(bmp, null, true, android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK) else @Suppress("DEPRECATION") wm.setBitmap(bmp)
                        anyOk = true
                    } catch (e: Exception) { blocked = true }
                }
                ActionType.WIFI_PANEL -> {
                    try {
                        val intent = if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY) else Intent(Settings.ACTION_WIFI_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent); anyOk = true
                    } catch (e: Exception) { blocked = true }
                }
                ActionType.BT_SETTINGS -> {
                    try {
                        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent); anyOk = true
                    } catch (e: Exception) { blocked = true }
                }
                ActionType.OPEN_URL -> {
                    val raw = item.param.trim()
                    if (raw.isBlank()) { blocked = true } else {
                        try {
                            val url = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); anyOk = true
                        } catch (e: Exception) { blocked = true }
                    }
                }
                ActionType.SHARE_TEXT -> {
                    try {
                        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, item.param.ifBlank { macro.name }) }
                        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); anyOk = true
                    } catch (e: Exception) { blocked = true }
                }
                ActionType.SMS_COMPOSE -> {
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:")).apply { putExtra("sms_body", item.param) }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent); anyOk = true
                    } catch (e: Exception) { blocked = true }
                }
                ActionType.CALENDAR_ADD -> {
                    if (!hasCalendarPermission(context)) { blocked = true } else {
                        try {
                            val cr = context.contentResolver
                            var calId: Long? = null
                            cr.query(android.provider.CalendarContract.Calendars.CONTENT_URI, arrayOf(android.provider.CalendarContract.Calendars._ID), "${android.provider.CalendarContract.Calendars.VISIBLE} = 1", null, null)?.use { c -> if (c.moveToFirst()) calId = c.getLong(0) }
                            val id = calId
                            if (id == null) { blocked = true } else {
                                val now = System.currentTimeMillis()
                                val values = android.content.ContentValues().apply {
                                    put(android.provider.CalendarContract.Events.CALENDAR_ID, id)
                                    put(android.provider.CalendarContract.Events.TITLE, item.param.ifBlank { macro.name })
                                    put(android.provider.CalendarContract.Events.DTSTART, now)
                                    put(android.provider.CalendarContract.Events.DTEND, now + 60 * 60_000)
                                    put(android.provider.CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
                                }
                                if (cr.insert(android.provider.CalendarContract.Events.CONTENT_URI, values) != null) anyOk = true else blocked = true
                            }
                        } catch (e: Exception) { blocked = true }
                    }
                }
                ActionType.MEMO_SAVE -> {
                    try {
                        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                        val file = java.io.File(dir, "kikkake-memo.txt")
                        val ts = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(java.util.Date())
                        file.appendText("$ts ${item.param.ifBlank { macro.name }}\n")
                        anyOk = true
                    } catch (e: Exception) { blocked = true }
                }
                ActionType.CLIPBOARD_COPY -> {
                    try {
                        val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("kikkake", item.param))
                        anyOk = true
                    } catch (e: Exception) { blocked = true }
                }
                ActionType.LOCATION_NOTIFY -> {
                    val ll = lastKnownLatLng(context)
                    if (ll == null || !hasNotificationPermission(context)) { blocked = true } else {
                        val place = try {
                            @Suppress("DEPRECATION") val list = android.location.Geocoder(context, Locale.JAPAN).getFromLocation(ll.first, ll.second, 1)
                            val a = list?.firstOrNull()
                            if (a != null) listOfNotNull(a.locality, a.thoroughfare).joinToString(" ").ifBlank { null } else null
                        } catch (e: Exception) { null }
                        val text = place ?: "緯度 ${"%.5f".format(ll.first)}、経度 ${"%.5f".format(ll.second)}"
                        if (postNotify(context, macro, "現在地：$text")) anyOk = true else blocked = true
                    }
                }
                ActionType.WEATHER_NOTIFY -> {
                    val ll = lastKnownLatLng(context)
                    if (ll == null || !hasNotificationPermission(context)) { blocked = true } else {
                        val w = fetchWeather(ll.first, ll.second)
                        if (w != null && postNotify(context, macro, w)) anyOk = true else blocked = true
                    }
                }
                ActionType.WAIT -> {
                    try {
                        val sec = when (item.param) { "10秒" -> 10L; "30秒" -> 30L; "1分" -> 60L; "5分" -> 300L; else -> 30L }
                        Thread.sleep(sec * 1000); anyOk = true
                    } catch (e: Exception) { }
                }
                ActionType.WEB_REQUEST -> {
                    if (sendWebRequest(item.param)) anyOk = true else blocked = true
                }
                ActionType.AI_ASK -> {
                    if (item.param.isBlank() || !hasNotificationPermission(context)) { blocked = true } else {
                        val ans = askAi(context, item.param).answer
                        if (ans != null) {
                            varAi = ans
                            StateStore(context).lastAiResponse = ans
                            if (postNotify(context, macro, ans)) anyOk = true else blocked = true
                        } else blocked = true
                    }
                }
                ActionType.WIDGET_SET -> {
                    StateStore(context).widgetText = item.param
                    KikkakeWidgetProvider.updateAll(context)
                    anyOk = true
                }
                ActionType.MUSIC_PLAY -> {
                    try {
                        val am = context.getSystemService(AudioManager::class.java)
                        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
                        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
                        anyOk = true
                    } catch (e: Exception) { }
                }
                ActionType.SCREEN_KEEP -> {
                    if (!canWriteSettings(context)) { blocked = true } else {
                        try {
                            val cr = context.contentResolver
                            val cur = Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT)
                            val st = StateStore(context)
                            if (cur == Int.MAX_VALUE) {
                                Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, if (st.screenTimeoutPrev > 0) st.screenTimeoutPrev else 120000)
                            } else {
                                if (cur > 0) st.screenTimeoutPrev = cur
                                Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, Int.MAX_VALUE)
                            }
                            anyOk = true
                        } catch (e: Exception) { blocked = true }
                    }
                }
                ActionType.VIDEO_PLAY -> { if (openMediaUrl(context, item.param, video = true)) anyOk = true else blocked = true }
                ActionType.PLAYLIST_PLAY -> { if (openMediaUrl(context, item.param, video = false)) anyOk = true else blocked = true }
                ActionType.CAMERA_OPEN -> {
                    try {
                        context.startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); anyOk = true
                    } catch (e: Exception) { blocked = true }
                }
                ActionType.COMMAND_RUN -> {
                    if (!hasNotificationPermission(context)) { blocked = true } else {
                        val out = runShellCommand(item.param)
                        if (out != null && postNotify(context, macro, out)) anyOk = true else blocked = true
                    }
                }
                ActionType.JS_RUN -> {
                    if (!hasNotificationPermission(context)) { blocked = true } else {
                        if (postNotify(context, macro, runJavaScript(item.param))) anyOk = true else blocked = true
                    }
                }
                ActionType.REPEAT -> { } // 展開済みなのでここには来ない
            }
        }
        if (anyOk) {
            val now = System.currentTimeMillis()
            val store = MacroStore(context)
            val list = store.load().map { if (it.id == macro.id) it.copy(lastRunAt = now) else it }
            store.save(list)
            recordRun(context, macro, true)
            // 一部だけ許可不足で止まった場合は、部分成功として呼び出し側へ伝える（UX2-7）
            return if (blocked) ExecResult.PARTIAL_OK else ExecResult.OK
        }
        recordRun(context, macro, false, if (blocked) "許可が足りない" else "動作できなかった")
        return if (blocked) ExecResult.BLOCKED_BY_PERMISSION else ExecResult.NOTHING_RAN
    }

    // ライトの実際の状態を読む（コールバック登録直後に現在の状態が通知される仕組みを使う）
    private fun currentTorchOn(cm: CameraManager, cameraId: String, fallback: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < 33) return fallback
        var result = fallback
        val latch = CountDownLatch(1)
        val cb = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) { result = enabled; latch.countDown() }
            }
            override fun onTorchModeUnavailable(id: String) { }
        }
        val thread = HandlerThread("kikkake-torch")
        return try {
            thread.start()
            cm.registerTorchCallback(cb, Handler(thread.looper))
            latch.await(400, TimeUnit.MILLISECONDS)
            try { cm.unregisterTorchCallback(cb) } catch (e: Exception) { }
            result
        } catch (e: Exception) { fallback } finally { thread.quitSafely() }
    }

    // TTSの初期化と発声の完了まで待ち、本当に声が出た時だけ true を返す（UX2-1）
    private fun speakAloud(context: Context, text: String): Boolean {
        var tts: TextToSpeech? = null
        return try {
            val initLatch = CountDownLatch(1)
            var initOk = false
            tts = TextToSpeech(context.applicationContext) { status ->
                initOk = status == TextToSpeech.SUCCESS
                initLatch.countDown()
            }
            if (!initLatch.await(3, TimeUnit.SECONDS) || !initOk) { tts?.shutdown(); return false }
            val engine = tts ?: return false
            engine.language = Locale.JAPAN
            val doneLatch = CountDownLatch(1)
            var speakOk = false
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { }
                override fun onDone(utteranceId: String?) { speakOk = true; doneLatch.countDown() }
                @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { doneLatch.countDown() }
                override fun onError(utteranceId: String?, errorCode: Int) { doneLatch.countDown() }
            })
            if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kikkake-tts") != TextToSpeech.SUCCESS) { engine.shutdown(); return false }
            val finished = doneLatch.await(6, TimeUnit.SECONDS)
            val ok = finished && speakOk
            engine.shutdown()
            ok
        } catch (e: Exception) {
            try { tts?.shutdown() } catch (e2: Exception) { }
            false
        }
    }

    fun scheduleTimeAlarm(context: Context, macro: Macro) {
        if (macro.trigger != TriggerType.TIME || !macro.enabled) return
        val timePart = macro.triggerParam.substringBefore("@")
        val parts = timePart.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val days = Logic.timeDaysOf(macro.triggerParam)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            // 曜日指定がある場合は、条件に合う次の日まで進める（最大7日先）
            var guard = 0
            while ((timeInMillis <= System.currentTimeMillis() || (days != null && !days.contains(get(Calendar.DAY_OF_WEEK)))) && guard < 8) {
                add(Calendar.DAY_OF_YEAR, 1); guard++
            }
        }
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmPendingIntent(context, macro.id)
        try {
            if (canExactAlarm(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    fun cancelTimeAlarm(context: Context, macro: Macro) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(alarmPendingIntent(context, macro.id))
    }

    private fun alarmPendingIntent(context: Context, macroId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply { putExtra(EXTRA_MACRO_ID, macroId) }
        return PendingIntent.getBroadcast(context, macroId.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun rescheduleAll(context: Context) {
        MacroStore(context).load().filter { it.enabled }.forEach {
            scheduleTimeAlarm(context, it)
            scheduleSunAlarm(context, it)
        }
    }

    // 日の出・日の入りアラーム（パラメータ "sunrise@緯度,経度" / "sunset@緯度,経度"）
    fun scheduleSunAlarm(context: Context, macro: Macro) {
        if (macro.trigger != TriggerType.SUN_EVENT || !macro.enabled) return
        val parts = macro.triggerParam.split("@")
        val sunrise = parts.getOrNull(0) != "sunset"
        val ll = parts.getOrNull(1)?.split(",")
        val lat = ll?.getOrNull(0)?.toDoubleOrNull() ?: return
        val lng = ll?.getOrNull(1)?.toDoubleOrNull() ?: return
        val now = Calendar.getInstance()
        val tzMin = (now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET)) / 60_000
        var target: Calendar? = null
        for (add in 0..1) {
            val day = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, add) }
            val mins = Logic.sunEventMinutes(lat, lng, day.get(Calendar.DAY_OF_YEAR), tzMin, sunrise)
            if (mins < 0) continue
            val cand = (day.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, mins / 60); set(Calendar.MINUTE, mins % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (cand.timeInMillis > System.currentTimeMillis()) { target = cand; break }
        }
        val t = target ?: return
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmPendingIntent(context, macro.id)
        try {
            if (canExactAlarm(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.timeInMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.timeInMillis, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.timeInMillis, pi)
        }
    }

    // 端末状態（集中モード／省電力／機内モード）のエッジ検出。レシーバーと定期チェックの両方から呼ばれる
    fun checkStateEdges(context: Context) {
        val st = StateStore(context)
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            val dndOn = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL && nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            fireStateTrigger(context, TriggerType.DND_STATE, st.stateDnd, dndOn) { st.stateDnd = it }
        } catch (e: Exception) { }
        try {
            val pm = context.getSystemService(android.os.PowerManager::class.java)
            fireStateTrigger(context, TriggerType.POWER_SAVE, st.statePowerSave, pm.isPowerSaveMode) { st.statePowerSave = it }
        } catch (e: Exception) { }
        try {
            val airOn = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
            fireStateTrigger(context, TriggerType.AIRPLANE, st.stateAirplane, airOn) { st.stateAirplane = it }
        } catch (e: Exception) { }
    }

    private fun fireStateTrigger(context: Context, type: TriggerType, prev: Int, nowOn: Boolean, save: (Int) -> Unit) {
        val now = if (nowOn) 1 else 0
        if (prev == now) return
        save(now)
        if (prev == -1) return // 初回観測は発火させず状態だけ覚える
        val want = if (nowOn) "on" else "off"
        MacroStore(context).load().filter { it.enabled && it.trigger == type && (it.triggerParam == want || it.triggerParam.isBlank()) }.forEach { execute(context, it) }
    }

    fun onBluetoothConnected(context: Context, address: String?) {
        MacroStore(context).load().filter {
            it.enabled && it.trigger == TriggerType.BLUETOOTH && (it.triggerParam.isBlank() || it.triggerParam == address)
        }.forEach { execute(context, it) }
    }

    // 通知が来たら：パラメータ "パッケージ名|キーワード"（キーワード空なら全部）
    fun onNotificationPosted(context: Context, pkg: String, title: String, text: String) {
        if (pkg == context.packageName) return
        MacroStore(context).load().filter { it.enabled && it.trigger == TriggerType.NOTIFICATION_POSTED }.forEach { m ->
            val parts = m.triggerParam.split("|")
            val targetPkg = parts.getOrNull(0) ?: ""
            val keyword = parts.getOrNull(1) ?: ""
            if (targetPkg.isNotBlank() && targetPkg == pkg && (keyword.isBlank() || title.contains(keyword) || text.contains(keyword))) {
                execute(context, m)
            }
        }
    }

    // 場所きっかけ：最後に分かっている位置と登録地点（半径200m）の内外エッジで発火
    fun checkLocations(context: Context) {
        val targets = MacroStore(context).load().filter { it.enabled && it.trigger == TriggerType.LOCATION }
        if (targets.isEmpty()) return
        val ll = lastKnownLatLng(context) ?: return
        val st = StateStore(context)
        val states = st.placeState.split(",").filter { it.isNotBlank() }.associate { it.substringBefore("=") to (it.substringAfter("=", "0") == "1") }.toMutableMap()
        var changed = false
        targets.forEach { m ->
            val parts = m.triggerParam.split("@")
            val tll = parts.getOrNull(1)?.split(",")
            val tLat = tll?.getOrNull(0)?.toDoubleOrNull() ?: return@forEach
            val tLng = tll?.getOrNull(1)?.toDoubleOrNull() ?: return@forEach
            val res = FloatArray(1)
            android.location.Location.distanceBetween(ll.first, ll.second, tLat, tLng, res)
            val inside = res[0] <= 200f
            val key = m.id.toString()
            val prev = states[key]
            if (prev == null) { states[key] = inside; changed = true }
            else if (prev != inside) {
                states[key] = inside; changed = true
                val mode = parts.getOrNull(0) ?: "arrive"
                if ((mode == "arrive" && inside) || (mode == "leave" && !inside)) execute(context, m)
            }
        }
        if (changed) st.placeState = states.entries.joinToString(",") { "${it.key}=${if (it.value) 1 else 0}" }
    }

    // アプリを閉じたら：前面アプリの移り変わりで判定（定期チェック内）
    fun checkAppClosed(context: Context) {
        val targets = MacroStore(context).load().filter { it.enabled && it.trigger == TriggerType.APP_CLOSED && it.triggerParam.isNotBlank() }
        if (targets.isEmpty()) return
        if (!hasUsageAccess(context)) return
        try {
            val st = StateStore(context)
            val now = System.currentTimeMillis()
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val events = usm.queryEvents(now - 15 * 60_000, now)
            val ev = android.app.usage.UsageEvents.Event()
            var fg: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(ev)
                if (ev.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED || ev.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) fg = ev.packageName
            }
            val current = fg ?: return
            val prev = st.lastFgPkg
            if (prev.isNotBlank() && prev != current && current != context.packageName) {
                targets.filter { it.triggerParam == prev }.forEach { execute(context, it) }
            }
            if (current != context.packageName) st.lastFgPkg = current
        } catch (e: Exception) { }
    }

    fun checkBattery(context: Context, level: Int) {
        val st = StateStore(context)
        val prev = st.lastBattery
        val store = MacroStore(context)
        store.load().filter { it.enabled && it.trigger == TriggerType.BATTERY_LOW }.forEach { m ->
            val th = m.triggerParam.replace("%", "").trim().toIntOrNull() ?: 20
            if (Logic.batteryCrossed(prev, level, th)) execute(context, m)
        }
        store.load().filter { it.enabled && it.trigger == TriggerType.BATTERY_RISES }.forEach { m ->
            val th = m.triggerParam.replace("%", "").trim().toIntOrNull() ?: 80
            if (Logic.batteryRisen(prev, level, th)) execute(context, m)
        }
        st.lastBattery = level
    }

    fun checkWifi(context: Context, connected: Boolean) {
        val st = StateStore(context)
        val prev = st.lastWifi
        if (Logic.wifiConnectedEdge(prev, connected)) {
            MacroStore(context).load().filter { it.enabled && it.trigger == TriggerType.WIFI_CONNECTED }.forEach { execute(context, it) }
        }
        st.lastWifi = connected
    }

    fun onChargingStarted(context: Context) {
        MacroStore(context).load().filter { it.enabled && it.trigger == TriggerType.CHARGING_START }.forEach { execute(context, it) }
    }

    fun onChargingDone(context: Context) {
        MacroStore(context).load().filter { it.enabled && it.trigger == TriggerType.CHARGING_DONE }.forEach { execute(context, it) }
    }

    fun onHeadsetPlugged(context: Context) {
        MacroStore(context).load().filter { it.enabled && it.trigger == TriggerType.HEADSET }.forEach { execute(context, it) }
    }

    fun checkAppLaunches(context: Context) {
        val st = StateStore(context)
        val now = System.currentTimeMillis()
        val from = if (st.lastUsageCheck == 0L) now - 15 * 60_000 else st.lastUsageCheck
        val targets = MacroStore(context).load().filter { it.enabled && it.trigger == TriggerType.APP_LAUNCH && it.triggerParam.isNotBlank() }
        if (targets.isEmpty()) { st.lastUsageCheck = now; return }
        if (!hasUsageAccess(context)) { st.lastUsageCheck = now; return }
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val events = usm.queryEvents(from, now)
            val ev = android.app.usage.UsageEvents.Event()
            val launchedAt = mutableMapOf<String, Long>()
            while (events.hasNextEvent()) {
                events.getNextEvent(ev)
                if (ev.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED || ev.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    launchedAt[ev.packageName] = ev.timeStamp
                }
            }
            targets.forEach { m ->
                val t = launchedAt[m.triggerParam]
                if (t != null && t > m.lastRunAt) execute(context, m)
            }
        } catch (e: Exception) { }
        st.lastUsageCheck = now
    }
}
