package app.kikkake

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

object Engine {
    const val CHANNEL_ID = "kikkake"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "きっかけ", NotificationManager.IMPORTANCE_DEFAULT))
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

    /** @return 実際に実行できたか */
    fun execute(context: Context, macro: Macro, showToast: Boolean = true): Boolean {
        val cal = Calendar.getInstance()
        if (!Logic.constraintOk(macro.constraintType, cal.get(Calendar.DAY_OF_WEEK), cal.get(Calendar.HOUR_OF_DAY))) {
            if (showToast) Toast.makeText(context, "条件に合わないため実行しませんでした", Toast.LENGTH_SHORT).show()
            return false
        }
        var ok = true
        try {
            when (macro.action) {
                ActionType.NOTIFICATION -> {
                    if (!hasNotificationPermission(context)) {
                        if (showToast) Toast.makeText(context, "通知の許可が必要です。ホームの「権限チェック」から許可してね", Toast.LENGTH_LONG).show()
                        ok = false
                    } else {
                        ensureChannel(context)
                        val nm = context.getSystemService(NotificationManager::class.java)
                        val n = NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle("きっかけ")
                            .setContentText("「${macro.name}」が動きました")
                            .setAutoCancel(true).build()
                        try { nm.notify(macro.id.toInt(), n) } catch (e: SecurityException) { ok = false }
                    }
                }
                ActionType.VOLUME_UP -> {
                    val am = context.getSystemService(AudioManager::class.java)
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * 0.7).toInt(), 0)
                    if (showToast) Toast.makeText(context, "音量を上げました", Toast.LENGTH_SHORT).show()
                }
                ActionType.BRIGHTNESS_DOWN -> {
                    if (!canWriteSettings(context)) {
                        if (showToast) Toast.makeText(context, "明るさ変更には許可が必要です。ホームの「権限チェック」から許可してね", Toast.LENGTH_LONG).show()
                        ok = false
                    } else {
                        val cur = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 120)
                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (cur / 2).coerceAtLeast(20))
                        if (showToast) Toast.makeText(context, "明るさを下げました", Toast.LENGTH_SHORT).show()
                    }
                }
                ActionType.SILENT -> {
                    if (!hasDndAccess(context)) {
                        if (showToast) Toast.makeText(context, "マナーモードには許可が必要です。ホームの「権限チェック」から許可してね", Toast.LENGTH_LONG).show()
                        ok = false
                    } else {
                        val am = context.getSystemService(AudioManager::class.java)
                        am.ringerMode = AudioManager.RINGER_MODE_SILENT
                        if (showToast) Toast.makeText(context, "マナーモードにしました", Toast.LENGTH_SHORT).show()
                    }
                }
                ActionType.LAUNCH_APP -> {
                    val pkg = macro.actionParam.ifBlank { "" }
                    val intent = if (pkg.isNotBlank()) context.packageManager.getLaunchIntentForPackage(pkg) else null
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } else {
                        if (showToast) Toast.makeText(context, "開くアプリが選ばれていないか、見つかりません", Toast.LENGTH_LONG).show()
                        ok = false
                    }
                }
                ActionType.FLASHLIGHT -> {
                    val state = StateStore(context)
                    val cm = context.getSystemService(CameraManager::class.java)
                    val id = cm.cameraIdList.firstOrNull()
                    if (id != null) {
                        val next = !state.torchOn
                        cm.setTorchMode(id, next)
                        state.torchOn = next
                        if (showToast) Toast.makeText(context, if (next) "ライトをつけました" else "ライトを消しました", Toast.LENGTH_SHORT).show()
                    } else ok = false
                }
            }
        } catch (e: Exception) {
            if (showToast) Toast.makeText(context, "実行できませんでした: ${e.message}", Toast.LENGTH_SHORT).show()
            ok = false
        }
        if (ok) {
            val store = MacroStore(context)
            val list = store.load().map { if (it.id == macro.id) it.copy(lastRunAt = System.currentTimeMillis()) else it }
            store.save(list)
        }
        return ok
    }

    fun scheduleTimeAlarm(context: Context, macro: Macro) {
        if (macro.trigger != TriggerType.TIME || !macro.enabled) return
        val parts = macro.triggerParam.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val min = parts[1].toIntOrNull() ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, min); set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }
        val intent = Intent(context, AlarmReceiver::class.java).apply { putExtra("macroId", macro.id) }
        val pi = PendingIntent.getBroadcast(context, macro.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = context.getSystemService(AlarmManager::class.java)
        try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi) } catch (e: Exception) { }
    }

    fun cancelTimeAlarm(context: Context, macro: Macro) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, macro.id.toInt(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        val am = context.getSystemService(AlarmManager::class.java)
        try { am.cancel(pi) } catch (e: Exception) { }
    }

    fun rescheduleAll(context: Context) {
        MacroStore(context).load().filter { it.enabled }.forEach { scheduleTimeAlarm(context, it) }
    }

    /** 電池レベルをチェックし、しきい値を下回った瞬間のマクロだけ実行（連発防止） */
    fun checkBattery(context: Context, levelPct: Int) {
        val state = StateStore(context)
        val prev = state.lastBattery
        MacroStore(context).load().filter { it.enabled && it.trigger == TriggerType.BATTERY_LOW }.forEach { m ->
            val th = m.triggerParam.replace("%", "").toIntOrNull() ?: 20
            if (Logic.batteryCrossed(prev, levelPct, th)) execute(context, m, showToast = false)
        }
        state.lastBattery = levelPct
    }

    /** Wi-Fi接続状態をチェックし、つながった瞬間のマクロだけ実行 */
    fun checkWifi(context: Context, connected: Boolean) {
        val state = StateStore(context)
        val prev = state.lastWifi
        if (Logic.wifiConnectedEdge(prev, connected)) {
            MacroStore(context).load().filter { it.enabled && it.trigger == TriggerType.WIFI_CONNECTED }
                .forEach { execute(context, it, showToast = false) }
        }
        state.lastWifi = connected
    }

    fun onChargingStarted(context: Context) {
        MacroStore(context).load().filter { it.enabled && it.trigger == TriggerType.CHARGING_START }
            .forEach { execute(context, it, showToast = false) }
    }
}
