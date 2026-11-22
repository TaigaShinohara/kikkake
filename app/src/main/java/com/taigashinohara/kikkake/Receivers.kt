package com.taigashinohara.kikkake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

// 実行はバックグラウンドスレッドで行う（声で知らせる等、完了待ちのある動作のため）
private fun BroadcastReceiver.runAsync(context: Context, block: (Context) -> Unit) {
    val pending = goAsync()
    val appCtx = context.applicationContext
    Thread {
        try { block(appCtx) } catch (e: Exception) { } finally { pending.finish() }
    }.start()
}

class PowerConnectedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED) runAsync(context) { Engine.onChargingStarted(it) }
    }
}

class PowerDisconnectedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_DISCONNECTED) runAsync(context) { Engine.onChargingDone(it) }
    }
}

class HeadsetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_HEADSET_PLUG) {
            val state = intent.getIntExtra("state", -1)
            if (state == 1) runAsync(context) { Engine.onHeadsetPlugged(it) }
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Engine.EXTRA_MACRO_ID, -1L)
        if (id <= 0) return
        val macro = MacroStore(context).load().find { it.id == id } ?: return
        if (macro.enabled) {
            runAsync(context) {
                Engine.execute(it, macro)
                Engine.scheduleTimeAlarm(it, macro)
                Engine.scheduleSunAlarm(it, macro)
            }
        }
    }
}

class AirplaneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) runAsync(context) { Engine.checkStateEdges(it) }
    }
}

class PowerSaveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) runAsync(context) { Engine.checkStateEdges(it) }
    }
}

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED) {
            @Suppress("DEPRECATION") val device = intent.getParcelableExtra<android.bluetooth.BluetoothDevice>(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
            val addr = try { device?.address } catch (e: Exception) { null }
            runAsync(context) { Engine.onBluetoothConnected(it, addr) }
        }
    }
}

// 通知が来たら（メール・メッセージ・支払い通知もここで拾う）
class KikkakeNotificationListener : android.service.notification.NotificationListenerService() {
    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification) {
        try {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
            val ctx = applicationContext
            val pkg = sbn.packageName
            Thread { try { Engine.onNotificationPosted(ctx, pkg, title, text) } catch (e: Exception) { } }.start()
        } catch (e: Exception) { }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Engine.rescheduleAll(context)
            MonitorWorker.enqueue(context)
        }
    }
}

class MonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        try {
            val batteryIntent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            if (level >= 0 && scale > 0) Engine.checkBattery(ctx, level * 100 / scale)
        } catch (e: Exception) { }
        try {
            val cm = ctx.getSystemService(ConnectivityManager::class.java)
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            Engine.checkWifi(ctx, wifi)
        } catch (e: Exception) { }
        try { Engine.checkAppLaunches(ctx) } catch (e: Exception) { }
        try { Engine.checkAppClosed(ctx) } catch (e: Exception) { }
        try { Engine.checkStateEdges(ctx) } catch (e: Exception) { }
        try { Engine.checkLocations(ctx) } catch (e: Exception) { }
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val req = PeriodicWorkRequestBuilder<MonitorWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("kikkake-monitor", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
