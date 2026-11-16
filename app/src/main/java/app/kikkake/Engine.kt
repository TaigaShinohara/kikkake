package app.kikkake

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.Calendar

object Engine {
    const val CHANNEL_ID = "kikkake"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "きっかけ", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    fun execute(context: Context, macro: Macro, showToast: Boolean = true) {
        val cal = Calendar.getInstance()
        if (!Logic.constraintOk(macro.constraintType, cal.get(Calendar.DAY_OF_WEEK), cal.get(Calendar.HOUR_OF_DAY))) {
            if (showToast) Toast.makeText(context, "条件に合わないため実行しませんでした", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            when (macro.action) {
                ActionType.NOTIFICATION -> {
                    ensureChannel(context)
                    val nm = context.getSystemService(NotificationManager::class.java)
                    val n = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("きっかけ")
                        .setContentText("「${macro.name}」が動きました")
                        .setAutoCancel(true).build()
                    try { nm.notify(macro.id.toInt(), n) } catch (e: SecurityException) {
                        if (showToast) Toast.makeText(context, "通知の許可が必要です", Toast.LENGTH_SHORT).show()
                    }
                }
                ActionType.VOLUME_UP -> {
                    val am = context.getSystemService(AudioManager::class.java)
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * 0.7).toInt(), 0)
                    if (showToast) Toast.makeText(context, "音量を上げました", Toast.LENGTH_SHORT).show()
                }
                ActionType.BRIGHTNESS_DOWN -> {
                    try {
                        val cur = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 120)
                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (cur / 2).coerceAtLeast(20))
                        if (showToast) Toast.makeText(context, "明るさを下げました", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        if (showToast) Toast.makeText(context, "明るさ変更には設定の許可が必要です", Toast.LENGTH_LONG).show()
                    }
                }
                ActionType.SILENT -> {
                    val am = context.getSystemService(AudioManager::class.java)
                    try {
                        am.ringerMode = AudioManager.RINGER_MODE_SILENT
                        if (showToast) Toast.makeText(context, "マナーモードにしました", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        if (showToast) Toast.makeText(context, "マナーモードには許可が必要です", Toast.LENGTH_LONG).show()
                    }
                }
                ActionType.LAUNCH_APP -> {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.android.settings")
                    if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
                }
                ActionType.FLASHLIGHT -> {
                    val cm = context.getSystemService(CameraManager::class.java)
                    val id = cm.cameraIdList.firstOrNull()
                    if (id != null) cm.setTorchMode(id, true)
                    if (showToast) Toast.makeText(context, "ライトをつけました", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            if (showToast) Toast.makeText(context, "実行できませんでした: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        // update lastRunAt
        val store = MacroStore(context)
        val list = store.load().map { if (it.id == macro.id) it.copy(lastRunAt = System.currentTimeMillis()) else it }
        store.save(list)
    }

    fun scheduleTimeAlarm(context: Context, macro: Macro) {
        if (macro.trigger != TriggerType.TIME) return
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
        try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi) } catch (e: Exception) { /* ignore in MVP */ }
    }

    fun handleBattery(context: Context, levelPercent: Int, chargingStarted: Boolean) {
        val store = MacroStore(context)
        store.load().filter { it.enabled }.forEach { m ->
            when {
                chargingStarted && m.trigger == TriggerType.CHARGING_START -> execute(context, m, showToast = false)
                m.trigger == TriggerType.BATTERY_LOW -> {
                    val th = m.triggerParam.replace("%", "").toIntOrNull() ?: 20
                    if (levelPercent <= th) execute(context, m, showToast = false)
                }
            }
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("macroId", -1L)
        if (id < 0) return
        val store = MacroStore(context)
        val macro = store.load().find { it.id == id && it.enabled } ?: return
        Engine.execute(context, macro, showToast = false)
        Engine.scheduleTimeAlarm(context, macro)
    }
}

class PowerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> Engine.handleBattery(context, 100, chargingStarted = true)
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val pct = if (scale > 0) level * 100 / scale else 100
                Engine.handleBattery(context, pct, chargingStarted = false)
            }
        }
    }
}
