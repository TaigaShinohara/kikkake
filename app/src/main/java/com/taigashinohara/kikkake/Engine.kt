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
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.widget.Toast
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
        var anyOk = false
        var blocked = false
        for (item in macro.actions) {
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
                        try { Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 140); anyOk = true } catch (e: Exception) { blocked = true }
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
                        try { Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 30); anyOk = true } catch (e: Exception) { blocked = true }
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
            }
        }
        if (blocked && !anyOk) Toast.makeText(context, "許可が足りない動作があります。ホームの「権限チェック」から許可してね", Toast.LENGTH_LONG).show()
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
        val parts = macro.triggerParam.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
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
        MacroStore(context).load().filter { it.enabled }.forEach { scheduleTimeAlarm(context, it) }
    }

    fun checkBattery(context: Context, level: Int) {
        val st = StateStore(context)
        val prev = st.lastBattery
        val store = MacroStore(context)
        store.load().filter { it.enabled && it.trigger == TriggerType.BATTERY_LOW }.forEach { m ->
            val th = m.triggerParam.replace("%", "").trim().toIntOrNull() ?: 20
            if (Logic.batteryCrossed(prev, level, th)) execute(context, m)
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
